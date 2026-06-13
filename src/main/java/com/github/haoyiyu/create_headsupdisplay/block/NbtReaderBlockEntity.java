package com.github.haoyiyu.create_headsupdisplay.block;

import com.github.haoyiyu.create_headsupdisplay.block.probe.ChannelRegistry;
import com.github.haoyiyu.create_headsupdisplay.network.OpenNbtReaderScreenPayload;
import com.github.haoyiyu.create_headsupdisplay.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class NbtReaderBlockEntity extends BlockEntity {

    private String probeName = "NBT";
    private String nbtPath = "";
    private int updateInterval = 10;
    private String lastValue = "";
    private int tickCounter;

    public NbtReaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NBT_READER.get(), pos, state);
    }

    public String getProbeName() { return probeName; }
    public String getSelectedChannelId() { return "nbt_path"; } // 唯一的通道
    public CompoundTag getChannelConfig() { CompoundTag c = new CompoundTag(); c.putString("path", nbtPath); return c; }
    public int getUpdateInterval() { return updateInterval; }
    public String getLastValue() { return lastValue; }

    public void saveConfig(String nbtPath, String name, int interval) {
        this.nbtPath = nbtPath != null ? nbtPath : "";
        this.probeName = name != null && !name.isEmpty() ? name : "NBT";
        this.updateInterval = Math.clamp(interval, 1, 100);
        setChanged();
    }

    public BlockPos getTargetPos() {
        return worldPosition.relative(getBlockState().getValue(NbtReaderBlock.FACING).getOpposite());
    }

    public String readValue() {
        if (level == null || nbtPath.isEmpty()) return "?";
        BlockPos tp = getTargetPos();
        BlockEntity targetBe = level.getBlockEntity(tp);
        try {
            return ChannelRegistry.nbt().readValue(level, tp, level.getBlockState(tp), targetBe, getChannelConfig());
        } catch (Exception e) { return "ERR"; }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NbtReaderBlockEntity be) {
        if (level.isClientSide || be.nbtPath.isEmpty()) return;
        be.tickCounter++;
        if (be.tickCounter < be.updateInterval) return;
        be.tickCounter = 0;
        be.lastValue = be.readValue();
        be.setChanged();
    }

    public void openConfigScreen(Player player) {
        if (level == null || level.isClientSide || !(player instanceof ServerPlayer sp)) return;
        CompoundTag data = new CompoundTag();
        data.putLong("ProbePos", worldPosition.asLong());
        BlockPos tp = getTargetPos();
        data.putString("TargetName", level.getBlockState(tp).getBlock().getName().getString());
        data.putString("NbtPath", nbtPath);
        data.putString("ProbeName", probeName);
        data.putInt("UpdateInterval", updateInterval);
        data.putString("LastValue", lastValue);
        BlockEntity targetBe = level.getBlockEntity(tp);
        if (targetBe != null) data.put("NbtTree", targetBe.saveWithFullMetadata(level.registryAccess()));
        PacketDistributor.sendToPlayer(sp, new OpenNbtReaderScreenPayload(data));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putString("ProbeName", probeName);
        tag.putString("NbtPath", nbtPath);
        tag.putInt("UpdateInterval", updateInterval);
        tag.putString("LastValue", lastValue);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        probeName = tag.getString("ProbeName");
        nbtPath = tag.getString("NbtPath");
        updateInterval = tag.getInt("UpdateInterval");
        if (updateInterval <= 0) updateInterval = 10;
        lastValue = tag.getString("LastValue");
    }
}
