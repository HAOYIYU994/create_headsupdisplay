package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.config.HudPositionConfig;
import com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientHudData {
    private static HudPositionConfig currentConfig = HudPositionConfig.defaultConfig();
    private static final Map<BlockPos, TerminalData> dataByTerminal = new ConcurrentHashMap<>();

    public static void updateConfig(HudPositionConfig config) {
        currentConfig = config;
    }

    public static HudPositionConfig getCurrentConfig() {
        return currentConfig;
    }

    /** 接收终端同步数据，按终端隔离存储 */
    public static void updateDisplayData(CompoundTag data) {
        BlockPos terminalPos = data.contains("TerminalPos")
                ? BlockPos.of(data.getLong("TerminalPos")) : BlockPos.ZERO;

        TerminalData td = dataByTerminal.computeIfAbsent(terminalPos, k -> new TerminalData());
        td.update(data);
    }

    /** 获取指定终端的槽位数据 */
    public static List<SlotRenderData> getSlotsFor(BlockPos terminal) {
        TerminalData td = dataByTerminal.get(terminal);
        return td != null ? td.slots : List.of();
    }

    /** 获取指定终端的静态文本 */
    public static List<StaticTextRenderData> getStaticTextsFor(BlockPos terminal) {
        TerminalData td = dataByTerminal.get(terminal);
        return td != null ? td.staticTexts : List.of();
    }

    /** 获取指定终端的图片 */
    public static List<ImageRenderData> getImagesFor(BlockPos terminal) {
        TerminalData td = dataByTerminal.get(terminal);
        return td != null ? td.images : List.of();
    }

    /** 获取指定终端的雷达槽位 */
    public static List<RadarRenderData> getRadarSlotsFor(BlockPos terminal) {
        TerminalData td = dataByTerminal.get(terminal);
        return td != null ? td.radarSlots : List.of();
    }

    // ---- 雷达全局数据（来自雷达 Monitor，非终端级） ----
    private static List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> radarTracks = new ArrayList<>();
    private static float radarSweepAngle = 0f;
    private static float radarGlobalRange = 50f;
    private static double radarX, radarY, radarZ;

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
    public static List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> getRadarTracks() { return radarTracks; }

    // ---- 兼容旧 API（无终端参数时返回第一个终端数据） ----
    public static List<SlotRenderData> getSlots() {
        return dataByTerminal.values().stream().findFirst()
                .map(td -> td.slots).orElse(List.of());
    }
    public static List<StaticTextRenderData> getStaticTextSlots() {
        return dataByTerminal.values().stream().findFirst()
                .map(td -> td.staticTexts).orElse(List.of());
    }
    public static List<ImageRenderData> getImages() {
        return dataByTerminal.values().stream().findFirst()
                .map(td -> td.images).orElse(List.of());
    }
    public static List<RadarRenderData> getRadarSlots() {
        return dataByTerminal.values().stream().findFirst()
                .map(td -> td.radarSlots).orElse(List.of());
    }

    // ========== 渲染数据类 ==========

    public static class SlotRenderData {
        public final BlockPos sourcePos;
        public final int posX, posY, layerIndex;
        public final float scale;
        public final String text;
        public final int displayLine;
        public final int displayMode;
        public final float displayMax, displayMin;
        public final String displayUnit;
        public final float rotation;
        public final int color, alpha;
        public final boolean frozen;
        public final java.util.List<com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation> animations;
        public final ResourceLocation displayModeId;
        public final com.github.haoyiyu.create_headsupdisplay.api.DisplayModeConfig modeConfig;
        public final java.util.List<String> dataValues;
        public final java.util.List<String> sourceNames;

        public SlotRenderData(BlockPos sourcePos, int posX, int posY, float scale, String text, int displayLine,
                              int displayMode, float displayMax, float displayMin, String displayUnit,
                              float rotation, int color, int alpha, int layerIndex, boolean frozen,
                              java.util.List<com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation> animations,
                              ResourceLocation displayModeId,
                              com.github.haoyiyu.create_headsupdisplay.api.DisplayModeConfig modeConfig,
                              java.util.List<String> dataValues,
                              java.util.List<String> sourceNames) {
            this.sourcePos = sourcePos; this.posX = posX; this.posY = posY;
            this.scale = scale; this.text = text; this.displayLine = displayLine;
            this.displayMode = displayMode;
            this.displayMax = displayMax; this.displayMin = displayMin;
            this.displayUnit = displayUnit != null ? displayUnit : "";
            this.rotation = rotation; this.color = color; this.alpha = alpha;
            this.layerIndex = layerIndex; this.frozen = frozen;
            this.animations = animations != null ? animations : java.util.List.of();
            this.displayModeId = displayModeId;
            this.modeConfig = modeConfig != null ? modeConfig : new com.github.haoyiyu.create_headsupdisplay.api.DisplayModeConfig(displayMax, displayMin, displayUnit);
            this.dataValues = dataValues != null ? dataValues : java.util.List.of(text);
            this.sourceNames = sourceNames != null ? sourceNames : java.util.List.of();
        }
    }

    public static class StaticTextRenderData {
        public final String text;
        public final int posX, posY, layerIndex;
        public final float scale, rotation;
        public final int color, alpha;
        public final boolean frozen;
        public final List<SlotAnimation> animations;

        public StaticTextRenderData(String text, int posX, int posY, float scale, float rotation, int color, int alpha, int layerIndex, boolean frozen,
                                    List<SlotAnimation> animations) {
            this.text = text; this.posX = posX; this.posY = posY;
            this.scale = scale; this.rotation = rotation; this.color = color; this.alpha = alpha;
            this.layerIndex = layerIndex; this.frozen = frozen;
            this.animations = animations != null ? animations : List.of();
        }
    }

    public static class ImageRenderData {
        public final UUID imageId;
        public final byte[] imageData;
        public final String fileName;
        public final int posX, posY, layerIndex;
        public final float scale, rotation;
        public final int alpha;
        public final boolean frozen;
        public final List<SlotAnimation> animations;

        public ImageRenderData(UUID imageId, byte[] imageData, String fileName, int posX, int posY, float scale, float rotation, int alpha, int layerIndex, boolean frozen,
                               List<SlotAnimation> animations) {
            this.imageId = imageId; this.imageData = imageData; this.fileName = fileName;
            this.posX = posX; this.posY = posY; this.scale = scale; this.rotation = rotation;
            this.alpha = alpha; this.layerIndex = layerIndex; this.frozen = frozen;
            this.animations = animations != null ? animations : List.of();
        }
    }

    public static class RadarRenderData {
        public final int posX, posY, layerIndex;
        public final float scale, rotation;
        public final int alpha, radarRange;
        public final boolean frozen;
        public final List<SlotAnimation> animations;

        public RadarRenderData(int posX, int posY, float scale, float rotation, int alpha, int range, int layerIndex, boolean frozen,
                               List<SlotAnimation> animations) {
            this.posX = posX; this.posY = posY; this.scale = scale; this.rotation = rotation;
            this.alpha = alpha; this.radarRange = range; this.layerIndex = layerIndex; this.frozen = frozen;
            this.animations = animations != null ? animations : List.of();
        }
    }

    private static List<SlotAnimation> readAnims(CompoundTag tag) {
        var list = new ArrayList<SlotAnimation>();
        com.github.haoyiyu.create_headsupdisplay.config.AnimationIO.read(tag, list);
        return list;
    }

    // ========== 内部数据容器 ==========

    private static class TerminalData {
        List<SlotRenderData> slots = new ArrayList<>();
        List<StaticTextRenderData> staticTexts = new ArrayList<>();
        List<ImageRenderData> images = new ArrayList<>();
        List<RadarRenderData> radarSlots = new ArrayList<>();

        void update(CompoundTag data) {
            slots.clear(); staticTexts.clear(); images.clear(); radarSlots.clear();
            // 解析图层冻结信息
            java.util.Map<Integer, Boolean> frozenMap = new java.util.HashMap<>();
            if (data.contains("layerCount")) {
                int lc = data.getInt("layerCount");
                for (int i = 0; i < lc; i++) {
                    CompoundTag lt = data.getCompound("layer_" + i);
                    if (lt != null) frozenMap.put(i, lt.getBoolean("Frozen"));
                }
            }

            if (data.contains("slotCount")) {
                int count = data.getInt("slotCount");
                for (int i = 0; i < count; i++) {
                    CompoundTag tag = data.getCompound("slot_" + i);
                    BlockPos sp = BlockPos.of(tag.getLong("sourcePos"));
                    int li = tag.getInt("layerIndex");
                    var anims = new java.util.ArrayList<com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation>();
                    com.github.haoyiyu.create_headsupdisplay.config.AnimationIO.read(tag, anims);
                    ResourceLocation dmId = null;
                    if (tag.contains("DisplayModeId")) try { dmId = ResourceLocation.parse(tag.getString("DisplayModeId")); } catch (Exception ignored) {}
                    com.github.haoyiyu.create_headsupdisplay.api.DisplayModeConfig cfg = null;
                    if (tag.contains("ModeConfig")) cfg = com.github.haoyiyu.create_headsupdisplay.api.DisplayModeConfig.deserialize(tag.getCompound("ModeConfig"));
                    java.util.List<String> dv = java.util.List.of(tag.getString("text"));
                    if (tag.contains("DataValues")) {
                        var dvt = tag.getList("DataValues", CompoundTag.TAG_COMPOUND);
                        var list = new java.util.ArrayList<String>();
                        for (int di = 0; di < dvt.size(); di++) list.add(dvt.getCompound(di).getString("Val"));
                        if (!list.isEmpty()) dv = list;
                    }
                    java.util.List<String> sn = java.util.List.of();
                    if (tag.contains("SourceNames")) {
                        var snt = tag.getList("SourceNames", CompoundTag.TAG_COMPOUND);
                        var list = new java.util.ArrayList<String>();
                        for (int di = 0; di < snt.size(); di++) list.add(snt.getCompound(di).getString("Nm"));
                        if (!list.isEmpty()) sn = list;
                    }
                    slots.add(new SlotRenderData(sp, tag.getInt("posX"), tag.getInt("posY"),
                            tag.getFloat("scale"), tag.getString("text"), tag.getInt("displayLine"),
                            tag.getInt("DisplayMode"), tag.getFloat("DisplayMax"), tag.getFloat("DisplayMin"),
                            tag.getString("DisplayUnit"),
                            tag.getFloat("rotation"), tag.getInt("color"), tag.getInt("alpha"),
                            li, frozenMap.getOrDefault(li, false), anims, dmId, cfg, dv, sn));
                }
            }
            if (data.contains("staticCount")) {
                int count = data.getInt("staticCount");
                for (int i = 0; i < count; i++) {
                    CompoundTag tag = data.getCompound("static_" + i);
                    int li = tag.getInt("layerIndex");
                    staticTexts.add(new StaticTextRenderData(tag.getString("text"),
                            tag.getInt("posX"), tag.getInt("posY"), tag.getFloat("scale"),
                            tag.getFloat("rotation"), tag.getInt("color"), tag.getInt("alpha"),
                            li, frozenMap.getOrDefault(li, false), readAnims(tag)));
                }
            }
            if (data.contains("imageCount")) {
                int count = data.getInt("imageCount");
                for (int i = 0; i < count; i++) {
                    CompoundTag tag = data.getCompound("image_" + i);
                    UUID id = tag.getUUID("ImageId"); byte[] bytes = tag.getByteArray("ImageData");
                    int li = tag.getInt("layerIndex");
                    images.add(new ImageRenderData(id, bytes, tag.getString("FileName"),
                            tag.getInt("PosX"), tag.getInt("PosY"), tag.getFloat("Scale"),
                            tag.getFloat("Rotation"), tag.getInt("Alpha"),
                            li, frozenMap.getOrDefault(li, false), readAnims(tag)));
                    DynamicTextureCache.ensureUpdated(id, bytes);
                }
            }
            if (data.contains("radarCount")) {
                int count = data.getInt("radarCount");
                for (int i = 0; i < count; i++) {
                    CompoundTag tag = data.getCompound("radar_" + i);
                    int li = tag.getInt("layerIndex");
                    radarSlots.add(new RadarRenderData(tag.getInt("PosX"), tag.getInt("PosY"),
                            tag.getFloat("Scale"), tag.getFloat("Rotation"),
                            tag.getInt("Alpha"), tag.getInt("RadarRange"),
                            li, frozenMap.getOrDefault(li, false), readAnims(tag)));
                }
            }
        }
    }
}
