package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class XYZMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "xyz");
    @Override public ResourceLocation getId() { return ID; }
    @Override public int getMinDataSourceCount() { return 3; }
    @Override public int getMaxDataSourceCount() { return 3; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 120; }
    @Override public int getDefaultHeight() { return 48; }
    @Override
    public List<ConfigParamDescriptor> getConfigParameters() {
        return List.of(ConfigParamDescriptor.of("labelX",ConfigParamType.STRING,"X"), ConfigParamDescriptor.of("labelY",ConfigParamType.STRING,"Y"), ConfigParamDescriptor.of("labelZ",ConfigParamType.STRING,"Z"), ConfigParamDescriptor.of("unit",ConfigParamType.STRING,""));
    }
    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        String lx=config.getString("labelX","X"),ly=config.getString("labelY","Y"),lz=config.getString("labelZ","Z"),u=config.getUnit();
        String vx=dataValues.size()>0?dataValues.get(0).replaceAll("§[0-9a-fk-or]","").trim():"?";
        String vy=dataValues.size()>1?dataValues.get(1).replaceAll("§[0-9a-fk-or]","").trim():"?";
        String vz=dataValues.size()>2?dataValues.get(2).replaceAll("§[0-9a-fk-or]","").trim():"?";
        float fx=GaugeUtil.parseFloat(vx),fy=GaugeUtil.parseFloat(vy),fz=GaugeUtil.parseFloat(vz);
        int rh=14,x=4,y=2;
        g.fill(0,0,w,h,0x88101018);
        g.fill(x,y,x+w-8,y+rh,0x22442222); g.drawString(font,String.format("%s: %.1f%s",lx,fx,u),x+4,y+3,0xFFFF4444);
        y+=rh+2; g.fill(x,y,x+w-8,y+rh,0x22224422); g.drawString(font,String.format("%s: %.1f%s",ly,fy,u),x+4,y+3,0xFF44FF44);
        y+=rh+2; g.fill(x,y,x+w-8,y+rh,0x22222244); g.drawString(font,String.format("%s: %.1f%s",lz,fz,u),x+4,y+3,0xFF4488FF);
    }
}
