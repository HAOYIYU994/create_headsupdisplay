package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class DigitalMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "digital");
    private static ResourceLocation BG;
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getLegacyId() { return 4; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 130; }
    @Override public int getDefaultHeight() { return 32; }
    @Override public Component getDisplayName() { return Component.translatable("gui.create_headsupdisplay.pro.display_mode.digit"); }
    @Override
    public void registerTextures(TextureRegistrar reg) {
        var img = new NativeImage(NativeImage.Format.RGBA,120,28,false);
        for (int y=0; y<28; y++) for (int x=0; x<120; x++) img.setPixelRGBA(x,y,0x00000000);
        for (int x=0; x<120; x++) img.setPixelRGBA(x,27,0x66448844);
        BG = reg.register("digital_bg", img);
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        float val=GaugeUtil.parseFloat(dataValues.get(0)); String unit=config.getUnit();
        String num=String.format("%.1f",val); int nw=font.width(num);
        RenderSystem.enableBlend();
        int dbW=Math.max(nw+30,100);
        if (BG!=null) g.blit(BG,w/2-dbW/2,h/2-14,dbW,28,0,0,120,28,120,28);
        RenderSystem.disableBlend();
        g.drawString(font,num,w/2-nw/2,h/2-6,0xFF00FF44);
        if (!unit.isEmpty()) g.drawString(font,unit,w/2+dbW/2+4,h/2-2,0xFF448844);
    }
}
