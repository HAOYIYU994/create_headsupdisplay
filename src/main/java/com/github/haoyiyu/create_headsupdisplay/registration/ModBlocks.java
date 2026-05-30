package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateHeadsUpDisplay.MOD_ID);

    static {
        System.out.println("ModBlocks static init");
    }

    public static final DeferredBlock<DisplayTerminalBlock> DISPLAY_TERMINAL = BLOCKS.register("display_terminal",
            () -> {
                System.out.println("Creating DisplayTerminalBlock instance");
                return new DisplayTerminalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
            });

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus);
        System.out.println("ModBlocks.register called");
        System.out.println("ModBlocks.register finished");
    }
}