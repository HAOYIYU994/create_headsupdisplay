package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务端：给终端命名 */
public record SetTerminalNamePayload(BlockPos corePos, int terminalIndex, String name) implements CustomPacketPayload {
    public static final Type<SetTerminalNamePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "set_terminal_name")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetTerminalNamePayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.corePos); buf.writeInt(p.terminalIndex); buf.writeUtf(p.name); },
            buf -> new SetTerminalNamePayload(buf.readBlockPos(), buf.readInt(), buf.readUtf())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetTerminalNamePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var be = ctx.player().level().getBlockEntity(payload.corePos);
            if (be instanceof OmniCoreBlockEntity core && payload.terminalIndex >= 0
                    && payload.terminalIndex < core.getBoundTerminals().size()) {
                core.setTerminalName(core.getBoundTerminals().get(payload.terminalIndex), payload.name);
            }
        });
    }
}
