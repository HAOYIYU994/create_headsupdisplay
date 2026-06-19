package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import com.github.haoyiyu.create_headsupdisplay.item.ImagePluginItem;
import com.github.haoyiyu.create_headsupdisplay.item.LinkBlockItem;
import com.github.haoyiyu.create_headsupdisplay.item.OmniCoreItem;
import com.github.haoyiyu.create_headsupdisplay.item.PluginBaseItem;
import com.github.haoyiyu.create_headsupdisplay.item.FastDisplayLinkBlockItem;
import com.github.haoyiyu.create_headsupdisplay.item.RadarPluginItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateHeadsUpDisplay.MOD_ID);

    public static final DeferredItem<HeadMountDisplayItem> HEAD_MOUNT_DISPLAY = ITEMS.register("head_mount_display",
            () -> new HeadMountDisplayItem(new Item.Properties().stacksTo(1).durability(0)));

    public static final DeferredItem<BlockItem> DISPLAY_TERMINAL_ITEM = ITEMS.register("display_terminal",
            () -> new BlockItem(ModBlocks.DISPLAY_TERMINAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> DISPLAY_TERMINAL_PRO_ITEM = ITEMS.register("display_terminal_pro",
            () -> new BlockItem(ModBlocks.DISPLAY_TERMINAL_PRO.get(), new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // 新增 OmniCore 物品
    public static final DeferredItem<OmniCoreItem> OMNI_CORE_ITEM = ITEMS.register("omni_core",
            () -> new OmniCoreItem(ModBlocks.OMNI_CORE.get(), new Item.Properties()));

    public static final DeferredItem<LinkBlockItem> LINK_BLOCK_ITEM = ITEMS.register("link_block",
            () -> new LinkBlockItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<BlockItem> DISPLAY_ITEM = ITEMS.register("display",
            () -> new BlockItem(ModBlocks.DISPLAY.get(), new Item.Properties()));

    public static final DeferredItem<ImagePluginItem> IMAGE_PLUGIN = ITEMS.register("image_plugin",
            () -> new ImagePluginItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<RadarPluginItem> RADAR_PLUGIN = ITEMS.register("radar_plugin",
            () -> new RadarPluginItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<PluginBaseItem> PLUGIN_BASE = ITEMS.register("plugin_base",
            () -> new PluginBaseItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<BlockItem> NBT_READER_ITEM = ITEMS.register("nbt_reader",
            () -> new BlockItem(ModBlocks.NBT_READER.get(), new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final DeferredItem<BlockItem> FAST_DISPLAY_LINK_ITEM = ITEMS.register("fast_display_link",
            FastDisplayLinkBlockItem::new);

    public static final DeferredItem<Item> PRO_UPGRADE_KIT = ITEMS.register("pro_upgrade_kit",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        ITEMS.register(bus);
    }
}