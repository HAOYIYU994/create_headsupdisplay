package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 坞 X 专用：删除槽位和数据源缓存 */
public record RemoveSlotSourcePayload(BlockPos terminalPos, BlockPos sourcePos) implements CustomPacketPayload {
    public static final Type<RemoveSlotSourcePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_slot_source")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveSlotSourcePayload> CODEC = StreamCodec.of(
            (buf, payload) -> { buf.writeBlockPos(payload.terminalPos); buf.writeBlockPos(payload.sourcePos); },
            buf -> new RemoveSlotSourcePayload(buf.readBlockPos(), buf.readBlockPos())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}