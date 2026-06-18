package com.github.haoyiyu.create_headsupdisplay.hud;

import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.github.haoyiyu.create_headsupdisplay.client.DynamicTextureCache;
import com.github.haoyiyu.create_headsupdisplay.item.HeadMountDisplayItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.*;

@EventBusSubscriber(Dist.CLIENT)
public class HudOverlayRenderer {

    // ========== 凝滞器：世界锁定 + 深度视差 ==========

    /** 世界空间锚点方向 (yaw, pitch)，按元素标识符索引 */
    private static final Map<String, float[]> frozenWorldAnchors = new HashMap<>();
    private static final float FROZEN_DEFAULT_DEPTH = 5.0f; // 默认深度（方块）
    private static final float X_SENSITIVITY = 0.75f; // X 轴世界锁定强度（1.0=完整）

    /** 上一帧相机位置，用于计算视差位移 */
    private static double lastCamX = Double.NaN, lastCamY, lastCamZ;
    private static float lastFov = -1;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack helmet = player.getInventory().armor.get(3);
        if (!(helmet.getItem() instanceof HeadMountDisplayItem)) return;
        BlockPos boundTerminal = HeadMountDisplayItem.getBoundTerminalPos(helmet);
        if (boundTerminal == null) {
            // 脱下头盔时清理锚点，避免多终端串数据
            frozenWorldAnchors.clear();
            lastCamX = Double.NaN;
            return;
        }

