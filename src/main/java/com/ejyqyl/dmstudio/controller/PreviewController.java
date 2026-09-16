package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import com.ejyqyl.dmstudio.viewmodel.PreviewViewModel;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

/**
 * Controller for Live YAML Preview panel using RichTextFX (§6.5).
 */
public class PreviewController {

    @FXML private StackPane codeAreaContainer;
    @FXML private Button btnCopy;
    @FXML private Label copyStatusLabel;

    private CodeArea codeArea;
    private PreviewViewModel previewViewModel;

    @FXML
    public void initialize() {
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setEditable(false);
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-background-color: #1a1a22;");

        codeAreaContainer.getChildren().add(codeArea);

        btnCopy.setOnAction(e -> {
            if (previewViewModel != null) {
                previewViewModel.copyToClipboard();
                showCopiedFeedback();
            }
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.previewViewModel = mainViewModel.getPreviewViewModel();

        previewViewModel.yamlTextProperty().addListener((obs, oldVal, newVal) -> {
            codeArea.replaceText(newVal != null ? newVal : "");
        });

        codeArea.replaceText(previewViewModel.getYamlText());
    }

    private void showCopiedFeedback() {
        copyStatusLabel.setText(I18nService.getInstance().get("preview.copied"));
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> copyStatusLabel.setText(""));
        delay.play();
    }
}
