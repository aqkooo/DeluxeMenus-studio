package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureExtractorService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for selecting Minecraft JAR/zip and extracting item/block textures.
 */
public final class TextureExtractorDialog {

    private TextureExtractorDialog() {}

    public static void show(Stage stage, Runnable onComplete) {
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
                if (onComplete != null) onComplete.run();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18nService.getInstance().get("dialog.extract_textures_title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nService.getInstance().get("dialog.extract_textures_success"));
                alert.showAndWait();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle(I18nService.getInstance().get("dialog.error_title"));
                err.setHeaderText(null);
                err.setContentText(I18nService.getInstance().get("dialog.extract_textures_failed", selectedPath.getFileName()));
                err.showAndWait();
            }
        }
    }
}
