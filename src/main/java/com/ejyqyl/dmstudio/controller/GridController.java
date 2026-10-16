package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import com.ejyqyl.dmstudio.ui.dialog.ComponentsDialog;
import com.ejyqyl.dmstudio.ui.dialog.MaterialPickerDialog;
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

    private static final DataFormat SLOT_FORMAT = new DataFormat("application/x-deluxemenus-slot");

    @FXML private GridPane inventoryGrid;
    @FXML private TextField gridSearchField;
    @FXML private ToggleButton btnBrush;
    @FXML private Button btnBorder;
    @FXML private Button btnChecker;
    @FXML private Button btnClearAll;

    @FXML private HBox paletteBar;
    @FXML private HBox paletteItemsBox;
    @FXML private Button btnChoosePaletteMaterial, btnHeadPalette;
    @FXML private StackPane activeMaterialPreview;
    @FXML private Label activeMaterialNameLabel;

    private String activePlacingMaterial = "GRAY_STAINED_GLASS_PANE";
    private MainViewModel mainViewModel;
    private GridViewModel gridViewModel;
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
        btnChecker.setOnAction(e -> {
            if (gridViewModel != null) gridViewModel.applyPresetCheckerboard("BLACK_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE");
        });
        btnClearAll.setOnAction(e -> {
            if (gridViewModel != null) gridViewModel.applyPresetClearAll();
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
                com.ejyqyl.dmstudio.ui.dialog.HeadConfigurationDialog.show(win, activePlacingMaterial, this::onSelectPaletteMaterial);
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
        int rows = Math.max(1, (size + cols - 1) / cols);

        // Recreate slot cell pool only when size or columns change
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

        // Fast differential in-place update without allocating new nodes
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
        // Mouse click
        slotCell.setOnMouseClicked(e -> {
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
                    gridViewModel.toggleMultiSelectSlot(slotIndex);
                } else {
                    gridViewModel.selectSlot(slotIndex);
                }
            }
        });

        // Context menu (RMB)
        slotCell.setOnContextMenuRequested(e -> {
            if (btnBrush != null && btnBrush.isSelected()) return;
            ContextMenu menu = createContextMenu(slotCell, slotIndex);
            menu.show(slotCell, e.getScreenX(), e.getScreenY());
        });

        // Drag to paint with Brush or standard Drag and Drop
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
            }
            e.consume();
        });

        slotCell.setOnDragDropped(e -> {
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

    private ContextMenu createContextMenu(SlotCell slotCell, int slotIndex) {
        ContextMenu menu = new ContextMenu();
        I18nService i18n = I18nService.getInstance();
        List<MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(slotIndex);
        boolean hasItems = !items.isEmpty();
        String currentMat = hasItems ? items.get(0).getMaterial() : "STONE";

        menu.getItems().addAll(
                createMenuItem(i18n.get("grid.context.choose_material"), "CHEST", false,
                        ev -> MaterialPickerDialog.show(slotCell.getScene().getWindow(), currentMat, c -> setMaterial(slotIndex, c))),
                createMenuItem(i18n.get("grid.context.configure_head"), "PLAYER_HEAD", false,
                        ev -> com.ejyqyl.dmstudio.ui.dialog.HeadConfigurationDialog.show(slotCell.getScene().getWindow(), currentMat, c -> setMaterial(slotIndex, c))),
                createQuickPaletteMenu(i18n.get("grid.context.quick_palette"), slotIndex),
                createMenuItem(i18n.get("grid.context.insert_component"), "CRAFTING_TABLE", false, ev -> {
                    ComponentsDialog.show(slotCell.getScene().getWindow(), slotIndex, (compItem, targetSlot) -> {
                        mainViewModel.getCommandStack().execute(new SetItemCommand(mainViewModel.getConfig(), compItem));
                        gridViewModel.selectSlot(targetSlot);
                    });
                }),
                new SeparatorMenuItem(),
                createMenuItem(i18n.get("grid.context.copy"), null, !hasItems, ev -> gridViewModel.copyItemAt(slotIndex)),
                createMenuItem(i18n.get("grid.context.paste"), null, !gridViewModel.hasClipboardItem(), ev -> gridViewModel.pasteItemAt(slotIndex)),
                createMenuItem(i18n.get("grid.context.duplicate"), null, !hasItems, ev -> gridViewModel.duplicateItemAt(slotIndex)),
                createMenuItem(i18n.get("grid.context.lower_priority"), null, !hasItems, ev -> gridViewModel.createLowerPriorityVariant(slotIndex)),
                new SeparatorMenuItem(),
                createMenuItem(i18n.get("grid.context.clear"), null, !hasItems, ev -> gridViewModel.clearSlot(slotIndex))
        );
        return menu;
    }

    private javafx.scene.control.MenuItem createMenuItem(String text, String icon, boolean disabled, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(text);
        if (icon != null) item.setGraphic(TextureManager.getInstance().createIconNode(icon, 16));
        item.setDisable(disabled);
        item.setOnAction(action);
        return item;
    }

    private void setMaterial(int slotIndex, String chosen) {
        if (gridViewModel.getMultiSelectedSlots().contains(slotIndex) && gridViewModel.getMultiSelectedSlots().size() > 1) {
            gridViewModel.setMaterialForSelectedSlots(chosen);
        } else {
            gridViewModel.changeMaterialAt(slotIndex, chosen);
        }
    }

    private javafx.scene.control.Menu createQuickPaletteMenu(String title, int slotIndex) {
        javafx.scene.control.Menu menu = new javafx.scene.control.Menu(title);
        menu.setGraphic(TextureManager.getInstance().createIconNode("GRAY_STAINED_GLASS_PANE", 16));
        String[] quickMats = {
                "BLACK_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", "WHITE_STAINED_GLASS_PANE",
                "LIME_STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
                "STONE", "OAK_PLANKS", "CHEST", "DIAMOND", "GOLD_INGOT", "EMERALD",
                "NETHERITE_SWORD", "COMPASS", "CLOCK", "BOOK", "FEATHER", "BARRIER", "PLAYER_HEAD"
        };
        for (String qm : quickMats) {
            javafx.scene.control.MenuItem itm = new javafx.scene.control.MenuItem(qm);
            itm.setGraphic(TextureManager.getInstance().createIconNode(qm, 16));
            itm.setOnAction(ev -> setMaterial(slotIndex, qm));
            menu.getItems().add(itm);
        }
        return menu;
    }
}
