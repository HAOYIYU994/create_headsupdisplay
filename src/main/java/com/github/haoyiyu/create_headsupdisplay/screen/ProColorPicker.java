package com.github.haoyiyu.create_headsupdisplay.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 浮动调色板 — HSV 色相环 + SV 矩形 + 滑块 + HEX + 最近使用
 * 渲染在屏幕上层，不替换当前屏幕。
 */
public class ProColorPicker {
    // 色相环位置和大小（相对于面板）
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 175;
    private static final int WHEEL_CX = 55;
    private static final int WHEEL_CY = 55;
    private static final int WHEEL_OUTER_R = 48;
    private static final int WHEEL_INNER_R = 28;
    private static final int SV_SIZE = WHEEL_INNER_R * 2 - 10;
    private static final int SV_X = WHEEL_CX - SV_SIZE / 2;
    private static final int SV_Y = WHEEL_CY - SV_SIZE / 2;
    private static final int SEGMENTS = 72;

    private int panelX, panelY;
    private boolean visible = false;
    private int draggingTitle = 0; // 0=none, 1=title bar drag
    private int titleDragOffX, titleDragOffY;
    private int dragTarget = 0; // 0=none, 1=hue ring, 2=SV square, 3=alpha bar

    // 颜色值 0-1
    private float hue, saturation, value, alpha;
    private final Consumer<Integer> onConfirm;

    // 最近使用的颜色
    private static final List<Integer> recentColors = new ArrayList<>();
    private static final int MAX_RECENT = 12;

    // 滑块和输入
    private EditBox hexInput;
    private boolean hexFocused = false; // 手动追踪焦点

    public ProColorPicker(int initialColorHex, int initialAlpha, Consumer<Integer> onConfirm) {
        this.onConfirm = onConfirm;
        float[] hsv = rgbToHsv(initialColorHex);
        this.hue = hsv[0]; this.saturation = hsv[1]; this.value = hsv[2];
        this.alpha = initialAlpha / 255f;
    }

    public void show(int x, int y, int colorHex, int alphaVal) {
        this.panelX = Math.max(0, Math.min(x, 800 - PANEL_W));
        this.panelY = Math.max(0, Math.min(y, 600 - PANEL_H));
        loadColor(colorHex, alphaVal);
        this.visible = true;
    }

    /** 切换编辑目标时只更新颜色，不改变面板位置 */
    public void switchTarget(int colorHex, int alphaVal) {
        loadColor(colorHex, alphaVal);
    }

    private void loadColor(int colorHex, int alphaVal) {
        float[] hsv = rgbToHsv(colorHex & 0xFFFFFF);
        this.hue = hsv[0]; this.saturation = hsv[1]; this.value = hsv[2];
        this.alpha = alphaVal / 255f;
        if (hexInput != null) hexInput.setValue(String.format("%06X", colorHex & 0xFFFFFF));
    }

    public void hide() { this.visible = false; }
    public boolean isVisible() { return visible; }

