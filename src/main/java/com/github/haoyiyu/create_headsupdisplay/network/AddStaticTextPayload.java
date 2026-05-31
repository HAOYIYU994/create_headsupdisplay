package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AddStaticTextPayload(BlockPos terminalPos, String text, int posX, int posY, float scale, float rotation, int color, int alpha) implements CustomPacketPayload {
    public static final Type<AddStaticTextPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "add_redstone_slot_to_core")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AddStaticTextPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeUtf(payload.text);
                buf.writeInt(payload.posX);
                buf.writeInt(payload.posY);
                buf.writeFloat(payload.scale);
                buf.writeFloat(payload.rotation);
                buf.writeInt(payload.color);
                buf.writeInt(payload.alpha);
            },
            buf -> new AddStaticTextPayload(
                    buf.readBlockPos(), buf.readUtf(),
                    buf.readInt(), buf.readInt(),
                    buf.readFloat(), buf.readFloat(),
                    buf.readInt(), buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}