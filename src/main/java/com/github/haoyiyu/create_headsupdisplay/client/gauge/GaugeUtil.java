package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.mojang.blaze3d.platform.NativeImage;

public final class GaugeUtil {
    private GaugeUtil() {}
    public static float parseFloat(String text) {
        String t = text.replaceAll("§[0-9a-fk-or]", "").trim();
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);
        try { return Float.parseFloat(t); } catch (NumberFormatException e) { return 0; }
    }
    public static String extractUnit(String text) {
        String t = text.replaceAll("§[0-9a-fk-or]", "").trim();
        int sp = t.indexOf(' ');
        return sp > 0 ? t.substring(sp + 1).trim() : "";
    }
    public static int niceStep(float rough) {
        if (rough <= 0.1f) return 1;
        int exp = (int) Math.pow(10, (int) Math.log10(rough));
        float mant = rough / exp;
        if (mant < 1.5f) return exp; if (mant < 3.5f) return exp * 2;
        if (mant < 7.5f) return exp * 5; return exp * 10;
    }
    public static void setPixel(NativeImage img, int x, int y, int color) {
        if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) img.setPixelRGBA(x, y, color);
    }
    public static void fillCircle(NativeImage img, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++)
            if (dx*dx + dy*dy <= r*r) setPixel(img, cx+dx, cy+dy, color);
    }
    public static void ringPixels(NativeImage img, int cx, int cy, int r, int thick, int color) {
        for (int dy = -r-thick; dy <= r+thick; dy++) for (int dx = -r-thick; dx <= r+thick; dx++) {
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist >= r && dist < r+thick) setPixel(img, cx+dx, cy+dy, color);
        }
    }
    public static void drawThickLine(NativeImage img, int x1, int y1, int x2, int y2, int thick, int color) {
        int dx = Math.abs(x2-x1), dy = -Math.abs(y2-y1), sx = x1<x2?1:-1, sy = y1<y2?1:-1, err = dx+dy;
        while (true) {
            for (int t = -thick/2; t <= thick/2; t++) { setPixel(img, x1+t, y1, color); setPixel(img, x1, y1+t, color); }
            if (x1==x2 && y1==y2) break;
            int e2 = 2*err; if (e2>=dy) { err+=dy; x1+=sx; } if (e2<=dx) { err+=dx; y1+=sy; }
        }
    }
}
