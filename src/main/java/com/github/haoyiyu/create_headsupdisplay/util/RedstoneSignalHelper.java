package com.github.haoyiyu.create_headsupdisplay.util;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RedstoneSignalHelper {
    /**
     * 根据两个频率物品，查询当前世界中的红石信号强度。
     * 无线红石频率由两个物品的 Frequency 共同决定（Couple<Frequency>）。
     */
    public static int getSignalStrength(Level level, ItemStack frequencyItem1, ItemStack frequencyItem2) {
        if (frequencyItem1.isEmpty() && frequencyItem2.isEmpty()) return 0;
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        Frequency freq1 = Frequency.of(frequencyItem1);
        Frequency freq2 = Frequency.of(frequencyItem2);
        Couple<Frequency> key = Couple.create(freq1, freq2);
        var networkSet = handler.networksIn(level).get(key);
        if (networkSet == null) return 0;
        int strength = 0;
        for (IRedstoneLinkable link : networkSet) {
            strength = Math.max(strength, link.getTransmittedStrength());
        }
        return strength;
    }
}