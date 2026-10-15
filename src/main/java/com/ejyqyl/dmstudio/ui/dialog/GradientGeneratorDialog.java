package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.render.ColorCodeParser;
import com.ejyqyl.dmstudio.service.render.GradientGeneratorService;
import com.ejyqyl.dmstudio.service.render.GradientGeneratorService.FormatMode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Interactive dialog controller for designing multi-color gradients (§Visual Tools).
 */
public class GradientGeneratorDialog {

    @FXML private TextField txtInput;
    @FXML private ColorPicker startColorPicker;
    @FXML private CheckBox chkMiddleColor;
    @FXML private ColorPicker middleColorPicker;
    @FXML private ColorPicker endColorPicker;
    @FXML private ComboBox<FormatMode> formatCombo;
    @FXML private CheckBox chkBold;
    @FXML private CheckBox chkItalic;
    @FXML private CheckBox chkUnderline;
    @FXML private CheckBox chkStrikethrough;
    @FXML private TextFlow previewFlow;
    @FXML private TextField txtOutput;
    @FXML private Button btnApplyName;
    @FXML private Button btnApplyLore;
    @FXML private Button btnCopy;
    @FXML private Button btnClose;

    private Consumer<String> onApplyToName;
    private Consumer<String> onApplyToLore;
    private Stage stage;

    @FXML
    public void initialize() {
        startColorPicker.setValue(Color.web("#e74c3c"));
        middleColorPicker.setValue(Color.web("#f1c40f"));
        endColorPicker.setValue(Color.web("#3498db"));

        formatCombo.getItems().setAll(FormatMode.DELUXE_MENUS_HEX, FormatMode.MINI_MESSAGE, FormatMode.SPIGOT_LEGACY_HEX);
        formatCombo.setValue(FormatMode.DELUXE_MENUS_HEX);

        chkMiddleColor.selectedProperty().addListener((obs, oldV, newV) -> {
            middleColorPicker.setDisable(!newV);
            recalculate();
        });

        txtInput.textProperty().addListener((obs, oldV, newV) -> recalculate());
        startColorPicker.valueProperty().addListener((obs, oldV, newV) -> recalculate());
        middleColorPicker.valueProperty().addListener((obs, oldV, newV) -> recalculate());
        endColorPicker.valueProperty().addListener((obs, oldV, newV) -> recalculate());
        formatCombo.valueProperty().addListener((obs, oldV, newV) -> recalculate());

        chkBold.selectedProperty().addListener((obs, oldV, newV) -> recalculate());
        chkItalic.selectedProperty().addListener((obs, oldV, newV) -> recalculate());
        chkUnderline.selectedProperty().addListener((obs, oldV, newV) -> recalculate());
        chkStrikethrough.selectedProperty().addListener((obs, oldV, newV) -> recalculate());

        btnCopy.setOnAction(e -> {
            String text = txtOutput.getText();
            if (text != null && !text.isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
            }
        });

        btnApplyName.setOnAction(e -> {
            if (onApplyToName != null) {
                onApplyToName.accept(txtOutput.getText());
                if (stage != null) stage.close();
            }
        });

        btnApplyLore.setOnAction(e -> {
            if (onApplyToLore != null) {
                onApplyToLore.accept(txtOutput.getText());
                if (stage != null) stage.close();
            }
        });

        btnClose.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        txtInput.setText("DeluxeMenus Studio");
    }

    private void recalculate() {
        String input = txtInput.getText();
        if (input == null || input.isEmpty()) {
            txtOutput.setText("");
            previewFlow.getChildren().clear();
            return;
        }

        Color start = startColorPicker.getValue();
        Color middle = chkMiddleColor.isSelected() ? middleColorPicker.getValue() : null;
        Color end = endColorPicker.getValue();
        FormatMode mode = formatCombo.getValue() != null ? formatCombo.getValue() : FormatMode.DELUXE_MENUS_HEX;

        String formatted = GradientGeneratorService.generateGradient(
                input, start, middle, end, mode,
                chkBold.isSelected(), chkItalic.isSelected(),
                chkUnderline.isSelected(), chkStrikethrough.isSelected()
        );

        txtOutput.setText(formatted);

        // Update live preview TextFlow using standard parser
        previewFlow.getChildren().clear();
        ColorCodeParser.populateTextFlow(previewFlow, formatted, Color.WHITE);
    }

    public static void show(Window owner, Consumer<String> onApplyName, Consumer<String> onApplyLore) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GradientGeneratorDialog.class.getResource("/fxml/gradient_dialog.fxml"),
                    I18nService.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle(I18nService.getInstance().get("tools.gradient.title"));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);

            Scene scene = new Scene(loader.load());
            com.ejyqyl.dmstudio.service.theme.ThemeService.getInstance().applyTheme(scene);

            GradientGeneratorDialog controller = loader.getController();
            controller.stage = stage;
            controller.onApplyToName = onApplyName;
            controller.onApplyToLore = onApplyLore;

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
