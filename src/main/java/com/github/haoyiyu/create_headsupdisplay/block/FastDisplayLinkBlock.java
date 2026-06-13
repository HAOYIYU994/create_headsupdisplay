package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class FastDisplayLinkBlock extends DisplayLinkBlock {
    public static final MapCodec<DisplayLinkBlock> CODEC = simpleCodec(FastDisplayLinkBlock::new);
    public FastDisplayLinkBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends DisplayLinkBlock> codec() { return CODEC; }
    @Override public BlockEntityType<? extends FastDisplayLinkBlockEntity> getBlockEntityType() {
        return ModBlockEntities.FAST_DISPLAY_LINK.get();
    }
}
