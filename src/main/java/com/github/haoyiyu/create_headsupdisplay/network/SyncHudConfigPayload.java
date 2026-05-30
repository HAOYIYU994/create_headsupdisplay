package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.config.HudPositionConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncHudConfigPayload(HudPositionConfig config) implements CustomPacketPayload {
    public static final Type<SyncHudConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "sync_hud_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHudConfigPayload> CODEC = StreamCodec.composite(
            HudPositionConfig.STREAM_CODEC, SyncHudConfigPayload::config,
            SyncHudConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}