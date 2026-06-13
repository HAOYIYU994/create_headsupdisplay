package com.github.haoyiyu.create_headsupdisplay.item;

import com.github.haoyiyu.create_headsupdisplay.block.NbtReaderBlock;
import com.github.haoyiyu.create_headsupdisplay.registration.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class PluginBaseItem extends Item {
    public PluginBaseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos probePos = stack.get(ModDataComponents.LINKED_PROBE_POS.get());
        if (probePos != null) {
            tooltip.add(Component.translatable("tooltip.create_headsupdisplay.plugin_base.probe",
                    probePos.toShortString()).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        // Shift+右键：清除已记录的读取器
        if (player.isShiftKeyDown() && stack.has(ModDataComponents.LINKED_PROBE_POS.get())) {
            if (!level.isClientSide) {
                stack.remove(ModDataComponents.LINKED_PROBE_POS.get());
                player.displayClientMessage(Component.translatable("message.create_headsupdisplay.probe_clear"), true);
            }
            return InteractionResult.SUCCESS;
        }

        // 右键 DataProbe：记录读取器位置
        if (level.getBlockState(clickedPos).getBlock() instanceof NbtReaderBlock) {
            if (!level.isClientSide) {
                stack.set(ModDataComponents.LINKED_PROBE_POS.get(), clickedPos);
                player.displayClientMessage(Component.translatable(
                        "message.create_headsupdisplay.probe_recorded", clickedPos.toShortString()), true);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
