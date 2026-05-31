package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.client.ClientFrequencySelectionCache;
import com.github.haoyiyu.create_headsupdisplay.menu.FrequencySelectionMenu;
import com.github.haoyiyu.create_headsupdisplay.network.AddRedstoneSourcePayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class FrequencySelectionScreen extends AbstractContainerScreen<FrequencySelectionMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("create_headsupdisplay", "textures/gui/frequency_selection.png");

    private final BlockPos corePos;
    private EditBox nameField;

    public FrequencySelectionScreen(FrequencySelectionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.corePos = ClientFrequencySelectionCache.getAndClear();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 83;

        // 名称输入框
        this.nameField = new EditBox(this.font, this.leftPos + 36, this.topPos + 56, 104, 16, Component.translatable("gui.create_headsupdisplay.signal_name"));
        this.nameField.setMaxLength(32);
        this.addRenderableWidget(this.nameField);

        // 确认按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.create_headsupdisplay.add"),
                btn -> this.onConfirm()
        ).bounds(this.leftPos + 60, this.topPos + 74, 56, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> this.onClose()
        ).bounds(this.leftPos + 120, this.topPos + 74, 48, 20).build());
    }

    private void onConfirm() {
        ItemStack freqItem1 = this.menu.getFrequencyItem1();
        ItemStack freqItem2 = this.menu.getFrequencyItem2();
        if (freqItem1.isEmpty() && freqItem2.isEmpty()) return;

        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) {
            if (!freqItem1.isEmpty() && !freqItem2.isEmpty()) {
                name = freqItem1.getHoverName().getString() + " + " + freqItem2.getHoverName().getString();
            } else if (!freqItem1.isEmpty()) {
                name = freqItem1.getHoverName().getString();
            } else {
                name = freqItem2.getHoverName().getString();
            }
        }
        PacketDistributor.sendToServer(new AddRedstoneSourcePayload(corePos, name, freqItem1, freqItem2));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 槽位底色：槽1红，槽2蓝，始终渲染在物品下方
        int slotSize = 16;
        graphics.fill(this.leftPos + 62, this.topPos + 38, this.leftPos + 62 + slotSize, this.topPos + 38 + slotSize, 0x88FF0000);
        graphics.fill(this.leftPos + 98, this.topPos + 38, this.leftPos + 98 + slotSize, this.topPos + 38 + slotSize, 0x880000FF);
    }
}