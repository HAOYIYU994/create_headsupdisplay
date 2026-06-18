package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/** 槽位动画：关键帧 + 触发条件 + 结束行为 */
public class SlotAnimation {
    public enum TriggerType { ALWAYS, DATA_ABOVE, DATA_BELOW, DATA_BETWEEN }
    public enum EndBehavior { STOP, FINISH, HOLD, REVERSE }

    public TriggerType trigger = TriggerType.ALWAYS;
    public float triggerValue1, triggerValue2;
    public String triggerSourceName = "";
    public float cycleTime = 2f;
    public boolean loop = true;
    public EndBehavior endBehavior = EndBehavior.STOP;
    public final List<Keyframe> keyframes = new ArrayList<>();
    // Runtime state (not serialized)
    boolean wasActive;
    long deactivateStart;
    float deactivateProgress;
    public boolean getWasActive() { return wasActive; }
    public void setWasActive(boolean v) { wasActive = v; }
    public long getDeactivateStart() { return deactivateStart; }
    public void setDeactivateStart(long v) { deactivateStart = v; }
    public float getDeactivateProgress() { return deactivateProgress; }
    public void setDeactivateProgress(float v) { deactivateProgress = v; }

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
        t.putString("src", triggerSourceName);
        t.putFloat("cycle", cycleTime); t.putBoolean("loop", loop);
        t.putString("end", endBehavior.name());
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
        a.triggerSourceName = t.getString("src");
        a.cycleTime = t.getFloat("cycle"); a.loop = t.getBoolean("loop");
        if (t.contains("end")) try { a.endBehavior = EndBehavior.valueOf(t.getString("end")); } catch (Exception ignored) {}
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

    /** 根据指定源或自身数据判断是否触发。自动从文本中提取数值。 */
    public boolean isActive(String sourceValue) {
        if (trigger == TriggerType.ALWAYS) return true;
        if (sourceValue == null || sourceValue.isEmpty()) return false;
        float v = extractNumber(sourceValue);
        if (Float.isNaN(v)) return false;
        return switch (trigger) {
            case DATA_ABOVE -> v > triggerValue1;
            case DATA_BELOW -> v < triggerValue1;
            case DATA_BETWEEN -> v >= triggerValue1 && v <= triggerValue2;
            default -> true;
        };
    }

    /** Pull the first number from a string like "15 km/h" → 15. Returns NaN on failure. */
    public static float extractNumber(String raw) {
        String s = raw.replaceAll("§[0-9a-fk-or]", "").trim();
        // Try direct parse first
        try { return Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        // Find first numeric segment
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[-+]?\\d*\\.?\\d+").matcher(s);
        if (m.find()) {
            try { return Float.parseFloat(m.group()); } catch (NumberFormatException ignored) {}
        }
        return Float.NaN;
    }
}
