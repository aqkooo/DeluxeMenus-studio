package com.ejyqyl.dmstudio.service.render;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighter for YAML configuration preview using RichTextFX (§6.5).
 */
public final class YamlHighlighter {

    private static final String KEY_PATTERN = "(?m)^[ \\t]*([a-zA-Z0-9_\\-]+)(?=:)";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String NUMBER_PATTERN = "\\b-?\\d+(\\.\\d+)?\\b";
    private static final String BOOLEAN_PATTERN = "\\b(true|false|yes|no)\\b";
    private static final String COMMENT_PATTERN = "#[^\\n\\r]*";
    private static final String PLACEHOLDER_PATTERN = "%[a-zA-Z0-9_]+%";
    private static final String ACTION_PATTERN = "\\[[a-zA-Z0-9_]+\\]";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEY>" + KEY_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<BOOLEAN>" + BOOLEAN_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<PLACEHOLDER>" + PLACEHOLDER_PATTERN + ")"
                    + "|(?<ACTION>" + ACTION_PATTERN + ")"
    );

    private YamlHighlighter() {}

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            return new StyleSpansBuilder<Collection<String>>().add(Collections.emptyList(), 0).create();
        }

        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEY") != null ? "yaml-key" :
                    matcher.group("STRING") != null ? "yaml-string" :
                    matcher.group("NUMBER") != null ? "yaml-number" :
                    matcher.group("BOOLEAN") != null ? "yaml-boolean" :
                    matcher.group("COMMENT") != null ? "yaml-comment" :
                    matcher.group("PLACEHOLDER") != null ? "yaml-placeholder" :
                    matcher.group("ACTION") != null ? "yaml-action" : null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
