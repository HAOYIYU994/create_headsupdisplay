package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.network.UpdateNbtReaderConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class NbtReaderScreen extends Screen {
    private static final int WIDTH = 260, HEIGHT = 200;
    private static final int TREE_X = 10, TREE_Y = 26, TREE_W = 240, TREE_ITEM_H = 12;

    private int guiLeft, guiTop;

    private final BlockPos probePos;
    private final String targetName;
    private String nbtPath;
    private String probeName;
    private int updateInterval;
    private String lastValue;
    private CompoundTag nbtTree;

    private int treeScrollOffset;
    private boolean editingName;
    private String nameBuffer;
    private final Set<String> expandedPaths = new HashSet<>();
    private boolean expandedToSelection;

    public NbtReaderScreen(CompoundTag data) {
        super(Component.translatable("gui.create_headsupdisplay.nbt_reader.title"));
        this.probePos = BlockPos.of(data.getLong("ProbePos"));
        this.targetName = data.getString("TargetName");
        this.nbtPath = data.getString("NbtPath");
        this.probeName = data.getString("ProbeName");
        this.updateInterval = Math.max(1, data.getInt("UpdateInterval"));
        this.lastValue = data.getString("LastValue");
        this.nbtTree = data.getCompound("NbtTree");
        this.nameBuffer = this.probeName;
    }

    @Override protected void init() { super.init(); guiLeft = (width - WIDTH) / 2; guiTop = (height - HEIGHT) / 2; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        var f = Minecraft.getInstance().font;
        g.fill(guiLeft, guiTop, guiLeft + WIDTH, guiTop + HEIGHT, 0xCC_2C2C2C);
        g.fill(guiLeft + 1, guiTop + 1, guiLeft + WIDTH - 1, guiTop + HEIGHT - 1, 0xFF_3C3C3C);
        g.drawString(f, title, guiLeft + 10, guiTop + 8, 0xFFFFFF, false);

        // 目标方块 + 当前路径
        g.drawString(f, Component.translatable("gui.create_headsupdisplay.nbt_reader.target", targetName).getString(),
                guiLeft + TREE_X, guiTop + TREE_Y - 16, 0xBBBBBB, false);
        if (!nbtPath.isEmpty())
            g.drawString(f, "> " + nbtPath, guiLeft + TREE_X, guiTop + TREE_Y - 4, 0x55FF55, false);

        // NBT 树
        if (!expandedToSelection && !nbtPath.isEmpty()) { expandToPath(nbtPath); expandedToSelection = true; }
        renderNbtTree(g, mx, my);

        // 底部：名称、间隔、值、保存
        int by = guiTop + HEIGHT - 50;
        g.drawString(f, Component.translatable("gui.create_headsupdisplay.data_probe.name").getString(), guiLeft + 10, by, 0xAAAAAA, false);
        int nw = Math.max(60, f.width(editingName ? nameBuffer + "_" : nameBuffer) + 8);
        g.fill(guiLeft + 50, by - 1, guiLeft + 50 + nw, by + 13, 0xFF_1A1A1A);
        g.drawString(f, editingName ? nameBuffer + "_" : nameBuffer, guiLeft + 52, by + 2, editingName ? 0x55FF55 : 0xFFFFFF, false);

        String intL = Component.translatable("gui.create_headsupdisplay.data_probe.interval", updateInterval).getString();
        g.drawString(f, intL, guiLeft + 140, by, 0xAAAAAA, false);
        int bx = guiLeft + 210;
        g.fill(bx, by - 1, bx + 12, by + 13, 0xFF_555555); g.drawString(f, "-", bx + 4, by + 2, 0xFFAAAA, false);
        g.fill(bx + 16, by - 1, bx + 28, by + 13, 0xFF_555555); g.drawString(f, "+", bx + 19, by + 2, 0xAAFFAA, false);

        g.drawString(f, Component.translatable("gui.create_headsupdisplay.data_probe.value", lastValue).getString(), guiLeft + 10, by + 18, 0xFFFFFF, false);

        int bty = guiTop + HEIGHT - 22;
        String sl = Component.translatable("gui.create_headsupdisplay.data_probe.save").getString(); int sw = f.width(sl) + 12;
        g.fill(guiLeft + WIDTH - sw - 40, bty - 1, guiLeft + WIDTH - 40, bty + 15, 0xFF_448844);
        g.drawString(f, sl, guiLeft + WIDTH - sw - 34, bty + 3, 0xFFFFFF, false);
        String cl = Component.translatable("gui.create_headsupdisplay.data_probe.cancel").getString();
        g.fill(guiLeft + WIDTH - sw - 100, bty - 1, guiLeft + WIDTH - sw - 46, bty + 15, 0xFF_664444);
        g.drawString(f, cl, guiLeft + WIDTH - sw - 96, bty + 3, 0xFFFFFF, false);
    }

    private void renderNbtTree(GuiGraphics g, int mx, int my) {
        var f = Minecraft.getInstance().font;
        List<FlatNode> flat = buildFlatTree();
        int sy = guiTop + TREE_Y, bt = guiTop + HEIGHT - 58;
        g.fill(guiLeft + TREE_X - 1, sy - 1, guiLeft + TREE_X + TREE_W, bt + 1, 0xFF_2A2A2A);
        for (int i = 0; i < flat.size(); i++) {
            int y = sy + i * TREE_ITEM_H - treeScrollOffset * TREE_ITEM_H;
            if (y < sy || y > bt) continue;
            FlatNode n = flat.get(i);
            int x = guiLeft + TREE_X + 4 + n.depth * 8;
            String prefix = n.expandable ? (n.expanded ? "▼ " : "▶ ") : "  ";
            String d = prefix + n.label;
            int color = n.isLeaf ? 0xCCCCCC : 0xFFFFFF;
            if (n.path.equals(nbtPath)) color = 0x55FF55;
            g.drawString(f, d, x, y, color, false);
            if (n.valueSuffix != null && !n.valueSuffix.isEmpty())
                g.drawString(f, n.valueSuffix, x + f.width(d) + 4, y, 0x888888, false);
        }
    }

    private List<FlatNode> buildFlatTree() { List<FlatNode> l = new ArrayList<>(); buildTree(l, "", nbtTree, 0); return l; }

    private void buildTree(List<FlatNode> l, String path, Tag tag, int depth) {
        if (tag instanceof CompoundTag ct) {
            boolean exp = expandedPaths.contains(path.isEmpty() ? "/" : path);
            String lbl = path.isEmpty() ? "/" : path.substring(path.lastIndexOf('.') + 1);
            l.add(new FlatNode(lbl, null, depth, true, exp, path, false));
            if (exp || path.isEmpty()) for (String k : ct.getAllKeys()) buildTree(l, path.isEmpty() ? k : path + "." + k, ct.get(k), depth + 1);
        } else if (tag instanceof ListTag lt) {
            boolean exp = expandedPaths.contains(path);
            l.add(new FlatNode(path.substring(path.lastIndexOf('.') + 1) + "[" + lt.size() + "]", null, depth, true, exp, path, false));
            if (exp) for (int i = 0; i < Math.min(lt.size(), 50); i++) buildTree(l, path + "[" + i + "]", lt.get(i), depth + 1);
        } else {
            String k = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            l.add(new FlatNode(k, tagToString(tag), depth, false, false, path, true));
        }
    }

    private String tagToString(Tag t) { return switch (t) { case ByteTag b -> String.valueOf(b.getAsByte()); case ShortTag s -> String.valueOf(s.getAsShort()); case IntTag i -> String.valueOf(i.getAsInt()); case LongTag lg -> String.valueOf(lg.getAsLong()); case FloatTag f -> String.valueOf(f.getAsFloat()); case DoubleTag d -> String.valueOf(d.getAsDouble()); case StringTag s -> "\"" + s.getAsString() + "\""; default -> t.getAsString(); }; }

    private void expandToPath(String path) {
        String[] parts = path.split("\\."); StringBuilder sb = new StringBuilder();
        for (String part : parts) { int bi = part.indexOf('['); if (bi > 0) { if (!sb.isEmpty()) sb.append('.'); sb.append(part, 0, bi); expandedPaths.add(sb.toString()); } if (!sb.isEmpty()) sb.append('.'); sb.append(part); expandedPaths.add(sb.toString()); }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false; int x = (int) mx, y = (int) my; var f = Minecraft.getInstance().font;
        List<FlatNode> flat = buildFlatTree();
        int sy = guiTop + TREE_Y, bt = guiTop + HEIGHT - 58;
        if (x >= guiLeft + TREE_X && x <= guiLeft + TREE_X + TREE_W && y >= sy && y <= bt) {
            for (int i = 0; i < flat.size(); i++) {
                int ny = sy + i * TREE_ITEM_H - treeScrollOffset * TREE_ITEM_H;
                if (ny < sy || ny + TREE_ITEM_H > bt) continue;
                if (y >= ny && y < ny + TREE_ITEM_H) { FlatNode n = flat.get(i);
                    if (n.expandable) { String k = n.path.isEmpty() ? "/" : n.path; if (expandedPaths.contains(k)) expandedPaths.remove(k); else expandedPaths.add(k); }
                    else if (n.isLeaf) nbtPath = n.path;
                    return true; }
            }
        }
        int by = guiTop + HEIGHT - 50;
        if (x >= guiLeft + 50 && x <= guiLeft + 110 && y >= by - 1 && y <= by + 13) { editingName = !editingName; return true; }
        int bx = guiLeft + 210;
        if (x >= bx && x <= bx + 12 && y >= by - 1 && y <= by + 13) { if (updateInterval > 1) updateInterval--; return true; }
        if (x >= bx + 16 && x <= bx + 28 && y >= by - 1 && y <= by + 13) { if (updateInterval < 100) updateInterval++; return true; }
        int bty = guiTop + HEIGHT - 22; String sl = Component.translatable("gui.create_headsupdisplay.data_probe.save").getString(); int sw = f.width(sl) + 12;
        if (x >= guiLeft + WIDTH - sw - 40 && x <= guiLeft + WIDTH - 40 && y >= bty - 1 && y <= bty + 15) { saveAndClose(); return true; }
        if (x >= guiLeft + WIDTH - sw - 100 && x <= guiLeft + WIDTH - sw - 46 && y >= bty - 1 && y <= bty + 15) { onClose(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = Math.max(0, buildFlatTree().size() - 14); if (sy > 0 && treeScrollOffset > 0) treeScrollOffset--; if (sy < 0 && treeScrollOffset < max) treeScrollOffset++; return true;
    }

    @Override public boolean keyPressed(int kc, int sc, int mod) {
        if (editingName) return txt(kc, nameBuffer, v -> { nameBuffer = v; probeName = v; }, () -> editingName = false);
        return super.keyPressed(kc, sc, mod);
    }
    private boolean txt(int kc, String cur, java.util.function.Consumer<String> set, Runnable done) { if (kc == 257 || kc == 335) { done.run(); return true; } if (kc == 256) { done.run(); return true; } if (kc == 259) { if (!cur.isEmpty()) set.accept(cur.substring(0, cur.length() - 1)); return true; } return false; }
    @Override public boolean charTyped(char cp, int mod) { if (editingName && cp >= 32 && cp < 127) { nameBuffer += cp; probeName = nameBuffer; return true; } return super.charTyped(cp, mod); }

    private void saveAndClose() { PacketDistributor.sendToServer(new UpdateNbtReaderConfigPayload(probePos, nbtPath, probeName, updateInterval)); onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    private record FlatNode(String label, String valueSuffix, int depth, boolean expandable, boolean expanded, String path, boolean isLeaf) {}
}
