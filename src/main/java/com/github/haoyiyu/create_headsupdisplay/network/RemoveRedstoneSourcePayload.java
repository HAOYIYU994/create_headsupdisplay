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

/** 按名称删除源，避免 auto-sort 导致的索引错位 */
public record RemoveRedstoneSourcePayload(BlockPos corePos, int index, String sourceName) implements CustomPacketPayload {
    public static final Type<RemoveRedstoneSourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "remove_redstone_source"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveRedstoneSourcePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveRedstoneSourcePayload::corePos,
            ByteBufCodecs.INT, RemoveRedstoneSourcePayload::index,
            ByteBufCodecs.STRING_UTF8, RemoveRedstoneSourcePayload::sourceName,
            RemoveRedstoneSourcePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveRedstoneSourcePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    // 优先按名称查找，避免索引错位
                    int idx = core.findSourceIndexByName(payload.sourceName);
                    if (idx < 0) idx = payload.index; // 名称找不到则退回到索引
                    if (idx >= 0) core.removeRedstoneSource(idx);
                    core.openConfigScreen(sp);
                }
            }
        });
    }
}