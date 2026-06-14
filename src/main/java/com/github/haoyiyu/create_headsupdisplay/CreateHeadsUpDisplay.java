package com.github.haoyiyu.create_headsupdisplay;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalTarget;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalProTarget;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreDisplayTarget;
import com.github.haoyiyu.create_headsupdisplay.client.HeadMountDisplayClient;
import com.github.haoyiyu.create_headsupdisplay.config.ModConfig;
import net.neoforged.fml.ModContainer;
import com.github.haoyiyu.create_headsupdisplay.menu.DisplayTerminalScreen;
import com.github.haoyiyu.create_headsupdisplay.screen.FrequencySelectionScreen;
import com.github.haoyiyu.create_headsupdisplay.screen.PluginSlotScreen;
import com.github.haoyiyu.create_headsupdisplay.registration.*;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalProBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CreateHeadsUpDisplay.MOD_ID)
public class CreateHeadsUpDisplay {
    public static final String MOD_ID = "create_headsupdisplay";
    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.TextMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.BarMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.AltimeterMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.DialMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.DigitalMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.HudAltMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.CompassMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.SpeedoMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.ChartMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.XYZMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.LEDMode());
        com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.register(new com.github.haoyiyu.create_headsupdisplay.client.gauge.GyroMode());
    }

    public CreateHeadsUpDisplay(IEventBus modBus, ModContainer container) {
        ModConfig.register(container);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
        ModDataComponents.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
        }

        // 注册 Tooltip 事件监听（使用 NeoForge 事件总线）
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        // 玩家登录时标记需要同步，下一 tick 执行
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
            if (pendingSync.remove(e.getEntity().getUUID()) && e.getEntity() instanceof ServerPlayer sp) {
                var level = sp.serverLevel();
                BlockPos pos = sp.blockPosition();
                int r = 128;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r,-r,-r), pos.offset(r,r,r))) {
                    if (!level.isLoaded(p)) continue;
                    if (level.getBlockEntity(p) instanceof DisplayTerminalBlockEntity t) t.syncToBoundPlayers();
                    else if (level.getBlockEntity(p) instanceof DisplayTerminalProBlockEntity t) t.syncToBoundPlayers();
                }
            }
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.lock();
            DisplayTarget.BY_BLOCK.register(ModBlocks.DISPLAY_TERMINAL.get(), new DisplayTerminalTarget());
            DisplayTarget.BY_BLOCK.register(ModBlocks.DISPLAY_TERMINAL_PRO.get(), new DisplayTerminalProTarget());
            DisplayTarget.BY_BLOCK.register(ModBlocks.OMNI_CORE.get(), new OmniCoreDisplayTarget());
            // 雷达集成（软依赖，通过反射安全调用）
            // 注意：必须用 Throwable 而非 Exception，因为 NoClassDefFoundError 是 Error 的子类
            try {
                Class<?> radarIntegration = Class.forName("com.github.haoyiyu.create_headsupdisplay.integration.RadarIntegration");
                radarIntegration.getMethod("init").invoke(null);
            } catch (Throwable ignored) {}
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // 屏幕注册通过 RegisterMenuScreensEvent 完成
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        boolean isShiftHeld = net.minecraft.client.gui.screens.Screen.hasShiftDown();

        if (stack.is(ModItems.HEAD_MOUNT_DISPLAY.get())) {
            if (isShiftHeld) {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.head_mount_display.line1"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.head_mount_display.line2"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.head_mount_display.line3"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                        Component.keybind("key.sneak")));
            }
        } else if (stack.is(ModItems.IMAGE_PLUGIN.get())) {
            if (isShiftHeld) {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.image_plugin.line1"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.image_plugin.line2"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.image_plugin.line3"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                        Component.keybind("key.sneak")));
            }
        } else if (stack.is(ModItems.DISPLAY_TERMINAL_ITEM.get())) {
            if (isShiftHeld) {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal.line1"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal.line2"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal.line3"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal.line4"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal.line5"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal.line6"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                        Component.keybind("key.sneak")));
            }
        } else if (stack.is(ModItems.DISPLAY_TERMINAL_PRO_ITEM.get())) {
            if (isShiftHeld) {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal_pro.line1"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.display_terminal_pro.line2"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                        Component.keybind("key.sneak")));
            }
        } else if (stack.is(ModItems.FAST_DISPLAY_LINK_ITEM.get())) {
            if (isShiftHeld) {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.fast_display_link.line1"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                        Component.keybind("key.sneak")));
            }
        } else if (stack.is(ModItems.RADAR_PLUGIN.get())) {
            if (isShiftHeld) {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.radar_plugin.line1"));
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.radar_plugin.line2"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                        Component.keybind("key.sneak")));
            }
        }
    }

    /** 玩家登录时立即同步附近终端数据到头盔和显示器 */
    private static final java.util.Set<java.util.UUID> pendingSync = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            pendingSync.add(sp.getUUID());
        }
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.DISPLAY_TERMINAL_MENU.get(), DisplayTerminalScreen::new);
            event.register(ModMenus.FREQUENCY_SELECTION_MENU.get(), FrequencySelectionScreen::new);
            event.register(ModMenus.PLUGIN_SLOT_MENU.get(), PluginSlotScreen::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                HeadMountDisplayClient.init();
            });
        }
    }
}