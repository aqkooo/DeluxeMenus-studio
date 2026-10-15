package com.ejyqyl.dmstudio.service.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Singleton service managing persistence and lifecycle of user settings.
 */
public class SettingsService {

    private static final String APP_DIR_NAME = ".deluxemenus-studio";
    private static final String SETTINGS_FILE_NAME = "settings.json";

    private static final SettingsService INSTANCE = new SettingsService();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path settingsPath;
    private UserSettings settings;
    private final List<Consumer<UserSettings>> changeListeners = new ArrayList<>();

    public static SettingsService getInstance() {
        return INSTANCE;
    }

    SettingsService() {
        this(Paths.get(System.getProperty("user.home"), APP_DIR_NAME, SETTINGS_FILE_NAME));
    }

    SettingsService(Path settingsPath) {
        this.settingsPath = settingsPath;
        this.settings = loadInternal();
    }

    public UserSettings getSettings() {
        return settings;
    }

    public Path getSettingsPath() {
        return settingsPath;
    }

    public synchronized void save() {
        try {
            Path parent = settingsPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Path tempFile = Files.createTempFile(parent != null ? parent : Paths.get("."), "settings_", ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                gson.toJson(settings, writer);
            }
            Files.move(tempFile, settingsPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            notifyListeners();
        } catch (IOException e) {
            System.err.println("[SettingsService] Failed to persist user settings: " + e.getMessage());
        }
    }

    public synchronized void reload() {
        this.settings = loadInternal();
        notifyListeners();
    }

    public void addChangeListener(Consumer<UserSettings> listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Consumer<UserSettings> listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Consumer<UserSettings> listener : changeListeners) {
            try {
                listener.accept(settings);
            } catch (Exception e) {
                System.err.println("[SettingsService] Listener notification error: " + e.getMessage());
            }
        }
    }

    private UserSettings loadInternal() {
        if (!Files.exists(settingsPath)) {
            UserSettings defaultSettings = new UserSettings();
            return defaultSettings;
        }
        try (Reader reader = Files.newBufferedReader(settingsPath, StandardCharsets.UTF_8)) {
            UserSettings loaded = gson.fromJson(reader, UserSettings.class);
            return loaded != null ? loaded : new UserSettings();
        } catch (Exception e) {
            System.err.println("[SettingsService] Could not parse settings file, using defaults: " + e.getMessage());
            return new UserSettings();
        }
    }
}
