package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalProBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    private static boolean registered = false;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        if (registered) return;
        registered = true;

        final PayloadRegistrar registrar = event.registrar("1");

        // 更新槽位配置（客户端 -> 服务端）
        registrar.playToServer(
                UpdateSlotPayload.TYPE,
                UpdateSlotPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalBlockEntity terminal) {
                        terminal.updateSlotConfig(payload.sourcePos(), payload.posX(), payload.posY(), payload.scale(), payload.rotation(), payload.color(), payload.alpha());
                    } else if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalProBlockEntity terminal) {
                        terminal.updateSlotConfigById(payload.slotId(), payload.sourcePos(), payload.posX(), payload.posY(), payload.scale(), payload.rotation(), payload.color(), payload.alpha());
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
                    } else if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalProBlockEntity terminal) {
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
                    } else if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalProBlockEntity terminal) {
                        terminal.addStaticTextSlot(payload.text(), payload.posX(), payload.posY(), payload.scale(), payload.rotation(), payload.color(), payload.alpha(), 0);
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
                    } else if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalProBlockEntity terminal) {
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
                    } else if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalProBlockEntity terminal) {
                        terminal.removeSlot(payload.sourcePos());
                    }
                })
        );

        // 坞 X：删槽位+缓存
        registrar.playToServer(
                RemoveSlotSourcePayload.TYPE,
                RemoveSlotSourcePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var level = context.player().level();
                    if (level.getBlockEntity(payload.terminalPos()) instanceof DisplayTerminalProBlockEntity terminal) {
                        terminal.removeSourceCard(payload.sourcePos());
                    }
                })
        );

        // ========== 数据集成核心网络包 ==========
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

        // ========== 图片系统网络包 ==========
        // 初始化分块图片上传（客户端 -> 服务端）
        registrar.playToServer(
                UploadImageInitPayload.TYPE,
                UploadImageInitPayload.CODEC,
                UploadImageInitPayload::handle
        );

        // 图片数据块（客户端 -> 服务端）
        registrar.playToServer(
                UploadImageChunkPayload.TYPE,
                UploadImageChunkPayload.CODEC,
                UploadImageChunkPayload::handle
        );

        // 图片上传完成（客户端 -> 服务端）
        registrar.playToServer(
                UploadImageCompletePayload.TYPE,
                UploadImageCompletePayload.CODEC,
                UploadImageCompletePayload::handle
        );

        // 更新图片布局配置（客户端 -> 服务端）
        registrar.playToServer(
                UpdateImageConfigPayload.TYPE,
                UpdateImageConfigPayload.CODEC,
                UpdateImageConfigPayload::handle
        );

        // 删除图片（客户端 -> 服务端）
        registrar.playToServer(
                RemoveImagePayload.TYPE,
                RemoveImagePayload.CODEC,
                RemoveImagePayload::handle
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

        // 推送雷达槽位到终端（客户端 -> 服务端）
        registrar.playToServer(
                PushRadarSlotsPayload.TYPE,
                PushRadarSlotsPayload.CODEC,
                PushRadarSlotsPayload::handle
        );

        // 扫描附近雷达 Monitor（客户端 -> 服务端）
        registrar.playToServer(
                ScanRadarPayload.TYPE,
                ScanRadarPayload.CODEC,
                ScanRadarPayload::handle
        );

        // ========== 雷达系统网络包 ==========
        // 添加雷达槽位（客户端 -> 服务端）
        registrar.playToServer(
                AddRadarSlotPayload.TYPE,
                AddRadarSlotPayload.CODEC,
                AddRadarSlotPayload::handle
        );

        // 删除雷达槽位（客户端 -> 服务端）
        registrar.playToServer(
                RemoveRadarSlotPayload.TYPE,
                RemoveRadarSlotPayload.CODEC,
                RemoveRadarSlotPayload::handle
        );

        // 更新雷达槽位（客户端 -> 服务端）
        registrar.playToServer(
                UpdateRadarSlotPayload.TYPE,
                UpdateRadarSlotPayload.CODEC,
                UpdateRadarSlotPayload::handle
        );

        // 终端命名（客户端 -> 服务端）
        registrar.playToServer(
                SetTerminalNamePayload.TYPE,
                SetTerminalNamePayload.CODEC,
                SetTerminalNamePayload::handle
        );

        // 保存Pro配置（客户端→服务端：图层+全部槽位）
        registrar.playToServer(
                SaveProConfigPayload.TYPE,
                SaveProConfigPayload.CODEC,
                SaveProConfigPayload::handle
        );

        // 请求打开插件槽位（客户端 -> 服务端）
        registrar.playToServer(
                RequestOpenPluginSlotsPayload.TYPE,
                RequestOpenPluginSlotsPayload.CODEC,
                RequestOpenPluginSlotsPayload::handle
        );

        // ========== NbtReader 网络包 ==========
        registrar.playToClient(
                OpenNbtReaderScreenPayload.TYPE,
                OpenNbtReaderScreenPayload.CODEC,
                OpenNbtReaderScreenPayload::handle
        );
        registrar.playToServer(
                UpdateNbtReaderConfigPayload.TYPE,
                UpdateNbtReaderConfigPayload.CODEC,
                UpdateNbtReaderConfigPayload::handle
        );

        // ========== HUD 显示同步 + 客户端屏幕 ==========
        registrar.playToClient(
                SyncDisplayDataPayload.TYPE,
                SyncDisplayDataPayload.CODEC,
                SyncDisplayDataPayload::handle
        );
        registrar.playToClient(
                OpenTerminalConfigScreenPayload.TYPE,
                OpenTerminalConfigScreenPayload.CODEC,
                OpenTerminalConfigScreenPayload::handle
        );
        registrar.playToClient(
                OpenTerminalProConfigScreenPayload.TYPE,
                OpenTerminalProConfigScreenPayload.CODEC,
                OpenTerminalProConfigScreenPayload::handle
        );
        registrar.playToClient(
                SyncRadarDataPayload.TYPE,
                SyncRadarDataPayload.CODEC,
                SyncRadarDataPayload::handle
        );
    }

}