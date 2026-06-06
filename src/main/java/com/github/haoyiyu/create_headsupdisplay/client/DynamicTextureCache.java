package com.github.haoyiyu.create_headsupdisplay.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端动态纹理缓存：管理从 PNG 字节创建的 DynamicTexture，
 * 将 UUID 映射到已注册的 ResourceLocation，供 HUD 渲染时使用。
 */
@OnlyIn(Dist.CLIENT)
public class DynamicTextureCache {
    private static final Map<UUID, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<UUID, int[]> SIZE_CACHE = new HashMap<>(); // [width, height]
    private static final int MAX_PNG_BYTES = 512 * 1024; // 512KB

    /**
     * 获取或创建纹理。
     * @return 已注册的 ResourceLocation，失败返回 null
     */
    public static ResourceLocation getOrCreate(UUID imageId, byte[] pngBytes) {
        if (CACHE.containsKey(imageId)) {
            return CACHE.get(imageId);
        }

        // 大小上限保护
        if (pngBytes == null || pngBytes.length > MAX_PNG_BYTES) {
            return null;
        }

        try {
            // 使用 InputStream 方式读取，避免 NativeImage.read(byte[]) 的栈溢出
            ByteArrayInputStream in = new ByteArrayInputStream(pngBytes);
            NativeImage nativeImage = NativeImage.read(in);
            in.close();

            DynamicTexture texture = new DynamicTexture(nativeImage);

            // 记录尺寸
            SIZE_CACHE.put(imageId, new int[]{nativeImage.getWidth(), nativeImage.getHeight()});

            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "create_headsupdisplay", "dynamic/" + imageId.toString());
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            CACHE.put(imageId, loc);
            return loc;
        } catch (IOException e) {
            return null;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /** 获取已缓存纹理的宽度，-1 表示未缓存 */
    public static int getWidth(UUID imageId) {
        int[] size = SIZE_CACHE.get(imageId);
        return size != null ? size[0] : -1;
    }

    /** 获取已缓存纹理的高度，-1 表示未缓存 */
    public static int getHeight(UUID imageId) {
        int[] size = SIZE_CACHE.get(imageId);
        return size != null ? size[1] : -1;
    }

    /** 移除单个纹理 */
    public static void remove(UUID imageId) {
        ResourceLocation loc = CACHE.remove(imageId);
        SIZE_CACHE.remove(imageId);
        if (loc != null) {
            Minecraft.getInstance().getTextureManager().release(loc);
        }
    }

    /** 清除所有缓存的纹理并释放 GPU 资源 */
    public static void clear() {
        var tm = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation loc : CACHE.values()) {
            tm.release(loc);
        }
        CACHE.clear();
        SIZE_CACHE.clear();
    }
}
