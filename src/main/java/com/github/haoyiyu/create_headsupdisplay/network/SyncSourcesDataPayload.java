package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.client.ClientOmniCoreData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncSourcesDataPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncSourcesDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "sync_sources_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSourcesDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SyncSourcesDataPayload::data,
            SyncSourcesDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncSourcesDataPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientOmniCoreData.updateSources(payload.data));
    }
}