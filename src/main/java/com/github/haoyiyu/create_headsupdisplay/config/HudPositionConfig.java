package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record HudPositionConfig(int posX, int posY) {
    public static final StreamCodec<RegistryFriendlyByteBuf, HudPositionConfig> STREAM_CODEC = StreamCodec.of(
            (buf, config) -> {
                buf.writeInt(config.posX);
                buf.writeInt(config.posY);
            },
            buf -> new HudPositionConfig(buf.readInt(), buf.readInt())
    );

    public static HudPositionConfig defaultConfig() {
        return new HudPositionConfig(10, 10);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("posX", posX);
        tag.putInt("posY", posY);
        return tag;
    }

    public static HudPositionConfig fromNbt(CompoundTag tag) {
        return new HudPositionConfig(tag.getInt("posX"), tag.getInt("posY"));
    }
}