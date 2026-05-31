package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.client.ClientFrequencySelectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenFrequencySelectionPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<OpenFrequencySelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "open_frequency_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFrequencySelectionPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, OpenFrequencySelectionPayload::corePos, OpenFrequencySelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void sendTo(net.minecraft.server.level.ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, this);
    }

    public static void handle(OpenFrequencySelectionPayload payload, IPayloadContext ctx) {
        // 客户端：将 corePos 缓存，随后打开的 FrequencySelectionScreen 会读取
        ClientFrequencySelectionCache.setCorePos(payload.corePos());
    }
}