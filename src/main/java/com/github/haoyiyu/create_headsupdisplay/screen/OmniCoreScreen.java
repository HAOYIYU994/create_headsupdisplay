package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.client.ClientOmniCoreData;
import com.github.haoyiyu.create_headsupdisplay.network.RemoveRedstoneSourcePayload;
import com.github.haoyiyu.create_headsupdisplay.network.RequestOpenFrequencySelectionPayload;
import com.github.haoyiyu.create_headsupdisplay.network.RequestSourcesDataPayload;
import com.github.haoyiyu.create_headsupdisplay.network.SendSourceToTerminalPayload;
import com.github.haoyiyu.create_headsupdisplay.network.ToggleAutoSortPayload;
import com.github.haoyiyu.create_headsupdisplay.network.UpdateTranslationPayload;
import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class OmniCoreScreen extends Screen {
    private final BlockPos corePos;
    private final List<SourceEntry> sources = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 25;
    private int tickCounter = 0;
    private long lastDataHash = 0;
    private boolean autoSort = true;

    public OmniCoreScreen(CompoundTag data) {
        super(Component.translatable("gui.create_headsupdisplay.omni_core.title"));
        this.corePos = BlockPos.of(data.getLong("CorePos"));
        loadSourcesFromTag(data);
    }

    private void loadSourcesFromTag(CompoundTag data) {
        sources.clear();
        ListTag sourcesTag = data.getList("Sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < sourcesTag.size(); i++) {
            CompoundTag srcTag = sourcesTag.getCompound(i);
            String name = srcTag.getString("name");
            ItemStack item1 = ItemStack.EMPTY;
            ItemStack item2 = ItemStack.EMPTY;
            try {
                if (srcTag.contains("item1")) {
                    item1 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("item1"));
                } else if (srcTag.contains("freqItem1")) {
                    item1 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("freqItem1"));
                }
                if (srcTag.contains("item2")) {
                    item2 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("item2"));
                } else if (srcTag.contains("freqItem2")) {
                    item2 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("freqItem2"));
                }
            } catch (Exception ignored) {}
            int strength = srcTag.getInt("strength");
            String display = srcTag.contains("display") ? srcTag.getString("display") : String.valueOf(strength);
            String dlText = srcTag.contains("dlText") ? srcTag.getString("dlText") : null;
            BlockPos dlSourcePos = srcTag.contains("dlSourcePos") ? BlockPos.of(srcTag.getLong("dlSourcePos")) : null;
            SourceEntry entry = new SourceEntry(name, item1, item2, strength, display, dlText, dlSourcePos);
            if (srcTag.contains("translation")) {
                entry.transConfig = TranslationConfig.deserialize(srcTag.getCompound("translation"));
                entry.trans = true;
            }
            sources.add(entry);
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.add_source"), b -> {
            PacketDistributor.sendToServer(new RequestOpenFrequencySelectionPayload(corePos));
        }).bounds(10, 10, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.sort_on"), b -> {
            PacketDistributor.sendToServer(new ToggleAutoSortPayload(corePos));
            autoSort = !autoSort;
            b.setMessage(Component.translatable(autoSort ? "gui.create_headsupdisplay.sort_on" : "gui.create_headsupdisplay.sort_off"));
        }).bounds(10, 54, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.back"), b -> onClose())
                .bounds(10, height - 30, 60, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (tickCounter >= 3) {
            tickCounter = 0;
            PacketDistributor.sendToServer(new RequestSourcesDataPayload(corePos));
        }
        CompoundTag synced = ClientOmniCoreData.getSourcesData(corePos);
        if (synced != null) {
            autoSort = synced.getBoolean("AutoSort");
            loadSourcesFromTag(synced);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int startX = 160;
        int startY = 18;
        graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.signal_sources").getString(), startX, startY - 5, 0xFFFFFF);

        // 信息栏纯黑底色
        int listTop = startY;
        int listBottom = startY + sources.size() * ENTRY_HEIGHT;
        int listLeft = startX - 44;
        graphics.fill(listLeft, listTop, width, listBottom, 0xCC000000);

        for (int i = 0; i < sources.size(); i++) {
            int y = startY + i * ENTRY_HEIGHT - scrollOffset;
            if (y < 0 || y > height) continue;
            SourceEntry src = sources.get(i);

            // 频率物品图标区域
            int iconY = y + 3;
            int iconSize = 16;
            int icon1X = startX - 42;
            int icon2X = startX - 22;

            if (src.dlText != null) {
                // Display Link 源：黄色长条 + 源方块图标
                int yellowLeft = icon1X - 1;
                int yellowRight = icon2X + iconSize + 1;
                graphics.fill(yellowLeft, iconY - 1, yellowRight, iconY + iconSize + 1, 0xAAFFAA00);
                // 尝试获取源方块图标
                ItemStack displayIcon = ItemStack.EMPTY;
                if (src.dlSourcePos != null && Minecraft.getInstance().level != null) {
                    var sourceState = Minecraft.getInstance().level.getBlockState(src.dlSourcePos);
                    displayIcon = new ItemStack(sourceState.getBlock().asItem());
                }
                if (displayIcon.isEmpty()) {
                    displayIcon = new ItemStack(net.minecraft.world.item.Items.REPEATER); // 默认用中继器图标
                }
                graphics.renderItem(displayIcon, (yellowLeft + yellowRight) / 2 - 8, iconY);
            } else {
                // 红石频率源：红底+蓝底
                graphics.fill(icon1X - 1, iconY - 1, icon1X + iconSize + 1, iconY + iconSize + 1, 0xAAFF0000);
                graphics.fill(icon2X - 1, iconY - 1, icon2X + iconSize + 1, iconY + iconSize + 1, 0xAA0000FF);
                if (!src.freqItem1.isEmpty()) {
                    graphics.renderItem(src.freqItem1, icon1X, iconY);
                }
                if (!src.freqItem2.isEmpty()) {
                    graphics.renderItem(src.freqItem2, icon2X, iconY);
                }
            }

            String display = (src.dlText != null) ? (src.name + ": " + src.dlText) : (src.name + ": " + src.display);
            display = display.replaceAll("§[0-9a-fk-or]", "");
            graphics.drawString(font, display, startX, y + 5, 0xFFFFFF);

            // 转译按钮
            int transBtnX = width - 120;
            graphics.fill(transBtnX, y, transBtnX + 30, y + 20, src.trans ? 0xAAFFA500 : 0xAA555555);
            graphics.drawString(font, "T", transBtnX + 11, y + 6, 0xFFFFFF);

            int sendBtnX = width - 80;
            int delBtnX = width - 40;
            graphics.fill(sendBtnX, y, sendBtnX + 30, y + 20, 0xAA00AA00);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.send").getString(), sendBtnX + 5, y + 6, 0xFFFFFF);
            graphics.fill(delBtnX, y, delBtnX + 30, y + 20, 0xAAFF0000);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.delete").getString(), delBtnX + 5, y + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int startY = 20;
        for (int i = 0; i < sources.size(); i++) {
            int y = startY + i * ENTRY_HEIGHT - scrollOffset;
            if (y < 0 || y > height) continue;
            // 转译按钮
            if (mouseX >= width - 120 && mouseX <= width - 90 && mouseY >= y && mouseY <= y + 20) {
                final int idx = i;
                SourceEntry entry = sources.get(idx);
                TranslationConfig tc = entry.transConfig != null ? entry.transConfig : new TranslationConfig();
                Minecraft.getInstance().setScreen(new TranslationScreen(tc, config -> {
                    entry.transConfig = config;
                    entry.trans = config.getMode() != TranslationConfig.Mode.NONE;
                    PacketDistributor.sendToServer(new UpdateTranslationPayload(corePos, idx, config.serialize()));
                }));
                return true;
            }
            if (mouseX >= width - 80 && mouseX <= width - 50 && mouseY >= y && mouseY <= y + 20) {
                PacketDistributor.sendToServer(new SendSourceToTerminalPayload(corePos, i));
                return true;
            }
            if (mouseX >= width - 40 && mouseX <= width - 10 && mouseY >= y && mouseY <= y + 20) {
                PacketDistributor.sendToServer(new RemoveRedstoneSourcePayload(corePos, i));
                sources.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = sources.size() * ENTRY_HEIGHT - (height - 40);
        if (maxScroll > 0) {
            scrollOffset = (int) Math.clamp(scrollOffset - scrollY * 10, 0, maxScroll);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void addSource(String name) {
        sources.add(new SourceEntry(name, ItemStack.EMPTY, ItemStack.EMPTY, 0, "0", null, null));
    }

    private static class SourceEntry {
        String name;
        ItemStack freqItem1;
        ItemStack freqItem2;
        int strength;
        String display;
        String dlText;
        BlockPos dlSourcePos;
        boolean trans;
        TranslationConfig transConfig;
        SourceEntry(String n, ItemStack i1, ItemStack i2, int s, String disp, String d, BlockPos dp) {
            name = n; freqItem1 = i1; freqItem2 = i2; strength = s; display = disp; dlText = d; dlSourcePos = dp;
        }
    }
}