package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class TranslationScreen extends Screen {
    private final Consumer<TranslationConfig> onSave;
    private TranslationConfig config;
    private EditBox exprInput;
    private final List<String> availableSourceNames;
    private final List<String> availableImageNames;

    public TranslationScreen(TranslationConfig current, Consumer<TranslationConfig> onSave, List<String> availableSourceNames, List<String> availableImageNames) {
        super(Component.translatable("gui.create_headsupdisplay.translation.title"));
        this.config = current != null ? current : new TranslationConfig();
        this.onSave = onSave;
        this.availableSourceNames = availableSourceNames != null ? availableSourceNames : List.of();
        this.availableImageNames = availableImageNames != null ? availableImageNames : List.of();
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2 - 100;

        addRenderableWidget(Button.builder(modeLabel(), b -> {
            config.setMode(TranslationConfig.Mode.values()[(config.getMode().ordinal() + 1) % 4]);
            if (config.getMode() == TranslationConfig.Mode.CONDITIONAL || config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) ensureRules();
            b.setMessage(modeLabel());
            rebuild();
        }).bounds(cx, 10, 200, 20).build());

        if (config.getMode() == TranslationConfig.Mode.EXPRESSION) {
            exprInput = new EditBox(font, cx, 45, 200, 20, Component.literal("y = ..."));
            exprInput.setMaxLength(64);
            exprInput.setValue(config.getExpression().isEmpty() ? "3*x+6" : config.getExpression());
            addRenderableWidget(exprInput);
        }

        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL || config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
            ensureRules();
            rebuildConditionRows();
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.display_terminal.save"), b -> {
            saveCurrent();
            onSave.accept(config);
            onClose();
        }).bounds(cx, height - 30, 90, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx + 110, height - 30, 90, 20).build());
    }

    private void ensureRules() {
        var r = config.getRules();
        if (r.isEmpty()) {
            r.add(new TranslationConfig.ConditionalRule(TranslationConfig.ConditionalRule.Op.EQUAL, 0, ""));
            r.add(new TranslationConfig.ConditionalRule(TranslationConfig.ConditionalRule.Op.EQUAL, 0, ""));
        }
    }

    private void rebuild() {
        if (exprInput != null) { removeWidget(exprInput); exprInput = null; }
        clearCondWidgets();
        if (config.getMode() == TranslationConfig.Mode.EXPRESSION) {
            exprInput = new EditBox(font, width / 2 - 100, 45, 200, 20, Component.literal("y = ..."));
            exprInput.setMaxLength(64); exprInput.setValue(config.getExpression());
            addRenderableWidget(exprInput);
        }
        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL || config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
            ensureRules();
            rebuildConditionRows();
        }
    }

    private final java.util.List<CondRow> condRows = new java.util.ArrayList<>();
    private Button addElseIfBtn;

    private static class CondRow {
        EditBox valBox, outBox;
        Button opBtn, delBtn, imgBtn;
        int ruleIndex;
    }

    private void clearCondWidgets() {
        for (var row : condRows) {
            if (row.opBtn != null) removeWidget(row.opBtn);
            if (row.valBox != null) removeWidget(row.valBox);
            if (row.outBox != null) removeWidget(row.outBox);
            if (row.delBtn != null) removeWidget(row.delBtn);
            if (row.imgBtn != null) removeWidget(row.imgBtn);
        }
        condRows.clear();
        if (addElseIfBtn != null) { removeWidget(addElseIfBtn); addElseIfBtn = null; }
    }

    private void rebuildConditionRows() {
        clearCondWidgets();
        var rules = config.getRules();
        int cx = width / 2 - 130;
        boolean isImageMode = config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL;
        // 最后一行是 else 输出
        int last = rules.size() - 1;

        for (int i = 0; i < rules.size(); i++) {
            int y = 45 + i * 30;
            var rule = rules.get(i);
            boolean isElse = (i == last);
            CondRow row = new CondRow();
            row.ruleIndex = i;

            if (isElse) {
                row.opBtn = addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.translation.else"), b -> {})
                        .bounds(cx, y, 80, 20).build());
                if (isImageMode) {
                    final int ri = i;
                    row.imgBtn = addRenderableWidget(Button.builder(
                            Component.literal(rule.imageName.isEmpty()
                                ? Component.translatable("gui.create_headsupdisplay.translation.pick_image").getString()
                                : rule.imageName), b -> openImagePicker(ri)
                            ).bounds(cx + 85, y, 150, 20).build());
                } else {
                    row.outBox = new EditBox(font, cx + 85, y, 150, 20, Component.literal("out"));
                    row.outBox.setMaxLength(32); row.outBox.setValue(rule.output);
                    addRenderableWidget(row.outBox);
                }
            } else {
                final boolean isFirst = (i == 0);
                final int idx = i;
                Component label = isFirst
                    ? Component.translatable("gui.create_headsupdisplay.translation.if")
                    : Component.translatable("gui.create_headsupdisplay.translation.else_if");
                row.opBtn = addRenderableWidget(Button.builder(
                        Component.literal(label.getString() + " " + opStr(rule.op)), b -> {
                    rule.op = TranslationConfig.ConditionalRule.Op.values()[(rule.op.ordinal() + 1) % 6];
                    b.setMessage(Component.literal(label.getString() + " " + opStr(rule.op)));
                }).bounds(cx, y, 80, 20).build());

                row.valBox = new EditBox(font, cx + 85, y, 40, 20, Component.literal("val"));
                row.valBox.setMaxLength(6); row.valBox.setValue(String.valueOf(rule.compareValue));
                addRenderableWidget(row.valBox);

                if (isImageMode) {
                    final int ri = i;
                    row.imgBtn = addRenderableWidget(Button.builder(
                            Component.literal(rule.imageName.isEmpty()
                                ? Component.translatable("gui.create_headsupdisplay.translation.pick_image").getString()
                                : rule.imageName), b -> openImagePicker(ri)
                            ).bounds(cx + 130, y, 100, 20).build());
                } else {
                    row.outBox = new EditBox(font, cx + 130, y, 100, 20, Component.literal("out"));
                    row.outBox.setMaxLength(32); row.outBox.setValue(rule.output);
                    addRenderableWidget(row.outBox);
                }

                if (rules.size() > 2) {
                    row.delBtn = addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                        collectRowData();
                        rules.remove(idx);
                        rebuildConditionRows();
                    }).bounds(cx + 235, y, 20, 20).build());
                }
            }
            condRows.add(row);
        }

        if (rules.size() < 10) {
            int btnY = 45 + rules.size() * 30 + 5;
            addElseIfBtn = addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.translation.add_else_if"), b -> {
                collectRowData();
                rules.add(rules.size() - 1, new TranslationConfig.ConditionalRule(TranslationConfig.ConditionalRule.Op.EQUAL, 0, ""));
                rebuildConditionRows();
            }).bounds(cx, btnY, 70, 20).build());
        }
    }

    private void openImagePicker(int ruleIndex) {
        collectRowData();
        var rules = config.getRules();
        if (ruleIndex < 0 || ruleIndex >= rules.size()) return;
        minecraft.setScreen(new ImageSelectScreen(
            availableImageNames,
            rules.get(ruleIndex).imageName,
            selected -> {
                rules.get(ruleIndex).imageName = selected;
                minecraft.setScreen(this);
                rebuildConditionRows();
            }
        ));
    }

    private void collectRowData() {
        var rules = config.getRules();
        boolean isImageMode = config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL;
        for (int i = 0; i < condRows.size() && i < rules.size(); i++) {
            var row = condRows.get(i);
            var rule = rules.get(i);
            if (rule != null && row.outBox != null && !isImageMode) rule.output = row.outBox.getValue();
            if (rule != null && row.valBox != null) {
                try { rule.compareValue = Integer.parseInt(row.valBox.getValue()); } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void saveCurrent() {
        if (config.getMode() == TranslationConfig.Mode.EXPRESSION && exprInput != null) {
            config.setExpression(exprInput.getValue());
        }
        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL || config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL) {
            collectRowData();
        }
    }

    private Component modeLabel() {
        return switch (config.getMode()) {
            case NONE -> Component.translatable("gui.create_headsupdisplay.translation.mode_off");
            case EXPRESSION -> Component.translatable("gui.create_headsupdisplay.translation.mode_expression");
            case CONDITIONAL -> Component.translatable("gui.create_headsupdisplay.translation.mode_conditional");
            case IMAGE_CONDITIONAL -> Component.translatable("gui.create_headsupdisplay.translation.mode_image_conditional");
        };
    }

    private static String opStr(TranslationConfig.ConditionalRule.Op op) {
        return switch (op) {
            case GREATER -> ">"; case GREATER_EQ -> ">="; case EQUAL -> "=";
            case LESS_EQ -> "<="; case LESS -> "<"; case NOT_EQUAL -> "!=";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);
        if (config.getMode() == TranslationConfig.Mode.EXPRESSION) {
            g.drawString(font, "y = f(x) :", width / 2 - 100, 35, 0xFFFFFF);
        }
        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL && !availableSourceNames.isEmpty()) {
            String hint = Component.translatable("gui.create_headsupdisplay.translation.refs_hint",
                    String.join(", ", availableSourceNames)).getString();
            g.drawString(font, hint, width / 2 - 100, 35, 0xAAAAAA);
        }
        if (config.getMode() == TranslationConfig.Mode.IMAGE_CONDITIONAL && !availableImageNames.isEmpty()) {
            String hint = Component.translatable("gui.create_headsupdisplay.translation.images_hint",
                    String.join(", ", availableImageNames)).getString();
            g.drawString(font, hint, width / 2 - 100, 35, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() { saveCurrent(); super.onClose(); }
    @Override
    public boolean isPauseScreen() { return false; }

    // ===== 图片选择弹窗 =====
    private static class ImageSelectScreen extends Screen {
        private final List<String> images;
        private final String current;
        private final java.util.function.Consumer<String> callback;

        ImageSelectScreen(List<String> images, String current, java.util.function.Consumer<String> callback) {
            super(Component.translatable("gui.create_headsupdisplay.translation.select_image"));
            this.images = images;
            this.current = current != null ? current : "";
            this.callback = callback;
        }

        @Override
        protected void init() {
            int cx = width / 2 - 100;
            int y = 30;
            // "None" 选项
            addRenderableWidget(Button.builder(
                    Component.literal(current.isEmpty() ? "> [None] <" : "  [None]"),
                    b -> callback.accept("")
            ).bounds(cx, y, 200, 20).build());
            // 各图片选项
            for (int i = 0; i < images.size(); i++) {
                y += 22;
                final String name = images.get(i);
                boolean sel = name.equals(current);
                addRenderableWidget(Button.builder(
                        Component.literal(sel ? ("> " + name + " <") : ("  " + name)),
                        b -> callback.accept(name)
                ).bounds(cx, y, 200, 20).build());
            }
            y += 30;
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
                    b -> callback.accept(current) // 取消，保持原值
            ).bounds(cx, y, 200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            renderBackground(g, mx, my, pt);
            super.render(g, mx, my, pt);
        }

        @Override
        public boolean isPauseScreen() { return false; }
    }
}