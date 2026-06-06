package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务端：从 OmniCore 删除雷达槽位 */
public record RemoveRadarSlotPayload(BlockPos corePos, int index) implements CustomPacketPayload {
    public static final Type<RemoveRadarSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_radar_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveRadarSlotPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.corePos); buf.writeInt(p.index); },
            buf -> new RemoveRadarSlotPayload(buf.readBlockPos(), buf.readInt())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RemoveRadarSlotPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            var be = level.getBlockEntity(payload.corePos);
            if (be instanceof OmniCoreBlockEntity core) {
                core.removeRadarSlot(payload.index);
            } else if (be instanceof DisplayTerminalBlockEntity terminal) {
                if (payload.index >= 0 && payload.index < terminal.getRadarSlots().size()) {
                    terminal.getRadarSlots().remove(payload.index);
                    terminal.setChanged();
                    terminal.syncToBoundPlayers();
                }
            }
        });
    }
}
