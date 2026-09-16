package com.ejyqyl.dmstudio.model.requirement;

import com.ejyqyl.dmstudio.model.Action;

import java.util.*;

/**
 * A single requirement condition in DeluxeMenus.
 */
public class Requirement {

    private String type; // e.g. "has permission" (without '!' prefix)
    private boolean inverted; // true if prefixed with '!'
    private Boolean optional;
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final List<Action> denyCommands = new ArrayList<>();
    private final List<Action> successCommands = new ArrayList<>();
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public Requirement() {
        this.type = "has permission";
        this.inverted = false;
    }

    public Requirement(String rawType) {
        setRawType(rawType);
    }

    public void setRawType(String rawType) {
        if (rawType == null) {
            this.type = "";
            this.inverted = false;
            return;
        }
        String trimmed = rawType.trim();
        if (trimmed.startsWith("!")) {
            this.inverted = true;
            this.type = trimmed.substring(1).trim();
        } else {
            this.inverted = false;
            this.type = trimmed;
        }
    }

    public String getRawType() {
        if (inverted) {
            return "!" + type;
        }
        return type;
    }

    public Requirement copy() {
        Requirement copy = new Requirement();
        copy.type = this.type;
        copy.inverted = this.inverted;
        copy.optional = this.optional;
        copy.fields.putAll(this.fields);
        for (Action action : this.denyCommands) {
            copy.denyCommands.add(action.copy());
        }
        for (Action action : this.successCommands) {
            copy.successCommands.add(action.copy());
        }
        copy.unknownFields.putAll(this.unknownFields);
        return copy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setField(String key, Object value) {
        if (value == null) {
            fields.remove(key);
        } else {
            fields.put(key, value);
        }
    }

    public Object getField(String key) {
        return fields.get(key);
    }

    public List<Action> getDenyCommands() {
        return denyCommands;
    }

    public List<Action> getSuccessCommands() {
        return successCommands;
    }

    public Map<String, Object> getUnknownFields() {
        return unknownFields;
    }
}
