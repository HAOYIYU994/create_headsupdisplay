package com.github.haoyiyu.create_headsupdisplay.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** 图层浮动面板，类似调色板 */
public class ProLayerPanel {
    private static final int PANEL_W = 150;
    private static final int ROW_H = 14;

    private int panelX, panelY;
    private boolean visible = false;
    private int draggingTitle = 0, titleDragOffX, titleDragOffY;

    public static class LayerInfo {
        public String name;
        public boolean visible = true, locked = false, frozen = false;
        public LayerInfo(String n) { name = n; }
    }

    private final List<LayerInfo> layers = new ArrayList<>();
    private int currentLayer = 0;
    private final Runnable onChange;
    private int contextMenuLayer = -1; // 右键菜单目标图层
    private boolean renaming = false;
    private String renameInput = "";

    public ProLayerPanel(Runnable onChange) { this.onChange = onChange; }

    public void show(int x, int y) { panelX = x; panelY = y; visible = true; }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }
    public int getPanelX() { return panelX; }
    public int getPanelY() { return panelY; }

    public List<LayerInfo> getLayers() { return layers; }
    public int getCurrentLayer() { return currentLayer; }
    public void setCurrentLayer(int i) { currentLayer = i; }

    public void render(GuiGraphics g, net.minecraft.client.gui.Font font, int mx, int my) {
        if (!visible) return;
        int h = 16 + 2 + layers.size() * ROW_H + 2 + ROW_H + 2; // title + rows + add btn
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + h, 0xFF222233);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFF5050AA);

        // 标题栏
        int ty = panelY + 2;
        g.fill(panelX + 2, ty, panelX + PANEL_W - 2, ty + 12, 0xFF334455);
        g.drawCenteredString(font, Component.translatable("gui.create_headsupdisplay.pro.layer_panel"), panelX + PANEL_W / 2, ty + 2, 0xFFCCCCCC);
        int closeX = panelX + PANEL_W - 16;
        boolean ch = mx >= closeX && mx <= closeX + 12 && my >= ty && my <= ty + 12;
        g.fill(closeX, ty, closeX + 12, ty + 12, ch ? 0xFFFF4444 : 0xAA883333);
        g.drawString(font, "X", closeX + 3, ty + 2, 0xFFFFFFFF);

        // 图层行
        int ry = panelY + 18;
        for (int i = 0; i < layers.size(); i++) {
            LayerInfo li = layers.get(i);
            boolean hover = mx >= panelX + 2 && mx <= panelX + PANEL_W - 2 && my >= ry && my <= ry + ROW_H;
            int bg;
            if (li.frozen) {
                bg = i == currentLayer ? 0xFF886644 : (hover ? 0xFF665522 : 0xFF443311); // 黄色调
            } else {
                bg = i == currentLayer ? 0xFF445566 : (hover ? 0xFF333355 : 0xFF222233);
            }
            g.fill(panelX + 2, ry, panelX + PANEL_W - 2, ry + ROW_H, bg);

            // 可见性
            int vx = panelX + 6;
            boolean vh = mx >= vx && mx <= vx + 12 && my >= ry && my <= ry + ROW_H;
            int vc = li.visible ? 0xFF44AA44 : 0xFF664444;
            g.fill(vx, ry + 1, vx + 12, ry + ROW_H - 1, vh ? lighten(vc) : vc);
            g.drawString(font, li.visible ? "V" : "H", vx + 3, ry + 2, 0xFFFFFFFF);

            // 锁定
            int lx = vx + 14;
            boolean lh = mx >= lx && mx <= lx + 12 && my >= ry && my <= ry + ROW_H;
            int lc = li.locked ? 0xFFAA8844 : 0xFF444444;
            g.fill(lx, ry + 1, lx + 12, ry + ROW_H - 1, lh ? lighten(lc) : lc);
            g.drawString(font, "L", lx + 3, ry + 2, 0xFFFFFFFF);

            // 名称 + 凝滞标记
            String displayName = li.frozen ? "❄ " + li.name : li.name;
            int nameColor = li.frozen ? 0xFFFFCC44 : (i == currentLayer ? 0xFF00FF00 : 0xFFFFFFFF);
            g.drawString(font, displayName, lx + 16, ry + 2, nameColor);
            ry += ROW_H;
        }
        ry += 2;

        // +图层
        boolean addHover = mx >= panelX + 2 && mx <= panelX + PANEL_W / 2 && my >= ry && my <= ry + ROW_H;
        g.fill(panelX + 2, ry, panelX + PANEL_W / 2, ry + ROW_H, addHover ? 0xFF336644 : 0xFF223322);
        g.drawCenteredString(font, Component.translatable("gui.create_headsupdisplay.pro.add_layer").getString(), panelX + PANEL_W / 4, ry + 2, 0xFF88AA88);
        boolean delHover = mx >= panelX + PANEL_W / 2 + 2 && mx <= panelX + PANEL_W - 2 && my >= ry && my <= ry + ROW_H;
        g.fill(panelX + PANEL_W / 2 + 2, ry, panelX + PANEL_W - 2, ry + ROW_H, delHover ? 0xFF664444 : 0xFF332222);
        g.drawCenteredString(font, Component.translatable("gui.create_headsupdisplay.pro.del_layer").getString(), panelX + PANEL_W * 3 / 4, ry + 2, 0xFFAA8888);

        // 右键菜单
        if (contextMenuLayer >= 0) {
            int cmx = panelX + PANEL_W + 2, cmy = panelY + 18 + contextMenuLayer * ROW_H;
            int mw = 100, mh = renaming ? 36 : 34;
            g.fill(cmx, cmy, cmx + mw, cmy + mh, 0xFF333344);
            boolean rh = mx >= cmx && mx <= cmx + mw && my >= cmy && my <= cmy + 16;
            g.fill(cmx, cmy, cmx + mw, cmy + 16, rh ? 0xFF445566 : 0xFF333344);
            if (renaming) {
                g.drawString(font, renameInput + "_", cmx + 4, cmy + 3, 0xFF00FF00);
                g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.enter_save").getString(), cmx + 4, cmy + 17, 0xFF888888);
            } else {
                g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.rename").getString(), cmx + 4, cmy + 3, 0xFFCCCCCC);
                boolean fh = mx >= cmx && mx <= cmx + mw && my >= cmy + 16 && my <= cmy + 34;
                int fc = layers.get(contextMenuLayer).frozen ? 0xFF44AAFF : 0xFF444455;
                g.fill(cmx, cmy + 16, cmx + mw, cmy + 34, fh ? lighten(fc) : fc);
                String fzText = Component.translatable("gui.create_headsupdisplay.pro.freeze").getString() + ": " + (layers.get(contextMenuLayer).frozen ? "ON" : "OFF");
                g.drawString(font, fzText, cmx + 4, cmy + 19, 0xFFFFFFFF);
            }
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        int h = 16 + 2 + layers.size() * ROW_H + 2 + ROW_H + 2;
        // 右键菜单点击优先
        if (contextMenuLayer >= 0) {
            int cmx = panelX + PANEL_W + 2, cmy = panelY + 18 + contextMenuLayer * ROW_H;
            if (mx >= cmx && mx <= cmx + 100 && my >= cmy && my <= cmy + (renaming ? 36 : 34)) {
                if (renaming) return true;
                if (my >= cmy && my <= cmy + 16) { renaming = true; renameInput = layers.get(contextMenuLayer).name; return true; }
                if (my >= cmy + 16 && my <= cmy + 34) { layers.get(contextMenuLayer).frozen = !layers.get(contextMenuLayer).frozen; onChange.run(); return true; }
            }
            contextMenuLayer = -1; renaming = false;
        }
        // 关闭
        int ty = panelY + 2;
        int closeX = panelX + PANEL_W - 16;
        if (mx >= closeX && mx <= closeX + 12 && my >= ty && my <= ty + 12) { visible = false; return true; }
        // 标题拖拽
        if (mx >= panelX + 2 && mx <= panelX + PANEL_W - 18 && my >= ty && my <= ty + 12) {
            draggingTitle = 1; titleDragOffX = (int)(mx - panelX); titleDragOffY = (int)(my - panelY); return true;
        }
        // 图层行
        int ry = panelY + 18;
        for (int i = 0; i < layers.size(); i++) {
            if (mx >= panelX + 2 && mx <= panelX + PANEL_W - 2 && my >= ry && my <= ry + ROW_H) {
                LayerInfo li = layers.get(i);
                int vx = panelX + 6;
                if (mx >= vx && mx <= vx + 12) { li.visible = !li.visible; onChange.run(); return true; }
                int lx = vx + 14;
                if (mx >= lx && mx <= lx + 12) { li.locked = !li.locked; onChange.run(); return true; }
                // 左键→选中，右键→菜单
                if (button == 1) { contextMenuLayer = i; return true; }
                currentLayer = i; onChange.run(); return true;
            }
            ry += ROW_H;
        }
        ry += 2;
        // +图层
        if (mx >= panelX + 2 && mx <= panelX + PANEL_W / 2 && my >= ry && my <= ry + ROW_H) {
            layers.add(new LayerInfo(Component.translatable("gui.create_headsupdisplay.pro.layer").getString() + " " + (layers.size() + 1))); onChange.run(); return true;
        }
        // 删除
        if (mx >= panelX + PANEL_W / 2 + 2 && mx <= panelX + PANEL_W - 2 && my >= ry && my <= ry + ROW_H && layers.size() > 1) {
            layers.remove(currentLayer); currentLayer = Mth.clamp(currentLayer, 0, layers.size() - 1); onChange.run(); return true;
        }
        // 外面点击 → 关闭
        if (my < panelY || my > panelY + h || mx < panelX || mx > panelX + PANEL_W) return false;
        return true;
    }

    public boolean mouseDragged(double mx, double my) {
        if (!visible || draggingTitle == 0) return false;
        panelX = (int) mx - titleDragOffX; panelY = (int) my - titleDragOffY;
        return true;
    }

    public boolean mouseReleased() { draggingTitle = 0; return false; }

    public boolean keyPressed(int keyCode) {
        if (!visible || !renaming) return false;
        if (keyCode == 259 && !renameInput.isEmpty()) renameInput = renameInput.substring(0, renameInput.length() - 1);
        else if (keyCode == 257 && !renameInput.isEmpty()) {
            layers.get(contextMenuLayer).name = renameInput; renaming = false; contextMenuLayer = -1; onChange.run();
        } else if (keyCode == 256) { renaming = false; contextMenuLayer = -1; }
        return true;
    }

    public boolean charTyped(char c) {
        if (!visible || !renaming) return false;
        if (c >= 32 && renameInput.length() < 16) renameInput += c;
        return true;
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((c >> 8) & 0xFF) + 40);
        int b = Math.min(255, (c & 0xFF) + 40);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
