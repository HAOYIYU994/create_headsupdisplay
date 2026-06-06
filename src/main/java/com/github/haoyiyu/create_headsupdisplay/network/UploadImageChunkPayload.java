package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.util.ImageUploadTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * C→S：传输图片的一个数据块（≤30KB）。
 */
public record UploadImageChunkPayload(UUID imageId, int chunkIndex, byte[] data) implements CustomPacketPayload {
    public static final Type<UploadImageChunkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "upload_image_chunk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadImageChunkPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.imageId);
                buf.writeInt(payload.chunkIndex);
                buf.writeByteArray(payload.data);
            },
            buf -> new UploadImageChunkPayload(buf.readUUID(), buf.readInt(), buf.readByteArray())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UploadImageChunkPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ImageUploadTracker.storeChunk(payload.imageId, payload.chunkIndex, payload.data);
        });
    }
}
