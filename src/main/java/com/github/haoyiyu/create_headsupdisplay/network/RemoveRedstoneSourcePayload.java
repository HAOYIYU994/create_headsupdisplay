package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record RemoveRedstoneSourcePayload(BlockPos corePos, int index) implements CustomPacketPayload {
    public static final Type<RemoveRedstoneSourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_redstone_source"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveRedstoneSourcePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveRedstoneSourcePayload::corePos,
            ByteBufCodecs.INT, RemoveRedstoneSourcePayload::index,
            RemoveRedstoneSourcePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveRedstoneSourcePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.removeRedstoneSource(payload.index);
                    core.openConfigScreen(sp);
                }
            }
        });
    }
}