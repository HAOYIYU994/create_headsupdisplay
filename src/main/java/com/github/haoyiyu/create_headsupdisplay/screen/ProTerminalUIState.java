package com.github.haoyiyu.create_headsupdisplay.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-side only persistence for Terminal Pro UI state.
 * Saves panel visibility/positions and current layer to a local NBT file.
 * Never sent to server.
 */
public class ProTerminalUIState {
    private static final String DIR_NAME = "create_headsupdisplay";
    private static final String FILE_NAME = "pro_terminal_ui_state.dat";

    // ── UI session state ──
    public boolean layerPanelVisible;
    public int layerPanelX, layerPanelY;
    public boolean colorPickerVisible;
    public int colorPickerX, colorPickerY;
    public boolean stylePanelVisible;
    public int stylePanelX, stylePanelY;
    public boolean animPanelVisible;
    public int animPanelX, animPanelY;
    public int currentLayer;
    public float canvasZoom = 1f;

    // ── Persistent settings ──
    public boolean menuBarDefaultOpen = true;
    public boolean dockDefaultOpen = true;
    public int imageScalePreview = 0;   // 0=normal, 1=don't change
    public int exitBehavior = 0;        // 0=don't save, 1=save directly, 2=always ask
    public int uiTheme = 0;             // 0=dark, 1=light

    private static final ProTerminalUIState INSTANCE = new ProTerminalUIState();

    public static ProTerminalUIState get() { return INSTANCE; }

    private Path getFilePath() {
        //noinspection resource – gameDirectory is never null on client
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve(DIR_NAME);
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve(FILE_NAME);
    }

    /** Save current state to local file */
    public void save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("layerPanelVisible", layerPanelVisible);
        tag.putInt("layerPanelX", layerPanelX);
        tag.putInt("layerPanelY", layerPanelY);
        tag.putBoolean("colorPickerVisible", colorPickerVisible);
        tag.putInt("colorPickerX", colorPickerX);
        tag.putInt("colorPickerY", colorPickerY);
        tag.putBoolean("stylePanelVisible", stylePanelVisible);
        tag.putInt("stylePanelX", stylePanelX);
        tag.putInt("stylePanelY", stylePanelY);
        tag.putBoolean("animPanelVisible", animPanelVisible);
        tag.putInt("animPanelX", animPanelX);
        tag.putInt("animPanelY", animPanelY);
        tag.putInt("currentLayer", currentLayer);
        tag.putFloat("canvasZoom", canvasZoom);
        // settings
        tag.putBoolean("menuBarDefaultOpen", menuBarDefaultOpen);
        tag.putBoolean("dockDefaultOpen", dockDefaultOpen);
        tag.putInt("imageScalePreview", imageScalePreview);
        tag.putInt("exitBehavior", exitBehavior);
        tag.putInt("uiTheme", uiTheme);
        try {
            NbtIo.writeCompressed(tag, getFilePath());
        } catch (IOException ignored) {}
    }

    /** Load state from local file */
    public void load() {
        try {
            Path path = getFilePath();
            if (!Files.exists(path)) return;
            CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            layerPanelVisible = tag.getBoolean("layerPanelVisible");
            layerPanelX = tag.getInt("layerPanelX");
            layerPanelY = tag.getInt("layerPanelY");
            colorPickerVisible = tag.getBoolean("colorPickerVisible");
            colorPickerX = tag.getInt("colorPickerX");
            colorPickerY = tag.getInt("colorPickerY");
            stylePanelVisible = tag.getBoolean("stylePanelVisible");
            stylePanelX = tag.getInt("stylePanelX");
            stylePanelY = tag.getInt("stylePanelY");
            animPanelVisible = tag.getBoolean("animPanelVisible");
            animPanelX = tag.getInt("animPanelX");
            animPanelY = tag.getInt("animPanelY");
            currentLayer = tag.getInt("currentLayer");
            canvasZoom = tag.contains("canvasZoom") ? tag.getFloat("canvasZoom") : 1f;
            // settings (defaults are already set above)
            if (tag.contains("menuBarDefaultOpen")) menuBarDefaultOpen = tag.getBoolean("menuBarDefaultOpen");
            if (tag.contains("dockDefaultOpen")) dockDefaultOpen = tag.getBoolean("dockDefaultOpen");
            if (tag.contains("imageScalePreview")) imageScalePreview = tag.getInt("imageScalePreview");
            if (tag.contains("exitBehavior")) exitBehavior = tag.getInt("exitBehavior");
            if (tag.contains("uiTheme")) uiTheme = tag.getInt("uiTheme");
        } catch (Exception ignored) {}
    }
}
