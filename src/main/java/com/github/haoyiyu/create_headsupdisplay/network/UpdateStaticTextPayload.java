package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateStaticTextPayload(BlockPos terminalPos, int index, String text, int posX, int posY, float scale, float rotation, int color, int alpha) implements CustomPacketPayload {
    public static final Type<UpdateStaticTextPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_static_text")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateStaticTextPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeInt(payload.index);
                buf.writeUtf(payload.text);
                buf.writeInt(payload.posX);
                buf.writeInt(payload.posY);
                buf.writeFloat(payload.scale);
                buf.writeFloat(payload.rotation);
                buf.writeInt(payload.color);
                buf.writeInt(payload.alpha);
            },
            buf -> new UpdateStaticTextPayload(
                    buf.readBlockPos(), buf.readInt(), buf.readUtf(),
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