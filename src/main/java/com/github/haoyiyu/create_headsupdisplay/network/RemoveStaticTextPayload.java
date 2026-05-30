package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveStaticTextPayload(BlockPos terminalPos, int index) implements CustomPacketPayload {
    public static final Type<RemoveStaticTextPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_static_text")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveStaticTextPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeInt(payload.index);
            },
            buf -> new RemoveStaticTextPayload(buf.readBlockPos(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}