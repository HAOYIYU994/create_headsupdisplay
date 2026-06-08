package com.github.haoyiyu.create_headsupdisplay.config;

import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 转译配置：支持无转译、表达式型、条件型、图片逻辑型四种模式。
 */
public class TranslationConfig {
    public enum Mode { NONE, EXPRESSION, CONDITIONAL, IMAGE_CONDITIONAL }

    private Mode mode = Mode.NONE;
    private String expression = "";  // 表达式型：如 "3*x+6"
    private List<ConditionalRule> rules = new ArrayList<>(); // 条件型规则列表

    // ===== 条件型规则 =====
    public static class ConditionalRule {
        public enum Op { GREATER, GREATER_EQ, EQUAL, LESS_EQ, LESS, NOT_EQUAL }
        public Op op = Op.EQUAL;
        public int compareValue;
        public String output = "";
        public String imageName = ""; // IMAGE_CONDITIONAL 模式专用

        public ConditionalRule() {}
        public ConditionalRule(Op op, int val, String out) { this.op = op; this.compareValue = val; this.output = out; }
        public ConditionalRule(Op op, int val, String out, String imgName) { this.op = op; this.compareValue = val; this.output = out; this.imageName = imgName; }
    }

    public TranslationConfig() {}

    public Mode getMode() { return mode; }
    public void setMode(Mode m) { this.mode = m; }
    public String getExpression() { return expression; }
    public void setExpression(String expr) { this.expression = expr; }
    public List<ConditionalRule> getRules() { return rules; }

    /** 返回转译后的显示文本。null 表示无转译。 */
    public String getDisplay(int input) {
        if (mode == Mode.EXPRESSION && !expression.isEmpty()) {
            return String.valueOf(evaluateExpression(expression, input));
        }
        if (mode == Mode.CONDITIONAL && !rules.isEmpty()) {
            int last = rules.size() - 1;
            for (int i = 0; i < rules.size(); i++) {
                ConditionalRule rule = rules.get(i);
                if (i == last || checkCondition(input, rule)) {
                    String out = rule.output.trim();
                    return out.isEmpty() ? String.valueOf(input) : out;
                }
            }
        }
        return null;
    }

    /** 图片逻辑型：根据输入值返回选中图片的源名称。null 表示未匹配。 */
    public String getSelectedImage(int input) {
        if (mode == Mode.IMAGE_CONDITIONAL && !rules.isEmpty()) {
            int last = rules.size() - 1;
            for (int i = 0; i < rules.size(); i++) {
                ConditionalRule rule = rules.get(i);
                if (i == last || checkCondition(input, rule)) {
                    return rule.imageName.isEmpty() ? null : rule.imageName;
                }
            }
        }
        return null;
    }

    // ===== 引用语法支持 =====
    private static final Pattern REF_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /** 检查文本是否包含 \${...} 引用占位符 */
    public static boolean hasReferences(String text) {
        return text != null && REF_PATTERN.matcher(text).find();
    }

    /**
     * 解析文本中的 \${名称} 引用占位符，使用 resolver 获取替换值。
     * 名称不存在时保留原 \${名称} 不变。
     */
    public static String resolveReferences(String text, Function<String, String> resolver) {
        if (text == null || text.isEmpty()) return text;
        Matcher m = REF_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String refName = m.group(1);
            String replacement = resolver.apply(refName);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement != null ? replacement : m.group(0)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean checkCondition(int input, ConditionalRule rule) {
        return switch (rule.op) {
            case GREATER     -> input >  rule.compareValue;
            case GREATER_EQ  -> input >= rule.compareValue;
            case EQUAL       -> input == rule.compareValue;
            case LESS_EQ     -> input <= rule.compareValue;
            case LESS        -> input <  rule.compareValue;
            case NOT_EQUAL   -> input != rule.compareValue;
        };
    }

    /** 简易表达式求值：支持 + - * / ^ ( ) 和变量 x，返回整数 */
    private int evaluateExpression(String expr, int x) {
        try {
            String s = expr.replaceAll(" ", "");
            if (s.startsWith("y=")) s = s.substring(2);
            return (int) Math.round(parseExpr(s, x));
        } catch (Exception e) {
            return x;
        }
    }

    // ---- 递归下降解析器 ----
    private int pos;
    private String src;
    private int varX;

    private double parseExpr(String s, int x) {
        this.src = s; this.pos = 0; this.varX = x;
        double val = parseAddSub();
        if (pos != src.length()) throw new RuntimeException("Unexpected: " + src.charAt(pos));
        return val;
    }

    private double parseAddSub() {
        double left = parseMulDiv();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '+') { pos++; left += parseMulDiv(); }
            else if (c == '-') { pos++; left -= parseMulDiv(); }
            else break;
        }
        return left;
    }

    private double parseMulDiv() {
        double left = parsePower();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '*') { pos++; left *= parsePower(); }
            else if (c == '/') { pos++; double d = parsePower(); if (d != 0) left /= d; }
            else break;
        }
        return left;
    }

    private double parsePower() {
        double left = parseUnary();
        while (pos < src.length() && src.charAt(pos) == '^') {
            pos++; left = Math.pow(left, parseUnary());
        }
        return left;
    }

    private double parseUnary() {
        if (pos < src.length() && src.charAt(pos) == '-') {
            pos++; return -parseAtom();
        }
        return parseAtom();
    }

    private double parseAtom() {
        if (pos >= src.length()) throw new RuntimeException("Unexpected end");
        char c = src.charAt(pos);
        if (c == '(') {
            pos++; double val = parseAddSub();
            if (pos < src.length() && src.charAt(pos) == ')') pos++;
            return val;
        }
        if (c == 'x' || c == 'X') { pos++; return varX; }
        if (c == 'y' || c == 'Y') { pos++; return 0; } // y 作为表达式左侧不可引用
        if (Character.isDigit(c) || c == '.') {
            int start = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
            return Double.parseDouble(src.substring(start, pos));
        }
        throw new RuntimeException("Unexpected char: " + c);
    }

    // ===== 序列化 =====
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("mode", mode.ordinal());
        tag.putString("expr", expression);
        List<CompoundTag> rulesTag = new ArrayList<>();
        for (ConditionalRule r : rules) {
            CompoundTag rt = new CompoundTag();
            rt.putInt("op", r.op.ordinal());
            rt.putInt("val", r.compareValue);
            rt.putString("out", r.output);
            if (!r.imageName.isEmpty()) rt.putString("img", r.imageName);
            rulesTag.add(rt);
        }
        tag.putInt("ruleCount", rulesTag.size());
        for (int i = 0; i < rulesTag.size(); i++) tag.put("rule_" + i, rulesTag.get(i));
        return tag;
    }

    public static TranslationConfig deserialize(CompoundTag tag) {
        TranslationConfig tc = new TranslationConfig();
        tc.mode = Mode.values()[tag.getInt("mode")];
        tc.expression = tag.getString("expr");
        int count = tag.getInt("ruleCount");
        for (int i = 0; i < count; i++) {
            CompoundTag rt = tag.getCompound("rule_" + i);
            tc.rules.add(new ConditionalRule(ConditionalRule.Op.values()[rt.getInt("op")], rt.getInt("val"), rt.getString("out"),
                    rt.contains("img") ? rt.getString("img") : ""));
        }
        return tc;
    }
}