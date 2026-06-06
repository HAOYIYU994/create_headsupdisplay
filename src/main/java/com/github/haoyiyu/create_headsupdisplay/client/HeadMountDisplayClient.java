package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.client.render.DisplayBlockRenderer;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class HeadMountDisplayClient {
    public static final PartialModel HEAD_MOUNT_DISPLAY_MODEL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "item/head_mount_display")
    );

    public static void init() {}

    @EventBusSubscriber(modid = CreateHeadsUpDisplay.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.DISPLAY.get(), DisplayBlockRenderer::new);
        }
    }
}
