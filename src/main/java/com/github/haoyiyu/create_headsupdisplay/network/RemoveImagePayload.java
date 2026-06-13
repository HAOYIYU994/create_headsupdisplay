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
 * C→S：从终端删除指定的图片槽位。
 */
public record RemoveImagePayload(BlockPos terminalPos, UUID imageId) implements CustomPacketPayload {
    public static final Type<RemoveImagePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_image"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveImagePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.terminalPos);
                buf.writeUUID(payload.imageId);
            },
            buf -> new RemoveImagePayload(buf.readBlockPos(), buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveImagePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = ctx.player().level();
            if (level.getBlockEntity(payload.terminalPos) instanceof DisplayTerminalBlockEntity terminal) {
                terminal.removeImageSlot(payload.imageId);
            } else if (level.getBlockEntity(payload.terminalPos) instanceof DisplayTerminalProBlockEntity terminal) {
                terminal.removeImageSlot(payload.imageId);
            }
        });
    }
}
