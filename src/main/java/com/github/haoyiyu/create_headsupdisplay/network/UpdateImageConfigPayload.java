package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * C→S：更新已有图片的布局参数（位置/缩放/旋转/透明度），不重传图片数据。
 */
public record UpdateImageConfigPayload(BlockPos terminalPos, UUID imageId, int posX, int posY, float scale, float rotation, int alpha) implements CustomPacketPayload {
    public static final Type<UpdateImageConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_image_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImageConfigPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeUUID(payload.imageId);
                buf.writeInt(payload.posX);
                buf.writeInt(payload.posY);
                buf.writeFloat(payload.scale);
                buf.writeFloat(payload.rotation);
                buf.writeInt(payload.alpha);
            },
            buf -> new UpdateImageConfigPayload(
                    buf.readBlockPos(), buf.readUUID(),
                    buf.readInt(), buf.readInt(),
                    buf.readFloat(), buf.readFloat(),
                    buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateImageConfigPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = ctx.player().level();
            if (level.getBlockEntity(payload.terminalPos) instanceof DisplayTerminalBlockEntity terminal) {
                terminal.updateImageConfig(payload.imageId, payload.posX, payload.posY,
                        payload.scale, payload.rotation, payload.alpha);
            }
        });
    }
}
