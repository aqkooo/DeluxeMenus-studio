package com.ejyqyl.dmstudio.service.simulator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline, safe evaluator for DeluxeMenus JavaScript and comparator requirements (§Play Mode).
 */
public class JavascriptRequirementEngine {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
            "^\\s*(.*?)\\s*(===|!==|==|!=|>=|<=|>|<)\\s*(.*?)\\s*$"
    );

    /**
     * Evaluates a comparator requirement (type: >, >=, <, <=, ==, !=).
     */
    public static boolean evaluateComparator(String operator, String input, String output) {
        if (operator == null) return false;
        String op = operator.trim();
        String in = input != null ? input.trim() : "";
        String out = output != null ? output.trim() : "";

        // Try numeric comparison first
        try {
            double inNum = Double.parseDouble(in);
            double outNum = Double.parseDouble(out);
            return switch (op) {
                case ">" -> inNum > outNum;
                case ">=" -> inNum >= outNum;
                case "<" -> inNum < outNum;
                case "<=" -> inNum <= outNum;
                case "==", "===" -> Math.abs(inNum - outNum) < 1e-9;
                case "!=", "!==" -> Math.abs(inNum - outNum) >= 1e-9;
                default -> false;
            };
        } catch (NumberFormatException ignored) {
            // Fallback to string comparison
            int cmp = in.compareTo(out);
            return switch (op) {
                case "==", "===" -> in.equals(out);
                case "!=", "!==" -> !in.equals(out);
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                default -> false;
            };
        }
    }

    /**
     * Evaluates an expression with player placeholder replacements.
     */
    public static boolean evaluate(String expression, SimulatedPlayerState player) {
        if (expression == null || expression.isBlank()) return false;
        String resolved = player != null ? player.replacePlaceholders(expression) : expression;
        return evaluateExpression(resolved);
    }

    /**
     * Evaluates a JavaScript condition expression string offline.
     * Supports boolean combinations (&&, ||, !), comparisons, and parentheses.
     */
    public static boolean evaluateExpression(String expression) {
        if (expression == null || expression.isBlank()) return false;
        try {
            String expr = expression.trim();
            if (expr.toLowerCase().startsWith("javascript:")) {
                expr = expr.substring(11).trim();
            } else if (expr.toLowerCase().startsWith("js:")) {
                expr = expr.substring(3).trim();
            }
            return evalOr(expr);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean evalOr(String expr) {
        List<String> parts = splitTopLevel(expr, "||");
        for (String part : parts) {
            if (evalAnd(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean evalAnd(String expr) {
        List<String> parts = splitTopLevel(expr, "&&");
        for (String part : parts) {
            if (!evalAtom(part.trim())) {
                return false;
            }
        }
        return true;
    }

    private static boolean evalAtom(String expr) {
        String s = expr.trim();
        if (s.isEmpty()) return false;

        // Handle NOT
        if (s.startsWith("!") && !s.startsWith("!=")) {
            return !evalAtom(s.substring(1).trim());
        }

        // Handle surrounding parentheses
        if (s.startsWith("(") && s.endsWith(")")) {
            // Ensure parentheses match whole expression
            int depth = 0;
            boolean wrapsAll = true;
            for (int i = 0; i < s.length() - 1; i++) {
                char c = s.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                if (depth == 0) {
                    wrapsAll = false;
                    break;
                }
            }
            if (wrapsAll) {
                return evalOr(s.substring(1, s.length() - 1).trim());
            }
        }

        // True / False literals
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;

        // Comparison
        Matcher matcher = COMPARISON_PATTERN.matcher(s);
        if (matcher.matches()) {
            if (matcher.group(1).isBlank() || matcher.group(3).isBlank()) {
                return false;
            }
            String left = stripQuotes(matcher.group(1));
            String op = matcher.group(2);
            String right = stripQuotes(matcher.group(3));
            return evaluateComparator(op, left, right);
        }

        // Quoted string literal
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return !stripQuotes(s).isEmpty();
        }

        // Numeric literal
        try {
            double val = Double.parseDouble(s);
            return val != 0.0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static List<String> splitTopLevel(String expr, String delimiter) {
        List<String> tokens = new ArrayList<>();
        int depth = 0;
        int last = 0;
        int dLen = delimiter.length();

        for (int i = 0; i <= expr.length() - dLen; i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && expr.startsWith(delimiter, i)) {
                tokens.add(expr.substring(last, i));
                last = i + dLen;
                i += dLen - 1;
            }
        }
        tokens.add(expr.substring(last));
        return tokens;
    }

    private static String stripQuotes(String val) {
        if (val == null) return "";
        String s = val.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
