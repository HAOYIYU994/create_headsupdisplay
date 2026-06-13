package com.github.haoyiyu.create_headsupdisplay.hud;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

/**
 * 可选 sable 物理引擎集成：读取终端所在物理体的真实世界朝向，
 * 同时计算玩家在物理体上的世界朝向（处理坐垫旋转同步）。
 * 如果 sable 未加载或终端不在 SubLevel 中，返回 null。
 */
final class SablePhysicsHelper {

    private static final boolean SABLE_LOADED;
    private static final Class<?> CLIENT_SUB_LEVEL_CLASS;

    static {
        boolean loaded = false;
        Class<?> csl = null;
        try {
            Class.forName("dev.ryanhcode.sable.Sable");
            csl = Class.forName("dev.ryanhcode.sable.sublevel.ClientSubLevel");
            loaded = true;
        } catch (ClassNotFoundException ignored) {
        }
        SABLE_LOADED = loaded;
        CLIENT_SUB_LEVEL_CLASS = csl;
    }

    /** 缓存终端原始 FACING yaw，防止装配后 BlockState 丢失 */
    private static final Map<BlockPos, Float> facingYawCache = new HashMap<>();

    /**
     * 缓存或读取终端的原始 FACING yaw。
     */
    static float getOrCacheFacingYaw(BlockPos terminalPos, float currentFacing, boolean hasFacing) {
        if (hasFacing) {
            facingYawCache.put(terminalPos.immutable(), currentFacing);
            return currentFacing;
        }
        Float cached = facingYawCache.get(terminalPos);
        return cached != null ? cached : currentFacing;
    }

    /**
     * 从玩家或玩家坐骑位置查找 SubLevel（物理体飞走后 terminalPos 不在原地了）。
     */
    private static Object findSubLevelByPlayer(Level level, net.minecraft.world.entity.player.Player player) {
        if (!SABLE_LOADED || level == null) return null;
        try {
            Object sable = Class.forName("dev.ryanhcode.sable.Sable")
                    .getField("HELPER").get(null);
            // 先查玩家坐骑（物理体上的座位）
            Object subLevel = sable.getClass()
                    .getMethod("getContainingClient", net.minecraft.world.entity.Entity.class)
                    .invoke(sable, player);
            if (subLevel != null && CLIENT_SUB_LEVEL_CLASS.isInstance(subLevel)) return subLevel;
            // 再查玩家位置
            subLevel = sable.getClass()
                    .getMethod("getContainingClient", double.class, double.class)
                    .invoke(sable, player.getX(), player.getZ());
            if (subLevel != null && CLIENT_SUB_LEVEL_CLASS.isInstance(subLevel)) return subLevel;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查找终端所属物理体的世界朝向。
     * 优先从玩家位置查找 SubLevel（物理体可能已飞离终端原始坐标）。
     * @return float[3]: {terminalWorldYaw, terminalWorldPitch, bodyYaw}
     *         或 null
     */
    static float[] tryGetPhysicsOrientation(Level level, BlockPos terminalPos,
                                             float cachedFacingYaw,
                                             net.minecraft.world.entity.player.Player player) {
        if (!SABLE_LOADED || level == null) return null;

        try {
            // 优先从玩家位置查 SubLevel，其次从终端坐标查
            Object subLevel = findSubLevelByPlayer(level, player);
            if (subLevel == null) {
                // 终端坐标备选
                Object sable = Class.forName("dev.ryanhcode.sable.Sable")
                        .getField("HELPER").get(null);
                subLevel = sable.getClass()
                        .getMethod("getContainingClient", double.class, double.class)
                        .invoke(sable, (double) terminalPos.getX(), (double) terminalPos.getZ());
            }
            if (subLevel == null || !CLIENT_SUB_LEVEL_CLASS.isInstance(subLevel)) return null;

            Object pose = CLIENT_SUB_LEVEL_CLASS.getMethod("renderPose").invoke(subLevel);
            Object quat = pose.getClass().getMethod("orientation").invoke(pose);
            if (!(quat instanceof Quaterniond q)) return null;

            // 终端世界 yaw（本地 FACING × 四元数，减 90° 补偿装配旋转）
            double yawRad = Math.toRadians(cachedFacingYaw);
            Vector3d localDir = new Vector3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
            q.transform(localDir);
            float termWorldYaw = (float) Math.toDegrees(Math.atan2(-localDir.x, localDir.z)) - 90f;

            // pitch 用 SubLevel 前向轴 (0,0,1)，不用终端 FACING，避免 FACING 垂直于俯仰轴
            Vector3d bodyFwd = new Vector3d(0, 0, 1);
            q.transform(bodyFwd);
            float termWorldPitch = -(float) Math.toDegrees(Math.asin(bodyFwd.y));
            float bodyYaw = (float) Math.toDegrees(Math.atan2(-bodyFwd.x, bodyFwd.z));

            return new float[]{termWorldYaw, termWorldPitch, bodyYaw};
        } catch (Exception e) {
            return null;
        }
    }

    /** 玩家是否在物理体上（通过玩家位置能否找到 SubLevel 判断） */
    static boolean isPlayerOnSubLevel(Level level, net.minecraft.world.entity.player.Player player) {
        return findSubLevelByPlayer(level, player) != null;
    }
}
