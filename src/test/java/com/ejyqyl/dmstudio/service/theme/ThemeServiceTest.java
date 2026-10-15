package com.ejyqyl.dmstudio.service.theme;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ThemeServiceTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testAllThemesHaveExistingCssResources() {
        for (AppTheme theme : AppTheme.values()) {
            URL url = getClass().getResource(theme.getCssPath());
            assertThat(url)
                    .as("Stylesheet for theme %s at %s must exist", theme.name(), theme.getCssPath())
                    .isNotNull();
        }
    }

    @Test
    void testThemeApplicationAndSwitching() {
        ThemeService service = ThemeService.getInstance();
        Scene scene = new Scene(new Pane(), 800, 600);

        service.setTheme(AppTheme.LIGHT_GRADIENT);
        service.applyTheme(scene);

        assertThat(scene.getStylesheets()).isNotEmpty();
        assertThat(scene.getStylesheets().get(0)).contains("theme-light-gradient.css");

        AtomicReference<AppTheme> changed = new AtomicReference<>();
        service.addThemeChangeListener(changed::set);

        service.setTheme(AppTheme.DARK_OBSIDIAN);

        assertThat(service.getCurrentTheme()).isEqualTo(AppTheme.DARK_OBSIDIAN);
        assertThat(changed.get()).isEqualTo(AppTheme.DARK_OBSIDIAN);
        assertThat(scene.getStylesheets().get(0)).contains("theme-dark-obsidian.css");

        // Switch to OLED Midnight
        service.setTheme(AppTheme.MIDNIGHT_OLED);
        assertThat(scene.getStylesheets().get(0)).contains("theme-midnight.css");

        // Switch to Minecraft Classic
        service.setTheme(AppTheme.MINECRAFT_CLASSIC);
        assertThat(scene.getStylesheets().get(0)).contains("theme-minecraft-classic.css");
    }
}
