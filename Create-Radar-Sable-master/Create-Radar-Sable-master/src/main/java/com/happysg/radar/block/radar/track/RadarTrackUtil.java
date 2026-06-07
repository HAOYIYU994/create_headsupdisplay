package com.happysg.radar.block.radar.track;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RadarTrackUtil {

    public static CompoundTag serializeNBTList(Collection<RadarTrack> tracks) {
        ListTag list = new ListTag();
        for (RadarTrack track : tracks) {
            list.add(track.serializeNBT());
        }
        CompoundTag tag = new CompoundTag();
        tag.put("tracks", list);
        return tag;
    }

    public static CompoundTag serializeCompactNBTList(Collection<RadarTrack> tracks) {
        ListTag list = new ListTag();
        for (RadarTrack track : tracks) {
            Vec3 position = track.position();
            Vec3 velocity = track.velocity();
            CompoundTag trackTag = new CompoundTag();
            trackTag.putString("i", track.id());
            trackTag.putDouble("x", position.x);
            trackTag.putDouble("y", position.y);
            trackTag.putDouble("z", position.z);
            trackTag.putDouble("a", velocity.x);
            trackTag.putDouble("b", velocity.y);
            trackTag.putDouble("c", velocity.z);
            trackTag.putLong("t", track.scannedTime());
            trackTag.putInt("k", track.trackCategory().ordinal());
            trackTag.putString("e", track.entityType());
            trackTag.putFloat("h", track.getEnityHeight());
            trackTag.putString("s", track.source());
            trackTag.putBoolean("w", track.weaponTargetable());
            list.add(trackTag);
        }

        CompoundTag tag = new CompoundTag();
        tag.put("t", list);
        return tag;
    }

    public static List<RadarTrack> deserializeListNBT(CompoundTag tag) {
        List<RadarTrack> tracks = new ArrayList<>();
        ListTag list = tag.contains("t", Tag.TAG_LIST)
                ? tag.getList("t", Tag.TAG_COMPOUND)
                : tag.getList("tracks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag trackTag = list.getCompound(i);
            tracks.add(trackTag.contains("i", Tag.TAG_STRING)
                    ? deserializeCompactTrack(trackTag)
                    : RadarTrack.deserializeNBT(trackTag));
        }
        return tracks;
    }

    private static RadarTrack deserializeCompactTrack(CompoundTag tag) {
        return new RadarTrack(tag.getString("i"),
                new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")),
                new Vec3(tag.getDouble("a"), tag.getDouble("b"), tag.getDouble("c")),
                tag.getLong("t"),
                categoryFromOrdinal(tag.getInt("k")),
                tag.getString("e"),
                tag.getFloat("h"),
                tag.contains("s", Tag.TAG_STRING) ? tag.getString("s") : RadarTrack.SOURCE_ENTITY,
                !tag.contains("w", Tag.TAG_BYTE) || tag.getBoolean("w"));
    }

    private static TrackCategory categoryFromOrdinal(int ordinal) {
        TrackCategory[] values = TrackCategory.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TrackCategory.MISC;
        }
        return values[ordinal];
    }

}
