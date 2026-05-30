package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTerminalConfigScreenPayload(CompoundTag slotsData) implements CustomPacketPayload {
    public static final Type<OpenTerminalConfigScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "open_terminal_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalConfigScreenPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.slotsData),
            buf -> new OpenTerminalConfigScreenPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}