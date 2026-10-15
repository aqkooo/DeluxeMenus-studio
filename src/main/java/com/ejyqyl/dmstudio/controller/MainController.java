package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureExtractorService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.ejyqyl.dmstudio.service.export.MenuPosterExportService;
import com.ejyqyl.dmstudio.service.optimizer.ConfigOptimizerService;
import com.ejyqyl.dmstudio.service.optimizer.ConfigOptimizerService.OptimizationResult;
import com.ejyqyl.dmstudio.service.project.WorkspaceManager;
import com.ejyqyl.dmstudio.ui.dialog.ComponentsDialog;
import com.ejyqyl.dmstudio.ui.dialog.GradientGeneratorDialog;
import com.ejyqyl.dmstudio.ui.dialog.PapiHelperDialog;
import com.ejyqyl.dmstudio.ui.dialog.SimulatorDialog;
import com.ejyqyl.dmstudio.service.theme.AppTheme;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import com.ejyqyl.dmstudio.ui.dialog.SettingsDialog;
import javafx.stage.DirectoryChooser;

/**
 * Root application controller orchestrating layout, menu commands, and sub-controllers.
 */
public class MainController {

    @FXML private MenuBar menuBar;
    @FXML private MenuItem menuNew, menuOpen, menuSave, menuSaveAs, menuOpenWorkspace, menuExportPoster, menuExit;
    @FXML private MenuItem menuUndo, menuRedo;
    @FXML private MenuItem menuSimulator, menuGradient, menuComponents, menuPapi, menuOptimize, menuExtractTextures;
    @FXML private MenuItem menuPreferences, menuAbout;
    @FXML private RadioMenuItem menuThemeLight, menuThemeDark, menuThemeMidnight, menuThemeMinecraft;
    @FXML private RadioMenuItem menuLangRu, menuLangEn;

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
        menuOpenWorkspace.setOnAction(e -> handleOpenWorkspace());
        menuSave.setOnAction(e -> handleSave());
        menuSaveAs.setOnAction(e -> handleSaveAs());
        menuExportPoster.setOnAction(e -> handleExportPoster());
        menuExit.setOnAction(e -> handleExit());

        menuUndo.setOnAction(e -> {
            if (mainViewModel != null) mainViewModel.getCommandStack().undo();
        });
        menuRedo.setOnAction(e -> {
            if (mainViewModel != null) mainViewModel.getCommandStack().redo();
        });

        menuSimulator.setOnAction(e -> handleSimulator());
        menuGradient.setOnAction(e -> handleGradient());
        menuComponents.setOnAction(e -> handleComponents());
        menuPapi.setOnAction(e -> handlePapi());
        menuOptimize.setOnAction(e -> handleOptimize());
        menuExtractTextures.setOnAction(e -> showExtractTexturesDialog());

        menuLangRu.setOnAction(e -> I18nService.getInstance().setLocale(Locale.of("ru", "RU")));
        menuLangEn.setOnAction(e -> I18nService.getInstance().setLocale(Locale.ENGLISH));

        menuAbout.setOnAction(e -> showAboutDialog());

