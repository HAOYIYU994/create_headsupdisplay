package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.client.DynamicTextureCache;
import com.github.haoyiyu.create_headsupdisplay.network.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class TerminalProConfigScreen extends Screen {
    // ====== 布局常量 ======
    private static final int MENU_HEIGHT = 28;
    private static final int MENU_COLLAPSED = 10;
    private static final int DOCK_HEIGHT = 72;
    private static final int DOCK_COLLAPSED = 10;
    private static final int TRANSFORM_HANDLE_SIZE = 12;
    private static final int CARD_W = 96;
    private static final int CARD_H = 50;

    // ====== 数据 ======
    private BlockPos terminalPos;
    private final List<SlotEntry> allSlots = new ArrayList<>();      // 所有数据源槽位
    private final List<StaticTextEntry> staticTexts = new ArrayList<>();
    private final List<ImageEntry> images = new ArrayList<>();
    private final List<RadarEntry> radars = new ArrayList<>();
    private final List<SourceCard> sourceCards = new ArrayList<>();  // 信息源坞卡片
    // 图层面板
    private final ProLayerPanel layerPanel;
    private int currentLayer = 0;
    private List<ProLayerPanel.LayerInfo> layers() { return layerPanel.getLayers(); }

    // ====== UI 状态 ======
    private boolean menuExpanded = true;
    private boolean dockExpanded = true;
    private float menuAnim = 1f;   // 0=收 1=开
    private float dockAnim = 1f;
    private int dockScroll = 0;
    private float canvasZoom = com.github.haoyiyu.create_headsupdisplay.config.ModConfig.CANVAS_ZOOM.get().floatValue();

    // 选择与拖拽
    private Object selectedElement = null; // SlotEntry | StaticTextEntry | ImageEntry | RadarEntry
    private int selectedType = 0; // 1=slot 2=static 3=image 4=radar
    private int dragMode = 0;      // 0=none 1=move 2=scaleBR 3=rotate 4=dockDrag
    private int dragStartMX, dragStartMY;
    private int dragStartPX, dragStartPY;
    private float dragStartScale, dragStartRotation;
    private float dragStartDist; // 拖拽起点到元素中心的距离
    private SourceCard dockDragCard = null;

    // 调色板
    private final ProColorPicker colorPicker;
    private final DisplayStylePanel stylePanel;
    private final AnimationPanel animPanel;
    private Object colorEditTarget = null;

    // 图层重命名
    private boolean renameLayerMode = false;
    private String renameLayerText = "";
    // 文本编辑
    private StaticTextEntry textEditTarget = null;
    private String textEditValue = "";

    // 动画时间
    private long animStartTime = 0;

    // ====== 内部数据类 ======
    private static class SlotEntry implements DisplayStylePanel.DisplayStyleTarget, DisplayStylePanel.SlotDataProvider {
        final BlockPos sourcePos;
        int instanceId;
        int posX, posY;
        float scale = 1f, rotation = 0f;
        int color = 0xFFFFFF, alpha = 255;
        String text = "", sourceName;
        int displayLine, layerIndex;
        int displayMode;
        float displayMax = 100f, displayMin = 0f;
        String displayUnit = "";
        final List<com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation> animations = new ArrayList<>();
        static int nextId = 0;
        SlotEntry(BlockPos sp, int lx) { sourcePos = sp; posX = 100; posY = 100; layerIndex = lx; instanceId = nextId++; }
        @Override public void setDisplayMode(int m) { this.displayMode = m; }
        @Override public int getDisplayMode() { return displayMode; }
        @Override public String getText() { return text; }
        @Override public float getDisplayMax() { return displayMax; }
        @Override public float getDisplayMin() { return displayMin; }
        @Override public String getDisplayUnit() { return displayUnit; }
        @Override public float getScale() { return scale; }
        @Override public int getColor() { return color; }
        @Override public int getAlpha() { return alpha; }
    }

    private static class StaticTextEntry {
        String text;
        int posX, posY;
        float scale = 1f, rotation = 0f;
        int color = 0xFFFFFF, alpha = 255;
        int layerIndex;
        StaticTextEntry(String t, int x, int y) { text = t; posX = x; posY = y; }
    }

    private static class ImageEntry {
        final UUID imageId;
        final String fileName;
        final byte[] imageData;
        int posX, posY;
        float scale = 1f, rotation = 0f;
        int alpha = 255, layerIndex, instanceId;
        ImageEntry(UUID id, String fn, byte[] d, int x, int y) { imageId = id; fileName = fn; imageData = d; posX = x; posY = y; }
    }

    private static class RadarEntry {
        int posX, posY;
        float scale = 1f, rotation = 0f;
        int alpha = 255, radarRange = 50, layerIndex, instanceId;
    }

    /** 信息源坞中的卡片 */
    private static class SourceCard {
        String name, value;
        BlockPos sourcePos;
        int type; // 0=displaylink 1=image 2=radar
        UUID imageId;
        byte[] imageData;
        SourceCard(String n, String v, BlockPos sp) { name = n; value = v; sourcePos = sp; type = 0; }
    }

    // ====== 构造 ======
    public TerminalProConfigScreen(CompoundTag data) {
        super(Component.translatable("gui.create_headsupdisplay.display_terminal_pro.title"));
        this.terminalPos = BlockPos.of(data.getLong("TerminalPos"));
        this.layerPanel = new ProLayerPanel(this::onLayersChanged);
        this.colorPicker = new ProColorPicker(0xFFFFFF, 255, this::onColorConfirmed);
        this.stylePanel = new DisplayStylePanel(this::onStyleParamChanged);
        this.animPanel = new AnimationPanel(this::onAnimChanged);

        // 加载图层
        layerPanel.getLayers().clear();
        if (data.contains("Layers")) {
            ListTag lt = data.getList("Layers", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < lt.size(); i++) {
                CompoundTag lc = lt.getCompound(i);
                ProLayerPanel.LayerInfo li = new ProLayerPanel.LayerInfo(lc.getString("Name"));
                li.visible = lc.getBoolean("Visible"); li.locked = lc.getBoolean("Locked"); li.frozen = lc.getBoolean("Frozen");
                layerPanel.getLayers().add(li);
            }
        }
        if (layerPanel.getLayers().isEmpty()) layerPanel.getLayers().add(new ProLayerPanel.LayerInfo("Main"));

        // 加载数据源槽位
        int lc = data.getInt("LayerCount");
        if (lc == 0) lc = 1;
        for (int li = 0; li < lc; li++) {
            String key = "layerSlots_" + li;
            if (!data.contains(key)) continue;
            ListTag sl = data.getList(key, CompoundTag.TAG_COMPOUND);
            for (int j = 0; j < sl.size(); j++) {
                CompoundTag st = sl.getCompound(j);
                SlotEntry se = new SlotEntry(BlockPos.of(st.getLong("SourcePos")), li);
                se.posX = st.getInt("PosX"); se.posY = st.getInt("PosY");
                se.scale = st.getFloat("Scale"); se.rotation = st.getFloat("Rotation");
                se.color = st.getInt("Color"); se.alpha = st.getInt("Alpha");
                se.text = st.getString("LastData"); se.displayLine = st.getInt("DisplayLine");
                se.displayMode = st.getInt("DisplayMode");
                se.displayMax = st.getFloat("DisplayMax"); se.displayMin = st.getFloat("DisplayMin");
                se.displayUnit = st.getString("DisplayUnit");
                se.instanceId = st.getInt("SlotId");
                if (st.contains("SourceName")) se.sourceName = st.getString("SourceName");
                if (st.contains("Animations")) {
                    var at = st.getList("Animations", net.minecraft.nbt.CompoundTag.TAG_COMPOUND);
                    for (int ai = 0; ai < at.size(); ai++)
                        se.animations.add(com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation.deserialize(at.getCompound(ai)));
                }
                allSlots.add(se);
            }
        }

        // 加载静态文本
        if (data.contains("StaticTexts")) {
            ListTag stt = data.getList("StaticTexts", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < stt.size(); i++) {
                CompoundTag t = stt.getCompound(i);
                StaticTextEntry e = new StaticTextEntry(t.getString("text"), t.getInt("posX"), t.getInt("posY"));
                e.scale = t.getFloat("scale"); e.rotation = t.getFloat("rotation");
                e.color = t.getInt("color"); e.alpha = t.getInt("alpha");
                e.layerIndex = t.getInt("layerIndex");
                staticTexts.add(e);
                savedStaticTexts.add(e); // 从 NBT 加载的是已保存的
            }
        }

        // 加载图片
        if (data.contains("Images")) {
            ListTag imt = data.getList("Images", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < imt.size(); i++) {
                CompoundTag t = imt.getCompound(i);
                ImageEntry e = new ImageEntry(t.getUUID("ImageId"), t.getString("FileName"),
                        t.getByteArray("ImageData"), t.getInt("PosX"), t.getInt("PosY"));
                e.scale = t.getFloat("Scale"); e.rotation = t.getFloat("Rotation");
                e.alpha = t.getInt("Alpha"); e.layerIndex = t.getInt("layerIndex");
                images.add(e);
                savedImages.add(e.imageId);
            }
        }

        // 加载雷达
        if (data.contains("RadarSlots")) {
            ListTag rdt = data.getList("RadarSlots", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < rdt.size(); i++) {
                CompoundTag t = rdt.getCompound(i);
                RadarEntry e = new RadarEntry();
                e.posX = t.getInt("PosX"); e.posY = t.getInt("PosY");
                e.scale = t.getFloat("Scale"); e.rotation = t.getFloat("Rotation");
                e.alpha = t.getInt("Alpha"); e.radarRange = t.getInt("RadarRange");
                e.layerIndex = t.getInt("layerIndex");
                radars.add(e);
                sourceCards.add(new SourceCard(t("gui.create_headsupdisplay.pro.radar_placeholder"), "Range:" + e.radarRange, BlockPos.ZERO));
            }
        }

        // 从数据源缓存加载坞卡片
        if (data.contains("srcCacheCount")) {
            int count = data.getInt("srcCacheCount");
            for (int i = 0; i < count; i++) {
                CompoundTag ct = data.getCompound("srcCache_" + i);
                BlockPos sp = BlockPos.of(ct.getLong("pos"));
                String name = ct.getString("name");
                String val = ct.getString("val");
                sourceCards.add(new SourceCard(name.isEmpty() ? "Source" : name, val, sp));
            }
        } else {
            Set<BlockPos> seen = new HashSet<>();
            for (SlotEntry se : allSlots) {
                if (seen.add(se.sourcePos))
                    sourceCards.add(new SourceCard(se.sourceName != null ? se.sourceName : se.text, se.text, se.sourcePos));
            }
        }

        // 加载图片源卡片
        if (data.contains("imgSrcCount")) {
            int count = data.getInt("imgSrcCount");
            for (int i = 0; i < count; i++) {
                CompoundTag ct = data.getCompound("imgSrc_" + i);
                UUID id = ct.getUUID("id");
                String name = ct.getString("name");
                SourceCard sc = new SourceCard(name.isEmpty() ? "Image" : name, "[Image]", BlockPos.ZERO);
                sc.type = 1; sc.imageId = id;
                sc.imageData = ct.getByteArray("data");
                sourceCards.add(sc);
            }
        }

    }

    // ====== 生命周期 ======
    @Override
    protected void init() {
        super.init();
        animStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ====== 主渲染 ======
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt); // 必须调用以初始化渲染管线
        g.fill(0, 0, width, height, 0xFF101018);

        long elapsed = System.currentTimeMillis() - animStartTime;
        float targetMenu = menuExpanded ? 1f : 0f;
        float targetDock = dockExpanded ? 1f : 0f;
        menuAnim = Mth.lerp(Math.min(elapsed / 200f, 1f), menuAnim, targetMenu);
        dockAnim = Mth.lerp(Math.min(elapsed / 200f, 1f), dockAnim, targetDock);
        menuAnim = Mth.clamp(menuAnim, 0f, 1f);
        dockAnim = Mth.clamp(dockAnim, 0f, 1f);

        int menuH = (int) Mth.lerp(menuAnim, MENU_COLLAPSED, MENU_HEIGHT);
        int dockH = (int) Mth.lerp(dockAnim, DOCK_COLLAPSED, DOCK_HEIGHT);
        int canvasY = 0;
        int canvasH = height;

        renderCanvas(g, mx, my, canvasY, canvasH);
        renderMenuBar(g, mx, my, menuH);
        renderDock(g, mx, my, height - dockH, dockH);

        // 文本编辑提示
        if (textEditTarget != null) {
            String t = textEditValue + "_";
            int tw = font.width(t);
            int tx = textEditTarget.posX;
            int ty = textEditTarget.posY + (int)(20 * textEditTarget.scale) + 4;
            g.fill(tx - 2, ty - 2, tx + tw + 4, ty + 14, 0xFF000000);
            g.drawString(font, t, tx + 2, ty + 2, 0xFF00FF00);
            g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.edit_hint").getString(), tx + 2, ty + 16, 0xFF666688);
        }

        // 图层面板
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        layerPanel.render(g, font, mx, my);
        g.pose().popPose();
        // 显示样式面板
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        stylePanel.render(g, font, mx, my);
        g.pose().popPose();
        // 动画面板
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        animPanel.render(g, font, mx, my);
        g.pose().popPose();
        // 浮动调色板
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        colorPicker.render(g, font, mx, my);
        g.pose().popPose();
        if (!colorPicker.isVisible()) colorEditTarget = null;

        // NOTE: no super.render() — it causes background blur in 1.21.1
    }

    // ================================================================
    //  菜单栏
    // ================================================================
    private void renderMenuBar(GuiGraphics g, int mx, int my, int h) {
        if (menuAnim < 0.3f) {
            // 收起：只显示箭头，无背景条
            boolean hover = mx >= 2 && mx <= 16 && my >= 0 && my <= h;
            int bg = hover ? 0xCC556688 : 0x88334455;
            g.fill(2, 0, 16, h, bg);
            g.drawString(font, "▼", 4, 0, 0xFFFFFFFF);
            return;
        }

        // 背景
        g.fill(0, 0, width, h, 0xFF282840);
        g.fill(0, h - 1, width, h, 0xFF5050AA);

        int y = 4;
        int x = 4;
        boolean hoverCollapse = mx >= x && mx <= x + 18 && my >= 0 && my <= h;
        g.fill(x, 0, x + 18, h, hoverCollapse ? 0xFF6677AA : 0xFF4A5080);
        g.drawString(font, "▲", x + 3, y, 0xFFFFFFFF);
        x += 22;

        // 图层选择
        String curLayerName = currentLayer < layers().size() ? layers().get(currentLayer).name : "Main";
        g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.layer").getString() + ":", x, y + 1, 0xFFAAAAAA);
        x += 36;
        boolean hoverLayer = mx >= x && mx <= x + 70 && my >= y && my <= y + 14;
        int lw = Math.max(70, font.width(curLayerName) + 10);
        g.fill(x, y - 1, x + lw, y + 15, hoverLayer ? 0xFF5A6AAA : 0xFF3A4A6A);
        g.drawString(font, curLayerName, x + 3, y + 2, 0xFFFFFFFF);
        g.drawString(font, "▾", x + lw - 14, y + 2, 0xFFCCCCCC);
        x += lw + 4;

        // 分隔线
        g.fill(x, 2, x + 1, h - 2, 0xFF6060BB);
        x += 5;

        // 分隔线
        g.fill(x, 2, x + 1, h - 2, 0xFF6060BB);
        x += 5;

        // 工具按钮
        x = drawToolBtn(g, mx, my, x, y, h, t("gui.create_headsupdisplay.pro.tool_text"), t("gui.create_headsupdisplay.pro.tool_text.tip"));
        boolean hasSelection = selectedElement != null;
        boolean isSlot = selectedElement instanceof SlotEntry;
        String modeLabel = isSlot ? modeName(((SlotEntry)selectedElement).displayMode) : t("gui.create_headsupdisplay.pro.tool_mode");
        x = drawToolBtn(g, mx, my, x, y, h, modeLabel, t("gui.create_headsupdisplay.pro.tool_mode.tip"), isSlot ? 0xFF446688 : 0xFF333344);
        x = drawToolBtn(g, mx, my, x, y, h, t("gui.create_headsupdisplay.pro.tool_color"), t("gui.create_headsupdisplay.pro.tool_color.tip"), hasSelection ? 0xFF4444AA : 0xFF333344);
        x = drawToolBtn(g, mx, my, x, y, h, t("gui.create_headsupdisplay.pro.tool_anim"), t("gui.create_headsupdisplay.pro.tool_anim.tip"), isSlot ? 0xFF448866 : 0xFF333344);
        x = drawToolBtn(g, mx, my, x, y, h, t("gui.create_headsupdisplay.pro.tool_delete"), t("gui.create_headsupdisplay.pro.tool_delete.tip"), hasSelection ? 0xFFFF4444 : 0xFF664444);
        x += 4;
        // 保存
        x = drawToolBtn(g, mx, my, x, y, h, t("gui.create_headsupdisplay.pro.tool_save"), t("gui.create_headsupdisplay.pro.tool_save.tip"), 0xFF228822);

        // 右侧图层下拉（展开时）
        if (renameLayerMode) {
            int rx = width - 160;
            g.fill(rx - 2, 1, rx + 155, h - 1, 0xEE333344);
            g.drawString(font, t("gui.create_headsupdisplay.pro.rename_label"), rx, y, 0xAAAAAA);
            g.drawString(font, renameLayerText + "_", rx + 46, y, 0x00FF00);
            g.fill(rx + 46, y + 12, rx + 130, y + 13, 0xFF00FF00);
            if (mx >= rx + 46 && mx <= rx + 130 && my >= y && my <= y + 14) {
                // 输入区域
            }
        }
    }

    private int drawToolBtn(GuiGraphics g, int mx, int my, int x, int y, int barH, String label, String tooltip) {
        return drawToolBtn(g, mx, my, x, y, barH, label, tooltip, 0xFF3A4A6A);
    }

    private int drawToolBtn(GuiGraphics g, int mx, int my, int x, int y, int barH, String label, String tooltip, int color) {
        int w = font.width(label) + 8;
        boolean hover = mx >= x && mx <= x + w && my >= 0 && my <= barH;
        int bg = hover ? lighten(color) : color;
        g.fill(x, 1, x + w, barH - 1, bg);
        g.drawString(font, label, x + 4, y, 0xFFFFFFFF);
        return x + w + 2;
    }

    private int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((c >> 8) & 0xFF) + 40);
        int b = Math.min(255, (c & 0xFF) + 40);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    // ================================================================
    //  编辑画布
    // ================================================================
    private void renderCanvas(GuiGraphics g, int mx, int my, int canvasY, int canvasH) {
        int gridSpacing = 40;

        // 裁剪画布
        g.enableScissor(0, canvasY, width, canvasY + canvasH);

        // 画布深色底
        g.fill(0, canvasY, width, canvasY + canvasH, 0xFF181828);
        // 缩放变换（只影响内容元素和网格）
        float cz = canvasZoom;
        float cx = width / 2f, cy = canvasY + canvasH / 2f;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(cz, cz, 1f);
        g.pose().translate(-cx, -cy, 0);

        // 网格（随缩放）
        for (int gx = gridSpacing; gx < width; gx += gridSpacing)
            g.fill(gx, canvasY, gx + 1, canvasY + canvasH, 0x20FFFFFF);
        for (int gy = canvasY + gridSpacing; gy < canvasY + canvasH; gy += gridSpacing)
            g.fill(0, gy, width, gy + 1, 0x20FFFFFF);
        // 网格边界粗线（画布最边缘）
        g.fill(0, canvasY, 2, canvasY + canvasH, 0x40FFFFFF);
        g.fill(width - 2, canvasY, width, canvasY + canvasH, 0x40FFFFFF);
        g.fill(0, canvasY, width, canvasY + 2, 0x40FFFFFF);
        g.fill(0, canvasY + canvasH - 2, width, canvasY + canvasH, 0x40FFFFFF);

        // 空画布提示
        if (allSlots.isEmpty() && staticTexts.isEmpty() && images.isEmpty() && radars.isEmpty()) {
            String hint = t("gui.create_headsupdisplay.pro.canvas_hint1");
            int hw = font.width(hint);
            g.drawString(font, hint, (width - hw) / 2, canvasY + canvasH / 2 - 8, 0xFF8888AA);
            String hint2 = t("gui.create_headsupdisplay.pro.canvas_hint2");
            int hw2 = font.width(hint2);
            g.drawString(font, hint2, (width - hw2) / 2, canvasY + canvasH / 2 + 8, 0xFF6666AA);
        }

        // 渲染当前图层可见的槽位
        renderLayerSlots(g, mx, my, canvasY);
        renderLayerStaticTexts(g, mx, my, canvasY);
        renderLayerImages(g, mx, my, canvasY);
        renderLayerRadars(g, mx, my, canvasY);

        // 拖拽中的预览（从信息源坞拖出）
        if (dragMode == 4 && dockDragCard != null) {
            int px = mx - CARD_W / 2;
            int py = my - CARD_H / 2;
            g.fill(px, py, px + CARD_W, py + CARD_H, 0x88AAAAAA);
            g.drawString(font, dockDragCard.name, px + 4, py + 4, 0xFFFFFF);
        }

        g.disableScissor();
        g.pose().popPose(); // 缩放

        // 缩放指示器
        if (canvasZoom != 1f) {
            String z = String.format("%.0f%%", canvasZoom * 100);
            int zw = font.width(z);
            g.fill(width - zw - 10, canvasY + 4, width - 4, canvasY + 16, 0xCC222244);
            g.drawString(font, z, width - zw - 6, canvasY + 6, 0xFFCCCCFF);
        }
    }

    /** 图层叠加：visible=true 的图层全部渲染，但编辑只限选中图层 */
    private boolean isElementShown(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= layers().size()) return layerIndex < 0;
        return layers().get(layerIndex).visible;
    }
    private boolean isElementLocked(int layerIndex) {
        return layerIndex < 0 || layerIndex >= layers().size() || layers().get(layerIndex).locked || layerIndex != currentLayer;
    }

    private void renderLayerSlots(GuiGraphics g, int mx, int my, int canvasY) {
        for (SlotEntry slot : allSlots) {
            if (!isElementShown(slot.layerIndex)) continue;
            boolean sel = selectedElement == slot;
            boolean onCurrent = slot.layerIndex == currentLayer;
            int mode = slot.displayMode;
            String text = slot.text.replaceAll("§[0-9a-fk-or]", "");
            int tc = (slot.alpha << 24) | (slot.color & 0xFFFFFF);

            // gauge 尺寸
            int gw = 100, gh = 80;
            if (mode == 2 || mode == 5) { gw = 60; gh = 140; } // 高度计
            else if (mode == 3) { gw = 90; gh = 90; } // 表盘
            else if (mode == 4) { gw = 130; gh = 32; } // 数字
            else if (mode == 1) { gw = 140; gh = 40; } // 进度条
            // mode 0 text: 用原来的文字尺寸
            int bw = mode == 0 ? Math.max(80, font.width(text.isEmpty() ? "[empty]" : text) + 16) : gw;
            int bh = mode == 0 ? 20 : gh;

            g.pose().pushPose();
            g.pose().translate(slot.posX + bw * slot.scale / 2f, slot.posY + bh * slot.scale / 2f, 0);
            g.pose().scale(slot.scale, slot.scale, 1f);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));
            g.pose().translate(-bw / 2f, -bh / 2f, 0);

            if (mode == 0) {
                // 文字模式：原有渲染
                g.fill(0, 0, bw, bh, onCurrent ? 0xFF303050 : 0xFF1A1A30);
                g.fill(0, 0, bw, 1, onCurrent ? 0xFF5050AA : 0xFF282850);
                g.fill(0, bh - 1, bw, bh, onCurrent ? 0xFF5050AA : 0xFF282850);
                int accent = 0xFF000000 | (slot.color & 0xFFFFFF);
                g.fill(0, 0, 4, bh, accent);
                g.drawString(font, text.isEmpty() ? t("gui.create_headsupdisplay.pro.empty_slot") : text, 8, 4, onCurrent ? 0xFFFFFFFF : 0xFF888888);
            } else {
                // gauge 模式：用 GaugeRenderer 直接渲染
                int dim = onCurrent ? 0xFF : 0x44;
                switch (mode) {
                    case 1 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderBar(
                            g, font, text, dim << 24 | (tc & 0xFFFFFF), slot.displayMax, slot.displayUnit, bw, bh);
                    case 2 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderAltimeter(
                            g, font, text, dim << 24 | (tc & 0xFFFFFF), slot.displayMin, slot.displayMax, slot.displayUnit, bw, bh);
                    case 3 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderDial(
                            g, font, text, dim << 24 | (tc & 0xFFFFFF), slot.displayMax, slot.displayUnit, bw, bh);
                    case 4 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderDigital(
                            g, font, text, dim << 24 | (tc & 0xFFFFFF), slot.displayUnit, bw, bh);
                    case 5 -> com.github.haoyiyu.create_headsupdisplay.client.GaugeRenderer.renderHudAltimeter(
                            g, font, text, dim << 24 | (tc & 0xFFFFFF), slot.displayMin, slot.displayMax, slot.displayUnit, bw, bh);
                }
                // 非当前图层加一层暗滤镜
                if (!onCurrent) {
                    g.fill(0, 0, bw, bh, 0x88000000);
                }
            }
            g.pose().popPose();

            if (sel) {
                int sw = (int)(bw * slot.scale), sh = (int)(bh * slot.scale);
                drawSelectionHandles(g, mx, my, slot.posX, slot.posY, sw, sh, slot.scale, slot.rotation);
            }
        }
    }

    private void renderLayerStaticTexts(GuiGraphics g, int mx, int my, int canvasY) {
        for (StaticTextEntry e : staticTexts) {
            if (!isElementShown(e.layerIndex)) continue;
            boolean sel = selectedElement == e;
            int bw = Math.max(60, font.width(e.text) + 16);
            int bh = 20;

            g.pose().pushPose();
            g.pose().translate(e.posX + bw * e.scale / 2f, e.posY + bh * e.scale / 2f, 0);
            g.pose().scale(e.scale, e.scale, 1f);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(e.rotation));
            g.pose().translate(-bw / 2f, -bh / 2f, 0);

            boolean onCur = e.layerIndex == currentLayer;
            g.fill(0, 0, bw, bh, onCur ? 0xFF2A3A58 : 0xFF151A30);
            g.fill(0, 0, bw, 1, onCur ? 0xFF4060AA : 0xFF1A2050);
            g.fill(0, bh - 1, bw, bh, onCur ? 0xFF4060AA : 0xFF1A2050);
            int accent = 0xFF000000 | (e.color & 0xFFFFFF);
            g.fill(0, 0, 4, bh, accent);
            g.drawString(font, e.text, 8, 4, onCur ? 0xFFFFFFFF : 0xFF888888);
            g.pose().popPose();

            if (sel) {
                int sw = (int)(bw * e.scale), sh = (int)(bh * e.scale);
                drawSelectionHandles(g, mx, my, e.posX, e.posY, sw, sh, e.scale, e.rotation);
                g.drawString(font, Component.translatable("gui.create_headsupdisplay.pro.dbl_edit").getString(), e.posX + 2, e.posY + sh + 2, 0xFF8888AA);
            }
        }
    }

    private void renderLayerImages(GuiGraphics g, int mx, int my, int canvasY) {
        for (ImageEntry e : images) {
            if (!isElementShown(e.layerIndex)) continue;
            boolean sel = selectedElement == e;
            int iw = DynamicTextureCache.getWidth(e.imageId);
            int ih = DynamicTextureCache.getHeight(e.imageId);
            if (iw <= 0) { iw = 100; ih = 60; }
            int bw = iw, bh = ih;

            ResourceLocation tex = DynamicTextureCache.getOrCreate(e.imageId, e.imageData);
            g.pose().pushPose();
            g.pose().translate(e.posX + bw * e.scale / 2f, e.posY + bh * e.scale / 2f, 0);
            g.pose().scale(e.scale, e.scale, 1f);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(e.rotation));
            g.pose().translate(-bw / 2f, -bh / 2f, 0);

            float dim = e.layerIndex == currentLayer ? 1f : 0.3f;
            if (tex != null) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, e.alpha / 255f * dim);
                g.blit(tex, 0, 0, bw, bh, 0, 0, iw, ih, iw, ih);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
            } else {
                g.fill(0, 0, bw, bh, 0xFF2A2A48);
                g.drawString(font, e.fileName, 4, 4, dim > 0.5f ? 0xFFFFFFFF : 0xFF666666);
            }
            g.pose().popPose();

            if (sel) {
                int sw = (int)(bw * e.scale), sh = (int)(bh * e.scale);
                drawSelectionHandles(g, mx, my, e.posX, e.posY, sw, sh, e.scale, e.rotation);
            }
        }
    }

    private void renderLayerRadars(GuiGraphics g, int mx, int my, int canvasY) {
        for (RadarEntry e : radars) {
            if (!isElementShown(e.layerIndex)) continue;
            boolean sel = selectedElement == e;
            int bw = 100, bh = 60;

            g.pose().pushPose();
            g.pose().translate(e.posX + bw * e.scale / 2f, e.posY + bh * e.scale / 2f, 0);
            g.pose().scale(e.scale, e.scale, 1f);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(e.rotation));
            g.pose().translate(-bw / 2f, -bh / 2f, 0);

            boolean onCur = e.layerIndex == currentLayer;
            g.fill(0, 0, bw, bh, onCur ? 0xFF1A2A1A : 0xFF0A0A10);
            g.fill(0, 0, bw, 1, onCur ? 0xFF33AA33 : 0xFF1A4422);
            g.fill(0, bh - 1, bw, bh, onCur ? 0xFF33AA33 : 0xFF1A4422);
            int cx = bw / 2, cy = bh / 2, r = Math.min(bw, bh) / 3;
            g.fill(cx - r, cy, cx + r, cy + 1, onCur ? 0xFF228822 : 0xFF114411);
            g.fill(cx, cy - r, cx + 1, cy + r, onCur ? 0xFF228822 : 0xFF114411);
            g.fill(cx - 3, cy - 3, cx + 4, cy + 4, onCur ? 0xFF00FF00 : 0xFF228822);
            g.drawString(font, t("gui.create_headsupdisplay.pro.radar_placeholder"), 4, 4, onCur ? 0x22FF22 : 0x116611);
            g.pose().popPose();

            if (sel) {
                int sw = (int)(bw * e.scale), sh = (int)(bh * e.scale);
                drawSelectionHandles(g, mx, my, e.posX, e.posY, sw, sh, e.scale, e.rotation);
            }
        }
    }

    /** 绘制选中元素的变换手柄 */
    private void drawSelectionHandles(GuiGraphics g, int mx, int my, int x, int y, int w, int h, float scale, float rotation) {
        // 虚线选中框
        dashRect(g, x - 2, y - 2, x + w + 2, y + h + 2, 0xFF00AAFF);

        // 旋转手柄（顶部中心圆圈 + 连线）
        int rcx = x + w / 2;
        int rcy = y - 16;
        g.fill(rcx, y, rcx + 1, rcy, 0xFF00AAFF); // 连线
        g.fill(rcx - 4, rcy - 4, rcx + 5, rcy + 5, 0xFF00AAFF);
        g.fill(rcx - 2, rcy - 2, rcx + 3, rcy + 3, 0xFF3388CC);

        // 四角缩放手柄
        int hs = TRANSFORM_HANDLE_SIZE;
        g.fill(x - hs / 2, y - hs / 2, x + hs / 2, y + hs / 2, 0xFFFFFFFF);
        g.fill(x + w - hs / 2, y - hs / 2, x + w + hs / 2, y + hs / 2, 0xFFFFFFFF);
        g.fill(x - hs / 2, y + h - hs / 2, x + hs / 2, y + h + hs / 2, 0xFFFFFFFF);
        g.fill(x + w - hs / 2, y + h - hs / 2, x + w + hs / 2, y + h + hs / 2, 0xFFFFFFFF);

        // 浮动数值提示（拖拽时，显示视觉位置）
        if (dragMode != 0) {
            String info = "";
            if (dragMode == 2) info = String.format("%.1fx", scale);
            else if (dragMode == 3) info = (int) rotation + "°";
            else if (dragMode == 1) info = x + "," + y;
            if (!info.isEmpty()) {
                int iw = font.width(info);
                int ix = Math.min(Math.max(mx - iw / 2, 4), width - iw - 4);
                int iy = Math.max(4, my - 20);
                g.fill(ix - 2, iy - 2, ix + iw + 2, iy + 12, 0xCC000000);
                g.drawString(font, info, ix, iy, 0xFFFFFF);
            }
        }
    }

    private void dashRect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dashLen = 4;
        // 上边
        for (int x = x1; x < x2; x += dashLen * 2) g.fill(x, y1, Math.min(x + dashLen, x2), y1 + 1, color);
        // 下边
        for (int x = x1; x < x2; x += dashLen * 2) g.fill(x, y2 - 1, Math.min(x + dashLen, x2), y2, color);
        // 左边
        for (int y = y1; y < y2; y += dashLen * 2) g.fill(x1, y, x1 + 1, Math.min(y + dashLen, y2), color);
        // 右边
        for (int y = y1; y < y2; y += dashLen * 2) g.fill(x2 - 1, y, x2, Math.min(y + dashLen, y2), color);
    }

    // ================================================================
    //  信息源坞
    // ================================================================
    private void renderDock(GuiGraphics g, int mx, int my, int dockY, int dockH) {
        if (dockAnim < 0.2f) {
            // 收起：只显示箭头
            boolean hover = mx >= 2 && mx <= 16 && my >= dockY && my <= dockY + dockH;
            int bg = hover ? 0xCC556688 : 0x88334455;
            g.fill(2, dockY, 16, dockY + dockH, bg);
            g.drawString(font, "▲", 4, dockY, 0xFFFFFFFF);
            return;
        }

        g.fill(0, dockY, width, dockY + dockH, 0xFF282840);
        g.fill(0, dockY, width, dockY + 1, 0xFF5050AA);

        int y = dockY + 2;
        boolean hoverToggle = mx >= 2 && mx <= 18 && my >= dockY && my <= dockY + 14;
        g.fill(2, y, 18, y + 12, hoverToggle ? 0xFF6677AA : 0xFF4A5080);
        g.drawString(font, "▼", 5, y + 1, 0xFFFFFFFF);

        // 卡片列表（可横向滚动）
        int cardY = dockY + 16;
        int startX = 28 - dockScroll;
        g.enableScissor(0, dockY + 1, width, dockY + dockH);

        if (sourceCards.isEmpty()) {
            String hint = t("gui.create_headsupdisplay.pro.dock_empty");
            g.drawString(font, hint, 32, cardY + 8, 0xFF8888AA);
        }

        for (SourceCard card : sourceCards) {
            int cx = startX;
            if (cx + CARD_W > 0 && cx < width) {
                boolean hover = mx >= cx && mx <= cx + CARD_W && my >= cardY && my <= cardY + CARD_H;
                int bgColor = card.type == 1 ? 0xFF553388 : (card.type == 2 ? 0xFF338833 : 0xFF3355AA);
                if (hover) bgColor = lighten(bgColor);
                g.fill(cx, cardY, cx + CARD_W, cardY + CARD_H, bgColor);
                g.fill(cx, cardY, cx + CARD_W, cardY + 1, 0xFF8888FF);
                String name = card.name.length() > 10 ? card.name.substring(0, 9) + "." : card.name;
                g.drawString(font, name, cx + 4, cardY + 3, 0xFFFFFFFF);
                String val = card.value.length() > 12 ? card.value.substring(0, 11) + "." : card.value;
                g.drawString(font, val, cx + 4, cardY + 18, 0xFFFFFFFF);
                String tag = card.type == 1 ? t("gui.create_headsupdisplay.pro.card_img") : (card.type == 2 ? t("gui.create_headsupdisplay.pro.card_rdr") : t("gui.create_headsupdisplay.pro.card_src"));
                g.drawString(font, tag, cx + CARD_W - font.width(tag) - 4, cardY + CARD_H - 10, 0xFFCCCCCC);
                // 删除按钮（右上）
                int ddx = cx + CARD_W - 14, ddy = cardY;
                boolean dhover = mx >= ddx && mx <= ddx + 12 && my >= ddy && my <= ddy + 10;
                g.fill(ddx, ddy, ddx + 12, ddy + 10, dhover ? 0xFFFF4444 : 0xDD883333);
                g.drawString(font, "X", ddx + 3, ddy + 1, 0xFFFFFFFF);
            }
            startX += CARD_W + 6;
        }
        g.disableScissor();

        // 右端渐隐提示（如果内容超出）
        int totalW = sourceCards.size() * (CARD_W + 6);
        if (totalW > width - 30) {
            g.fillGradient(width - 30, dockY, width, dockY + dockH, 0x00282840, 0xFF282840);
        }
    }

    // ================================================================
    //  鼠标事件
    // ================================================================
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int menuH = (int) Mth.lerp(menuAnim, MENU_COLLAPSED, MENU_HEIGHT);
        int dockH = (int) Mth.lerp(dockAnim, DOCK_COLLAPSED, DOCK_HEIGHT);
        int canvasY = 0;
        int canvasH = height;
        int dockY = height - dockH;

        // 图层/调色板优先
        if (layerPanel.isVisible() && layerPanel.mouseClicked(mx, my, button)) return true;
        if (stylePanel.isVisible() && stylePanel.mouseClicked(mx, my, button)) return true;
        if (animPanel.isVisible() && animPanel.mouseClicked(mx, my, button)) return true;
        if (colorPicker.isVisible() && colorPicker.mouseClicked(mx, my, button)) return true;

        // 菜单栏（展开和收起都能点）
        if (my <= menuH || (my >= 0 && my <= MENU_COLLAPSED && !menuExpanded)) {
            return handleMenuClick(mx, my, menuH);
        }

        // 信息源坞（展开和收起都能点）
        if (my >= dockY) {
            return handleDockClick(mx, my, dockY);
        }

        // 画布
        return handleCanvasClick(mx, my, canvasY, canvasH);
    }

    private void playClick() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
        }
    }

    private static final String[] MODE_KEYS = {
        "gui.create_headsupdisplay.pro.display_mode.text",
        "gui.create_headsupdisplay.pro.display_mode.bar",
        "gui.create_headsupdisplay.pro.display_mode.alt",
        "gui.create_headsupdisplay.pro.display_mode.dial",
        "gui.create_headsupdisplay.pro.display_mode.digit",
        "gui.create_headsupdisplay.pro.display_mode.hudalt"
    };
    private String modeName(int m) { return m >= 0 && m < MODE_KEYS.length ? Component.translatable(MODE_KEYS[m]).getString() : "?"; }

    private boolean handleMenuClick(double mx, double my, int menuH) {
        // 收起/展开按钮
        if (mx >= 1 && mx <= 18 && my >= 0 && my <= menuH) {
            playClick();
            menuExpanded = !menuExpanded;
            animStartTime = System.currentTimeMillis();
            return true;
        }
        if (!menuExpanded) return false;

        // 计算与 renderMenuBar 一致的 x 位置
        int y = 4;
        int x = 4;          // start
        x += 22;            // collapse btn
        // "Layer:" label (36px)
        x += 36;
        // layer dropdown
        String curLN = currentLayer < layers().size() ? layers().get(currentLayer).name : "Main";
        int lw = Math.max(70, font.width(curLN) + 10);
        if (mx >= x && mx <= x + lw && my >= y - 1 && my <= y + 15) {
            playClick();
            layerPanel.show(4 + 22 + 36, menuH + 2);
            return true;
        }
        x += lw + 4;
        x += 1; // separator
        x += 5; // gap

        // Aa btn
        int aw = font.width("Aa") + 8;
        if (mx >= x && mx <= x + aw && my >= 0 && my <= menuH) {
            playClick();
            StaticTextEntry e = new StaticTextEntry(t("gui.create_headsupdisplay.pro.new_text"), width / 2, height / 2);
            e.layerIndex = currentLayer;
            staticTexts.add(e);
            selectedElement = e;
            selectedType = 2;
            return true;
        }
        x += aw + 2;

        // Mode btn
        String ml = selectedElement instanceof SlotEntry ? modeName(((SlotEntry)selectedElement).displayMode) : "Mode";
        int mw = font.width(ml) + 8;
        if (mx >= x && mx <= x + mw && my >= 0 && my <= menuH && selectedElement instanceof SlotEntry se) {
            playClick();
            stylePanel.show(width / 2 - 140, 40, se);
            return true;
        }
        x += mw + 2;

        // Color btn
        int cw = font.width("🎨") + 8;
        if (mx >= x && mx <= x + cw && my >= 0 && my <= menuH && selectedElement != null) {
            playClick();
            int c = getElementColor(selectedElement);
            colorEditTarget = selectedElement;
            colorPicker.show((width - 200) / 2, 40, getElementColor(selectedElement), getElementAlpha(selectedElement));
            return true;
        }
        x += cw + 2;

        // Anim btn
        int animW = font.width("▶") + 8;
        if (mx >= x && mx <= x + animW && my >= 0 && my <= menuH && selectedElement instanceof SlotEntry se) {
            playClick();
            animPanel.show(width / 2 - 150, 40, se.animations);
            return true;
        }
        x += animW + 2;

        // Delete btn
        int dw = font.width("🗑") + 8;
        if (mx >= x && mx <= x + dw && my >= 0 && my <= menuH) {
            playClick();
            if (selectedElement != null) { deleteElement(selectedElement); selectedElement = null; }
            return true;
        }
        x += dw + 2;
        x += 4;  // gap

        // Save btn
        int saveW = font.width("💾") + 8;
        if (mx >= x && mx <= x + saveW && my >= 0 && my <= menuH) {
            playClick();
            saveAll();
            return true;
        }

        return false;
    }

    private long lastClickTime = 0;
    private Object lastClickElement = null;

    private boolean handleCanvasClick(double mx, double my, int canvasY, int canvasH) {
        if (button(0)) {
            // 转画布坐标（匹配缩放后的视觉位置）
            double cmx = canvasX(mx, canvasY, canvasH), cmy = canvasY(my, canvasY, canvasH);
            // 拖拽起点存画布坐标
            int sx = (int) cmx, sy = (int) cmy;

            // 检测选中元素的变换手柄
            if (selectedElement != null) {
                int[] bounds = getElementBounds(selectedElement);
                if (bounds != null) {
                    int x = bounds[0], y = bounds[1], w = bounds[2], h = bounds[3];

                    int rcx = x + w / 2;
                    int rcy = y - 16;
                    if (Math.abs(cmx - rcx) <= 6 && Math.abs(cmy - rcy) <= 6) {
                        dragMode = 3;
                        dragStartMX = sx; dragStartMY = sy;
                        dragStartRotation = getElementRotation(selectedElement);
                        return true;
                    }

                    if (Math.abs(cmx - (x + w)) <= TRANSFORM_HANDLE_SIZE && Math.abs(cmy - (y + h)) <= TRANSFORM_HANDLE_SIZE) {
                        dragMode = 2;
                        dragStartMX = sx; dragStartMY = sy;
                        dragStartScale = getElementScale(selectedElement);
                        dragStartDist = (float) Math.sqrt((cmx - x) * (cmx - x) + (cmy - y) * (cmy - y));
                        return true;
                    }
                }
            }

            // 检测点击元素（画布坐标）
            Object hit = hitTest(cmx, cmy);
            if (hit != null && !isElementLocked(getElementLayerIndex(hit))) {
                // 双击检测（静态文本 → 打开编辑）
                long now = System.currentTimeMillis();
                if (hit instanceof StaticTextEntry && hit == lastClickElement && (now - lastClickTime) < 400) {
                    StaticTextEntry st = (StaticTextEntry) hit;
                    startTextEditing(st);
                    lastClickTime = 0;
                    return true;
                }
                lastClickTime = now;
                lastClickElement = hit;

                selectedElement = hit;
                selectedType = getElementType(hit);
                // 调色板开着就切换编辑对象（保持位置）
                if (colorPicker.isVisible()) {
                    colorEditTarget = hit;
                    colorPicker.switchTarget(getElementColor(hit), getElementAlpha(hit));
                }
                dragMode = 1;
                int[] b = getElementBounds(hit);
                dragStartMX = sx; dragStartMY = sy;
                dragStartPX = b != null ? b[0] : 0;
                dragStartPY = b != null ? b[1] : 0;
                return true;
            }

            // 点击空白
            selectedElement = null;
            selectedType = 0;
        }
        return false;
    }

    private boolean handleDockClick(double mx, double my, int dockY) {
        if (my < dockY + 14) {
            if (mx >= 2 && mx <= 20) {
                dockExpanded = !dockExpanded;
                animStartTime = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        // 检测卡片点击 → 开始拖拽
        int cardY = dockY + 16;
        int startX = 28 - dockScroll;
        for (int i = 0; i < sourceCards.size(); i++) {
            SourceCard card = sourceCards.get(i);
            if (mx >= startX && mx <= startX + CARD_W && my >= cardY && my <= cardY + CARD_H) {
                // 删除按钮（右上角）
                int ddx = startX + CARD_W - 14, ddy = cardY;
                if (mx >= ddx && mx <= ddx + 12 && my >= ddy && my <= ddy + 10) {
                    playClick();
                    deleteSourceCard(card, i);
                    return true;
                }
                // 拖拽
                dockDragCard = card;
                dragMode = 4;
                dragStartMX = (int) mx; dragStartMY = (int) my;
                return true;
            }
            startX += CARD_W + 6;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (layerPanel.isVisible() && layerPanel.mouseDragged(mx, my)) return true;
        if (stylePanel.isVisible() && stylePanel.mouseDragged(mx, my)) return true;
        if (animPanel.isVisible() && animPanel.mouseDragged(mx, my)) return true;
        if (colorPicker.isVisible() && colorPicker.mouseDragged(mx, my)) return true;

        if (dragMode == 0) return false;

        if (dragMode == 4 && dockDragCard != null) {
            return true;
        }

        if (selectedElement == null) return false;

        // 画布坐标
        double cmx = canvasX(mx, 0, height), cmy = canvasY(my, 0, height);

        int[] bounds = getElementBounds(selectedElement);
        if (bounds == null) return false;

        switch (dragMode) {
            case 1: { // 移动（画布坐标，直接加减）
                int newX = dragStartPX + (int) cmx - dragStartMX;
                int newY = dragStartPY + (int) cmy - dragStartMY;
                int marginX = bounds[2] * 2, marginY = bounds[3] * 2;
                newX = Math.max(-marginX, Math.min(newX, width - bounds[2] + marginX));
                newY = Math.max(-marginY, Math.min(newY, height - bounds[3] + marginY));
                setElementPos(selectedElement, newX, newY);
                return true;
            }
            case 2: { // 缩放
                float dist = (float) Math.sqrt((cmx - bounds[0]) * (cmx - bounds[0]) + (cmy - bounds[1]) * (cmy - bounds[1]));
                float ratio = dragStartDist > 1 ? dist / dragStartDist : 1f;
                float newScale = Math.max(0.1f, dragStartScale * ratio);
                if (Screen.hasShiftDown()) newScale = Math.round(newScale * 4) / 4f;
                newScale = Math.min(newScale, com.github.haoyiyu.create_headsupdisplay.config.ModConfig.MAX_SCALE.get().floatValue());
                setElementScale(selectedElement, newScale);
                return true;
            }
            case 3: { // 旋转
                int bcx = bounds[0] + bounds[2] / 2;
                int bcy = bounds[1] + bounds[3] / 2;
                float angle = (float) Math.toDegrees(Math.atan2(cmy - bcy, cmx - bcx)) + 90;
                if (Screen.hasShiftDown()) angle = Math.round(angle / 15) * 15;
                setElementRotation(selectedElement, angle);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        layerPanel.mouseReleased();
        stylePanel.mouseReleased();
        animPanel.mouseReleased();
        colorPicker.mouseReleased();

        if (dragMode == 4 && dockDragCard != null) {
            int mh = (int) Mth.lerp(menuAnim, MENU_COLLAPSED, MENU_HEIGHT);
            int dh = (int) Mth.lerp(dockAnim, DOCK_COLLAPSED, DOCK_HEIGHT);
            int canvasY = 0;
            int dockY = height - dh;
            // 只接受落在画布上的释放（不能在坞内释放）
            if (my > canvasY && my < dockY) {
                if (dockDragCard.type == 0 && dockDragCard.sourcePos != null
                        && !BlockPos.ZERO.equals(dockDragCard.sourcePos)) {
                    SlotEntry se = new SlotEntry(dockDragCard.sourcePos, currentLayer);
                    se.posX = Math.max(0, (int) mx - 40);
                    se.posY = Math.max(canvasY, (int) my - 10);
                    se.text = dockDragCard.value;
                    se.sourceName = dockDragCard.name;
                    allSlots.add(se);
                    playClick();
                    PacketDistributor.sendToServer(new UpdateSlotPayload(terminalPos, se.sourcePos, se.instanceId,
                            se.posX, se.posY, se.scale, se.rotation, se.color, se.alpha));
                } else if (dockDragCard.type == 1 && dockDragCard.imageId != null) {
                    ImageEntry ie = new ImageEntry(dockDragCard.imageId, dockDragCard.name,
                            dockDragCard.imageData != null ? dockDragCard.imageData : new byte[0],
                            (int) mx - 50, (int) my - 30);
                    ie.layerIndex = currentLayer;
                    ie.scale = 0.1f;
                    images.add(ie);
                    savedImages.add(ie.imageId);
                    playClick();
                    PacketDistributor.sendToServer(new UpdateImageConfigPayload(terminalPos, ie.imageId,
                            ie.posX, ie.posY, ie.scale, ie.rotation, ie.alpha));
                }
            }
            dockDragCard = null;
            dragMode = 0;
            return true;
        }

        dragMode = 0;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int dockH = (int) Mth.lerp(dockAnim, DOCK_COLLAPSED, DOCK_HEIGHT);
        int dockY = height - dockH;

        // 信息源坞横向滚动
        if (my >= dockY && dockExpanded) {
            int maxScroll = Math.max(0, sourceCards.size() * (CARD_W + 6) - (width - 28));
            dockScroll = (int) Mth.clamp(dockScroll - sy * 20, 0, maxScroll);
            return true;
        }

        // Ctrl+滚轮 → 画布缩放
        if (Screen.hasControlDown()) {
            canvasZoom = Mth.clamp(canvasZoom + (float)(sy * 0.1f), 0.25f, 1f);
            com.github.haoyiyu.create_headsupdisplay.config.ModConfig.CANVAS_ZOOM.set((double) canvasZoom);
            return true;
        }

        // 画布上滚轮缩放选中元素
        if (selectedElement != null) {
            float ds = (float) (sy > 0 ? 0.05f : -0.05f);
            float s = Mth.clamp(getElementScale(selectedElement) + ds, 0.1f, 5f);
            setElementScale(selectedElement, s);
            return true;
        }

        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (layerPanel.isVisible() && layerPanel.keyPressed(keyCode)) return true;
        if (stylePanel.isVisible() && stylePanel.keyPressed(keyCode)) return true;
        if (colorPicker.isVisible() && colorPicker.keyPressed(keyCode)) return true;
        // 文本编辑模式
        if (textEditTarget != null) {
            if (keyCode == 259 && !textEditValue.isEmpty()) {
                textEditValue = textEditValue.substring(0, textEditValue.length() - 1);
            } else if (keyCode == 257 || keyCode == 256) { // Enter or Escape
                if (keyCode == 257 && !textEditValue.isEmpty()) textEditTarget.text = textEditValue;
                textEditTarget = null;
                textEditValue = "";
            }
            return true;
        }
        if (renameLayerMode) {
            if (keyCode == 259 && !renameLayerText.isEmpty()) {
                renameLayerText = renameLayerText.substring(0, renameLayerText.length() - 1);
            } else if (keyCode == 257) {
                renameLayerMode = false;
                if (currentLayer < layers().size() && !renameLayerText.isEmpty()) {
                    layers().get(currentLayer).name = renameLayerText;
                }
                renameLayerText = "";
            }
            return true;
        }
        // Delete 键删除选中元素
        if ((keyCode == 261 || keyCode == 259) && selectedElement != null && textEditTarget == null) {
            deleteElement(selectedElement);
            selectedElement = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (layerPanel.isVisible() && layerPanel.charTyped(c)) return true;
        if (stylePanel.isVisible() && stylePanel.charTyped(c)) return true;
        if (colorPicker.isVisible() && colorPicker.charTyped(c)) return true;
        if (textEditTarget != null && c >= 32 && textEditValue.length() < 64) {
            textEditValue += c;
            return true;
        }
        if (renameLayerMode && c >= 32 && renameLayerText.length() < 16) {
            renameLayerText += c;
            return true;
        }
        return super.charTyped(c, mods);
    }

    private void startTextEditing(StaticTextEntry entry) {
        textEditTarget = entry;
        textEditValue = entry.text;
    }

    // ====== 辅助方法 ======
    /** 将屏幕鼠标坐标转换为画布内坐标（考虑缩放） */
    private double canvasX(double screenX, int canvasY, int canvasH) {
        float cx = width / 2f, cy = canvasY + canvasH / 2f;
        return (screenX - cx) / canvasZoom + cx;
    }
    private double canvasY(double screenY, int canvasY, int canvasH) {
        float cx = width / 2f, cy = canvasY + canvasH / 2f;
        return (screenY - cy) / canvasZoom + cy;
    }

    private int getElementLayerIndex(Object e) {
        if (e instanceof SlotEntry s) return s.layerIndex;
        if (e instanceof StaticTextEntry s) return s.layerIndex;
        if (e instanceof ImageEntry s) return s.layerIndex;
        if (e instanceof RadarEntry s) return s.layerIndex;
        return -1;
    }
    private Object hitTest(double mx, double my) {
        for (int i = radars.size() - 1; i >= 0; i--) {
            RadarEntry e = radars.get(i);
            if (isElementShown(e.layerIndex)) {
                int w = (int)(100 * e.scale), h = (int)(60 * e.scale);
                if (mx >= e.posX && mx <= e.posX + w && my >= e.posY && my <= e.posY + h) return e;
            }
        }
        for (int i = images.size() - 1; i >= 0; i--) {
            ImageEntry e = images.get(i);
            if (isElementShown(e.layerIndex)) {
                int iw = DynamicTextureCache.getWidth(e.imageId);
                int ih = DynamicTextureCache.getHeight(e.imageId);
                if (iw <= 0) { iw = 100; ih = 60; }
                int w = (int)(iw * e.scale), h = (int)(ih * e.scale);
                if (mx >= e.posX && mx <= e.posX + w && my >= e.posY && my <= e.posY + h) return e;
            }
        }
        for (int i = staticTexts.size() - 1; i >= 0; i--) {
            StaticTextEntry e = staticTexts.get(i);
            if (isElementShown(e.layerIndex)) {
                int bw = Math.max(60, font.width(e.text) + 16);
                int w = (int)(bw * e.scale), h = (int)(20 * e.scale);
                if (mx >= e.posX && mx <= e.posX + w && my >= e.posY && my <= e.posY + h) return e;
            }
        }
        for (int i = allSlots.size() - 1; i >= 0; i--) {
            SlotEntry e = allSlots.get(i);
            if (isElementShown(e.layerIndex)) {
                int bw, bh;
                if (e.displayMode == 0) {
                    bw = Math.max(80, font.width(e.text.isEmpty() ? "[empty]" : e.text) + 16);
                    bh = 20;
                } else {
                    int[] gs = gaugeSize(e.displayMode);
                    bw = gs[0]; bh = gs[1];
                }
                int w = (int)(bw * e.scale), h = (int)(bh * e.scale);
                if (mx >= e.posX && mx <= e.posX + w && my >= e.posY && my <= e.posY + h) return e;
            }
        }
        return null;
    }

    private int[] getElementBounds(Object e) {
        if (e instanceof SlotEntry s) {
            int bw, bh;
            if (s.displayMode == 0) {
                bw = Math.max(80, font.width(s.text.isEmpty() ? "[empty]" : s.text) + 16);
                bh = 20;
            } else {
                int[] gs = gaugeSize(s.displayMode);
                bw = gs[0]; bh = gs[1];
            }
            return new int[]{s.posX, s.posY, (int)(bw * s.scale), (int)(bh * s.scale)};
        }
        if (e instanceof StaticTextEntry s) {
            int bw = Math.max(60, font.width(s.text) + 16);
            return new int[]{s.posX, s.posY, (int)(bw * s.scale), (int)(20 * s.scale)};
        }
        if (e instanceof ImageEntry s) {
            int iw = DynamicTextureCache.getWidth(s.imageId);
            int ih = DynamicTextureCache.getHeight(s.imageId);
            if (iw <= 0) { iw = 100; ih = 60; }
            return new int[]{s.posX, s.posY, (int)(iw * s.scale), (int)(ih * s.scale)};
        }
        if (e instanceof RadarEntry s) {
            return new int[]{s.posX, s.posY, (int)(100 * s.scale), (int)(60 * s.scale)};
        }
        return null;
    }

    private int getElementType(Object e) {
        if (e instanceof SlotEntry) return 1;
        if (e instanceof StaticTextEntry) return 2;
        if (e instanceof ImageEntry) return 3;
        if (e instanceof RadarEntry) return 4;
        return 0;
    }

    private int getElementColor(Object e) {
        if (e instanceof SlotEntry s) return s.color;
        if (e instanceof StaticTextEntry s) return s.color;
        return 0xFFFFFF;
    }
    private int getElementAlpha(Object e) {
        if (e instanceof SlotEntry s) return s.alpha;
        if (e instanceof StaticTextEntry s) return s.alpha;
        if (e instanceof ImageEntry s) return s.alpha;
        if (e instanceof RadarEntry s) return s.alpha;
        return 255;
    }
    private float getElementScale(Object e) {
        if (e instanceof SlotEntry s) return s.scale;
        if (e instanceof StaticTextEntry s) return s.scale;
        if (e instanceof ImageEntry s) return s.scale;
        if (e instanceof RadarEntry s) return s.scale;
        return 1f;
    }
    private float getElementRotation(Object e) {
        if (e instanceof SlotEntry s) return s.rotation;
        if (e instanceof StaticTextEntry s) return s.rotation;
        if (e instanceof ImageEntry s) return s.rotation;
        if (e instanceof RadarEntry s) return s.rotation;
        return 0f;
    }

    private void setElementPos(Object e, int x, int y) {
        if (e instanceof SlotEntry s) { s.posX = x; s.posY = y; }
        else if (e instanceof StaticTextEntry s) { s.posX = x; s.posY = y; }
        else if (e instanceof ImageEntry s) { s.posX = x; s.posY = y; }
        else if (e instanceof RadarEntry s) { s.posX = x; s.posY = y; }
    }
    private void setElementScale(Object e, float s) {
        if (e instanceof SlotEntry se) se.scale = s;
        else if (e instanceof StaticTextEntry se) se.scale = s;
        else if (e instanceof ImageEntry se) se.scale = s;
        else if (e instanceof RadarEntry se) se.scale = s;
    }
    private void setElementRotation(Object e, float r) {
        if (e instanceof SlotEntry se) se.rotation = r % 360;
        else if (e instanceof StaticTextEntry se) se.rotation = r % 360;
        else if (e instanceof ImageEntry se) se.rotation = r % 360;
        else if (e instanceof RadarEntry se) se.rotation = r % 360;
    }
    /** 坞 X：删除卡片、画布槽位、服务端数据 */
    private void deleteSourceCard(SourceCard card, int idx) {
        if (card.type == 0 && !BlockPos.ZERO.equals(card.sourcePos)) {
            allSlots.removeIf(s -> s.sourcePos.equals(card.sourcePos));
            PacketDistributor.sendToServer(new RemoveSlotSourcePayload(terminalPos, card.sourcePos));
        }
        if (card.type == 1 && card.imageId != null) {
            images.removeIf(im -> im.imageId.equals(card.imageId));
            PacketDistributor.sendToServer(new RemoveImagePayload(terminalPos, card.imageId));
        }
        if (card.type == 2) {
            radars.clear();
        }
        sourceCards.remove(idx);
    }

    /** 工具栏🗑：只删画布槽位，保留坞卡片 */
    private void deleteElement(Object e) {
        if (e instanceof SlotEntry s) {
            allSlots.remove(s);
            PacketDistributor.sendToServer(new RemoveSlotPayload(terminalPos, s.sourcePos));
            // sourceCards 不会被清除（refreshSourceCards 只增不减），卡片保留
        } else if (e instanceof StaticTextEntry s) {
            int idx = staticTexts.indexOf(s);
            staticTexts.remove(s);
            PacketDistributor.sendToServer(new RemoveStaticTextPayload(terminalPos, idx));
        } else if (e instanceof ImageEntry s) {
            images.remove(s);
            PacketDistributor.sendToServer(new RemoveImagePayload(terminalPos, s.imageId));
        } else if (e instanceof RadarEntry s) {
            int idx = radars.indexOf(s);
            radars.remove(s);
            PacketDistributor.sendToServer(new RemoveRadarSlotPayload(terminalPos, idx));
        }
    }

    private void onLayersChanged() { currentLayer = layerPanel.getCurrentLayer(); }

    private void onAnimChanged(String action) {
        if (!(selectedElement instanceof SlotEntry se)) return;
        if (action.startsWith("capture:")) {
            String[] parts = action.split(":");
            int animIdx = Integer.parseInt(parts[1]), kfIdx = Integer.parseInt(parts[2]);
            if (animIdx >= 0 && animIdx < se.animations.size() && kfIdx >= 0 && kfIdx < se.animations.get(animIdx).keyframes.size()) {
                var k = se.animations.get(animIdx).keyframes.get(kfIdx);
                k.posX = (float) se.posX; k.posY = (float) se.posY;
                k.scale = se.scale; k.rotation = se.rotation;
                k.color = se.color; k.alpha = se.alpha;
            }
        }
    }

    private void onStyleParamChanged(String param) {
        if (!(selectedElement instanceof SlotEntry se)) return;
        if (param.equals("mode")) return; // mode already set by panel
        if (param.startsWith("max:")) {
            try { se.displayMax = Float.parseFloat(param.substring(4)); } catch (NumberFormatException ignored) {}
        } else if (param.startsWith("min:")) {
            try { se.displayMin = Float.parseFloat(param.substring(4)); } catch (NumberFormatException ignored) {}
        } else if (param.startsWith("unit:")) {
            se.displayUnit = param.substring(5);
        }
    }

    private void onColorConfirmed(int argb) {
        if (colorEditTarget != null) {
            int color = argb & 0xFFFFFF;
            int alpha = (argb >> 24) & 0xFF;
            if (colorEditTarget instanceof SlotEntry s) { s.color = color; s.alpha = alpha; }
            else if (colorEditTarget instanceof StaticTextEntry s) { s.color = color; s.alpha = alpha; }
            else if (colorEditTarget instanceof ImageEntry s) { s.alpha = alpha; }
            else if (colorEditTarget instanceof RadarEntry s) { s.alpha = alpha; }
        }
    }

    /** 保存所有修改到服务端 */
    private final Set<StaticTextEntry> savedStaticTexts = new HashSet<>();
    private final Set<UUID> savedImages = new HashSet<>();

    private void saveAll() {
        CompoundTag full = new CompoundTag();
        full.putLong("TerminalPos", terminalPos.asLong());

        // 图层结构
        ListTag layersTag = new ListTag();
        for (ProLayerPanel.LayerInfo li : layers()) {
            CompoundTag lc = new CompoundTag();
            lc.putString("Name", li.name);
            lc.putBoolean("Visible", li.visible);
            lc.putBoolean("Locked", li.locked);
            lc.putBoolean("Frozen", li.frozen);
            ListTag sl = new ListTag();
            for (SlotEntry s : allSlots) {
                if (s.layerIndex == layers().indexOf(li)) sl.add(serializeSlot(s));
            }
            lc.put("Slots", sl);
            layersTag.add(lc);
        }
        full.put("Layers", layersTag);

        // 静态文本
        ListTag stTag = new ListTag();
        for (StaticTextEntry e : staticTexts) {
            CompoundTag t = new CompoundTag();
            t.putString("text", e.text); t.putInt("posX", e.posX); t.putInt("posY", e.posY);
            t.putFloat("scale", e.scale); t.putFloat("rotation", e.rotation);
            t.putInt("color", e.color); t.putInt("alpha", e.alpha); t.putInt("layerIndex", e.layerIndex);
            stTag.add(t);
        }
        full.put("StaticTexts", stTag);

        // 图片
        ListTag imgTag = new ListTag();
        for (ImageEntry e : images) {
            CompoundTag t = new CompoundTag();
            t.putUUID("ImageId", e.imageId); t.putString("FileName", e.fileName);
            t.putByteArray("ImageData", e.imageData);
            t.putInt("PosX", e.posX); t.putInt("PosY", e.posY);
            t.putFloat("Scale", e.scale); t.putFloat("Rotation", e.rotation);
            t.putInt("Alpha", e.alpha); t.putInt("layerIndex", e.layerIndex);
            imgTag.add(t);
        }
        full.put("Images", imgTag);

        // 雷达
        ListTag radTag = new ListTag();
        for (RadarEntry e : radars) {
            CompoundTag t = new CompoundTag();
            t.putInt("PosX", e.posX); t.putInt("PosY", e.posY);
            t.putFloat("Scale", e.scale); t.putFloat("Rotation", e.rotation);
            t.putInt("Alpha", e.alpha); t.putInt("RadarRange", e.radarRange);
            t.putInt("layerIndex", e.layerIndex);
            radTag.add(t);
        }
        full.put("RadarSlots", radTag);

        PacketDistributor.sendToServer(new SaveProConfigPayload(full));
    }

    private CompoundTag serializeSlot(SlotEntry s) {
        CompoundTag t = new CompoundTag();
        t.putLong("SourcePos", s.sourcePos.asLong());
        t.putInt("PosX", s.posX); t.putInt("PosY", s.posY);
        t.putFloat("Scale", s.scale); t.putFloat("Rotation", s.rotation);
        t.putInt("Color", s.color); t.putInt("Alpha", s.alpha);
        t.putString("LastData", s.text); t.putInt("DisplayLine", s.displayLine);
        t.putInt("DisplayMode", s.displayMode);
        t.putFloat("DisplayMax", s.displayMax); t.putFloat("DisplayMin", s.displayMin);
        t.putString("DisplayUnit", s.displayUnit != null ? s.displayUnit : "");
        t.putInt("SlotId", s.instanceId);
        if (s.sourceName != null) t.putString("SourceName", s.sourceName);
        net.minecraft.nbt.ListTag animTag = new net.minecraft.nbt.ListTag();
        for (var a : s.animations) animTag.add(a.serialize());
        t.put("Animations", animTag);
        return t;
    }

    private static int[] gaugeSize(int mode) {
        return switch (mode) {
            case 1 -> new int[]{140, 40};  // 进度条
            case 2, 5 -> new int[]{60, 140}; // 高度计
            case 3 -> new int[]{90, 90};   // 表盘
            case 4 -> new int[]{130, 32};  // 数字
            default -> new int[]{80, 20};  // fallback
        };
    }

    private boolean button(int b) { return b == 0; }

    private static String t(String key) { return Component.translatable(key).getString(); }
}
