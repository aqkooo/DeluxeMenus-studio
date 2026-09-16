package com.ejyqyl.dmstudio.model.inventory;

import java.util.List;

/**
 * Supported inventory types in DeluxeMenus.
 */
public enum InventoryType {
    CHEST(27, true, 9, 3, List.of(9, 18, 27, 36, 45, 54)),
    HOPPER(5, false, 5, 1, List.of(5)),
    DROPPER(9, false, 3, 3, List.of(9)),
    DISPENSER(9, false, 3, 3, List.of(9)),
    FURNACE(3, false, 3, 1, List.of(3)),
    WORKBENCH(10, false, 3, 3, List.of(10)),
    ANVIL(3, false, 3, 1, List.of(3));

    private final int defaultSize;
    private final boolean allowsCustomSize;
    private final int columns;
    private final int defaultRows;
    private final List<Integer> validSizes;

    InventoryType(int defaultSize, boolean allowsCustomSize, int columns, int defaultRows, List<Integer> validSizes) {
        this.defaultSize = defaultSize;
        this.allowsCustomSize = allowsCustomSize;
        this.columns = columns;
        this.defaultRows = defaultRows;
        this.validSizes = validSizes;
    }

    public int getDefaultSize() {
        return defaultSize;
    }

    public boolean allowsCustomSize() {
        return allowsCustomSize;
    }

    public int getColumns() {
        return columns;
    }

    public int getDefaultRows() {
        return defaultRows;
    }

    public List<Integer> getValidSizes() {
        return validSizes;
    }

    public int getRowsForSize(int size) {
        if (!allowsCustomSize) {
            return defaultRows;
        }
        return Math.max(1, (size + columns - 1) / columns);
    }
}
