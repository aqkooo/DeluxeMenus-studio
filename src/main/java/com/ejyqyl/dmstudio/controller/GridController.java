package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import com.ejyqyl.dmstudio.ui.dialog.HeadConfigurationDialog;
import com.ejyqyl.dmstudio.ui.dialog.MaterialPickerDialog;
import com.ejyqyl.dmstudio.ui.dialog.TemplatesDialog;
import com.ejyqyl.dmstudio.view.SlotCell;
import com.ejyqyl.dmstudio.viewmodel.GridViewModel;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for visual inventory grid with drag-and-drop, slot selection, and layout presets (§6.2).
 */
public class GridController {

    @FXML private GridPane inventoryGrid;
    @FXML private TextField gridSearchField;
    @FXML private ToggleButton btnBrush;
    @FXML private Button btnBorder;
    @FXML private Button btnTemplates;
    @FXML private Button btnClearAll;

    @FXML private HBox paletteBar;
    @FXML private HBox paletteItemsBox;
    @FXML private Button btnChoosePaletteMaterial, btnHeadPalette;
    @FXML private StackPane activeMaterialPreview;
    @FXML private Label activeMaterialNameLabel;

    private String activePlacingMaterial = "GRAY_STAINED_GLASS_PANE";
    private MainViewModel mainViewModel;
    private GridViewModel gridViewModel;
    private GridKeyHandler keyHandler;
    private final List<SlotCell> slotCells = new ArrayList<>();

