package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.List;

/**
 * Artificial horizon / attitude indicator.
 * 2 data sources: [0]=pitch (-90..90), [1]=roll (degrees).
 *
 * Design:
 *   Moving layer (rotated by roll, shifted by pitch):
 *     - Sky/ground fills + horizon line
 *     - Infinite pitch ladder ticks + labels
 *   Fixed layer (drawn after):
 *     - Aircraft symbol at center
 *     - Yellow reference wings
 *     - Outer ring (roll scale + cardinal marks) — clips everything naturally
 */
public class GyroMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "gyro");

    @Override public ResourceLocation getId() { return ID; }
    @Override public int getMinDataSourceCount() { return 2; }
    @Override public int getMaxDataSourceCount() { return 2; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 110; }
    @Override public int getDefaultHeight() { return 110; }
    @Override public List<ConfigParamDescriptor> getConfigParameters() { return List.of(); }

    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        float pitch = GaugeUtil.parseFloat(dataValues.size()>0 ? dataValues.get(0) : "0");
        float roll  = dataValues.size()>1 ? GaugeUtil.parseFloat(dataValues.get(1)) : 0;
        int cx = w/2, cy = h/2, r = Math.min(w,h)/2 - 6;
        float pxPerDeg = 2.0f;
        int degStep = 10;
        RenderSystem.enableBlend();

        // ═══════════════════════════════════════════
        //  MOVING LAYER — rotated by roll, shifted by pitch
        // ═══════════════════════════════════════════
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-roll));
        g.pose().translate(0, pitch * pxPerDeg, 0);

        // Horizon line only — no fills (ring clips the ends visually)
        g.fill(-r*4, -1, r*4, 2, 0xFFFFFFFF);

        // Pitch ladder — continuous infinite grid
        int baseDeg = (Math.round(pitch) / degStep) * degStep;
        int visibleTicks = (int)(r * 1.5f / (degStep * pxPerDeg)) + 3;
        for (int i = -visibleTicks; i <= visibleTicks; i++) {
            int deg = baseDeg + i * degStep;
            int y = (int)(-deg * pxPerDeg); // local Y (center of sky/ground is 0)
            boolean major = deg % 30 == 0;
            int len = major ? r + 4 : r/2 + 2;
            g.fill(-len, y, len + 1, y + 1, 0xCCFFFFFF);
            if (major) {
                g.drawString(font, String.valueOf(deg), len - font.width(String.valueOf(deg)) - 2, y - 5, 0xCCFFFFFF);
            }
        }

        g.pose().popPose();

        // ═══════════════════════════════════════════
        //  FIXED LAYER
        // ═══════════════════════════════════════════

        // Aircraft wings (orange, fixed at center)
        int wc = 0xFFCC6600;
        g.fill(cx - 10, cy - 1, cx - 3, cy + 2, wc);   // left wing
        g.fill(cx + 4, cy - 1, cx + 11, cy + 2, wc);    // right wing
        g.fill(cx - 1, cy - 8, cx + 2, cy + 9, wc);     // vertical fin
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFCC44); // center dot

        // Yellow reference bar (drawn just behind aircraft, inside ring)
        g.fill(cx - r + 6, cy - 1, cx + r - 5, cy + 2, 0x6666AA00);

        RenderSystem.disableBlend();
    }

    private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        int dx=Math.abs(x2-x1), dy=-Math.abs(y2-y1), sx=x1<x2?1:-1, sy=y1<y2?1:-1, err=dx+dy;
        while(true){g.fill(x1,y1,x1+1,y1+1,c);if(x1==x2&&y1==y2)break;int e2=2*err;if(e2>=dy){err+=dy;x1+=sx;}if(e2<=dx){err+=dx;y1+=sy;}}
    }
}
