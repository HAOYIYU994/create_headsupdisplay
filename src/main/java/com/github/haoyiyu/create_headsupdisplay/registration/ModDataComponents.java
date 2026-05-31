package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateHeadsUpDisplay.MOD_ID);

    public static final Supplier<DataComponentType<BlockPos>> BOUND_TERMINAL_POS =
            DATA_COMPONENTS.register("bound_terminal_pos",
                    () -> DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static final Supplier<DataComponentType<BlockPos>> OMNI_CORE_BOUND_TERMINAL =
            DATA_COMPONENTS.register("omni_core_bound_terminal",
                    () -> DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static final Supplier<DataComponentType<BlockPos>> LINKED_OMNI_CORE_POS =
            DATA_COMPONENTS.register("linked_omni_core_pos",
                    () -> DataComponentType.<BlockPos>builder()
                            .persistent(BlockPos.CODEC)
                            .networkSynchronized(BlockPos.STREAM_CODEC)
                            .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}