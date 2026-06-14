package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.List;

public class BarMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "bar");
    private static ResourceLocation BAR_FRAME;
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getLegacyId() { return 1; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 140; }
    @Override public int getDefaultHeight() { return 40; }
    @Override public Component getDisplayName() { return Component.translatable("gui.create_headsupdisplay.pro.display_mode.bar"); }
    @Override
    public void registerTextures(TextureRegistrar reg) {
        var img = new NativeImage(NativeImage.Format.RGBA, 180, 28, false);
        for (int y=0; y<28; y++) for (int x=0; x<180; x++) img.setPixelRGBA(x,y,0x00000000);
        for (int x=0; x<180; x++) { img.setPixelRGBA(x,0,0x88666688); img.setPixelRGBA(x,27,0x88666688); }
        for (int y=0; y<28; y++) { img.setPixelRGBA(0,y,0x88666688); img.setPixelRGBA(179,y,0x88666688); }
        for (int i=1; i<10; i++) { int x=i*180/10; img.setPixelRGBA(x,26,0x55555577); }
        BAR_FRAME = reg.register("bar_frame", img);
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        float val = GaugeUtil.parseFloat(dataValues.get(0));
        float maxVal = config.getMax(); String unit = config.getUnit();
        float pct = Mth.clamp(maxVal>0?val/maxVal:0f,0f,1f);
        int barW=w-40, barH=20, bx=w/2-barW/2, by=h/2-barH/2;
        RenderSystem.enableBlend();
        if (BAR_FRAME!=null) g.blit(BAR_FRAME,bx,by,barW,barH,0,0,180,28,180,28);
        int fc = pct>0.75f?0x8844CC44:(pct>0.25f?0x88CCAA44:0x88CC4444);
        g.fill(bx+1,by+1,bx+1+(int)((barW-2)*pct),by+barH-1,fc);
        RenderSystem.disableBlend();
        String label = String.format("%.0f / %.0f%s",val,maxVal,unit);
        int tw = font.width(label); g.drawString(font,label,w/2-tw/2,by+barH+4,0xFFFFFFFF,true);
    }
}
