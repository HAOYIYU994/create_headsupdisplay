package com.github.haoyiyu.create_headsupdisplay.registration;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
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

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        ITEMS.register(bus);
    }
}