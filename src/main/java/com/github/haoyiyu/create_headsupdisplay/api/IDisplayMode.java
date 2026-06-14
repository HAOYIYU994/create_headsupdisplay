package com.github.haoyiyu.create_headsupdisplay.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Display mode interface. Implement and register via {@link DisplayModeRegistry#register}. */
public interface IDisplayMode {
    ResourceLocation getId();
    int getMinDataSourceCount();  // how many sources this mode needs
    int getMaxDataSourceCount();  // -1 = unlimited
    boolean needsNumericData();
    int getDefaultWidth();
    int getDefaultHeight();
    void render(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h);
    default void renderPreview(GuiGraphics g, Font font, List<String> dataValues, DisplayModeConfig config, int w, int h) {
        render(g, font, dataValues, config, w, h);
    }
    default void registerTextures(TextureRegistrar reg) {}
    default List<ConfigParamDescriptor> getConfigParameters() {
        return List.of(
            ConfigParamDescriptor.of("max", ConfigParamType.FLOAT, 100f),
            ConfigParamDescriptor.of("min", ConfigParamType.FLOAT, 0f),
            ConfigParamDescriptor.of("unit", ConfigParamType.STRING, "")
        );
    }
    default int getLegacyId() { return -1; }
    default Component getDisplayName() {
        ResourceLocation id = getId();
        return Component.translatable("display_mode." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
    }
}
