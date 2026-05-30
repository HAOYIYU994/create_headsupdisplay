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

    // 存储 / 读取绑定的终端位置（保持不变）
    public static BlockPos getBoundTerminalPos(ItemStack stack) {
        Long posLong = stack.get(ModDataComponents.BOUND_TERMINAL_POS.get());
        return posLong != null ? BlockPos.of(posLong) : null;
    }

    public static void bindTerminal(ItemStack stack, BlockPos pos) {
        stack.set(ModDataComponents.BOUND_TERMINAL_POS.get(), pos.asLong());
    }

    // 右键点击方块时的行为（绑定终端）
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && be.getBlockState().getBlock() instanceof DisplayTerminalBlock) {
            if (!level.isClientSide) {
                ItemStack stack = context.getItemInHand();
                bindTerminal(stack, pos);
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Glasses bound to terminal at " + pos.toShortString()),
                            true
                    );
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    // 实现 Equipable 接口要求的方法：指定装备槽位为头盔
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    // 实现 Equipable 接口的 use 方法，调用原版的交换装备逻辑（支持右键装备 + 拖拽装备）
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
}