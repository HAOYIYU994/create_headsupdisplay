package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.DisplayTerminalProBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端→服务端：完整保存图层和所有槽位配置 */
public record SaveProConfigPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<SaveProConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "save_pro_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveProConfigPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeNbt(p.data),
            buf -> new SaveProConfigPayload(buf.readNbt())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SaveProConfigPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = ctx.player().level();
            BlockPos pos = BlockPos.of(payload.data.getLong("TerminalPos"));
            if (level.getBlockEntity(pos) instanceof DisplayTerminalProBlockEntity be) {
                be.loadConfigFromClient(payload.data);
            }
        });
    }
}