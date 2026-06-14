package com.github.haoyiyu.create_headsupdisplay.api;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
@FunctionalInterface
public interface TextureRegistrar {
    ResourceLocation register(String name, NativeImage image);
}
