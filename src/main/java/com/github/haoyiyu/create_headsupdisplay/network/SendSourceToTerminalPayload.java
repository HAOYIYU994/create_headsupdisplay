package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record SendSourceToTerminalPayload(BlockPos corePos, int sourceIndex) implements CustomPacketPayload {
    public static final Type<SendSourceToTerminalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "send_source_to_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendSourceToTerminalPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SendSourceToTerminalPayload::corePos,
            ByteBufCodecs.INT, SendSourceToTerminalPayload::sourceIndex,
            SendSourceToTerminalPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendSourceToTerminalPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.sendToTerminal(payload.sourceIndex);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_headsupdisplay.source_sent"), true);
                    core.openConfigScreen(sp);
                }
            }
        });
    }
}