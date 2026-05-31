package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.menu.DisplayTerminalMenu;
import com.github.haoyiyu.create_headsupdisplay.menu.FrequencySelectionMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, CreateHeadsUpDisplay.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<DisplayTerminalMenu>> DISPLAY_TERMINAL_MENU =
            MENUS.register("display_terminal_menu",
                    () -> new MenuType<>(DisplayTerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FrequencySelectionMenu>> FREQUENCY_SELECTION_MENU =
            MENUS.register("frequency_selection_menu",
                    () -> new MenuType<>(FrequencySelectionMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        MENUS.register(bus);
    }
}