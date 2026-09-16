package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.render.YamlHighlighter;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import com.ejyqyl.dmstudio.viewmodel.PreviewViewModel;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

/**
 * Controller for Live YAML Preview panel using RichTextFX (§6.5).
 */
public class PreviewController {

    @FXML private StackPane codeAreaContainer;
    @FXML private Button btnCopy;
    @FXML private Label copyStatusLabel;
    @FXML private Label formatHintLabel;

    private CodeArea codeArea;
    private PreviewViewModel previewViewModel;

    @FXML
    public void initialize() {
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setEditable(false);
        codeArea.getStyleClass().add("code-area");

        VirtualizedScrollPane<CodeArea> vsPane = new VirtualizedScrollPane<>(codeArea);
        codeAreaContainer.getChildren().add(vsPane);

        btnCopy.setOnAction(e -> {
            if (previewViewModel != null) {
                previewViewModel.copyToClipboard();
                showCopiedFeedback();
            }
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.previewViewModel = mainViewModel.getPreviewViewModel();

        previewViewModel.yamlTextProperty().addListener((obs, oldVal, newVal) -> updateYamlView(newVal));
        updateYamlView(previewViewModel.getYamlText());
    }

    private void updateYamlView(String yaml) {
        String safeYaml = yaml != null ? yaml : "";
        codeArea.replaceText(safeYaml);
        if (!safeYaml.isEmpty()) {
            codeArea.setStyleSpans(0, YamlHighlighter.computeHighlighting(safeYaml));
        }
    }

    private void showCopiedFeedback() {
        copyStatusLabel.setText(I18nService.getInstance().get("preview.copied"));
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> copyStatusLabel.setText(""));
        delay.play();
    }
}
