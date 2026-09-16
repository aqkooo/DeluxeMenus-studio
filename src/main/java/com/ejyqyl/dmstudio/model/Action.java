package com.ejyqyl.dmstudio.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Click command or menu action with optional modifiers (&lt;delay=&gt;, &lt;chance=&gt;).
 */
public class Action {

    private static final Pattern DELAY_PATTERN = Pattern.compile("<delay=(\\d+)>");
    private static final Pattern CHANCE_PATTERN = Pattern.compile("<chance=(\\d+(?:\\.\\d+)?)>");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(\\[[a-zA-Z0-9_-]+])\\s*(.*)$");

    private String type; // e.g. "[console]", "[player]", "[message]"
    private String argument; // e.g. "give %player_name% diamond 1"
    private Integer delay; // in ticks (null if none)
    private Double chance; // in percent (null if none)

    public Action() {
        this.type = "[console]";
        this.argument = "";
    }

    public Action(String type, String argument) {
        this(type, argument, null, null);
    }

    public Action(String type, String argument, Integer delay, Double chance) {
        this.type = type != null ? type : "[console]";
        this.argument = argument != null ? argument : "";
        this.delay = delay;
        this.chance = chance;
    }

    public Action copy() {
        return new Action(type, argument, delay, chance);
    }

    public static Action parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Action("", "");
        }
        String working = raw.trim();

        // Extract delay
        Integer delay = null;
        Matcher delayMatcher = DELAY_PATTERN.matcher(working);
        if (delayMatcher.find()) {
            try {
                delay = Integer.parseInt(delayMatcher.group(1));
            } catch (NumberFormatException ignored) {}
            working = delayMatcher.replaceAll("");
        }

        // Extract chance
        Double chance = null;
        Matcher chanceMatcher = CHANCE_PATTERN.matcher(working);
        if (chanceMatcher.find()) {
            try {
                chance = Double.parseDouble(chanceMatcher.group(1));
            } catch (NumberFormatException ignored) {}
            working = chanceMatcher.replaceAll("");
        }

        // Extract prefix
        String type = "[console]";
        String argument = working.trim();
        Matcher prefixMatcher = PREFIX_PATTERN.matcher(working.trim());
        if (prefixMatcher.find()) {
            type = prefixMatcher.group(1);
            argument = prefixMatcher.group(2).trim();
        }

        return new Action(type, argument, delay, chance);
    }

    public String toSerializedString() {
        StringBuilder sb = new StringBuilder();
        if (type != null && !type.isBlank()) {
            sb.append(type);
            if (argument != null && !argument.isBlank()) {
                sb.append(" ").append(argument);
            }
        } else if (argument != null) {
            sb.append(argument);
        }

        if (delay != null && delay > 0) {
            sb.append("<delay=").append(delay).append(">");
        }
        if (chance != null && chance > 0 && chance < 100) {
            if (chance == chance.intValue()) {
                sb.append("<chance=").append(chance.intValue()).append(">");
            } else {
                sb.append("<chance=").append(chance).append(">");
            }
        }
        return sb.toString();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Double getChance() {
        return chance;
    }

    public void setChance(Double chance) {
        this.chance = chance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action action)) return false;
        return Objects.equals(type, action.type) &&
                Objects.equals(argument, action.argument) &&
                Objects.equals(delay, action.delay) &&
                Objects.equals(chance, action.chance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, argument, delay, chance);
    }

    @Override
    public String toString() {
        return toSerializedString();
    }
}
