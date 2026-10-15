package com.ejyqyl.dmstudio.service.theme;

/**
 * Enumeration of available visual themes in DeluxeMenus Studio.
 */
public enum AppTheme {

    LIGHT_GRADIENT("theme.light_gradient", "/css/theme-light-gradient.css", "#ffffff", "#0969da"),
    DARK_OBSIDIAN("theme.dark_obsidian", "/css/theme-dark-obsidian.css", "#181b24", "#3b82f6"),
    MIDNIGHT_OLED("theme.midnight_oled", "/css/theme-midnight.css", "#000000", "#06b6d4"),
    MINECRAFT_CLASSIC("theme.minecraft_classic", "/css/theme-minecraft-classic.css", "#c6c6c6", "#2563eb");

    private final String i18nKey;
    private final String cssPath;
    private final String previewBgColor;
    private final String previewAccentColor;

    AppTheme(String i18nKey, String cssPath, String previewBgColor, String previewAccentColor) {
        this.i18nKey = i18nKey;
        this.cssPath = cssPath;
        this.previewBgColor = previewBgColor;
        this.previewAccentColor = previewAccentColor;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public String getCssPath() {
        return cssPath;
    }

    public String getPreviewBgColor() {
        return previewBgColor;
    }

    public String getPreviewAccentColor() {
        return previewAccentColor;
    }
}
