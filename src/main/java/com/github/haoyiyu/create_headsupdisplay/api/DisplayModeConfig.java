package com.github.haoyiyu.create_headsupdisplay.api;
import net.minecraft.nbt.CompoundTag;
import java.util.LinkedHashMap;
import java.util.Map;

public class DisplayModeConfig {
    private final Map<String, Object> values = new LinkedHashMap<>();
    public DisplayModeConfig() {}
    public DisplayModeConfig(float max, float min, String unit) { setMax(max); setMin(min); setUnit(unit); }
    public float getFloat(String key, float def) { Object v = values.get(key); if (v instanceof Number n) return n.floatValue(); return def; }
    public int getInt(String key, int def) { Object v = values.get(key); if (v instanceof Number n) return n.intValue(); return def; }
    public String getString(String key, String def) { Object v = values.get(key); return v != null ? v.toString() : def; }
    public boolean getBoolean(String key, boolean def) { Object v = values.get(key); if (v instanceof Boolean b) return b; return def; }
    public void setFloat(String k, float v) { values.put(k, v); }
    public void setInt(String k, int v) { values.put(k, v); }
    public void setString(String k, String v) { values.put(k, v); }
    public void setBoolean(String k, boolean v) { values.put(k, v); }
    public float getMax() { return getFloat("max", 100f); }
    public float getMin() { return getFloat("min", 0f); }
    public String getUnit() { return getString("unit", ""); }
    public void setMax(float v) { setFloat("max", v); }
    public void setMin(float v) { setFloat("min", v); }
    public void setUnit(String u) { setString("unit", u); }
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        for (var e : values.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Float f) tag.putFloat(e.getKey(), f);
            else if (v instanceof Double d) tag.putFloat(e.getKey(), d.floatValue());
            else if (v instanceof Integer i) tag.putInt(e.getKey(), i);
            else if (v instanceof Boolean b) tag.putBoolean(e.getKey(), b);
            else if (v != null) tag.putString(e.getKey(), v.toString());
        }
        return tag;
    }
    public static DisplayModeConfig deserialize(CompoundTag tag) {
        DisplayModeConfig cfg = new DisplayModeConfig();
        for (String k : tag.getAllKeys()) {
            byte t = tag.getTagType(k);
            if (t == CompoundTag.TAG_FLOAT) cfg.setFloat(k, tag.getFloat(k));
            else if (t == CompoundTag.TAG_INT) cfg.setInt(k, tag.getInt(k));
            else if (t == CompoundTag.TAG_STRING) cfg.setString(k, tag.getString(k));
            else if (t == CompoundTag.TAG_BYTE) cfg.setBoolean(k, tag.getBoolean(k));
        }
        return cfg;
    }
}
