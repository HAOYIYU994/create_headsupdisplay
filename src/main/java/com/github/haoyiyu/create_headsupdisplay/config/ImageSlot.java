package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * 图片槽位数据模型：存储在 DisplayTerminalBlockEntity 中，
 * 与 StaticTextSlot 平行，支持 HUD 上的图片渲染。
 */
public class ImageSlot {
    private final UUID imageId;
    private String fileName;
    private byte[] imageData; // PNG raw bytes
    private int posX, posY;
    private float scale;
    private float rotation;
    private int alpha;
    private final java.util.List<SlotAnimation> animations = new java.util.ArrayList<>();
    public java.util.List<SlotAnimation> getAnimations() { return animations; }

    public ImageSlot(UUID imageId, String fileName, byte[] imageData) {
        this.imageId = imageId;
        this.fileName = fileName;
        this.imageData = imageData;
        this.posX = 10;
        this.posY = 10;
        this.scale = 1.0f;
        this.rotation = 0f;
        this.alpha = 255;
    }

    // getters
    public UUID getImageId() { return imageId; }
    public String getFileName() { return fileName; }
    public byte[] getImageData() { return imageData; }
    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public float getScale() { return scale; }
    public float getRotation() { return rotation; }
    public int getAlpha() { return alpha; }

    // setters
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public void setPos(int x, int y) { posX = x; posY = y; }
    public void setScale(float scale) { this.scale = scale; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public void setAlpha(int alpha) { this.alpha = alpha; }

    /** 序列化为 NBT（存储和同步用） */
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("ImageId", imageId);
        tag.putString("FileName", fileName);
        tag.putByteArray("ImageData", imageData);
        tag.putInt("PosX", posX);
        tag.putInt("PosY", posY);
        tag.putFloat("Scale", scale);
        tag.putFloat("Rotation", rotation);
        tag.putInt("Alpha", alpha);
        AnimationIO.write(tag, animations);
        return tag;
    }

    /** 从 NBT 反序列化 */
    public static ImageSlot deserialize(CompoundTag tag) {
        UUID imageId = tag.getUUID("ImageId");
        String fileName = tag.getString("FileName");
        byte[] imageData = tag.getByteArray("ImageData");
        ImageSlot slot = new ImageSlot(imageId, fileName, imageData);
        slot.posX = tag.getInt("PosX");
        slot.posY = tag.getInt("PosY");
        slot.scale = tag.getFloat("Scale");
        slot.rotation = tag.getFloat("Rotation");
        slot.alpha = tag.getInt("Alpha");
        AnimationIO.read(tag, slot.animations);
        return slot;
    }
}
