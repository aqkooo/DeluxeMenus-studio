package com.ejyqyl.dmstudio.service.render;

import javafx.scene.paint.Color;
import java.util.Locale;

/**
 * Service for generating RGB gradients in various Minecraft color formats (§Visual Tools).
 */
public class GradientGeneratorService {

    public enum FormatMode {
        DELUXE_MENUS_HEX,
        MINI_MESSAGE,
        SPIGOT_LEGACY_HEX
    }

    /**
     * Generates a gradient string for the given input text across two or three colors.
     */
    public static String generateGradient(
            String text,
            Color start,
            Color middle,
            Color end,
            FormatMode mode,
            boolean bold,
            boolean italic,
            boolean underline,
            boolean strikethrough
    ) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // MiniMessage special case: supports native <gradient> tag
        if (mode == FormatMode.MINI_MESSAGE) {
            StringBuilder sb = new StringBuilder("<gradient:");
            sb.append(toHex(start));
            if (middle != null) {
                sb.append(":").append(toHex(middle));
            }
            sb.append(":").append(toHex(end)).append(">");

            if (bold) sb.append("<b>");
            if (italic) sb.append("<i>");
            if (underline) sb.append("<u>");
            if (strikethrough) sb.append("<st>");

            sb.append(text);

            if (strikethrough) sb.append("</st>");
            if (underline) sb.append("</u>");
            if (italic) sb.append("</i>");
            if (bold) sb.append("</b>");

            sb.append("</gradient>");
            return sb.toString();
        }

        // Per-character interpolation for DELUXE_MENUS_HEX and SPIGOT_LEGACY_HEX
        StringBuilder styles = new StringBuilder();
        if (bold) styles.append("&l");
        if (italic) styles.append("&o");
        if (underline) styles.append("&n");
        if (strikethrough) styles.append("&m");
        String styleStr = styles.toString();

        StringBuilder result = new StringBuilder();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                result.append(c);
                continue;
            }

            double progress = len > 1 ? (double) i / (len - 1) : 0.0;
            Color charColor = interpolate(start, middle, end, progress);

            if (mode == FormatMode.SPIGOT_LEGACY_HEX) {
                result.append(toSpigotLegacyHex(charColor));
            } else {
                result.append("&#").append(toHex(charColor));
            }
            result.append(styleStr).append(c);
        }

        return result.toString();
    }

    private static Color interpolate(Color start, Color middle, Color end, double progress) {
        if (middle == null) {
            return interpolateTwo(start, end, progress);
        }
        if (progress < 0.5) {
            return interpolateTwo(start, middle, progress * 2.0);
        } else {
            return interpolateTwo(middle, end, (progress - 0.5) * 2.0);
        }
    }

    private static Color interpolateTwo(Color c1, Color c2, double fraction) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        double r = c1.getRed() + (c2.getRed() - c1.getRed()) * f;
        double g = c1.getGreen() + (c2.getGreen() - c1.getGreen()) * f;
        double b = c1.getBlue() + (c2.getBlue() - c1.getBlue()) * f;
        return new Color(r, g, b, 1.0);
    }

    public static String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        return String.format(Locale.ROOT, "%02x%02x%02x", r, g, b);
    }

    private static String toSpigotLegacyHex(Color color) {
        String hex = toHex(color);
        StringBuilder sb = new StringBuilder("&x");
        for (char c : hex.toCharArray()) {
            sb.append('&').append(c);
        }
        return sb.toString();
    }
}
