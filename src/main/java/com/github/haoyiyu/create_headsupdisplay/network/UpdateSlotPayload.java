package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateSlotPayload(BlockPos terminalPos, BlockPos sourcePos, int posX, int posY, float scale, float rotation, int color, int alpha) implements CustomPacketPayload {
    public static final Type<UpdateSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSlotPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeBlockPos(payload.sourcePos);
                buf.writeInt(payload.posX);
                buf.writeInt(payload.posY);
                buf.writeFloat(payload.scale);
                buf.writeFloat(payload.rotation);
                buf.writeInt(payload.color);
                buf.writeInt(payload.alpha);
            },
            buf -> new UpdateSlotPayload(
                    buf.readBlockPos(), buf.readBlockPos(),
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