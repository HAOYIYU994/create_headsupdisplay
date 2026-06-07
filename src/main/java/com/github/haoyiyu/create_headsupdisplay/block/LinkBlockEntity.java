package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 链接方块实体：存储 OmniCore 坐标和终端坐标。
 * 放置时自动通知 OmniCore 绑定该终端。
 */
public class LinkBlockEntity extends BlockEntity {
    private BlockPos omniCorePos;
    private BlockPos terminalPos;
    private BlockPos monitorPos; // 雷达 Monitor 位置

    public LinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINK.get(), pos, state);
    }

    public void setOmniCorePos(BlockPos pos) {
        this.omniCorePos = pos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setTerminalPos(BlockPos pos) { this.terminalPos = pos; setChanged(); }
    public void setMonitorPos(BlockPos pos) { this.monitorPos = pos; setChanged(); }

    public BlockPos getOmniCorePos() { return omniCorePos; }
    public BlockPos getTerminalPos() { return terminalPos; }
    public BlockPos getMonitorPos() { return monitorPos; }

    /** 被拆除时调用，断开 OmniCore 与终端的连接 */
    public void onBroken() {
        if (level == null || level.isClientSide || omniCorePos == null) return;
        BlockEntity be = level.getBlockEntity(omniCorePos);
        if (be instanceof OmniCoreBlockEntity core) {
            if (terminalPos != null) core.removeBoundTerminal(terminalPos);
            if (monitorPos != null) core.clearLinkedMonitor();
        }
    }

    /** 被放置到世界后调用，通知 OmniCore 绑定终端或监听雷达 */
    public void onPlaced() {
        if (level == null || level.isClientSide) return;
        if (omniCorePos == null) return;

        BlockEntity be = level.getBlockEntity(omniCorePos);
        if (be instanceof OmniCoreBlockEntity core) {
            if (terminalPos != null) core.setBoundTerminal(terminalPos);
            if (monitorPos != null) core.setLinkedMonitor(monitorPos);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (omniCorePos != null) tag.putLong("OmniCorePos", omniCorePos.asLong());
        if (terminalPos != null) tag.putLong("TerminalPos", terminalPos.asLong());
        if (monitorPos != null) tag.putLong("MonitorPos", monitorPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("OmniCorePos")) omniCorePos = BlockPos.of(tag.getLong("OmniCorePos"));
        if (tag.contains("TerminalPos")) terminalPos = BlockPos.of(tag.getLong("TerminalPos"));
        if (tag.contains("MonitorPos")) monitorPos = BlockPos.of(tag.getLong("MonitorPos"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}