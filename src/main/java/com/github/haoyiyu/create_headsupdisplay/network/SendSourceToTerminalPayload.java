package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 客户端→服务端：将指定源发送到指定终端（-1 = 所有终端） */
public record SendSourceToTerminalPayload(BlockPos corePos, int sourceIndex, int terminalIndex) implements CustomPacketPayload {
    public static final Type<SendSourceToTerminalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "send_source_to_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendSourceToTerminalPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.corePos); buf.writeInt(p.sourceIndex); buf.writeInt(p.terminalIndex); },
            buf -> new SendSourceToTerminalPayload(buf.readBlockPos(), buf.readInt(), buf.readInt())
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
                    core.sendToTerminal(payload.sourceIndex, payload.terminalIndex);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_headsupdisplay.source_sent"), true);
                    core.openConfigScreen(sp);
                }
            }
        });
    }
}
