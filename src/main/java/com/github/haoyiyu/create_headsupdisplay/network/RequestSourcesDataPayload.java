package com.github.haoyiyu.create_headsupdisplay.network;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.block.OmniCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record RequestSourcesDataPayload(BlockPos corePos) implements CustomPacketPayload {
    public static final Type<RequestSourcesDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeadsUpDisplay.MOD_ID, "request_sources_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSourcesDataPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RequestSourcesDataPayload::corePos,
            RequestSourcesDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestSourcesDataPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var be = sp.level().getBlockEntity(payload.corePos);
                if (be instanceof OmniCoreBlockEntity core) {
                    CompoundTag data = new CompoundTag();
                    data.putLong("CorePos", payload.corePos().asLong());
                    ListTag list = new ListTag();
                    for (int i = 0; i < core.getSourceCount(); i++) {
                        var src = core.getSource(i);
                        CompoundTag tag = new CompoundTag();
                        tag.putString("type", src.sourceType());
                        tag.putString("name", src.name());
                        tag.putInt("strength", src.strength());
                        tag.putString("display", src.displayText());
                        tag.put("item1", src.item1().saveOptional(sp.level().registryAccess()));
                        tag.put("item2", src.item2().saveOptional(sp.level().registryAccess()));
                        if (src.displayLinkSourcePos() != null) {
                            tag.putString("dlText", src.displayText());
                            tag.putLong("dlSourcePos", src.displayLinkSourcePos().asLong());
                        }
                        // 转译配置
                        var tc = core.getTranslation(i);
                        if (tc != null && tc.getMode() != com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig.Mode.NONE) {
                            tag.put("translation", tc.serialize());
                        }
                        // IMAGE 类型字段
                        if ("IMAGE".equals(src.sourceType())) {
                            tag.putUUID("ImageId", src.imageId());
                            tag.putString("ImageFileName", src.imageFileName());
                            tag.putByteArray("ImageData", src.imageData());
                        }
                        list.add(tag);
                    }
                    data.putBoolean("AutoSort", core.isAutoSortEnabled());
                    data.putBoolean("HasImagePlugin", core.hasImagePlugin());
                    data.putBoolean("HasRadarPlugin", core.hasRadarPlugin());
                    data.put("Sources", list);
                    // 雷达槽位
                    ListTag radarSlotTag = new ListTag();
                    for (var slot : core.getRadarSlots()) radarSlotTag.add(slot.serialize());
                    data.put("RadarSlots", radarSlotTag);
                    PacketDistributor.sendToPlayer(sp, new SyncSourcesDataPayload(data));
                }
            }
        });
    }
}