package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class DisplaySlot {
    private final BlockPos sourcePos;
    private int posX, posY;
    private float scale;
    private String lastData;
    private int displayLine;
    private float rotation;
    private int color = 0xFFFFFF;
    private int alpha = 255;
    private String sourceName;  // 数据源名称（仅在终端编辑界面显示）

    public DisplaySlot(BlockPos sourcePos) {
        this.sourcePos = sourcePos;
        this.posX = 10;
        this.posY = 10;
        this.scale = 1.0f;
        this.lastData = "";
        this.displayLine = 0;
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
    public String getSourceName() { return sourceName; }

    public void setPos(int x, int y) { this.posX = x; this.posY = y; }
    public void setScale(float scale) { this.scale = scale; }
    public void setLastData(String data) { this.lastData = data; }
    public void setDisplayLine(int line) { this.displayLine = line; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public void setColor(int color) { this.color = color; }
    public void setAlpha(int alpha) { this.alpha = alpha; }
    public void setSourceName(String name) { this.sourceName = name; }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("SourcePos", sourcePos.asLong());
        tag.putInt("PosX", posX);
        tag.putInt("PosY", posY);
        tag.putFloat("Scale", scale);
        tag.putString("LastData", lastData);
        tag.putInt("DisplayLine", displayLine);
        tag.putFloat("Rotation", rotation);
        tag.putInt("Color", color);
        tag.putInt("Alpha", alpha);
        if (sourceName != null) tag.putString("SourceName", sourceName);
        return tag;
    }

    public static DisplaySlot deserialize(CompoundTag tag) {
        BlockPos sourcePos = BlockPos.of(tag.getLong("SourcePos"));
        DisplaySlot slot = new DisplaySlot(sourcePos);
        slot.posX = tag.getInt("PosX");
        slot.posY = tag.getInt("PosY");
        slot.scale = tag.getFloat("Scale");
        slot.lastData = tag.getString("LastData");
        slot.displayLine = tag.getInt("DisplayLine");
        slot.rotation = tag.getFloat("Rotation");
        slot.color = tag.getInt("Color");
        slot.alpha = tag.getInt("Alpha");
        if (tag.contains("SourceName")) slot.sourceName = tag.getString("SourceName");
        return slot;
    }
}