package com.github.haoyiyu.create_headsupdisplay.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 显示模式选择面板 — 左侧模式列表，右侧大预览 + 参数编辑。
 */
public class DisplayStylePanel {
    private static final int PANEL_W = 260;
    private static final int LIST_W = 64;
    private static final int PREVIEW_X = LIST_W + 2;
    private static final int PREVIEW_W = PANEL_W - PREVIEW_X;
    private static final int PREVIEW_H = 80;
    private static final int PARAM_H = 14;
    private static final int ROW_H = 20;

    private int panelX, panelY;
    private boolean visible = false;
    private int draggingTitle, dragOffX, dragOffY;

    // 模式定义
    private static final String[] MODE_KEYS = {
        "gui.create_headsupdisplay.pro.display_mode.text",
        "gui.create_headsupdisplay.pro.display_mode.bar",
        "gui.create_headsupdisplay.pro.display_mode.alt",
        "gui.create_headsupdisplay.pro.display_mode.dial",
        "gui.create_headsupdisplay.pro.display_mode.digit",
        "gui.create_headsupdisplay.pro.display_mode.hudalt"
    };
    private static final boolean[] MODE_NUMERIC = {false, true, true, true, true, true};
    private static final int MODE_COUNT = 6;

    private SlotDataProvider slotData;
    private int hoveredMode = -1;
    private final java.util.function.Consumer<String> onParamChange;

    // 参数编辑状态
    private int editingField = -1; // 0=max 1=min 2=unit
    private String editMax = "", editMin = "", editUnit = "";
    private boolean dataIsNumeric;

    public interface SlotDataProvider {
        String getText();
        int getDisplayMode();
        float getDisplayMax();
        float getDisplayMin();
        String getDisplayUnit();
        float getScale();
        int getColor();
        int getAlpha();
    }

    public interface DisplayStyleTarget {
        void setDisplayMode(int mode);
    }

    public DisplayStylePanel(java.util.function.Consumer<String> onParamChange) {
        this.onParamChange = onParamChange;
    }

    public void show(int x, int y, SlotDataProvider data) {
        panelX = x; panelY = y; visible = true;
        slotData = data;
        dataIsNumeric = checkNumeric(data.getText());
        int mode = data.getDisplayMode();
        editMax = formatFloat(data.getDisplayMax());
        editMin = formatFloat(data.getDisplayMin());
        editUnit = data.getDisplayUnit();
        // 数据自带单位时自动填充
        if (editUnit.isEmpty()) {
            String autoUnit = com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.extractUnit(data.getText());
            if (!autoUnit.isEmpty()) editUnit = autoUnit;
        }
        hoveredMode = mode;
        editingField = -1;
    }

    private static String formatFloat(float v) {
        if (v == (int) v) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }

    public void hide() { visible = false; editingField = -1; }
    public boolean isVisible() { return visible; }

