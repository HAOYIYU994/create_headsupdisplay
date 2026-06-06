package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.config.HudPositionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientHudData {
    private static HudPositionConfig currentConfig = HudPositionConfig.defaultConfig();
    private static CompoundTag currentDisplayData = new CompoundTag();
    private static List<SlotRenderData> slots = new ArrayList<>();
    private static List<StaticTextRenderData> staticTexts = new ArrayList<>();
    private static List<ImageRenderData> images = new ArrayList<>();

    // 雷达数据
    private static List<RadarRenderData> radarSlots = new ArrayList<>();
    private static List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> radarTracks = new ArrayList<>();
    private static float radarSweepAngle = 0f;
    private static float radarGlobalRange = 50f;
    private static double radarX, radarY, radarZ;

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

        // 解析图片槽位
        images.clear();
        if (data.contains("imageCount")) {
            int imageCount = data.getInt("imageCount");
            for (int i = 0; i < imageCount; i++) {
                CompoundTag tag = data.getCompound("image_" + i);
                UUID imageId = tag.getUUID("ImageId");
                String fileName = tag.getString("FileName");
                byte[] imageBytes = tag.getByteArray("ImageData");
                int posX = tag.getInt("PosX");
                int posY = tag.getInt("PosY");
                float scale = tag.getFloat("Scale");
                float rotation = tag.getFloat("Rotation");
                int alpha = tag.getInt("Alpha");
                images.add(new ImageRenderData(imageId, imageBytes, fileName, posX, posY, scale, rotation, alpha));
                DynamicTextureCache.getOrCreate(imageId, imageBytes);
            }
        }

        // 解析雷达图槽位
        radarSlots.clear();
        if (data.contains("radarCount")) {
            int radarCount = data.getInt("radarCount");
            for (int i = 0; i < radarCount; i++) {
                CompoundTag tag = data.getCompound("radar_" + i);
                radarSlots.add(new RadarRenderData(
                    tag.getInt("PosX"), tag.getInt("PosY"),
                    tag.getFloat("Scale"), tag.getFloat("Rotation"),
                    tag.getInt("Alpha"), tag.getInt("RadarRange")
                ));
            }
            System.out.println("[HUD] Received " + radarCount + " radar slots, tracks=" + radarTracks.size());
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

    public static List<ImageRenderData> getImages() {
        return images;
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

    // 图片槽位渲染数据
    public static class ImageRenderData {
        public final UUID imageId;
        public final byte[] imageData;
        public final String fileName;
        public final int posX, posY;
        public final float scale;
        public final float rotation;
        public final int alpha;

        public ImageRenderData(UUID imageId, byte[] imageData, String fileName, int posX, int posY, float scale, float rotation, int alpha) {
            this.imageId = imageId;
            this.imageData = imageData;
            this.fileName = fileName;
            this.posX = posX;
            this.posY = posY;
            this.scale = scale;
            this.rotation = rotation;
            this.alpha = alpha;
        }
    }

    // ========== 雷达数据 ==========

    /** 更新雷达轨迹+扫描角度+雷达坐标 */
    public static void updateRadarTracks(
            List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> tracks,
            float sweepAngle, float range, double rX, double rY, double rZ) {
        radarTracks = tracks;
        radarSweepAngle = sweepAngle;
        radarGlobalRange = range;
        radarX = rX;
        radarY = rY;
        radarZ = rZ;
    }

    public static float getRadarSweepAngle() { return radarSweepAngle; }
    public static float getRadarGlobalRange() { return radarGlobalRange; }
    public static double getRadarX() { return radarX; }
    public static double getRadarY() { return radarY; }
    public static double getRadarZ() { return radarZ; }

    /** 更新雷达槽位配置（由 OpenOmniCoreScreenPayload 触发） */
    public static void updateRadarSlots(List<RadarRenderData> slots) {
        radarSlots = slots;
    }

    public static List<RadarRenderData> getRadarSlots() { return radarSlots; }

    public static List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> getRadarTracks() {
        return radarTracks;
    }

    /** 雷达槽位渲染数据 */
    public static class RadarRenderData {
        public final int posX, posY;
        public final float scale;
        public final float rotation;
        public final int alpha;
        public final int radarRange;

        public RadarRenderData(int posX, int posY, float scale, float rotation, int alpha, int range) {
            this.posX = posX;
            this.posY = posY;
            this.scale = scale;
            this.rotation = rotation;
            this.alpha = alpha;
            this.radarRange = range;
        }
    }
}