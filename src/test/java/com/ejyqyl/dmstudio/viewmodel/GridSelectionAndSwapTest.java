package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.command.CommandStack;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GridSelectionAndSwapTest {

    private MenuConfig config;
    private CommandStack commandStack;
    private GridViewModel gridVm;

    @BeforeEach
    void setUp() {
        config = new MenuConfig();
        config.setSize(27);
        commandStack = new CommandStack();
        gridVm = new GridViewModel(config, commandStack);
    }

    @Test
    void testSelectAllAndRangeSelection() {
        gridVm.selectAllSlots();
        assertThat(gridVm.getMultiSelectedSlots()).hasSize(27);

        gridVm.selectSlot(0);
        assertThat(gridVm.getMultiSelectedSlots()).containsExactly(0);

        gridVm.selectRange(0, 8);
        assertThat(gridVm.getMultiSelectedSlots()).contains(0, 1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(gridVm.getSelectedSlot()).isEqualTo(8);
    }

    @Test
    void testClearSelectedSlotsAndUndo() {
        gridVm.createDefaultItemAt(0, "STONE");
        gridVm.createDefaultItemAt(1, "DIRT");

        gridVm.selectRange(0, 1);
        gridVm.clearSelectedSlots();

        assertThat(config.getItemsAtSlot(0)).isEmpty();
        assertThat(config.getItemsAtSlot(1)).isEmpty();

        // Undo
        commandStack.undo();
        assertThat(config.getItemsAtSlot(0)).isNotEmpty();
        assertThat(config.getItemsAtSlot(1)).isNotEmpty();
    }

    @Test
    void testSwapItemsOnDragAndDrop() {
        MenuItem diamond = gridVm.createDefaultItemAt(0, "DIAMOND");
        MenuItem emerald = gridVm.createDefaultItemAt(1, "EMERALD");

        // Drag slot 0 onto occupied slot 1 -> should swap!
        gridVm.moveItem(0, 1, false);

        assertThat(config.getItemsAtSlot(0).get(0).getMaterial()).isEqualTo("EMERALD");
        assertThat(config.getItemsAtSlot(1).get(0).getMaterial()).isEqualTo("DIAMOND");

        // Undo swap
        commandStack.undo();
        assertThat(config.getItemsAtSlot(0).get(0).getMaterial()).isEqualTo("DIAMOND");
        assertThat(config.getItemsAtSlot(1).get(0).getMaterial()).isEqualTo("EMERALD");
    }

    @Test
    void testSetPriorityForSelectedSlots() {
        gridVm.createDefaultItemAt(2, "GOLD_INGOT");
        gridVm.createDefaultItemAt(3, "IRON_INGOT");

        gridVm.selectRange(2, 3);
        gridVm.setPriorityForSelectedSlots(5);

        assertThat(config.getItemsAtSlot(2).get(0).getPriority()).isEqualTo(5);
        assertThat(config.getItemsAtSlot(3).get(0).getPriority()).isEqualTo(5);

        // Undo
        commandStack.undo();
        assertThat(config.getItemsAtSlot(2).get(0).getPriority()).isEqualTo(0);
        assertThat(config.getItemsAtSlot(3).get(0).getPriority()).isEqualTo(0);
    }
}
