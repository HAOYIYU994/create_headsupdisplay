package com.github.haoyiyu.create_headsupdisplay.client;

import net.minecraft.core.BlockPos;

public class ClientFrequencySelectionCache {
    private static BlockPos cachedCorePos = null;

    public static void setCorePos(BlockPos pos) {
        cachedCorePos = pos;
    }

    public static BlockPos getAndClear() {
        BlockPos pos = cachedCorePos;
        cachedCorePos = null;
        return pos;
    }
}