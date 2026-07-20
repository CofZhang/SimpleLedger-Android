package com.simpleledger.app;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.5 表达式求值工具类：支持 + - * / 和括号的表达式计算，
 * 例如 "120*3 + 50" -> 410.0
 * 不依赖任何外部库，使用递归下降解析器实现。
 */
public class ExpressionEvaluator {

    /** 表达式是否合法（仅包含数字、+ - * / ( ) 和小数点、空格） */
    public static boolean isValidExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) return false;
        for (char c : expr.toCharArray()) {
            if (!(Character.isDigit(c) || c == '.' || c == '+'
                    || c == '-' || c == '*' || c == '/' || c == '('
                    || c == ')' || c == ' ' || c == ',')) {
                return false;
            }
        }
        // 简单校验括号匹配
        int depth = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth < 0) return false;
            }
        }
        return depth == 0;
    }

    /** 判断是否为表达式（包含运算符） */
    public static boolean isExpression(String text) {
        if (text == null) return false;
        String trimmed = text.trim().replace(",", "");
        return trimmed.contains("+") || trimmed.contains("-")
                || trimmed.contains("*") || trimmed.contains("/")
                || trimmed.contains("(");
    }

    /** 计算表达式，失败时返回 Double.NaN */
    public static double evaluate(String expr) {
        if (expr == null || expr.trim().isEmpty()) return Double.NaN;
        String cleaned = expr.trim().replace(",", "").replace(" ", "");
        if (cleaned.isEmpty()) return Double.NaN;
        try {
            Parser parser = new Parser(cleaned);
            double result = parser.parseExpression();
            if (parser.pos != parser.chars.length) return Double.NaN;
            return result;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** 将纯数字字符串格式化为千分位形式（保留两位小数）：1000 -> 1,000.00 */
    public static String formatWithThousands(double value) {
        boolean isNeg = value < 0;
        double abs = Math.abs(value);
        long intPart = (long) abs;
        int fracPart = (int) Math.round((abs - intPart) * 100);
        String intStr = String.valueOf(intPart);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intStr.length(); i++) {
            if (i > 0 && (intStr.length() - i) % 3 == 0) {
                sb.append(",");
            }
            sb.append(intStr.charAt(i));
        }
        if (isNeg) sb.insert(0, "-");
        return sb.toString() + "." + (fracPart < 10 ? "0" + fracPart : fracPart);
    }

    /**
     * 实时格式化输入：将当前输入的数字部分加上千分位（不破坏正在输入的表达式）。
     * 例如 "1200+3" -> "1,200+3"
     */
    public static String formatLive(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        StringBuilder currentNum = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                currentNum.append(c);
            } else {
                if (currentNum.length() > 0) {
                    result.append(formatNumberSegment(currentNum.toString()));
                    currentNum.setLength(0);
                }
                result.append(c);
            }
        }
        if (currentNum.length() > 0) {
            result.append(formatNumberSegment(currentNum.toString()));
        }
        return result.toString();
    }

    /** 格式化单个数字段（不带小数部分千分位） */
    private static String formatNumberSegment(String num) {
        if (num == null || num.isEmpty()) return num;
        if (num.contains(".")) {
            int dotIdx = num.indexOf('.');
            String intPart = num.substring(0, dotIdx);
            String fracPart = num.substring(dotIdx);
            return addThousandsSeparator(intPart) + fracPart;
        }
        return addThousandsSeparator(num);
    }

    private static String addThousandsSeparator(String intStr) {
        if (intStr.length() <= 3) return intStr;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intStr.length(); i++) {
            if (i > 0 && (intStr.length() - i) % 3 == 0) {
                sb.append(",");
            }
            sb.append(intStr.charAt(i));
        }
        return sb.toString();
    }

    /** 递归下降解析器 */
    private static class Parser {
        final char[] chars;
        int pos;

        Parser(String s) {
            this.chars = s.toCharArray();
            this.pos = 0;
        }

        void skipSpaces() {
            while (pos < chars.length && Character.isWhitespace(chars[pos])) pos++;
        }

        double parseExpression() {
            double v = parseTerm();
            skipSpaces();
            while (pos < chars.length && (chars[pos] == '+' || chars[pos] == '-')) {
                char op = chars[pos++];
                double t = parseTerm();
                if (op == '+') v += t; else v -= t;
                skipSpaces();
            }
            return v;
        }

        double parseTerm() {
            double v = parseFactor();
            skipSpaces();
            while (pos < chars.length && (chars[pos] == '*' || chars[pos] == '/')) {
                char op = chars[pos++];
                double f = parseFactor();
                if (op == '*') v *= f;
                else {
                    if (f == 0) throw new ArithmeticException("divide by zero");
                    v /= f;
                }
                skipSpaces();
            }
            return v;
        }

        double parseFactor() {
            skipSpaces();
            if (pos >= chars.length) throw new RuntimeException("unexpected end");
            if (chars[pos] == '(') {
                pos++;
                double v = parseExpression();
                skipSpaces();
                if (pos < chars.length && chars[pos] == ')') pos++;
                return v;
            }
            if (chars[pos] == '-') {
                pos++;
                return -parseFactor();
            }
            if (chars[pos] == '+') {
                pos++;
                return parseFactor();
            }
            int start = pos;
            while (pos < chars.length && (Character.isDigit(chars[pos]) || chars[pos] == '.')) {
                pos++;
            }
            if (start == pos) throw new RuntimeException("number expected at " + pos);
            return Double.parseDouble(new String(chars, start, pos - start));
        }
    }
}
