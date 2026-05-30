package com.github.haoyiyu.create_headsupdisplay.client.model;

import com.github.haoyiyu.create_headsupdisplay.client.HeadMountDisplayClient;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class HeadMountDisplayModel extends BakedModelWrapper<BakedModel> {
    public HeadMountDisplayModel(BakedModel original) {
        super(original);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, com.mojang.blaze3d.vertex.PoseStack poseStack, boolean leftHanded) {
        if (transformType == ItemDisplayContext.HEAD) {
            return HeadMountDisplayClient.HEAD_MOUNT_DISPLAY_MODEL.get().applyTransform(transformType, poseStack, leftHanded);
        }
        return super.applyTransform(transformType, poseStack, leftHanded);
    }
}