package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class TranslationScreen extends Screen {
    private final Consumer<TranslationConfig> onSave;
    private TranslationConfig config;
    private EditBox exprInput;

    public TranslationScreen(TranslationConfig current, Consumer<TranslationConfig> onSave) {
        super(Component.translatable("gui.create_headsupdisplay.translation.title"));
        this.config = current != null ? current : new TranslationConfig();
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2 - 100;

        addRenderableWidget(Button.builder(Component.literal(modeLabel()), b -> {
            config.setMode(TranslationConfig.Mode.values()[(config.getMode().ordinal() + 1) % 3]);
            if (config.getMode() == TranslationConfig.Mode.CONDITIONAL) ensureRules();
            rebuild();
        }).bounds(cx, 10, 200, 20).build());

        if (config.getMode() == TranslationConfig.Mode.EXPRESSION) {
            exprInput = new EditBox(font, cx, 45, 200, 20, Component.literal("y = ..."));
            exprInput.setMaxLength(64);
            exprInput.setValue(config.getExpression().isEmpty() ? "3*x+6" : config.getExpression());
            addRenderableWidget(exprInput);
        }

        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL) {
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
        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL) {
            ensureRules();
            rebuildConditionRows();
        }
    }

    private final java.util.List<CondRow> condRows = new java.util.ArrayList<>();
    private Button addElseIfBtn;

    private static class CondRow {
        EditBox valBox, outBox;
        Button opBtn, delBtn;
    }

    private void clearCondWidgets() {
        for (var row : condRows) {
            if (row.opBtn != null) removeWidget(row.opBtn);
            if (row.valBox != null) removeWidget(row.valBox);
            if (row.outBox != null) removeWidget(row.outBox);
            if (row.delBtn != null) removeWidget(row.delBtn);
        }
        condRows.clear();
        if (addElseIfBtn != null) { removeWidget(addElseIfBtn); addElseIfBtn = null; }
    }

    private void rebuildConditionRows() {
        clearCondWidgets();
        var rules = config.getRules();
        int cx = width / 2 - 130;
        // 最后一行是 else 输出
        int last = rules.size() - 1;

        for (int i = 0; i < rules.size(); i++) {
            int y = 45 + i * 30;
            var rule = rules.get(i);
            boolean isElse = (i == last);
            CondRow row = new CondRow();

            if (isElse) {
                row.opBtn = addRenderableWidget(Button.builder(Component.literal((i == 0) ? "Else" : "Else"), b -> {})
                        .bounds(cx, y, 80, 20).build());
                row.outBox = new EditBox(font, cx + 85, y, 150, 20, Component.literal("out"));
                row.outBox.setMaxLength(32); row.outBox.setValue(rule.output);
                addRenderableWidget(row.outBox);
            } else {
                String label = (i == 0) ? "If" : "ElsIf";
                final int idx = i;
                row.opBtn = addRenderableWidget(Button.builder(Component.literal(label + " " + opStr(rule.op)), b -> {
                    rule.op = TranslationConfig.ConditionalRule.Op.values()[(rule.op.ordinal() + 1) % 6];
                    b.setMessage(Component.literal(label + " " + opStr(rule.op)));
                }).bounds(cx, y, 80, 20).build());

                row.valBox = new EditBox(font, cx + 85, y, 40, 20, Component.literal("val"));
                row.valBox.setMaxLength(6); row.valBox.setValue(String.valueOf(rule.compareValue));
                addRenderableWidget(row.valBox);

                row.outBox = new EditBox(font, cx + 130, y, 100, 20, Component.literal("out"));
                row.outBox.setMaxLength(32); row.outBox.setValue(rule.output);
                addRenderableWidget(row.outBox);

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
            addElseIfBtn = addRenderableWidget(Button.builder(Component.literal("+ Else If"), b -> {
                collectRowData();
                rules.add(rules.size() - 1, new TranslationConfig.ConditionalRule(TranslationConfig.ConditionalRule.Op.EQUAL, 0, ""));
                rebuildConditionRows();
            }).bounds(cx, btnY, 70, 20).build());
        }
    }

    private void collectRowData() {
        var rules = config.getRules();
        for (int i = 0; i < condRows.size() && i < rules.size(); i++) {
            var row = condRows.get(i);
            var rule = rules.get(i);
            if (rule != null && row.outBox != null) rule.output = row.outBox.getValue();
            if (rule != null && row.valBox != null) {
                try { rule.compareValue = Integer.parseInt(row.valBox.getValue()); } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void saveCurrent() {
        if (config.getMode() == TranslationConfig.Mode.EXPRESSION && exprInput != null) {
            config.setExpression(exprInput.getValue());
        }
        if (config.getMode() == TranslationConfig.Mode.CONDITIONAL) {
            collectRowData();
        }
    }

    private String modeLabel() {
        return switch (config.getMode()) {
            case NONE -> "Mode: Off";
            case EXPRESSION -> "Mode: Expression";
            case CONDITIONAL -> "Mode: Conditional";
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
    }

    @Override
    public void onClose() { saveCurrent(); super.onClose(); }
    @Override
    public boolean isPauseScreen() { return false; }
}