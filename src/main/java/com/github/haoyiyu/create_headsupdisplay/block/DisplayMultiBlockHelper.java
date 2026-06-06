package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import static com.github.haoyiyu.create_headsupdisplay.block.DisplayBlock.FACING;
import static com.github.haoyiyu.create_headsupdisplay.block.DisplayBlock.SHAPE;

/** 多方块显示器辅助 — 支持任意矩形 (width × height) */
public class DisplayMultiBlockHelper {
    private static final int MAX_W = 32;
    private static final int MAX_H = 16;

    public static void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay.LOGGER.info("DisplayMultiBlock onPlace pos={} shape={} moved={}", pos.toShortString(), state.getValue(SHAPE), moved);
        if (state.getValue(SHAPE) != DisplayBlock.Shape.SINGLE && !moved) return;
        Direction facing = state.getValue(FACING);
        BlockPos.betweenClosedStream(new AABB(pos).inflate(Math.max(MAX_W, MAX_H))).forEach(candidate -> {
            BlockState cs = level.getBlockState(candidate);
            if (!cs.is(ModBlocks.DISPLAY.get())) return;
            if (cs.getValue(FACING) != facing) return;
            if (level.getBlockEntity(candidate) instanceof DisplayBlockEntity be) {
                int w = be.getPanelWidth(), h = be.getPanelHeight();
                if (w > 1 || h > 1) { formMulti(level, be.getControllerPos(), w, h); return; }
            }
        });
    }

    public static void refreshAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.DISPLAY.get())) return;
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        int bestW = 1, bestH = 1, bestArea = 1;
        BlockPos bestController = pos;
        for (int w = 1; w <= MAX_W; w++) {
            for (int h = 1; h <= MAX_H; h++) {
                if (w == 1 && h == 1) continue;
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        BlockPos controller = pos.below(i).relative(right, -j);
                        if (isValidFormation(level, controller, facing, w, h)) {
                            int area = w * h;
                            if (area > bestArea) { bestW = w; bestH = h; bestArea = area; bestController = controller; }
                        }
                    }
                }
            }
        }
        if (bestArea > 1) formMulti(level, bestController, bestW, bestH);
    }

    public static boolean isValidFormation(Level level, BlockPos controller, Direction facing, int w, int h) {
        Direction right = facing.getClockWise();
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                BlockPos pos = controller.above(i).relative(right, j);
                BlockState state = level.getBlockState(pos);
                if (!state.is(ModBlocks.DISPLAY.get())) return false;
                if (state.getValue(FACING) != facing) return false;
            }
        }
        return true;
    }

    public static void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (newState.is(ModBlocks.DISPLAY.get()) && !moved) return;
        if (level.getBlockEntity(pos) instanceof DisplayBlockEntity be)
            destroyMulti(state, level, pos, be.getControllerPos(), be.getPanelWidth(), be.getPanelHeight());
    }

    static void formMulti(Level level, BlockPos controllerPos, int w, int h) {
        BlockState ctrlState = level.getBlockState(controllerPos);
        if (!ctrlState.is(ModBlocks.DISPLAY.get())) return;
        Direction facing = ctrlState.getValue(FACING);
        Direction right = facing.getClockWise();
        BlockState baseState = ModBlocks.DISPLAY.get().defaultBlockState().setValue(FACING, facing);
        // 从控制方块自动绑定相邻终端
        BlockPos term = null;
        for (Direction d : net.minecraft.core.Direction.values()) {
            BlockPos n = controllerPos.relative(d);
            if (level.getBlockEntity(n) instanceof com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalBlockEntity) {
                term = n; break;
            }
        }
        // 回退：扫描已存在的绑定
        if (term == null) {
            for (int i = 0; i < h; i++)
                for (int j = 0; j < w; j++)
                    if (level.getBlockEntity(controllerPos.above(i).relative(right, j)) instanceof DisplayBlockEntity be && be.getBoundTerminal() != null)
                        term = be.getBoundTerminal();
        }
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                DisplayBlock.Shape shape = shapeFor(i, j, w, h);
                BlockPos pos = controllerPos.above(i).relative(right, j);
                level.setBlockAndUpdate(pos, baseState.setValue(SHAPE, shape));
                if (level.getBlockEntity(pos) instanceof DisplayBlockEntity be) {
                    be.setControllerPos(controllerPos, w, h);
                    if (term != null)
                        be.setBoundTerminal(term);
                }
            }
        }
        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++)
                level.sendBlockUpdated(controllerPos.above(i).relative(right, j), baseState, baseState.setValue(SHAPE, shapeFor(i, j, w, h)), 3);
    }

    private static void destroyMulti(BlockState state, Level level, BlockPos removedPos, BlockPos controllerPos, int w, int h) {
        if (w <= 1 && h <= 1) return;
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                BlockPos pos = controllerPos.above(i).relative(right, j);
                if (pos.equals(removedPos)) continue;
                if (level.getBlockEntity(pos) instanceof DisplayBlockEntity be && be.getControllerPos().equals(controllerPos)) {
                    be.setControllerPos(pos, 1, 1);
                    be.setBoundTerminal(null);
                    level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(SHAPE, DisplayBlock.Shape.SINGLE));
                }
            }
        }
    }

    static DisplayBlock.Shape shapeFor(int i, int j, int w, int h) {
        if (w <= 1 && h <= 1) return DisplayBlock.Shape.SINGLE;
        boolean bottom = (i == 0);
        boolean top    = (i == h - 1);
        boolean left   = (j == w - 1);
        boolean right  = (j == 0);

        if (bottom && right)  return DisplayBlock.Shape.LOWER_RIGHT;
        if (bottom && left)   return DisplayBlock.Shape.LOWER_LEFT;
        if (top    && right)  return DisplayBlock.Shape.UPPER_RIGHT;
        if (top    && left)   return DisplayBlock.Shape.UPPER_LEFT;
        if (bottom)           return DisplayBlock.Shape.LOWER_CENTER;
        if (top)              return DisplayBlock.Shape.UPPER_CENTER;
        if (right)            return DisplayBlock.Shape.MIDDLE_RIGHT;
        if (left)             return DisplayBlock.Shape.MIDDLE_LEFT;
        return DisplayBlock.Shape.CENTER;
    }
}
