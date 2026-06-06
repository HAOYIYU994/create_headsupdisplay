package com.github.haoyiyu.create_headsupdisplay.integration;

import com.happysg.radar.block.datalink.DataController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

/**
 * OmniCore 的雷达 DataController 桥接。
 * 注册后，雷达的 DataLink 系统可将 OmniCore 视为合法的显示目标。
 */
public class OmniCoreRadarBehavior extends DataController {

    @Override
    public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
        return new AABB(pos).inflate(0.5);
    }
}
