package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.ArrayList;
import java.util.List;

public class DisplaySlot {
    private final BlockPos sourcePos;
    private int posX, posY;
    private float scale;
    private String lastData;
    private int displayLine;
    private int displayMode;     // 0=text 1=progress 2=altimeter 3=dial 4=digital
    private float displayMax;    // 量程上限
    private float displayMin;    // 量程下限
    private String displayUnit;  // 单位标签
    private float rotation;
    private int color = 0xFFFFFF;
    private int alpha = 255;
    private int slotId; // 唯一标识，允许多个同源槽位
    private String sourceName;  // 数据源名称
    private final List<SlotAnimation> animations = new ArrayList<>();

    public DisplaySlot(BlockPos sourcePos) {
        this.sourcePos = sourcePos;
        this.posX = 10;
        this.posY = 10;
        this.scale = 1.0f;
        this.lastData = "";
        this.displayLine = 0;
        this.displayMode = 0;
        this.displayMax = 100f;
        this.displayMin = 0f;
        this.displayUnit = "";
        this.rotation = 0f;
    }

    public BlockPos getSourcePos() { return sourcePos; }
    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public float getScale() { return scale; }
    public String getLastData() { return lastData; }
    public int getDisplayLine() { return displayLine; }
    public float getRotation() { return rotation; }
    public int getColor() { return color; }
    public int getAlpha() { return alpha;}
    public int getSlotId() { return slotId; }
    public void setSlotId(int id) { this.slotId = id; }
    public String getSourceName() { return sourceName; }

    public void setPos(int x, int y) { this.posX = x; this.posY = y; }
    public void setScale(float scale) { this.scale = scale; }
    public void setLastData(String data) { this.lastData = data; }
    public void setDisplayLine(int line) { this.displayLine = line; }
    public int getDisplayMode() { return displayMode; }
    public void setDisplayMode(int m) { this.displayMode = m; }
    public float getDisplayMax() { return displayMax; }
    public void setDisplayMax(float v) { this.displayMax = v; }
    public float getDisplayMin() { return displayMin; }
    public void setDisplayMin(float v) { this.displayMin = v; }
    public String getDisplayUnit() { return displayUnit; }
    public void setDisplayUnit(String u) { this.displayUnit = u; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public void setColor(int color) { this.color = color; }
    public void setAlpha(int alpha) { this.alpha = alpha; }
    public void setSourceName(String name) { this.sourceName = name; }
    public List<SlotAnimation> getAnimations() { return animations; }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("SourcePos", sourcePos.asLong());
        tag.putInt("PosX", posX);
        tag.putInt("PosY", posY);
        tag.putFloat("Scale", scale);
        tag.putString("LastData", lastData);
        tag.putInt("DisplayLine", displayLine);
        tag.putInt("DisplayMode", displayMode);
        tag.putFloat("DisplayMax", displayMax);
        tag.putFloat("DisplayMin", displayMin);
        tag.putString("DisplayUnit", displayUnit != null ? displayUnit : "");
        tag.putFloat("Rotation", rotation);
        tag.putInt("Color", color);
        tag.putInt("Alpha", alpha);
        tag.putInt("SlotId", slotId);
        if (sourceName != null) tag.putString("SourceName", sourceName);
        ListTag animTag = new ListTag();
        for (SlotAnimation a : animations) animTag.add(a.serialize());
        tag.put("Animations", animTag);
        return tag;
    }

    public static DisplaySlot deserialize(CompoundTag tag) {
        BlockPos sourcePos = BlockPos.of(tag.getLong("SourcePos"));
        DisplaySlot slot = new DisplaySlot(sourcePos);
        slot.slotId = tag.getInt("SlotId");
        slot.posX = tag.getInt("PosX");
        slot.posY = tag.getInt("PosY");
        slot.scale = tag.getFloat("Scale");
        slot.lastData = tag.getString("LastData");
        slot.displayLine = tag.getInt("DisplayLine");
        slot.displayMode = tag.getInt("DisplayMode");
        slot.displayMax = tag.getFloat("DisplayMax");
        slot.displayMin = tag.getFloat("DisplayMin");
        slot.displayUnit = tag.getString("DisplayUnit");
        slot.rotation = tag.getFloat("Rotation");
        slot.color = tag.getInt("Color");
        slot.alpha = tag.getInt("Alpha");
        if (tag.contains("SourceName")) slot.sourceName = tag.getString("SourceName");
        if (tag.contains("Animations")) {
            ListTag animTag = tag.getList("Animations", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < animTag.size(); i++)
                slot.animations.add(SlotAnimation.deserialize(animTag.getCompound(i)));
        }
        return slot;
    }
}