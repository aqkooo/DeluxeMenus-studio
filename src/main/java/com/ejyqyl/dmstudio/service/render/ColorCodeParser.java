package com.ejyqyl.dmstudio.service.render;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Minecraft color and formatting codes (&amp;0-&amp;f, &amp;l/o/n/m/k/r, &amp;#rrggbb)
 * into styled JavaFX Text nodes.
 */
public final class ColorCodeParser {

    private static final Map<Character, String> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put('0', "#000000"); // Black
        COLOR_MAP.put('1', "#0000AA"); // Dark Blue
        COLOR_MAP.put('2', "#00AA00"); // Dark Green
        COLOR_MAP.put('3', "#00AAAA"); // Dark Aqua
        COLOR_MAP.put('4', "#AA0000"); // Dark Red
        COLOR_MAP.put('5', "#AA00AA"); // Dark Purple
        COLOR_MAP.put('6', "#FFAA00"); // Gold
        COLOR_MAP.put('7', "#AAAAAA"); // Gray
        COLOR_MAP.put('8', "#555555"); // Dark Gray
        COLOR_MAP.put('9', "#5555FF"); // Blue
        COLOR_MAP.put('a', "#55FF55"); // Green
        COLOR_MAP.put('b', "#55FFFF"); // Aqua
        COLOR_MAP.put('c', "#FF5555"); // Red
        COLOR_MAP.put('d', "#FF55FF"); // Light Purple
        COLOR_MAP.put('e', "#FFFF55"); // Yellow
        COLOR_MAP.put('f', "#FFFFFF"); // White
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(&#[0-9a-fA-F]{6}|&[0-9a-fk-orA-FK-OR])");

    public static String stripColor(String input) {
        if (input == null) return "";
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static List<Text> parseToNodes(String input) {
        return parseToNodes(input, Color.web("#FFFFFF"));
    }

    public static List<Text> parseToNodes(String input, Color defaultColor) {
        List<Text> result = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return result;
        }

        Color currentColor = defaultColor;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;

        int i = 0;
        int len = input.length();
        StringBuilder buffer = new StringBuilder();

        while (i < len) {
            // Check for hex color: &#rrggbb
            if (i + 7 < len && input.charAt(i) == '&' && input.charAt(i + 1) == '#') {
                Matcher matcher = HEX_PATTERN.matcher(input.substring(i, i + 8));
                if (matcher.matches()) {
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    try {
                        currentColor = Color.web("#" + matcher.group(1));
                    } catch (Exception ignored) {}
                    i += 8;
                    continue;
                }
            }

            // Check for standard formatting code: &x
            if (i + 1 < len && input.charAt(i) == '&') {
                char code = Character.toLowerCase(input.charAt(i + 1));
                if (COLOR_MAP.containsKey(code)) {
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    currentColor = Color.web(COLOR_MAP.get(code));
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    i += 2;
                    continue;
                } else if (code == 'l') { // Bold
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    bold = true;
                    i += 2;
                    continue;
                } else if (code == 'o') { // Italic
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    italic = true;
                    i += 2;
                    continue;
                } else if (code == 'n') { // Underline
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    underline = true;
                    i += 2;
                    continue;
                } else if (code == 'm') { // Strikethrough
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    strikethrough = true;
                    i += 2;
                    continue;
                } else if (code == 'r') { // Reset
                    if (buffer.length() > 0) {
                        result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
                        buffer.setLength(0);
                    }
                    currentColor = defaultColor;
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    i += 2;
                    continue;
                }
            }

            buffer.append(input.charAt(i));
            i++;
        }

        if (buffer.length() > 0) {
            result.add(createText(buffer.toString(), currentColor, bold, italic, underline, strikethrough));
        }

        return result;
    }

    private static Text createText(String content, Color color, boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        Text text = new Text(content);
        text.setFill(color);
        FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        text.setFont(Font.font("Consolas", weight, posture, 13));
        text.setUnderline(underline);
        text.setStrikethrough(strikethrough);
        return text;
    }

    public static void populateTextFlow(TextFlow textFlow, String input, Color defaultColor) {
        textFlow.getChildren().clear();
        textFlow.getChildren().addAll(parseToNodes(input, defaultColor));
    }
}
