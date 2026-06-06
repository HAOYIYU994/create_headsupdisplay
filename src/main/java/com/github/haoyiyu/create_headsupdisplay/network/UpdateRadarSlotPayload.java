package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务端：更新雷达槽位配置（位置、缩放、旋转等） */
public record UpdateRadarSlotPayload(BlockPos corePos, int index, int posX, int posY, float scale, float rotation, int alpha, int range) implements CustomPacketPayload {
    public static final Type<UpdateRadarSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_radar_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRadarSlotPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.corePos); buf.writeInt(p.index);
                buf.writeInt(p.posX); buf.writeInt(p.posY); buf.writeFloat(p.scale);
                buf.writeFloat(p.rotation); buf.writeInt(p.alpha); buf.writeInt(p.range);
            },
            buf -> new UpdateRadarSlotPayload(buf.readBlockPos(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readInt())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UpdateRadarSlotPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            var be = level.getBlockEntity(payload.corePos);
            if (be instanceof OmniCoreBlockEntity core) {
                core.updateRadarSlot(payload.index, payload.posX, payload.posY, payload.scale, payload.rotation, payload.alpha, payload.range);
            } else if (be instanceof DisplayTerminalBlockEntity terminal) {
                if (payload.index >= 0 && payload.index < terminal.getRadarSlots().size()) {
                    var slot = terminal.getRadarSlots().get(payload.index);
                    slot.setPos(payload.posX, payload.posY);
                    slot.setScale(payload.scale);
                    slot.setRotation(payload.rotation);
                    slot.setAlpha(payload.alpha);
                    slot.setRadarRange(payload.range);
                    terminal.setChanged();
                    terminal.syncToBoundPlayers();
                }
            }
        });
    }
}
