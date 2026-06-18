package com.github.haoyiyu.create_headsupdisplay.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Local PNG cache in {@code run/create_headsupdisplay_images/}.
 * Images are saved on first receipt from the server and reloaded from disk
 * on subsequent sessions so the server doesn't have to re-send the raw bytes.
 */
@OnlyIn(Dist.CLIENT)
public class ImageCache {
    private static final Path DIR = Path.of("run", "create_headsupdisplay_images", "cache");

    public static Path fileFor(UUID id) {
        return DIR.resolve(id.toString() + ".png");
    }

    /** Save PNG bytes to disk. Creates the cache directory if missing. */
    public static void save(UUID id, byte[] data) {
        if (data == null || data.length < 8) return;
        try {
            Files.createDirectories(DIR);
            Files.write(fileFor(id), data);
        } catch (IOException ignored) {}
    }

    /** Load cached PNG bytes from disk, or {@code null} if not cached. */
    public static byte[] load(UUID id) {
        try {
            Path p = fileFor(id);
            if (Files.exists(p) && Files.size(p) > 7)
                return Files.readAllBytes(p);
        } catch (IOException ignored) {}
        return null;
    }
}
