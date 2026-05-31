package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.config.DisplaySlot;
import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import com.github.haoyiyu.create_headsupdisplay.network.OpenOmniCoreScreenPayload;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.github.haoyiyu.create_headsupdisplay.util.RedstoneSignalHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OmniCoreBlockEntity extends BlockEntity {
    private boolean autoSortEnabled = true; // 自动置顶开关
    private BlockPos boundTerminalPos;
    private List<RedstoneSource> sources = new ArrayList<>();
    private Map<Integer, BlockPos> sentSources = new HashMap<>();

    public OmniCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMNI_CORE.get(), pos, state);
    }

    public boolean isAutoSortEnabled() { return autoSortEnabled; }
    public void toggleAutoSort() { this.autoSortEnabled = !this.autoSortEnabled; setChanged(); }

    public void setBoundTerminal(BlockPos pos) {
        this.boundTerminalPos = pos;
        setChanged();
    }

    public BlockPos getBoundTerminal() {
        return boundTerminalPos;
    }

    public void openConfigScreen(Player player) {
        if (level == null) return;
        if (player instanceof ServerPlayer sp) {
            CompoundTag data = new CompoundTag();
            data.putLong("CorePos", worldPosition.asLong());
            if (boundTerminalPos != null) {
                data.putLong("BoundTerminal", boundTerminalPos.asLong());
            }
            ListTag sourcesTag = new ListTag();
            for (RedstoneSource src : sources) {
                CompoundTag srcTag = new CompoundTag();
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
                sourcesTag.add(srcTag);
            }
            data.put("Sources", sourcesTag);
            PacketDistributor.sendToPlayer(sp, new OpenOmniCoreScreenPayload(data));
        }
    }

    public void addRedstoneSource(String name, ItemStack frequencyItem1, ItemStack frequencyItem2) {
        sources.add(new RedstoneSource(name, frequencyItem1, frequencyItem2));
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

    private void removeFromTerminal(int sourceIndex) {
        if (boundTerminalPos == null) return;
        BlockEntity be = level.getBlockEntity(boundTerminalPos);
        if (be instanceof DisplayTerminalBlockEntity terminal) {
            BlockPos virtualPos = sentSources.get(sourceIndex);
            if (virtualPos != null) {
                terminal.removeSlot(virtualPos);
                sentSources.remove(sourceIndex);
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

    /** Display Link 推送数据时添加或更新源，文本变化时自动置顶 */
    public void addOrUpdateDisplayLinkSource(BlockPos sourcePos, String name, String text) {
        for (int i = 0; i < sources.size(); i++) {
            RedstoneSource src = sources.get(i);
            if (sourcePos.equals(src.displayLinkSourcePos)) {
                if (!text.equals(src.displayLinkText)) {
                    src.displayLinkText = text;
                    if (autoSortEnabled) moveToTop(i);
                }
                setChanged();
                return;
            }
        }
        RedstoneSource src = new RedstoneSource(name, ItemStack.EMPTY, ItemStack.EMPTY);
        src.displayLinkSourcePos = sourcePos;
        src.displayLinkText = text;
        sources.add(0, src);
        // 重映射 sentSources：所有索引 +1
        Map<Integer, BlockPos> remapped = new HashMap<>();
        for (var entry : sentSources.entrySet()) {
            remapped.put(entry.getKey() + 1, entry.getValue());
        }
        sentSources = remapped;
        setChanged();
    }

    /** 供 RequestSourcesDataPayload 读取源数据，返回不可变快照 */
    public SourceSnapshot getSource(int index) {
        RedstoneSource src = sources.get(index);
        int strength = getCurrentStrength(src);
        String display = getDisplayText(src);
        if (src.displayLinkSourcePos != null) {
            return new SourceSnapshot(src.name, src.frequencyItem1, src.frequencyItem2, strength, display, src.displayLinkSourcePos);
        }
        return new SourceSnapshot(src.name, src.frequencyItem1, src.frequencyItem2, strength, display, null);
    }

    public record SourceSnapshot(String name, net.minecraft.world.item.ItemStack item1,
                                  net.minecraft.world.item.ItemStack item2, int strength,
                                  String displayText, BlockPos displayLinkSourcePos) {}

    public void sendToTerminal(int sourceIndex) {
        if (boundTerminalPos == null) return;
        if (sourceIndex < 0 || sourceIndex >= sources.size()) return;
        BlockEntity be = level.getBlockEntity(boundTerminalPos);
        if (!(be instanceof DisplayTerminalBlockEntity terminal)) return;

        RedstoneSource src = sources.get(sourceIndex);
        // 用源名称做稳定标识
        BlockPos virtualPos;
        if (src.displayLinkSourcePos != null) {
            virtualPos = new BlockPos(src.displayLinkSourcePos.hashCode(), 0, 0);
        } else {
            virtualPos = new BlockPos(0, 0, Math.abs(src.name.hashCode()));
        }

        boolean isNew = !sentSources.containsKey(sourceIndex);
        if (isNew) {
            terminal.updateSlotConfig(virtualPos,
                    10, 50 + sentSources.size() * 20,
                    1.0f, 0f, 0xFFFFFF, 255);
        }

        // 只发送数据值，名称存到 sourceName
        String displayText;
        if (src.displayLinkText != null) {
            displayText = src.displayLinkText;
        } else {
            displayText = getDisplayText(src);
        }
        terminal.updateSlotData(virtualPos, displayText);
        terminal.updateSlotSourceName(virtualPos, src.name.replaceAll("§[0-9a-fk-or]", ""));
        sentSources.put(sourceIndex, virtualPos);
        setChanged();
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
        if (src.displayLinkSourcePos != null) return 0;
        return RedstoneSignalHelper.getSignalStrength(level, src.frequencyItem1, src.frequencyItem2);
    }

    /** 获取源的显示文本（应用转译后） */
    private String getDisplayText(RedstoneSource src) {
        int raw = getCurrentStrength(src);
        if (src.translation != null && src.translation.getMode() != TranslationConfig.Mode.NONE) {
            String t = src.translation.getDisplay(raw);
            if (t != null) return t;
        }
        return String.valueOf(raw);
    }

    // 每 tick 执行，实时更新已发送槽位的数据；强度变化时自动置顶
    public static void tick(Level level, BlockPos pos, BlockState state, OmniCoreBlockEntity be) {
        if (level.isClientSide) return;

        // 第一步：检测所有红石源强度变化（不限已发送），变化时置顶（不需要绑定终端）
        for (int idx = 0; idx < be.sources.size(); idx++) {
            RedstoneSource src = be.sources.get(idx);
            if (src.displayLinkSourcePos != null) continue; // 跳过 Display Link 源
            int strength = RedstoneSignalHelper.getSignalStrength(level, src.frequencyItem1, src.frequencyItem2);
            if (be.autoSortEnabled && strength != src.lastStrength) {
                src.lastStrength = strength;
                be.moveToTop(idx);
                break;
            }
        }

        // 第二步：更新已发送到终端的槽位数据（需绑定终端）
        if (be.boundTerminalPos == null) return;
        BlockEntity terminalBe = level.getBlockEntity(be.boundTerminalPos);
        if (!(terminalBe instanceof DisplayTerminalBlockEntity terminal)) return;

        var sentIndices = new java.util.ArrayList<>(be.sentSources.keySet());
        for (int idx : sentIndices) {
            BlockPos virtualPos = be.sentSources.get(idx);
            if (virtualPos == null) continue;
            if (idx < 0 || idx >= be.sources.size()) {
                be.sentSources.remove(idx);
                continue;
            }
            // 终端已删除 → 停止更新
            if (terminal.getSlot(virtualPos) == null) {
                be.sentSources.remove(idx);
                continue;
            }
            RedstoneSource src = be.sources.get(idx);
            if (src.displayLinkSourcePos != null) continue;
            String newText = be.getDisplayText(src);
            // 只在文本变化时更新，避免频繁写终端干扰编辑
            if (!newText.equals(src.lastSentText)) {
                src.lastSentText = newText;
                terminal.updateSlotData(virtualPos, newText);
            }
        }
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
        if (boundTerminalPos != null) {
            tag.putLong("BoundTerminal", boundTerminalPos.asLong());
        }
        ListTag sourcesTag = new ListTag();
        for (RedstoneSource src : sources) {
            CompoundTag srcTag = new CompoundTag();
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BoundTerminal")) {
            boundTerminalPos = BlockPos.of(tag.getLong("BoundTerminal"));
        }
        sources.clear();
        ListTag sourcesTag = tag.getList("Sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < sourcesTag.size(); i++) {
            CompoundTag srcTag = sourcesTag.getCompound(i);
            String name = srcTag.getString("name");
            ItemStack freqItem1 = ItemStack.parseOptional(registries, srcTag.getCompound("freqItem1"));
            ItemStack freqItem2 = ItemStack.parseOptional(registries, srcTag.getCompound("freqItem2"));
            RedstoneSource src = new RedstoneSource(name, freqItem1, freqItem2);
            if (srcTag.contains("dlSourcePos")) {
                src.displayLinkSourcePos = BlockPos.of(srcTag.getLong("dlSourcePos"));
            }
            if (srcTag.contains("dlText")) {
                src.displayLinkText = srcTag.getString("dlText");
            }
            if (srcTag.contains("translation")) {
                src.translation = TranslationConfig.deserialize(srcTag.getCompound("translation"));
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
    }

    private static class RedstoneSource {
        String name;
        ItemStack frequencyItem1;
        ItemStack frequencyItem2;
        int lastStrength = -1;
        String lastSentText;
        BlockPos displayLinkSourcePos;
        String displayLinkText;
        TranslationConfig translation;

        RedstoneSource(String name, ItemStack item1, ItemStack item2) {
            this.name = name;
            this.frequencyItem1 = item1;
            this.frequencyItem2 = item2;
        }
    }
}