package com.ejyqyl.dmstudio.service.settings;

import com.ejyqyl.dmstudio.service.theme.AppTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    private Path testSettingsFile;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        testSettingsFile = tempDir.resolve("test_settings.json");
        settingsService = new SettingsService(testSettingsFile);
    }

    @Test
    void testDefaultsWhenFileMissing() {
        UserSettings settings = settingsService.getSettings();
        assertThat(settings).isNotNull();
        assertThat(settings.getTheme()).isEqualTo(AppTheme.LIGHT_GRADIENT);
        assertThat(settings.getLanguage()).isEqualTo("ru");
        assertThat(settings.getAutoSaveIntervalMinutes()).isEqualTo(5);
        assertThat(settings.isAutoSaveEnabled()).isTrue();
        assertThat(settings.isHighlightSyntax()).isTrue();
    }

    @Test
    void testSaveAndReload() {
        UserSettings settings = settingsService.getSettings();
        settings.setTheme(AppTheme.DARK_OBSIDIAN);
        settings.setLanguage("en");
        settings.setAutoSaveIntervalMinutes(15);
        settings.setAutoSaveEnabled(false);
        settings.setHighlightSyntax(false);

        settingsService.save();

        assertThat(Files.exists(testSettingsFile)).isTrue();

        // Create new service pointing to same file
        SettingsService reloaded = new SettingsService(testSettingsFile);
        UserSettings loaded = reloaded.getSettings();

        assertThat(loaded.getTheme()).isEqualTo(AppTheme.DARK_OBSIDIAN);
        assertThat(loaded.getLanguage()).isEqualTo("en");
        assertThat(loaded.getAutoSaveIntervalMinutes()).isEqualTo(15);
        assertThat(loaded.isAutoSaveEnabled()).isFalse();
        assertThat(loaded.isHighlightSyntax()).isFalse();
    }

    @Test
    void testCorruptedFileFallsBackToDefaults() throws IOException {
        Files.writeString(testSettingsFile, "{ invalid json content !!!");
        SettingsService fallback = new SettingsService(testSettingsFile);
        UserSettings loaded = fallback.getSettings();

        assertThat(loaded).isNotNull();
        assertThat(loaded.getTheme()).isEqualTo(AppTheme.LIGHT_GRADIENT);
        assertThat(loaded.getLanguage()).isEqualTo("ru");
    }

    @Test
    void testChangeListener() {
        AtomicBoolean called = new AtomicBoolean(false);
        settingsService.addChangeListener(s -> called.set(true));

        settingsService.getSettings().setTheme(AppTheme.MIDNIGHT_OLED);
        settingsService.save();

        assertThat(called.get()).isTrue();
    }
}
