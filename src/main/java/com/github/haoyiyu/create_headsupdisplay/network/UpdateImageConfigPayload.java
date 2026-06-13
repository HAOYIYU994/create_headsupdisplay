package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalProBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * C→S：更新已有图片的布局参数（位置/缩放/旋转/透明度），不重传图片数据。
 */
public record UpdateImageConfigPayload(BlockPos terminalPos, UUID imageId, int slotId, int posX, int posY, float scale, float rotation, int alpha) implements CustomPacketPayload {
    public UpdateImageConfigPayload(BlockPos tp, UUID id, int posX, int posY, float scale, float rotation, int alpha) {
        this(tp, id, 0, posX, posY, scale, rotation, alpha);
    }
    public static final Type<UpdateImageConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_image_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImageConfigPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.terminalPos); buf.writeUUID(p.imageId); buf.writeInt(p.slotId);
                buf.writeInt(p.posX); buf.writeInt(p.posY); buf.writeFloat(p.scale); buf.writeFloat(p.rotation); buf.writeInt(p.alpha); },
            buf -> new UpdateImageConfigPayload(buf.readBlockPos(), buf.readUUID(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(UpdateImageConfigPayload p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = ctx.player().level();
            if (level.getBlockEntity(p.terminalPos) instanceof DisplayTerminalBlockEntity t) t.updateImageConfig(p.imageId, p.posX, p.posY, p.scale, p.rotation, p.alpha);
            else if (level.getBlockEntity(p.terminalPos) instanceof DisplayTerminalProBlockEntity t) t.updateImageConfigById(p.slotId, p.imageId, p.posX, p.posY, p.scale, p.rotation, p.alpha);
        });
    }
}
