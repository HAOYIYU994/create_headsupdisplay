package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleAutoSortPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<ToggleAutoSortPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "toggle_auto_sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleAutoSortPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleAutoSortPayload::corePos,
            ToggleAutoSortPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleAutoSortPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.toggleAutoSort();
                }
            }
        });
    }
}