package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DisplayTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<DisplayTerminalBlock> CODEC = simpleCodec(DisplayTerminalBlock::new);

    public DisplayTerminalBlock(Properties properties) {
        super(properties);
        System.out.println("DisplayTerminalBlock constructor called");
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayTerminalBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DisplayTerminalBlockEntity terminalBE) {
            terminalBE.openConfigurationScreen(player);
        }
        return InteractionResult.SUCCESS;
    }
}