package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.settings.SettingsService;
import com.ejyqyl.dmstudio.service.settings.UserSettings;
import com.ejyqyl.dmstudio.service.theme.AppTheme;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller and modal launcher for the Settings dialog.
 */
public class SettingsDialog {

    @FXML private TabPane tabPane;
    @FXML private RadioButton rbThemeLight;
    @FXML private RadioButton rbThemeDark;
    @FXML private RadioButton rbThemeMidnight;
    @FXML private RadioButton rbThemeMinecraft;
    @FXML private ToggleGroup themeGroup;

    @FXML private RadioButton rbLangRu;
    @FXML private RadioButton rbLangEn;
    @FXML private ToggleGroup langGroup;

    @FXML private CheckBox cbAutoSave;
    @FXML private Spinner<Integer> spAutoSaveInterval;
    @FXML private CheckBox cbHighlightSyntax;

    @FXML private Label lblOs;
    @FXML private Label lblJava;
    @FXML private Label lblJavafx;
    @FXML private Label lblMemory;
    @FXML private TextField txtConfigPath;

    @FXML private Button btnApply;
    @FXML private Button btnClose;

    private Stage stage;

    public static void show(Stage owner) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", I18nService.getInstance().getCurrentLocale());
            FXMLLoader loader = new FXMLLoader(SettingsDialog.class.getResource("/fxml/settings_dialog.fxml"), bundle);
            Parent root = loader.load();

            SettingsDialog controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                dialogStage.initOwner(owner);
            }
            dialogStage.setTitle(I18nService.getInstance().get("settings.title"));

            Scene scene = new Scene(root);
            ThemeService.getInstance().applyTheme(scene);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            controller.setStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            System.err.println("[SettingsDialog] Failed to open settings dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        UserSettings settings = SettingsService.getInstance().getSettings();

        // 1. Theme Selection
        AppTheme currentTheme = ThemeService.getInstance().getCurrentTheme();
        if (currentTheme == AppTheme.DARK_OBSIDIAN) {
            rbThemeDark.setSelected(true);
        } else if (currentTheme == AppTheme.MIDNIGHT_OLED) {
            rbThemeMidnight.setSelected(true);
        } else if (currentTheme == AppTheme.MINECRAFT_CLASSIC) {
            rbThemeMinecraft.setSelected(true);
        } else {
            rbThemeLight.setSelected(true);
        }

        // Live preview on theme radio click
        themeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            AppTheme selected = getSelectedTheme();
            if (selected != null) {
                ThemeService.getInstance().setTheme(selected);
            }
        });

        // 2. Language Selection
        String lang = settings.getLanguage();
        if ("en".equalsIgnoreCase(lang)) {
            rbLangEn.setSelected(true);
        } else {
            rbLangRu.setSelected(true);
        }

        // 3. Editor Preferences
        cbAutoSave.setSelected(settings.isAutoSaveEnabled());
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, settings.getAutoSaveIntervalMinutes());
        spAutoSaveInterval.setValueFactory(factory);
        spAutoSaveInterval.disableProperty().bind(cbAutoSave.selectedProperty().not());
        cbHighlightSyntax.setSelected(settings.isHighlightSyntax());

        // 4. System Info
        populateSystemInfo();

        // 5. Button Actions
        btnApply.setOnAction(e -> handleApply());
        btnClose.setOnAction(e -> handleClose());
    }

    private AppTheme getSelectedTheme() {
        if (rbThemeDark.isSelected()) return AppTheme.DARK_OBSIDIAN;
        if (rbThemeMidnight.isSelected()) return AppTheme.MIDNIGHT_OLED;
        if (rbThemeMinecraft.isSelected()) return AppTheme.MINECRAFT_CLASSIC;
        return AppTheme.LIGHT_GRADIENT;
    }

    private void populateSystemInfo() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
        lblOs.setText(os);

        String javaVer = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
        lblJava.setText(javaVer);

        String fxVer = System.getProperty("javafx.version", "21.0.2");
        lblJavafx.setText(fxVer);

        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        lblMemory.setText(usedMem + " MB / " + maxMem + " MB");

        txtConfigPath.setText(SettingsService.getInstance().getSettingsPath().toAbsolutePath().toString());
    }

    private void handleApply() {
        UserSettings settings = SettingsService.getInstance().getSettings();
        AppTheme chosenTheme = getSelectedTheme();
        settings.setTheme(chosenTheme);

        String chosenLang = rbLangEn.isSelected() ? "en" : "ru";
        boolean langChanged = !chosenLang.equalsIgnoreCase(settings.getLanguage());
        settings.setLanguage(chosenLang);

        settings.setAutoSaveEnabled(cbAutoSave.isSelected());
        if (spAutoSaveInterval.getValue() != null) {
            settings.setAutoSaveIntervalMinutes(spAutoSaveInterval.getValue());
        }
        settings.setHighlightSyntax(cbHighlightSyntax.isSelected());

        SettingsService.getInstance().save();
        ThemeService.getInstance().setTheme(chosenTheme);

        if (langChanged) {
            if ("en".equalsIgnoreCase(chosenLang)) {
                I18nService.getInstance().setLocale(Locale.ENGLISH);
            } else {
                I18nService.getInstance().setLocale(Locale.of("ru", "RU"));
            }
        }

        if (stage != null) {
            stage.close();
        }
    }

    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
