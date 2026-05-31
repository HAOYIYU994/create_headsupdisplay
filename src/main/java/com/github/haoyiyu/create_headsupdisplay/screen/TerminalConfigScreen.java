package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class TerminalConfigScreen extends Screen {
    private final List<SlotEntry> slots = new ArrayList<>();
    private final List<StaticTextEntry> staticTexts = new ArrayList<>();
    private BlockPos terminalPos;

    private SlotEntry draggingSlot = null;
    private StaticTextEntry draggingStatic = null;
    private int dragOffsetX, dragOffsetY;

    private EditBox addTextInput;
    private Button addTextButton;
    private Button resetLayoutButton;

    public TerminalConfigScreen(CompoundTag data) {
        super(Component.translatable("gui.create_headsupdisplay.display_terminal.title"));
        this.terminalPos = BlockPos.of(data.getLong("TerminalPos"));

        ListTag slotsTag = data.getList("Slots", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < slotsTag.size(); i++) {
            CompoundTag slotTag = slotsTag.getCompound(i);
            BlockPos sourcePos = BlockPos.of(slotTag.getLong("sourcePos"));
            int posX = slotTag.getInt("posX");
            int posY = slotTag.getInt("posY");
            float scale = slotTag.getFloat("scale");
            String text = slotTag.getString("text");
            int displayLine = slotTag.getInt("displayLine");
            float rotation = slotTag.getFloat("rotation");
            int color = slotTag.getInt("color");
            int alpha = slotTag.getInt("alpha");
            String sourceName = slotTag.contains("sourceName") ? slotTag.getString("sourceName") : null;
            slots.add(new SlotEntry(sourcePos, posX, posY, scale, text, displayLine, rotation, color, alpha, sourceName));
        }

        ListTag staticTag = data.getList("StaticTexts", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < staticTag.size(); i++) {
            CompoundTag st = staticTag.getCompound(i);
            String text = st.getString("text");
            int posX = st.getInt("posX");
            int posY = st.getInt("posY");
            float scale = st.getFloat("scale");
            float rotation = st.getFloat("rotation");
            int color = st.getInt("color");
            int alpha = st.getInt("alpha");
            staticTexts.add(new StaticTextEntry(text, posX, posY, scale, rotation, color, alpha));
        }
    }

    @Override
    protected void init() {
        super.init();

        resetLayoutButton = Button.builder(Component.translatable("gui.create_headsupdisplay.display_terminal.reset_layout"), b -> resetLayout())
                .bounds(width / 2 + 20, height - 30, 60, 20)
                .build();
        addRenderableWidget(resetLayoutButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.display_terminal.save"), b -> saveChanges())
                .bounds(width / 2 - 40, height - 30, 80, 20)
                .build());

        addTextInput = new EditBox(font, width / 2 - 100, height - 60, 150, 20, Component.translatable("gui.create_headsupdisplay.display_terminal.text_placeholder"));
        addTextInput.setMaxLength(64);
        addRenderableWidget(addTextInput);

        addTextButton = Button.builder(Component.translatable("gui.create_headsupdisplay.display_terminal.add_static_text"), b -> addStaticText())
                .bounds(width / 2 + 60, height - 60, 60, 20)
                .build();
        addRenderableWidget(addTextButton);
    }

    private void resetLayout() {
        int startX = width / 2 - 150;
        int startY = height / 2 - 60;
        int col = 0, row = 0;
        int spacingX = 260;
        int spacingY = 40;

        for (SlotEntry slot : slots) {
            slot.posX = startX + col * spacingX;
            slot.posY = startY + row * spacingY;
            slot.posX = Math.min(Math.max(slot.posX, 0), width - getSlotWidth(slot));
            slot.posY = Math.min(Math.max(slot.posY, 0), height - (int)(20 * slot.scale));
            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }
        for (StaticTextEntry entry : staticTexts) {
            entry.posX = startX + col * spacingX;
            entry.posY = startY + row * spacingY;
            entry.posX = Math.min(Math.max(entry.posX, 0), width - getStaticWidth(entry));
            entry.posY = Math.min(Math.max(entry.posY, 0), height - (int)(20 * entry.scale));
            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }
    }

    private void addStaticText() {
        String text = addTextInput.getValue();
        if (text.isEmpty()) return;
        int defaultX = width / 2 - 40;
        int defaultY = height / 2 - 10;
        float defaultScale = 1.0f;
        float defaultRotation = 0;
        int defaultColor = 0xFFFFFF;
        int defaultAlpha = 255;
        staticTexts.add(new StaticTextEntry(text, defaultX, defaultY, defaultScale, defaultRotation, defaultColor, defaultAlpha));
        PacketDistributor.sendToServer(new AddStaticTextPayload(terminalPos, text, defaultX, defaultY, defaultScale, defaultRotation, defaultColor, defaultAlpha));
        addTextInput.setValue("");
    }

    private void saveChanges() {
        for (SlotEntry slot : slots) {
            PacketDistributor.sendToServer(new UpdateSlotPayload(terminalPos, slot.sourcePos, slot.posX, slot.posY, slot.scale, slot.rotation, slot.color, slot.alpha));
        }
        for (int i = 0; i < staticTexts.size(); i++) {
            StaticTextEntry entry = staticTexts.get(i);
            PacketDistributor.sendToServer(new UpdateStaticTextPayload(terminalPos, i, entry.text, entry.posX, entry.posY, entry.scale, entry.rotation, entry.color, entry.alpha));
        }
        onClose();
    }

    private int getSlotWidth(SlotEntry slot) {
        int baseWidth = (int)(100 * slot.scale);
        int textWidth = font.width(slot.text);
        return Math.max(baseWidth, textWidth + 4);
    }

    private int getStaticWidth(StaticTextEntry entry) {
        int baseWidth = (int)(80 * entry.scale);
        int textWidth = font.width(entry.text);
        return Math.max(baseWidth, textWidth + 4);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // 数据源槽位
        for (SlotEntry slot : slots) {
            int w = getSlotWidth(slot);
            int h = (int)(20 * slot.scale);
            graphics.fill(slot.posX, slot.posY, slot.posX + w, slot.posY + h, 0xAA333333);
            graphics.drawString(font, slot.text, slot.posX + 2, slot.posY + 2, 0xFFFFFF, false);
            // 来源名称（显示在文本框下方）
            if (slot.sourceName != null) {
                graphics.drawString(font, slot.sourceName, slot.posX + 2, slot.posY + h + 2, 0xAAAAAA, false);
            }
            // 删除按钮
            int delX = slot.posX + w - 12;
            int delY = slot.posY;
            graphics.fill(delX, delY, delX + 10, delY + 10, 0xAAFF4444);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.display_terminal.delete_button"), delX + 2, delY + 1, 0xFFFFFF, false);
            // 旋转角度显示
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.display_terminal.rotation_prefix", (int)slot.rotation), slot.posX + w - 30, slot.posY + h + 2, 0xAAAAAA, false);
            // 颜色预览块
            int previewX = slot.posX + 2;
            int previewY = slot.posY + h - 10;
            int previewSize = 8;
            if (previewY < slot.posY) previewY = slot.posY;
            int previewColor = 0xFF000000 | (slot.color & 0x00FFFFFF);
            graphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, previewColor);
        }

        // 静态文本槽位
        for (StaticTextEntry entry : staticTexts) {
            int w = getStaticWidth(entry);
            int h = (int)(20 * entry.scale);
            graphics.fill(entry.posX, entry.posY, entry.posX + w, entry.posY + h, 0xAA336699);
            graphics.drawString(font, entry.text, entry.posX + 2, entry.posY + 2, 0xFFFFFF, false);
            int delX = entry.posX + w - 12;
            int delY = entry.posY;
            graphics.fill(delX, delY, delX + 10, delY + 10, 0xAAFF4444);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.display_terminal.delete_button"), delX + 2, delY + 1, 0xFFFFFF, false);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.display_terminal.rotation_prefix", (int)entry.rotation), entry.posX + w - 30, entry.posY + h + 2, 0xAAAAAA, false);
            int previewX = entry.posX + 2;
            int previewY = entry.posY + h - 10;
            int previewSize = 8;
            if (previewY < entry.posY) previewY = entry.posY;
            int previewColor = 0xFF000000 | (entry.color & 0x00FFFFFF);
            graphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, previewColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 颜色预览块点击检测
            for (SlotEntry slot : slots) {
                int w = getSlotWidth(slot);
                int h = (int)(20 * slot.scale);
                int previewX = slot.posX + 2;
                int previewY = slot.posY + h - 10;
                int previewSize = 8;
                if (previewY < slot.posY) previewY = slot.posY;
                if (mouseX >= previewX && mouseX <= previewX + previewSize && mouseY >= previewY && mouseY <= previewY + previewSize) {
                    openColorEditScreen(slot);
                    return true;
                }
            }
            for (StaticTextEntry entry : staticTexts) {
                int w = getStaticWidth(entry);
                int h = (int)(20 * entry.scale);
                int previewX = entry.posX + 2;
                int previewY = entry.posY + h - 10;
                int previewSize = 8;
                if (previewY < entry.posY) previewY = entry.posY;
                if (mouseX >= previewX && mouseX <= previewX + previewSize && mouseY >= previewY && mouseY <= previewY + previewSize) {
                    openColorEditScreen(entry);
                    return true;
                }
            }
        }

        // 删除和拖拽逻辑
        if (button == 0) {
            // 数据源删除按钮
            for (SlotEntry slot : slots) {
                int w = getSlotWidth(slot);
                int delX = slot.posX + w - 12;
                int delY = slot.posY;
                if (mouseX >= delX && mouseX <= delX + 10 && mouseY >= delY && mouseY <= delY + 10) {
                    PacketDistributor.sendToServer(new RemoveSlotPayload(terminalPos, slot.sourcePos));
                    slots.remove(slot);
                    return true;
                }
            }
            // 静态文本删除按钮
            for (StaticTextEntry entry : staticTexts) {
                int w = getStaticWidth(entry);
                int delX = entry.posX + w - 12;
                int delY = entry.posY;
                if (mouseX >= delX && mouseX <= delX + 10 && mouseY >= delY && mouseY <= delY + 10) {
                    PacketDistributor.sendToServer(new RemoveStaticTextPayload(terminalPos, staticTexts.indexOf(entry)));
                    staticTexts.remove(entry);
                    return true;
                }
            }
            // 数据源拖拽区域
            for (SlotEntry slot : slots) {
                int w = getSlotWidth(slot);
                int h = (int)(20 * slot.scale);
                if (mouseX >= slot.posX && mouseX <= slot.posX + w && mouseY >= slot.posY && mouseY <= slot.posY + h) {
                    draggingSlot = slot;
                    dragOffsetX = (int)(mouseX - slot.posX);
                    dragOffsetY = (int)(mouseY - slot.posY);
                    return true;
                }
            }
            // 静态文本拖拽区域
            for (StaticTextEntry entry : staticTexts) {
                int w = getStaticWidth(entry);
                int h = (int)(20 * entry.scale);
                if (mouseX >= entry.posX && mouseX <= entry.posX + w && mouseY >= entry.posY && mouseY <= entry.posY + h) {
                    draggingStatic = entry;
                    dragOffsetX = (int)(mouseX - entry.posX);
                    dragOffsetY = (int)(mouseY - entry.posY);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openColorEditScreen(Object entry) {
        Minecraft.getInstance().setScreen(new ColorEditScreen(entry, this));
    }

    public void updateSlotColorAndAlpha(Object entry, int color, int alpha) {
        if (entry instanceof SlotEntry) {
            ((SlotEntry) entry).color = color;
            ((SlotEntry) entry).alpha = alpha;
        } else if (entry instanceof StaticTextEntry) {
            ((StaticTextEntry) entry).color = color;
            ((StaticTextEntry) entry).alpha = alpha;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlot != null && button == 0) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            int w = getSlotWidth(draggingSlot);
            int h = (int)(20 * draggingSlot.scale);
            draggingSlot.posX = Math.min(Math.max(newX, 0), width - w);
            draggingSlot.posY = Math.min(Math.max(newY, 0), height - h);
            return true;
        }
        if (draggingStatic != null && button == 0) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            int w = getStaticWidth(draggingStatic);
            int h = (int)(20 * draggingStatic.scale);
            draggingStatic.posX = Math.min(Math.max(newX, 0), width - w);
            draggingStatic.posY = Math.min(Math.max(newY, 0), height - h);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlot = null;
        draggingStatic = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double scrollDelta = scrollY;
        boolean ctrl = Screen.hasControlDown();

        for (SlotEntry slot : slots) {
            int w = getSlotWidth(slot);
            int h = (int)(20 * slot.scale);
            if (mouseX >= slot.posX && mouseX <= slot.posX + w && mouseY >= slot.posY && mouseY <= slot.posY + h) {
                if (ctrl) {
                    float delta = (float)(scrollDelta > 0 ? 5f : -5f);
                    slot.rotation += delta;
                    slot.rotation %= 360;
                } else {
                    float newScale = slot.scale + (float)(scrollDelta > 0 ? 0.1f : -0.1f);
                    slot.scale = Math.min(Math.max(newScale, 0.5f), 2.0f);
                }
                return true;
            }
        }
        for (StaticTextEntry entry : staticTexts) {
            int w = getStaticWidth(entry);
            int h = (int)(20 * entry.scale);
            if (mouseX >= entry.posX && mouseX <= entry.posX + w && mouseY >= entry.posY && mouseY <= entry.posY + h) {
                if (ctrl) {
                    float delta = (float)(scrollDelta > 0 ? 5f : -5f);
                    entry.rotation += delta;
                    entry.rotation %= 360;
                } else {
                    float newScale = entry.scale + (float)(scrollDelta > 0 ? 0.1f : -0.1f);
                    entry.scale = Math.min(Math.max(newScale, 0.5f), 2.0f);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ========== 内部颜色编辑屏幕 ==========
    private class ColorEditScreen extends Screen {
        private final Object target;
        private final TerminalConfigScreen parent;
        private EditBox colorInput;
        private AbstractSliderButton alphaSlider;
        private Button confirmButton;
        private int currentColor, currentAlpha;

        protected ColorEditScreen(Object target, TerminalConfigScreen parent) {
            super(Component.translatable("gui.create_headsupdisplay.display_terminal.color_picker.title"));
            this.target = target;
            this.parent = parent;
            this.currentColor = getColorFromEntry(target);
            this.currentAlpha = getAlphaFromEntry(target);
        }

        @Override
        protected void init() {
            super.init();
            int centerX = width / 2;
            int centerY = height / 2;

            colorInput = new EditBox(font, centerX - 100, centerY - 20, 80, 20, Component.translatable("gui.create_headsupdisplay.display_terminal.color_picker.hex_hint"));
            colorInput.setMaxLength(6);
            colorInput.setValue(String.format("%06X", currentColor));
            addRenderableWidget(colorInput);

            alphaSlider = new AbstractSliderButton(centerX - 10, centerY - 20, 100, 20, Component.translatable("gui.create_headsupdisplay.display_terminal.alpha", currentAlpha), currentAlpha / 255.0) {
                @Override
                protected void updateMessage() {
                    setMessage(Component.translatable("gui.create_headsupdisplay.display_terminal.alpha", (int)(value * 255)));
                }
                @Override
                protected void applyValue() {
                    currentAlpha = (int)(value * 255);
                }
            };
            addRenderableWidget(alphaSlider);

            confirmButton = Button.builder(Component.translatable("gui.create_headsupdisplay.display_terminal.confirm"), b -> {
                try {
                    currentColor = Integer.parseInt(colorInput.getValue(), 16);
                } catch (NumberFormatException ignored) {}
                parent.updateSlotColorAndAlpha(target, currentColor, currentAlpha);
                Minecraft.getInstance().setScreen(parent);
            }).bounds(centerX - 20, centerY + 20, 40, 20).build();
            addRenderableWidget(confirmButton);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.display_terminal.color_picker.subtitle"), width / 2 - 70, height / 2 - 50, 0xFFFFFF, false);
        }
    }

    // 辅助方法
    private int getColorFromEntry(Object entry) {
        if (entry instanceof SlotEntry) return ((SlotEntry) entry).color;
        if (entry instanceof StaticTextEntry) return ((StaticTextEntry) entry).color;
        return 0xFFFFFF;
    }
    private int getAlphaFromEntry(Object entry) {
        if (entry instanceof SlotEntry) return ((SlotEntry) entry).alpha;
        if (entry instanceof StaticTextEntry) return ((StaticTextEntry) entry).alpha;
        return 255;
    }

    // 内部类
    private static class SlotEntry {
        final BlockPos sourcePos;
        int posX, posY;
        float scale;
        String text;
        int displayLine;
        float rotation;
        int color, alpha;
        String sourceName;
        SlotEntry(BlockPos sourcePos, int posX, int posY, float scale, String text, int displayLine, float rotation, int color, int alpha, String sourceName) {
            this.sourcePos = sourcePos;
            this.posX = posX;
            this.posY = posY;
            this.scale = scale;
            this.text = text;
            this.displayLine = displayLine;
            this.rotation = rotation;
            this.color = color;
            this.alpha = alpha;
            this.sourceName = sourceName;
        }
    }

    private static class StaticTextEntry {
        String text;
        int posX, posY;
        float scale;
        float rotation;
        int color, alpha;
        StaticTextEntry(String text, int posX, int posY, float scale, float rotation, int color, int alpha) {
            this.text = text;
            this.posX = posX;
            this.posY = posY;
            this.scale = scale;
            this.rotation = rotation;
            this.color = color;
            this.alpha = alpha;
        }
    }
}