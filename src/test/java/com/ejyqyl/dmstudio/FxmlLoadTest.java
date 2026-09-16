package com.ejyqyl.dmstudio;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class FxmlLoadTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testAllFxmlFilesLoadWithoutError() {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", Locale.of("ru", "RU"));

        String[] fxmlFiles = {
                "/fxml/grid.fxml",
                "/fxml/menu_properties.fxml",
                "/fxml/item_inspector.fxml",
                "/fxml/preview.fxml",
                "/fxml/problems.fxml",
                "/fxml/main.fxml"
        };

        for (String file : fxmlFiles) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(file), bundle);
            Object root = null;
            try {
                root = loader.load();
            } catch (Exception e) {
                throw new AssertionError("Failed to load FXML: " + file, e);
            }
            assertThat(root).as("Root element of " + file).isNotNull();
        }
    }
}
