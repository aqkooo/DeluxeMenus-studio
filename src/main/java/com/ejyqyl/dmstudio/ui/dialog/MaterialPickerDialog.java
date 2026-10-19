package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.registry.CapabilityRegistry;
import com.ejyqyl.dmstudio.service.registry.MaterialDescriptor;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import javafx.application.Platform;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Visual Minecraft block and item picker dialog with category filters and real texture preview.
 */
public class MaterialPickerDialog {

    private static final int PAGE_SIZE = 72; // 8 rows of 9 items

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TilePane materialsTilePane;
    @FXML private Label statsLabel;
    @FXML private Button btnPrevPage;
    @FXML private Label pageLabel;
    @FXML private Button btnNextPage;
    @FXML private StackPane previewIconBox;
    @FXML private TextField customInputField;
    @FXML private Button btnConfigureHead;
    @FXML private Button btnSelect;
    @FXML private Button btnCancel;

    private Stage stage;
    private Consumer<String> onSelect;
    private final List<MaterialDescriptor> allMaterials = new ArrayList<>();
    private final List<MaterialDescriptor> filteredMaterials = new ArrayList<>();
    private int currentPage = 0;

    @FXML
    public void initialize() {
        I18nService i18n = I18nService.getInstance();
        allMaterials.addAll(CapabilityRegistry.getInstance().getMaterials());

        categoryCombo.setItems(FXCollections.observableArrayList(
                "ALL", "BLOCKS", "DECORATIONS", "REDSTONE", "TOOLS", "COMBAT", "FOOD", "MISC"
        ));
        categoryCombo.setValue("ALL");

        categoryCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        customInputField.textProperty().addListener((obs, oldV, newV) -> updatePreviewIcon(newV));

        btnPrevPage.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                renderPage();
            }
        });

        btnNextPage.setOnAction(e -> {
            int maxPage = Math.max(0, (filteredMaterials.size() - 1) / PAGE_SIZE);
            if (currentPage < maxPage) {
                currentPage++;
                renderPage();
            }
        });

        if (btnConfigureHead != null) {
            btnConfigureHead.setOnAction(e -> {
                HeadConfigurationDialog.show(stage, customInputField.getText(), val -> {
                    customInputField.setText(val);
                    updatePreviewIcon(val);
                });
            });
        }
        btnSelect.setOnAction(e -> confirmSelection());
        btnCancel.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        applyFilter();
    }

    private void applyFilter() {
        String cat = categoryCombo.getValue();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toUpperCase(Locale.ROOT);

        filteredMaterials.clear();
        for (MaterialDescriptor mat : allMaterials) {
            boolean catMatch = "ALL".equalsIgnoreCase(cat) || cat == null || cat.equalsIgnoreCase(mat.category());
            boolean queryMatch = query.isEmpty() || mat.id().toUpperCase(Locale.ROOT).contains(query);
            if (catMatch && queryMatch) {
                filteredMaterials.add(mat);
            }
        }

        currentPage = 0;
        statsLabel.setText(I18nService.getInstance().get("dialog.material_picker.items_found", filteredMaterials.size()));
        renderPage();
    }

    private void renderPage() {
        materialsTilePane.getChildren().clear();
        int total = filteredMaterials.size();
        int maxPage = Math.max(0, (total - 1) / PAGE_SIZE);
        if (currentPage > maxPage) currentPage = maxPage;

        pageLabel.setText((currentPage + 1) + " / " + Math.max(1, maxPage + 1));
        btnPrevPage.setDisable(currentPage <= 0);
        btnNextPage.setDisable(currentPage >= maxPage);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(total, start + PAGE_SIZE);

        for (int i = start; i < end; i++) {
            MaterialDescriptor mat = filteredMaterials.get(i);
            StackPane tile = createTile(mat.id());
            materialsTilePane.getChildren().add(tile);
        }
    }

    private StackPane selectedTile;

    private StackPane createTile(String matId) {
        StackPane tile = new StackPane();
        tile.setPrefSize(48, 48);
        tile.setMinSize(48, 48);
        tile.setMaxSize(48, 48);
        tile.getStyleClass().add("inventory-slot");

        Node icon = TextureManager.getInstance().createIconNode(matId, 32);
        tile.getChildren().add(icon);

        Tooltip.install(tile, new Tooltip(matId));

        if (customInputField.getText() != null && customInputField.getText().equalsIgnoreCase(matId)) {
            selectedTile = tile;
            tile.setStyle("-fx-border-color: -accent; -fx-border-width: 2px; -fx-border-radius: 4px;");
        }

        tile.setOnMouseClicked(e -> {
            if (selectedTile != null) {
                selectedTile.setStyle("");
            }
            selectedTile = tile;
            tile.setStyle("-fx-border-color: -accent; -fx-border-width: 2px; -fx-border-radius: 4px;");
            customInputField.setText(matId);
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                confirmSelection();
            }
        });

        return tile;
    }

    private void updatePreviewIcon(String matId) {
        previewIconBox.getChildren().clear();
        if (matId != null && !matId.isBlank()) {
            Node icon = TextureManager.getInstance().createIconNode(matId.trim(), 32);
            previewIconBox.getChildren().add(icon);
        }
    }

    private void confirmSelection() {
        String selected = customInputField.getText();
        if (selected != null && !selected.isBlank()) {
            if (onSelect != null) {
                onSelect.accept(selected.trim());
            }
            if (stage != null) {
                stage.close();
            }
        }
    }

    public static void show(Window owner, String initialMaterial, Consumer<String> onSelect) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MaterialPickerDialog.class.getResource("/fxml/material_picker.fxml"),
                    I18nService.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle(I18nService.getInstance().get("dialog.material_picker.title"));
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);

            Scene scene = new Scene(loader.load());
            com.ejyqyl.dmstudio.service.theme.ThemeService.getInstance().applyTheme(scene);

            MaterialPickerDialog controller = loader.getController();
            controller.stage = stage;
            controller.onSelect = onSelect;

            if (initialMaterial != null && !initialMaterial.isBlank()) {
                controller.customInputField.setText(initialMaterial);
                controller.searchField.setText(initialMaterial);
            }

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
