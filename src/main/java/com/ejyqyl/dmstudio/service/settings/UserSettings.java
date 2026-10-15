package com.ejyqyl.dmstudio.service.settings;

import com.ejyqyl.dmstudio.service.theme.AppTheme;

/**
 * Data model for persistent user application preferences.
 */
public class UserSettings {

    private AppTheme theme = AppTheme.LIGHT_GRADIENT;
    private String language = "ru";
    private int autoSaveIntervalMinutes = 5;
    private boolean autoSaveEnabled = true;
    private boolean highlightSyntax = true;

    public UserSettings() {
    }

    public AppTheme getTheme() {
        return theme != null ? theme : AppTheme.LIGHT_GRADIENT;
    }

    public void setTheme(AppTheme theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language != null ? language : "ru";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getAutoSaveIntervalMinutes() {
        return autoSaveIntervalMinutes;
    }

    public void setAutoSaveIntervalMinutes(int autoSaveIntervalMinutes) {
        this.autoSaveIntervalMinutes = autoSaveIntervalMinutes;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void setAutoSaveEnabled(boolean autoSaveEnabled) {
        this.autoSaveEnabled = autoSaveEnabled;
    }

    public boolean isHighlightSyntax() {
        return highlightSyntax;
    }

    public void setHighlightSyntax(boolean highlightSyntax) {
        this.highlightSyntax = highlightSyntax;
    }
}
