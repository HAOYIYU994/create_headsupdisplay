package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.screen.NbtReaderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenNbtReaderScreenPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<OpenNbtReaderScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "open_nbt_reader_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNbtReaderScreenPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, OpenNbtReaderScreenPayload::data, OpenNbtReaderScreenPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(OpenNbtReaderScreenPayload p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new NbtReaderScreen(p.data())));
    }
}
