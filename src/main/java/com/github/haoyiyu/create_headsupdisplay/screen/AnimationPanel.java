package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 动画编辑器 — 时间线 + 关键帧编辑 */
public class AnimationPanel {
    private static final int PANEL_W = 320;
    private static final int ROW_H = 14;
    private static final int TIMELINE_H = 20;

    private int panelX, panelY;
    private boolean visible;
    private int draggingTitle, dragOffX, dragOffY;
    private boolean draggingKf;

    private List<SlotAnimation> animations = new ArrayList<>();
    private int selectedAnim = -1;
    private int selectedKf = -1;
    private final Consumer<String> onChange;

    public AnimationPanel(Consumer<String> onChange) { this.onChange = onChange; }

    public void show(int x, int y, List<SlotAnimation> anims) {
        panelX = x; panelY = y; visible = true;
        animations = anims;
        selectedAnim = anims.isEmpty() ? -1 : 0;
        selectedKf = -1;
    }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }

    public void render(GuiGraphics g, Font font, int mx, int my) {
        if (!visible) return;
        SlotAnimation a = selectedAnim >= 0 && selectedAnim < animations.size() ? animations.get(selectedAnim) : null;
        int h = 16 + ROW_H + 4 + (a != null ? 8 * ROW_H + TIMELINE_H + a.keyframes.size() * ROW_H : 0) + 4;
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + h, 0xFF1A1A2E);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFF6060CC);

        int ty = panelY + 2;
        g.drawString(font, t("gui.create_headsupdisplay.pro.anim.title"), panelX + 6, ty + 2, 0xFFCCCCFF);
        int cx = panelX + PANEL_W - 16;
        g.fill(cx, ty, cx + 12, ty + 12, mx >= cx && mx <= cx + 12 && my >= ty && my <= ty + 12 ? 0xFFFF4444 : 0xCC883333);
        g.drawString(font, "X", cx + 3, ty + 2, 0xFFFFFFFF);

        int ry = panelY + 18;
        // +Anim / -Anim + 选择器
        boolean addH = mx >= panelX + 2 && mx <= panelX + 50 && my >= ry && my <= ry + ROW_H;
        g.fill(panelX + 2, ry, panelX + 50, ry + ROW_H, addH ? 0xFF336644 : 0xFF223322);
        g.drawString(font, t("gui.create_headsupdisplay.pro.anim.add"), panelX + 6, ry + 2, 0xFF88AA88);
        boolean delH = animations.size() > 0 && mx >= panelX + 52 && mx <= panelX + 102 && my >= ry && my <= ry + ROW_H;
        g.fill(panelX + 52, ry, panelX + 102, ry + ROW_H, delH ? 0xFF664444 : 0xFF332222);
        g.drawString(font, t("gui.create_headsupdisplay.pro.anim.del"), panelX + 56, ry + 2, 0xFFAA8888);
        for (int i = 0; i < animations.size(); i++) {
            int sx = panelX + 108 + i * 28;
            boolean sel = i == selectedAnim;
            g.fill(sx, ry, sx + 26, ry + ROW_H, sel ? 0xFF445577 : (mx >= sx && mx <= sx + 26 && my >= ry && my <= ry + ROW_H ? 0xFF333355 : 0xFF222233));
            g.drawString(font, "#" + (i + 1), sx + 4, ry + 2, sel ? 0xFF88CCFF : 0xFFCCCCCC);
        }
        ry += ROW_H + 4;
        if (a == null) { g.drawString(font, t("gui.create_headsupdisplay.pro.anim.no_anims"), panelX + 6, ry, 0xFF8888AA); return; }

        // 触发与周期 — 并排显示
        String[] tk = {"gui.create_headsupdisplay.pro.anim.trigger_always","gui.create_headsupdisplay.pro.anim.trigger_above","gui.create_headsupdisplay.pro.anim.trigger_below","gui.create_headsupdisplay.pro.anim.trigger_between"};
        String trig = t(tk[a.trigger.ordinal()]);
        String cyc = String.format("%.1fs", a.cycleTime);
        drawBtn(g, font, mx, my, panelX + 2, ry, 72, t("gui.create_headsupdisplay.pro.anim.trigger") + " " + trig);
        drawBtn(g, font, mx, my, panelX + 76, ry, 72, t("gui.create_headsupdisplay.pro.anim.cycle") + " " + cyc);
        drawBtn(g, font, mx, my, panelX + 150, ry, 48, t("gui.create_headsupdisplay.pro.anim.loop") + ": " + (a.loop ? "↻" : "→"));
        if (a.trigger != SlotAnimation.TriggerType.ALWAYS) {
            float v1 = a.triggerValue1;
            drawBtn(g, font, mx, my, panelX + 200, ry, 48, "V1:" + String.format("%.0f", v1));
            if (a.trigger == SlotAnimation.TriggerType.DATA_BETWEEN) {
                drawBtn(g, font, mx, my, panelX + 250, ry, 48, "V2:" + String.format("%.0f", a.triggerValue2));
            }
        }
        ry += ROW_H + 2;

        // 时间线
        int tlX = panelX + 24, tlY = ry, tlW = PANEL_W - 28, tlH = TIMELINE_H;
        g.fill(tlX, tlY, tlX + tlW, tlY + tlH, 0xFF0A0A18);
        g.fill(tlX, tlY + tlH/2, tlX + tlW, tlY + tlH/2 + 1, 0xFF333355);
        for (int i = 0; i < a.keyframes.size(); i++) {
            var k = a.keyframes.get(i);
            int kx = tlX + (int)(k.time * tlW);
            boolean hov = mx >= kx - 5 && mx <= kx + 5 && my >= tlY && my <= tlY + tlH;
            int kc = i == selectedKf ? 0xFFFFCC00 : (hov ? 0xFF88CCFF : 0xFF4488AA);
            g.fill(kx - 1, tlY, kx + 2, tlY + tlH, kc);
            g.drawString(font, String.valueOf(i + 1), kx - 3, tlY + tlH - 9, 0xFFFFFFFF);
        }
        ry += tlH + 2;

        // 关键帧列表
        for (int i = 0; i < a.keyframes.size(); i++) {
            var k = a.keyframes.get(i);
            boolean sel = i == selectedKf;
            boolean hov = mx >= panelX + 2 && mx <= panelX + PANEL_W - 40 && my >= ry && my <= ry + ROW_H;
            int bg = sel ? 0xFF334455 : (hov ? 0xFF222244 : 0xFF1A1A28);
            g.fill(panelX + 2, ry, panelX + PANEL_W - 40, ry + ROW_H, bg);
            String kfStr = String.format("KF%d t=%.2f", i + 1, k.time);
            if (k.posX != null || k.posY != null) kfStr += String.format("  pos:(%.0f,%.0f)", k.posX != null ? k.posX : 0, k.posY != null ? k.posY : 0);
            if (k.scale != null) kfStr += String.format("  s:%.2f", k.scale);
            if (k.rotation != null) kfStr += String.format("  r:%.0f°", k.rotation);
            g.drawString(font, kfStr, panelX + 6, ry + 1, sel ? 0xFFFFFF88 : 0xFFAAAAAA);
            // X 删除
            int dx = panelX + PANEL_W - 36;
            boolean dh = mx >= dx && mx <= dx + 12 && my >= ry && my <= ry + ROW_H;
            g.fill(dx, ry, dx + 12, ry + ROW_H, dh ? 0xFFFF4444 : 0xFF442222);
            g.drawString(font, "X", dx + 3, ry + 2, 0xFFFFFFFF);
            ry += ROW_H;
        }

        // +KF / 捕获
        ry += 2;
        drawBtn(g, font, mx, my, panelX + 2, ry, 92, t("gui.create_headsupdisplay.pro.anim.add_kf"));
        drawBtn(g, font, mx, my, panelX + 96, ry, 92, t("gui.create_headsupdisplay.pro.anim.capture"));
        // 提示
        g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.anim.hint").getString(), panelX + 192, ry + 2, 0xFF666688);
    }

    private void drawBtn(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, String text) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;
        g.fill(x, y, x + w, y + ROW_H, hover ? 0xFF333355 : 0xFF222233);
        g.drawString(font, text, x + 3, y + 2, hover ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        int ty = panelY + 2, cx = panelX + PANEL_W - 16;
        if (mx >= cx && mx <= cx + 12 && my >= ty && my <= ty + 12) { visible = false; return true; }
        if (mx >= panelX + 2 && mx <= panelX + PANEL_W - 18 && my >= ty && my <= ty + 12) {
            draggingTitle = 1; dragOffX = (int)(mx - panelX); dragOffY = (int)(my - panelY); return true;
        }

        int ry = panelY + 18;
        // +Anim
        if (mx >= panelX + 2 && mx <= panelX + 50 && my >= ry && my <= ry + ROW_H) {
            animations.add(new SlotAnimation()); selectedAnim = animations.size() - 1; selectedKf = -1;
            onChange.accept("added"); return true;
        }
        // -Anim
        if (animations.size() > 0 && mx >= panelX + 52 && mx <= panelX + 102 && my >= ry && my <= ry + ROW_H && selectedAnim >= 0) {
            animations.remove(selectedAnim); selectedAnim = Mth.clamp(selectedAnim, 0, animations.size() - 1); selectedKf = -1;
            onChange.accept("removed"); return true;
        }
        // 动画选择（右键复制）
        for (int i = 0; i < animations.size(); i++) {
            int sx = panelX + 108 + i * 28;
            if (mx >= sx && mx <= sx + 26 && my >= ry && my <= ry + ROW_H) {
                if (button == 1) { // 右键复制
                    var copy = SlotAnimation.deserialize(animations.get(i).serialize());
                    animations.add(copy); selectedAnim = animations.size() - 1; selectedKf = -1;
                    onChange.accept("added"); return true;
                }
                selectedAnim = i; selectedKf = -1; return true;
            }
        }
        ry += ROW_H + 4;
        if (selectedAnim < 0 || selectedAnim >= animations.size()) return false;
        SlotAnimation a = animations.get(selectedAnim);

        // Trigger
        if (mx >= panelX + 2 && mx <= panelX + 74 && my >= ry && my <= ry + ROW_H) {
            a.trigger = SlotAnimation.TriggerType.values()[(a.trigger.ordinal() + 1) % 4]; onChange.accept("trigger"); return true;
        }
        // Cycle
        if (mx >= panelX + 76 && mx <= panelX + 148 && my >= ry && my <= ry + ROW_H) {
            a.cycleTime = a.cycleTime >= 5f ? 0.5f : a.cycleTime + 0.5f; onChange.accept("cycle"); return true;
        }
        // Loop
        if (mx >= panelX + 150 && mx <= panelX + 198 && my >= ry && my <= ry + ROW_H) {
            a.loop = !a.loop; onChange.accept("loop"); return true;
        }
        // V1 / V2
        if (a.trigger != SlotAnimation.TriggerType.ALWAYS && mx >= panelX + 200 && mx <= panelX + 248 && my >= ry && my <= ry + ROW_H) {
            a.triggerValue1 += (button == 0 ? 10 : -10); if (a.triggerValue1 < 0) a.triggerValue1 = 0; onChange.accept("v1"); return true;
        }
        if (a.trigger == SlotAnimation.TriggerType.DATA_BETWEEN && mx >= panelX + 250 && mx <= panelX + 298 && my >= ry && my <= ry + ROW_H) {
            a.triggerValue2 += (button == 0 ? 10 : -10); if (a.triggerValue2 < 0) a.triggerValue2 = 0; onChange.accept("v2"); return true;
        }
        ry += ROW_H + 2;

        // 时间线点击 → 选关键帧。如果同位有多帧，循环切换选中
        int tlX = panelX + 24, tlW = PANEL_W - 28;
        if (my >= ry && my <= ry + TIMELINE_H) {
            // 找所有重合的关键帧
            var same = new java.util.ArrayList<Integer>();
            for (int i = 0; i < a.keyframes.size(); i++) {
                int kx = tlX + (int)(a.keyframes.get(i).time * tlW);
                if (mx >= kx - 8 && mx <= kx + 8) same.add(i);
            }
            if (!same.isEmpty()) {
                // 如果当前选中的在重合列表里，切到下一个；否则选第一个
                int idx = same.indexOf(selectedKf);
                selectedKf = same.get((idx + 1) % same.size());
                draggingKf = true;
                return true;
            }
            draggingKf = true;
            return true;
        }
        ry += TIMELINE_H + 2;

        // 关键帧列表：点击选中 / 点击 X 删除 / 点击 t= 编辑时间
        for (int i = 0; i < a.keyframes.size(); i++) {
            int dx = panelX + PANEL_W - 36;
            if (mx >= dx && mx <= dx + 12 && my >= ry && my <= ry + ROW_H) {
                if (a.keyframes.size() > 1) { a.keyframes.remove(i); selectedKf = Mth.clamp(selectedKf, 0, a.keyframes.size() - 1); onChange.accept("kf_del"); }
                return true;
            }
            if (mx >= panelX + 2 && mx <= panelX + PANEL_W - 40 && my >= ry && my <= ry + ROW_H) {
                if (button == 1) { // 右键复制
                    var orig = a.keyframes.get(i);
                    var copy = new SlotAnimation.Keyframe();
                    copy.time = Mth.clamp(orig.time + 0.05f, 0f, 1f); // 微偏移避免重合
                    copy.posX = orig.posX; copy.posY = orig.posY;
                    copy.scale = orig.scale; copy.rotation = orig.rotation;
                    copy.color = orig.color; copy.alpha = orig.alpha;
                    a.keyframes.add(copy); selectedKf = a.keyframes.size() - 1;
                    onChange.accept("kf_copy"); return true;
                }
                selectedKf = i;
                onChange.accept("kf_sel");
                return true;
            }
            ry += ROW_H;
        }
        ry += 2;

        // +关键帧（时间=1.0，抓当前状态）
        if (mx >= panelX + 2 && mx <= panelX + 94 && my >= ry && my <= ry + ROW_H) {
            var k = new SlotAnimation.Keyframe(); k.time = 1f;
            a.keyframes.add(k); selectedKf = a.keyframes.size() - 1;
            onChange.accept("capture:" + selectedAnim + ":" + (a.keyframes.size() - 1));
            return true;
        }
        // 捕获当前（时间=0.0，抓当前状态）
        if (mx >= panelX + 96 && mx <= panelX + 188 && my >= ry && my <= ry + ROW_H) {
            var k = new SlotAnimation.Keyframe(); k.time = 0f;
            a.keyframes.add(k); selectedKf = a.keyframes.size() - 1;
            onChange.accept("capture:" + selectedAnim + ":" + (a.keyframes.size() - 1));
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (!visible) return false;
        // 关键帧时间线拖拽
        if (draggingKf && selectedKf >= 0 && selectedAnim >= 0 && selectedAnim < animations.size()) {
            var a = animations.get(selectedAnim);
            if (selectedKf < a.keyframes.size()) {
                int tlX = panelX + 24, tlW = PANEL_W - 28;
                int tlY = panelY + 18 + ROW_H + 4 + ROW_H + 2;
                float t = Mth.clamp((float)(mx - tlX) / tlW, 0f, 1f);
                a.keyframes.get(selectedKf).time = t;
                onChange.accept("kf_drag");
                return true;
            }
        }
        // 标题栏拖拽
        if (draggingTitle != 0) {
            panelX = (int) mx - dragOffX; panelY = (int) my - dragOffY;
            return true;
        }
        return false;
    }
    public boolean mouseReleased() { draggingTitle = 0; draggingKf = false; return false; }

    private static String t(String key) { return Component.translatable(key).getString(); }
}
