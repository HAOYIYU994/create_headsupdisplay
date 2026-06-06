package com.github.haoyiyu.create_headsupdisplay.hud;

import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.github.haoyiyu.create_headsupdisplay.client.DynamicTextureCache;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

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

        // 图片槽位
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

        // 雷达图槽位
        var radarSlots = ClientHudData.getRadarSlots();
        var radarTracks = ClientHudData.getRadarTracks();
        if (radarSlots != null && !radarSlots.isEmpty() && mc.player != null) {
            for (var slot : radarSlots) {
                renderRadarSlot(graphics, mc, slot, radarTracks);
            }
        }

        // 数据源槽位
        var slots = ClientHudData.getSlots();
        if (slots != null) {
            for (var slot : slots) {
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
                                int barWidth = 80, barHeight = 10;
                                graphics.fill(0, 0, barWidth, barHeight, 0xFF333333);
                                graphics.fill(0, 0, (int)(barWidth * percent), barHeight, 0xFF00FF00);
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

        // 静态文本槽位
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

    private static final ResourceLocation TEX_FILLER    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_bg_filler.png");
    private static final ResourceLocation TEX_CIRCLE    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_bg_circle.png");
    private static final ResourceLocation TEX_SWEEP     = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_sweep.png");
    private static final ResourceLocation TEX_PLAYER    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/player.png");
    private static final ResourceLocation TEX_ENTITY    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/entity_hitbox.png");
    private static final ResourceLocation TEX_CONTRAPTION = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/contraption_hitbox.png");
    private static final ResourceLocation TEX_PROJECTILE = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/projectile.png");

    private static final float ALPHA_BACKGROUND = 0.6f;
    private static final float ALPHA_GRID = 0.1f;
    private static final float ALPHA_SWEEP = 0.8f;
    private static final float TRACK_POSITION_SCALE = 0.75f;
    private static final int RADAR_TEXTURE_SIZE = 128;
    private static final int TRACK_TEXTURE_SIZE = 256;
    private static final int TRACK_MARKER_SOURCE_PX = 16;
    private static final int TRACK_MARKER_BASE_PX = 18;

    private static void renderRadarSlot(GuiGraphics gg, Minecraft mc, ClientHudData.RadarRenderData slot,
                                  List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> tracks) {
        // 优先使用雷达实际探测距离（自动同步），槽位配置作为后备
        float actualRadarRange = ClientHudData.getRadarGlobalRange();
        float range = actualRadarRange > 0 ? actualRadarRange : (slot.radarRange > 0 ? slot.radarRange : 50f);
        float globalAngle = ClientHudData.getRadarSweepAngle();
        double radarX = ClientHudData.getRadarX();
        double radarZ = ClientHudData.getRadarZ();

        gg.pose().pushPose();
        gg.pose().translate(slot.posX, slot.posY, 0);
        gg.pose().scale(slot.scale, slot.scale, 1.0f);
        gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));

        int uiSize = 128;
        float uiScale = uiSize / 320f;
        int margin = 0;
        int radarLeft = margin;
        int radarTop = margin;
        int radarSize = uiSize - margin * 2;
        int gridColor = 0x00CC00;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();


        // 网格
        int halfCells = 5;
        int totalCells = halfCells * 2;
        float spacing = radarSize / (float) totalCells;
        int ga = (int)(ALPHA_GRID * slot.alpha / 255f * 255);
        int gridArg = (ga << 24) | (gridColor & 0xFFFFFF);
        for (int i = 0; i <= totalCells; i++) {
            int x = radarLeft + Math.round(i * spacing);
            gg.fill(x, radarTop, x + 1, radarTop + radarSize, gridArg);
        }
        for (int i = 0; i <= totalCells; i++) {
            int y = radarTop + Math.round(i * spacing);
            gg.fill(radarLeft, y, radarLeft + radarSize, y + 1, gridArg);
        }
        int cx = radarLeft + radarSize / 2;
        int cy = radarTop + radarSize / 2;
        gg.fill(cx, radarTop, cx + 1, radarTop + radarSize, gridArg);
        gg.fill(radarLeft, cy, radarLeft + radarSize, cy + 1, gridArg);

        // 圆形黑色底
        mc.getTextureManager().getTexture(TEX_FILLER).setFilter(false, false);
        float aBg = ALPHA_BACKGROUND * slot.alpha / 255f;
        gg.setColor(0f, 0f, 0f, aBg);
        gg.blit(TEX_FILLER, radarLeft, radarTop, radarSize, radarSize, 0, 0, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE);
        gg.setColor(1f, 1f, 1f, 1f);

        // 圆形边框
        mc.getTextureManager().getTexture(TEX_CIRCLE).setFilter(false, false);
        float aCircle = ALPHA_BACKGROUND * 0.5f * slot.alpha / 255f;
        gg.setColor(0f, 1f, 0f, aCircle);
        gg.blit(TEX_CIRCLE, radarLeft, radarTop, radarSize, radarSize, 0, 0, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE);
        gg.setColor(1f, 1f, 1f, 1f);

        // 十字线
        int lineColor = (int)(slot.alpha * 0.4f) << 24 | (gridColor & 0xFFFFFF);
        gg.fill(cx, radarTop, cx + 1, radarTop + radarSize, lineColor);
        gg.fill(radarLeft, cy, radarLeft + radarSize, cy + 1, lineColor);

        // 扫描线
        mc.getTextureManager().getTexture(TEX_SWEEP).setFilter(false, false);
        float playerYaw = mc.player != null ? mc.player.getYRot() : 0f;
        float sweepAngle = (playerYaw + globalAngle) % 360f;
        if (sweepAngle < 0) sweepAngle += 360;
        gg.setColor(0f, 204f/255f, 0f, ALPHA_SWEEP * slot.alpha / 255f);
        gg.pose().pushPose();
        gg.pose().translate(cx, cy, 0);
        gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-sweepAngle));
        gg.pose().translate(-cx, -cy, 0);
        gg.blit(TEX_SWEEP, radarLeft, radarTop, radarSize, radarSize, 0, 0, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE, RADAR_TEXTURE_SIZE);
        gg.pose().popPose();
        gg.setColor(1f, 1f, 1f, 1f);

        // 目标标记
        if (tracks != null && !tracks.isEmpty()) {
            float rangeScale = Mth.clamp(50f / Math.max(range, 1f), 0.25f, 2f);
            int markerSize = Math.round(TRACK_MARKER_BASE_PX * uiScale * rangeScale);
            markerSize = Math.max(4, markerSize);
            float uvOff = (TRACK_TEXTURE_SIZE - TRACK_MARKER_SOURCE_PX) * 0.5f;
            mc.getTextureManager().getTexture(TEX_ENTITY).setFilter(false, false);
            mc.getTextureManager().getTexture(TEX_PLAYER).setFilter(false, false);
            mc.getTextureManager().getTexture(TEX_CONTRAPTION).setFilter(false, false);
            mc.getTextureManager().getTexture(TEX_PROJECTILE).setFilter(false, false);

            for (var track : tracks) {
                double relX = -(track.x() - radarX);
                double relZ = track.z() - radarZ;
                double dist = Math.sqrt(relX * relX + relZ * relZ);
                if (dist > range) continue;

                float xOff = (float)(relX / range) / 2f * TRACK_POSITION_SCALE;
                float zOff = (float)(relZ / range) / 2f * TRACK_POSITION_SCALE;

                if (Math.abs(xOff) > 0.5f || Math.abs(zOff) > 0.5f) continue;

                double rad = Math.toRadians(playerYaw);
                float rx = (float)(xOff * Math.cos(rad) - zOff * Math.sin(rad));
                float rz = (float)(xOff * Math.sin(rad) + zOff * Math.cos(rad));

                int px = radarLeft + Math.round((0.5f + rx) * radarSize);
                int pz = radarTop + Math.round((0.5f - rz) * radarSize);

                int col = getTrackColor(track.categoryOrdinal());
                float tr = ((col >> 16) & 0xFF) / 255f;
                float tg = ((col >> 8) & 0xFF) / 255f;
                float tb = (col & 0xFF) / 255f;
                ResourceLocation tex = switch (track.categoryOrdinal()) {
                    case 0 -> TEX_PLAYER;
                    case 5 -> TEX_CONTRAPTION;
                    case 4 -> TEX_PROJECTILE;
                    default -> TEX_ENTITY;
                };
                int sx = px - markerSize / 2;
                int sy = pz - markerSize / 2;
                gg.setColor(tr, tg, tb, slot.alpha / 255f);
                gg.blit(tex, sx, sy, markerSize, markerSize, uvOff, uvOff, TRACK_MARKER_SOURCE_PX, TRACK_MARKER_SOURCE_PX, TRACK_TEXTURE_SIZE, TRACK_TEXTURE_SIZE);

                // renderLabel — 玩家显示名字而非 UUID
                if (track.categoryOrdinal() == 0 && !track.id().isEmpty()) {
                    gg.setColor(1f, 1f, 1f, 1f);
                    String label = resolvePlayerName(mc, track.id());
                    gg.pose().pushPose();
                    gg.pose().translate(px, pz + Math.round(8 * uiScale), 0);
                    float ls = Math.max(0.5f, uiScale * 1.5f);
                    gg.pose().scale(ls, ls, 1f);
                    gg.drawCenteredString(mc.font, label, 0, 0, 0xFFFFFF);
                    gg.pose().popPose();
                }
            }
            gg.setColor(1f, 1f, 1f, 1f);
        }

        RenderSystem.disableBlend();
        gg.pose().popPose();
    }

    private static void drawCircleOutline(GuiGraphics g, int cx, int cy, int r, int color) {
        int x = 0, y = r, d = 3 - 2 * r;
        while (x <= y) {
            g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
            g.fill(cx - x, cy + y, cx - x + 1, cy + y + 1, color);
            g.fill(cx + x, cy - y, cx + x + 1, cy - y + 1, color);
            g.fill(cx - x, cy - y, cx - x + 1, cy - y + 1, color);
            g.fill(cx + y, cy + x, cx + y + 1, cy + x + 1, color);
            g.fill(cx - y, cy + x, cx - y + 1, cy + x + 1, color);
            g.fill(cx + y, cy - x, cx + y + 1, cy - x + 1, color);
            g.fill(cx - y, cy - x, cx - y + 1, cy - x + 1, color);
            if (d < 0) d += 4 * x + 6;
            else { d += 4 * (x - y) + 10; y--; }
            x++;
        }
    }

    /** 尝试将 track id（可能是 UUID）解析为玩家名 */
    private static String resolvePlayerName(Minecraft mc, String trackId) {
        if (mc.level == null) return trackId;
        for (var player : mc.level.players()) {
            String uuid = player.getStringUUID();
            String name = player.getName().getString();
            if (uuid.equals(trackId) || name.equals(trackId)) return name;
        }
        // 不是 UUID 也不是名字，直接截断显示
        return trackId.length() > 12 ? trackId.substring(0, 12) : trackId;
    }

    private static int getTrackColor(int category) {
        return switch (category) {
            case 0 -> 0x00FF00;
            case 1, 3 -> 0xFFFF00;
            case 2 -> 0xFF0000;
            case 4 -> 0xFF8800;
            case 5 -> 0x4488FF;
            case 6 -> 0xCCCCCC;
            default -> 0x888888;
        };
    }
}
