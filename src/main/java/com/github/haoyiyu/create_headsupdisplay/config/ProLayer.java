package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/** 图层数据：一组槽位的容器，支持可见性和锁定 */
public class ProLayer {
    private String name;
    private boolean visible = true;
    private boolean locked = false;
    private boolean frozen = false;

    // 该图层上的数据源槽位（sourcePos → DisplaySlot）
    private final List<DisplaySlot> slots = new ArrayList<>();

    public ProLayer(String name) {
        this.name = name;
    }

    // ---- getters ----
    public String getName() { return name; }
    public boolean isVisible() { return visible; }
    public boolean isLocked() { return locked; }
    public List<DisplaySlot> getSlots() { return slots; }

    // ---- setters ----
    public void setName(String n) { this.name = n; }
    public void setVisible(boolean v) { this.visible = v; }
    public void setLocked(boolean l) { this.locked = l; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean f) { this.frozen = f; }

    // ---- 槽位快捷方法 ----
    public void addSlot(DisplaySlot slot) { slots.add(slot); }
    public void removeSlot(DisplaySlot slot) { slots.remove(slot); }
    public boolean hasSlot(DisplaySlot slot) { return slots.contains(slot); }

    // ---- 序列化 ----
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putBoolean("Visible", visible);
        tag.putBoolean("Locked", locked);
        tag.putBoolean("Frozen", frozen);
        ListTag slotList = new ListTag();
        for (DisplaySlot s : slots) {
            slotList.add(s.serialize());
        }
        tag.put("Slots", slotList);
        return tag;
    }

    public static ProLayer deserialize(CompoundTag tag) {
        ProLayer layer = new ProLayer(tag.getString("Name"));
        layer.visible = tag.getBoolean("Visible");
        layer.locked = tag.getBoolean("Locked");
        layer.frozen = tag.getBoolean("Frozen");
        ListTag slotList = tag.getList("Slots", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < slotList.size(); i++) {
            layer.slots.add(DisplaySlot.deserialize(slotList.getCompound(i)));
        }
        return layer;
    }
}
