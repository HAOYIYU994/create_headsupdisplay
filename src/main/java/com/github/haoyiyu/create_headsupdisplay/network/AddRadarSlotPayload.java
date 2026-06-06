package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务端：在 OmniCore 中添加雷达槽位 */
public record AddRadarSlotPayload(BlockPos corePos, int posX, int posY, float scale, float rotation, int alpha, int range) implements CustomPacketPayload {
    public static final Type<AddRadarSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "add_radar_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AddRadarSlotPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.corePos); buf.writeInt(p.posX); buf.writeInt(p.posY); buf.writeFloat(p.scale); buf.writeFloat(p.rotation); buf.writeInt(p.alpha); buf.writeInt(p.range); },
            buf -> new AddRadarSlotPayload(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readInt())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AddRadarSlotPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            if (level.getBlockEntity(payload.corePos) instanceof OmniCoreBlockEntity core) {
                core.addRadarSlot(payload.posX, payload.posY, payload.scale, payload.rotation, payload.alpha, payload.range);
            }
        });
    }
}
