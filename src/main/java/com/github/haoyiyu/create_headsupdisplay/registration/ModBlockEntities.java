package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.FastDisplayLinkBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.NbtReaderBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalProBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.LinkBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateHeadsUpDisplay.MOD_ID);

    public static final Supplier<BlockEntityType<DisplayTerminalBlockEntity>> DISPLAY_TERMINAL_BE =
            BLOCK_ENTITIES.register("display_terminal_be",
                    () -> BlockEntityType.Builder.of(DisplayTerminalBlockEntity::new,
                            ModBlocks.DISPLAY_TERMINAL.get()).build(null));

    public static final Supplier<BlockEntityType<DisplayTerminalProBlockEntity>> DISPLAY_TERMINAL_PRO_BE =
            BLOCK_ENTITIES.register("display_terminal_pro_be",
                    () -> BlockEntityType.Builder.of(DisplayTerminalProBlockEntity::new,
                            ModBlocks.DISPLAY_TERMINAL_PRO.get()).build(null));

    // 使用 Supplier 统一风格，避免 DeferredHolder 导入问题
    public static final Supplier<BlockEntityType<OmniCoreBlockEntity>> OMNI_CORE =
            BLOCK_ENTITIES.register("omni_core", () -> BlockEntityType.Builder.of(OmniCoreBlockEntity::new,
                    ModBlocks.OMNI_CORE.get()).build(null));

    public static final Supplier<BlockEntityType<LinkBlockEntity>> LINK =
            BLOCK_ENTITIES.register("link", () -> BlockEntityType.Builder.of(LinkBlockEntity::new,
                    ModBlocks.LINK_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<DisplayBlockEntity>> DISPLAY =
            BLOCK_ENTITIES.register("display", () -> BlockEntityType.Builder.of(DisplayBlockEntity::new,
                    ModBlocks.DISPLAY.get()).build(null));

    public static final Supplier<BlockEntityType<NbtReaderBlockEntity>> NBT_READER =
            BLOCK_ENTITIES.register("nbt_reader", () -> BlockEntityType.Builder.of(NbtReaderBlockEntity::new,
                    ModBlocks.NBT_READER.get()).build(null));

    public static final Supplier<BlockEntityType<FastDisplayLinkBlockEntity>> FAST_DISPLAY_LINK =
            BLOCK_ENTITIES.register("fast_display_link", () -> BlockEntityType.Builder.of(
                    FastDisplayLinkBlockEntity::new,
                    ModBlocks.FAST_DISPLAY_LINK.get()).build(null));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}