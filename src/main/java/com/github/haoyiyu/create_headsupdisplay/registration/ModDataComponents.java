package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(CreateHeadsUpDisplay.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> BOUND_TERMINAL_POS =
            DATA_COMPONENTS.registerComponentType("bound_terminal_pos",
                    builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}