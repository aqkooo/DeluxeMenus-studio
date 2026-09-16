package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.render.ItemIconProvider;
import com.ejyqyl.dmstudio.viewmodel.GridViewModel;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Controller for visual inventory grid with drag-and-drop, slot selection, and layout presets (§6.2).
 */
public class GridController {

    private static final DataFormat SLOT_FORMAT = new DataFormat("application/x-deluxemenus-slot");

    @FXML private GridPane inventoryGrid;
    @FXML private Button btnBorder;
    @FXML private Button btnChecker;
    @FXML private Button btnClearAll;

    private MainViewModel mainViewModel;
    private GridViewModel gridViewModel;

    @FXML
    public void initialize() {
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
        inventoryGrid.getChildren().clear();

        MenuConfig config = mainViewModel.getConfig();
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int rows = Math.max(1, (size + cols - 1) / cols);

        for (int i = 0; i < size; i++) {
            int row = i / cols;
            int col = i % cols;
            Node slotNode = createSlotNode(i, config);
            inventoryGrid.add(slotNode, col, row);
        }
    }

    private Node createSlotNode(int slotIndex, MenuConfig config) {
        StackPane slotPane = new StackPane();
        slotPane.setPrefSize(48, 48);
        slotPane.setMinSize(48, 48);
        slotPane.setMaxSize(48, 48);
        slotPane.getStyleClass().add("inventory-slot");

        boolean isSelected = gridViewModel.getMultiSelectedSlots().contains(slotIndex);
        if (isSelected) {
            slotPane.getStyleClass().add("inventory-slot-selected");
        }

        List<MenuItem> items = config.getItemsAtSlot(slotIndex);
        if (items.size() > 1) {
            // Check collision (same priority)
            int p0 = items.get(0).getPriority();
            boolean collision = false;
            for (int i = 1; i < items.size(); i++) {
                if (items.get(i).getPriority() == p0) {
                    collision = true;
                    break;
                }
            }
            if (collision) {
                slotPane.getStyleClass().add("inventory-slot-collision");
            }
        }

        // Slot number label (top-left)
        Label slotLabel = new Label(String.valueOf(slotIndex));
        slotLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #b0b0b0; -fx-padding: 2 0 0 2;");
        StackPane.setAlignment(slotLabel, Pos.TOP_LEFT);
        slotPane.getChildren().add(slotLabel);

        if (!items.isEmpty()) {
            MenuItem primaryItem = items.get(0);
            Node icon = ItemIconProvider.createIcon(primaryItem.getMaterial(), 36);
            slotPane.getChildren().add(icon);

            // Amount badge (bottom-right)
            if (primaryItem.getAmount() > 1) {
                Label amountLabel = new Label(String.valueOf(primaryItem.getAmount()));
                amountLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, black, 2, 0, 1, 1);");
                StackPane.setAlignment(amountLabel, Pos.BOTTOM_RIGHT);
                slotPane.getChildren().add(amountLabel);
            }

            // Priority badge if multiple items on slot: [P:0]
            if (items.size() > 1) {
                Label prioBadge = new Label("P:" + primaryItem.getPriority());
                prioBadge.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #ffff55; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 1 2 1 2; -fx-background-radius: 2;");
                StackPane.setAlignment(prioBadge, Pos.BOTTOM_LEFT);
                slotPane.getChildren().add(prioBadge);
            }

            // Tooltip
            String title = primaryItem.getAppearance().getDisplayName() != null ? primaryItem.getAppearance().getDisplayName() : primaryItem.getKey();
            Tooltip.install(slotPane, new Tooltip(I18nService.getInstance().get("grid.slot_tooltip", slotIndex, title, primaryItem.getPriority())));
        }

        setupInteractivity(slotPane, slotIndex, items);
        return slotPane;
    }

    private void setupInteractivity(StackPane slotPane, int slotIndex, List<MenuItem> items) {
        // Mouse click
        slotPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 2 && items.isEmpty()) {
                    gridViewModel.createDefaultItemAt(slotIndex, "STONE");
                } else if (e.isShiftDown()) {
                    gridViewModel.toggleMultiSelectSlot(slotIndex);
                } else {
                    gridViewModel.selectSlot(slotIndex);
                }
            }
        });

        // Context menu (RMB)
        ContextMenu menu = new ContextMenu();
        MenuItem editMi = new MenuItem("grid.context.edit", "Edit");
        MenuItem clearMi = new MenuItem("grid.context.clear", "Clear Slot");
        MenuItem dupMi = new MenuItem("grid.context.duplicate", "Duplicate");
        MenuItem copyMi = new MenuItem("grid.context.copy", "Copy");
        MenuItem pasteMi = new MenuItem("grid.context.paste", "Paste");
        MenuItem lowerPrioMi = new MenuItem("grid.context.lower_priority", "Add Lower Priority Variant");

        javafx.scene.control.MenuItem itmClear = new javafx.scene.control.MenuItem(I18nService.getInstance().get("grid.context.clear"));
        itmClear.setOnAction(e -> gridViewModel.clearSlot(slotIndex));

        javafx.scene.control.MenuItem itmDup = new javafx.scene.control.MenuItem(I18nService.getInstance().get("grid.context.duplicate"));
        itmDup.setOnAction(e -> gridViewModel.duplicateItemAt(slotIndex));

        javafx.scene.control.MenuItem itmCopy = new javafx.scene.control.MenuItem(I18nService.getInstance().get("grid.context.copy"));
        itmCopy.setOnAction(e -> gridViewModel.copyItemAt(slotIndex));

        javafx.scene.control.MenuItem itmPaste = new javafx.scene.control.MenuItem(I18nService.getInstance().get("grid.context.paste"));
        itmPaste.setOnAction(e -> gridViewModel.pasteItemAt(slotIndex));

        javafx.scene.control.MenuItem itmLowerPrio = new javafx.scene.control.MenuItem(I18nService.getInstance().get("grid.context.lower_priority"));
        itmLowerPrio.setOnAction(e -> gridViewModel.createLowerPriorityVariant(slotIndex));

        menu.getItems().addAll(itmClear, itmDup, itmCopy, itmPaste, new SeparatorMenuItem(), itmLowerPrio);
        slotPane.setOnContextMenuRequested(e -> menu.show(slotPane, e.getScreenX(), e.getScreenY()));

        // Drag and Drop
        slotPane.setOnDragDetected(e -> {
            if (!items.isEmpty()) {
                Dragboard db = slotPane.startDragAndDrop(e.isControlDown() ? TransferMode.COPY : TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(slotIndex));
                db.setContent(content);
                e.consume();
            }
        });

        slotPane.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });

        slotPane.setOnDragDropped(e -> {
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
}
