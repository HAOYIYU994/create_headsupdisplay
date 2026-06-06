package com.github.haoyiyu.create_headsupdisplay.hud;

import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.github.haoyiyu.create_headsupdisplay.client.DynamicTextureCache;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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

        // 渲染图片槽位（底层）
        var images = ClientHudData.getImages();
        if (images != null && !images.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (var img : images) {
                graphics.pose().pushPose();
                graphics.pose().translate(img.posX, img.posY, 0);
                graphics.pose().scale(img.scale, img.scale, 1.0f);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(img.rotation));

                ResourceLocation tex = DynamicTextureCache.getOrCreate(img.imageId, img.imageData);
                if (tex != null) {
                    int w = DynamicTextureCache.getWidth(img.imageId);
                    int h = DynamicTextureCache.getHeight(img.imageId);
                    if (w > 0 && h > 0) {
                        RenderSystem.setShaderColor(1f, 1f, 1f, img.alpha / 255f);
                        graphics.blit(tex, 0, 0, 0, 0, w, h, w, h);
                    }
                }
                graphics.pose().popPose();
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        // 渲染数据源槽位（顶层）
        var slots = ClientHudData.getSlots();
        if (slots != null) {
            for (ClientHudData.SlotRenderData slot : slots) {
                graphics.pose().pushPose();
                graphics.pose().translate(slot.posX, slot.posY, 0);
                graphics.pose().scale(slot.scale, slot.scale, 1.0f);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));

                int textColor = (slot.alpha << 24) | (slot.color & 0x00FFFFFF);

                String text = slot.text.replaceAll("§[0-9a-fk-or]", "");
                if (slot.displayLine == 2) {
                    String[] parts = text.split("/");
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
                                String display = current + "/" + max;
                                int textWidth = font.width(display);
                                graphics.drawString(font, display, (barWidth - textWidth) / 2, barHeight + 2, textColor, true);
                            } else {
                                graphics.drawString(font, text, 0, 0, textColor, true);
                            }
                        } catch (NumberFormatException e) {
                            graphics.drawString(font, text, 0, 0, textColor, true);
                        }
                    } else {
                        graphics.drawString(font, text, 0, 0, textColor, true);
                    }
                } else {
                    graphics.drawString(font, text, 0, 0, textColor, true);
                }

                graphics.pose().popPose();
            }
        }

        // 渲染静态文本槽位（顶层）
        var staticSlots = ClientHudData.getStaticTextSlots();
        if (staticSlots != null && !staticSlots.isEmpty()) {
            for (var slot : staticSlots) {
                graphics.pose().pushPose();
                graphics.pose().translate(slot.posX, slot.posY, 0);
                graphics.pose().scale(slot.scale, slot.scale, 1.0f);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));
                int argb = (slot.alpha << 24) | (slot.color & 0x00FFFFFF);
                graphics.drawString(font, slot.text.replaceAll("§[0-9a-fk-or]", ""), 0, 0, argb, false);
                graphics.pose().popPose();
            }
        }
    }
}