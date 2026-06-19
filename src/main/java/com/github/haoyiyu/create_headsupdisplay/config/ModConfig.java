package com.github.haoyiyu.create_headsupdisplay.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec SERVER_CONFIG;
    public static final ModConfigSpec CLIENT_CONFIG;

    public static final ModConfigSpec.IntValue IMAGE_MAX_SIZE_KB;
    public static final ModConfigSpec.DoubleValue MAX_SCALE;
    public static final ModConfigSpec.DoubleValue CANVAS_ZOOM;

    static {
        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        serverBuilder.push("image");
        IMAGE_MAX_SIZE_KB = serverBuilder
                .comment("Maximum image upload size in kilobytes. Set to 0 to disable image uploads entirely. (default: 512)")
                .defineInRange("maxSizeKB", 512, 0, 10240);
        serverBuilder.pop();
        serverBuilder.push("display");
        MAX_SCALE = serverBuilder
                .comment("Maximum scale multiplier for HUD slots. (default: 5.0, range: 0.5 ~ 20.0)")
                .defineInRange("maxScale", 5.0, 0.5, 20.0);
        serverBuilder.pop();
        SERVER_CONFIG = serverBuilder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.push("editor");
        CANVAS_ZOOM = clientBuilder
                .comment("Canvas zoom level for the Terminal Pro configuration screen. (default: 1.0, range: 0.01 ~ 1.0)")
                .defineInRange("canvasZoom", 1.0, 0.01, 1.0);
        clientBuilder.pop();
        CLIENT_CONFIG = clientBuilder.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(Type.SERVER, SERVER_CONFIG);
        container.registerConfig(Type.CLIENT, CLIENT_CONFIG);
    }
}
