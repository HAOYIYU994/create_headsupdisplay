package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.config.DisplaySlot;
import com.github.haoyiyu.create_headsupdisplay.config.StaticTextSlot;
import com.github.haoyiyu.create_headsupdisplay.menu.DisplayTerminalMenu;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import com.github.haoyiyu.create_headsupdisplay.network.SyncDisplayDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import com.github.haoyiyu.create_headsupdisplay.network.OpenTerminalConfigScreenPayload;
import com.github.haoyiyu.create_headsupdisplay.network.UpdateSlotPayload;
import com.github.haoyiyu.create_headsupdisplay.network.UpdateStaticTextPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DisplayTerminalBlockEntity extends BlockEntity {
    // 数据源槽位（来自显示连接器）
    private final Map<BlockPos, DisplaySlot> slots = new HashMap<>();
    // 静态文本槽位（用户手动添加）
    private final List<StaticTextSlot> staticTextSlots = new ArrayList<>();

    public DisplayTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_TERMINAL_BE.get(), pos, state);
    }

    // ========== 数据源槽位管理 ==========
    public List<DisplaySlot> getAllSlots() {
        return new ArrayList<>(slots.values());
    }

    public DisplaySlot getOrCreateSlot(BlockPos sourcePos) {
        return slots.computeIfAbsent(sourcePos, k -> new DisplaySlot(sourcePos));
    }

    public void updateSlotData(BlockPos sourcePos, String text) {
        DisplaySlot slot = getOrCreateSlot(sourcePos);
        slot.setLastData(text);
        setChanged();
        syncToBoundPlayers();
    }

    // 修改：增加 rotation 参数
    public void updateSlotConfig(BlockPos sourcePos, int posX, int posY, float scale, float rotation, int color, int alpha) {
        DisplaySlot slot = getOrCreateSlot(sourcePos);
        slot.setPos(posX, posY);
        slot.setScale(scale);
        slot.setRotation(rotation);
        slot.setColor(color);
        slot.setAlpha(alpha);
        setChanged();
        syncToBoundPlayers();
    }

    public void updateSlotDataAndStyle(BlockPos sourcePos, String text, int line) {
        DisplaySlot slot = getOrCreateSlot(sourcePos);
        slot.setLastData(text);
        slot.setDisplayLine(line);
        setChanged();
        syncToBoundPlayers();
    }

    public void removeSlot(BlockPos sourcePos) {
        if (slots.containsKey(sourcePos)) {
            slots.remove(sourcePos);
            setChanged();
            syncToBoundPlayers();
        }
    }

    // ========== 静态文本槽位管理 ==========
    public List<StaticTextSlot> getStaticTextSlots() {
        return new ArrayList<>(staticTextSlots);
    }

    public void addStaticTextSlot(String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
        staticTextSlots.add(new StaticTextSlot(text, posX, posY, scale, rotation, color, alpha));
        setChanged();
        syncToBoundPlayers();
    }
    public void removeStaticTextSlot(int index) {
        if (index >= 0 && index < staticTextSlots.size()) {
            staticTextSlots.remove(index);
            setChanged();
            syncToBoundPlayers();
        }
    }

    public void updateStaticTextSlot(int index, String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
        if (index >= 0 && index < staticTextSlots.size()) {
            StaticTextSlot slot = staticTextSlots.get(index);
            slot.setText(text);
            slot.setPos(posX, posY);
            slot.setScale(scale);
            slot.setRotation(rotation);
            slot.setColor(color);
            slot.setAlpha(alpha);
            setChanged();
            syncToBoundPlayers();
        }
    }

    // ========== 同步数据给客户端（头盔） ==========
    private void syncToBoundPlayers() {
        if (level == null || level.isClientSide) return;

        // 打包数据源槽位（增加 rotation）
        List<CompoundTag> slotsData = new ArrayList<>();
        for (DisplaySlot slot : slots.values()) {
            CompoundTag data = new CompoundTag();
            data.putLong("sourcePos", slot.getSourcePos().asLong());
            data.putInt("posX", slot.getPosX());
            data.putInt("posY", slot.getPosY());
            data.putFloat("scale", slot.getScale());
            data.putString("text", slot.getLastData());
            data.putInt("displayLine", slot.getDisplayLine());
            data.putFloat("rotation", slot.getRotation());
            data.putInt("color", slot.getColor());
            data.putInt("alpha", slot.getAlpha());
            slotsData.add(data);

        }

        // 打包静态文本槽位
        List<CompoundTag> staticData = new ArrayList<>();
        for (StaticTextSlot slot : staticTextSlots) {
            CompoundTag tag = new CompoundTag();
            tag.putString("text", slot.getText());
            tag.putInt("posX", slot.getPosX());
            tag.putInt("posY", slot.getPosY());
            tag.putFloat("scale", slot.getScale());
            tag.putFloat("rotation", slot.getRotation());
            tag.putInt("color", slot.getColor());
            tag.putInt("alpha", slot.getAlpha());
            staticData.add(tag);

        }

        CompoundTag full = new CompoundTag();
        full.putInt("slotCount", slotsData.size());
        for (int i = 0; i < slotsData.size(); i++) {
            full.put("slot_" + i, slotsData.get(i));
        }
        full.putInt("staticCount", staticData.size());
        for (int i = 0; i < staticData.size(); i++) {
            full.put("static_" + i, staticData.get(i));
        }

        // 发送给所有绑定此终端的玩家
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            var helmet = player.getInventory().armor.get(3);
            if (helmet.getItem() instanceof HeadMountDisplayItem) {
                BlockPos boundPos = HeadMountDisplayItem.getBoundTerminalPos(helmet);
                if (boundPos != null && boundPos.equals(this.worldPosition)) {
                    PacketDistributor.sendToPlayer(player, new SyncDisplayDataPayload(full));
                }
            }
        }
    }

    // ========== 打开配置屏幕（发送所有数据） ==========
    public void openConfigurationScreen(Player player) {
        if (level != null && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            sendOpenConfigScreen(serverPlayer);
        }
    }

    public void sendOpenConfigScreen(ServerPlayer player) {
        CompoundTag full = new CompoundTag();
        full.putLong("TerminalPos", worldPosition.asLong());

        // 数据源槽位（增加 rotation）
        ListTag slotsTag = new ListTag();
        for (DisplaySlot slot : slots.values()) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putLong("sourcePos", slot.getSourcePos().asLong());
            slotTag.putInt("posX", slot.getPosX());
            slotTag.putInt("posY", slot.getPosY());
            slotTag.putFloat("scale", slot.getScale());
            slotTag.putString("text", slot.getLastData());
            slotTag.putInt("displayLine", slot.getDisplayLine());
            slotTag.putFloat("rotation", slot.getRotation());
            slotTag.putInt("color", slot.getColor());
            slotTag.putInt("alpha", slot.getAlpha());
            slotsTag.add(slotTag);
        }
        full.put("Slots", slotsTag);

        // 静态文本槽位
        ListTag staticTag = new ListTag();
        for (StaticTextSlot slot : staticTextSlots) {
            CompoundTag tag = new CompoundTag();
            tag.putString("text", slot.getText());
            tag.putInt("posX", slot.getPosX());
            tag.putInt("posY", slot.getPosY());
            tag.putFloat("scale", slot.getScale());
            tag.putFloat("rotation", slot.getRotation());
            tag.putInt("color", slot.getColor());
            tag.putInt("alpha", slot.getAlpha());
            staticTag.add(tag);
        }
        full.put("StaticTexts", staticTag);

        PacketDistributor.sendToPlayer(player, new OpenTerminalConfigScreenPayload(full));
    }

    // ========== 处理来自客户端的更新 ==========
    public void handleStaticTextUpdate(int index, String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
        if (index == -1) {
            addStaticTextSlot(text, posX, posY, scale, rotation, color, alpha);
        } else {
            updateStaticTextSlot(index, text, posX, posY, scale, rotation, color, alpha);
        }
    }
    // ========== 数据持久化 ==========
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // 保存数据源槽位
        ListTag slotsTag = new ListTag();
        for (DisplaySlot slot : slots.values()) {
            slotsTag.add(slot.serialize()); // 假设 serialize 已包含 rotation
        }
        tag.put("Slots", slotsTag);

        // 保存静态文本槽位
        ListTag staticTag = new ListTag();
        for (StaticTextSlot slot : staticTextSlots) {
            CompoundTag st = new CompoundTag();
            st.putString("text", slot.getText());
            st.putInt("posX", slot.getPosX());
            st.putInt("posY", slot.getPosY());
            st.putFloat("scale", slot.getScale());
            st.putFloat("rotation", slot.getRotation());
            staticTag.add(st);
        }
        tag.put("StaticTexts", staticTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // 加载数据源槽位
        slots.clear();
        ListTag slotsTag = tag.getList("Slots", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < slotsTag.size(); i++) {
            CompoundTag entry = slotsTag.getCompound(i);
            DisplaySlot slot = DisplaySlot.deserialize(entry); // 假设 deserialize 已读取 rotation
            slots.put(slot.getSourcePos(), slot);
        }

        // 加载静态文本槽位
        staticTextSlots.clear();
        ListTag staticTag = tag.getList("StaticTexts", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < staticTag.size(); i++) {
            CompoundTag st = staticTag.getCompound(i);
            String text = st.getString("text");
            int posX = st.getInt("posX");
            int posY = st.getInt("posY");
            int color = st.getInt("color");
            int alpha = st.getInt("alpha");
            float scale = st.getFloat("scale");
            float rotation = st.getFloat("rotation");
            staticTextSlots.add(new StaticTextSlot(text, posX, posY, scale, rotation, color, alpha));
        }
    }

    public void removeStaticText(int index) {
        if (index >= 0 && index < staticTextSlots.size()) {
            staticTextSlots.remove(index);
            setChanged();
            syncToBoundPlayers();
        }
    }

    public void displayData(BlockPos sourcePos, CompoundTag data) {
        String text = data.getString("display_text");
        updateSlotData(sourcePos, text);
    }
}