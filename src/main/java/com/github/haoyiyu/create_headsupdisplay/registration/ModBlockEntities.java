package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateHeadsUpDisplay.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayTerminalBlockEntity>> DISPLAY_TERMINAL_BE =
            BLOCK_ENTITIES.register("display_terminal_be",
                    () -> BlockEntityType.Builder.of(DisplayTerminalBlockEntity::new, ModBlocks.DISPLAY_TERMINAL.get()).build(null));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}