    public void render(GuiGraphics g, net.minecraft.client.gui.Font font, int mouseX, int mouseY) {
        if (!visible) return;

        // 强制关闭混合，确保下方文字不穿透
        RenderSystem.disableBlend();

        // 面板背景
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF222233);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFF6666AA);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFF6666AA);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, 0xFF6666AA);
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF6666AA);

        // 标题栏
        int titleY = panelY + 3;
        g.fill(panelX + 2, titleY, panelX + PANEL_W - 2, titleY + 14, 0xFF334455);
        g.drawCenteredString(font, Component.translatable("gui.create_headsupdisplay.pro.color_picker"), panelX + PANEL_W / 2, titleY + 3, 0xCCCCCC);
        // 关闭按钮
        int closeX = panelX + PANEL_W - 18;
        g.fill(closeX, titleY, closeX + 14, titleY + 14, 0xAAFF4444);
        g.drawString(font, "X", closeX + 4, titleY + 3, 0xFFFFFF);

        int contentTop = panelY + 22;

        // === 色相环 ===
        int wx = panelX + WHEEL_CX;
        int wy = contentTop + WHEEL_CY;
        // 渲染 72 段色相环
        for (int i = 0; i < SEGMENTS; i++) {
            double angle1 = Math.toRadians((i * 360.0 / SEGMENTS) - 90);
            double angle2 = Math.toRadians(((i + 1) * 360.0 / SEGMENTS) - 90);
            int c1 = hsvToRgbInt(i * 360f / SEGMENTS, 1f, 1f);
            int c2 = hsvToRgbInt((i + 1f) * 360f / SEGMENTS, 1f, 1f);
            // 用多个小矩形近似环形扇区
            int steps = 3;
            for (int r = 0; r < steps; r++) {
                float t = (float) r / steps;
                float t2 = (float) (r + 1) / steps;
                double r1 = WHEEL_INNER_R + (WHEEL_OUTER_R - WHEEL_INNER_R) * t;
                double r2 = WHEEL_INNER_R + (WHEEL_OUTER_R - WHEEL_INNER_R) * t2;
                int x1 = (int) (wx + Math.cos(angle1) * r1);
                int y1 = (int) (wy + Math.sin(angle1) * r1);
                int x2 = (int) (wx + Math.cos(angle1) * r2);
                int y2 = (int) (wy + Math.sin(angle1) * r2);
                int x3 = (int) (wx + Math.cos(angle2) * r2);
                int y3 = (int) (wy + Math.sin(angle2) * r2);
                int x4 = (int) (wx + Math.cos(angle2) * r1);
                int y4 = (int) (wy + Math.sin(angle2) * r1);
                int color = r < steps / 2 ? c1 : blendColor(c1, c2);
                // 简单四边形填充
                g.fill(Math.min(x1, Math.min(x2, Math.min(x3, x4))),
                       Math.min(y1, Math.min(y2, Math.min(y3, y4))),
                       Math.max(x1, Math.max(x2, Math.max(x3, x4))),
                       Math.max(y1, Math.max(y2, Math.max(y3, y4))), color);
            }
        }

        // SV 矩形（中心方框）
        for (int sy = 0; sy < SV_SIZE; sy += 3) {
            for (int sx = 0; sx < SV_SIZE; sx += 3) {
                float s = (float) sx / SV_SIZE;
                float v = 1f - (float) sy / SV_SIZE;
                int color = hsvToRgbInt(hue * 360f, s, v);
                g.fill(panelX + SV_X + sx, contentTop + SV_Y + sy,
                       panelX + SV_X + sx + 3, contentTop + SV_Y + sy + 3, 0xFF000000 | color);
            }
        }
        // SV 光标
        int svCx = panelX + SV_X + (int) (saturation * SV_SIZE);
        int svCy = contentTop + SV_Y + (int) ((1f - value) * SV_SIZE);
        g.fill(svCx - 3, svCy - 3, svCx + 4, svCy + 4, 0xFFFFFFFF);
        g.fill(svCx - 2, svCy - 2, svCx + 3, svCy + 3, 0xFF000000);

        // 色相环指示器
        double hAngle = Math.toRadians(hue * 360 - 90);
        int hix = (int) (wx + Math.cos(hAngle) * (WHEEL_INNER_R + WHEEL_OUTER_R) / 2);
        int hiy = (int) (wy + Math.sin(hAngle) * (WHEEL_INNER_R + WHEEL_OUTER_R) / 2);
        g.fill(hix - 3, hiy - 3, hix + 4, hiy + 4, 0xFFFFFFFF);
        g.fill(hix - 2, hiy - 2, hix + 3, hiy + 3, 0xFF000000);

        // === 右侧预览 ===
        int rightX = panelX + 112;
        int ry = contentTop + 5;

        int curRgb = hsvToRgbInt(hue * 360f, saturation, value);
        int curArgb = ((int)(alpha * 255) << 24) | (curRgb & 0xFFFFFF);
        g.fill(rightX, ry, rightX + 76, ry + 30, curArgb);
        ry += 36;

        // HEX 输入
        if (hexInput == null) {
            hexInput = new EditBox(font, rightX, ry - 4, 50, 14, Component.literal("HEX"));
            hexInput.setMaxLength(6);
            hexInput.setValue(String.format("%06X", curRgb));
        }
        hexInput.setX(rightX);
        hexInput.setY(ry - 4);
        hexInput.render(g, mouseX, mouseY, 0);
        if (hexFocused) {
            int tw = font.width(hexInput.getValue());
            g.fill(rightX + tw, ry - 4, rightX + tw + 1, ry + 10, 0xFFFFFFFF); // 光标
        }
        ry += 18;

        // HSV 标签
        g.drawString(font, "H:" + (int)(hue * 360) + "°", rightX, ry, 0xFF5555);
        ry += 10;
        g.drawString(font, "S:" + (int)(saturation * 100) + "%", rightX, ry, 0x55FF55);
        ry += 10;
        g.drawString(font, "V:" + (int)(value * 100) + "%", rightX, ry, 0x5555FF);
        ry += 14;

        // Alpha 滑块（色环下方）
        int alY = panelY + 22 + WHEEL_CY + WHEEL_OUTER_R + 4;
        g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.alpha").getString(), panelX + 8, alY, 0xFF888888);
        int asw = PANEL_W - 24;
        int asx = panelX + 12;
        g.fill(asx, alY + 10, asx + asw, alY + 20, 0xFF444455);
        int afill = (int)(alpha * asw);
        g.fill(asx, alY + 10, asx + afill, alY + 20, 0xFF6666AA);
        String aPct = (int)(alpha * 100) + "%";
        g.drawCenteredString(font, aPct, panelX + PANEL_W / 2, alY + 10, 0xFFFFFFFF);

        // 最近使用
        int recY = panelY + PANEL_H - 22;
        g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.recent").getString(), panelX + 4, recY - 10, 0xFF888888);
        int maxShow = Math.min(recentColors.size(), 8);
        int swatch = 18, gap = 2;
        int totalW = maxShow * (swatch + gap);
        int rx = panelX + (PANEL_W - totalW) / 2;
        for (int i = 0; i < maxShow; i++) {
            g.fill(rx + i * (swatch + gap), recY, rx + i * (swatch + gap) + swatch, recY + 14,
                    0xFF000000 | recentColors.get(i));
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        dragTarget = 0;

        int contentTop = panelY + 22;
        int wx = panelX + WHEEL_CX;
        int wy = contentTop + WHEEL_CY;

        int closeX = panelX + PANEL_W - 18;
        if (mx >= closeX && mx <= closeX + 14 && my >= panelY + 3 && my <= panelY + 17) {
            visible = false; hexFocused = false;
            return true;
        }

        if (mx >= panelX + 2 && mx <= panelX + PANEL_W - 20 && my >= panelY + 3 && my <= panelY + 17) {
            draggingTitle = 1;
            titleDragOffX = (int) (mx - panelX);
            titleDragOffY = (int) (my - panelY);
            hexFocused = false;
            return true;
        }

        // HEX 最先检测，确保不被色环拦截
        if (hexInput != null && mx >= hexInput.getX() - 4 && mx <= hexInput.getX() + 54 &&
            my >= hexInput.getY() - 2 && my <= hexInput.getY() + 16) {
            hexFocused = true;
            return true;
        }
        hexFocused = false;

        // Alpha 滑块
        int asx = panelX + 12, asw = PANEL_W - 24, asy = panelY + 22 + WHEEL_CY + WHEEL_OUTER_R + 4 + 10;
        if (mx >= asx && mx <= asx + asw && my >= asy && my <= asy + 10) {
            dragTarget = 3;
            alpha = Mth.clamp((float)(mx - asx) / asw, 0f, 1f);
            return true;
        }

        if (mx >= panelX + SV_X && mx <= panelX + SV_X + SV_SIZE &&
            my >= contentTop + SV_Y && my <= contentTop + SV_Y + SV_SIZE) {
            dragTarget = 2;
            saturation = Mth.clamp((float) (mx - panelX - SV_X) / SV_SIZE, 0f, 1f);
            value = Mth.clamp(1f - (float) (my - contentTop - SV_Y) / SV_SIZE, 0f, 1f);
            updateHexInput();
            return true;
        }

        double dist = Math.sqrt((mx - wx) * (mx - wx) + (my - wy) * (my - wy));
        if (dist >= WHEEL_INNER_R && dist <= WHEEL_OUTER_R) {
            dragTarget = 1;
            double angle = Math.toDegrees(Math.atan2(my - wy, mx - wx)) + 90;
            if (angle < 0) angle += 360;
            hue = (float) (angle / 360.0);
            updateHexInput();
            return true;
        }

        // 最近使用
        int recY = panelY + PANEL_H - 22;
        if (my >= recY && my <= recY + 14) {
            int maxShow = Math.min(recentColors.size(), 8);
            int swatch = 18, gap = 2;
            int totalW = maxShow * (swatch + gap);
            int startX = panelX + (PANEL_W - totalW) / 2;
            int idx = (int) (mx - startX) / (swatch + gap);
            if (idx >= 0 && idx < maxShow) {
                int c = recentColors.get(idx);
                float[] hsv = rgbToHsv(c);
                hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
                updateHexInput();
                applyColor();
                return true;
            }
        }

        // 点击面板外部 → 不拦截
        if (mx < panelX || mx > panelX + PANEL_W || my < panelY || my > panelY + PANEL_H) {
            return false;
        }
        return true;
    }

    public boolean mouseDragged(double mx, double my) {
        if (!visible) return false;
        if (draggingTitle == 1) {
            panelX = (int) mx - titleDragOffX;
            panelY = (int) my - titleDragOffY;
            return true;
        }

        int contentTop = panelY + 22;
        int wx = panelX + WHEEL_CX;
        int wy = contentTop + WHEEL_CY;

        if (dragTarget == 1) {
            double dx = mx - wx, dy = my - wy;
            double dist = Math.sqrt(dx * dx + dy * dy);
            // 约束距离在色相环范围内，避免鼠标太靠近中心导致角度跳变
            double effDist = Math.max(WHEEL_INNER_R + 4, Math.min(WHEEL_OUTER_R - 2, dist));
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0) angle += 360;
            hue = (float) (angle / 360.0);
            updateHexInput();
            return true;
        } else if (dragTarget == 2) {
            saturation = Mth.clamp((float) (mx - panelX - SV_X) / SV_SIZE, 0f, 1f);
            value = Mth.clamp(1f - (float) (my - contentTop - SV_Y) / SV_SIZE, 0f, 1f);
            updateHexInput();
            return true;
        } else if (dragTarget == 3) {
            int asx = panelX + 12, asw = PANEL_W - 24;
            alpha = Mth.clamp((float)(mx - asx) / asw, 0f, 1f);
            return true;
        }
        return false;
    }

    public boolean mouseReleased() {
        if (draggingTitle == 0 && dragTarget != 0) {
            applyColor();
        }
        draggingTitle = 0;
        dragTarget = 0;
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!visible || !hexFocused) return false;
        if (keyCode == 259 && hexInput != null && !hexInput.getValue().isEmpty()) { // Backspace
            String v = hexInput.getValue();
            hexInput.setValue(v.substring(0, v.length() - 1));
            applyHex();
            return true;
        }
        if (keyCode == 257) { applyHex(); hexFocused = false; return true; } // Enter
        return false;
    }

    public boolean charTyped(char c) {
        if (!visible || !hexFocused || hexInput == null) return false;
        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
            String v = hexInput.getValue();
            if (v.length() < 6) {
                hexInput.setValue(v + Character.toUpperCase(c));
                applyHex();
            }
        }
        return true;
    }

    private void applyHex() {
        if (hexInput == null) return;
        try {
            int parsed = Integer.parseInt(hexInput.getValue(), 16);
            float[] hsv = rgbToHsv(parsed);
            hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
        } catch (NumberFormatException ignored) {}
    }

    private void applyColor() {
        int rgb = hsvToRgbInt(hue * 360f, saturation, value);
        int a = (int) (alpha * 255);
        addRecent(rgb);
        onConfirm.accept((a << 24) | (rgb & 0xFFFFFF));
    }

    private void updateHexInput() {
        if (hexInput != null) {
            hexInput.setValue(String.format("%06X", hsvToRgbInt(hue * 360f, saturation, value)));
        }
    }

    private static void addRecent(int rgb) {
        recentColors.removeIf(c -> c == rgb);
        recentColors.addFirst(rgb);
        while (recentColors.size() > MAX_RECENT) recentColors.removeLast();
    }

    // ====== 颜色转换工具 ======

    public static int hsvToRgbInt(float h, float s, float v) {
        h = h % 360f;
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r, g, b;
        if (h < 60)      { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        return ((int) ((r + m) * 255) << 16) | ((int) ((g + m) * 255) << 8) | (int) ((b + m) * 255);
    }

    public static float[] rgbToHsv(int hex) {
        float r = ((hex >> 16) & 0xFF) / 255f;
        float g = ((hex >> 8) & 0xFF) / 255f;
        float b = (hex & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d != 0) {
            if (max == r)       h = ((g - b) / d) % 6;
            else if (max == g)  h = (b - r) / d + 2;
            else                h = (r - g) / d + 4;
        }
        h = (h * 60 + 360) % 360;
        float s = max == 0 ? 0 : d / max;
        return new float[]{h / 360f, s, max};
    }

    private static int blendColor(int c1, int c2) {
        int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF)) / 2;
        int g = (((c1 >> 8) & 0xFF) + ((c2 >> 8) & 0xFF)) / 2;
        int b = ((c1 & 0xFF) + (c2 & 0xFF)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
