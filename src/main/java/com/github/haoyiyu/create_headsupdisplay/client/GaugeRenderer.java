package com.github.haoyiyu.create_headsupdisplay.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 用纹理渲染各显示模式 */
public class GaugeRenderer {

    /** 模式1：进度条 */
    public static void renderBar(GuiGraphics g, Font font, String text, int color,
                                  float maxVal, String unit, int w, int h) {
        float val = parseFloat(text);
        float pct = Mth.clamp(maxVal > 0 ? val / maxVal : 0f, 0f, 1f);

        int barW = w - 40, barH = 20;
        int bx = w/2 - barW/2, by = h/2 - barH/2;

        // 细边框 + 填充
        RenderSystem.enableBlend();
        g.blit(GaugeTextures.BAR_FRAME, bx, by, barW, barH, 0, 0, 180, 28, 180, 28);
        int fillColor = pct > 0.75f ? 0x8844CC44 : (pct > 0.25f ? 0x88CCAA44 : 0x88CC4444);
        g.fill(bx + 1, by + 1, bx + 1 + (int)((barW - 2) * pct), by + barH - 1, fillColor);
        RenderSystem.disableBlend();

        // 标签
        String label = String.format("%.0f / %.0f%s", val, maxVal, unit);
        int tw = font.width(label);
        g.drawString(font, label, w/2 - tw/2, by + barH + 4, color, true);
    }

    /** 模式2：高度计 */
    public static void renderAltimeter(GuiGraphics g, Font font, String text, int color,
                                        float minVal, float maxVal, String unit, int w, int h) {
        float val = parseFloat(text);
        if (maxVal <= minVal) maxVal = minVal + 100f;
        float range = maxVal - minVal;
        float pct = Mth.clamp((val - minVal) / range, 0f, 1f);

        int scaleW = 48, scaleH = h - 10;
        int sx = 4, sy = 5;

        // 标尺纹理
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, GaugeTextures.ALTIMETER_SCALE);
        g.blit(GaugeTextures.ALTIMETER_SCALE, sx, sy, scaleW, scaleH, 0, 0, 48, 160, 48, 160);

        // 指针（透明箭头）
        int ptrY = sy + (int)((1f - pct) * scaleH);
        int triTip = sx + scaleW - 20;
        g.fill(triTip, ptrY - 3, sx + scaleW + 4, ptrY + 4, 0x88FF4444);
        g.fill(sx - 2, ptrY - 1, sx + scaleW, ptrY + 2, 0x4400FF00);

