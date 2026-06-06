package com.github.haoyiyu.create_headsupdisplay.client.render;

import com.github.haoyiyu.create_headsupdisplay.block.DisplayBlock;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayBlockEntity;
import com.github.haoyiyu.create_headsupdisplay.client.ClientHudData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

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
                .translate(0, 0, 0.01f);

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

        // 初始化字体 buffer consumer
        font.drawInBatch(" ", 0, 0, 0x00000000, false, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);

        // --- 渲染动态槽位 ---
        if (slots != null) {
            for (var slot : slots) {
                float dx = slot.posX * scaleX;
                float dy = slot.posY * scaleY;
                if (!inRegion(dx, dy, regionLeft, regionRight, regionTop, regionBottom)) continue;

                renderText(pose, buffer, font, slot.text, dx, dy, slot.scale, slot.color, slot.alpha, light);
                renderedCount++;
            }
        }

        // --- 渲染静态文本 ---
        if (staticTexts != null) {
            for (var st : staticTexts) {
                float dx = st.posX * scaleX;
                float dy = st.posY * scaleY;
                if (!inRegion(dx, dy, regionLeft, regionRight, regionTop, regionBottom)) continue;

                renderText(pose, buffer, font, st.text, dx, dy, st.scale, st.color, st.alpha, light);
                renderedCount++;
            }
        }

        // 结束批处理（始终 flush，即使无文本）
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }

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
