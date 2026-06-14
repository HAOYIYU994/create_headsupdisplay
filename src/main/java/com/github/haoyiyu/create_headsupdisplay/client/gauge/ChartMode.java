package com.github.haoyiyu.create_headsupdisplay.client.gauge;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.*;

public class ChartMode implements IDisplayMode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "line_chart");
    // Per-slot buffer: key → rolling list of values
    private static final Map<String, LinkedList<Float>> buffers = new HashMap<>();
    private static final Map<String, Long> lastTicks = new HashMap<>();
    private static final long TICK_MS = 200; // record a point every 200ms

    @Override public ResourceLocation getId() { return ID; }
    @Override public int getMinDataSourceCount() { return 1; }
    @Override public int getMaxDataSourceCount() { return 1; }
    @Override public boolean needsNumericData() { return true; }
    @Override public int getDefaultWidth() { return 160; }
    @Override public int getDefaultHeight() { return 80; }
    @Override
    public List<ConfigParamDescriptor> getConfigParameters() {
        return List.of(ConfigParamDescriptor.of("max",ConfigParamType.FLOAT,100f), ConfigParamDescriptor.of("min",ConfigParamType.FLOAT,0f), ConfigParamDescriptor.of("unit",ConfigParamType.STRING,""), ConfigParamDescriptor.of("historySize",ConfigParamType.INT,60), ConfigParamDescriptor.of("lineColor",ConfigParamType.COLOR,0x00FF00));
    }

    /** Must be called externally with a stable key to feed data into the buffer */
    public static void feed(String key, float value, int maxSize) {
        long now = System.currentTimeMillis();
        Long last = lastTicks.get(key);
        if (last != null && now - last < TICK_MS) return; // rate limit
        lastTicks.put(key, now);
        var buf = buffers.computeIfAbsent(key, k -> new LinkedList<>());
        buf.addLast(value);
        while (buf.size() > maxSize) buf.removeFirst();
    }

    @Override
    public void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        float val = GaugeUtil.parseFloat(dataValues.get(0));
        float mx = config.getMax(), mn = config.getMin();
        int hs = config.getInt("historySize", 60), lc = config.getInt("lineColor", 0x00FF00);
        float range = mx - mn; if (range <= 0) range = 100f;

        // Auto-generate a unique key per slot instance, stored in config
        String key = config.getString("_chartKey", "");
        if (key.isEmpty()) { key = UUID.randomUUID().toString(); config.setString("_chartKey", key); }
        // Feed current value into buffer (rate-limited internally)
        long now = System.currentTimeMillis();
        Long last = lastTicks.get(key);
        if (last == null || now - last >= TICK_MS) {
            lastTicks.put(key, now);
            var buf = buffers.computeIfAbsent(key, k -> new LinkedList<>());
            buf.addLast(val);
            while (buf.size() > hs) buf.removeFirst();
        }

        int m = 4, px = m + 28, pw = w - px - m, py = m, ph = h - m * 2 - 12;

        // Background + border
        g.fill(0, 0, w, h, 0xFF101018);
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0x44666688);

        // Y-axis labels
        g.drawString(font, String.format("%.0f", mx), 2, py, 0xFF888888);
        g.drawString(font, String.format("%.0f", (mx + mn) / 2), 2, py + ph / 2 - 4, 0xFF888888);
        g.drawString(font, String.format("%.0f", mn), 2, py + ph - 8, 0xFF888888);

        // Center grid line
        g.fill(px, py + ph / 2, px + pw, py + ph / 2 + 1, 0x22666688);

        // Current value
        String unit = config.getUnit();
        g.drawString(font, String.format("%.1f%s", val, unit), px + 2, py + ph + 2, 0xFFFFFFFF);

        // Draw line from buffer
        var buf = buffers.get(key);
        if (buf == null || buf.isEmpty()) {
            float pct = Mth.clamp((val - mn) / range, 0f, 1f);
            int dotY = py + ph - (int) (pct * ph);
            g.fill(px + pw / 2 - 2, dotY - 2, px + pw / 2 + 3, dotY + 3, 0xFFFFFFFF);
            return;
        }

        int n = buf.size(), step = Math.max(1, pw / Math.max(1, n - 1));
        int lc24 = 0xFF000000 | (lc & 0xFFFFFF);
        Integer prevX = null, prevY = null;
        int i = 0;
        for (float pv : buf) {
            float pct = Mth.clamp((pv - mn) / range, 0f, 1f);
            int sy = py + ph - (int) (pct * ph);
            int sx = px + Math.min(i * step, pw - 1);
            if (prevX != null) drawLine(g, prevX, prevY, sx, sy, lc24);
            prevX = sx; prevY = sy;
            i++;
        }
        if (prevX != null) g.fill(prevX - 2, prevY - 2, prevX + 3, prevY + 3, 0xFFFFFFFF);
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }
}
