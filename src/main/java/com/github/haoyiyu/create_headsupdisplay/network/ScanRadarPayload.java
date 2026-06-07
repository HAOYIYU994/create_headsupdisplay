package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 客户端→服务端：扫描附近雷达 Monitor 并连接到 OmniCore */
public record ScanRadarPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<ScanRadarPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "scan_radar")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanRadarPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeBlockPos(p.corePos),
            buf -> new ScanRadarPayload(buf.readBlockPos())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ScanRadarPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            var be = level.getBlockEntity(payload.corePos);
            if (!(be instanceof OmniCoreBlockEntity core)) return;

            // 大范围扫描 MonitorBlockEntity（32 格半径）
            BlockPos pos = payload.corePos;
            int range = 32;
            BlockPos found = null;
            for (BlockPos p : BlockPos.betweenClosed(
                    pos.offset(-range, -range, -range),
                    pos.offset(range, range, range))) {
                BlockEntity nearby = level.getBlockEntity(p);
                if (nearby != null && isMonitor(nearby)) {
                    found = p.immutable();
                    break;
                }
            }

            if (found != null) {
                core.setLinkedMonitor(found);
                context.player().displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aRadar Monitor found and linked at " + found.toShortString()),
                        false);
            } else {
                context.player().displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cNo radar Monitor found within " + range + " blocks"),
                        false);
            }
        });
    }

    private static boolean isMonitor(BlockEntity be) {
        if (be == null) return false;
        String name = be.getClass().getName();
        return name.contains("MonitorBlockEntity") || name.contains("RadarBearingBlockEntity");
    }
}
