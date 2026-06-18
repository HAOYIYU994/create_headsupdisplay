package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.List;

/** Unified animation read/write for all slot types — one key, one helper. */
public class AnimationIO {
    public static final String KEY = "Animations";

    public static void write(CompoundTag tag, List<SlotAnimation> animations) {
        ListTag list = new ListTag();
        for (SlotAnimation a : animations) list.add(a.serialize());
        tag.put(KEY, list);
    }

    public static void read(CompoundTag tag, List<SlotAnimation> out) {
        if (!tag.contains(KEY)) return;
        ListTag list = tag.getList(KEY, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
            out.add(SlotAnimation.deserialize(list.getCompound(i)));
    }
}
