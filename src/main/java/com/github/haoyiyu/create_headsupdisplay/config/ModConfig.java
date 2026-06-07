package com.github.haoyiyu.create_headsupdisplay.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec SERVER_CONFIG;

    public static final ModConfigSpec.IntValue IMAGE_MAX_SIZE_KB;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("image");
        IMAGE_MAX_SIZE_KB = builder
                .comment("Maximum image upload size in kilobytes. Set to 0 to disable image uploads entirely. (default: 512)")
                .defineInRange("maxSizeKB", 512, 0, 10240);
        builder.pop();
        SERVER_CONFIG = builder.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(Type.SERVER, SERVER_CONFIG);
    }
}
