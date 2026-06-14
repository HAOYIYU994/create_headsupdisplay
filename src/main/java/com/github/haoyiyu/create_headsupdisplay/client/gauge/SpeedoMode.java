package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.List;

public class SpeedoMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "speedometer");
    private static ResourceLocation FACE, NEEDLE;
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 120; }
    @Override public int getDefaultHeight() { return 80; }
    @Override
    public List<ConfigParamDescriptor> getConfigParameters() {
        return List.of(ConfigParamDescriptor.of("max",ConfigParamType.FLOAT,200f), ConfigParamDescriptor.of("min",ConfigParamType.FLOAT,0f), ConfigParamDescriptor.of("unit",ConfigParamType.STRING,"km/h"));
    }
    @Override
    public void registerTextures(TextureRegistrar reg) {
        var f=new NativeImage(NativeImage.Format.RGBA,100,60,false);
        for (int y=0;y<60;y++) for (int x=0;x<100;x++) f.setPixelRGBA(x,y,0x00000000);
        int cx=50,cy=60,r=55; GaugeUtil.ringPixels(f,cx,cy,r,2,0x88CCCCDD);
        for (int i=0;i<=18;i++) { double rad=Math.toRadians(180+i*180.0/18.0); boolean m=i%3==0; double r1=r-(m?10:6); int x1=cx+(int)(Math.cos(rad)*r1),y1=cy-(int)(Math.sin(rad)*r1),x2=cx+(int)(Math.cos(rad)*r),y2=cy-(int)(Math.sin(rad)*r); GaugeUtil.drawThickLine(f,x1,y1,x2,y2,1,m?0xFFCCCCDD:0x66555577); }
        FACE=reg.register("speedo_face",f);
        var n=new NativeImage(NativeImage.Format.RGBA,4,50,false);
        for (int y=0;y<50;y++) { int col=y<17?0xFFFF4444:0xFFFFCC44; for (int x=0;x<4;x++) n.setPixelRGBA(x,y,col); }
        NEEDLE=reg.register("speedo_needle",n);
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        float val=GaugeUtil.parseFloat(dataValues.get(0)), pct=Mth.clamp(config.getMax()>0?val/config.getMax():0f,0f,1f);
        int cx=w/2,cy=h-10,r=Math.min(w,cy)-4;
        RenderSystem.enableBlend();
        if (FACE!=null) g.blit(FACE,cx-50,cy-55,100,60,0,0,100,60,100,60);
        g.pose().pushPose(); g.pose().translate(cx,cy,0); g.pose().mulPose(Axis.ZP.rotationDegrees(180f+pct*180f));
        if (NEEDLE!=null) g.blit(NEEDLE,-2,-48,4,48,0,0,4,50,4,50);
        g.pose().popPose();
        g.fill(cx-3,cy-3,cx+4,cy+4,0x88CCCCDD);
        String num=String.format("%.0f %s",val,config.getUnit()); int nw=font.width(num);
        g.fill(cx-nw/2-4,cy+6,cx+nw/2+4,cy+18,0x88111122); g.drawString(font,num,cx-nw/2,cy+8,0xFF44CC44);
        RenderSystem.disableBlend();
    }
}
