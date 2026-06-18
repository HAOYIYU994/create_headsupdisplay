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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DisplayTerminalProBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<DisplayTerminalProBlock> CODEC = simpleCodec(DisplayTerminalProBlock::new);

    public DisplayTerminalProBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayTerminalProBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DisplayTerminalProBlockEntity terminalBE) {
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !newState.is(state.getBlock())) {
            tryAutoUnbindFromOmniCore(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    /** 拆除终端时自动通知相邻 OmniCore 解绑 */
    private void tryAutoUnbindFromOmniCore(Level level, BlockPos terminalPos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = terminalPos.relative(dir);
            if (level.getBlockState(neighbor).is(ModBlocks.OMNI_CORE.get())) {
                BlockEntity be = level.getBlockEntity(neighbor);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.removeBoundTerminal(terminalPos);
                }
            }
        }
    }
}
