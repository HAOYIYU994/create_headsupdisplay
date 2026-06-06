package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DisplayBlockEntity extends SmartBlockEntity {
    private BlockPos controllerPos;
    private int panelWidth = 1;
    private int panelHeight = 1;
    private BlockPos boundTerminal;

    public DisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.controllerPos = pos;
    }

    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DISPLAY.get(), pos, state);
    }

    public BlockPos getControllerPos() { return controllerPos; }
    public int getPanelWidth() { return panelWidth; }
    public int getPanelHeight() { return panelHeight; }
    public boolean isController() { return controllerPos != null && controllerPos.equals(worldPosition); }

    public void setControllerPos(BlockPos pos, int w, int h) {
        this.controllerPos = pos; this.panelWidth = w; this.panelHeight = h; setChanged();
    }

    public BlockPos getBoundTerminal() {
        if (!isController() && controllerPos != null && level != null) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof DisplayBlockEntity ctrl) return ctrl.boundTerminal;
            return null;
        }
        return boundTerminal;
    }

    public void setBoundTerminal(BlockPos pos) {
        if (isController()) { boundTerminal = pos; setChanged(); }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        if (boundTerminal != null) return;
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (level.getBlockEntity(n) instanceof DisplayTerminalBlockEntity term) {
                boundTerminal = n;
                if (isController()) setChanged();
                if (getBlockState().getValue(DisplayBlock.SHAPE) == DisplayBlock.Shape.SINGLE)
                    DisplayMultiBlockHelper.refreshAt(level, worldPosition);
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        // 仅验证现有多方块结构完整性
        if (level.getGameTime() % 20 == 0 && isController()
                && (panelWidth > 1 || panelHeight > 1)) {
            Direction facing = getBlockState().getValue(DisplayBlock.FACING);
            if (!DisplayMultiBlockHelper.isValidFormation(level, controllerPos, facing, panelWidth, panelHeight)) {
                DisplayMultiBlockHelper.refreshAt(level, worldPosition);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider reg, boolean clientPacket) {
        super.write(tag, reg, clientPacket);
        tag.putLong("ctrl", controllerPos.asLong());
        tag.putInt("pw", panelWidth);
        tag.putInt("ph", panelHeight);
        if (boundTerminal != null) tag.putLong("term", boundTerminal.asLong());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider reg, boolean clientPacket) {
        super.read(tag, reg, clientPacket);
        controllerPos = tag.contains("ctrl") ? BlockPos.of(tag.getLong("ctrl")) : worldPosition;
        panelWidth = tag.contains("pw") ? tag.getInt("pw") : (tag.contains("size") ? tag.getInt("size") : 1);
        panelHeight = tag.contains("ph") ? tag.getInt("ph") : (tag.contains("size") ? tag.getInt("size") : 1);
        if (tag.contains("term")) boundTerminal = BlockPos.of(tag.getLong("term"));
    }
}
