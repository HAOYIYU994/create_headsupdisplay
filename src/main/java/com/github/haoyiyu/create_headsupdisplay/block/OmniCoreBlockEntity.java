package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.config.DisplaySlot;
import com.github.haoyiyu.create_headsupdisplay.config.ImageSlot;
import com.github.haoyiyu.create_headsupdisplay.config.RadarSlot;
import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import com.github.haoyiyu.create_headsupdisplay.network.OpenOmniCoreScreenPayload;
import com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.github.haoyiyu.create_headsupdisplay.util.RedstoneSignalHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OmniCoreBlockEntity extends BlockEntity {
    /** 1x1 透明 PNG，IMAGE_CONDITIONAL 无匹配时保持槽位 */
    private static final byte[] BLANK_PNG;
    static {
        byte[] b = null;
        try {
            var img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 0);
            var bos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", bos);
            b = bos.toByteArray();
        } catch (Throwable ignored) {}
        BLANK_PNG = b != null && b.length > 0 ? b : new byte[0];
    }

    private boolean autoSortEnabled = true;
    private int nextSourceId = 1; // 固定的数字编码计数器
    private List<BlockPos> boundTerminals = new ArrayList<>();
    private Map<BlockPos, String> terminalNames = new HashMap<>();
    private List<RedstoneSource> sources = new ArrayList<>();
    private Map<Integer, BlockPos> sentSources = new HashMap<>();

    // 雷达图槽位
    private List<RadarSlot> radarSlots = new ArrayList<>();
    private BlockPos linkedMonitorPos;

    // 插件存储（27格箱子）
    private net.minecraft.world.SimpleContainer pluginInventory = new net.minecraft.world.SimpleContainer(27) {
        @Override public void setChanged() { OmniCoreBlockEntity.this.setChanged(); }
    };

    /** 每 tick 扫描插件槽中的 DataProbe 底板，拉取读取值并更新 PROBE 源 */
    private void pollProbesFromPlugins() {
        java.util.Set<BlockPos> seenProbes = new java.util.HashSet<>();
        for (int i = 0; i < pluginInventory.getContainerSize(); i++) {
            ItemStack stack = pluginInventory.getItem(i);
            if (stack.isEmpty()) continue;
            BlockPos probePos = stack.get(com.github.haoyiyu.create_headsupdisplay.registration.ModDataComponents.LINKED_PROBE_POS.get());
            if (probePos == null) continue;
            seenProbes.add(probePos);

            BlockEntity probeBe = level.getBlockEntity(probePos);
            if (!(probeBe instanceof NbtReaderBlockEntity probe)) continue;
            if (probe.getSelectedChannelId().isEmpty()) continue;

            String value = probe.readValue();
            String name = probe.getProbeName();
            // 找到或创建 PROBE 源
            RedstoneSource existing = null;
            for (RedstoneSource src : sources) {
                if (src.sourceType == RedstoneSource.Type.PROBE && probePos.equals(src.probeSourcePos)) {
                    existing = src;
                    break;
                }
            }
            if (existing != null) {
                if (!value.equals(existing.probeValue)) {
                    existing.probeValue = value;
                    existing.name = name;
                    existing.lastSentText = value;
                    setChanged();
                    sendProbeToTerminals(sources.indexOf(existing));
                }
            } else {
                // 添加新 PROBE 源
                RedstoneSource src = RedstoneSource.probe(probePos, name, value);
                assignSourceId(src);
                sources.add(0, src);
                remapSentAfterInsert();
                setChanged();
                sendProbeToTerminals(0);
            }
        }

        // 清理已拔出插件的 PROBE 源
        var toRemove = new java.util.ArrayList<Integer>();
        for (int i = 0; i < sources.size(); i++) {
            RedstoneSource src = sources.get(i);
            if (src.sourceType == RedstoneSource.Type.PROBE && !seenProbes.contains(src.probeSourcePos)) {
                toRemove.add(i);
            }
        }
        for (int j = toRemove.size() - 1; j >= 0; j--) {
            removeRedstoneSource(toRemove.get(j));
        }
    }

    /** 新源插入列表顶部后重映射 sentSources */
    private void remapSentAfterInsert() {
        java.util.Map<Integer, BlockPos> remapped = new java.util.HashMap<>();
        for (var entry : sentSources.entrySet()) {
            remapped.put(entry.getKey() + 1, entry.getValue());
        }
        sentSources = remapped;
    }

    public boolean hasImagePlugin() {
        for (int i = 0; i < pluginInventory.getContainerSize(); i++)
            if (pluginInventory.getItem(i).getItem() instanceof com.github.haoyiyu.create_headsupdisplay.item.ImagePluginItem)
                return true;
        return false;
    }

    public boolean hasRadarPlugin() {
        for (int i = 0; i < pluginInventory.getContainerSize(); i++)
            if (pluginInventory.getItem(i).getItem() instanceof com.github.haoyiyu.create_headsupdisplay.item.RadarPluginItem)
                return true;
        return false;
    }

    public net.minecraft.world.SimpleContainer getPluginInventory() { return pluginInventory; }

    public net.minecraft.core.NonNullList<ItemStack> getPluginSlots() {
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        for (int i = 0; i < 27; i++) list.set(i, pluginInventory.getItem(i));
        return list;
    }

    public void setPluginSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 27) { pluginInventory.setItem(slot, stack.copyWithCount(1)); setChanged(); }
    }

    public ItemStack removePluginSlot(int slot) {
        if (slot >= 0 && slot < 27) {
            ItemStack ret = pluginInventory.getItem(slot).copy();
            pluginInventory.setItem(slot, ItemStack.EMPTY);
            setChanged();
            return ret;
        }
        return ItemStack.EMPTY;
    }

    /** 图片插件被拔出 → 通知所有终端删除图片槽位 */
    public void cleanupImageSources() {
        for (BlockPos tp : boundTerminals) {
            BlockEntity be = level.getBlockEntity(tp);
            if (be instanceof DisplayTerminalBlockEntity terminal) {
                for (var img : new ArrayList<>(terminal.getImageSlots()))
                    terminal.removeImageSlot(img.getImageId());
            } else if (be instanceof DisplayTerminalProBlockEntity terminal) {
                for (var img : new ArrayList<>(terminal.getImageSlots()))
                    terminal.removeImageSlot(img.getImageId());
            }
        }
        sentSources.clear();
    }

    /** 雷达插件被拔出 → 清空终端雷达槽位 */
    public void cleanupRadarSources() {
        for (BlockPos tp : boundTerminals) {
            BlockEntity be = level.getBlockEntity(tp);
            if (be instanceof DisplayTerminalBlockEntity terminal) {
                terminal.setRadarSlots(new ArrayList<>());
            } else if (be instanceof DisplayTerminalProBlockEntity terminal) {
                terminal.setRadarSlots(new ArrayList<>());
            }
        }
    }

    public OmniCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMNI_CORE.get(), pos, state);
    }

    public boolean isAutoSortEnabled() { return autoSortEnabled; }
    public void toggleAutoSort() { this.autoSortEnabled = !this.autoSortEnabled; setChanged(); }

    public void setBoundTerminal(BlockPos pos) {
        if (!boundTerminals.contains(pos)) {
            boundTerminals.add(pos);
        }
        setChanged();
    }

    public BlockPos getBoundTerminal() {
        return boundTerminals.isEmpty() ? null : boundTerminals.get(0);
    }

    public List<BlockPos> getBoundTerminals() { return boundTerminals; }

    public void removeBoundTerminal(BlockPos pos) {
        boundTerminals.remove(pos);
        terminalNames.remove(pos);
        setChanged();
    }

    public String getTerminalName(BlockPos pos) {
        return terminalNames.getOrDefault(pos, "");
    }

    public void setTerminalName(BlockPos pos, String name) {
        if (name == null || name.isEmpty()) terminalNames.remove(pos);
        else terminalNames.put(pos, name);
        setChanged();
    }

    public void clearLinkedMonitor() {
        linkedMonitorPos = null;
        setChanged();
    }

    /** 连接雷达 Monitor，自动创建默认雷达槽位并同步到终端 */
    public void setLinkedMonitor(BlockPos pos) {
        this.linkedMonitorPos = pos;
        if (radarSlots.isEmpty()) {
            RadarSlot slot = new RadarSlot();
            slot.setPos(200, 10);
            slot.setScale(1.0f);
            slot.setAlpha(220);
            slot.setRadarRange(50);
            radarSlots.add(slot);
        }
        setChanged();
        CreateHeadsUpDisplay.LOGGER.info("OMNI linkedMonitor SET to {}", pos);
    }

    /** 将雷达槽位配置推送到指定终端（-1 = 所有） */
    public void pushRadarSlotsToTerminal(int terminalIndex) {
        if (level == null) return;
        var targets = new ArrayList<BlockPos>();
        if (terminalIndex < 0) {
            targets.addAll(boundTerminals);
        } else if (terminalIndex < boundTerminals.size()) {
            targets.add(boundTerminals.get(terminalIndex));
        }
        for (BlockPos tp : targets) {
            BlockEntity be = level.getBlockEntity(tp);
            if (be instanceof DisplayTerminalBlockEntity terminal) {
                // 深拷贝：每个终端独立 RadarSlot 对象
                List<RadarSlot> copy = new ArrayList<>();
                for (RadarSlot s : radarSlots) copy.add(RadarSlot.deserialize(s.serialize()));
                terminal.setRadarSlots(copy);
            }
        }
    }

    public void pushRadarSlotsToTerminal() {
        pushRadarSlotsToTerminal(-1);
    }

    public BlockPos getLinkedMonitor() { return linkedMonitorPos; }

    public void openConfigScreen(Player player) {
        if (level == null) return;
        if (player instanceof ServerPlayer sp) {
            CompoundTag data = new CompoundTag();
            data.putLong("CorePos", worldPosition.asLong());
            if (!boundTerminals.isEmpty()) {
                data.putLong("BoundTerminal", boundTerminals.get(0).asLong());
            }
            data.putBoolean("HasImagePlugin", hasImagePlugin());
            data.putBoolean("HasRadarPlugin", hasRadarPlugin());

            ListTag terminalsTag = new ListTag();
            for (BlockPos tp : boundTerminals) {
                CompoundTag tt = new CompoundTag();
                tt.putLong("Pos", tp.asLong());
                String name = terminalNames.get(tp);
                if (name != null && !name.isEmpty()) tt.putString("Name", name);
                terminalsTag.add(tt);
            }
            data.put("BoundTerminalsList", terminalsTag);
            ListTag sourcesTag = new ListTag();
            for (RedstoneSource src : sources) {
                CompoundTag srcTag = new CompoundTag();
                srcTag.putString("type", src.sourceType.name());
                srcTag.putBoolean("system", src.systemSource);
                srcTag.putString("name", src.name);
                srcTag.put("freqItem1", src.frequencyItem1.saveOptional(level.registryAccess()));
                srcTag.put("freqItem2", src.frequencyItem2.saveOptional(level.registryAccess()));
                if (src.displayLinkText != null) {
                    srcTag.putString("dlText", src.displayLinkText);
                    srcTag.putLong("dlSourcePos", src.displayLinkSourcePos.asLong());
                }
                if (src.translation != null && src.translation.getMode() != TranslationConfig.Mode.NONE) {
                    srcTag.put("translation", src.translation.serialize());
                }
                // IMAGE 字段
                if (src.sourceType == RedstoneSource.Type.IMAGE) {
                    srcTag.putUUID("ImageId", src.imageId);
                    srcTag.putString("ImageFileName", src.imageFileName);
                    srcTag.putByteArray("ImageData", src.imageData);
                }
                sourcesTag.add(srcTag);
            }
            data.put("Sources", sourcesTag);

            // 雷达槽位
            ListTag radarSlotTag = new ListTag();
            for (RadarSlot slot : radarSlots) {
                radarSlotTag.add(slot.serialize());
            }
            data.put("RadarSlots", radarSlotTag);

            PacketDistributor.sendToPlayer(sp, new OpenOmniCoreScreenPayload(data));
        }
    }

    private int assignSourceId(RedstoneSource src) {
        int id = nextSourceId++;
        src.sourceId = id;
        return id;
    }

    public void addRedstoneSource(String name, ItemStack frequencyItem1, ItemStack frequencyItem2) {
        RedstoneSource src = new RedstoneSource(name, frequencyItem1, frequencyItem2);
        assignSourceId(src);
        sources.add(src);
        setChanged();
    }

    /** 添加一个 IMAGE 类型的图片源 */
    public void addImageSource(UUID imageId, String fileName, byte[] imageData) {
        if (imageData == null || imageData.length > com.github.haoyiyu.create_headsupdisplay.config.ModConfig.IMAGE_MAX_SIZE_KB.get() * 1024) return;
        if (imageData.length < 8) return;
        RedstoneSource src = RedstoneSource.image(imageId, fileName, imageData);
        assignSourceId(src);
        sources.add(src);
        setChanged();
    }

    public void removeRedstoneSource(int index) {
        if (index >= 0 && index < sources.size()) {
            // 先移除对应终端槽位
            removeFromTerminal(index);
            sources.remove(index);
            // 重排 sentSources：index 之后的键减 1
            Map<Integer, BlockPos> remapped = new HashMap<>();
            for (var entry : sentSources.entrySet()) {
                int oldIdx = entry.getKey();
                if (oldIdx == index) continue; // 已删除
                remapped.put(oldIdx > index ? oldIdx - 1 : oldIdx, entry.getValue());
            }
            sentSources = remapped;
            setChanged();
        }
    }

    /** 按名称查找源索引，找不到返回 -1 */
    public int findSourceIndexByName(String name) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    private void removeFromTerminal(int sourceIndex) {
        for (BlockPos tp : boundTerminals) {
            BlockEntity be = level.getBlockEntity(tp);
            if (be instanceof DisplayTerminalBlockEntity terminal) {
                removeSourceFromTerminal(terminal, sourceIndex);
            } else if (be instanceof DisplayTerminalProBlockEntity terminalPro) {
                removeSourceFromTerminalPro(terminalPro, sourceIndex);
            }
        }
        sentSources.remove(sourceIndex);
    }

    private void removeSourceFromTerminal(DisplayTerminalBlockEntity terminal, int sourceIndex) {
        if (sourceIndex >= 0 && sourceIndex < sources.size()) {
            RedstoneSource src = sources.get(sourceIndex);
            if (src.sourceType == RedstoneSource.Type.IMAGE && src.imageId != null) {
                terminal.removeImageSlot(src.imageId);
            } else if (src.translation != null && src.translation.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
                UUID slotId = UUID.nameUUIDFromBytes(("img_cond_" + src.name).getBytes());
                terminal.removeImageSlot(slotId);
            } else {
                BlockPos virtualPos = sentSources.get(sourceIndex);
                if (virtualPos != null) terminal.removeSlot(virtualPos);
            }
        }
    }

    private void removeSourceFromTerminalPro(DisplayTerminalProBlockEntity terminal, int sourceIndex) {
        if (sourceIndex >= 0 && sourceIndex < sources.size()) {
            RedstoneSource src = sources.get(sourceIndex);
            if (src.sourceType == RedstoneSource.Type.IMAGE && src.imageId != null) {
                terminal.removeImageSlot(src.imageId);
            } else if (src.translation != null && src.translation.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
                UUID slotId = UUID.nameUUIDFromBytes(("img_cond_" + src.name).getBytes());
                terminal.removeImageSlot(slotId);
            } else {
                BlockPos virtualPos = sentSources.get(sourceIndex);
                if (virtualPos != null) terminal.removeSlot(virtualPos);
            }
        }
    }

    /** 将所有源发送到绑定的终端 */
    public void sendAllToTerminal() {
        for (int i = 0; i < sources.size(); i++) {
            sendToTerminal(i);
        }
    }

    public int getSourceCount() {
        return sources.size();
    }

    // ========== 雷达槽位管理 ==========

    public List<RadarSlot> getRadarSlots() { return radarSlots; }

    public void addRadarSlot(int posX, int posY, float scale, float rotation, int alpha, int range) {
        RadarSlot slot = new RadarSlot();
        slot.setPos(posX, posY);
        slot.setScale(scale);
        slot.setRotation(rotation);
        slot.setAlpha(alpha);
        slot.setRadarRange(range);
        radarSlots.add(slot);
        setChanged();
    }

    public void removeRadarSlot(int index) {
        if (index >= 0 && index < radarSlots.size()) {
            radarSlots.remove(index);
            pushRadarSlotsToTerminal(-1); // 同步删除到所有终端
            setChanged();
        }
    }

    public void updateRadarSlot(int index, int posX, int posY, float scale, float rotation, int alpha, int range) {
        if (index >= 0 && index < radarSlots.size()) {
            RadarSlot slot = radarSlots.get(index);
            slot.setPos(posX, posY);
            slot.setScale(scale);
            slot.setRotation(rotation);
            slot.setAlpha(alpha);
            slot.setRadarRange(range);
            setChanged();
        }
    }

    /**
     * 从连接的 Monitor 读取雷达轨迹，同步到所有玩家客户端用于 HUD 实时渲染。
     */
    private void syncRadarTracks() {
        if (level == null || level.isClientSide || linkedMonitorPos == null) return;
        BlockEntity be = level.getBlockEntity(linkedMonitorPos);
        if (be == null) { linkedMonitorPos = null; setChanged(); return; }
        var beClass = be.getClass().getName();
        if (!beClass.equals("com.happysg.radar.block.monitor.MonitorBlockEntity")
            && !beClass.equals("com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity"))
            return;

        try {
            boolean isBearing = beClass.contains("RadarBearing");

            // 获取 tracks
            java.util.Collection<?> tracks;
            if (isBearing) {
                tracks = (java.util.Collection<?>) be.getClass().getMethod("getTracks").invoke(be);
            } else {
                Object controller = be.getClass().getMethod("getController").invoke(be);
                var tracksField = controller.getClass().getDeclaredField("cachedTracks");
                tracksField.setAccessible(true);
                tracks = (java.util.Collection<?>) tracksField.get(controller);
            }

            java.util.List<SyncRadarDataPayload.RadarTrackEntry> entries = new java.util.ArrayList<>();
            if (tracks != null) {
                for (Object track : tracks) {
                    try {
                        var posMethod = track.getClass().getMethod("position");
                        var catMethod = track.getClass().getMethod("trackCategory");
                        var idMethod = track.getClass().getMethod("id");
                        var velMethod = track.getClass().getMethod("velocity");
                        Object pos = posMethod.invoke(track);
                        Object vel = velMethod.invoke(track);
                        Object cat = catMethod.invoke(track);
                        String id = (String) idMethod.invoke(track);
                        double x = (double) pos.getClass().getMethod("x").invoke(pos);
                        double y = (double) pos.getClass().getMethod("y").invoke(pos);
                        double z = (double) pos.getClass().getMethod("z").invoke(pos);
                        double vx = (double) vel.getClass().getMethod("x").invoke(vel);
                        double vy = (double) vel.getClass().getMethod("y").invoke(vel);
                        double vz = (double) vel.getClass().getMethod("z").invoke(vel);
                        int catOrd = ((Enum<?>) cat).ordinal();
                        entries.add(new SyncRadarDataPayload.RadarTrackEntry(
                                id, x, y, z, vx, vy, vz, catOrd,
                                track.getClass().getMethod("entityType").invoke(track).toString()));
                    } catch (Exception ignored) {}
                }
            }

            // 获取雷达角度、范围、位置
            float sweepAngle = 0f;
            float radarRange = 50f;
            double rX = 0, rY = 0, rZ = 0;
            try {
                if (isBearing) {
                    sweepAngle = (float) be.getClass().getMethod("getGlobalAngle").invoke(be);
                    radarRange = (float) be.getClass().getMethod("getRange").invoke(be);
                    Object worldPos = be.getClass().getMethod("getWorldPos").invoke(be);
                    rX = (double) worldPos.getClass().getMethod("getX").invoke(worldPos);
                    rY = (double) worldPos.getClass().getMethod("getY").invoke(worldPos);
                    rZ = (double) worldPos.getClass().getMethod("getZ").invoke(worldPos);
                } else {
                    Object controller = be.getClass().getMethod("getController").invoke(be);
                    var getRadarMethod = controller.getClass().getMethod("getRadar");
                    var radarOpt = getRadarMethod.invoke(controller);
                    if (radarOpt instanceof java.util.Optional<?> opt && opt.isPresent()) {
                        Object radar = opt.get();
                        sweepAngle = (float) radar.getClass().getMethod("getGlobalAngle").invoke(radar);
                        radarRange = (float) radar.getClass().getMethod("getRange").invoke(radar);
                    }
                    Object center = controller.getClass().getMethod("getRadarCenterPos").invoke(controller);
                    if (center != null) {
                        rX = (double) center.getClass().getMethod("x").invoke(center);
                        rY = (double) center.getClass().getMethod("y").invoke(center);
                        rZ = (double) center.getClass().getMethod("z").invoke(center);
                    }
                }
            } catch (Exception ignored) {}

            if (!entries.isEmpty()) {
                var payload = new SyncRadarDataPayload(entries, sweepAngle, radarRange, rX, rY, rZ);
                PacketDistributor.sendToAllPlayers(payload);
                if (level.getGameTime() % 100 == 0)
                    CreateHeadsUpDisplay.LOGGER.info("Radar sync: {} tracks, angle={}, pos=({},{},{})", entries.size(), sweepAngle, (int)rX, (int)rY, (int)rZ);
            }
        } catch (Exception e) {
            if (level.getGameTime() % 100 == 0)
                CreateHeadsUpDisplay.LOGGER.warn("Radar sync failed: {}", e.getMessage());
        }
    }

    /** Display Link 推送数据时添加或更新源，文本变化时自动置顶 */
    public void addOrUpdateDisplayLinkSource(BlockPos sourcePos, String name, String text) {
        for (int i = 0; i < sources.size(); i++) {
            RedstoneSource src = sources.get(i);
            if (src.sourceType == RedstoneSource.Type.DISPLAY_LINK && name.equals(src.name)) {
                if (!text.equals(src.displayLinkText)) {
                    src.displayLinkText = text;
                    if (autoSortEnabled) moveToTop(i);
                }
                setChanged();
                return;
            }
        }
        RedstoneSource src = new RedstoneSource(name, ItemStack.EMPTY, ItemStack.EMPTY);
        src.sourceType = RedstoneSource.Type.DISPLAY_LINK;
        src.displayLinkSourcePos = sourcePos;
        src.displayLinkText = text;
        assignSourceId(src);
        sources.add(0, src);
        // 重映射 sentSources：所有索引 +1
        Map<Integer, BlockPos> remapped = new HashMap<>();
        for (var entry : sentSources.entrySet()) {
            remapped.put(entry.getKey() + 1, entry.getValue());
        }
        sentSources = remapped;
        setChanged();
    }

    /** DataProbe 推送数据时添加或更新源，数值变化时自动置顶 */
    public void addOrUpdateProbeSource(BlockPos probePos, String name, String value) {
        for (int i = 0; i < sources.size(); i++) {
            RedstoneSource src = sources.get(i);
            if (src.sourceType == RedstoneSource.Type.PROBE
                    && probePos.equals(src.probeSourcePos)) {
                if (!value.equals(src.probeValue)) {
                    src.probeValue = value;
                    src.name = name;
                    if (autoSortEnabled) moveToTop(i);
                }
                setChanged();
                // 自动推送到已绑定的终端（已发送过的）
                sendProbeToTerminals(i);
                return;
            }
        }
        RedstoneSource src = RedstoneSource.probe(probePos, name, value);
        assignSourceId(src);
        sources.add(0, src);
        // 重映射 sentSources
        Map<Integer, BlockPos> remapped = new HashMap<>();
        for (var entry : sentSources.entrySet()) {
            remapped.put(entry.getKey() + 1, entry.getValue());
        }
        sentSources = remapped;
        setChanged();
        // 新源自动推送到所有终端
        sendProbeToTerminals(0);
    }

    /** 将 PROBE 源的当前值推送到所有已绑定的终端 */
    private void sendProbeToTerminals(int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= sources.size()) return;
        RedstoneSource src = sources.get(sourceIndex);
        if (src.sourceType != RedstoneSource.Type.PROBE) return;
        String displayText = src.probeValue != null ? src.probeValue : "0";
        String cleanName = src.name.replaceAll("§[0-9a-fk-or]", "");
        BlockPos virtualPos = new BlockPos(src.sourceId, 0, 0);

        for (BlockPos tp : boundTerminals) {
            BlockEntity be = level.getBlockEntity(tp);
            if (be instanceof DisplayTerminalBlockEntity terminal) {
                if (terminal.getSlotBySourceName(cleanName) == null) {
                    terminal.updateSlotConfig(virtualPos,
                            10, 50 + sentSources.size() * 20,
                            1.0f, 0f, 0xFFFFFF, 255);
                }
                terminal.updateSlotData(virtualPos, displayText);
                terminal.updateSlotSourceName(virtualPos, cleanName);
            } else if (be instanceof DisplayTerminalProBlockEntity terminalPro) {
                terminalPro.getSourceCache().put(virtualPos, displayText);
                terminalPro.getSourceNameCache().put(virtualPos, cleanName);
                if (terminalPro.findSlot(virtualPos) != null) {
                    terminalPro.updateSlotDataAndStyle(virtualPos, displayText, 0, cleanName);
                }
                terminalPro.setChanged();
                terminalPro.syncToBoundPlayers();
            }
        }
        sentSources.put(sourceIndex, virtualPos);
        setChanged();
    }

    /** 移除指定 DataProbe 创建的所有源（Probe 被拆除时调用） */
    public void removeProbeSource(BlockPos probePos) {
        var toRemove = new ArrayList<Integer>();
        for (int i = 0; i < sources.size(); i++) {
            RedstoneSource src = sources.get(i);
            if (src.sourceType == RedstoneSource.Type.PROBE && probePos.equals(src.probeSourcePos)) {
                toRemove.add(i);
            }
        }
        // 从后往前删，避免索引错位
        for (int j = toRemove.size() - 1; j >= 0; j--) {
            removeRedstoneSource(toRemove.get(j));
        }
    }

    /** 供 RequestSourcesDataPayload 读取源数据，返回不可变快照 */
    public SourceSnapshot getSource(int index) {
        RedstoneSource src = sources.get(index);
        int strength = getCurrentStrength(src);
        // DisplayLink/Probe/Physics 源：用实际文本而非强度值；红石源：用转译后的文本
        String display;
        if (src.sourceType == RedstoneSource.Type.DISPLAY_LINK) {
            display = src.displayLinkText != null ? src.displayLinkText : "0";
        } else if (src.sourceType == RedstoneSource.Type.PROBE || src.sourceType == RedstoneSource.Type.PHYSICS) {
            display = src.probeValue != null ? src.probeValue : "0";
        } else {
            display = getDisplayText(src);
        }
        BlockPos sourcePos = src.sourceType == RedstoneSource.Type.PROBE
                ? src.probeSourcePos : src.displayLinkSourcePos;
        return new SourceSnapshot(src.name, src.frequencyItem1, src.frequencyItem2,
                strength, display, sourcePos,
                src.sourceType.name(), src.imageId, src.imageFileName, src.imageData,
                src.systemSource);
    }

    public record SourceSnapshot(String name, net.minecraft.world.item.ItemStack item1,
                                  net.minecraft.world.item.ItemStack item2, int strength,
                                  String displayText, BlockPos displayLinkSourcePos,
                                  String sourceType, UUID imageId,
                                  String imageFileName, byte[] imageData,
                                  boolean systemSource) {}

    /** @param terminalIndex -1 = 所有终端，>=0 = 指定索引 */
    public void sendToTerminal(int sourceIndex, int terminalIndex) {
        if (sourceIndex < 0 || sourceIndex >= sources.size()) return;
        RedstoneSource src = sources.get(sourceIndex);

        var targets = new ArrayList<BlockPos>();
        if (terminalIndex < 0) {
            targets.addAll(boundTerminals);
        } else if (terminalIndex < boundTerminals.size()) {
            targets.add(boundTerminals.get(terminalIndex));
        } else {
            return;
        }

        // IMAGE 类型：直接发送图片到终端图片槽位
        if (src.sourceType == RedstoneSource.Type.IMAGE) {
            for (BlockPos tp : targets) {
                BlockEntity be = level.getBlockEntity(tp);
                if (be instanceof DisplayTerminalBlockEntity terminal) {
                    terminal.addImageSlot(src.imageId, src.imageFileName, src.imageData);
                } else if (be instanceof DisplayTerminalProBlockEntity terminal) {
                    terminal.cacheImageSource(src.imageId, src.imageFileName, src.imageData);
                }
            }
            sentSources.put(sourceIndex, new BlockPos(src.sourceId, 0, 0));
            setChanged();
            return;
        }

        // REDSTONE + IMAGE_CONDITIONAL：条件匹配后推送图片槽位
        if (src.sourceType == RedstoneSource.Type.REDSTONE && src.translation != null
                && src.translation.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
            String imgName = src.translation.getSelectedImage(getCurrentStrength(src));
            if (imgName != null && !imgName.isEmpty()) {
                RedstoneSource imgSrc = findImageSourceByName(imgName);
                if (imgSrc != null) {
                    UUID slotId = UUID.nameUUIDFromBytes(("img_cond_" + src.name).getBytes());
                    for (BlockPos tp : targets) {
                        BlockEntity be = level.getBlockEntity(tp);
                        if (be instanceof DisplayTerminalBlockEntity terminal) {
                            terminal.updateImageData(slotId, imgSrc.imageFileName, imgSrc.imageData);
                        } else if (be instanceof DisplayTerminalProBlockEntity terminal) {
                            terminal.cacheImageSource(slotId, imgSrc.imageFileName, imgSrc.imageData);
                        }
                    }
                    sentSources.put(sourceIndex, new BlockPos(src.sourceId, 0, 0));
                    setChanged();
                    return;
                }
            }
            // 没有选中图片 → 缓存透明图片
            UUID slotId = UUID.nameUUIDFromBytes(("img_cond_" + src.name).getBytes());
            for (BlockPos tp : targets) {
                BlockEntity be = level.getBlockEntity(tp);
                if (be instanceof DisplayTerminalBlockEntity terminal) {
                    terminal.updateImageData(slotId, "", BLANK_PNG);
                } else if (be instanceof DisplayTerminalProBlockEntity terminal) {
                    terminal.cacheImageSource(slotId, "", BLANK_PNG);
                }
            }
            sentSources.put(sourceIndex, new BlockPos(0, slotId.hashCode(), 0));
            setChanged();
            return;
        }

        BlockPos virtualPos = new BlockPos(src.sourceId, 0, 0); // 稳定的数字编码

        String displayText;
        if (src.sourceType == RedstoneSource.Type.PROBE || src.sourceType == RedstoneSource.Type.PHYSICS) {
            displayText = src.probeValue != null ? src.probeValue : "0";
        } else {
            displayText = src.displayLinkText != null ? src.displayLinkText : getDisplayText(src);
        }
        String cleanName = src.name.replaceAll("§[0-9a-fk-or]", "");

        for (BlockPos tp : targets) {
            BlockEntity be = level.getBlockEntity(tp);
            if (be instanceof DisplayTerminalBlockEntity terminal) {
                sendToTerminalInternal(terminal, virtualPos, cleanName, displayText);
            } else if (be instanceof DisplayTerminalProBlockEntity terminalPro) {
                sendToTerminalProInternal(terminalPro, virtualPos, cleanName, displayText);
            }
        }
        sentSources.put(sourceIndex, virtualPos);
        setChanged();
    }

    private void sendToTerminalInternal(DisplayTerminalBlockEntity terminal, BlockPos virtualPos, String cleanName, String displayText) {
        if (terminal.getSlotBySourceName(cleanName) == null) {
            terminal.updateSlotConfig(virtualPos,
                    10, 50 + sentSources.size() * 20,
                    1.0f, 0f, 0xFFFFFF, 255);
        }
        terminal.updateSlotData(virtualPos, displayText);
        terminal.updateSlotSourceName(virtualPos, cleanName);
    }

    private void sendToTerminalProInternal(DisplayTerminalProBlockEntity terminal, BlockPos virtualPos, String cleanName, String displayText) {
        // 存入数据源缓存（不入画布）
        terminal.getSourceCache().put(virtualPos, displayText);
        terminal.getSourceNameCache().put(virtualPos, cleanName);
        // 如果画布上已有槽位，同步更新数据
        if (terminal.findSlot(virtualPos) != null) {
            terminal.updateSlotDataAndStyle(virtualPos, displayText, 0, cleanName);
        }
        terminal.setChanged();
        terminal.syncToBoundPlayers();
    }

    /** 发送到所有终端（兼容旧调用） */
    public void sendToTerminal(int sourceIndex) {
        sendToTerminal(sourceIndex, -1);
    }

    public void setTranslation(int index, TranslationConfig tc) {
        if (index >= 0 && index < sources.size()) {
            sources.get(index).translation = tc;
            setChanged();
        }
    }

    public TranslationConfig getTranslation(int index) {
        if (index >= 0 && index < sources.size()) return sources.get(index).translation;
        return null;
    }

    private int getCurrentStrength(RedstoneSource src) {
        if (src.sourceType == RedstoneSource.Type.PROBE || src.sourceType == RedstoneSource.Type.PHYSICS) return 0;
        if (src.displayLinkSourcePos != null) return 0;
        return RedstoneSignalHelper.getSignalStrength(level, src.frequencyItem1, src.frequencyItem2);
    }

    /** 获取源的显示文本（应用转译后），入口方法 */
    private String getDisplayText(RedstoneSource src) {
        return getDisplayText(src, new HashSet<>());
    }

    /** 获取源的显示文本，携带已访问集合防止循环引用 */
    private String getDisplayText(RedstoneSource src, Set<String> visited) {
        int raw = getCurrentStrength(src);
        visited.add(src.name);
        if (src.translation != null && src.translation.getMode() != TranslationConfig.Mode.NONE) {
            if (src.translation.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
                return "[IMAGE]";
            }
            String t = src.translation.getDisplay(raw);
            if (t != null) {
                if (TranslationConfig.hasReferences(t)) {
                    t = TranslationConfig.resolveReferences(t, refName -> resolveRefValue(refName, visited));
                }
                return t;
            }
        }
        return String.valueOf(raw);
    }

    /**
     * 按名称解析引用：在 sources 列表中查找同名源，返回其显示值。
     * RADAR 返回 "[RADAR]"，循环引用返回原始值，名称不存在保留 \${name} 原样。
     */
    private String resolveRefValue(String name, Set<String> visited) {
        for (RedstoneSource s : sources) {
            if (name.equals(s.name)) {
                if (s.sourceType == RedstoneSource.Type.RADAR) return "[RADAR]";
                if (visited.contains(name)) {
                    return String.valueOf(getCurrentStrength(s));
                }
                return switch (s.sourceType) {
                    case REDSTONE -> getDisplayText(s, new HashSet<>(visited));
                    case DISPLAY_LINK -> s.displayLinkText != null ? s.displayLinkText : "0";
                    case IMAGE -> s.imageFileName != null ? s.imageFileName : "[IMAGE]";
                    default -> String.valueOf(getCurrentStrength(s));
                };
            }
        }
        return "${" + name + "}";
    }

    /** 按名称查找 IMAGE 类型源 */
    private RedstoneSource findImageSourceByName(String name) {
        for (RedstoneSource s : sources) {
            if (s.sourceType == RedstoneSource.Type.IMAGE && name.equals(s.name)) {
                return s;
            }
        }
        return null;
    }

    // 每 tick 执行，实时更新已发送槽位的数据；强度变化时自动置顶
    public static void tick(Level level, BlockPos pos, BlockState state, OmniCoreBlockEntity be) {
        if (level.isClientSide) return;

        // 雷达数据同步（每 tick，连接了 Monitor 时）
        if (be.linkedMonitorPos != null) {
            be.syncRadarTracks();
        }

        // 插件槽 DataProbe 轮询
        be.pollProbesFromPlugins();

        // 第一步：检测所有源的变化，变化时置顶（不需要绑定终端）
        for (int idx = 0; idx < be.sources.size(); idx++) {
            RedstoneSource src = be.sources.get(idx);
            boolean changed = false;
            if (src.sourceType == RedstoneSource.Type.REDSTONE) {
                int strength = RedstoneSignalHelper.getSignalStrength(level, src.frequencyItem1, src.frequencyItem2);
                if (be.autoSortEnabled && strength != src.lastStrength) {
                    src.lastStrength = strength;
                    changed = true;
                }
            } else if (src.sourceType == RedstoneSource.Type.DISPLAY_LINK) {
                // DisplayLink 源在 addOrUpdateDisplayLinkSource 中已处理置顶
            }
            if (changed) {
                be.moveToTop(idx);
                break;
            }
        }

        // 第二步：更新所有已绑定终端上已发送的槽位数据
        if (be.boundTerminals.isEmpty()) return;

        var toRemove = new ArrayList<Integer>();
        var sentIndices = new java.util.ArrayList<>(be.sentSources.keySet());
        for (int idx : sentIndices) {
            BlockPos virtualPos = be.sentSources.get(idx);
            if (virtualPos == null || idx < 0 || idx >= be.sources.size()) {
                toRemove.add(idx);
                continue;
            }
            RedstoneSource src = be.sources.get(idx);
            // IMAGE_CONDITIONAL 源：每 tick 确保图片槽位数据正确，图片名变化时同步 HMD
            if (src.sourceType == RedstoneSource.Type.REDSTONE && src.translation != null
                    && src.translation.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
                String imgName = src.translation.getSelectedImage(be.getCurrentStrength(src));
                boolean changed = !java.util.Objects.equals(imgName, src.lastImageName);
                if (changed) src.lastImageName = imgName;
                for (BlockPos tp : be.boundTerminals) {
                    BlockEntity terminalBe = level.getBlockEntity(tp);
                    if (terminalBe instanceof DisplayTerminalBlockEntity terminal) {
                        UUID slotId = UUID.nameUUIDFromBytes(("img_cond_" + src.name).getBytes());
                        if (imgName != null && !imgName.isEmpty()) {
                            RedstoneSource imgSrc = be.findImageSourceByName(imgName);
                            if (imgSrc != null) {
                                var existing = terminal.getImageSlot(slotId);
                                if (existing != null) {
                                    existing.setFileName(imgSrc.imageFileName);
                                    existing.setImageData(imgSrc.imageData);
                                    terminal.setChanged();
                                } else {
                                    be.sentSources.remove(idx);
                                    be.setChanged();
                                }
                                if (changed) terminal.syncToBoundPlayers();
                            }
                        } else if (changed) {
                            terminal.updateImageData(slotId, "", BLANK_PNG);
                        }
                    } else if (terminalBe instanceof DisplayTerminalProBlockEntity terminal) {
                        UUID slotId = UUID.nameUUIDFromBytes(("img_cond_" + src.name).getBytes());
                        if (imgName != null && !imgName.isEmpty()) {
                            RedstoneSource imgSrc = be.findImageSourceByName(imgName);
                            if (imgSrc != null) {
                                terminal.cacheImageSource(slotId, imgSrc.imageFileName, imgSrc.imageData);
                                if (changed) terminal.syncToBoundPlayers();
                            }
                        } else if (changed) {
                            terminal.cacheImageSource(slotId, "", BLANK_PNG);
                        }
                    }
                }
                continue;
            }
            String newText = null;
            if (src.sourceType == RedstoneSource.Type.DISPLAY_LINK) {
                newText = src.displayLinkText;
            } else if (src.sourceType == RedstoneSource.Type.PROBE || src.sourceType == RedstoneSource.Type.PHYSICS) {
                newText = src.probeValue;
            } else if (src.sourceType == RedstoneSource.Type.REDSTONE) {
                newText = be.getDisplayText(src);
            }
            if (newText == null) continue;

            boolean foundAny = false;
            for (BlockPos tp : be.boundTerminals) {
                BlockEntity terminalBe = level.getBlockEntity(tp);
                if (terminalBe instanceof DisplayTerminalBlockEntity terminal && terminal.getSlot(virtualPos) != null) {
                    foundAny = true;
                    if (!newText.equals(src.lastSentText)) {
                        terminal.updateSlotData(virtualPos, newText);
                    }
                } else if (terminalBe instanceof DisplayTerminalProBlockEntity terminalPro &&
                        (terminalPro.findSlot(virtualPos) != null || terminalPro.getSourceCache().containsKey(virtualPos))) {
                    foundAny = true;
                    terminalPro.getSourceCache().put(virtualPos, newText);
                    terminalPro.setChanged();
                    if (terminalPro.findSlot(virtualPos) != null && !newText.equals(src.lastSentText)) {
                        terminalPro.updateSlotData(virtualPos, newText);
                    }
                }
            }
            if (foundAny) {
                src.lastSentText = newText;
            } else {
                toRemove.add(idx);
            }
        }
        for (int idx : toRemove) be.sentSources.remove(idx);
    }

    /** 将指定索引的源移到列表最顶部，同时重映射 sentSources */
    private void moveToTop(int index) {
        if (index <= 0 || index >= sources.size()) return;
        RedstoneSource moved = sources.remove(index);
        sources.add(0, moved);

        // 重映射 sentSources：index 之前的条目 +1，index 设为 0
        Map<Integer, BlockPos> remapped = new HashMap<>();
        for (var entry : sentSources.entrySet()) {
            int oldIdx = entry.getKey();
            if (oldIdx == index) {
                remapped.put(0, entry.getValue());
            } else if (oldIdx < index) {
                remapped.put(oldIdx + 1, entry.getValue());
            } else {
                remapped.put(oldIdx, entry.getValue());
            }
        }
        sentSources = remapped;
        setChanged();
    }

    // 供客户端请求实时强度时调用（返回所有源的当前强度列表）
    public List<Integer> getAllCurrentStrengths() {
        List<Integer> strengths = new ArrayList<>();
        for (RedstoneSource src : sources) {
            strengths.add(getCurrentStrength(src));
        }
        return strengths;
    }

    // 数据持久化
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag boundTag = new ListTag();
        for (BlockPos bp : boundTerminals) {
            CompoundTag bt = new CompoundTag();
            bt.putLong("Pos", bp.asLong());
            String name = terminalNames.get(bp);
            if (name != null && !name.isEmpty()) bt.putString("Name", name);
            boundTag.add(bt);
        }
        tag.put("BoundTerminals", boundTag);
        ListTag sourcesTag = new ListTag();
        for (RedstoneSource src : sources) {
            CompoundTag srcTag = new CompoundTag();
            srcTag.putString("type", src.sourceType.name());
            srcTag.putBoolean("system", src.systemSource);
            srcTag.putInt("sourceId", src.sourceId);
            srcTag.putString("name", src.name);
            srcTag.put("freqItem1", src.frequencyItem1.saveOptional(registries));
            srcTag.put("freqItem2", src.frequencyItem2.saveOptional(registries));
            if (src.displayLinkSourcePos != null) {
                srcTag.putLong("dlSourcePos", src.displayLinkSourcePos.asLong());
            }
            if (src.displayLinkText != null) {
                srcTag.putString("dlText", src.displayLinkText);
            }
            if (src.translation != null && src.translation.getMode() != TranslationConfig.Mode.NONE) {
                srcTag.put("translation", src.translation.serialize());
            }
            if (src.sourceType == RedstoneSource.Type.IMAGE) {
                srcTag.putUUID("ImageId", src.imageId);
                srcTag.putString("ImageFileName", src.imageFileName);
                srcTag.putByteArray("ImageData", src.imageData);
            }
            if (src.sourceType == RedstoneSource.Type.PROBE) {
                if (src.probeSourcePos != null) srcTag.putLong("ProbePos", src.probeSourcePos.asLong());
                if (src.probeValue != null) srcTag.putString("ProbeValue", src.probeValue);
            }
            sourcesTag.add(srcTag);
        }
        tag.put("Sources", sourcesTag);

        ListTag sentTag = new ListTag();
        for (var entry : sentSources.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("idx", entry.getKey());
            entryTag.putLong("pos", entry.getValue().asLong());
            sentTag.add(entryTag);
        }
        tag.put("SentSources", sentTag);

        // 雷达槽位序列化
        ListTag radarSlotTag = new ListTag();
        for (RadarSlot slot : radarSlots) {
            radarSlotTag.add(slot.serialize());
        }
        tag.put("RadarSlots", radarSlotTag);

        // 雷达轮询位置
        if (linkedMonitorPos != null) {
            tag.putLong("LinkedMonitorPos", linkedMonitorPos.asLong());
        }
        // 插件槽位
        ListTag pluginTag = new ListTag();
        for (int i = 0; i < 27; i++) {
            pluginTag.add(pluginInventory.getItem(i).saveOptional(registries));
        }
        tag.put("Plugins", pluginTag);
        tag.putInt("NextSourceId", nextSourceId);
        tag.putBoolean("AutoSort", autoSortEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        boundTerminals.clear();
        if (tag.contains("BoundTerminal")) {
            // 兼容旧格式
            boundTerminals.add(BlockPos.of(tag.getLong("BoundTerminal")));
        }
        terminalNames.clear();
        if (tag.contains("BoundTerminals")) {
            ListTag boundTag = tag.getList("BoundTerminals", Tag.TAG_COMPOUND);
            for (int i = 0; i < boundTag.size(); i++) {
                CompoundTag bt = boundTag.getCompound(i);
                BlockPos bp = BlockPos.of(bt.getLong("Pos"));
                boundTerminals.add(bp);
                if (bt.contains("Name")) terminalNames.put(bp, bt.getString("Name"));
            }
        }
        sources.clear();
        ListTag sourcesTag = tag.getList("Sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < sourcesTag.size(); i++) {
            CompoundTag srcTag = sourcesTag.getCompound(i);
            String name = srcTag.getString("name");
            ItemStack freqItem1 = ItemStack.parseOptional(registries, srcTag.getCompound("freqItem1"));
            ItemStack freqItem2 = ItemStack.parseOptional(registries, srcTag.getCompound("freqItem2"));
            RedstoneSource src = new RedstoneSource(name, freqItem1, freqItem2);
            src.sourceId = srcTag.getInt("sourceId"); // 还原固定编码，0 表示旧数据
            if (srcTag.contains("type")) {
                try { src.sourceType = RedstoneSource.Type.valueOf(srcTag.getString("type")); }
                catch (IllegalArgumentException e) { src.sourceType = RedstoneSource.Type.REDSTONE; }
            }
            src.systemSource = srcTag.getBoolean("system");
            if (srcTag.contains("dlSourcePos")) {
                src.displayLinkSourcePos = BlockPos.of(srcTag.getLong("dlSourcePos"));
            }
            if (srcTag.contains("dlText")) {
                src.displayLinkText = srcTag.getString("dlText");
            }
            if (srcTag.contains("translation")) {
                src.translation = TranslationConfig.deserialize(srcTag.getCompound("translation"));
            }
            if (src.sourceType == RedstoneSource.Type.IMAGE) {
                src.imageId = srcTag.getUUID("ImageId");
                src.imageFileName = srcTag.getString("ImageFileName");
                src.imageData = srcTag.getByteArray("ImageData");
            }
            if (src.sourceType == RedstoneSource.Type.PROBE) {
                if (srcTag.contains("ProbePos")) src.probeSourcePos = BlockPos.of(srcTag.getLong("ProbePos"));
                src.probeValue = srcTag.getString("ProbeValue");
            }
            sources.add(src);
        }
        sentSources.clear();
        ListTag sentTag = tag.getList("SentSources", Tag.TAG_COMPOUND);
        for (int i = 0; i < sentTag.size(); i++) {
            CompoundTag entryTag = sentTag.getCompound(i);
            int idx = entryTag.getInt("idx");
            BlockPos pos = BlockPos.of(entryTag.getLong("pos"));
            sentSources.put(idx, pos);
        }

        // 雷达槽位反序列化
        radarSlots.clear();
        if (tag.contains("RadarSlots")) {
            ListTag radarSlotTag = tag.getList("RadarSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < radarSlotTag.size(); i++) {
                radarSlots.add(RadarSlot.deserialize(radarSlotTag.getCompound(i)));
            }
        }
        if (tag.contains("LinkedMonitorPos")) {
            linkedMonitorPos = BlockPos.of(tag.getLong("LinkedMonitorPos"));
        } else {
            linkedMonitorPos = null;
        }
        // 插件槽位
        if (tag.contains("Plugins")) {
            ListTag pluginTag = tag.getList("Plugins", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(pluginTag.size(), 27); i++) {
                pluginInventory.setItem(i, ItemStack.parseOptional(registries, pluginTag.getCompound(i)));
            }
        }
        nextSourceId = tag.getInt("NextSourceId");
        if (nextSourceId == 0) nextSourceId = 1;
        autoSortEnabled = !tag.contains("AutoSort") || tag.getBoolean("AutoSort");
        // 给旧数据（没有 sourceId）的源补上编码
        for (RedstoneSource src : sources) {
            if (src.sourceId == 0) assignSourceId(src);
        }
    }

    private static class RedstoneSource {
        enum Type { REDSTONE, DISPLAY_LINK, IMAGE, RADAR, PROBE, PHYSICS }

        Type sourceType = Type.REDSTONE;
        String name;
        boolean systemSource; // 系统自动生成，不可删除
        ItemStack frequencyItem1;
        ItemStack frequencyItem2;
        int lastStrength = -1;
        String lastSentText;
        String lastImageName; // IMAGE_CONDITIONAL 模式：上次选中的图片源名称
        BlockPos displayLinkSourcePos;
        String displayLinkText;
        TranslationConfig translation;

        // IMAGE 类型专用字段
        UUID imageId;
        String imageFileName;
        byte[] imageData;

        // PROBE 类型专用字段
        BlockPos probeSourcePos;
        String probeValue;

        int sourceId; // 固定的数字编码，不随名称变化

        RedstoneSource(String name, ItemStack item1, ItemStack item2) {
            this.name = name;
            this.frequencyItem1 = item1;
            this.frequencyItem2 = item2;
        }

        /** 创建一个 IMAGE 类型的源 */
        static RedstoneSource image(UUID imageId, String fileName, byte[] data) {
            RedstoneSource src = new RedstoneSource(fileName, ItemStack.EMPTY, ItemStack.EMPTY);
            src.sourceType = Type.IMAGE;
            src.imageId = imageId;
            src.imageFileName = fileName;
            src.imageData = data;
            return src;
        }

        /** 创建一个 PROBE 类型的源 */
        static RedstoneSource probe(BlockPos probePos, String name, String value) {
            RedstoneSource src = new RedstoneSource(name, ItemStack.EMPTY, ItemStack.EMPTY);
            src.sourceType = Type.PROBE;
            src.probeSourcePos = probePos;
            src.probeValue = value;
            return src;
        }
    }
}