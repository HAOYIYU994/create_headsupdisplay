package com.github.haoyiyu.create_headsupdisplay.api;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads external {@link IDisplayMode} implementations from a folder on disk.
 * Drop {@code .class} files into {@code run/create_headsupdisplay_modes/} and
 * they will be discovered, instantiated and registered automatically on startup.
 */
public class ExternalModeLoader {

    private static final Path DIR = Path.of("run", "create_headsupdisplay_modes");

    /** Scan the external modes folder and register every found IDisplayMode. */
    public static void loadAndRegister() {
        if (!Files.isDirectory(DIR)) {
            try { Files.createDirectories(DIR); } catch (IOException ignored) {}
            return;
        }

        List<Class<?>> found = new ArrayList<>();
        try (var stream = Files.list(DIR)) {
            stream.filter(p -> p.toString().endsWith(".class")).forEach(clsFile -> {
                try {
                    String name = clsFile.getFileName().toString().replace(".class", "");
                    // Load from the parent of the .class file so packages work
                    URL[] urls = { DIR.toUri().toURL() };
                    try (URLClassLoader cl = new URLClassLoader(urls, IDisplayMode.class.getClassLoader())) {
                        Class<?> c = cl.loadClass(name);
                        if (IDisplayMode.class.isAssignableFrom(c) && !c.isInterface()) {
                            found.add(c);
                        }
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    CreateHeadsUpDisplay.LOGGER.warn("[ExternalMode] Failed to load {}: {}", clsFile, e.getMessage());
                }
            });
        } catch (IOException e) {
            CreateHeadsUpDisplay.LOGGER.warn("[ExternalMode] Scan failed: {}", e.getMessage());
        }

        for (Class<?> c : found) {
            try {
                IDisplayMode mode = (IDisplayMode) c.getDeclaredConstructor().newInstance();
                DisplayModeRegistry.register(mode);
                CreateHeadsUpDisplay.LOGGER.info("[ExternalMode] Registered: {}", mode.getId());
            } catch (Exception e) {
                CreateHeadsUpDisplay.LOGGER.warn("[ExternalMode] Instantiate {} failed: {}", c.getName(), e.getMessage());
            }
        }
    }
}
