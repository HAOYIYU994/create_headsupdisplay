package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.function.BiConsumer;

/**
 * 仪表盘纹理：用 NativeImage 逐像素绘制，注册为动态纹理。
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = CreateHeadsUpDisplay.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GaugeTextures {

    public static ResourceLocation DIAL_FACE;
    public static ResourceLocation DIAL_NEEDLE;
    public static ResourceLocation ALTIMETER_SCALE;
    public static ResourceLocation BAR_FRAME;
    public static ResourceLocation DIGITAL_BG;

    private static void register(String name, BiConsumer<NativeImage, Integer> painter, int w, int h) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setPixelRGBA(x, y, 0x00000000); // transparent

        painter.accept(img, w);

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "gauge/" + name);
        Minecraft.getInstance().getTextureManager().register(rl, tex);

        switch (name) {
            case "dial_face" -> DIAL_FACE = rl;
            case "dial_needle" -> DIAL_NEEDLE = rl;
            case "altimeter_scale" -> ALTIMETER_SCALE = rl;
            case "bar_frame" -> BAR_FRAME = rl;
            case "digital_bg" -> DIGITAL_BG = rl;
        }
    }

    @SubscribeEvent
    public static void onRegister(final RegisterClientReloadListenersEvent event) {
        // 圆盘表面 128×128 — 仅刻度环，透明背景
        register("dial_face", (img, w) -> {
            int cx = 64, cy = 64, r = 60;
            // 细外圈
            ringPixels(img, cx, cy, r + 1, 2, 0x88CCCCDD);
            // 刻度线 (240°弧)
            for (int i = 0; i <= 36; i++) {
                double rad = Math.toRadians(-120 + i * 240.0 / 36.0);
                boolean major = i % 9 == 0;
                double r1 = r - (major ? 10 : 6);
                double r2 = r;
                int x1 = cx + (int)(Math.cos(rad) * r1);
                int y1 = cy - (int)(Math.sin(rad) * r1);
                int x2 = cx + (int)(Math.cos(rad) * r2);
                int y2 = cy - (int)(Math.sin(rad) * r2);
                int tc = major ? 0xFFCCCCDD : 0x66555577;
                drawThickLine(img, x1, y1, x2, y2, 1, tc);
            }
        }, 128, 128);

        // 指针 12×72（红色，细长锥形）
        register("dial_needle", (img, w) -> {
            int h = 72;
            for (int y = 0; y < h; y++) {
                float t = y / (float)h;
                int halfW = 1 + (int)(t * 3);
                for (int x = w/2 - halfW; x <= w/2 + halfW; x++) {
                    if (x >= 0 && x < w) img.setPixelRGBA(x, y, 0xFFFF4444);
                }
            }
            // 底部小圆
            fillCircle(img, w/2, h - 4, 4, 0xFFFF4444);
            fillCircle(img, w/2, h - 4, 2, 0x88FF8888);
        }, 12, 72);

        // 高度计标尺 48×160 — 仅刻度线，透明背景
        register("altimeter_scale", (img, w) -> {
            int h = 160;
            // 刻度线 (51 条)
            for (int i = 0; i <= 50; i++) {
                int y = h - 1 - i * (h - 1) / 50;
                boolean major = i % 10 == 0;
                int tickLen = major ? 16 : 8;
                int tc = major ? 0xFFCCCCDD : 0x66555577;
                for (int t = 0; t < tickLen; t++)
                    img.setPixelRGBA(w - 2 - t, y, tc);
            }
            // 右侧细线
            for (int y = 0; y < h; y++) img.setPixelRGBA(w - 1, y, 0x44666688);
        }, 48, 160);

        // 进度条框架 180×28 — 仅边框，透明内部
        register("bar_frame", (img, w) -> {
            int h = 28;
            // 细边框
            for (int x = 0; x < w; x++) {
                img.setPixelRGBA(x, 0, 0x88666688);
                img.setPixelRGBA(x, h-1, 0x88666688);
            }
            for (int y = 0; y < h; y++) {
                img.setPixelRGBA(0, y, 0x88666688);
                img.setPixelRGBA(w-1, y, 0x88666688);
            }
            // 刻度小点
            for (int i = 1; i < 10; i++) {
                int x = i * w / 10;
                img.setPixelRGBA(x, h - 2, 0x55555577);
            }
        }, 180, 28);

        // 数字背景 120×28 — 仅下划线
        register("digital_bg", (img, w) -> {
            int h = 28;
            for (int x = 0; x < w; x++) img.setPixelRGBA(x, h - 1, 0x66448844);
        }, 120, 28);
    }

    // ====== 绘图工具 ======

    /** 画圆环（仅边框像素） */
    private static void ringPixels(NativeImage img, int cx, int cy, int r, int thick, int color) {
        for (int dy = -r - thick; dy <= r + thick; dy++)
            for (int dx = -r - thick; dx <= r + thick; dx++) {
                double dist = Math.sqrt(dx*dx + dy*dy);
                if (dist >= r && dist < r + thick)
                    setPixel(img, cx + dx, cy + dy, color);
            }
    }

    private static void fillCircle(NativeImage img, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++)
            for (int dx = -r; dx <= r; dx++)
                if (dx*dx + dy*dy <= r*r)
                    setPixel(img, cx + dx, cy + dy, color);
    }

    private static void drawThickLine(NativeImage img, int x1, int y1, int x2, int y2, int thickness, int color) {
        int dx = Math.abs(x2 - x1), dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx + dy, e2;
        while (true) {
            for (int t = -thickness/2; t <= thickness/2; t++) {
                setPixel(img, x1 + t, y1, color);
                setPixel(img, x1, y1 + t, color);
            }
            if (x1 == x2 && y1 == y2) break;
            e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    private static void setPixel(NativeImage img, int x, int y, int color) {
        if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight())
            img.setPixelRGBA(x, y, color);
    }
}
