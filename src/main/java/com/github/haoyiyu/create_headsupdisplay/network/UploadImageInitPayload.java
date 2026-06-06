package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.util.ImageUploadTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * C→S：初始化分块图片上传。
 */
public record UploadImageInitPayload(UUID imageId, String fileName, int totalChunks) implements CustomPacketPayload {
    public static final Type<UploadImageInitPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "upload_image_init"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadImageInitPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.imageId);
                buf.writeUtf(payload.fileName);
                buf.writeInt(payload.totalChunks);
            },
            buf -> new UploadImageInitPayload(buf.readUUID(), buf.readUtf(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UploadImageInitPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ImageUploadTracker.cleanStale();
            ImageUploadTracker.init(payload.imageId, payload.totalChunks, payload.fileName);
        });
    }
}
