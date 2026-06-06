package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncRadarDataPayload(java.util.List<RadarTrackEntry> tracks, float sweepAngle, float radarRange,
                                    double radarX, double radarY, double radarZ) implements CustomPacketPayload {
    public static final Type<SyncRadarDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "sync_radar_data")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRadarDataPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.tracks.size());
                for (var t : payload.tracks) {
                    buf.writeUtf(t.id);
                    buf.writeDouble(t.x); buf.writeDouble(t.y); buf.writeDouble(t.z);
                    buf.writeDouble(t.vx); buf.writeDouble(t.vy); buf.writeDouble(t.vz);
                    buf.writeInt(t.categoryOrdinal);
                    buf.writeUtf(t.entityType);
                }
                buf.writeFloat(payload.sweepAngle);
                buf.writeFloat(payload.radarRange);
                buf.writeDouble(payload.radarX);
                buf.writeDouble(payload.radarY);
                buf.writeDouble(payload.radarZ);
            },
            buf -> {
                int count = buf.readInt();
                var list = new java.util.ArrayList<RadarTrackEntry>(count);
                for (int i = 0; i < count; i++) {
                    list.add(new RadarTrackEntry(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readInt(), buf.readUtf()));
                }
                return new SyncRadarDataPayload(list, buf.readFloat(), buf.readFloat(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record RadarTrackEntry(String id, double x, double y, double z,
                                   double vx, double vy, double vz,
                                   int categoryOrdinal, String entityType) {}
}
