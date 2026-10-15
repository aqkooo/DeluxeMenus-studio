package com.ejyqyl.dmstudio.service.theme;

import com.ejyqyl.dmstudio.service.settings.SettingsService;
import javafx.application.Platform;
import javafx.scene.Scene;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Centralized theme management service responsible for dynamic stylesheet swapping.
 */
public class ThemeService {

    private static final ThemeService INSTANCE = new ThemeService();

    private final Set<Scene> activeScenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final List<Consumer<AppTheme>> themeChangeListeners = new ArrayList<>();
    private AppTheme currentTheme;

    public static ThemeService getInstance() {
        return INSTANCE;
    }

    ThemeService() {
        AppTheme initial = SettingsService.getInstance().getSettings().getTheme();
        this.currentTheme = initial != null ? initial : AppTheme.LIGHT_GRADIENT;
    }

    public AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public void applyTheme(Scene scene) {
        if (scene == null) return;
        synchronized (activeScenes) {
            activeScenes.add(scene);
        }
        updateSceneStylesheet(scene, currentTheme);
    }

    public void setTheme(AppTheme theme) {
        if (theme == null || theme == this.currentTheme) return;
        this.currentTheme = theme;

        SettingsService.getInstance().getSettings().setTheme(theme);
        SettingsService.getInstance().save();

        notifyThemeChanged(theme);

        Runnable updater = () -> {
            synchronized (activeScenes) {
                Iterator<Scene> it = activeScenes.iterator();
                while (it.hasNext()) {
                    Scene s = it.next();
                    if (s != null) {
                        updateSceneStylesheet(s, currentTheme);
                    } else {
                        it.remove();
                    }
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            updater.run();
        } else {
            try {
                updater.run();
            } catch (Exception e) {
                Platform.runLater(updater);
            }
        }
    }

    public void addThemeChangeListener(Consumer<AppTheme> listener) {
        if (listener != null) {
            themeChangeListeners.add(listener);
        }
    }

    public void removeThemeChangeListener(Consumer<AppTheme> listener) {
        themeChangeListeners.remove(listener);
    }

    private void notifyThemeChanged(AppTheme theme) {
        for (Consumer<AppTheme> listener : themeChangeListeners) {
            try {
                listener.accept(theme);
            } catch (Exception e) {
                System.err.println("[ThemeService] Error notifying theme listener: " + e.getMessage());
            }
        }
    }

    private void updateSceneStylesheet(Scene scene, AppTheme theme) {
        if (scene == null) return;

        // Remove any previously attached theme stylesheets
        List<String> toRemove = new ArrayList<>();
        for (String url : scene.getStylesheets()) {
            for (AppTheme candidate : AppTheme.values()) {
                if (url.endsWith(candidate.getCssPath().substring(candidate.getCssPath().lastIndexOf('/') + 1))) {
                    toRemove.add(url);
                }
            }
            if (url.endsWith("theme-dark.css")) {
                toRemove.add(url);
            }
        }
        scene.getStylesheets().removeAll(toRemove);

        URL res = getClass().getResource(theme.getCssPath());
        if (res != null) {
            scene.getStylesheets().add(res.toExternalForm());
        } else {
            System.err.println("[ThemeService] CSS resource not found: " + theme.getCssPath());
        }
    }
}
