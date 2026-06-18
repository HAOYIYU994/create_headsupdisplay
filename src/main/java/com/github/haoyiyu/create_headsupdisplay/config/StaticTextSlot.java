package com.github.haoyiyu.create_headsupdisplay.config;

import java.util.ArrayList;
import java.util.List;

public class StaticTextSlot {
    private String text;
    private int posX, posY;
    private float scale;
    private float rotation;
    private int color = 0xFFFFFF;
    private int alpha = 255;
    private final List<SlotAnimation> animations = new ArrayList<>();

    public StaticTextSlot(String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
        this.text = text;
        this.posX = posX;
        this.posY = posY;
        this.scale = scale;
        this.rotation = rotation;
        this.color = color;
        this.alpha = alpha;
    }

    // getters and setters...
    public String getText() { return text; }
    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public float getScale() { return scale; }
    public float getRotation() { return rotation; }
    public int getColor() { return color; }
    public int getAlpha() { return alpha; }
    public List<SlotAnimation> getAnimations() { return animations; }

    public void setText(String text) { this.text = text; }
    public void setPos(int x, int y) { posX = x; posY = y; }
    public void setScale(float scale) { this.scale = scale; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public void setColor(int color) { this.color = color; }
    public void setAlpha(int alpha) { this.alpha = alpha; }
}
