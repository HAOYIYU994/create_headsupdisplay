package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateTranslationPayload(BlockPos corePos, int sourceIndex, CompoundTag transData) implements CustomPacketPayload {
    public static final Type<UpdateTranslationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_translation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTranslationPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpdateTranslationPayload::corePos,
            ByteBufCodecs.INT, UpdateTranslationPayload::sourceIndex,
            ByteBufCodecs.COMPOUND_TAG, UpdateTranslationPayload::transData,
            UpdateTranslationPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UpdateTranslationPayload p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var be = ctx.player().level().getBlockEntity(p.corePos);
            if (be instanceof OmniCoreBlockEntity core) {
                core.setTranslation(p.sourceIndex, TranslationConfig.deserialize(p.transData));
            }
        });
    }
}