package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class HudAltMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "hudalt");
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getLegacyId() { return 5; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 60; }
    @Override public int getDefaultHeight() { return 140; }
    @Override public Component getDisplayName() { return Component.translatable("gui.create_headsupdisplay.pro.display_mode.hudalt"); }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h, int color, int alpha) {
        float val=GaugeUtil.parseFloat(dataValues.get(0)), min=config.getMin(), max=config.getMax(); String u=config.getUnit();
        if (max<=min) max=min+100f; float range=max-min, half=range/2;
        float smin=val-half, smax=val+half;
        int tw=w-20, th=h, tx=4, ty=0, cy=h/2, step=GaugeUtil.niceStep(range/5f);
        g.fill(tx,ty,tx+tw,ty+th,0x18181818);
        for (float t=smin-(smin%step); t<=smax; t+=step) {
            float pct=(t-smin)/range; int y=ty+th-(int)(pct*th);
            boolean maj=(Math.abs(t)%(step*5)<0.01f||Math.abs(t%(step*5)-step*5)<0.01f);
            int tk=maj?20:10; g.fill(tx+tw-tk,y,tx+tw,y+1,maj?0xCCCCCCCC:0x66555555);
            if (maj) g.drawString(font,String.format("%.0f",t),tx+2,y-4,0xFF888888);
        }
        g.fill(tx+tw-24,cy-3,tx+tw+4,cy+4,0xFFFF4444); g.fill(tx,cy,tx+tw,cy+1,0x44FFFF44);
        String vs=String.format("%.0f",val); int vw=font.width(vs);
        g.drawString(font,vs,tx+tw+8,cy-4,0xFF00FF00);
        if (!u.isEmpty()) g.drawString(font,u,tx+tw+10+vw,cy-2,0xFF448844);
    }
}
