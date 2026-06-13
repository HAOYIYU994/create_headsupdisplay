package com.github.haoyiyu.create_headsupdisplay.block.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 通过通用 NBT 路径读取任意数据。
 * config 中 "path" 指定点分路径，如 "Items[0].count" 或 "Energy"。
 * 支持数组索引（[N]）和嵌套对象（.key.subkey）。
 */
public class NbtPathChannel implements DataChannel {
    @Override public String getId() { return "nbt_path"; }
    @Override public String getDisplayNameKey() { return "gui.create_headsupdisplay.data_probe.channel.nbt_path"; }
    @Override public String getUnit() { return ""; }
    @Override public boolean needsConfig() { return true; }

    @Override
    public boolean canRead(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        return be != null; // 需要有方块实体才有 NBT
    }

    @Override
    public String readValue(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, CompoundTag config) {
        if (be == null) return "ERR";
        String path = config.getString("path");
        if (path.isEmpty()) return "ERR";

        CompoundTag root = be.saveWithFullMetadata(level.registryAccess());
        Tag current = traverse(root, path);
        if (current == null) return "ERR";

        return switch (current) {
            case net.minecraft.nbt.ByteTag b -> String.valueOf(b.getAsByte());
            case net.minecraft.nbt.ShortTag s -> String.valueOf(s.getAsShort());
            case net.minecraft.nbt.IntTag i -> String.valueOf(i.getAsInt());
            case net.minecraft.nbt.LongTag l -> String.valueOf(l.getAsLong());
            case net.minecraft.nbt.FloatTag f -> String.valueOf(f.getAsFloat());
            case net.minecraft.nbt.DoubleTag d -> String.valueOf(d.getAsDouble());
            case net.minecraft.nbt.StringTag s -> s.getAsString();
            default -> current.getAsString();
        };
    }

    /** 遍历点分路径，支持 .key.subkey 和 .key[N] 数组索引 */
    @Nullable
    private static Tag traverse(CompoundTag root, String path) {
        Tag current = root;
        String[] parts = path.split("\\.");
        for (String part : parts) {
            // 检查是否有数组索引 [N]
            int bracketIdx = part.indexOf('[');
            String key;
            int index = -1;
            if (bracketIdx > 0 && part.endsWith("]")) {
                key = part.substring(0, bracketIdx);
                try {
                    index = Integer.parseInt(part.substring(bracketIdx + 1, part.length() - 1));
                } catch (NumberFormatException e) { return null; }
            } else {
                key = part;
            }

            if (current instanceof CompoundTag ct) {
                if (!ct.contains(key)) return null;
                current = ct.get(key);
            } else {
                return null;
            }

            // 如果指定了数组索引，展开 ListTag
            if (index >= 0 && current instanceof net.minecraft.nbt.ListTag list) {
                if (index >= list.size()) return null;
                current = list.get(index);
            }
        }
        return current;
    }
}
