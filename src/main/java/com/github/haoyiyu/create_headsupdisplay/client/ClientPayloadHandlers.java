package com.github.haoyiyu.create_headsupdisplay.client;

import com.github.haoyiyu.create_headsupdisplay.screen.NbtReaderScreen;
import com.github.haoyiyu.create_headsupdisplay.screen.OmniCoreScreen;
import com.github.haoyiyu.create_headsupdisplay.screen.TerminalConfigScreen;
import com.github.haoyiyu.create_headsupdisplay.screen.TerminalProConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * 客户端专用 Payload 处理器。
 * 所有引用 Minecraft 客户端类（Minecraft、Screen 等）的代码集中在此，
 * 以避免 Payload 类的字节码常量池中包含 @OnlyIn(Dist.CLIENT) 类别，
 * 从而触发 NeoForge RuntimeDistCleaner 在专用服务端上的拒绝。
 */
public final class ClientPayloadHandlers {

    public static void openOmniCoreScreen(CompoundTag data) {
        Minecraft.getInstance().setScreen(new OmniCoreScreen(data));
    }

    public static void openNbtReaderScreen(CompoundTag data) {
        Minecraft.getInstance().setScreen(new NbtReaderScreen(data));
    }

    public static void openTerminalConfigScreen(CompoundTag slotsData) {
        Minecraft.getInstance().setScreen(new TerminalConfigScreen(slotsData));
    }

    public static void openTerminalProConfigScreen(CompoundTag slotsData) {
        Minecraft.getInstance().setScreen(new TerminalProConfigScreen(slotsData));
    }
}
