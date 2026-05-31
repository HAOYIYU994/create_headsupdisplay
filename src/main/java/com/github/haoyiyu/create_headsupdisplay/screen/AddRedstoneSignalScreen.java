package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.network.AddRedstoneSourcePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @deprecated 已被 FrequencySelectionScreen 替代，保留仅用于向后兼容。
 *             无线红石频率由两个物品共同决定，此界面仅取主手物品做两次填入。
 */
@Deprecated
public class AddRedstoneSignalScreen extends Screen {
    private final BlockPos corePos;
    private final OmniCoreScreen parent;
    private EditBox nameField;
    private ItemStack frequencyItem = ItemStack.EMPTY;

    public AddRedstoneSignalScreen(BlockPos corePos, OmniCoreScreen parent) {
        super(Component.literal("Add Redstone Signal"));
        this.corePos = corePos;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        nameField = new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, Component.literal("Name"));
        nameField.setMaxLength(32);
        addRenderableWidget(nameField);
        // 物品槽简化（已废弃）：用主手物品作为两个频率物品
        addRenderableWidget(Button.builder(Component.literal("Click to set frequency item"), b -> {
            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            if (!held.isEmpty()) {
                frequencyItem = held.copy();
                frequencyItem.setCount(1);
                b.setMessage(Component.literal("Item: " + held.getHoverName()));
            } else {
                b.setMessage(Component.literal("No item selected"));
            }
        }).bounds(width / 2 - 100, height / 2 - 40, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            String name = nameField.getValue();
            if (name.isEmpty()) name = frequencyItem.getHoverName().getString();
            if (!frequencyItem.isEmpty()) {
                // 旧界面取同一物品作为两个频率物品
                PacketDistributor.sendToServer(new AddRedstoneSourcePayload(corePos, name, frequencyItem, frequencyItem.copy()));
                parent.addSource(name);
                minecraft.setScreen(parent);
            }
        }).bounds(width / 2 - 40, height / 2 + 20, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, "Signal Name:", width / 2 - 100, height / 2 - 20, 0xFFFFFF);
        graphics.drawString(font, "Frequency Item:", width / 2 - 100, height / 2 - 50, 0xFFFFFF);
        if (!frequencyItem.isEmpty()) {
            graphics.renderItem(frequencyItem, width / 2 - 20, height / 2 - 35);
        }
    }
}