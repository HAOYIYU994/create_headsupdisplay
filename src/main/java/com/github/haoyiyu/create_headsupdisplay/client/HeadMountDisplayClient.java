package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class HeadMountDisplayClient {
    public static final PartialModel HEAD_MOUNT_DISPLAY_MODEL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "item/head_mount_display")
    );

    public static void init() {
        // 触发静态初始化（如果需要）
    }
}