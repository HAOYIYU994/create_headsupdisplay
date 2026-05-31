package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateHeadsUpDisplay.MOD_ID);

    public static final Supplier<CreativeModeTab> HEADS_UP_DISPLAY_TAB = CREATIVE_MODE_TABS.register(
            "heads_up_display_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_headsupdisplay"))
                    .icon(() -> new ItemStack(ModItems.HEAD_MOUNT_DISPLAY.get()))
                    .displayItems((parameters, output) -> {
                        // 添加模组的所有物品
                        output.accept(ModItems.HEAD_MOUNT_DISPLAY.get());
                        output.accept(ModItems.DISPLAY_TERMINAL_ITEM.get());
                        output.accept(ModItems.OMNI_CORE_ITEM.get());
                        output.accept(ModItems.LINK_BLOCK_ITEM.get());
                    })
                    .build()
    );
}