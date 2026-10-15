package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.head.HeadDatabaseService;
import com.ejyqyl.dmstudio.service.head.HeadEntry;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for Player Head / HeadDatabase configuration dialog (§HeadDatabase Support).
 */
public class HeadConfigurationDialog {

    @FXML private TabPane headTabPane;
    @FXML private Tab tabCatalog, tabCustom, tabPlayer;

    // Catalog tab
    @FXML private TextField catalogSearchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Label headsCountLabel, catalogSelectedInfoLabel;
    @FXML private TilePane headsTilePane;
    @FXML private RadioButton rbCatalogBasehead, rbCatalogHdb;

    // Custom tab
    @FXML private TextArea customValueArea;
    @FXML private RadioButton rbCustomBasehead, rbCustomHdb;
    @FXML private StackPane customPreviewBox;
    @FXML private Label customStatusLabel, customHintLabel;

    // Player tab
    @FXML private TextField playerNameField;
    @FXML private StackPane playerPreviewBox;

    // Bottom action bar
    @FXML private StackPane bottomPreviewBox;
    @FXML private TextField resultMaterialField;
    @FXML private Button btnApply, btnCancel;

    private Stage stage;
    private Consumer<String> onSelect;
    private HeadEntry selectedCatalogHead;

    @FXML
    public void initialize() {
        HeadDatabaseService service = HeadDatabaseService.getInstance();
        boolean isRu = "ru".equalsIgnoreCase(I18nService.getInstance().getCurrentLanguage());

        categoryCombo.setItems(FXCollections.observableArrayList(service.getCategories()));
        categoryCombo.setValue("ALL");

        categoryCombo.valueProperty().addListener((obs, oldV, newV) -> renderCatalog());
        catalogSearchField.textProperty().addListener((obs, oldV, newV) -> renderCatalog());

        rbCatalogBasehead.selectedProperty().addListener((obs, oldV, newV) -> updateCatalogSelection());
        rbCatalogHdb.selectedProperty().addListener((obs, oldV, newV) -> updateCatalogSelection());

        customValueArea.textProperty().addListener((obs, oldV, newV) -> parseCustomInput(newV));
        rbCustomBasehead.selectedProperty().addListener((obs, oldV, newV) -> parseCustomInput(customValueArea.getText()));
        rbCustomHdb.selectedProperty().addListener((obs, oldV, newV) -> parseCustomInput(customValueArea.getText()));

        playerNameField.textProperty().addListener((obs, oldV, newV) -> updatePlayerInput(newV));

        btnApply.setOnAction(e -> confirmSelection());
        btnCancel.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        headTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == tabCatalog) updateCatalogSelection();
            else if (newV == tabCustom) parseCustomInput(customValueArea.getText());
            else if (newV == tabPlayer) updatePlayerInput(playerNameField.getText());
        });

        renderCatalog();
    }

    private void renderCatalog() {
        headsTilePane.getChildren().clear();
        HeadDatabaseService service = HeadDatabaseService.getInstance();
        boolean isRu = "ru".equalsIgnoreCase(I18nService.getInstance().getCurrentLanguage());
        String cat = categoryCombo.getValue();
        String query = catalogSearchField.getText();

        List<HeadEntry> list;
        if (query != null && !query.isBlank()) {
            list = service.search(query, isRu);
            if (cat != null && !"ALL".equalsIgnoreCase(cat)) {
                list = list.stream().filter(h -> cat.equalsIgnoreCase(h.category())).toList();
            }
        } else {
            list = service.getByCategory(cat);
        }

        headsCountLabel.setText(I18nService.getInstance().get("dialog.head.count", list.size()));

        for (HeadEntry head : list) {
            StackPane tile = new StackPane();
            tile.setPrefSize(48, 48);
            tile.setMinSize(48, 48);
            tile.setMaxSize(48, 48);
            tile.getStyleClass().add("inventory-slot");

            Node icon = service.createHeadIcon("hdb-" + head.id(), 32);
            if (icon != null) tile.getChildren().add(icon);

            String tooltip = "#" + head.id() + " - " + head.getLocalizedName(isRu);
            Tooltip.install(tile, new Tooltip(tooltip));

            tile.setOnMouseClicked(e -> {
                selectedCatalogHead = head;
                updateCatalogSelection();
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    confirmSelection();
                }
            });

            headsTilePane.getChildren().add(tile);
        }

        if (selectedCatalogHead == null && !list.isEmpty()) {
            selectedCatalogHead = list.get(0);
        }
        updateCatalogSelection();
    }

    private void updateCatalogSelection() {
        if (selectedCatalogHead == null) return;
        boolean isRu = "ru".equalsIgnoreCase(I18nService.getInstance().getCurrentLanguage());
        catalogSelectedInfoLabel.setText("#" + selectedCatalogHead.id() + " " + selectedCatalogHead.getLocalizedName(isRu));

        String mat;
        if (rbCatalogHdb.isSelected()) {
            mat = "hdb-" + selectedCatalogHead.id();
        } else {
            mat = "basehead-" + selectedCatalogHead.value();
        }
        setResultMaterial(mat);
    }

    private void parseCustomInput(String input) {
        if (input == null || input.isBlank()) {
            customStatusLabel.setText(I18nService.getInstance().get("dialog.head.status_empty"));
            customHintLabel.setText("");
            customPreviewBox.getChildren().clear();
            setResultMaterial("");
            return;
        }

        String raw = input.trim();
        HeadDatabaseService service = HeadDatabaseService.getInstance();
        String resultMat = "";

        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            String b64 = HeadDatabaseService.urlToBaseheadValue(raw);
            resultMat = "basehead-" + b64;
            customStatusLabel.setText(I18nService.getInstance().get("dialog.head.status_url_converted"));
        } else if (raw.toLowerCase(Locale.ROOT).startsWith("hdb-")) {
            resultMat = raw;
            customStatusLabel.setText("HeadDatabase ID: " + raw.substring(4));
        } else if (raw.toLowerCase(Locale.ROOT).startsWith("basehead-")) {
            resultMat = raw;
            customStatusLabel.setText("Base64 Head Value");
        } else if (raw.toLowerCase(Locale.ROOT).startsWith("head-")) {
            resultMat = raw;
            customStatusLabel.setText("Player Skull: " + raw.substring(5));
        } else if (raw.matches("\\d+")) {
            if (rbCustomHdb.isSelected()) {
                resultMat = "hdb-" + raw;
            } else {
                Optional<HeadEntry> h = service.findById(raw);
                resultMat = h.isPresent() ? "basehead-" + h.get().value() : "hdb-" + raw;
            }
            customStatusLabel.setText("Head ID: " + raw);
        } else {
            // Raw Base64 string
            if (rbCustomHdb.isSelected()) {
                Optional<HeadEntry> h = service.findByValue(raw);
                resultMat = h.isPresent() ? "hdb-" + h.get().id() : "basehead-" + raw;
            } else {
                resultMat = "basehead-" + raw;
            }
            customStatusLabel.setText("Custom Base64 Skin");
        }

        String hash = HeadDatabaseService.extractTextureHash(resultMat);
        customHintLabel.setText(hash != null ? "Hash: " + hash.substring(0, Math.min(16, hash.length())) + "..." : "");

        Node icon = TextureManager.getInstance().createIconNode(resultMat, 32);
        customPreviewBox.getChildren().setAll(icon);

        setResultMaterial(resultMat);
    }

    private void updatePlayerInput(String name) {
        String clean = (name != null && !name.isBlank()) ? name.trim() : "Steve";
        String mat = "head-" + clean;
        Node icon = TextureManager.getInstance().createIconNode(mat, 32);
        playerPreviewBox.getChildren().setAll(icon);
        setResultMaterial(mat);
    }

    private void setResultMaterial(String mat) {
        resultMaterialField.setText(mat);
        bottomPreviewBox.getChildren().clear();
        if (mat != null && !mat.isBlank()) {
            Node icon = TextureManager.getInstance().createIconNode(mat, 32);
            bottomPreviewBox.getChildren().add(icon);
        }
    }

    private void confirmSelection() {
        String result = resultMaterialField.getText();
        if (result != null && !result.isBlank()) {
            if (onSelect != null) {
                onSelect.accept(result.trim());
            }
            if (stage != null) {
                stage.close();
            }
        }
    }

    public static void show(Window owner, String initialMaterial, Consumer<String> onSelect) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HeadConfigurationDialog.class.getResource("/fxml/head_dialog.fxml"),
                    I18nService.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle(I18nService.getInstance().get("dialog.head.title"));
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);

            Scene scene = new Scene(loader.load());
            com.ejyqyl.dmstudio.service.theme.ThemeService.getInstance().applyTheme(scene);

            HeadConfigurationDialog controller = loader.getController();
            controller.stage = stage;
            controller.onSelect = onSelect;

            if (initialMaterial != null && !initialMaterial.isBlank()) {
                String init = initialMaterial.trim();
                String lower = init.toLowerCase(Locale.ROOT);
                if (lower.startsWith("head-")) {
                    controller.headTabPane.getSelectionModel().select(controller.tabPlayer);
                    controller.playerNameField.setText(init.substring(5));
                } else if (lower.startsWith("basehead-") || lower.startsWith("http")) {
                    controller.headTabPane.getSelectionModel().select(controller.tabCustom);
                    controller.customValueArea.setText(init);
                } else if (lower.startsWith("hdb-")) {
                    controller.headTabPane.getSelectionModel().select(controller.tabCatalog);
                    controller.catalogSearchField.setText(init.substring(4));
                } else {
                    controller.setResultMaterial(init);
                }
            }

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
