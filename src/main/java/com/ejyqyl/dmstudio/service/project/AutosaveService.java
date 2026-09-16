package com.ejyqyl.dmstudio.service.project;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.yaml.YamlExportService;
import com.ejyqyl.dmstudio.service.yaml.YamlImportService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for periodically auto-saving project drafts to user home directory (§6.6).
 */
public class AutosaveService {

    private final Path autosaveDir;
    private final Path autosaveFile;
    private final YamlExportService exportService = new YamlExportService();
    private final YamlImportService importService = new YamlImportService();
    private ScheduledExecutorService scheduler;

    public AutosaveService() {
        String userHome = System.getProperty("user.home", ".");
        this.autosaveDir = Paths.get(userHome, ".deluxemenus-studio");
        this.autosaveFile = autosaveDir.resolve("autosave_draft.yml");
    }

    public void start(Supplier<MenuConfig> configSupplier, Supplier<Boolean> dirtySupplier) {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutosaveThread");
            t.setDaemon(true);
            return t;
        });

        // Autosave every 60 seconds (§6.6)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (dirtySupplier.get()) {
                    MenuConfig config = configSupplier.get();
                    if (config != null) {
                        saveDraft(config);
                    }
                }
            } catch (Exception ignored) {}
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public synchronized void saveDraft(MenuConfig config) throws IOException {
        if (!Files.exists(autosaveDir)) {
            Files.createDirectories(autosaveDir);
        }
        exportService.exportToFile(config, autosaveFile);
    }

    public boolean hasDraft() {
        return Files.exists(autosaveFile);
    }

    public MenuConfig loadDraft() {
        if (!hasDraft()) return null;
        YamlImportService.ImportResult result = importService.importFromFile(autosaveFile);
        return result.isSuccess() ? result.getConfig() : null;
    }

    public void clearDraft() {
        try {
            Files.deleteIfExists(autosaveFile);
        } catch (IOException ignored) {}
    }

    public Path getAutosaveFile() {
        return autosaveFile;
    }
}
