package com.github.haoyiyu.create_headsupdisplay.api;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

public class DisplayModeRegistry {
    private static final Map<ResourceLocation, IDisplayMode> MODES = new LinkedHashMap<>();
    private static final Map<Integer, ResourceLocation> LEGACY = new HashMap<>();
    private static boolean locked = false;

    public static void register(IDisplayMode mode) {
        if (locked) throw new IllegalStateException("DisplayModeRegistry is locked");
        MODES.put(mode.getId(), mode);
        if (mode.getLegacyId() >= 0) LEGACY.put(mode.getLegacyId(), mode.getId());
    }

    public static void lock() { locked = true; }
    public static IDisplayMode get(ResourceLocation id) { return MODES.get(id); }
    public static IDisplayMode get(int legacyId) { var rl = LEGACY.get(legacyId); return rl != null ? MODES.get(rl) : null; }
    public static Collection<IDisplayMode> getAll() { return Collections.unmodifiableCollection(MODES.values()); }
}
