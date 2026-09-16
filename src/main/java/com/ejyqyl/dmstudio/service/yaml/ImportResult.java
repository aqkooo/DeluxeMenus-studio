package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.MenuConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the result of importing a DeluxeMenus YAML configuration.
 */
public class ImportResult {

    private final MenuConfig config;
    private final List<String> replacementsApplied = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private String errorMessage;
    private int errorLine = -1;
    private int errorColumn = -1;

    public ImportResult(MenuConfig config) {
        this.config = config;
    }

    public ImportResult(String errorMessage, int errorLine, int errorColumn) {
        this.config = null;
        this.errorMessage = errorMessage;
        this.errorLine = errorLine;
        this.errorColumn = errorColumn;
    }

    public boolean isSuccess() {
        return config != null && errorMessage == null;
    }

    public MenuConfig getConfig() {
        return config;
    }

    public List<String> getReplacementsApplied() {
        return replacementsApplied;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorLine() {
        return errorLine;
    }

    public int getErrorColumn() {
        return errorColumn;
    }
}
