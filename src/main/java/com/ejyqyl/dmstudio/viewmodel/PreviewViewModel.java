package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.yaml.YamlExportService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * ViewModel for live YAML code preview with 150-250ms debounce (§6.5).
 */
public class PreviewViewModel {

    private final MenuConfig config;
    private final YamlExportService exportService = new YamlExportService();
    private final StringProperty yamlText = new SimpleStringProperty("");
    private Timeline debounceTimeline;

    public PreviewViewModel(MenuConfig config) {
        this.config = config;
        initDebounce();
        refreshImmediate();
    }

    private void initDebounce() {
        // Debounce of 200 ms (within specified 150-250 ms budget)
        debounceTimeline = new Timeline(new KeyFrame(Duration.millis(200), evt -> refreshImmediate()));
        debounceTimeline.setCycleCount(1);
    }

    public void requestUpdate() {
        try {
            if (Platform.isFxApplicationThread()) {
                debounceTimeline.stop();
                debounceTimeline.playFromStart();
            } else {
                Platform.runLater(this::requestUpdate);
            }
        } catch (IllegalStateException e) {
            // Running in headless test environment without FX Toolkit
            refreshImmediate();
        }
    }

    public void refreshImmediate() {
        try {
            String dumped = exportService.exportToString(config);
            yamlText.set(dumped);
        } catch (Exception e) {
            yamlText.set("# Error generating preview: " + e.getMessage());
        }
    }

    public void copyToClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(yamlText.get());
        clipboard.setContent(content);
    }

    public StringProperty yamlTextProperty() {
        return yamlText;
    }

    public String getYamlText() {
        return yamlText.get();
    }
}
