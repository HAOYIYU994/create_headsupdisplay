package com.github.haoyiyu.create_headsupdisplay.block.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ChannelRegistry {
    private static final NbtPathChannel NBT = new NbtPathChannel();

    public static DataChannel getById(String id) { return NBT; }

    public static NbtPathChannel nbt() { return NBT; }

    public static List<CompoundTag> scanTarget(Level level, BlockPos targetPos, BlockState targetState, BlockEntity targetBe) {
        CompoundTag info = new CompoundTag();
        info.putString("id", NBT.getId());
        info.putString("nameKey", NBT.getDisplayNameKey());
        info.putString("unit", NBT.getUnit());
        info.putBoolean("needsConfig", NBT.needsConfig());
        info.put("defaultConfig", NBT.getDefaultConfig());
        return List.of(info);
    }
}
