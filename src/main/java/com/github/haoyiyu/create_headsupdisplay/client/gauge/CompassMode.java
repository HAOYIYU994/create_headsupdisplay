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

public class CompassMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "compass");
    private static ResourceLocation FACE, NEEDLE;
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 100; }
    @Override public int getDefaultHeight() { return 100; }
    @Override
    public List<ConfigParamDescriptor> getConfigParameters() {
        return List.of(ConfigParamDescriptor.of("max",ConfigParamType.FLOAT,360f), ConfigParamDescriptor.of("min",ConfigParamType.FLOAT,0f), ConfigParamDescriptor.of("unit",ConfigParamType.STRING,"°"));
    }
    @Override
    public void registerTextures(TextureRegistrar reg) {
        int s=128,cx=64,cy=64,r=58;
        var f=new NativeImage(NativeImage.Format.RGBA,s,s,false);
        for (int y=0;y<s;y++) for (int x=0;x<s;x++) f.setPixelRGBA(x,y,0x00000000);
        GaugeUtil.ringPixels(f,cx,cy,r,3,0x88CCCCDD);
        for (int i=0;i<72;i++) { double rad=Math.toRadians(i*5.0); boolean m=i%18==0,md=i%9==0&&!m; double r1=r-(m?16:(md?10:6)); int x1=cx+(int)(Math.cos(rad)*r1),y1=cy-(int)(Math.sin(rad)*r1),x2=cx+(int)(Math.cos(rad)*r),y2=cy-(int)(Math.sin(rad)*r); GaugeUtil.drawThickLine(f,x1,y1,x2,y2,m?2:1,m?0xFFFFCC44:(md?0xCCAAAACC:0x66555577)); }
        FACE=reg.register("compass_face",f);
        var n=new NativeImage(NativeImage.Format.RGBA,8,52,false);
        for (int y=0;y<52;y++) { float t=y/52f; int hw=1+(int)(t*2); boolean north=y<26; int col=north?0xFFFF4444:0xFFCCCCCC; for (int x=4-hw;x<=4+hw;x++) if(x>=0&&x<8)n.setPixelRGBA(x,y,col); }
        GaugeUtil.fillCircle(n,4,26,3,0xFF888888);
        NEEDLE=reg.register("compass_needle",n);
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h, int color, int alpha) {
        float val=GaugeUtil.parseFloat(dataValues.get(0)), pct=Mth.clamp(val/360f,0f,1f);
        int r=Math.min(w,h)/2-4,cx=w/2,cy=h/2;
        RenderSystem.enableBlend();
        if (FACE!=null) g.blit(FACE,cx-r,cy-r,r*2,r*2,0,0,128,128,128,128);
        g.pose().pushPose(); g.pose().translate(cx,cy,0); g.pose().mulPose(Axis.ZP.rotationDegrees(-pct*360f));
        if (NEEDLE!=null) g.blit(NEEDLE,-4,-52,8,52,0,0,8,52,8,52);
        g.pose().popPose();
        g.fill(cx-2,cy-2,cx+3,cy+3,0x88FFFF44);
        String vs=String.format("%.0f°",val%360); int tw=font.width(vs); g.drawString(font,vs,cx-tw/2,cy+r/2+8,0xFFFFCC44);
        RenderSystem.disableBlend();
    }
}
