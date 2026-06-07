package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.menu.PluginSlotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PluginSlotScreen extends AbstractContainerScreen<PluginSlotMenu> {
    private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public PluginSlotScreen(PluginSlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int yOff = 17; // 上半部分背景
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, yOff);
        for (int row = 0; row < 3; row++)
            g.blit(BG, leftPos, topPos + yOff + row * 18, 0, yOff, imageWidth, 18);
        g.blit(BG, leftPos, topPos + yOff + 54, 0, 125, imageWidth, 97);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
