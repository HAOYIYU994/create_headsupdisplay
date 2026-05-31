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
    private static boolean registered = false;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        if (registered) {
            return;
        }
        registered = true;

        final PayloadRegistrar registrar = event.registrar("1");

        // 同步显示数据（服务端 -> 客户端）
        registrar.playToClient(
                SyncDisplayDataPayload.TYPE,
                SyncDisplayDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHudData.updateDisplayData(payload.data()))
        );

        // 打开终端配置屏幕（服务端 -> 客户端）
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

        // ========== 万物互联核心网络包 ==========
        // 打开核心配置屏幕（服务端 -> 客户端）
        registrar.playToClient(
                OpenOmniCoreScreenPayload.TYPE,
                OpenOmniCoreScreenPayload.CODEC,
                OpenOmniCoreScreenPayload::handle
        );

        // 添加红石信号源（客户端 -> 服务端）
        registrar.playToServer(
                AddRedstoneSourcePayload.TYPE,
                AddRedstoneSourcePayload.CODEC,
                AddRedstoneSourcePayload::handle
        );

        // 删除红石信号源（客户端 -> 服务端）
        registrar.playToServer(
                RemoveRedstoneSourcePayload.TYPE,
                RemoveRedstoneSourcePayload.CODEC,
                RemoveRedstoneSourcePayload::handle
        );

        // 发送信息源到终端（客户端 -> 服务端）
        registrar.playToServer(
                SendSourceToTerminalPayload.TYPE,
                SendSourceToTerminalPayload.CODEC,
                SendSourceToTerminalPayload::handle
        );

        // 请求核心数据（客户端 -> 服务端）
        registrar.playToServer(
                RequestSourcesDataPayload.TYPE,
                RequestSourcesDataPayload.CODEC,
                RequestSourcesDataPayload::handle
        );

        // 同步核心数据（服务端 -> 客户端）
        registrar.playToClient(
                SyncSourcesDataPayload.TYPE,
                SyncSourcesDataPayload.CODEC,
                SyncSourcesDataPayload::handle
        );

        // 切换自动置顶（客户端 -> 服务端）
        registrar.playToServer(
                ToggleAutoSortPayload.TYPE,
                ToggleAutoSortPayload.CODEC,
                ToggleAutoSortPayload::handle
        );

        // 更新转译配置（客户端 -> 服务端）
        registrar.playToServer(
                UpdateTranslationPayload.TYPE,
                UpdateTranslationPayload.CODEC,
                UpdateTranslationPayload::handle
        );

        // ========== 频率选择容器菜单包 ==========
        // 请求打开频率选择界面（客户端 -> 服务端）
        registrar.playToServer(
                RequestOpenFrequencySelectionPayload.TYPE,
                RequestOpenFrequencySelectionPayload.CODEC,
                RequestOpenFrequencySelectionPayload::handle
        );

        // 打开频率选择界面（服务端 -> 客户端）
        registrar.playToClient(
                OpenFrequencySelectionPayload.TYPE,
                OpenFrequencySelectionPayload.CODEC,
                OpenFrequencySelectionPayload::handle
        );

    }
}