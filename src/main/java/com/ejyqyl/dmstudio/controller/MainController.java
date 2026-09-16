package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.yaml.YamlImportService;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Root application controller orchestrating layout, menu commands, and sub-controllers.
 */
public class MainController {

    @FXML private MenuBar menuBar;
    @FXML private MenuItem menuNew;
    @FXML private MenuItem menuOpen;
    @FXML private MenuItem menuSave;
    @FXML private MenuItem menuSaveAs;
    @FXML private MenuItem menuExit;
    @FXML private MenuItem menuUndo;
    @FXML private MenuItem menuRedo;
    @FXML private MenuItem menuAbout;
    @FXML private RadioMenuItem menuLangRu;
    @FXML private RadioMenuItem menuLangEn;

    @FXML private MenuPropertiesController menuPropertiesController;
    @FXML private GridController gridController;
    @FXML private ItemInspectorController itemInspectorController;
    @FXML private PreviewController previewController;
    @FXML private ProblemsController problemsController;

    @FXML private Label statusFileLabel;
    @FXML private Label statusSlotLabel;
    @FXML private Label statusValidationLabel;

    private MainViewModel mainViewModel;
    private Stage stage;

    @FXML
    public void initialize() {
        initMenuActions();
    }

    public void init(MainViewModel viewModel, Stage stage) {
        this.mainViewModel = viewModel;
        this.stage = stage;

        menuPropertiesController.setMainViewModel(viewModel);
        gridController.setMainViewModel(viewModel);
        itemInspectorController.setMainViewModel(viewModel);
        previewController.setMainViewModel(viewModel);
        problemsController.setMainViewModel(viewModel);

        setupBindings();
    }

    private void initMenuActions() {
        menuNew.setOnAction(e -> handleNew());
        menuOpen.setOnAction(e -> handleOpen());
        menuSave.setOnAction(e -> handleSave());
        menuSaveAs.setOnAction(e -> handleSaveAs());
        menuExit.setOnAction(e -> handleExit());

        menuUndo.setOnAction(e -> {
            if (mainViewModel != null) mainViewModel.getCommandStack().undo();
        });
        menuRedo.setOnAction(e -> {
            if (mainViewModel != null) mainViewModel.getCommandStack().redo();
        });

        menuLangRu.setOnAction(e -> I18nService.getInstance().setLocale(Locale.of("ru", "RU")));
        menuLangEn.setOnAction(e -> I18nService.getInstance().setLocale(Locale.ENGLISH));

        menuAbout.setOnAction(e -> showAboutDialog());
    }

    private void setupBindings() {
        // Undo / Redo disabled properties
        menuUndo.disableProperty().bind(mainViewModel.getCommandStack().canUndoProperty().not());
        menuRedo.disableProperty().bind(mainViewModel.getCommandStack().canRedoProperty().not());

        // Window title with dirty marker
        mainViewModel.getCommandStack().dirtyProperty().addListener((obs, oldV, isDirty) -> updateTitle(isDirty));
        mainViewModel.currentFilePathProperty().addListener((obs, oldV, path) -> updateTitle(mainViewModel.getCommandStack().isDirty()));
        updateTitle(false);

        // Status bar slot label
        mainViewModel.getGridViewModel().selectedSlotProperty().addListener((obs, oldV, slot) -> {
            if (slot.intValue() >= 0) {
                statusSlotLabel.setText("Selected Slot: " + slot);
            } else {
                statusSlotLabel.setText("No slot selected");
            }
        });
    }

    private void updateTitle(boolean isDirty) {
        Path path = mainViewModel.currentFilePathProperty().get();
        String filename = path != null ? path.getFileName().toString() : "Untitled.yml";
        String title = "DeluxeMenus Studio - " + filename + (isDirty ? " *" : "");
        if (stage != null) {
            stage.setTitle(title);
        }
        statusFileLabel.setText(filename + (isDirty ? " (Modified)" : ""));
    }

    private void handleNew() {
        if (checkUnsavedChanges()) {
            mainViewModel.newMenu();
        }
    }

    private void handleOpen() {
        if (!checkUnsavedChanges()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nService.getInstance().get("menu.file.open"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML files (*.yml, *.yaml)", "*.yml", "*.yaml"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            YamlImportService.ImportResult result = mainViewModel.openFile(file.toPath());
            if (!result.isSuccess()) {
                showError("Import Error", result.getErrorMessage());
            } else if (!result.getReplacementsApplied().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Deprecated Options Migrated");
                alert.setHeaderText("The following deprecated options were automatically modernized:");
                alert.setContentText(String.join("\n", result.getReplacementsApplied()));
                alert.showAndWait();
            }
        }
    }

    private void handleSave() {
        Path current = mainViewModel.currentFilePathProperty().get();
        if (current == null) {
            handleSaveAs();
        } else {
            try {
                mainViewModel.saveFile(current);
            } catch (IOException e) {
                showError("Save Error", e.getMessage());
            }
        }
    }

    private void handleSaveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nService.getInstance().get("menu.file.save_as"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML files (*.yml)", "*.yml"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                mainViewModel.saveFile(file.toPath());
            } catch (IOException e) {
                showError("Save Error", e.getMessage());
            }
        }
    }

    public boolean checkUnsavedChanges() {
        if (mainViewModel.getCommandStack().isDirty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nService.getInstance().get("dialog.confirm_exit_title"));
            alert.setHeaderText(I18nService.getInstance().get("dialog.confirm_exit_msg"));
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }
        return true;
    }

    private void handleExit() {
        if (checkUnsavedChanges()) {
            Platform.exit();
        }
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nService.getInstance().get("app.about"));
        alert.setHeaderText(null);
        alert.setContentText(I18nService.getInstance().get("app.about.text"));
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
