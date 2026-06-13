package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/** 槽位动画：关键帧 + 触发条件 */
public class SlotAnimation {
    public enum TriggerType { ALWAYS, DATA_ABOVE, DATA_BELOW, DATA_BETWEEN }

    public TriggerType trigger = TriggerType.ALWAYS;
    public float triggerValue1, triggerValue2; // DATA_ABOVE:阈值 DATA_BETWEEN:上下限
    public float cycleTime = 2f; // 周期秒数
    public boolean loop = true;
    public final List<Keyframe> keyframes = new ArrayList<>();

    public static class Keyframe {
        public float time; // 0~1 周期内比例
        public Float posX, posY, scale, rotation;
        public Integer color, alpha;
    }

    public SlotAnimation() { keyframes.add(new Keyframe()); keyframes.get(0).time = 0f; }

    public CompoundTag serialize() {
        CompoundTag t = new CompoundTag();
        t.putString("trigger", trigger.name());
        t.putFloat("tv1", triggerValue1); t.putFloat("tv2", triggerValue2);
        t.putFloat("cycle", cycleTime); t.putBoolean("loop", loop);
        ListTag kf = new ListTag();
        for (Keyframe k : keyframes) {
            CompoundTag kt = new CompoundTag();
            kt.putFloat("t", k.time);
            if (k.posX != null) kt.putFloat("px", k.posX);
            if (k.posY != null) kt.putFloat("py", k.posY);
            if (k.scale != null) kt.putFloat("sc", k.scale);
            if (k.rotation != null) kt.putFloat("rt", k.rotation);
            if (k.color != null) kt.putInt("cl", k.color);
            if (k.alpha != null) kt.putInt("al", k.alpha);
            kf.add(kt);
        }
        t.put("kf", kf);
        return t;
    }

    public static SlotAnimation deserialize(CompoundTag t) {
        SlotAnimation a = new SlotAnimation();
        a.keyframes.clear();
        a.trigger = TriggerType.valueOf(t.getString("trigger"));
        a.triggerValue1 = t.getFloat("tv1"); a.triggerValue2 = t.getFloat("tv2");
        a.cycleTime = t.getFloat("cycle"); a.loop = t.getBoolean("loop");
        ListTag kf = t.getList("kf", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < kf.size(); i++) {
            CompoundTag kt = kf.getCompound(i);
            Keyframe k = new Keyframe();
            k.time = kt.getFloat("t");
            if (kt.contains("px")) k.posX = kt.getFloat("px");
            if (kt.contains("py")) k.posY = kt.getFloat("py");
            if (kt.contains("sc")) k.scale = kt.getFloat("sc");
            if (kt.contains("rt")) k.rotation = kt.getFloat("rt");
            if (kt.contains("cl")) k.color = kt.getInt("cl");
            if (kt.contains("al")) k.alpha = kt.getInt("al");
            a.keyframes.add(k);
        }
        return a;
    }

    /** 根据当前数据值判断是否触发 */
    public boolean isActive(String dataText) {
        if (trigger == TriggerType.ALWAYS) return true;
        try {
            float v = Float.parseFloat(dataText.replaceAll("§[0-9a-fk-or]", "").split(" ")[0].trim());
            return switch (trigger) {
                case DATA_ABOVE -> v > triggerValue1;
                case DATA_BELOW -> v < triggerValue1;
                case DATA_BETWEEN -> v >= triggerValue1 && v <= triggerValue2;
                default -> true;
            };
        } catch (NumberFormatException e) { return false; }
    }
}