        // 读取终端朝向作为冻结图层的参考方向（yaw 锚点以此为准，而非玩家朝向）
        float terminalYaw = player.getYRot(); // fallback
        float terminalPitch = 0f;
        float[] physics = null;
        if (mc.level != null) {
            var state = mc.level.getBlockState(boundTerminal);
            boolean hasFacing = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            float rawFacingYaw = hasFacing
                    ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).toYRot()
                    : terminalYaw;
            float cachedYaw = SablePhysicsHelper.getOrCacheFacingYaw(boundTerminal, rawFacingYaw, hasFacing);
            terminalYaw = cachedYaw;
            // 尝试从 sable 物理引擎获取真实世界朝向（航空学联动）
            physics = SablePhysicsHelper.tryGetPhysicsOrientation(mc.level, boundTerminal, cachedYaw, player);
            // 玩家坐在物理体坐垫上 → 用本地坐标，物理旋转在本地空间不体现
            boolean playerOnBody = player.getVehicle() != null && physics != null;
            if (physics != null && !playerOnBody) {
                terminalYaw = physics[0];
                terminalPitch = physics[1];
            } else if (playerOnBody) {
                // 本地模式也减 90°（SubLevel 坐标与终端 FACING 的固定偏差）
                terminalYaw = cachedYaw - 90f;
            }
        }

        GuiGraphics g = event.getGuiGraphics();
        var font = mc.font;

        // ================================================================
        //  帧初始化：相机参数 & 视差
        // ================================================================
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        // 物理体上：本地空间不需要四元数旋转
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float fov = mc.options.fov().get().floatValue();

        // 焦距（像素）：屏幕半宽 / tan(fov/2)
        float halfFovRad = (float) Math.toRadians(fov / 2.0);
        float focalPx = (screenW / 2.0f) / (float) Math.tan(halfFovRad);

        // 相机位移（用于深度视差）
        double camDX = 0, camDY = 0, camDZ = 0;
        if (!Double.isNaN(lastCamX) && lastFov == fov) {
            camDX = camPos.x - lastCamX;
            camDY = camPos.y - lastCamY;
            camDZ = camPos.z - lastCamZ;
        }
        lastCamX = camPos.x; lastCamY = camPos.y; lastCamZ = camPos.z;
        lastFov = fov;

        // 相机局部坐标轴
        float yawRad = (float) Math.toRadians(playerYaw);
        float pitchRad = (float) Math.toRadians(playerPitch);
        float cosY = Mth.cos(-yawRad + (float) Math.PI / 2f);
        float sinY = Mth.sin(-yawRad + (float) Math.PI / 2f);
        // 右向量（忽略 pitch 抬头时右向量仍水平）
        Vec3 camRight = new Vec3(cosY, 0, sinY);
        Vec3 camUp = new Vec3(
                -Mth.sin(-yawRad + (float) Math.PI / 2f) * Mth.sin(pitchRad),
                Mth.cos(pitchRad),
                Mth.cos(-yawRad + (float) Math.PI / 2f) * Mth.sin(pitchRad)
        );
        double latMove = camDX * camRight.x + camDY * camRight.y + camDZ * camRight.z;
        double upMove = camDX * camUp.x + camDY * camUp.y + camDZ * camUp.z;

        // ================================================================
        //  图片
        // ================================================================
        var images = ClientHudData.getImagesFor(boundTerminal);
        if (images != null) {
            RenderSystem.enableBlend();
            for (int idx = 0; idx < images.size(); idx++) {
                var img = images.get(idx);
                var aRes = new com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.Result();
                if (!img.animations.isEmpty()) {
                    com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.evaluate(
                        img.animations, img.fileName, aRes, new AnimSlotRef(img));
                }
                int aix = aRes.posX != null ? aRes.posX.intValue() : img.posX;
                int aiy = aRes.posY != null ? aRes.posY.intValue() : img.posY;
                float asc = aRes.scale != null ? aRes.scale : img.scale;
                float arot = aRes.rotation != null ? aRes.rotation : img.rotation;
                int aalpha = aRes.alpha != null ? aRes.alpha : img.alpha;

                int ix, iy;
                if (img.frozen) {
                    String key = "img:" + img.imageId;
                    float[] wp = getOrCreateWorldAnchor(key, aix, aiy,
                            terminalYaw, terminalPitch, screenW, screenH, focalPx, FROZEN_DEFAULT_DEPTH);
                    ix = (int) projectWorldToScreenX(wp[0], playerYaw, screenW, focalPx,
                            latMove, wp[2], focalPx);
                    float wy = projectWorldToScreenYFull(wp[1], playerPitch, screenH, focalPx, upMove, wp[2], focalPx);
                    iy = (int)(aiy + (wy - aiy) * Y_BLEND);
                    g.pose().pushPose();
                    g.pose().translate(ix, iy, 0);
                    g.pose().scale(asc, asc, 1f);
                    g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(arot));
                } else {
                    g.pose().pushPose();
                    g.pose().translate(aix, aiy, 0);
                    g.pose().scale(asc, asc, 1f);
                    g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(arot));
                }
                RenderSystem.setShaderColor(1f, 1f, 1f, aalpha / 255f);
                ResourceLocation tex = DynamicTextureCache.getOrCreate(img.imageId, img.imageData);
                if (tex != null) {
                    int iw = DynamicTextureCache.getWidth(img.imageId);
                    int ih = DynamicTextureCache.getHeight(img.imageId);
                    if (iw > 0 && ih > 0) {
                        RenderSystem.setShaderColor(1f, 1f, 1f, aalpha / 255f);
                        g.blit(tex, 0, 0, 0, 0, iw, ih, iw, ih);
                    }
                }
                g.pose().popPose();
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        // ================================================================
        //  雷达
        // ================================================================
        var radars = ClientHudData.getRadarSlotsFor(boundTerminal);
        var tracks = ClientHudData.getRadarTracks();
        if (radars != null && mc.player != null) {
            for (int idx = 0; idx < radars.size(); idx++) {
                var slot = radars.get(idx);
                var aRes = new com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.Result();
                if (!slot.animations.isEmpty()) {
                    com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.evaluate(
                        slot.animations, "", aRes, new AnimSlotRef(slot));
                }
                int arx = aRes.posX != null ? aRes.posX.intValue() : slot.posX;
                int ary = aRes.posY != null ? aRes.posY.intValue() : slot.posY;
                float asc = aRes.scale != null ? aRes.scale : slot.scale;
                float arot = aRes.rotation != null ? aRes.rotation : slot.rotation;
                int aalpha = aRes.alpha != null ? aRes.alpha : slot.alpha;
                // Apply animated values to a temporary slot copy
                var animSlot = new ClientHudData.RadarRenderData(arx, ary, asc, arot, aalpha, slot.radarRange, slot.layerIndex, slot.frozen, slot.animations);

                int rx, ry;
                if (slot.frozen) {
                    String key = "radar:" + idx;
                    float[] wp = getOrCreateWorldAnchor(key, arx, ary,
                            terminalYaw, terminalPitch, screenW, screenH, focalPx, FROZEN_DEFAULT_DEPTH);
                    rx = (int) projectWorldToScreenX(wp[0], playerYaw, screenW, focalPx,
                            latMove, wp[2], focalPx);
                    float wyr = projectWorldToScreenYFull(wp[1], playerPitch, screenH, focalPx, upMove, wp[2], focalPx);
                    ry = (int)(ary + (wyr - ary) * Y_BLEND);
                } else {
                    rx = arx; ry = ary;
                }
                renderRadarSlot(g, mc, animSlot, tracks, rx, ry);
            }
        }

        // ================================================================
        //  数据源槽位
        // ================================================================
        var slots = ClientHudData.getSlotsFor(boundTerminal);
        if (slots != null) {
            // 按名称索引所有源的值，供动画触发源查找
            var nameToValue = new java.util.HashMap<String, String>();
            for (var s : slots) {
                for (int si = 0; si < s.sourceNames.size() && si < s.dataValues.size(); si++)
                    nameToValue.put(s.sourceNames.get(si), s.dataValues.get(si));
            }

            for (var slot : slots) {
                // 解析动画触发源：triggerSourceName 非空时取对应源的值，否则取自身
                String triggerData = slot.text;
                for (var anim : slot.animations) {
                    if (!anim.triggerSourceName.isEmpty())
                        triggerData = nameToValue.getOrDefault(anim.triggerSourceName, slot.text);
                }
                // 动画求值
                var animRes = new com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.Result();
                com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.evaluate(slot.animations, triggerData, animRes,
                    new com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.SlotRef() {
                        public float getPosX() { return (float)slot.posX; } public void setPosX(float v) {}
                        public float getPosY() { return (float)slot.posY; } public void setPosY(float v) {}
                        public float getScale() { return slot.scale; } public void setScale(float v) {}
                        public float getRotation() { return slot.rotation; } public void setRotation(float v) {}
                        public int getColor() { return slot.color; } public void setColor(int v) {}
                        public int getAlpha() { return slot.alpha; } public void setAlpha(int v) {}
                    });
                float animPosX = animRes.posX != null ? animRes.posX : slot.posX;
                float animPosY = animRes.posY != null ? animRes.posY : slot.posY;
                float animScale = animRes.scale != null ? animRes.scale : slot.scale;
                float animRotation = animRes.rotation != null ? animRes.rotation : slot.rotation;
                int animColor = animRes.color != null ? animRes.color : slot.color;
                int animAlpha = animRes.alpha != null ? animRes.alpha : slot.alpha;

                int sx, sy;
                if (slot.frozen) {
                    String key = "slot:" + slot.sourcePos.toShortString();
                    float[] wp = getOrCreateWorldAnchor(key, (int)animPosX, (int)animPosY,
                            terminalYaw, terminalPitch, screenW, screenH, focalPx, FROZEN_DEFAULT_DEPTH);
                    sx = (int) projectWorldToScreenX(wp[0], playerYaw, screenW, focalPx,
                            latMove, wp[2], focalPx);
                    float wys = projectWorldToScreenYFull(wp[1], playerPitch, screenH, focalPx, upMove, wp[2], focalPx);
                    sy = (int)(animPosY + (wys - animPosY) * Y_BLEND);
                } else {
                    sx = (int)animPosX; sy = (int)animPosY;
                }
                g.pose().pushPose();
                g.pose().translate(sx, sy, 0);
                g.pose().scale(animScale, animScale, 1f);
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(animRotation));
                int tc = (animAlpha << 24) | (animColor & 0xFFFFFF);
                String text = slot.text.replaceAll("§[0-9a-fk-or]", "");
                var mode = slot.displayModeId != null
                    ? com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.get(slot.displayModeId)
                    : com.github.haoyiyu.create_headsupdisplay.api.DisplayModeRegistry.get(slot.displayMode);
                if (mode != null) {
                    mode.render(g, font, slot.dataValues, slot.modeConfig,
                        mode.getDefaultWidth(), mode.getDefaultHeight());
                } else {
                    g.drawString(font, text, 0, 0, tc, true);
                }
                g.pose().popPose();
            }
        }

        // ================================================================
        //  静态文本
        // ================================================================
        var sts = ClientHudData.getStaticTextsFor(boundTerminal);
        if (sts != null) {
            for (int idx = 0; idx < sts.size(); idx++) {
                var slot = sts.get(idx);
                // Evaluate animations
                var aRes = new com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.Result();
                if (!slot.animations.isEmpty()) {
                    com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.evaluate(
                        slot.animations, slot.text, aRes, new AnimSlotRef(slot));
                }
                int atx = aRes.posX != null ? aRes.posX.intValue() : slot.posX;
                int aty = aRes.posY != null ? aRes.posY.intValue() : slot.posY;
                float asc = aRes.scale != null ? aRes.scale : slot.scale;
                float arot = aRes.rotation != null ? aRes.rotation : slot.rotation;
                int acolor = aRes.color != null ? aRes.color : slot.color;
                int aalpha = aRes.alpha != null ? aRes.alpha : slot.alpha;

                int tx, ty;
                if (slot.frozen) {
                    String key = "text:" + slot.text.hashCode() + ":" + slot.layerIndex;
                    float[] wp = getOrCreateWorldAnchor(key, atx, aty,
                            terminalYaw, terminalPitch, screenW, screenH, focalPx, FROZEN_DEFAULT_DEPTH);
                    tx = (int) projectWorldToScreenX(wp[0], playerYaw, screenW, focalPx,
                            latMove, wp[2], focalPx);
                    float wyt = projectWorldToScreenYFull(wp[1], playerPitch, screenH, focalPx, upMove, wp[2], focalPx);
                    ty = (int)(aty + (wyt - aty) * Y_BLEND);
                } else {
                    tx = atx; ty = aty;
                }
                g.pose().pushPose();
                g.pose().translate(tx, ty, 0);
                g.pose().scale(asc, asc, 1f);
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(arot));
                int argb = (aalpha << 24) | (acolor & 0x00FFFFFF);
                g.drawString(font, slot.text.replaceAll("§[0-9a-fk-or]", ""), 0, 0, argb, false);
                g.pose().popPose();
            }
        }
    }

    // ================================================================
    //  世界锚点 & 投影工具方法
    // ================================================================

    /**
     * 获取或创建元素的世界空间锚点。
     * 首次见到的元素：根据玩家当前朝向 + 元素屏幕偏移量计算世界锚点方向。
     * 已存在的元素：直接返回存储的锚点。
     *
     * @return float[4]: [worldYaw, worldPitch, posX, depth]
     */
    private static float[] getOrCreateWorldAnchor(String key, int posX, int posY,
                                                   float referenceYaw, float referencePitch,
                                                   int screenW, int screenH,
                                                   float focalPx, float depth) {
        float[] stored = frozenWorldAnchors.get(key);
        // 检查 posX/posY 是否变化（用户在配置界面移动了元素）
        if (stored != null && (int) stored[2] == posX && (int) stored[3] == posY) {
            // 锚点存的是角偏移量，每帧用当前 reference 实时计算世界角度（物理体旋转后自动跟随）
            float worldYaw = referenceYaw + stored[0];
            float worldPitch = Mth.clamp(referencePitch + stored[1], -90f, 90f);
            return new float[]{worldYaw, worldPitch, depth};
        }

        // 新锚点：posX,posY 表示相对屏幕中心的偏移
        float fromCenterX = posX - screenW / 2.0f;
        float fromCenterY = posY - screenH / 2.0f;

        // 屏幕偏移 → 角度偏移。锚点存 1/sensitivity 倍，投影时再乘 sensitivity 还原初始位置
        float angleOffsetYaw = (float) Math.toDegrees(Math.atan2(fromCenterX, focalPx)) / X_SENSITIVITY;
        float angleOffsetPitch = (float) Math.toDegrees(Math.atan2(fromCenterY, focalPx)); // Y 轴：屏幕上方为负（-pitch=抬头），下方为正

        frozenWorldAnchors.put(key, new float[]{angleOffsetYaw, angleOffsetPitch, posX, posY});

        float worldYaw = referenceYaw + angleOffsetYaw;
        float worldPitch = Mth.clamp(referencePitch + angleOffsetPitch, -90f, 90f);
        return new float[]{worldYaw, worldPitch, depth};
    }

    /** 将世界锚点 yaw 投影到当前屏幕 X 坐标，含深度视差 */
    private static float projectWorldToScreenX(float worldYaw, float playerYaw,
                                                int screenW, float focalPx,
                                                double camLatMove, float depth, float fpx) {
        float deltaYaw = Mth.wrapDegrees(worldYaw - playerYaw) * X_SENSITIVITY;
        float screenX = screenW / 2.0f + (float) Math.tan(Math.toRadians(deltaYaw)) * focalPx;
        // 深度视差：相机侧移造成屏幕偏移
        if (depth > 0.1f) {
            screenX -= (float) (camLatMove / depth) * fpx;
        }
        return screenX;
    }

    /** 将世界锚点 pitch 投影到当前屏幕 Y 坐标（完整世界锁定），含深度视差 */
    private static float projectWorldToScreenYFull(float worldPitch, float playerPitch,
                                                int screenH, float focalPx,
                                                double camUpMove, float depth, float fpx) {
        float deltaPitch = playerPitch - worldPitch;
        float screenY = screenH / 2.0f - (float) Math.tan(Math.toRadians(deltaPitch)) * focalPx;
        if (depth > 0.1f) {
            screenY += (float) (camUpMove / depth) * fpx;
        }
        return screenY;
    }

    /** Y 轴世界锁定混合比例：50% 跟随世界角度，50% 保持原位 */
    private static final float Y_BLEND = 0.65f;

    // ================================================================
    //  雷达渲染
    // ================================================================

    private static final ResourceLocation TEX_FILLER = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_bg_filler.png");
    private static final ResourceLocation TEX_CIRCLE = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_bg_circle.png");
    private static final ResourceLocation TEX_SWEEP = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_sweep.png");
    private static final ResourceLocation TEX_PLAYER = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/player.png");
    private static final ResourceLocation TEX_ENTITY = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/entity_hitbox.png");
    private static final ResourceLocation TEX_CONTRAPTION = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/contraption_hitbox.png");
    private static final ResourceLocation TEX_PROJECTILE = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/projectile.png");

    private static void renderRadarSlot(GuiGraphics gg, Minecraft mc,
            ClientHudData.RadarRenderData slot,
            List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> tracks,
            int renderX, int renderY) {
        float range = ClientHudData.getRadarGlobalRange();
        if (range <= 0) range = slot.radarRange > 0 ? slot.radarRange : 50f;
        float gAngle = ClientHudData.getRadarSweepAngle();
        double rX = ClientHudData.getRadarX(), rZ = ClientHudData.getRadarZ();

        gg.pose().pushPose();
        gg.pose().translate(renderX, renderY, 0);
        gg.pose().scale(slot.scale, slot.scale, 1f);
        gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot.rotation));

        int ui = 128, m = 0, rl = m, rt = m, rs = ui, gc = 0x00CC00;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int h = 5, t = h * 2;
        float sp = rs / (float) t;
        int ga = (int) (0.1f * slot.alpha / 255f * 255);
        int gr = (ga << 24) | (gc & 0xFFFFFF);
        for (int i = 0; i <= t; i++) {
            int x = rl + Math.round(i * sp);
            gg.fill(x, rt, x + 1, rt + rs, gr);
        }
        for (int i = 0; i <= t; i++) {
            int y = rt + Math.round(i * sp);
            gg.fill(rl, y, rl + rs, y + 1, gr);
        }
        int cx = rl + rs / 2, cy = rt + rs / 2;
        gg.fill(cx, rt, cx + 1, rt + rs, gr);
        gg.fill(rl, cy, rl + rs, cy + 1, gr);

        mc.getTextureManager().getTexture(TEX_FILLER).setFilter(false, false);
        gg.setColor(0, 0, 0, 0.6f * slot.alpha / 255f);
        gg.blit(TEX_FILLER, rl, rt, rs, rs, 0, 0, 128, 128, 128, 128);
        gg.setColor(1, 1, 1, 1);

        mc.getTextureManager().getTexture(TEX_CIRCLE).setFilter(false, false);
        gg.setColor(0, 1, 0, 0.3f * slot.alpha / 255f);
        gg.blit(TEX_CIRCLE, rl, rt, rs, rs, 0, 0, 128, 128, 128, 128);
        gg.setColor(1, 1, 1, 1);

        int lc = (int) (slot.alpha * 0.4f) << 24 | (gc & 0xFFFFFF);
        gg.fill(cx, rt, cx + 1, rt + rs, lc);
        gg.fill(rl, cy, rl + rs, cy + 1, lc);

        mc.getTextureManager().getTexture(TEX_SWEEP).setFilter(false, false);
        float pY = mc.player != null ? mc.player.getYRot() : 0;
        float sw = (pY + gAngle) % 360;
        if (sw < 0) sw += 360;
        gg.setColor(0, 0.8f, 0, 0.8f * slot.alpha / 255f);
        gg.pose().pushPose();
        gg.pose().translate(cx, cy, 0);
        gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-sw));
        gg.pose().translate(-cx, -cy, 0);
        gg.blit(TEX_SWEEP, rl, rt, rs, rs, 0, 0, 128, 128, 128, 128);
        gg.pose().popPose();
        gg.setColor(1, 1, 1, 1);

        if (tracks != null && !tracks.isEmpty()) {
            float rs2 = Mth.clamp(50f / Math.max(range, 1f), 0.25f, 2f);
            int ms = Math.round(18 * 0.4f * rs2);
            ms = Math.max(4, ms);
            float uvo = (256 - 16) * 0.5f;
            mc.getTextureManager().getTexture(TEX_ENTITY).setFilter(false, false);
            mc.getTextureManager().getTexture(TEX_PLAYER).setFilter(false, false);
            mc.getTextureManager().getTexture(TEX_CONTRAPTION).setFilter(false, false);
            mc.getTextureManager().getTexture(TEX_PROJECTILE).setFilter(false, false);
            for (var track : tracks) {
                double rx = -(track.x() - rX);
                double rz = track.z() - rZ;
                double dist = Math.sqrt(rx * rx + rz * rz);
                if (dist > range) continue;
                float xo = (float) (rx / range) / 2f * 0.75f;
                float zo = (float) (rz / range) / 2f * 0.75f;
                if (Math.abs(xo) > 0.5f || Math.abs(zo) > 0.5f) continue;
                double rad = Math.toRadians(pY);
                float rxx = (float) (xo * Math.cos(rad) - zo * Math.sin(rad));
                float rzz = (float) (xo * Math.sin(rad) + zo * Math.cos(rad));
                int px = rl + Math.round((0.5f + rxx) * rs);
                int py = rt + Math.round((0.5f - rzz) * rs);
                int col = getTC(track.categoryOrdinal());
                float tr = ((col >> 16) & 0xFF) / 255f;
                float tg = ((col >> 8) & 0xFF) / 255f;
                float tb = (col & 0xFF) / 255f;
                ResourceLocation tx = switch (track.categoryOrdinal()) {
                    case 0 -> TEX_PLAYER;
                    case 5 -> TEX_CONTRAPTION;
                    case 4 -> TEX_PROJECTILE;
                    default -> TEX_ENTITY;
                };
                gg.setColor(tr, tg, tb, slot.alpha / 255f);
                gg.blit(tx, px - ms / 2, py - ms / 2, ms, ms, uvo, uvo, 16, 16, 256, 256);
                if (track.categoryOrdinal() == 0 && !track.id().isEmpty()) {
                    gg.setColor(1, 1, 1, 1);
                    String lb = resolveName(mc, track.id());
                    gg.pose().pushPose();
                    gg.pose().translate(px, py + Math.round(8 * 0.4f), 0);
                    float ls = Math.max(0.5f, 0.4f * 1.5f);
                    gg.pose().scale(ls, ls, 1);
                    gg.drawCenteredString(mc.font, lb, 0, 0, 0xFFFFFF);
                    gg.pose().popPose();
                }
            }
            gg.setColor(1, 1, 1, 1);
        }
        RenderSystem.disableBlend();
        gg.pose().popPose();
    }

    private static String resolveName(Minecraft mc, String id) {
        if (mc.level == null) return id;
        for (var p : mc.level.players()) {
            if (p.getStringUUID().equals(id) || p.getName().getString().equals(id))
                return p.getName().getString();
        }
        return id.length() > 12 ? id.substring(0, 12) : id;
    }

    /** Bridge any render-data object to AnimationEvaluator.SlotRef */
    private static class AnimSlotRef implements com.github.haoyiyu.create_headsupdisplay.client.AnimationEvaluator.SlotRef {
        private final Object data;
        AnimSlotRef(Object data) { this.data = data; }
        public float getPosX() { return (float)(data instanceof ClientHudData.SlotRenderData s ? s.posX : data instanceof ClientHudData.StaticTextRenderData t ? t.posX : data instanceof ClientHudData.ImageRenderData i ? i.posX : data instanceof ClientHudData.RadarRenderData r ? r.posX : 0); }
        public float getPosY() { return (float)(data instanceof ClientHudData.SlotRenderData s ? s.posY : data instanceof ClientHudData.StaticTextRenderData t ? t.posY : data instanceof ClientHudData.ImageRenderData i ? i.posY : data instanceof ClientHudData.RadarRenderData r ? r.posY : 0); }
        public float getScale() { return data instanceof ClientHudData.SlotRenderData s ? s.scale : data instanceof ClientHudData.StaticTextRenderData t ? t.scale : data instanceof ClientHudData.ImageRenderData i ? i.scale : data instanceof ClientHudData.RadarRenderData r ? r.scale : 1f; }
        public float getRotation() { return data instanceof ClientHudData.SlotRenderData s ? s.rotation : data instanceof ClientHudData.StaticTextRenderData t ? t.rotation : data instanceof ClientHudData.ImageRenderData i ? i.rotation : data instanceof ClientHudData.RadarRenderData r ? r.rotation : 0f; }
        public int getColor() { return data instanceof ClientHudData.SlotRenderData s ? s.color : data instanceof ClientHudData.StaticTextRenderData t ? t.color : 0xFFFFFF; }
        public int getAlpha() { return data instanceof ClientHudData.SlotRenderData s ? s.alpha : data instanceof ClientHudData.StaticTextRenderData t ? t.alpha : data instanceof ClientHudData.ImageRenderData i ? i.alpha : data instanceof ClientHudData.RadarRenderData r ? r.alpha : 255; }
        public void setPosX(float v) {} public void setPosY(float v) {} public void setScale(float v) {}
        public void setRotation(float v) {} public void setColor(int v) {} public void setAlpha(int v) {}
    }

    private static int getTC(int c) {
        return switch (c) {
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
