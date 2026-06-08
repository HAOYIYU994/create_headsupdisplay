package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.item.LinkBlockItem;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 手持链接方块时跳过默认交互，交给 LinkBlockItem.useOn 处理
        if (stack.getItem() instanceof LinkBlockItem) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        // 其他物品：走默认 useWithoutItem 逻辑
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) {
            tryAutoBindToOmniCore(level, pos);
        }
    }

    /** 放置终端时自动检测相邻位置是否有 OmniCore，有则直接绑定 */
    private void tryAutoBindToOmniCore(Level level, BlockPos terminalPos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = terminalPos.relative(dir);
            if (level.getBlockState(neighbor).is(ModBlocks.OMNI_CORE.get())) {
                BlockEntity be = level.getBlockEntity(neighbor);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.setBoundTerminal(terminalPos);
                }
            }
        }
    }
}