package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.command.CommandStack;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.project.AutosaveService;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationEngine;
import com.ejyqyl.dmstudio.service.yaml.YamlExportService;
import com.ejyqyl.dmstudio.service.yaml.YamlImportService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Root ViewModel coordinating MenuConfig, GridViewModel, ItemEditorViewModel,
 * PreviewViewModel, Validation, and Autosave (§4.1).
 */
public class MainViewModel {

    private final ObjectProperty<MenuConfig> config = new SimpleObjectProperty<>();
    private final ObjectProperty<Path> currentFilePath = new SimpleObjectProperty<>(null);
    private final StringProperty statusMessage = new SimpleStringProperty("");

    private final CommandStack commandStack = new CommandStack();
    private final ValidationEngine validationEngine = new ValidationEngine();
    private final ValidationContext validationContext = new ValidationContext("1.21", "1.14.0");
    private final AutosaveService autosaveService = new AutosaveService();
    private final YamlExportService exportService = new YamlExportService();
    private final YamlImportService importService = new YamlImportService();

    private GridViewModel gridViewModel;
    private ItemEditorViewModel itemEditorViewModel;
    private PreviewViewModel previewViewModel;
    private final ProblemsViewModel problemsViewModel = new ProblemsViewModel();

    public MainViewModel() {
        MenuConfig initial = new MenuConfig();
        setConfig(initial);
        autosaveService.start(this::getConfig, commandStack::isDirty);
    }

    public void setConfig(MenuConfig newConfig) {
        this.config.set(newConfig);
        this.commandStack.clear();

        this.previewViewModel = new PreviewViewModel(newConfig);
        this.gridViewModel = new GridViewModel(newConfig, commandStack);
        this.itemEditorViewModel = new ItemEditorViewModel(newConfig, commandStack, previewViewModel);

        // Bind grid active item to item inspector
        this.gridViewModel.activeItemProperty().addListener((obs, oldVal, newVal) -> {
            itemEditorViewModel.bindItem(newVal);
        });

        // Trigger live validation & preview on command stack changes
        this.commandStack.addChangeListener(() -> {
            runValidation();
            previewViewModel.requestUpdate();
        });

        runValidation();
        previewViewModel.requestUpdate();
    }

    public void newMenu() {
        MenuConfig fresh = new MenuConfig();
        fresh.setMenuTitle("&8Custom Menu");
        currentFilePath.set(null);
        setConfig(fresh);
    }

    public YamlImportService.ImportResult openFile(Path file) {
        YamlImportService.ImportResult result = importService.importFromFile(file);
        if (result.isSuccess()) {
            currentFilePath.set(file);
            setConfig(result.getConfig());
            commandStack.markSaved();
        }
        return result;
    }

    public void saveFile(Path file) throws IOException {
        exportService.exportToFile(config.get(), file);
        currentFilePath.set(file);
        commandStack.markSaved();
    }

    public void runValidation() {
        List<ValidationIssue> issues = validationEngine.validate(config.get(), validationContext);
        problemsViewModel.updateIssues(issues);
    }

    public MenuConfig getConfig() { return config.get(); }
    public ObjectProperty<MenuConfig> configProperty() { return config; }

    public CommandStack getCommandStack() { return commandStack; }
    public GridViewModel getGridViewModel() { return gridViewModel; }
    public ItemEditorViewModel getItemEditorViewModel() { return itemEditorViewModel; }
    public PreviewViewModel getPreviewViewModel() { return previewViewModel; }
    public ProblemsViewModel getProblemsViewModel() { return problemsViewModel; }
    public ValidationContext getValidationContext() { return validationContext; }
    public ObjectProperty<Path> currentFilePathProperty() { return currentFilePath; }
    public AutosaveService getAutosaveService() { return autosaveService; }

    public String getStatusMessage() { return statusMessage.get(); }
    public void setStatusMessage(String message) { this.statusMessage.set(message); }
    public StringProperty statusMessageProperty() { return statusMessage; }
}
