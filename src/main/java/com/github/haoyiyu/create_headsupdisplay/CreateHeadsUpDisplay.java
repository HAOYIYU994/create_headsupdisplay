package com.github.haoyiyu.create_headsupdisplay;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalTarget;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreDisplayTarget;
import com.github.haoyiyu.create_headsupdisplay.client.HeadMountDisplayClient;
import com.github.haoyiyu.create_headsupdisplay.integration.RadarIntegration;
import com.github.haoyiyu.create_headsupdisplay.menu.DisplayTerminalScreen;
import com.github.haoyiyu.create_headsupdisplay.screen.FrequencySelectionScreen;
import com.github.haoyiyu.create_headsupdisplay.registration.*;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CreateHeadsUpDisplay.MOD_ID)
public class CreateHeadsUpDisplay {
    public static final String MOD_ID = "create_headsupdisplay";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateHeadsUpDisplay(IEventBus modBus) {
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
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplayTarget.BY_BLOCK.register(ModBlocks.DISPLAY_TERMINAL.get(), new DisplayTerminalTarget());
            DisplayTarget.BY_BLOCK.register(ModBlocks.OMNI_CORE.get(), new OmniCoreDisplayTarget());
            // 雷达集成（软依赖，无雷达时安全跳过）
            RadarIntegration.init();
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // 屏幕注册通过 RegisterMenuScreensEvent 完成
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // 只处理我们自己的头盔物品
        if (!stack.is(ModItems.HEAD_MOUNT_DISPLAY.get())) return;

        // 直接使用 Screen.hasShiftDown() 检测 Shift 键（不依赖玩家实体）
        boolean isShiftHeld = net.minecraft.client.gui.screens.Screen.hasShiftDown();

        // 可选：日志输出便于调试（运行后查看控制台）
        CreateHeadsUpDisplay.LOGGER.info("Shift held (Screen.hasShiftDown): " + isShiftHeld);

        if (isShiftHeld) {
            event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.head_mount_display.line1"));
            event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.head_mount_display.line2"));
            event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.head_mount_display.line3"));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.create_headsupdisplay.hold_shift",
                    Component.keybind("key.sneak")));
        }
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.DISPLAY_TERMINAL_MENU.get(), DisplayTerminalScreen::new);
            event.register(ModMenus.FREQUENCY_SELECTION_MENU.get(), FrequencySelectionScreen::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                HeadMountDisplayClient.init();
            });
        }
    }
}