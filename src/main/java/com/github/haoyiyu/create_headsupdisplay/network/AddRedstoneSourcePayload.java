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
import net.minecraft.world.item.ItemStack;

public record AddRedstoneSourcePayload(BlockPos corePos, String name, ItemStack frequencyItem1, ItemStack frequencyItem2) implements CustomPacketPayload {
    public static final Type<AddRedstoneSourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "add_redstone_source"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddRedstoneSourcePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AddRedstoneSourcePayload::corePos,
            ByteBufCodecs.STRING_UTF8, AddRedstoneSourcePayload::name,
            ItemStack.OPTIONAL_STREAM_CODEC, AddRedstoneSourcePayload::frequencyItem1,
            ItemStack.OPTIONAL_STREAM_CODEC, AddRedstoneSourcePayload::frequencyItem2,
            AddRedstoneSourcePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AddRedstoneSourcePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    core.addRedstoneSource(payload.name, payload.frequencyItem1, payload.frequencyItem2);
                    // 刷新 GUI
                    core.openConfigScreen(sp);
                }
            }
        });
    }
}