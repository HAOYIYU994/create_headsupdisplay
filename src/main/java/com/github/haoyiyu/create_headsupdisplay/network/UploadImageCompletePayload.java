package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.util.ImageUploadTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * C→S：所有分块已发送完毕，服务端组装并持久化图片到 OmniCore。
 */
public record UploadImageCompletePayload(BlockPos corePos, UUID imageId) implements CustomPacketPayload {
    public static final Type<UploadImageCompletePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "upload_image_complete"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadImageCompletePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.corePos);
                buf.writeUUID(payload.imageId);
            },
            buf -> new UploadImageCompletePayload(buf.readBlockPos(), buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UploadImageCompletePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            byte[] assembled = ImageUploadTracker.assemble(payload.imageId);
            String fileName = ImageUploadTracker.getFileName(payload.imageId);
            ImageUploadTracker.remove(payload.imageId);

            if (assembled != null && ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.addImageSource(payload.imageId, fileName, assembled);
                    // 刷新 GUI
                    core.openConfigScreen(sp);
                }
            }
        });
    }
}
