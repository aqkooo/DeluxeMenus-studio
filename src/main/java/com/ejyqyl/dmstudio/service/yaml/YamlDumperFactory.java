package com.ejyqyl.dmstudio.service.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Set;

/**
 * Factory for creating configured SnakeYAML instances matching specification §8.2.
 */
public final class YamlDumperFactory {

    private static final Set<String> YAML_KEYWORDS = Set.of(
            "true", "false", "on", "off", "yes", "no", "null", "y", "n"
    );

    private YamlDumperFactory() {}

    public static DumperOptions createOptions() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setPrettyFlow(true);
        options.setSplitLines(false);
        options.setWidth(Integer.MAX_VALUE);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        return options;
    }

    /**
     * Creates a SnakeYAML instance with smart scalar quoting:
     * Quotes strings starting with '&', '!', containing '%', '#', empty strings, keywords, or numeric strings.
     */
    public static Yaml createYaml() {
        DumperOptions options = createOptions();
        Representer representer = new SmartRepresenter(options);
        return new Yaml(representer, options);
    }

    private static class SmartRepresenter extends Representer {
        public SmartRepresenter(DumperOptions options) {
            super(options);
            // Represent empty strings with single quotes
            this.representers.put(String.class, data -> representString((String) data));
        }

        private Node representString(String val) {
            if (val == null) {
                return representScalar(Tag.NULL, "null");
            }
            DumperOptions.ScalarStyle style = DumperOptions.ScalarStyle.PLAIN;

            // Specification §8.2: single quotes for strings containing '&', '!', '%', '#', quotes, or keywords
            String lower = val.toLowerCase();
            if (val.isEmpty()
                    || val.isBlank()
                    || val.startsWith("&")
                    || val.startsWith("!")
                    || val.contains("&#")
                    || val.contains("&")
                    || val.contains("%")
                    || val.contains("'")
                    || val.contains(":")
                    || val.contains("{")
                    || val.contains("}")
                    || val.contains("[")
                    || val.contains("]")
                    || YAML_KEYWORDS.contains(lower)
                    || isNumeric(val)) {
                style = DumperOptions.ScalarStyle.SINGLE_QUOTED;
            }
            return representScalar(Tag.STR, val, style);
        }

        private boolean isNumeric(String s) {
            if (s == null || s.isEmpty()) return false;
            try {
                Double.parseDouble(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
