package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.viewmodel.GridViewModel;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Handles keyboard navigation, editing shortcuts, and clipboard operations on the inventory grid.
 */
public class GridKeyHandler {

    private final MainViewModel mainViewModel;
    private final GridViewModel gridViewModel;

    public GridKeyHandler(MainViewModel mainViewModel, GridViewModel gridViewModel) {
        this.mainViewModel = mainViewModel;
        this.gridViewModel = gridViewModel;
    }

    public void handleKeyPressed(KeyEvent e) {
        if (mainViewModel == null || gridViewModel == null || mainViewModel.getConfig() == null) return;

        MenuConfig config = mainViewModel.getConfig();
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int currentSlot = gridViewModel.getSelectedSlot();

        boolean isCtrlOrCmd = e.isControlDown() || e.isShortcutDown();

        // Undo / Redo
        if (isCtrlOrCmd && e.getCode() == KeyCode.Z) {
            if (e.isShiftDown()) {
                mainViewModel.getCommandStack().redo();
            } else {
                mainViewModel.getCommandStack().undo();
            }
            e.consume();
            return;
        }
        if (isCtrlOrCmd && e.getCode() == KeyCode.Y) {
            mainViewModel.getCommandStack().redo();
            e.consume();
            return;
        }

        // Select All (Ctrl+A)
        if (isCtrlOrCmd && e.getCode() == KeyCode.A) {
            gridViewModel.selectAllSlots();
            e.consume();
            return;
        }

        // Copy / Paste / Duplicate
        if (isCtrlOrCmd && e.getCode() == KeyCode.C && currentSlot >= 0) {
            gridViewModel.copyItemAt(currentSlot);
            e.consume();
            return;
        }
        if (isCtrlOrCmd && e.getCode() == KeyCode.V && currentSlot >= 0) {
            gridViewModel.pasteItemAt(currentSlot);
            e.consume();
            return;
        }
        if (isCtrlOrCmd && e.getCode() == KeyCode.D && currentSlot >= 0) {
            gridViewModel.duplicateItemAt(currentSlot);
            e.consume();
            return;
        }

        // Delete / Clear
        if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
            if (!gridViewModel.getMultiSelectedSlots().isEmpty()) {
                gridViewModel.clearSelectedSlots();
            } else if (currentSlot >= 0) {
                gridViewModel.clearSlot(currentSlot);
            }
            e.consume();
            return;
        }

        // Escape: clear selection
        if (e.getCode() == KeyCode.ESCAPE) {
            gridViewModel.selectSlot(-1);
            e.consume();
            return;
        }

        // Arrow navigation
        int nextSlot = -1;
        if (e.getCode() == KeyCode.LEFT && currentSlot > 0) {
            nextSlot = currentSlot - 1;
        } else if (e.getCode() == KeyCode.RIGHT && currentSlot >= 0 && currentSlot < size - 1) {
            nextSlot = currentSlot + 1;
        } else if (e.getCode() == KeyCode.UP && currentSlot >= cols) {
            nextSlot = currentSlot - cols;
        } else if (e.getCode() == KeyCode.DOWN && currentSlot >= 0 && currentSlot + cols < size) {
            nextSlot = currentSlot + cols;
        }

        if (nextSlot >= 0) {
            if (e.isShiftDown()) {
                gridViewModel.selectRange(currentSlot, nextSlot);
            } else {
                gridViewModel.selectSlot(nextSlot);
            }
            e.consume();
        }
    }
}