    @FXML
    public void initialize() {
        initPaletteBar();
        if (gridSearchField != null) {
            gridSearchField.textProperty().addListener((obs, oldV, newV) -> applyGridFilter(newV));
        }
        if (btnBrush != null) {
            btnBrush.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV && mainViewModel != null) {
                    mainViewModel.setStatusMessage(I18nService.getInstance().get("grid.brush.active"));
                }
            });
        }
        btnBorder.setOnAction(e -> {
            if (gridViewModel != null) gridViewModel.applyPresetBorder("GRAY_STAINED_GLASS_PANE");
        });
        btnTemplates.setOnAction(e -> {
            if (mainViewModel != null) TemplatesDialog.show(inventoryGrid.getScene().getWindow(), mainViewModel);
        });
        btnClearAll.setOnAction(e -> {
            if (gridViewModel != null) gridViewModel.applyPresetClearAll();
        });

        inventoryGrid.setFocusTraversable(true);
        inventoryGrid.setOnKeyPressed(e -> {
            if (keyHandler != null) keyHandler.handleKeyPressed(e);
        });
    }

    private void initPaletteBar() {
        setActivePlacingMaterial("GRAY_STAINED_GLASS_PANE");

        String[] quickMats = {
                "BLACK_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", "WHITE_STAINED_GLASS_PANE",
                "LIME_STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
                "STONE", "OAK_PLANKS", "CHEST", "DIAMOND", "GOLD_INGOT",
                "NETHERITE_SWORD", "COMPASS", "CLOCK", "BARRIER"
        };
        for (String qm : quickMats) {
            Button b = new Button();
            b.setGraphic(TextureManager.getInstance().createIconNode(qm, 18));
            b.setTooltip(new Tooltip(qm));
            b.getStyleClass().add("button");
            b.setPrefSize(26, 26);
            b.setMinSize(26, 26);
            b.setMaxSize(26, 26);
            b.setStyle("-fx-padding: 0;");
            b.setOnAction(e -> onSelectPaletteMaterial(qm));
            if (paletteItemsBox != null) {
                paletteItemsBox.getChildren().add(b);
            }
        }

        if (btnChoosePaletteMaterial != null) {
            btnChoosePaletteMaterial.setOnAction(e -> {
                Window win = inventoryGrid.getScene() != null ? inventoryGrid.getScene().getWindow() : null;
                MaterialPickerDialog.show(win, activePlacingMaterial, this::onSelectPaletteMaterial);
            });
        }
        if (btnHeadPalette != null) {
            btnHeadPalette.setOnAction(e -> {
                Window win = inventoryGrid.getScene() != null ? inventoryGrid.getScene().getWindow() : null;
                HeadConfigurationDialog.show(win, activePlacingMaterial, this::onSelectPaletteMaterial);
            });
        }
    }

    public void setActivePlacingMaterial(String mat) {
        if (mat == null || mat.isBlank()) return;
        this.activePlacingMaterial = mat.trim();
        if (activeMaterialNameLabel != null) activeMaterialNameLabel.setText(activePlacingMaterial);
        if (activeMaterialPreview != null) {
            activeMaterialPreview.getChildren().setAll(TextureManager.getInstance().createIconNode(activePlacingMaterial, 20));
        }
    }

    private void onSelectPaletteMaterial(String material) {
        setActivePlacingMaterial(material);
        if (gridViewModel != null) {
            if (!gridViewModel.getMultiSelectedSlots().isEmpty()) {
                gridViewModel.setMaterialForSelectedSlots(material);
            } else if (gridViewModel.getSelectedSlot() >= 0) {
                gridViewModel.changeMaterialAt(gridViewModel.getSelectedSlot(), material);
            } else {
                if (btnBrush != null) btnBrush.setSelected(true);
            }
        }
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.gridViewModel = mainViewModel.getGridViewModel();
        this.keyHandler = new GridKeyHandler(mainViewModel, gridViewModel);

        gridViewModel.selectedSlotProperty().addListener((obs, oldV, newV) -> renderGrid());
        gridViewModel.getMultiSelectedSlots().addListener((javafx.collections.SetChangeListener<Integer>) c -> renderGrid());
        mainViewModel.configProperty().addListener((obs, oldV, newV) -> renderGrid());
        mainViewModel.getCommandStack().addChangeListener(this::renderGrid);

        renderGrid();
    }

    public void renderGrid() {
        if (mainViewModel == null || mainViewModel.getConfig() == null) return;

        MenuConfig config = mainViewModel.getConfig();
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();

        if (slotCells.size() != size) {
            inventoryGrid.getChildren().clear();
            slotCells.clear();

            for (int i = 0; i < size; i++) {
                int row = i / cols;
                int col = i % cols;
                SlotCell cell = new SlotCell(i);
                setupInteractivity(cell, i);
                slotCells.add(cell);
                inventoryGrid.add(cell, col, row);
            }
        }

        for (int i = 0; i < size; i++) {
            List<MenuItem> items = config.getItemsAtSlot(i);
            boolean isSelected = gridViewModel.getMultiSelectedSlots().contains(i);
            slotCells.get(i).update(items, isSelected);
        }
        if (gridSearchField != null && !gridSearchField.getText().isBlank()) {
            applyGridFilter(gridSearchField.getText());
        }
    }

    private void setupInteractivity(SlotCell slotCell, int slotIndex) {
        slotCell.setOnMouseClicked(e -> {
            inventoryGrid.requestFocus();
            if (e.getButton() == MouseButton.SECONDARY) {
                if (btnBrush != null && btnBrush.isSelected()) {
                    gridViewModel.clearSlot(slotIndex);
                }
                return;
            }

            if (e.getButton() == MouseButton.PRIMARY) {
                if (btnBrush != null && btnBrush.isSelected()) {
                    gridViewModel.changeMaterialAt(slotIndex, activePlacingMaterial);
                    return;
                }

                List<MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(slotIndex);
                if (e.getClickCount() == 2 && items.isEmpty()) {
                    Window win = slotCell.getScene() != null ? slotCell.getScene().getWindow() : null;
                    MaterialPickerDialog.show(win, activePlacingMaterial, chosen -> {
                        setActivePlacingMaterial(chosen);
                        gridViewModel.changeMaterialAt(slotIndex, chosen);
                    });
                } else if (e.isShiftDown()) {
                    int prev = gridViewModel.getSelectedSlot();
                    if (prev >= 0) {
                        gridViewModel.selectRange(prev, slotIndex);
                    } else {
                        gridViewModel.selectSlot(slotIndex);
                    }
                } else if (e.isControlDown() || e.isShortcutDown()) {
                    gridViewModel.toggleMultiSelectSlot(slotIndex);
                } else {
                    gridViewModel.selectSlot(slotIndex);
                }
            }
        });

        slotCell.setOnContextMenuRequested(e -> {
            if (btnBrush != null && btnBrush.isSelected()) return;
            ContextMenu menu = GridContextMenuBuilder.build(
                    slotCell, slotIndex, mainViewModel, gridViewModel, activePlacingMaterial, this::setActivePlacingMaterial
            );
            menu.show(slotCell, e.getScreenX(), e.getScreenY());
        });

        slotCell.setOnDragDetected(e -> {
            if (btnBrush != null && btnBrush.isSelected()) {
                slotCell.startFullDrag();
                gridViewModel.changeMaterialAt(slotIndex, activePlacingMaterial);
                e.consume();
                return;
            }

            List<MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(slotIndex);
            if (!items.isEmpty()) {
                boolean copyMode = e.isControlDown() || e.isAltDown();
                Dragboard db = slotCell.startDragAndDrop(copyMode ? TransferMode.COPY : TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(slotIndex));
                db.setContent(content);
                e.consume();
            }
        });

        slotCell.setOnMouseDragEntered(e -> {
            if (btnBrush != null && btnBrush.isSelected()) {
                gridViewModel.changeMaterialAt(slotIndex, activePlacingMaterial);
            }
        });

        slotCell.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                if (!slotCell.getStyleClass().contains("inventory-slot-drag-target")) {
                    slotCell.getStyleClass().add("inventory-slot-drag-target");
                }
            }
            e.consume();
        });

        slotCell.setOnDragExited(e -> {
            slotCell.getStyleClass().remove("inventory-slot-drag-target");
            e.consume();
        });

        slotCell.setOnDragDropped(e -> {
            slotCell.getStyleClass().remove("inventory-slot-drag-target");
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    int fromSlot = Integer.parseInt(db.getString());
                    gridViewModel.moveItem(fromSlot, slotIndex, e.getTransferMode() == TransferMode.COPY);
                    success = true;
                } catch (NumberFormatException ignored) {}
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    public void applyGridFilter(String query) {
        if (slotCells.isEmpty() || mainViewModel == null || mainViewModel.getConfig() == null) return;
        boolean hasFilter = query != null && !query.isBlank();
        String q = hasFilter ? query.toLowerCase().trim() : "";
        for (int i = 0; i < slotCells.size(); i++) {
            if (!hasFilter) {
                slotCells.get(i).setFilterHighlight(true, false);
                continue;
            }
            List<MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(i);
            boolean match = false;
            for (MenuItem it : items) {
                if (it.getMaterial() != null && it.getMaterial().toLowerCase().contains(q)) { match = true; break; }
                if (it.getAppearance() != null) {
                    if (it.getAppearance().getDisplayName() != null && it.getAppearance().getDisplayName().toLowerCase().contains(q)) { match = true; break; }
                    for (String line : it.getAppearance().getLore()) {
                        if (line != null && line.toLowerCase().contains(q)) { match = true; break; }
                    }
                }
            }
            slotCells.get(i).setFilterHighlight(match, true);
        }
    }
}
