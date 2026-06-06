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
 * 链接方块物品。支持两种连接模式：
 * 1. OmniCore → Display Terminal（右键核心→右键终端）
 * 2. OmniCore ← Monitor（右键核心→右键雷达监视器）
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

        // 有已存核心坐标：右键终端或监视器
        boolean isTerminal = level.getBlockState(clickedPos).getBlock() instanceof DisplayTerminalBlock;
        boolean isMonitor = isRadarMonitor(level.getBlockEntity(clickedPos));

        if (!isTerminal && !isMonitor) {
            return super.useOn(context);
        }

        // 监视器连接：放置 LinkBlock 并绑定（和终端逻辑一致）
        if (isMonitor) {
            InteractionResult result = super.useOn(context);
            if (!level.isClientSide && result.consumesAction()) {
                BlockPos placePos = clickedPos.relative(context.getClickedFace());
                BlockEntity placedBe = level.getBlockEntity(placePos);
                if (placedBe instanceof LinkBlockEntity linkBe) {
                    linkBe.setOmniCorePos(storedCore);
                    linkBe.setMonitorPos(clickedPos);
                    linkBe.onPlaced();
                }
                ItemStack remaining = player.getItemInHand(context.getHand());
                if (!remaining.isEmpty()) {
                    remaining.remove(ModDataComponents.LINKED_OMNI_CORE_POS.get());
                }
                player.displayClientMessage(Component.translatable(
                    "message.create_headsupdisplay.link_monitor", storedCore.toShortString()), true);
            }
            return result;
        }

        // 终端连接：放置 LinkBlock 并绑定
        InteractionResult result = super.useOn(context);
        if (!level.isClientSide && result.consumesAction()) {
            BlockPos placePos = clickedPos.relative(context.getClickedFace());
            BlockEntity placedBe = level.getBlockEntity(placePos);
            if (placedBe instanceof LinkBlockEntity linkBe) {
                linkBe.setOmniCorePos(storedCore);
                linkBe.setTerminalPos(clickedPos);
                linkBe.onPlaced();
            }
            ItemStack remaining = player.getItemInHand(context.getHand());
            if (!remaining.isEmpty()) {
                remaining.remove(ModDataComponents.LINKED_OMNI_CORE_POS.get());
            }
            player.displayClientMessage(Component.translatable("message.create_headsupdisplay.link_success"), true);
        }
        return result;
    }

    /** 软检测是否为雷达 Monitor 或 RadarBearing（轴承） */
    private static boolean isRadarMonitor(BlockEntity be) {
        if (be == null) return false;
        try {
            be.getClass().getMethod("getController");
            String name = be.getClass().getName();
            return name.contains("Monitor") || name.contains("RadarBearing");
        } catch (Exception e) {
            return false;
        }
    }
}