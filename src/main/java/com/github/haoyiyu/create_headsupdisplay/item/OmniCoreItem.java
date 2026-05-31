package com.github.haoyiyu.create_headsupdisplay.item;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlock;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.registration.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;  // 添加缺失的导入

public class OmniCoreItem extends BlockItem {
    public OmniCoreItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof DisplayTerminalBlock) {
            if (!level.isClientSide) {
                var stack = context.getItemInHand();
                stack.set(ModDataComponents.OMNI_CORE_BOUND_TERMINAL.get(), pos);
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Bound to terminal at " + pos.toShortString()), true
                    );
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var stack = context.getItemInHand();
        BlockPos boundTerminal = stack.get(ModDataComponents.OMNI_CORE_BOUND_TERMINAL.get());
        if (boundTerminal != null && super.placeBlock(context, state)) {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof OmniCoreBlockEntity core) {
                core.setBoundTerminal(boundTerminal);
            }
            return true;
        }
        return super.placeBlock(context, state);
    }
}