package com.github.haoyiyu.create_headsupdisplay.hud;

import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(Dist.CLIENT)
public class HudOverlayRenderer {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack helmet = player.getInventory().armor.get(3);
        if (!(helmet.getItem() instanceof HeadMountDisplayItem)) return;
        if (HeadMountDisplayItem.getBoundTerminalPos(helmet) == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        var font = mc.font;

        // 渲染数据源槽位（动态数据，来自显示连接器）
        var slots = ClientHudData.getSlots();
        if (slots != null) {
            for (ClientHudData.SlotRenderData slot : slots) {
                graphics.pose().pushPose();
                graphics.pose().translate(slot.posX, slot.posY, 0);
                graphics.pose().scale(slot.scale, slot.scale, 1.0f);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));

                int textColor = (slot.alpha << 24) | (slot.color & 0x00FFFFFF);

                if (slot.displayLine == 2) {
                    // 进度条模式
                    String[] parts = slot.text.split("/");
                    if (parts.length == 2) {
                        try {
                            int current = Integer.parseInt(parts[0].trim());
                            int max = Integer.parseInt(parts[1].trim());
                            if (max > 0) {
                                float percent = (float) current / max;
                                int barWidth = 80;
                                int barHeight = 10;
                                graphics.fill(0, 0, barWidth, barHeight, 0xFF333333);
                                int fillWidth = (int)(barWidth * percent);
                                graphics.fill(0, 0, fillWidth, barHeight, 0xFF00FF00);
                                String text = current + "/" + max;
                                int textWidth = font.width(text);
                                graphics.drawString(font, text, (barWidth - textWidth) / 2, barHeight + 2, textColor, true);
                            } else {
                                graphics.drawString(font, slot.text, 0, 0, textColor, true);
                            }
                        } catch (NumberFormatException e) {
                            graphics.drawString(font, slot.text, 0, 0, textColor, true);
                        }
                    } else {
                        graphics.drawString(font, slot.text, 0, 0, textColor, true);
                    }
                } else {
                    // 纯文本模式
                    graphics.drawString(font, slot.text, 0, 0, textColor, true);
                }

                graphics.pose().popPose();
            }
        }

        // 渲染静态文本槽位（支持透明度）
        var staticSlots = ClientHudData.getStaticTextSlots();
        if (staticSlots != null && !staticSlots.isEmpty()) {
            for (var slot : staticSlots) {
                graphics.pose().pushPose();
                graphics.pose().translate(slot.posX, slot.posY, 0);
                graphics.pose().scale(slot.scale, slot.scale, 1.0f);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));
                // 合成 ARGB 颜色
                int argb = (slot.alpha << 24) | (slot.color & 0x00FFFFFF);
                graphics.drawString(font, slot.text, 0, 0, argb, false);
                graphics.pose().popPose();
            }
        }
    }
}