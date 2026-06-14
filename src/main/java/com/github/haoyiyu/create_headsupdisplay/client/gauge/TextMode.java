package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class TextMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "text");
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getLegacyId() { return 0; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return false; }
    @Override public int getDefaultWidth() { return 80; }
    @Override public int getDefaultHeight() { return 20; }
    @Override public Component getDisplayName() { return Component.translatable("gui.create_headsupdisplay.pro.display_mode.text"); }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        String text = dataValues.isEmpty() ? "" : dataValues.get(0);
        g.drawString(font, text, 0, 4, 0xFFFFFFFF, true);
    }
}
