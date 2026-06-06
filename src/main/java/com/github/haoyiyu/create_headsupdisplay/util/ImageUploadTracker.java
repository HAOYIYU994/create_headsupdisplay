package com.github.haoyiyu.create_headsupdisplay.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端分块上传跟踪器：接收来自客户端的分块图片数据，
 * 在所有块到达后组装为完整字节数组。
 * 包含 60 秒超时自动清理机制。
 */
public class ImageUploadTracker {
    private static final Map<UUID, InProgressUpload> UPLOADS = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 60_000;

    /** 初始化一个新的上传任务 */
    public static void init(UUID id, int totalChunks, String fileName) {
        UPLOADS.put(id, new InProgressUpload(totalChunks, fileName));
    }

    /** 存储一个数据块 */
    public static void storeChunk(UUID id, int index, byte[] data) {
        InProgressUpload upload = UPLOADS.get(id);
        if (upload == null) return;
        if (index < 0 || index >= upload.chunks.length) return;
        if (upload.chunks[index] == null) {
            upload.chunks[index] = data;
            upload.receivedChunks++;
        }
    }

    /**
     * 尝试组装完整数据。
     * @return 完整字节数组，如果尚未收齐则返回 null
     */
    public static byte[] assemble(UUID id) {
        InProgressUpload upload = UPLOADS.get(id);
        if (upload == null) return null;
        if (upload.receivedChunks < upload.totalChunks) return null;

        // 计算总大小并组装
        int totalSize = 0;
        for (byte[] chunk : upload.chunks) {
            if (chunk == null) return null;
            totalSize += chunk.length;
        }

        byte[] result = new byte[totalSize];
        int offset = 0;
        for (byte[] chunk : upload.chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    /** 获取文件名 */
    public static String getFileName(UUID id) {
        InProgressUpload upload = UPLOADS.get(id);
        return upload != null ? upload.fileName : "unknown.png";
    }

    /** 移除上传记录 */
    public static void remove(UUID id) {
        UPLOADS.remove(id);
    }

    /** 清理超时的上传（超过 60 秒未完成） */
    public static void cleanStale() {
        long now = System.currentTimeMillis();
        UPLOADS.entrySet().removeIf(entry -> (now - entry.getValue().createdAt) > TIMEOUT_MS);
    }

    private static class InProgressUpload {
        final byte[][] chunks;
        final int totalChunks;
        final String fileName;
        final long createdAt;
        int receivedChunks;

        InProgressUpload(int totalChunks, String fileName) {
            this.totalChunks = totalChunks;
            this.fileName = fileName;
            this.chunks = new byte[totalChunks][];
            this.createdAt = System.currentTimeMillis();
            this.receivedChunks = 0;
        }
    }
}
