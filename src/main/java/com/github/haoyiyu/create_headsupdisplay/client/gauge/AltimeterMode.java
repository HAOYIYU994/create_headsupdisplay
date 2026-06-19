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

public class AltimeterMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "altimeter");
    private static ResourceLocation SCALE;
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getLegacyId() { return 2; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 60; }
    @Override public int getDefaultHeight() { return 140; }
    @Override public Component getDisplayName() { return Component.translatable("gui.create_headsupdisplay.pro.display_mode.alt"); }
    @Override
    public void registerTextures(TextureRegistrar reg) {
        var img = new NativeImage(NativeImage.Format.RGBA, 48, 160, false);
        for (int y=0; y<160; y++) for (int x=0; x<48; x++) img.setPixelRGBA(x,y,0x00000000);
        for (int i=0; i<=50; i++) {
            int y=159-i*159/50; boolean m=i%10==0; int tl=m?16:8, tc=m?0xFFCCCCDD:0x66555577;
            for (int t=0; t<tl; t++) img.setPixelRGBA(46-t,y,tc);
        }
        for (int y=0; y<160; y++) img.setPixelRGBA(47,y,0x44666688);
        SCALE = reg.register("altimeter_scale", img);
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h, int color, int alpha) {
        float val = GaugeUtil.parseFloat(dataValues.get(0));
        float min=config.getMin(), max=config.getMax(); String u=config.getUnit();
        if (max<=min) max=min+100f;
        float pct = Mth.clamp((val-min)/(max-min),0f,1f);
        int sw=48, sh=h-10, sx=4, sy=5;
        RenderSystem.enableBlend();
        if (SCALE!=null) { RenderSystem.setShaderTexture(0,SCALE); g.blit(SCALE,sx,sy,sw,sh,0,0,48,160,48,160); }
        int py = sy+(int)((1f-pct)*sh);
        g.fill(sx+sw-20,py-3,sx+sw+4,py+4,0x88FF4444);
        g.fill(sx-2,py-1,sx+sw,py+2,0x4400FF00);
        g.drawString(font,String.format("%.0f%s",val,u),sx+sw+6,py-4,0xFFFFFFFF);
        RenderSystem.disableBlend();
    }
}
