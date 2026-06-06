package com.github.haoyiyu.create_headsupdisplay.integration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import com.happysg.radar.registry.AllDataBehaviors;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Create Radar 集成桥接。
 * 所有雷达相关代码通过此类隔离，确保模组在无雷达时也可独立运行。
 */
public class RadarIntegration {

    private static final String RADAR_MOD_ID = "create_radar";
    private static Boolean loaded = null;
    private static boolean initialized = false;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(RADAR_MOD_ID);
        }
        return loaded;
    }

    /**
     * 在 FMLCommonSetupEvent 中调用。
     * 雷达加载时注册 OmniCore 为 DataController 显示目标。
     */
    public static void init() {
        if (!isLoaded() || initialized) return;
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    CreateHeadsUpDisplay.MOD_ID, "omni_core_radar_target");
            AllDataBehaviors.assignBlockEntity(
                    AllDataBehaviors.register(id, new OmniCoreRadarBehavior()),
                    ModBlockEntities.OMNI_CORE.get()
            );
            CreateHeadsUpDisplay.LOGGER.info("Radar integration: OmniCore registered as DataController");
            initialized = true;
        } catch (Exception e) {
            CreateHeadsUpDisplay.LOGGER.error("Failed to register radar integration: {}", e.getMessage());
        }
    }
}
