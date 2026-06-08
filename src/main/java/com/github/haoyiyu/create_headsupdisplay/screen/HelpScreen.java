package com.github.haoyiyu.create_headsupdisplay.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HelpScreen extends Screen {
    private final Screen parent;

    public HelpScreen(Screen parent) {
        super(Component.translatable("gui.create_headsupdisplay.help.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.back"), b -> {
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 30, height - 40, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int y = 36;
        for (int i = 1; i <= 15; i++) {
            String key = "gui.create_headsupdisplay.help.line" + i;
            String text = Component.translatable(key).getString();
            if (text.equals(key)) continue; // skip missing keys
            g.drawString(font, text, 20, y, 0xCCCCCC);
            y += 14;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
