package com.github.haoyiyu.create_headsupdisplay.menu;

import com.github.haoyiyu.create_headsupdisplay.registration.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FrequencySelectionMenu extends AbstractContainerMenu {
    private final SimpleContainer frequencyContainer = new SimpleContainer(2);
    private final Slot frequencySlot1;
    private final Slot frequencySlot2;

    public FrequencySelectionMenu(int containerId, Inventory inv) {
        super(ModMenus.FREQUENCY_SELECTION_MENU.get(), containerId);

        // 频率物品槽 1（左侧，x=62）
        this.frequencySlot1 = this.addSlot(new Slot(frequencyContainer, 0, 62, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 频率物品槽 2（右侧，x=98）
        this.frequencySlot2 = this.addSlot(new Slot(frequencyContainer, 1, 98, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 玩家背包槽位（y 从 94 开始）
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }
        // 快捷栏槽位
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 152));
        }
    }

    public ItemStack getFrequencyItem1() {
        return frequencyContainer.getItem(0);
    }

    public ItemStack getFrequencyItem2() {
        return frequencyContainer.getItem(1);
    }

    public boolean hasBothFrequencyItems() {
        return !frequencyContainer.getItem(0).isEmpty() && !frequencyContainer.getItem(1).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // 频率槽 (index 0-1) -> 玩家背包 (index 2-37)
        if (index == 0 || index == 1) {
            if (!this.moveItemStackTo(stack, 2, this.slots.size(), true))
                return ItemStack.EMPTY;
        } else { // 玩家背包 -> 优先填频率槽
            if (!this.moveItemStackTo(stack, 0, 2, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 关闭屏幕时，如果频率槽还有物品，丢给玩家
        if (!player.level().isClientSide) {
            ItemStack remaining1 = frequencyContainer.removeItemNoUpdate(0);
            if (!remaining1.isEmpty()) {
                player.drop(remaining1, false);
            }
            ItemStack remaining2 = frequencyContainer.removeItemNoUpdate(1);
            if (!remaining2.isEmpty()) {
                player.drop(remaining2, false);
            }
        }
    }
}