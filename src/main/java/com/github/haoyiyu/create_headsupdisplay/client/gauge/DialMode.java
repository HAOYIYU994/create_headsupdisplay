package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.List;

public class DialMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "dial");
    private static ResourceLocation FACE, NEEDLE;
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getLegacyId() { return 3; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 90; }
    @Override public int getDefaultHeight() { return 90; }
    @Override public Component getDisplayName() { return Component.translatable("gui.create_headsupdisplay.pro.display_mode.dial"); }
    @Override
    public void registerTextures(TextureRegistrar reg) {
        var face = new NativeImage(NativeImage.Format.RGBA,128,128,false);
        for (int y=0; y<128; y++) for (int x=0; x<128; x++) face.setPixelRGBA(x,y,0x00000000);
        int cx=64,cy=64,r=60;
        GaugeUtil.ringPixels(face,cx,cy,r+1,2,0x88CCCCDD);
        for (int i=0; i<=36; i++) {
            double rad=Math.toRadians(-120+i*240.0/36.0); boolean m=i%9==0;
            double r1=r-(m?10:6),r2=r;
            int x1=cx+(int)(Math.cos(rad)*r1),y1=cy-(int)(Math.sin(rad)*r1);
            int x2=cx+(int)(Math.cos(rad)*r2),y2=cy-(int)(Math.sin(rad)*r2);
            GaugeUtil.drawThickLine(face,x1,y1,x2,y2,1,m?0xFFCCCCDD:0x66555577);
        }
        FACE=reg.register("dial_face",face);
        var n = new NativeImage(NativeImage.Format.RGBA,12,72,false);
        for (int y=0; y<72; y++) { float t=y/72f; int hw=1+(int)(t*3); for (int x=6-hw; x<=6+hw; x++) if(x>=0&&x<12) n.setPixelRGBA(x,y,0xFFFF4444); }
        GaugeUtil.fillCircle(n,6,68,4,0xFFFF4444); GaugeUtil.fillCircle(n,6,68,2,0x88FF8888);
        NEEDLE=reg.register("dial_needle",n);
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h, int color, int alpha) {
        float val=GaugeUtil.parseFloat(dataValues.get(0)), mv=config.getMax(); String u=config.getUnit();
        float pct=Mth.clamp(mv>0?val/mv:0f,0f,1f);
        int r=Math.min(w,h)/2-4, cx=w/2, cy=h/2+4;
        RenderSystem.enableBlend();
        if (FACE!=null) g.blit(FACE,cx-r,cy-r,r*2,r*2,0,0,128,128,128,128);
        float a=(float)Math.toRadians(-120+pct*240);
        g.pose().pushPose(); g.pose().translate(cx,cy,0); g.pose().mulPose(Axis.ZP.rotationDegrees((float)Math.toDegrees(a)+90f));
        if (NEEDLE!=null) g.blit(NEEDLE,-6,-48,12,72,0,0,12,72,12,72);
        g.pose().popPose();
        g.fill(cx-2,cy-2,cx+3,cy+3,0x88CCCCDD);
        String vs=String.format("%.0f%s",val,u); int tw=font.width(vs); g.drawString(font,vs,cx-tw/2,cy+r/2+2,0xFFFFFFFF);
        RenderSystem.disableBlend();
    }
}
