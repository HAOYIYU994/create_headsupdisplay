package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.List;

public class LEDMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "led_array");
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getMinDataSourceCount() { return 2; }
    @Override public int getMaxDataSourceCount() { return 12; }
    @Override public boolean needsNumericData() { return false; }
    @Override public int getDefaultWidth() { return 80; }
    @Override public int getDefaultHeight() { return 60; }
    @Override
    public List<ConfigParamDescriptor> getConfigParameters() {
        return List.of(ConfigParamDescriptor.of("cols",ConfigParamType.INT,4), ConfigParamDescriptor.of("onColor",ConfigParamType.COLOR,0x00FF00), ConfigParamDescriptor.of("offColor",ConfigParamType.COLOR,0x333333), ConfigParamDescriptor.of("ledSize",ConfigParamType.INT,6));
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h, int color, int alpha) {
        int cols=Mth.clamp(config.getInt("cols",4),1,12), on=config.getInt("onColor",0x00FF00), off=config.getInt("offColor",0x333333), sz=Mth.clamp(config.getInt("ledSize",6),3,16);
        int cnt=dataValues.size(), rows=(int)Math.ceil((double)cnt/cols), sx=sz*2+4, sy=sz*2+4;
        int stx=(w-cols*sx)/2+sz, sty=(h-rows*sy)/2+sz;
        g.fill(0,0,w,h,0x88101018);
        for (int i=0;i<cnt;i++) { int col=i%cols,row=i/cols,cx=stx+col*sx,cy=sty+row*sy; boolean pos=isOn(dataValues.get(i)); int c=pos?on:off; if(pos)g.fill(cx-sz-1,cy-sz-1,cx+sz+2,cy+sz+2,0x44000000|(c&0x00FF00)); g.fill(cx-sz,cy-sz,cx+sz+1,cy+sz+1,0xFF000000|(c&0xFFFFFF)); g.fill(cx-sz/3,cy-sz/3,cx+sz/3+1,cy+sz/3+1,pos?0x88FFFFFF:0x44666666); }
    }
    private static boolean isOn(String d) { if(d==null||d.isEmpty())return false; String t=d.replaceAll("§[0-9a-fk-or]","").trim().toLowerCase(); if(t.equals("true")||t.equals("on")||t.equals("yes")||t.equals("1"))return true; try{return Float.parseFloat(t)>0;}catch(NumberFormatException e){return false;} }
}
