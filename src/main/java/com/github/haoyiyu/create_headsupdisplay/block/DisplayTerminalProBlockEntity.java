package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.config.DisplaySlot;
import com.github.haoyiyu.create_headsupdisplay.config.ImageSlot;
import com.github.haoyiyu.create_headsupdisplay.config.ProLayer;
import com.github.haoyiyu.create_headsupdisplay.config.RadarSlot;
import com.github.haoyiyu.create_headsupdisplay.config.StaticTextSlot;
import com.github.haoyiyu.create_headsupdisplay.network.OpenTerminalProConfigScreenPayload;
import com.github.haoyiyu.create_headsupdisplay.network.SyncDisplayDataPayload;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class DisplayTerminalProBlockEntity extends BlockEntity {
    // 图层系统
    private final List<ProLayer> layers = new ArrayList<>();
    // 静态文本槽位（所有图层，含 layerIndex）
    private final List<StaticTextSlot> staticTextSlots = new ArrayList<>();
    private final List<Integer> staticTextLayer = new ArrayList<>();
    // 图片槽位（列表支持同源多份）
    private final List<ImageSlot> imageSlots = new ArrayList<>();
    private final List<Integer> imageLayer = new ArrayList<>();
    private final List<Integer> imageSlotIds = new ArrayList<>();
    // 雷达图槽位
    private final List<RadarSlot> radarSlots = new ArrayList<>();
    private final List<Integer> radarLayer = new ArrayList<>();
    private final List<Integer> radarSlotIds = new ArrayList<>();

    // 数据源缓存：存储名称和最新值，不依赖画布槽位
    private final Map<BlockPos, String> sourceCache = new LinkedHashMap<>();
    private final Map<BlockPos, String> sourceNameCache = new HashMap<>();

    public DisplayTerminalProBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_TERMINAL_PRO_BE.get(), pos, state);
        if (layers.isEmpty()) layers.add(new ProLayer("Main"));
    }

    public Map<BlockPos, String> getSourceCache() { return sourceCache; }
    public Map<BlockPos, String> getSourceNameCache() { return sourceNameCache; }

    // 图片源缓存（不入画布，仅坞卡片）
    private final Map<UUID, byte[]> imageSourceCache = new LinkedHashMap<>();
    private final Map<UUID, String> imageSourceNameCache = new HashMap<>();
    public void cacheImageSource(UUID id, String name, byte[] data) {
        imageSourceCache.put(id, data); imageSourceNameCache.put(id, name); setChanged(); syncToBoundPlayers();
    }
    public void removeImageSource(UUID id) {
        imageSourceCache.remove(id); imageSourceNameCache.remove(id); setChanged(); syncToBoundPlayers();
    }
    public Map<UUID, byte[]> getImageSourceCache() { return imageSourceCache; }
    public Map<UUID, String> getImageSourceNameCache() { return imageSourceNameCache; }

    // ========== 图层管理 ==========
    public List<ProLayer> getLayers() { return layers; }
    public ProLayer getLayer(int index) {
        return index >= 0 && index < layers.size() ? layers.get(index) : layers.getFirst();
    }
    public int getLayerCount() { return layers.size(); }
    public void addLayer(String name) { layers.add(new ProLayer(name)); setChanged(); }
    public void removeLayer(int index) {
        if (index > 0 && index < layers.size()) {
            // 将该图层的数据源槽位移到主图层
            ProLayer removed = layers.remove(index);
            ProLayer main = layers.getFirst();
            for (DisplaySlot s : removed.getSlots()) main.addSlot(s);
            setChanged();
        }
    }

    // ========== 数据源槽位（存储在图层中） ==========
    public DisplaySlot getOrCreateSlot(BlockPos sourcePos, int layerIndex) {
        ProLayer layer = getLayer(layerIndex);
        for (DisplaySlot s : layer.getSlots()) {
            if (s.getSourcePos().equals(sourcePos)) return s;
        }
        DisplaySlot slot = new DisplaySlot(sourcePos);
        layer.addSlot(slot);
        return slot;
    }

    /** 总是创建新槽位（允许同源多份） */
    public DisplaySlot createNewSlot(BlockPos sourcePos, int layerIndex) {
        ProLayer layer = getLayer(layerIndex);
        DisplaySlot slot = new DisplaySlot(sourcePos);
        layer.addSlot(slot);
        setChanged();
        return slot;
    }

    public DisplaySlot findSlot(BlockPos sourcePos) {
        for (ProLayer layer : layers) {
            for (DisplaySlot s : layer.getSlots()) {
                if (s.getSourcePos().equals(sourcePos)) return s;
            }
        }
        return null;
    }

    public int findSlotLayer(BlockPos sourcePos) {
        for (int i = 0; i < layers.size(); i++) {
            for (DisplaySlot s : layers.get(i).getSlots()) {
                if (s.getSourcePos().equals(sourcePos)) return i;
            }
        }
        return 0;
    }

    public void updateSlotData(BlockPos sourcePos, String text) {
        updateSlotData(sourcePos, text, true);
    }

    public void updateSlotData(BlockPos sourcePos, String text, boolean sync) {
        for (ProLayer layer : layers)
            for (DisplaySlot s : layer.getSlots())
                if (s.getSourcePos().equals(sourcePos)) s.setLastData(text);
        // 不自动创建 —— 只有玩家拖放才创建新槽位
        setChanged();
        if (sync) syncToBoundPlayers();
    }

    private int nextSlotId = 1;

    /** 旧兼容：通过 sourcePos 找第一个槽位更新 */
    public void updateSlotConfig(BlockPos sourcePos, int posX, int posY, float scale, float rotation, int color, int alpha) {
        DisplaySlot slot = findSlot(sourcePos);
        if (slot == null) { slot = new DisplaySlot(sourcePos); slot.setSlotId(nextSlotId++); getLayer(0).addSlot(slot); }
        slot.setPos(posX, posY); slot.setScale(scale); slot.setRotation(rotation);
        slot.setColor(color); slot.setAlpha(alpha);
        setChanged(); syncToBoundPlayers();
    }

    /** 通过slotId更新已有槽位，找不到则创建新槽位 */
    public void updateSlotConfigById(int slotId, BlockPos sourcePos, int posX, int posY, float scale, float rotation, int color, int alpha) {
        DisplaySlot slot = slotId > 0 ? findSlotById(slotId) : null;
        if (slot == null) {
            slot = new DisplaySlot(sourcePos);
            if (slotId > 0) { slot.setSlotId(slotId); nextSlotId = Math.max(nextSlotId, slotId + 1); }
            else slot.setSlotId(nextSlotId++);
            getLayer(0).addSlot(slot);
            // 新建时从缓存填充最新数据
            String cached = sourceCache.get(sourcePos);
            if (cached != null) { slot.setLastData(cached); slot.setSourceName(sourceNameCache.getOrDefault(sourcePos, "")); }
        }
        slot.setPos(posX, posY); slot.setScale(scale); slot.setRotation(rotation);
        slot.setColor(color); slot.setAlpha(alpha);
        setChanged(); syncToBoundPlayers();
    }

    public DisplaySlot findSlotById(int id) {
        for (ProLayer layer : layers)
            for (DisplaySlot s : layer.getSlots())
                if (s.getSlotId() == id) return s;
        return null;
    }

    public void updateSlotDataAndStyle(BlockPos sourcePos, String text, int line) {
        updateSlotDataAndStyle(sourcePos, text, line, null);
    }

    public void updateSlotDataAndStyle(BlockPos sourcePos, String text, int line, String sourceName) {
        for (ProLayer layer : layers)
            for (DisplaySlot s : layer.getSlots())
                if (s.getSourcePos().equals(sourcePos)) {
                    s.setLastData(text); s.setDisplayLine(line);
                    if (sourceName != null) s.setSourceName(sourceName);
                }
        setChanged(); syncToBoundPlayers();
    }

    public void removeSlot(BlockPos sourcePos) {
        for (ProLayer layer : layers)
            layer.getSlots().removeIf(s -> s.getSourcePos().equals(sourcePos));
        // 不删 sourceCache —— 卡片只在坞 X 时删除
        setChanged();
        syncToBoundPlayers();
    }

    /** 坞 X：删除卡片、画布槽位和缓存 */
    public void removeSourceCard(BlockPos sourcePos) {
        for (ProLayer layer : layers)
            layer.getSlots().removeIf(s -> s.getSourcePos().equals(sourcePos));
        sourceCache.remove(sourcePos);
        sourceNameCache.remove(sourcePos);
        setChanged();
        syncToBoundPlayers();
    }

    /** Create a new slot at a specific position on a specific layer (dragged from dock) */
    public DisplaySlot createSlotOnLayer(BlockPos sourcePos, int layerIndex, int posX, int posY) {
        ProLayer layer = getLayer(layerIndex);
        DisplaySlot slot = new DisplaySlot(sourcePos);
        slot.setPos(posX, posY);
        layer.addSlot(slot);
        setChanged();
        syncToBoundPlayers();
        return slot;
    }

    // ========== 静态文本槽位 ==========
    public void addStaticTextSlot(String text, int posX, int posY, float scale, float rotation, int color, int alpha, int layerIdx) {
        staticTextSlots.add(new StaticTextSlot(text, posX, posY, scale, rotation, color, alpha));
        staticTextLayer.add(layerIdx);
        setChanged(); syncToBoundPlayers();
    }
    public void removeStaticTextSlot(int index) {
        if (index >= 0 && index < staticTextSlots.size()) {
            staticTextSlots.remove(index);
            staticTextLayer.remove(index);
            setChanged(); syncToBoundPlayers();
        }
    }
    public void updateStaticTextSlot(int index, String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
        if (index >= 0 && index < staticTextSlots.size()) {
            StaticTextSlot slot = staticTextSlots.get(index);
            slot.setText(text); slot.setPos(posX, posY);
            slot.setScale(scale); slot.setRotation(rotation);
            slot.setColor(color); slot.setAlpha(alpha);
            setChanged(); syncToBoundPlayers();
        }
    }
    public List<StaticTextSlot> getStaticTextSlots() { return staticTextSlots; }
    public int getStaticTextLayer(int i) { return i >= 0 && i < staticTextLayer.size() ? staticTextLayer.get(i) : 0; }
    public void setStaticTextLayer(int i, int l) { if (i >= 0 && i < staticTextLayer.size()) staticTextLayer.set(i, l); }

    public void handleStaticTextUpdate(int index, String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
        if (index == -1) addStaticTextSlot(text, posX, posY, scale, rotation, color, alpha, 0);
        else updateStaticTextSlot(index, text, posX, posY, scale, rotation, color, alpha);
    }

    // ========== 图片槽位（列表，支持同源多份） ==========
    private int nextImgSlotId = 1;
    public void addImageSlot(UUID imageId, String fileName, byte[] imageData, int layerIdx) {
        if (imageData == null || imageData.length > com.github.haoyiyu.create_headsupdisplay.config.ModConfig.IMAGE_MAX_SIZE_KB.get() * 1024) return;
        imageSlots.add(new ImageSlot(imageId, fileName, imageData));
        imageLayer.add(layerIdx);
        imageSlotIds.add(nextImgSlotId++);
        setChanged(); syncToBoundPlayers();
    }
    public void removeImageSlot(UUID imageId) {
        for (int i = imageSlots.size() - 1; i >= 0; i--)
            if (imageSlots.get(i).getImageId().equals(imageId)) { imageSlots.remove(i); imageLayer.remove(i); imageSlotIds.remove(i); break; }
        imageSourceCache.remove(imageId); imageSourceNameCache.remove(imageId);
        setChanged(); syncToBoundPlayers();
    }
    public void updateImageData(UUID imageId, String fileName, byte[] imageData) {
        if (imageData == null || imageData.length > com.github.haoyiyu.create_headsupdisplay.config.ModConfig.IMAGE_MAX_SIZE_KB.get() * 1024) return;
        for (ImageSlot s : imageSlots) if (s.getImageId().equals(imageId)) { s.setFileName(fileName); s.setImageData(imageData); }
        setChanged(); syncToBoundPlayers();
    }
    public void updateImageConfig(UUID imageId, int posX, int posY, float scale, float rotation, int alpha) {
        updateImageConfigById(0, imageId, posX, posY, scale, rotation, alpha);
    }
    public void updateImageConfigById(int slotId, UUID imageId, int posX, int posY, float scale, float rotation, int alpha) {
        ImageSlot slot = null;
        for (int i = 0; i < imageSlotIds.size(); i++) if (imageSlotIds.get(i) == slotId) { slot = imageSlots.get(i); break; }
        if (slot == null) {
            byte[] data = imageSourceCache.get(imageId);
            if (data != null && data.length > 0) {
                slot = new ImageSlot(imageId, imageSourceNameCache.getOrDefault(imageId, ""), data);
                imageSlots.add(slot); imageLayer.add(0); imageSlotIds.add(slotId > 0 ? slotId : nextImgSlotId++);
            } else return;
        }
        slot.setPos(posX, posY); slot.setScale(scale); slot.setRotation(rotation); slot.setAlpha(alpha);
        setChanged(); syncToBoundPlayers();
    }
    public List<ImageSlot> getImageSlots() { return imageSlots; }
    public int getImageLayer(int idx) { return idx >= 0 && idx < imageLayer.size() ? imageLayer.get(idx) : 0; }

    // ========== 雷达槽位 ==========
    public void setRadarSlots(List<RadarSlot> slots) {
        radarSlots.clear(); radarLayer.clear();
        for (RadarSlot s : slots) { radarSlots.add(s); radarLayer.add(0); }
        setChanged(); syncToBoundPlayers();
    }
    public List<RadarSlot> getRadarSlots() { return radarSlots; }
    public int getRadarLayer(int i) { return i >= 0 && i < radarLayer.size() ? radarLayer.get(i) : 0; }

    // ========== 同步 ==========
    public void syncToBoundPlayers() {
        if (level == null || level.isClientSide) return;
        CompoundTag full = new CompoundTag();
        full.putLong("TerminalPos", worldPosition.asLong());

        // 图层结构
        ListTag layersTag = new ListTag();
        for (ProLayer layer : layers) layersTag.add(layer.serialize());
        full.put("Layers", layersTag);

        // 数据源槽位（打包为按图层分组的格式）
        for (int li = 0; li < layers.size(); li++) {
            ListTag slotTag = new ListTag();
            for (DisplaySlot s : layers.get(li).getSlots()) slotTag.add(s.serialize());
            full.put("layerSlots_" + li, slotTag);
        }
        full.putInt("layerCount", layers.size());

        // 静态文本
        ListTag stTag = new ListTag();
        for (int i = 0; i < staticTextSlots.size(); i++) {
            CompoundTag t = new CompoundTag();
            StaticTextSlot st = staticTextSlots.get(i);
            t.putString("text", st.getText());
            t.putInt("posX", st.getPosX()); t.putInt("posY", st.getPosY());
            t.putFloat("scale", st.getScale()); t.putFloat("rotation", st.getRotation());
            t.putInt("color", st.getColor()); t.putInt("alpha", st.getAlpha());
            t.putInt("layerIndex", staticTextLayer.get(i));
            stTag.add(t);
        }
        full.put("StaticTexts", stTag);

        // 图片
        ListTag imgTag = new ListTag();
        for (int i = 0; i < imageSlots.size(); i++) {
            CompoundTag t = imageSlots.get(i).serialize();
            t.putInt("layerIndex", imageLayer.get(i));
            t.putInt("slotId", imageSlotIds.get(i));
            imgTag.add(t);
        }
        full.put("Images", imgTag);

        // 雷达
        ListTag radTag = new ListTag();
        for (int i = 0; i < radarSlots.size(); i++) {
            CompoundTag t = radarSlots.get(i).serialize();
            t.putInt("layerIndex", radarLayer.get(i));
            radTag.add(t);
        }
        full.put("RadarSlots", radTag);

        // 后向兼容：同时输出旧版扁平格式供 HUD 渲染（忽略不可见图层）
        List<CompoundTag> flatSlots = new ArrayList<>();
        for (int li = 0; li < layers.size(); li++) {
            if (!layers.get(li).isVisible()) continue;
            for (DisplaySlot s : layers.get(li).getSlots()) {
                CompoundTag fd = new CompoundTag();
                fd.putLong("sourcePos", s.getSourcePos().asLong());
                fd.putInt("posX", s.getPosX()); fd.putInt("posY", s.getPosY());
                fd.putFloat("scale", s.getScale()); fd.putString("text", s.getLastData());
                fd.putInt("displayLine", s.getDisplayLine()); fd.putFloat("rotation", s.getRotation());
                fd.putInt("DisplayMode", s.getDisplayMode());
                fd.putFloat("DisplayMax", s.getDisplayMax()); fd.putFloat("DisplayMin", s.getDisplayMin());
                fd.putString("DisplayUnit", s.getDisplayUnit() != null ? s.getDisplayUnit() : "");
                net.minecraft.nbt.ListTag animSync = new net.minecraft.nbt.ListTag();
                for (var a : s.getAnimations()) animSync.add(a.serialize());
                fd.put("Animations", animSync);
                fd.putInt("color", s.getColor()); fd.putInt("alpha", s.getAlpha());
                fd.putInt("layerIndex", li);
                flatSlots.add(fd);
            }
        }
        full.putInt("slotCount", flatSlots.size());
        for (int i = 0; i < flatSlots.size(); i++) full.put("slot_" + i, flatSlots.get(i));

        List<CompoundTag> flatStatic = new ArrayList<>();
        for (int i = 0; i < staticTextSlots.size(); i++) {
            int li = staticTextLayer.get(i);
            if (li >= 0 && li < layers.size() && !layers.get(li).isVisible()) continue;
            StaticTextSlot st = staticTextSlots.get(i);
            CompoundTag ft = new CompoundTag();
            ft.putString("text", st.getText()); ft.putInt("posX", st.getPosX()); ft.putInt("posY", st.getPosY());
            ft.putFloat("scale", st.getScale()); ft.putFloat("rotation", st.getRotation());
            ft.putInt("color", st.getColor()); ft.putInt("alpha", st.getAlpha());
            ft.putInt("layerIndex", li);
            flatStatic.add(ft);
        }
        full.putInt("staticCount", flatStatic.size());
        for (int i = 0; i < flatStatic.size(); i++) full.put("static_" + i, flatStatic.get(i));

        // 图层冻结信息
        full.putInt("layerCount", layers.size());
        for (int li = 0; li < layers.size(); li++) {
            CompoundTag lt = new CompoundTag();
            lt.putString("Name", layers.get(li).getName());
            lt.putBoolean("Visible", layers.get(li).isVisible());
            lt.putBoolean("Frozen", layers.get(li).isFrozen());
            full.put("layer_" + li, lt);
        }

        // 后向兼容：图片数据（供 HUD 渲染）
        List<CompoundTag> flatImg = new ArrayList<>();
        for (int j = 0; j < imageSlots.size(); j++) {
            int li = imageLayer.get(j);
            if (li >= 0 && li < layers.size() && !layers.get(li).isVisible()) continue;
            CompoundTag it = imageSlots.get(j).serialize();
            it.putInt("layerIndex", li);
            flatImg.add(it);
        }
        full.putInt("imageCount", flatImg.size());
        for (int i = 0; i < flatImg.size(); i++) full.put("image_" + i, flatImg.get(i));

        // 后向兼容：雷达数据
        List<CompoundTag> flatRadar = new ArrayList<>();
        for (int i = 0; i < radarSlots.size(); i++) {
            int li = radarLayer.get(i);
            if (li >= 0 && li < layers.size() && !layers.get(li).isVisible()) continue;
            CompoundTag rt = radarSlots.get(i).serialize();
            rt.putInt("layerIndex", li);
            flatRadar.add(rt);
        }
        full.putInt("radarCount", flatRadar.size());
        for (int i = 0; i < flatRadar.size(); i++) full.put("radar_" + i, flatRadar.get(i));

        // 数据源缓存（坞卡片用）
        List<CompoundTag> srcCache = new ArrayList<>();
        for (var e : sourceCache.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("pos", e.getKey().asLong());
            ct.putString("val", e.getValue());
            ct.putString("name", sourceNameCache.getOrDefault(e.getKey(), ""));
            srcCache.add(ct);
        }
        full.putInt("srcCacheCount", srcCache.size());
        for (int i = 0; i < srcCache.size(); i++) full.put("srcCache_" + i, srcCache.get(i));

        var syncPayload = new SyncDisplayDataPayload(full);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, syncPayload);
        }
    }

    public void openConfigurationScreen(Player player) {
        if (level != null && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            sendOpenConfigScreen(serverPlayer);
        }
    }

    public void sendOpenConfigScreen(ServerPlayer player) {
        CompoundTag full = new CompoundTag();
        full.putLong("TerminalPos", worldPosition.asLong());

        // 图层
        ListTag layersTag = new ListTag();
        for (ProLayer layer : layers) layersTag.add(layer.serialize());
        full.put("Layers", layersTag);
        full.putInt("LayerCount", layers.size());

        // 数据源槽位
        for (int li = 0; li < layers.size(); li++) {
            ListTag sl = new ListTag();
            for (DisplaySlot s : layers.get(li).getSlots()) sl.add(s.serialize());
            full.put("layerSlots_" + li, sl);
        }

        // 静态文本
        ListTag stTag = new ListTag();
        for (int i = 0; i < staticTextSlots.size(); i++) {
            CompoundTag t = new CompoundTag();
            StaticTextSlot st = staticTextSlots.get(i);
            t.putString("text", st.getText());
            t.putInt("posX", st.getPosX()); t.putInt("posY", st.getPosY());
            t.putFloat("scale", st.getScale()); t.putFloat("rotation", st.getRotation());
            t.putInt("color", st.getColor()); t.putInt("alpha", st.getAlpha());
            t.putInt("layerIndex", staticTextLayer.get(i));
            stTag.add(t);
        }
        full.put("StaticTexts", stTag);

        // 图片
        ListTag imgTag = new ListTag();
        for (int i = 0; i < imageSlots.size(); i++) {
            CompoundTag t = imageSlots.get(i).serialize();
            t.putInt("layerIndex", imageLayer.get(i));
            t.putInt("slotId", imageSlotIds.get(i));
            imgTag.add(t);
        }
        full.put("Images", imgTag);

        // 雷达
        ListTag radTag = new ListTag();
        for (int i = 0; i < radarSlots.size(); i++) {
            CompoundTag t = radarSlots.get(i).serialize();
            t.putInt("layerIndex", radarLayer.get(i));
            radTag.add(t);
        }
        full.put("RadarSlots", radTag);

        // 数据源缓存
        List<CompoundTag> srcCache = new ArrayList<>();
        for (var e : sourceCache.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("pos", e.getKey().asLong());
            ct.putString("val", e.getValue());
            ct.putString("name", sourceNameCache.getOrDefault(e.getKey(), ""));
            srcCache.add(ct);
        }
        full.putInt("srcCacheCount", srcCache.size());
        for (int i = 0; i < srcCache.size(); i++) full.put("srcCache_" + i, srcCache.get(i));

        // 图片源缓存
        List<CompoundTag> imgSrc = new ArrayList<>();
        for (var e : imageSourceCache.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("id", e.getKey());
            ct.putString("name", imageSourceNameCache.getOrDefault(e.getKey(), ""));
            ct.putByteArray("data", e.getValue());
            imgSrc.add(ct);
        }
        full.putInt("imgSrcCount", imgSrc.size());
        for (int i = 0; i < imgSrc.size(); i++) full.put("imgSrc_" + i, imgSrc.get(i));

        PacketDistributor.sendToPlayer(player, new OpenTerminalProConfigScreenPayload(full));
    }

    // ========== 持久化 ==========
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // sourceCache
        ListTag scTag = new ListTag();
        for (var e : sourceCache.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("pos", e.getKey().asLong());
            ct.putString("val", e.getValue());
            ct.putString("name", sourceNameCache.getOrDefault(e.getKey(), ""));
            scTag.add(ct);
        }
        tag.put("SourceCache", scTag);

        ListTag iscTag = new ListTag();
        for (var e : imageSourceCache.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("id", e.getKey());
            ct.putString("name", imageSourceNameCache.getOrDefault(e.getKey(), ""));
            ct.putByteArray("data", e.getValue());
            iscTag.add(ct);
        }
        tag.put("ImageSourceCache", iscTag);

        ListTag lt = new ListTag();
        for (ProLayer l : layers) lt.add(l.serialize());
        tag.put("Layers", lt);

        ListTag st = new ListTag();
        for (int i = 0; i < staticTextSlots.size(); i++) {
            CompoundTag t = new CompoundTag();
            StaticTextSlot s = staticTextSlots.get(i);
            t.putString("text", s.getText()); t.putInt("posX", s.getPosX()); t.putInt("posY", s.getPosY());
            t.putFloat("scale", s.getScale()); t.putFloat("rotation", s.getRotation());
            t.putInt("color", s.getColor()); t.putInt("alpha", s.getAlpha());
            t.putInt("layerIndex", staticTextLayer.get(i));
            st.add(t);
        }
        tag.put("StaticTexts", st);

        ListTag im = new ListTag();
        for (int i = 0; i < imageSlots.size(); i++) {
            CompoundTag t = imageSlots.get(i).serialize();
            t.putInt("layerIndex", imageLayer.get(i));
            t.putInt("slotId", imageSlotIds.get(i));
            im.add(t);
        }
        tag.put("Images", im);

        ListTag rd = new ListTag();
        for (int i = 0; i < radarSlots.size(); i++) {
            CompoundTag t = radarSlots.get(i).serialize();
            t.putInt("layerIndex", radarLayer.get(i));
            rd.add(t);
        }
        tag.put("RadarSlots", rd);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        layers.clear();
        if (tag.contains("Layers")) {
            ListTag lt = tag.getList("Layers", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < lt.size(); i++) layers.add(ProLayer.deserialize(lt.getCompound(i)));
        }
        if (layers.isEmpty()) layers.add(new ProLayer("Main"));

        sourceCache.clear(); sourceNameCache.clear();
        if (tag.contains("SourceCache")) {
            ListTag scTag = tag.getList("SourceCache", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < scTag.size(); i++) {
                CompoundTag ct = scTag.getCompound(i);
                sourceCache.put(BlockPos.of(ct.getLong("pos")), ct.getString("val"));
                sourceNameCache.put(BlockPos.of(ct.getLong("pos")), ct.getString("name"));
            }
        }
        imageSourceCache.clear(); imageSourceNameCache.clear();
        if (tag.contains("ImageSourceCache")) {
            ListTag isc = tag.getList("ImageSourceCache", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < isc.size(); i++) {
                CompoundTag ct = isc.getCompound(i);
                imageSourceCache.put(ct.getUUID("id"), ct.getByteArray("data"));
                imageSourceNameCache.put(ct.getUUID("id"), ct.getString("name"));
            }
        }

        staticTextSlots.clear(); staticTextLayer.clear();
        if (tag.contains("StaticTexts")) {
            ListTag st = tag.getList("StaticTexts", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < st.size(); i++) {
                CompoundTag t = st.getCompound(i);
                staticTextSlots.add(new StaticTextSlot(t.getString("text"), t.getInt("posX"), t.getInt("posY"),
                        t.getFloat("scale"), t.getFloat("rotation"), t.getInt("color"), t.getInt("alpha")));
                staticTextLayer.add(t.getInt("layerIndex"));
            }
        }

        imageSlots.clear(); imageLayer.clear();
        if (tag.contains("Images")) {
            ListTag im = tag.getList("Images", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < im.size(); i++) {
                CompoundTag t = im.getCompound(i);
                ImageSlot s = ImageSlot.deserialize(t);
                imageSlots.add(s);
                imageLayer.add(t.getInt("layerIndex"));
                imageSlotIds.add(t.getInt("slotId"));
            }
        }

        radarSlots.clear(); radarLayer.clear();
        if (tag.contains("RadarSlots")) {
            ListTag rd = tag.getList("RadarSlots", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < rd.size(); i++) {
                CompoundTag t = rd.getCompound(i);
                radarSlots.add(RadarSlot.deserialize(t));
                radarLayer.add(t.getInt("layerIndex"));
            }
        }
    }

    public void removeStaticText(int index) {
        if (index >= 0 && index < staticTextSlots.size()) {
            staticTextSlots.remove(index); staticTextLayer.remove(index);
            setChanged(); syncToBoundPlayers();
        }
    }

    /** 客户端保存：完整替换图层和槽位数据 */
    public void loadConfigFromClient(CompoundTag data) {
        // 图层结构
        layers.clear();
        if (data.contains("Layers")) {
            ListTag lt = data.getList("Layers", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < lt.size(); i++) layers.add(ProLayer.deserialize(lt.getCompound(i)));
        }
        if (layers.isEmpty()) layers.add(new ProLayer("Main"));

        // 静态文本
        staticTextSlots.clear(); staticTextLayer.clear();
        if (data.contains("StaticTexts")) {
            ListTag st = data.getList("StaticTexts", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < st.size(); i++) {
                CompoundTag t = st.getCompound(i);
                staticTextSlots.add(new StaticTextSlot(t.getString("text"), t.getInt("posX"), t.getInt("posY"),
                        t.getFloat("scale"), t.getFloat("rotation"), t.getInt("color"), t.getInt("alpha")));
                staticTextLayer.add(t.getInt("layerIndex"));
            }
        }
        // 图片
        imageSlots.clear(); imageLayer.clear();
        if (data.contains("Images")) {
            ListTag im = data.getList("Images", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < im.size(); i++) {
                CompoundTag t = im.getCompound(i);
                ImageSlot s = ImageSlot.deserialize(t);
                imageSlots.add(s);
                imageLayer.add(t.getInt("layerIndex"));
                imageSlotIds.add(t.getInt("slotId"));
            }
        }
        // 雷达
        radarSlots.clear(); radarLayer.clear();
        if (data.contains("RadarSlots")) {
            ListTag rd = data.getList("RadarSlots", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < rd.size(); i++) {
                CompoundTag t = rd.getCompound(i);
                radarSlots.add(RadarSlot.deserialize(t));
                radarLayer.add(t.getInt("layerIndex"));
            }
        }
        setChanged(); syncToBoundPlayers();
    }

    public void displayData(BlockPos sourcePos, CompoundTag data) {
        String text = data.getString("display_text");
        updateSlotData(sourcePos, text);
    }
}
