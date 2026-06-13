package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.config.SlotAnimation;
import net.minecraft.util.Mth;

import java.util.List;

/** 动画求值：根据时间和数据返回当前帧的插值属性 */
public class AnimationEvaluator {

    public static class Result {
        public Float posX, posY, scale, rotation;
        public Integer color, alpha;

        public void applyTo(SlotRef s) {
            if (posX != null) s.setPosX(posX);
            if (posY != null) s.setPosY(posY);
            if (scale != null) s.setScale(scale);
            if (rotation != null) s.setRotation(rotation);
            if (color != null) s.setColor(color);
            if (alpha != null) s.setAlpha(alpha);
        }
    }

    public interface SlotRef {
        float getPosX(); void setPosX(float v);
        float getPosY(); void setPosY(float v);
        float getScale(); void setScale(float v);
        float getRotation(); void setRotation(float v);
        int getColor(); void setColor(int v);
        int getAlpha(); void setAlpha(int v);
    }

    /** 对槽位的所有动画求值，返回合并后的结果 */
    public static void evaluate(List<SlotAnimation> animations, String dataText, Result out, SlotRef base) {
        out.posX = null; out.posY = null; out.scale = null; out.rotation = null; out.color = null; out.alpha = null;
        if (animations.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (SlotAnimation anim : animations) {
            if (!anim.isActive(dataText)) continue;
            if (anim.keyframes.size() < 2) continue;

            float t = ((now % (long)(anim.cycleTime * 1000f)) / (anim.cycleTime * 1000f));
            if (!anim.loop) t = Mth.clamp(t, 0f, 1f);

            // 找包围 t 的两个关键帧
            SlotAnimation.Keyframe k0 = null, k1 = null;
            for (SlotAnimation.Keyframe k : anim.keyframes) {
                if (k.time <= t && (k0 == null || k.time >= k0.time)) k0 = k;
                if (k.time >= t && (k1 == null || k.time <= k1.time)) k1 = k;
            }
            if (k0 == null) k0 = anim.keyframes.get(0);
            if (k1 == null) k1 = anim.keyframes.get(anim.keyframes.size() - 1);

            float blend = (k1.time == k0.time) ? 0f : Mth.clamp((t - k0.time) / (k1.time - k0.time), 0f, 1f);

            // 插值各属性
            interpolate(out, "posX", base.getPosX(), k0, k1, blend);
            interpolate(out, "posY", base.getPosY(), k0, k1, blend);
            interpolate(out, "scale", base.getScale(), k0, k1, blend);
            interpolate(out, "rotation", base.getRotation(), k0, k1, blend);
            interpolateInt(out, "color", base.getColor(), k0, k1, blend);
            interpolateInt(out, "alpha", base.getAlpha(), k0, k1, blend);
        }
    }

    private static void interpolate(Result out, String prop, float base, SlotAnimation.Keyframe k0, SlotAnimation.Keyframe k1, float t) {
        Float v0 = getFloat(k0, prop), v1 = getFloat(k1, prop);
        float val;
        if (v0 != null && v1 != null) val = Mth.lerp(t, v0, v1);
        else if (v0 != null) val = v0;
        else if (v1 != null) val = v1;
        else return;
        setFloat(out, prop, val);
    }

    private static void interpolateInt(Result out, String prop, int base, SlotAnimation.Keyframe k0, SlotAnimation.Keyframe k1, float t) {
        Integer v0 = getInt(k0, prop), v1 = getInt(k1, prop);
        int val;
        if (v0 != null && v1 != null) val = lerpColor(v0, v1, t);
        else if (v0 != null) val = v0;
        else if (v1 != null) val = v1;
        else return;
        setInt(out, prop, val);
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a>>16)&0xFF, ag = (a>>8)&0xFF, ab = a&0xFF, aa = (a>>24)&0xFF;
        int br = (b>>16)&0xFF, bg = (b>>8)&0xFF, bb = b&0xFF, ba = (b>>24)&0xFF;
        int r = (int)(ar + (br-ar)*t), g = (int)(ag + (bg-ag)*t);
        int bl = (int)(ab + (bb-ab)*t), al = (int)(aa + (ba-aa)*t);
        return (al<<24)|(r<<16)|(g<<8)|bl;
    }

    private static Float getFloat(SlotAnimation.Keyframe k, String prop) {
        return switch (prop) {
            case "posX" -> k.posX; case "posY" -> k.posY;
            case "scale" -> k.scale; case "rotation" -> k.rotation;
            default -> null;
        };
    }

    private static Integer getInt(SlotAnimation.Keyframe k, String prop) {
        return switch (prop) {
            case "color" -> k.color; case "alpha" -> k.alpha;
            default -> null;
        };
    }

    private static void setFloat(Result out, String prop, float v) {
        switch (prop) {
            case "posX" -> out.posX = v; case "posY" -> out.posY = v;
            case "scale" -> out.scale = v; case "rotation" -> out.rotation = v;
        }
    }

    private static void setInt(Result out, String prop, int v) {
        switch (prop) { case "color" -> out.color = v; case "alpha" -> out.alpha = v; }
    }
}
