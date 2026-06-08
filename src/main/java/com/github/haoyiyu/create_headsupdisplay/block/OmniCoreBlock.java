package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.item.LinkBlockItem;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class OmniCoreBlock extends Block implements EntityBlock {
    public OmniCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.OMNI_CORE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof OmniCoreBlockEntity core) {
                core.openConfigScreen(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof LinkBlockItem) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) {
            tryAutoBindTerminals(level, pos);
        }
    }

    /** 放置核心时自动检测相邻 DisplayTerminal 并绑定 */
    private void tryAutoBindTerminals(Level level, BlockPos corePos) {
        BlockEntity coreBe = level.getBlockEntity(corePos);
        if (!(coreBe instanceof OmniCoreBlockEntity core)) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = corePos.relative(dir);
            if (level.getBlockState(neighbor).is(ModBlocks.DISPLAY_TERMINAL.get())) {
                core.setBoundTerminal(neighbor);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 只在服务端执行 tick
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.OMNI_CORE.get(), OmniCoreBlockEntity::tick);
    }

    // 辅助方法：创建 ticker
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }
}