package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.config.HudPositionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientHudData {
    private static HudPositionConfig currentConfig = HudPositionConfig.defaultConfig();
    private static CompoundTag currentDisplayData = new CompoundTag();
    private static List<SlotRenderData> slots = new ArrayList<>();
    private static List<StaticTextRenderData> staticTexts = new ArrayList<>();

    public static void updateConfig(HudPositionConfig config) {
        currentConfig = config;
    }

    public static HudPositionConfig getCurrentConfig() {
        return currentConfig;
    }

    public static void updateDisplayData(CompoundTag data) {
        currentDisplayData = data;
        slots.clear();
        staticTexts.clear();

        // 解析数据源槽位
        if (data.contains("slotCount")) {
            int count = data.getInt("slotCount");
            for (int i = 0; i < count; i++) {
                CompoundTag slotTag = data.getCompound("slot_" + i);
                BlockPos sourcePos = BlockPos.of(slotTag.getLong("sourcePos"));
                int posX = slotTag.getInt("posX");
                int posY = slotTag.getInt("posY");
                float scale = slotTag.getFloat("scale");
                String text = slotTag.getString("text");
                int displayLine = slotTag.getInt("displayLine");
                float rotation = slotTag.getFloat("rotation");
                int color = slotTag.getInt("color");
                int alpha = slotTag.getInt("alpha");
                slots.add(new SlotRenderData(sourcePos, posX, posY, scale, text, displayLine, rotation, color, alpha));
            }
        }

        // 解析静态文本槽位
        if (data.contains("staticCount")) {
            int staticCount = data.getInt("staticCount");
            for (int i = 0; i < staticCount; i++) {
                CompoundTag tag = data.getCompound("static_" + i);
                String text = tag.getString("text");
                int posX = tag.getInt("posX");
                int posY = tag.getInt("posY");
                float scale = tag.getFloat("scale");
                float rotation = tag.getFloat("rotation");
                int color = tag.getInt("color");
                int alpha = tag.getInt("alpha");
                staticTexts.add(new StaticTextRenderData(text, posX, posY, scale, rotation, color, alpha));
            }
        }
    }

    public static CompoundTag getCurrentDisplayData() {
        return currentDisplayData;
    }

    public static List<SlotRenderData> getSlots() {
        return slots;
    }

    public static List<StaticTextRenderData> getStaticTextSlots() {
        return staticTexts;
    }

    // 数据源槽位渲染数据
    public static class SlotRenderData {
        public final BlockPos sourcePos;
        public final int posX, posY;
        public final float scale;
        public final String text;
        public final int displayLine;
        public final float rotation;
        public final int color;
        public final int alpha;

        public SlotRenderData(BlockPos sourcePos, int posX, int posY, float scale, String text, int displayLine, float rotation, int color, int alpha) {
            this.sourcePos = sourcePos;
            this.posX = posX;
            this.posY = posY;
            this.scale = scale;
            this.text = text;
            this.displayLine = displayLine;
            this.rotation = rotation;
            this.color = color;
            this.alpha = alpha;
        }
    }

    // 静态文本槽位渲染数据
    public static class StaticTextRenderData {
        public final String text;
        public final int posX, posY;
        public final float scale;
        public final float rotation;
        public final int color;
        public final int alpha;

        public StaticTextRenderData(String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
            this.text = text;
            this.posX = posX;
            this.posY = posY;
            this.scale = scale;
            this.rotation = rotation;
            this.color = color;
            this.alpha = alpha;
        }
    }
}