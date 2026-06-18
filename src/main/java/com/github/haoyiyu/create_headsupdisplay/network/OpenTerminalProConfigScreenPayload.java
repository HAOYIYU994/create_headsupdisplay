package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.client.ClientPayloadHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTerminalProConfigScreenPayload(CompoundTag slotsData) implements CustomPacketPayload {
    public static final Type<OpenTerminalProConfigScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "open_terminal_pro_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalProConfigScreenPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.slotsData),
            buf -> new OpenTerminalProConfigScreenPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTerminalProConfigScreenPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPayloadHandlers.openTerminalProConfigScreen(payload.slotsData()));
    }
}