        // 数值
        String valStr = String.format("%.0f%s", val, unit);
        g.drawString(font, valStr, sx + scaleW + 6, ptrY - 4, color);
        RenderSystem.disableBlend();
    }

    /** 模式3：圆盘表 */
    public static void renderDial(GuiGraphics g, Font font, String text, int color,
                                   float maxVal, String unit, int w, int h) {
        float val = parseFloat(text);
        float pct = Mth.clamp(maxVal > 0 ? val / maxVal : 0f, 0f, 1f);
        int r = Math.min(w, h) / 2 - 4;
        int cx = w / 2, cy = h / 2 + 4;

        RenderSystem.enableBlend();
        // 表盘面
        g.blit(GaugeTextures.DIAL_FACE, cx - r, cy - r, r * 2, r * 2, 0, 0, 128, 128, 128, 128);

        // 指针
        float angle = (float) Math.toRadians(-120 + pct * 240);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) Math.toDegrees(angle) + 90f));
        g.blit(GaugeTextures.DIAL_NEEDLE, -6, -48, 12, 72, 0, 0, 12, 72, 12, 72);
        g.pose().popPose();

        // 中心小点
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0x88CCCCDD);

        // 数值
        String valStr = String.format("%.0f%s", val, unit);
        int tw = font.width(valStr);
        g.drawString(font, valStr, cx - tw/2, cy + r/2 + 2, color);
        RenderSystem.disableBlend();
    }

    /** 模式4：数字表头 */
    public static void renderDigital(GuiGraphics g, Font font, String text, int color,
                                      String unit, int w, int h) {
        float val = parseFloat(text);
        String num = String.format("%.1f", val);
        int nw = font.width(num);

        RenderSystem.enableBlend();
        int dbW = Math.max(nw + 30, 100);
        g.blit(GaugeTextures.DIGITAL_BG, w/2 - dbW/2, h/2 - 14, dbW, 28, 0, 0, 120, 28, 120, 28);
        RenderSystem.disableBlend();

        g.drawString(font, num, w/2 - nw/2, h/2 - 6, 0xFF00FF44);
        if (!unit.isEmpty())
            g.drawString(font, unit, w/2 + dbW/2 + 4, h/2 - 2, 0xFF448844);
    }

    /** 模式5：战斗机 HUD 高度计 — 固定指针，刻度条滚动 */
    public static void renderHudAltimeter(GuiGraphics g, Font font, String text, int color,
                                           float minVal, float maxVal, String unit, int w, int h) {
        float val = parseFloat(text);
        if (maxVal <= minVal) maxVal = minVal + 100f;
        float range = maxVal - minVal;
        float rangeHalf = range / 2f;
        // 让 val 始终在刻度条中间附近：刻度条范围 = [val - rangeHalf, val + rangeHalf]
        float scaleMin = val - rangeHalf;
        float scaleMax = val + rangeHalf;

        int tapeW = w - 20, tapeH = h;
        int tapeX = 4, tapeY = 0;
        int centerY = h / 2;
        int majorStep = niceStep(range / 5f); // 保证刻度间距合适

        // 刻度条背景框
        g.fill(tapeX, tapeY, tapeX + tapeW, tapeY + tapeH, 0x18181818);

        // 滚动刻度
        for (float tick = scaleMin - (scaleMin % majorStep); tick <= scaleMax; tick += majorStep) {
            float pct = (tick - scaleMin) / range;
            int y = tapeY + tapeH - (int)(pct * tapeH);
            boolean major = (Math.abs(tick) % (majorStep * 5) < 0.01f || Math.abs(tick % (majorStep * 5) - majorStep * 5) < 0.01f);
            int tickW = major ? 20 : 10;
            g.fill(tapeX + tapeW - tickW, y, tapeX + tapeW, y + 1, major ? 0xCCCCCCCC : 0x66555555);
            if (major) {
                String ts = String.format("%.0f", tick);
                g.drawString(font, ts, tapeX + 2, y - 4, 0xFF888888);
            }
        }

        // 固定指针（中心三角）
        int ptrY = centerY;
        g.fill(tapeX + tapeW - 24, ptrY - 3, tapeX + tapeW + 4, ptrY + 4, 0xFFFF4444);
        // 水平参考线
        g.fill(tapeX, ptrY, tapeX + tapeW, ptrY + 1, 0x44FFFF44);

        // 数值显示在右边
        String valStr = String.format("%.0f", val);
        int vw = font.width(valStr);
        g.drawString(font, valStr, tapeX + tapeW + 8, centerY - 4, 0xFF00FF00);
        if (!unit.isEmpty())
            g.drawString(font, unit, tapeX + tapeW + 10 + vw, centerY - 2, 0xFF448844);
    }

    /** 取一个合适的刻度步长 (1, 2, 5, 10, 20, 50...) */
    private static int niceStep(float rough) {
        if (rough <= 0.1f) return 1;
        int exp = (int) Math.pow(10, (int) Math.log10(rough));
        float mant = rough / exp;
        if (mant < 1.5f) return exp;
        if (mant < 3.5f) return exp * 2;
        if (mant < 7.5f) return exp * 5;
        return exp * 10;
    }

    /** 从 "123.5 m" 或 "50" 中提取浮点数 */
    private static float parseFloat(String text) {
        String t = text.replaceAll("§[0-9a-fk-or]", "").trim();
        // "123.5 km/h" → "123.5"
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);
        try { return Float.parseFloat(t); }
        catch (NumberFormatException e) { return 0; }
    }

    /** 从 "123.5 km/h" 中提取单位部分（空格后的内容） */
    public static String extractUnit(String text) {
        String t = text.replaceAll("§[0-9a-fk-or]", "").trim();
        int sp = t.indexOf(' ');
        return sp > 0 ? t.substring(sp + 1).trim() : "";
    }
}
