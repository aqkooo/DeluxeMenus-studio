package com.ejyqyl.dmstudio.service.project;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.yaml.YamlImportService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages multi-menu project workspaces and cross-menu navigation (§Workspace Manager).
 */
public class WorkspaceManager {

    private static final WorkspaceManager INSTANCE = new WorkspaceManager();

    public static WorkspaceManager getInstance() {
        return INSTANCE;
    }

    private final ObjectProperty<Path> currentWorkspace = new SimpleObjectProperty<>();
    private final ObservableList<Path> menuFiles = FXCollections.observableArrayList();
    private final Map<String, Path> menuIndex = new HashMap<>();

    private WorkspaceManager() {}

    public ObjectProperty<Path> currentWorkspaceProperty() {
        return currentWorkspace;
    }

    public ObservableList<Path> getMenuFiles() {
        return menuFiles;
    }

    /**
     * Opens a directory as the active workspace and indexes all .yml menu files.
     */
    public void openWorkspace(File directory) {
        if (directory == null || !directory.isDirectory()) return;

        Path dirPath = directory.toPath();
        currentWorkspace.set(dirPath);
        refreshWorkspace();
    }

    public void refreshWorkspace() {
        Path root = currentWorkspace.get();
        menuFiles.clear();
        menuIndex.clear();
        if (root == null || !Files.isDirectory(root)) return;

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                    .forEach(p -> {
                        menuFiles.add(p);
                        String fileName = p.getFileName().toString();
                        String baseName = fileName.replaceFirst("\\.(yml|yaml)$", "").toLowerCase(Locale.ROOT);
                        menuIndex.put(baseName, p);
                    });
        } catch (IOException ignored) {}
    }

    /**
     * Finds a menu in the workspace by its open command name or file basename.
     */
    public Path findMenuPath(String menuName) {
        if (menuName == null) return null;
        String clean = menuName.trim().toLowerCase(Locale.ROOT);
        return menuIndex.get(clean);
    }

    public Optional<MenuConfig> loadMenu(String menuName) {
        Path path = findMenuPath(menuName);
        if (path == null) return Optional.empty();

        try {
            YamlImportService importer = new YamlImportService();
            YamlImportService.ImportResult result = importer.importFromFile(path);
            if (result.isSuccess()) {
                return Optional.ofNullable(result.getConfig());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
