package com.github.haoyiyu.create_headsupdisplay.item;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlock;
import com.github.haoyiyu.create_headsupdisplay.registration.ModDataComponents;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class HeadMountDisplayItem extends Item implements Equipable {

    private static boolean predicateRegistered = false;

    public HeadMountDisplayItem(Properties properties) {
        super(properties);
        // 注册谓词，让头盔被识别为护目镜（仅注册一次）
        if (!predicateRegistered) {
            GogglesItem.addIsWearingPredicate(player ->
                    player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HeadMountDisplayItem
            );
            predicateRegistered = true;
        }
    }

    public static BlockPos getBoundTerminalPos(ItemStack stack) {
        return stack.get(ModDataComponents.BOUND_TERMINAL_POS.get());
    }

    public static void bindTerminal(ItemStack stack, BlockPos pos) {
        stack.set(ModDataComponents.BOUND_TERMINAL_POS.get(), pos);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return super.useOn(context);

        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && be.getBlockState().getBlock() instanceof DisplayTerminalBlock) {
            if (!level.isClientSide) {
                ItemStack stack = context.getItemInHand();
                bindTerminal(stack, pos);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.create_headsupdisplay.bound", pos.toShortString()),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
}