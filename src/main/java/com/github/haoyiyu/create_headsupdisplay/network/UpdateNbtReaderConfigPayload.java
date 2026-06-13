package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.NbtReaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record UpdateNbtReaderConfigPayload(BlockPos probePos, String nbtPath, String name, int interval) implements CustomPacketPayload {
    public static final Type<UpdateNbtReaderConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "update_nbt_reader_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateNbtReaderConfigPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, UpdateNbtReaderConfigPayload::probePos,
                    ByteBufCodecs.STRING_UTF8, UpdateNbtReaderConfigPayload::nbtPath,
                    ByteBufCodecs.STRING_UTF8, UpdateNbtReaderConfigPayload::name,
                    ByteBufCodecs.VAR_INT, UpdateNbtReaderConfigPayload::interval,
                    UpdateNbtReaderConfigPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(UpdateNbtReaderConfigPayload p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> { Level l = ctx.player().level();
            if (l.getBlockEntity(p.probePos()) instanceof NbtReaderBlockEntity be) be.saveConfig(p.nbtPath(), p.name(), p.interval()); });
    }
}
