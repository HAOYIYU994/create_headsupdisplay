package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.github.haoyiyu.create_headsupdisplay.screen.TerminalConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // 同步显示数据（服务端 -> 客户端）
        registrar.playToClient(
                SyncDisplayDataPayload.TYPE,
                SyncDisplayDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHudData.updateDisplayData(payload.data()))
        );

        // 打开配置屏幕（服务端 -> 客户端）
        registrar.playToClient(
                OpenTerminalConfigScreenPayload.TYPE,
                OpenTerminalConfigScreenPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Minecraft.getInstance().setScreen(new TerminalConfigScreen(payload.slotsData()));
                })
        );

        // 更新槽位配置（客户端 -> 服务端）
        registrar.playToServer(
                UpdateSlotPayload.TYPE,
                UpdateSlotPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalBlockEntity terminal) {
                        terminal.updateSlotConfig(payload.sourcePos(), payload.posX(), payload.posY(), payload.scale(), payload.rotation(), payload.color(), payload.alpha());
                    }
                })
        );

        registrar.playToServer(
                UpdateStaticTextPayload.TYPE,
                UpdateStaticTextPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalBlockEntity terminal) {
                        terminal.handleStaticTextUpdate(payload.index(), payload.text(), payload.posX(), payload.posY(), payload.scale(), payload.rotation(), payload.color(), payload.alpha());
                    }
                })
        );

        registrar.playToServer(
                AddStaticTextPayload.TYPE,
                AddStaticTextPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalBlockEntity terminal) {
                        terminal.addStaticTextSlot(payload.text(), payload.posX(), payload.posY(), payload.scale(), payload.rotation(), payload.color(), payload.alpha());
                    }
                })
        );

        registrar.playToServer(
                RemoveStaticTextPayload.TYPE,
                RemoveStaticTextPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalBlockEntity terminal) {
                        terminal.removeStaticText(payload.index());
                    }
                })
        );

        // 删除槽位（客户端 -> 服务端）
        registrar.playToServer(
                RemoveSlotPayload.TYPE,
                RemoveSlotPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalBlockEntity terminal) {
                        terminal.removeSlot(payload.sourcePos());
                    }
                })
        );
    }
}