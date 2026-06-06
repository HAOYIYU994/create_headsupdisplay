package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务端：将雷达槽位推送到终端 */
public record PushRadarSlotsPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<PushRadarSlotsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "push_radar_slots")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PushRadarSlotsPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeBlockPos(p.corePos),
            buf -> new PushRadarSlotsPayload(buf.readBlockPos())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PushRadarSlotsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().getBlockEntity(payload.corePos) instanceof OmniCoreBlockEntity core) {
                if (core.getBoundTerminal() == null) {
                    ctx.player().displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cNo Display Terminal bound! Use LinkBlock to connect OmniCore -> Terminal first."),
                            false);
                    return;
                }
                core.pushRadarSlotsToTerminal();
                ctx.player().displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.create_headsupdisplay.source_sent"),
                        true);
            }
        });
    }
}
