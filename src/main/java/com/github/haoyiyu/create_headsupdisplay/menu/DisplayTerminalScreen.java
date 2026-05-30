package com.github.haoyiyu.create_headsupdisplay.menu;

import com.github.haoyiyu.create_headsupdisplay.config.HudPositionConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DisplayTerminalScreen extends AbstractContainerScreen<DisplayTerminalMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("create_headsupdisplay", "textures/gui/terminal.png");
    private int dragX, dragY;

    public DisplayTerminalScreen(DisplayTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.dragX = menu.getConfig().posX();
        this.dragY = menu.getConfig().posY();
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            menu.updateConfig(new HudPositionConfig(dragX, dragY));
        }).bounds(leftPos + 80, topPos + 50, 40, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(this.font, "Drag the red square", leftPos + 10, topPos + 20, 0xFFFFFF, true);
        graphics.fill(leftPos + dragX, topPos + dragY, leftPos + dragX + 10, topPos + dragY + 10, 0x88FF0000);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isHovering(leftPos + this.dragX, topPos + this.dragY, 10, 10, mouseX, mouseY)) {
            this.dragX = (int) (mouseX - leftPos);
            this.dragY = (int) (mouseY - topPos);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}