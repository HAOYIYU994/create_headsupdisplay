package com.github.haoyiyu.create_headsupdisplay.item;

import com.github.haoyiyu.create_headsupdisplay.block.LinkBlock;
import com.github.haoyiyu.create_headsupdisplay.block.LinkBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlock;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlocks;
import com.github.haoyiyu.create_headsupdisplay.registration.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 链接方块物品。仿 Create ClickToLinkBlockItem 模式：
 * 1. 右键 OmniCore → 存储核心坐标到物品
 * 2. 右键 Display Terminal → 放置链接方块，自动绑定终端到核心
 * Shift+右键 → 清除已存坐标
 */
public class LinkBlockItem extends BlockItem {

    public LinkBlockItem(Properties properties) {
        super(ModBlocks.LINK_BLOCK.get(), properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        // Shift+右键：清除已绑定的核心坐标
        if (player.isShiftKeyDown() && stack.has(ModDataComponents.LINKED_OMNI_CORE_POS.get())) {
            if (!level.isClientSide) {
                stack.remove(ModDataComponents.LINKED_OMNI_CORE_POS.get());
                player.displayClientMessage(Component.translatable("message.create_headsupdisplay.link_clear"), true);
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos storedCore = stack.get(ModDataComponents.LINKED_OMNI_CORE_POS.get());

        // 无已存核心坐标：右键 OmniCore 记录坐标
        if (storedCore == null) {
            BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof OmniCoreBlockEntity) {
                if (!level.isClientSide) {
                    stack.set(ModDataComponents.LINKED_OMNI_CORE_POS.get(), clickedPos);
                    player.displayClientMessage(Component.translatable("message.create_headsupdisplay.link_set", clickedPos.toShortString()), true);
                }
                return InteractionResult.SUCCESS;
            }
            return super.useOn(context);
        }

        // 有已存核心坐标：右键 Terminal → 放置并绑定
        boolean isTerminal = level.getBlockState(clickedPos).getBlock() instanceof DisplayTerminalBlock;
        BlockPos terminalPos = isTerminal ? clickedPos : null;

        // 放置方块
        InteractionResult result = super.useOn(context);

        if (!level.isClientSide && result.consumesAction()) {
            // 找到刚放置的 LinkBlockEntity（在点击面方向）
            BlockPos placePos = clickedPos.relative(context.getClickedFace());
            BlockEntity placedBe = level.getBlockEntity(placePos);
            if (placedBe instanceof LinkBlockEntity linkBe) {
                linkBe.setOmniCorePos(storedCore);
                if (terminalPos != null) {
                    linkBe.setTerminalPos(terminalPos);
                    linkBe.onPlaced(); // 通知 OmniCore 绑定
                }
            }
            // 清除物品上的坐标（仅消耗放置的那一个物品）
            ItemStack remaining = player.getItemInHand(context.getHand());
            if (!remaining.isEmpty()) {
                remaining.remove(ModDataComponents.LINKED_OMNI_CORE_POS.get());
            }
            player.displayClientMessage(Component.translatable("message.create_headsupdisplay.link_success"), true);
        }
        return result;
    }
}