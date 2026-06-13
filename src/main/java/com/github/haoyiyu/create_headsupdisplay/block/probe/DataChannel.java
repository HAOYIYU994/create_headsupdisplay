package com.github.haoyiyu.create_headsupdisplay.block.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 通用数据通道接口。
 * DataProbe 通过此接口自动探测目标方块可读取的数据类型。
 * 每个实现对应一种数据类别（红石信号、容器物品数、能量、流体等）。
 */
public interface DataChannel {

    /** 通道唯一标识，如 "redstone", "container_count", "energy_stored" */
    String getId();

    /** 可翻译的显示名键，如 "gui.create_headsupdisplay.data_probe.channel.redstone" */
    String getDisplayNameKey();

    /**
     * 检测目标方块是否支持此通道。
     * 用于 GUI 中过滤可用通道列表。
     */
    boolean canRead(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be);

    /**
     * 读取当前值，返回格式化后的字符串。
     * 如有错误则返回 "ERR"。
     */
    String readValue(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, CompoundTag config);

    /** 数值单位（如 "FE", "mB", "RPM", "%", ""） */
    String getUnit();

    /**
     * 是否需要额外配置（如 NBT 路径、方块状态属性名、槽位索引）。
     * 返回 true 表示 GUI 需要展示额外配置输入。
     */
    default boolean needsConfig() { return false; }

    /**
     * 获取额外配置的默认值，供 GUI 初始化和保存时使用。
     */
    default CompoundTag getDefaultConfig() { return new CompoundTag(); }
}