        if (menuPreferences != null) menuPreferences.setOnAction(e -> SettingsDialog.show(stage));
        ToggleGroup tgTheme = new ToggleGroup();
        if (menuThemeLight != null) {
            menuThemeLight.setToggleGroup(tgTheme);
            menuThemeLight.setOnAction(e -> ThemeService.getInstance().setTheme(AppTheme.LIGHT_GRADIENT));
        }
        if (menuThemeDark != null) {
            menuThemeDark.setToggleGroup(tgTheme);
            menuThemeDark.setOnAction(e -> ThemeService.getInstance().setTheme(AppTheme.DARK_OBSIDIAN));
        }
        if (menuThemeMidnight != null) {
            menuThemeMidnight.setToggleGroup(tgTheme);
            menuThemeMidnight.setOnAction(e -> ThemeService.getInstance().setTheme(AppTheme.MIDNIGHT_OLED));
        }
        if (menuThemeMinecraft != null) {
            menuThemeMinecraft.setToggleGroup(tgTheme);
            menuThemeMinecraft.setOnAction(e -> ThemeService.getInstance().setTheme(AppTheme.MINECRAFT_CLASSIC));
        }
        syncThemeToggles();
        ThemeService.getInstance().addThemeChangeListener(t -> syncThemeToggles());
    }

    private void syncThemeToggles() {
        AppTheme cur = ThemeService.getInstance().getCurrentTheme();
        if (menuThemeLight != null) menuThemeLight.setSelected(cur == AppTheme.LIGHT_GRADIENT);
        if (menuThemeDark != null) menuThemeDark.setSelected(cur == AppTheme.DARK_OBSIDIAN);
        if (menuThemeMidnight != null) menuThemeMidnight.setSelected(cur == AppTheme.MIDNIGHT_OLED);
        if (menuThemeMinecraft != null) menuThemeMinecraft.setSelected(cur == AppTheme.MINECRAFT_CLASSIC);
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
                statusSlotLabel.setText(I18nService.getInstance().get("status.slot_selected", slot));
            } else {
                statusSlotLabel.setText(I18nService.getInstance().get("status.no_slot"));
            }
        });
        statusSlotLabel.setText(I18nService.getInstance().get("status.no_slot"));
    }

    private void updateTitle(boolean isDirty) {
        Path path = mainViewModel.currentFilePathProperty().get();
        String filename = path != null ? path.getFileName().toString() : I18nService.getInstance().get("status.untitled");
        String title = "DeluxeMenus Studio - " + filename + (isDirty ? " *" : "");
        if (stage != null) {
            stage.setTitle(title);
        }
        statusFileLabel.setText(filename + (isDirty ? " " + I18nService.getInstance().get("status.modified") : ""));
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
                showError(I18nService.getInstance().get("dialog.error_title"), result.getErrorMessage());
            } else if (!result.getReplacementsApplied().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18nService.getInstance().get("dialog.info_title"));
                alert.setHeaderText(null);
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
                showError(I18nService.getInstance().get("dialog.error_title"), e.getMessage());
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
                showError(I18nService.getInstance().get("dialog.error_title"), e.getMessage());
            }
        }
    }

    private void showExtractTexturesDialog() {
        TextureExtractorService extractor = new TextureExtractorService();
        List<Path> installed = extractor.findInstalledMinecraftJars();

        FileChooser fc = new FileChooser();
        fc.setTitle(I18nService.getInstance().get("dialog.extract_textures_prompt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Minecraft JAR / Resource Pack (*.jar, *.zip)", "*.jar", "*.zip"));

        Path selectedPath = null;
        if (!installed.isEmpty()) {
            ChoiceDialog<Path> dialog = new ChoiceDialog<>(installed.get(0), installed);
            dialog.setTitle(I18nService.getInstance().get("dialog.extract_textures_title"));
            dialog.setHeaderText(I18nService.getInstance().get("dialog.extract_textures_header"));
            dialog.setContentText(I18nService.getInstance().get("dialog.extract_textures_auto"));
            Optional<Path> res = dialog.showAndWait();
            if (res.isPresent()) {
                selectedPath = res.get();
            }
        }

        if (selectedPath == null && installed.isEmpty()) {
            File f = fc.showOpenDialog(stage);
            if (f != null) selectedPath = f.toPath();
        }

        if (selectedPath != null) {
            Path targetDir = TextureManager.getInstance().getDefaultTexturesDir();
            boolean ok = extractor.extractAndBuildAtlas(selectedPath, targetDir, msg -> System.out.println("[TextureExtractor] " + msg));
            if (ok) {
                TextureManager.getInstance().reloadAtlas();
                gridController.renderGrid();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18nService.getInstance().get("dialog.extract_textures_title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nService.getInstance().get("dialog.extract_textures_success"));
                alert.showAndWait();
            } else {
                showError(I18nService.getInstance().get("dialog.error_title"), "Failed to extract textures from " + selectedPath.getFileName());
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

    private void handleOpenWorkspace() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18nService.getInstance().get("menu.file.open_workspace"));
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            WorkspaceManager.getInstance().openWorkspace(dir);
            int count = WorkspaceManager.getInstance().getMenuFiles().size();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18nService.getInstance().get("dialog.info_title"));
            alert.setHeaderText(null);
            alert.setContentText("Рабочая область успешно открыта: обнаружено " + count + " меню.");
            alert.showAndWait();
        }
    }

    private void handleExportPoster() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nService.getInstance().get("menu.file.export_poster"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));
        chooser.setInitialFileName("menu_poster.png");
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                MenuPosterExportService.exportPoster(mainViewModel.getConfig(), file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18nService.getInstance().get("dialog.info_title"));
                alert.setHeaderText(null);
                alert.setContentText("Постер меню успешно сохранён: " + file.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                showError(I18nService.getInstance().get("dialog.error_title"), "Не удалось экспортировать постер: " + ex.getMessage());
            }
        }
    }

    private void handleSimulator() {
        if (mainViewModel != null && mainViewModel.getConfig() != null) {
            SimulatorDialog.show(stage, mainViewModel.getConfig());
        }
    }

    private void handleGradient() {
        GradientGeneratorDialog.show(stage,
                appliedName -> {
                    if (mainViewModel != null && mainViewModel.getItemEditorViewModel() != null) {
                        mainViewModel.getItemEditorViewModel().displayNameProperty().set(appliedName);
                    }
                },
                appliedLore -> {
                    if (mainViewModel != null && mainViewModel.getItemEditorViewModel() != null) {
                        String cur = mainViewModel.getItemEditorViewModel().loreTextProperty().get();
                        String updated = (cur == null || cur.isBlank()) ? appliedLore : cur + "\n" + appliedLore;
                        mainViewModel.getItemEditorViewModel().loreTextProperty().set(updated);
                    }
                }
        );
    }

    private void handleComponents() {
        ComponentsDialog.show(stage, (componentItem, slot) -> {
            if (mainViewModel != null) {
                mainViewModel.getGridViewModel().setItemAt(slot, componentItem);
            }
        });
    }

    private void handlePapi() {
        PapiHelperDialog.show(stage, placeholder -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(placeholder);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            if (mainViewModel != null) {
                mainViewModel.setStatusMessage("Плейсхолдер скопирован: " + placeholder);
            }
        });
    }

    private void handleOptimize() {
        if (mainViewModel == null || mainViewModel.getConfig() == null) return;
        OptimizationResult res = new ConfigOptimizerService().optimize(mainViewModel.getConfig());
        gridController.renderGrid();
        mainViewModel.getCommandStack().setDirty(true);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nService.getInstance().get("tools.optimizer.success_title"));
        alert.setHeaderText(I18nService.getInstance().get("tools.optimizer.success_header"));
        alert.setContentText(I18nService.getInstance().get("tools.optimizer.report",
                res.compressedRangesCount(), res.strippedFieldsCount(), res.mergedItemsCount()));
        alert.showAndWait();
    }
}
