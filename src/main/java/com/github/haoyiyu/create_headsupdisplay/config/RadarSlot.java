package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;

/**
 * 雷达图槽位配置模型。
 * 类似 ImageSlot，在 HUD 上渲染迷你雷达图，支持拖拽、旋转、缩放。
 */
public class RadarSlot {
    private int posX, posY;
    private float scale;
    private float rotation;
    private int alpha;
    private int radarRange;  // 雷达显示范围（米），默认 50
    private final java.util.List<SlotAnimation> animations = new java.util.ArrayList<>();
    public java.util.List<SlotAnimation> getAnimations() { return animations; }

    public RadarSlot() {
        this.posX = 200;
        this.posY = 10;
        this.scale = 1.0f;
        this.rotation = 0f;
        this.alpha = 200;
        this.radarRange = 50;
    }

    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public float getScale() { return scale; }
    public float getRotation() { return rotation; }
    public int getAlpha() { return alpha; }
    public int getRadarRange() { return radarRange; }

    public void setPos(int x, int y) { posX = x; posY = y; }
    public void setScale(float scale) { this.scale = scale; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public void setAlpha(int alpha) { this.alpha = alpha; }
    public void setRadarRange(int range) { this.radarRange = range; }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("PosX", posX);
        tag.putInt("PosY", posY);
        tag.putFloat("Scale", scale);
        tag.putFloat("Rotation", rotation);
        tag.putInt("Alpha", alpha);
        tag.putInt("RadarRange", radarRange);
        AnimationIO.write(tag, animations);
        return tag;
    }

    public static RadarSlot deserialize(CompoundTag tag) {
        RadarSlot slot = new RadarSlot();
        slot.posX = tag.getInt("PosX");
        slot.posY = tag.getInt("PosY");
        slot.scale = tag.getFloat("Scale");
        slot.rotation = tag.getFloat("Rotation");
        slot.alpha = tag.getInt("Alpha");
        if (tag.contains("RadarRange")) slot.radarRange = tag.getInt("RadarRange");
        AnimationIO.read(tag, slot.animations);
        return slot;
    }
}
