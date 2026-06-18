package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

public record SyncDisplayDataPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncDisplayDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "sync_display_data")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDisplayDataPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.data),
            buf -> new SyncDisplayDataPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDisplayDataPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.github.haoyiyu.create_headsupdisplay.client.ClientHudData.updateDisplayData(payload.data()));
    }
}