    public void render(GuiGraphics g, Font font, int mx, int my) {
        if (!visible || slotData == null) return;
        int mode = slotData.getDisplayMode();
        int rows = MODE_COUNT;
        int listH = rows * ROW_H;
        int h = 16 + 2 + Math.max(listH, PREVIEW_H + PARAM_H * 3 + 4) + 2;
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + h, 0xFF1A1A2E);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFF6060CC);

        // 标题栏
        int ty = panelY + 2;
        g.fill(panelX + 2, ty, panelX + PANEL_W - 2, ty + 12, 0xFF2A2A4A);
        g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.display_style").getString(), panelX + 6, ty + 2, 0xFFCCCCFF);
        int closeX = panelX + PANEL_W - 16;
        boolean ch = mx >= closeX && mx <= closeX + 12 && my >= ty && my <= ty + 12;
        g.fill(closeX, ty, closeX + 12, ty + 12, ch ? 0xFFFF4444 : 0xCC883333);
        g.drawString(font, "X", closeX + 3, ty + 2, 0xFFFFFFFF);

        // === 左侧模式列表 ===
        int ry = panelY + 18;
        for (int i = 0; i < MODE_COUNT; i++) {
            boolean hover = mx >= panelX + 2 && mx <= panelX + LIST_W && my >= ry && my <= ry + ROW_H;
            boolean cur = mode == i;
            boolean disabled = MODE_NUMERIC[i] && !dataIsNumeric;

            int bg;
            if (cur) bg = 0xFF445577;
            else if (hover && !disabled) bg = 0xFF333355;
            else bg = 0xFF222233;
            g.fill(panelX + 2, ry, panelX + LIST_W, ry + ROW_H, bg);
            if (cur) {
                g.fill(panelX + 2, ry, panelX + 5, ry + ROW_H, 0xFF4488FF);
            }

            int nameC = disabled ? 0xFF444444 : (cur ? 0xFF88CCFF : 0xFFCCCCCC);
            g.drawString(font, Component.translatable(MODE_KEYS[i]).getString(), panelX + 9, ry + (ROW_H - 9) / 2, nameC);
            if (disabled) {
                g.drawString(font, "!", panelX + LIST_W - 10, ry + (ROW_H - 9) / 2, 0xFF663333);
            }

            ry += ROW_H;
        }

        // === 右侧预览 ===
        int px = panelX + PREVIEW_X + 4;
        int py = panelY + 18;
        int pw = PREVIEW_W - 8, ph = PREVIEW_H;
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF111122);
        g.fill(px - 2, py - 2, px + pw + 2, py, 0xFF335577);

        int mx_preview = px + pw / 2;
        int my_preview = py + ph / 2;
        renderLargePreview(g, font, mode, px, py, pw, ph);

        // === 参数编辑 ===
        int paramY = py + ph + 4;
        // Max
        drawParamRow(g, font, mx, my, paramY, 0, "gui.create_headsupdisplay.pro.param.max", editMax);
        paramY += PARAM_H;
        // Min
        drawParamRow(g, font, mx, my, paramY, 1, "gui.create_headsupdisplay.pro.param.min", editMin);
        paramY += PARAM_H;
        // Unit
        drawParamRow(g, font, mx, my, paramY, 2, "gui.create_headsupdisplay.pro.param.unit", editUnit);

        // 保存提示
        paramY += PARAM_H + 2;
        if (editingField >= 0) {
            g.drawString(font, "Enter→save  Esc→cancel", px, paramY, 0xFF8888AA);
        }

        // 数值不可用时提示
        if (!dataIsNumeric) {
            g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.data_not_numeric").getString(), px, py + ph / 2 - 4, 0xFFFF4444);
        }
    }

    private void drawParamRow(GuiGraphics g, Font font, int mx, int my, int y, int fieldId, String labelKey, String value) {
        int x = panelX + PREVIEW_X + 4;
        g.drawString(font, Component.translatable(labelKey).getString(), x, y + 2, 0xFF8888CC);
        String labelText = Component.translatable(labelKey).getString();
        int vx = x + font.width(labelText) + 2;
        int vw = 80;
        boolean hover = mx >= vx && mx <= vx + vw && my >= y && my <= y + PARAM_H;
        boolean editing = editingField == fieldId;
        int bg = editing ? 0xFF334466 : (hover ? 0xFF222244 : 0xFF1A1A30);
        g.fill(vx, y, vx + vw, y + PARAM_H, bg);
        String display = editing ? value + "_" : value;
        g.drawString(font, display, vx + 3, y + 2, editing ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    /** 大幅预览 — GaugeRenderer 在 (0,0) 渲染，pushPose 平移到预览区 */
    private void renderLargePreview(GuiGraphics g, Font font, int mode, int x, int y, int w, int h) {
        if (slotData == null) return;
        int tc = 0xFF000000 | (slotData.getColor() & 0xFFFFFF);
        String text = slotData.getText().replaceAll("§[0-9a-fk-or]", "");
        float max = slotData.getDisplayMax(), min = slotData.getDisplayMin();
        String unit = slotData.getDisplayUnit();

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        switch (mode) {
            case 0 -> {
                String t = text.length() > 24 ? text.substring(0, 23) + "." : text;
                int tw = font.width(t);
                g.drawString(font, t, w/2 - tw/2, h/2 - 4, tc);
            }
            case 1 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderBar(
                    g, font, text, tc, max, unit, w, h);
            case 2 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderAltimeter(
                    g, font, text, tc, min, max, unit, w, h);
            case 3 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderDial(
                    g, font, text, tc, max, unit, w, h);
            case 4 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderDigital(
                    g, font, text, tc, unit, w, h);
            case 5 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderHudAltimeter(
                    g, font, text, tc, min, max, unit, w, h);
        }
        g.pose().popPose();
    }

    // === 鼠标 ===

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        int rows = MODE_COUNT;
        int listH = rows * ROW_H;
        int h = 16 + 2 + Math.max(listH, PREVIEW_H + PARAM_H * 3 + 4) + 2;

        // 关闭
        int ty = panelY + 2;
        int closeX = panelX + PANEL_W - 16;
        if (mx >= closeX && mx <= closeX + 12 && my >= ty && my <= ty + 12) { visible = false; return true; }
        // 拖拽
        if (mx >= panelX + 2 && mx <= panelX + PANEL_W - 18 && my >= ty && my <= ty + 12) {
            draggingTitle = 1; dragOffX = (int)(mx - panelX); dragOffY = (int)(my - panelY); return true;
        }

        // 左侧模式列表
        int ry = panelY + 18;
        for (int i = 0; i < MODE_COUNT; i++) {
            if (mx >= panelX + 2 && mx <= panelX + LIST_W && my >= ry && my <= ry + ROW_H) {
                boolean disabled = MODE_NUMERIC[i] && !dataIsNumeric;
                if (!disabled && slotData instanceof DisplayStyleTarget target) {
                    target.setDisplayMode(i);
                    hoveredMode = i;
                    if (onParamChange != null) onParamChange.accept("mode");
                }
                return true;
            }
            ry += ROW_H;
        }

        // 参数编辑框
        int px = panelX + PREVIEW_X + 4;
        int py = panelY + 18;
        int paramYBase = py + PREVIEW_H + 4;
        for (int f = 0; f < 3; f++) {
            int fy = paramYBase + f * PARAM_H;
            int vx = px + fontWidth(f) + 2;
            int vw = 80;
            if (mx >= vx && mx <= vx + vw && my >= fy && my <= fy + PARAM_H) {
                if (MODE_NUMERIC[slotData.getDisplayMode()] || f == 2) { // unit always editable
                    editingField = f;
                }
                return true;
            }
        }

        // 外面
        if (my < panelY || my > panelY + h || mx < panelX || mx > panelX + PANEL_W) return false;
        // 面板内非交互区也消费事件
        return true;
    }

    private int fontWidth(int fieldId) {
        return 32; // "Max:" "Min:" "Unit:" 大约等宽
    }

    public boolean mouseDragged(double mx, double my) {
        if (!visible || draggingTitle == 0) return false;
        panelX = (int) mx - dragOffX; panelY = (int) my - dragOffY;
        return true;
    }
    public boolean mouseReleased() { draggingTitle = 0; return false; }

    // === 键盘 ===

    public boolean keyPressed(int keyCode) {
        if (!visible || editingField < 0) return false;
        String[] fields = {editMax, editMin, editUnit};
        String current = fields[editingField];
        if (keyCode == 259 && !current.isEmpty()) { // backspace
            setEditField(editingField, current.substring(0, current.length() - 1));
        } else if (keyCode == 257) { // Enter
            applyEdit();
            editingField = -1;
        } else if (keyCode == 256) { // Escape
            editingField = -1;
        }
        return true;
    }

    public boolean charTyped(char c) {
        if (!visible || editingField < 0) return false;
        String[] fields = {editMax, editMin, editUnit};
        String current = fields[editingField];
        if (editingField == 2) { // unit: allow letters
            if (c >= 32 && current.length() < 6) setEditField(2, current + c);
        } else { // max/min: allow digits, dot, minus
            if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                if (current.length() < 10) setEditField(editingField, current + c);
            }
        }
        return true;
    }

    private void setEditField(int f, String v) {
        switch (f) {
            case 0 -> editMax = v;
            case 1 -> editMin = v;
            case 2 -> editUnit = v;
        }
    }

    private void applyEdit() {
        if (slotData instanceof DisplayStyleTarget) {
            // 将编辑的值存到一个临时 map 或通过回调传出
        }
        if (onParamChange != null) {
            onParamChange.accept("max:" + editMax);
            onParamChange.accept("min:" + editMin);
            onParamChange.accept("unit:" + editUnit);
        }
    }

    public String getEditMax() { return editMax; }
    public String getEditMin() { return editMin; }
    public String getEditUnit() { return editUnit; }

    private static boolean checkNumeric(String text) {
        String t = text.replaceAll("§[0-9a-fk-or]", "").trim();
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);
        try {
            Float.parseFloat(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
