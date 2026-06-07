package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayBlock;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlock;
import com.github.haoyiyu.create_headsupdisplay.block.LinkBlock;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateHeadsUpDisplay.MOD_ID);

    public static final DeferredBlock<DisplayTerminalBlock> DISPLAY_TERMINAL = BLOCKS.registerBlock("display_terminal",
            DisplayTerminalBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops());

    // 数据集成核心方块
    public static final DeferredBlock<OmniCoreBlock> OMNI_CORE = BLOCKS.registerBlock("omni_core",
            OmniCoreBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0f));

    // 链接方块：连接 OmniCore 和 Display Terminal
    public static final DeferredBlock<LinkBlock> LINK_BLOCK = BLOCKS.registerBlock("link_block",
            LinkBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).noOcclusion());

    public static final DeferredBlock<DisplayBlock> DISPLAY = BLOCKS.registerBlock("display",
            DisplayBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).noOcclusion()
                    .lightLevel(s -> 8));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus);
    }
}