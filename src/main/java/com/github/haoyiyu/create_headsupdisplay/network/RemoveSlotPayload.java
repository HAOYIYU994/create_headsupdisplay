package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveSlotPayload(BlockPos terminalPos, BlockPos sourcePos) implements CustomPacketPayload {
    public static final Type<RemoveSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveSlotPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeBlockPos(payload.sourcePos);
            },
            buf -> new RemoveSlotPayload(buf.readBlockPos(), buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}