package com.github.haoyiyu.create_headsupdisplay.menu;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.config.HudPositionConfig;
import com.github.haoyiyu.create_headsupdisplay.registration.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class DisplayTerminalMenu extends AbstractContainerMenu {
    private final DisplayTerminalBlockEntity terminal;
    private final UUID playerUuid;
    private HudPositionConfig dummyConfig; // 临时占位

    // 双参数构造函数（供 MenuType 注册）
    public DisplayTerminalMenu(int containerId, Inventory inv) {
        this(containerId, inv, null, null);
    }

    // 四参数构造函数（供 SimpleMenuProvider 调用）
    public DisplayTerminalMenu(int containerId, Inventory inv, DisplayTerminalBlockEntity terminal, UUID playerUuid) {
        super(ModMenus.DISPLAY_TERMINAL_MENU.get(), containerId);
        this.terminal = terminal;
        this.playerUuid = playerUuid;
        this.dummyConfig = HudPositionConfig.defaultConfig();

        // 玩家背包槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
        }
    }

    // 临时方法（供旧 GUI 使用）
    public HudPositionConfig getConfig() {
        return dummyConfig;
    }

    public void updateConfig(HudPositionConfig config) {
        this.dummyConfig = config;
        // 这里可以后续实现保存到终端（但新GUI会替换）
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}