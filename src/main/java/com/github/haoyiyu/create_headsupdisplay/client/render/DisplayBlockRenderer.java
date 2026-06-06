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

        var slots = ClientHudData.getSlots();
        var staticTexts = ClientHudData.getStaticTextSlots();

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

        // --- 渲染图片（底层，Tessellator） ---
        var images = ClientHudData.getImages();
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

        // 初始化字体 buffer consumer
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
