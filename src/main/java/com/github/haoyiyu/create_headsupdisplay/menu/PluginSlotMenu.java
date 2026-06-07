package com.github.haoyiyu.create_headsupdisplay.menu;

import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.registration.ModMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PluginSlotMenu extends AbstractContainerMenu {
    private final SimpleContainer pluginContainer = new SimpleContainer(27);
    private SimpleContainer coreInv;
    private OmniCoreBlockEntity core;

    public PluginSlotMenu(int id, Inventory inv) {
        super(ModMenus.PLUGIN_SLOT_MENU.get(), id);
        setupSlots(inv);
    }

    public PluginSlotMenu(int id, Inventory inv, OmniCoreBlockEntity core) {
        super(ModMenus.PLUGIN_SLOT_MENU.get(), id);
        this.core = core;
        this.coreInv = core.getPluginInventory();
        for (int i = 0; i < 27; i++)
            pluginContainer.setItem(i, coreInv.getItem(i).copy());
        setupSlots(inv);
    }

    private void setupSlots(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(pluginContainer, col + row * 9, 8 + col * 18, 18 + row * 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (coreInv != null && !player.level().isClientSide) {
            boolean hadImage = core != null && core.hasImagePlugin();
            boolean hadRadar = core != null && core.hasRadarPlugin();
            for (int i = 0; i < 27; i++)
                coreInv.setItem(i, pluginContainer.getItem(i).copy());
            coreInv.setChanged();
            if (hadImage && core != null && !core.hasImagePlugin()) core.cleanupImageSources();
            if (hadRadar && core != null && !core.hasRadarPlugin()) core.cleanupRadarSources();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 63, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
