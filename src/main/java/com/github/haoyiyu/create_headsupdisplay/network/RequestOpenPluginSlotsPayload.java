package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.menu.PluginSlotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public record RequestOpenPluginSlotsPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<RequestOpenPluginSlotsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "request_open_plugin_slots"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenPluginSlotsPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestOpenPluginSlotsPayload::corePos, RequestOpenPluginSlotsPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestOpenPluginSlotsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos());
                if (be instanceof OmniCoreBlockEntity core) {
                    sp.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new PluginSlotMenu(id, inv, core),
                            Component.literal("Plugins")
                    ));
                }
            }
        });
    }
}
