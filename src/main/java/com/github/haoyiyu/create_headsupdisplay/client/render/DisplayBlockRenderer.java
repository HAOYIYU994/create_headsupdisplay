package com.github.haoyiyu.create_headsupdisplay.client.render;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayBlock;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.github.haoyiyu.create_headsupdisplay.client.DynamicTextureCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class DisplayBlockRenderer extends SmartBlockEntityRenderer<DisplayBlockEntity> {

    private static final int EDITOR_W = 427;
    private static final int EDITOR_H = 240;

    public DisplayBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected void renderSafe(DisplayBlockEntity dbe, float partialTick, PoseStack pose,
                              MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(dbe, partialTick, pose, buffer, light, overlay);

        if (dbe.getLevel() == null) return;

        Direction facing = dbe.getBlockState().getValue(DisplayBlock.FACING);
        BlockPos ctrlPos = dbe.getControllerPos();

        DisplayBlockEntity ctrl;
        if (dbe.isController()) {
            ctrl = dbe;
        } else {
            if (!(dbe.getLevel().getBlockEntity(ctrlPos) instanceof DisplayBlockEntity controller)) return;
            ctrl = controller;
        }

        int panelW = Math.max(1, ctrl.getPanelWidth());
        int panelH = Math.max(1, ctrl.getPanelHeight());

        BlockPos termPos = ctrl.getBoundTerminal();
        if (termPos == null) return;

        var slots = ClientHudData.getSlotsFor(termPos);
        var staticTexts = ClientHudData.getStaticTextsFor(termPos);

        Font font = Minecraft.getInstance().font;

        int i = dbe.getBlockPos().getY() - ctrlPos.getY();
        int j = horizontalIndex(dbe.getBlockPos(), ctrlPos, facing);

        float panelPX = panelW * 32f;
        float panelPY = panelH * 32f;
        float scaleX = panelPX / EDITOR_W;
        float scaleY = panelPY / EDITOR_H;

        // --- 变换设置（参照 FlapDisplayRenderer） ---
        pose.pushPose();
        TransformStack.of(pose)
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .uncenter()
                .translate(0, 0, -0.0667f);

        pose.translate(0, 1, 1);
        pose.scale(1f / 32f, 1f / 32f, 1f / 32f);
        pose.scale(1, -1, 1);
        pose.translate(0, 0, 0.5f);

        int offX = (panelW - 1 - j) * 32;
        int offY = (panelH - 1 - i) * 32;
        pose.translate(-offX, -offY, 0);

        float regionLeft = offX, regionRight = offX + 32;
        float regionTop = offY, regionBottom = offY + 32;

        int renderedCount = 0;

        // --- 渲染雷达图（底层） ---
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        var radarSlots = ClientHudData.getRadarSlotsFor(termPos);
        var radarTracks = ClientHudData.getRadarTracks();
        if (radarSlots != null && !radarSlots.isEmpty()) {
            for (var slot : radarSlots) {
                float dx = slot.posX * scaleX;
                float dy = slot.posY * scaleY;
                if (inRegion(dx + 16, dy + 16, regionLeft - 32, regionRight + 32, regionTop - 32, regionBottom + 32))
                    renderRadarOnPanel(pose, font, buffer, slot, radarTracks, dx, dy, light, facing);
            }
        }

        // --- 渲染图片（底层） ---
        var images = ClientHudData.getImagesFor(termPos);
        if (images != null && !images.isEmpty()) {
            for (var img : images) {
                float dx = img.posX * scaleX;
                float dy = img.posY * scaleY;
                if (!inRegion(dx + 8, dy + 8, regionLeft - 32, regionRight + 32, regionTop - 32, regionBottom + 32))
                    continue;
                ResourceLocation tex = DynamicTextureCache.getOrCreate(img.imageId, img.imageData);
                if (tex != null) {
                    int iw = DynamicTextureCache.getWidth(img.imageId);
                    int ih = DynamicTextureCache.getHeight(img.imageId);
                    if (iw > 0 && ih > 0) {
                        renderImageOnPanel(pose, tex, dx, dy, img.scale, img.rotation, iw, ih, img.alpha);
                        renderedCount++;
                    }
                }
            }
        }

        // 关闭深度测试，开始字体渲染
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        font.drawInBatch(" ", 0, 0, 0x00000000, false, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);

        // --- 渲染动态槽位（顶层） ---
        if (slots != null) {
            for (var slot : slots) {
                float dx = slot.posX * scaleX;
                float dy = slot.posY * scaleY;
                if (!inRegion(dx, dy, regionLeft, regionRight, regionTop, regionBottom)) continue;

                renderText(pose, buffer, font, slot.text, dx, dy, slot.scale, slot.color, slot.alpha, light);
                renderedCount++;
            }
        }

        // --- 渲染静态文本（顶层） ---
        if (staticTexts != null) {
            for (var st : staticTexts) {
                float dx = st.posX * scaleX;
                float dy = st.posY * scaleY;
                if (!inRegion(dx, dy, regionLeft, regionRight, regionTop, regionBottom)) continue;

                renderText(pose, buffer, font, st.text, dx, dy, st.scale, st.color, st.alpha, light);
                renderedCount++;
            }
        }

        // 结束批处理
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }

        pose.popPose();
    }

    private static final ResourceLocation TEX_FILLER    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_bg_filler.png");
    private static final ResourceLocation TEX_CIRCLE    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_bg_circle.png");
    private static final ResourceLocation TEX_SWEEP     = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/radar_sweep.png");
    private static final ResourceLocation TEX_PLAYER    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/player.png");
    private static final ResourceLocation TEX_ENTITY    = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/entity_hitbox.png");
    private static final ResourceLocation TEX_CONTRAPTION = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/contraption_hitbox.png");
    private static final ResourceLocation TEX_PROJECTILE = ResourceLocation.fromNamespaceAndPath("create_radar", "textures/monitor_sprite/projectile.png");
    private static final int RADAR_SIZE = 24; // 面板上的雷达显示尺寸

    private void renderRadarOnPanel(PoseStack pose, Font font, MultiBufferSource buffer,
                                     ClientHudData.RadarRenderData slot,
                                     java.util.List<com.github.haoyiyu.create_headsupdisplay.network.SyncRadarDataPayload.RadarTrackEntry> tracks,
                                     float dx, float dy, int light, Direction facing) {
        float baseRange = ClientHudData.getRadarGlobalRange();
        float range = baseRange > 0 ? baseRange : (slot.radarRange > 0 ? slot.radarRange : 50f);
        double rX = ClientHudData.getRadarX();
        double rZ = ClientHudData.getRadarZ();
        float globalAngle = ClientHudData.getRadarSweepAngle();
        // 雷达方向跟随显示器方块朝向
        float faceAngle = AngleHelper.horizontalAngle(facing) + 180f;

        float s = Math.max(0.1f, slot.scale);
        float size = RADAR_SIZE * s;
        float half = size / 2f;
        float cx = dx + half;
        float cy = dy + half;

        pose.pushPose();

        // 圆形黑底
        RenderSystem.setShaderTexture(0, TEX_FILLER);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0f, 0f, 0f, 0.6f * slot.alpha / 255f);
        drawTexturedQuad(pose, dx, dy, size, size);

        // 圆形绿边框
        RenderSystem.setShaderTexture(0, TEX_CIRCLE);
        RenderSystem.setShaderColor(0f, 1f, 0f, 0.3f * slot.alpha / 255f);
        drawTexturedQuad(pose, dx, dy, size, size);

        // 扫描线
        float sweepAngle = (globalAngle - faceAngle) % 360f;
        if (sweepAngle < 0) sweepAngle += 360;
        double sweepRad = Math.toRadians(sweepAngle);
        float sweepX = cx + half * (float)Math.sin(sweepRad);
        float sweepY = cy - half * (float)Math.cos(sweepRad);
        RenderSystem.setShaderTexture(0, TEX_SWEEP);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(0f, 0.8f, 0f, 0.3f * slot.alpha / 255f);
        pose.pushPose();
        pose.translate(cx, cy, 0.005f);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-sweepAngle));
        pose.translate(-cx, -cy, 0);
        drawTexturedQuad(pose, dx, dy, size, size);
        pose.popPose();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 目标标记
        if (tracks != null && !tracks.isEmpty()) {
            float trackSize = Math.max(2f, 4f * s);
            for (var track : tracks) {
                double relX = -(track.x() - rX);
                double relZ = track.z() - rZ;
                double dist = Math.sqrt(relX * relX + relZ * relZ);
                if (dist > range) continue;

                float xOff = (float)(relX / range) / 2f * 0.75f;
                float zOff = (float)(relZ / range) / 2f * 0.75f;
                if (Math.abs(xOff) > 0.5f || Math.abs(zOff) > 0.5f) continue;

                // 按方块朝向旋转
                double rad = Math.toRadians(-faceAngle);
                float rx = (float)(xOff * Math.cos(rad) - zOff * Math.sin(rad));
                float rz = (float)(xOff * Math.sin(rad) + zOff * Math.cos(rad));

                float tx = cx + rx * size;
                float ty = cy - rz * size;

                int col = getRadarTrackColor(track.categoryOrdinal());
                float tr = ((col >> 16) & 0xFF) / 255f;
                float tg = ((col >> 8) & 0xFF) / 255f;
                float tb = (col & 0xFF) / 255f;

                ResourceLocation tTex = switch (track.categoryOrdinal()) {
                    case 0 -> TEX_PLAYER;
                    case 5 -> TEX_CONTRAPTION;
                    case 4 -> TEX_PROJECTILE;
                    default -> TEX_ENTITY;
                };

                RenderSystem.setShaderTexture(0, tTex);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(tr, tg, tb, slot.alpha / 255f);
                drawTexturedQuad(pose, tx - trackSize/2, ty - trackSize/2, trackSize, trackSize);

                // 玩家名
                if (track.categoryOrdinal() == 0 && !track.id().isEmpty()) {
                    String name = resolveRadarPlayerName(track.id());
                    if (name.length() > 8) name = name.substring(0, 8);
                    float labelY = ty + trackSize - size / 5f;
                    pose.pushPose();
                    pose.translate(tx, labelY, 0.01f);
                    pose.scale(0.1f, 0.1f, 1f);
                    font.drawInBatch(name, -font.width(name)/2f, 0, 0xFFFFFFFF,
                            false, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
                    pose.popPose();
                }
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        RenderSystem.disableBlend();
        pose.popPose();
    }

    private void drawTexturedQuad(PoseStack pose, float x, float y, float w, float h) {
        Matrix4f mat = pose.last().pose();
        var tesselator = Tesselator.getInstance();
        var builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(mat, x, y + h, 0).setUv(0, 1);
        builder.addVertex(mat, x + w, y + h, 0).setUv(1, 1);
        builder.addVertex(mat, x + w, y, 0).setUv(1, 0);
        builder.addVertex(mat, x, y, 0).setUv(0, 0);
        var meshData = builder.build();
        if (meshData != null) BufferUploader.drawWithShader(meshData);
    }

    private static int getRadarTrackColor(int cat) {
        return switch (cat) {
            case 0 -> 0x00FF00;
            case 1, 3 -> 0xFFFF00;
            case 2 -> 0xFF0000;
            case 4 -> 0xFF8800;
            case 5 -> 0x4488FF;
            case 6 -> 0xCCCCCC;
            default -> 0x888888;
        };
    }

    private String resolveRadarPlayerName(String trackId) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return trackId;
        for (var p : mc.level.players()) {
            if (p.getStringUUID().equals(trackId) || p.getName().getString().equals(trackId))
                return p.getName().getString();
        }
        return trackId.length() > 12 ? trackId.substring(0, 12) : trackId;
    }

    /** 用 Tessellator 在面板上渲染图片纹理（POSITION_COLOR_TEX，无需 UV1/UV2/Normal） */
    private void renderImageOnPanel(PoseStack pose, ResourceLocation tex,
                                    float x, float y, float scale, float rotation,
                                    int iw, int ih, int alpha) {
        pose.pushPose();
        float s = Math.max(0.1f, scale);
        // 归一化：scale=1 时长边 = 20 面板单位，保持宽高比
        float baseSize = 20f * s;
        float maxSide = Math.max(iw, ih);
        float dw = iw / maxSide * baseSize;
        float dh = ih / maxSide * baseSize;
        // 先移到位置，再绕图片中心旋转
        pose.translate(x + dw/2, y + dh/2, 0.01f);
        if (rotation != 0) {
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        }
        pose.translate(-dw/2, -dh/2, 0);
        Matrix4f mat = pose.last().pose();
        float a = (alpha & 0xFF) / 255f;

        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, a);

        var tesselator = Tesselator.getInstance();
        var builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(mat, 0, 0, 0).setUv(0, 0);
        builder.addVertex(mat, 0, dh, 0).setUv(0, 1);
        builder.addVertex(mat, dw, dh, 0).setUv(1, 1);
        builder.addVertex(mat, dw, 0, 0).setUv(1, 0);
        var meshData = builder.build();
        if (meshData != null) BufferUploader.drawWithShader(meshData);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.disableBlend();
        pose.popPose();
    }

    private int horizontalIndex(BlockPos pos, BlockPos ctrl, Direction facing) {
        Direction right = facing.getClockWise();
        if (right == Direction.EAST)  return pos.getX() - ctrl.getX();
        if (right == Direction.WEST)  return ctrl.getX() - pos.getX();
        if (right == Direction.SOUTH) return pos.getZ() - ctrl.getZ();
        return ctrl.getZ() - pos.getZ();
    }

    private boolean inRegion(float x, float y, float left, float right, float top, float bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    private void renderText(PoseStack pose, MultiBufferSource buffer, Font font,
                            String text, float x, float y, float scale,
                            int color, int alpha, int light) {
        if (text == null || text.isEmpty()) return;

        String clean = text.replaceAll("§[0-9a-fk-or]", "");
        if (clean.isEmpty()) return;

        pose.pushPose();
        pose.translate(x, y, 0);

        if (scale != 1.0f && scale > 0) {
            pose.scale(scale, scale, 1);
        }

        int a = alpha & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        font.drawInBatch(
                clean,
                0, 9f,
                argb,
                false,
                pose.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                light
        );

        pose.popPose();
    }

    /** 屏幕发光：纯色半透明覆层 + 全亮度 */
    private void renderDebugMarker(PoseStack pose, MultiBufferSource buffer, Font font,
                                   String label, float x, float y, int light) {
        pose.pushPose();
        pose.translate(x, y, 0);
        font.drawInBatch(
                label,
                -font.width(label) / 2f, -4f,
                0xFFFFFF00,
                false,
                pose.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                light
        );
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(DisplayBlockEntity dbe) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
