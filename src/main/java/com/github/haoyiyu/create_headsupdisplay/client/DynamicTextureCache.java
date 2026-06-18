package com.github.haoyiyu.create_headsupdisplay.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
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
    private static final Map<UUID, Integer> HASH_CACHE = new HashMap<>(); // 用于检测数据变化
    private static int maxBytes() { return com.github.haoyiyu.create_headsupdisplay.config.ModConfig.IMAGE_MAX_SIZE_KB.get() * 1024; }

    /**
     * 获取或创建纹理。优先从本地缓存加载；若无缓存则使用传入的 bytes 并写入磁盘。
     */
    public static ResourceLocation getOrCreate(UUID imageId, byte[] pngBytes) {
        if (CACHE.containsKey(imageId)) {
            return CACHE.get(imageId);
        }
        // Prefer local disk cache over network bytes
        byte[] src = ImageCache.load(imageId);
        if (src == null && pngBytes != null && pngBytes.length <= maxBytes()) {
            src = pngBytes;
            ImageCache.save(imageId, pngBytes);
        }
        if (src == null || src.length > maxBytes()) return null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(src);
            NativeImage nativeImage = NativeImage.read(in);
            in.close();
            DynamicTexture texture = new DynamicTexture(nativeImage);
            SIZE_CACHE.put(imageId, new int[]{nativeImage.getWidth(), nativeImage.getHeight()});
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "create_headsupdisplay", "dynamic/" + imageId.toString());
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            CACHE.put(imageId, loc);
            return loc;
        } catch (IOException e) { return null; }
    }

    /** 更新纹理数据（用于雷达图等实时刷新的图片） */
    public static ResourceLocation getOrUpdate(UUID imageId, byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length > maxBytes()) return CACHE.get(imageId);
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(pngBytes);
            NativeImage nativeImage = NativeImage.read(in);
            in.close();
            if (CACHE.containsKey(imageId)) {
                ResourceLocation loc = CACHE.get(imageId);
                var tex = Minecraft.getInstance().getTextureManager().getTexture(loc);
                if (tex instanceof DynamicTexture dt) {
                    dt.setPixels(nativeImage);
                    dt.upload();
                }
                SIZE_CACHE.put(imageId, new int[]{nativeImage.getWidth(), nativeImage.getHeight()});
                return loc;
            }
            // 新建
            DynamicTexture texture = new DynamicTexture(nativeImage);
            SIZE_CACHE.put(imageId, new int[]{nativeImage.getWidth(), nativeImage.getHeight()});
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "create_headsupdisplay", "dynamic/" + imageId.toString());
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            CACHE.put(imageId, loc);
            return loc;
        } catch (IOException e) { return CACHE.get(imageId); }
    }

    /** 仅在数据变化时更新纹理（由数据到达时调用，避免每帧重复上传）。首次写入本地缓存。 */
    public static void ensureUpdated(UUID imageId, byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length > maxBytes()) return;
        // Write to local disk cache so future sessions can reload without network
        ImageCache.save(imageId, pngBytes);
        int newHash = Arrays.hashCode(pngBytes);
        Integer oldHash = HASH_CACHE.get(imageId);
        if (oldHash != null && oldHash == newHash) return; // 未变化，跳过
        HASH_CACHE.put(imageId, newHash);
        // 释放旧纹理并重建，保证 GPU 上不留残留
        if (CACHE.containsKey(imageId)) {
            Minecraft.getInstance().getTextureManager().release(CACHE.get(imageId));
            CACHE.remove(imageId);
        }
        SIZE_CACHE.remove(imageId);
        getOrCreate(imageId, pngBytes);
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
        HASH_CACHE.remove(imageId);
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
        HASH_CACHE.clear();
    }
}
