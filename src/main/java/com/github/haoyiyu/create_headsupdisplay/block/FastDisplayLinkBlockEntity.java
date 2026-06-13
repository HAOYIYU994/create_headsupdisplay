package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class FastDisplayLinkBlockEntity extends DisplayLinkBlockEntity {

    public FastDisplayLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAST_DISPLAY_LINK.get(), pos, state);
    }

    @Override
    public void tick() {
        refreshTicks = 999;
        super.tick();
    }
}
