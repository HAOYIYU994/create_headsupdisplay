package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.menu.FrequencySelectionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;

public record RequestOpenFrequencySelectionPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<RequestOpenFrequencySelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "request_open_frequency_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenFrequencySelectionPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RequestOpenFrequencySelectionPayload::corePos, RequestOpenFrequencySelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenFrequencySelectionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                // 发送 OpenFrequencySelectionPayload 告知客户端 corePos，然后打开菜单
                new OpenFrequencySelectionPayload(payload.corePos()).sendTo(sp);
                // 打开容器菜单（屏幕会由 MenuScreens 注册自动创建）
                sp.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new FrequencySelectionMenu(id, inv),
                        Component.literal("Frequency Selection")));
            }
        });
    }
}