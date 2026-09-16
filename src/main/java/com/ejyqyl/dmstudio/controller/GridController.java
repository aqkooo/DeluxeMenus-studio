package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.view.SlotCell;
import com.ejyqyl.dmstudio.viewmodel.GridViewModel;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
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
    private final List<SlotCell> slotCells = new ArrayList<>();

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
    }

    private void setupInteractivity(SlotCell slotCell, int slotIndex) {
        // Mouse click
        slotCell.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                List<MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(slotIndex);
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
        slotCell.setOnContextMenuRequested(e -> menu.show(slotCell, e.getScreenX(), e.getScreenY()));

        // Drag and Drop
        slotCell.setOnDragDetected(e -> {
            List<MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(slotIndex);
            if (!items.isEmpty()) {
                Dragboard db = slotCell.startDragAndDrop(e.isControlDown() ? TransferMode.COPY : TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(slotIndex));
                db.setContent(content);
                e.consume();
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
}
