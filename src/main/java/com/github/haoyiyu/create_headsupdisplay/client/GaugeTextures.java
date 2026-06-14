package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import java.util.function.BiConsumer;

@EventBusSubscriber(value = Dist.CLIENT, modid = CreateHeadsUpDisplay.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GaugeTextures {

    public static ResourceLocation DIAL_FACE;
    public static ResourceLocation DIAL_NEEDLE;
    public static ResourceLocation ALTIMETER_SCALE;
    public static ResourceLocation BAR_FRAME;
    public static ResourceLocation DIGITAL_BG;

    @SubscribeEvent
    public static void onRegister(final RegisterClientReloadListenersEvent event) {
        for (var mode : DisplayModeRegistry.getAll()) {
            mode.registerTextures((name, image) -> {
                var tex = new DynamicTexture(image);
                var rl = ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "gauge/" + name);
                Minecraft.getInstance().getTextureManager().register(rl, tex);
                switch (name) {
                    case "dial_face" -> DIAL_FACE = rl;
                    case "dial_needle" -> DIAL_NEEDLE = rl;
                    case "altimeter_scale" -> ALTIMETER_SCALE = rl;
                    case "bar_frame" -> BAR_FRAME = rl;
                    case "digital_bg" -> DIGITAL_BG = rl;
                    default -> {}
                }
                return rl;
            });
        }
    }
}
