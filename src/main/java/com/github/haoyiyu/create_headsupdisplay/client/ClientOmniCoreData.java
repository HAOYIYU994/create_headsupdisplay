package com.github.haoyiyu.create_headsupdisplay.client;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class ClientOmniCoreData {
    /** 缓存完整源数据（名字 + 信号强度 + 物品），key 为 corePos */
    private static final Map<BlockPos, CompoundTag> cache = new HashMap<>();

    public static void updateSources(CompoundTag data) {
        BlockPos corePos = BlockPos.of(data.getLong("CorePos"));
        cache.put(corePos, data);
    }

    public static CompoundTag getSourcesData(BlockPos corePos) {
        return cache.get(corePos);
    }

    public static void clear(BlockPos corePos) {
        cache.remove(corePos);
    }
}