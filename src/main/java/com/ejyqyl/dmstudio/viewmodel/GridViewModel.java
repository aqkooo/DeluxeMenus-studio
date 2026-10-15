package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.command.CommandStack;
import com.ejyqyl.dmstudio.command.impl.BatchCommand;
import com.ejyqyl.dmstudio.command.impl.RemoveItemCommand;
import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.*;

/**
 * ViewModel managing the inventory grid state, selection, drag-and-drop, and batch layout presets.
 */
public class GridViewModel {

    private final MenuConfig config;
    private final CommandStack commandStack;

    private final IntegerProperty selectedSlot = new SimpleIntegerProperty(-1);
    private final ObservableSet<Integer> multiSelectedSlots = FXCollections.observableSet(new HashSet<>());
    private final ObjectProperty<MenuItem> activeItem = new SimpleObjectProperty<>(null);

    private MenuItem clipboardItem = null;

    public GridViewModel(MenuConfig config, CommandStack commandStack) {
        this.config = config;
        this.commandStack = commandStack;
        this.selectedSlot.addListener((obs, oldVal, newVal) -> {
            int slot = newVal.intValue();
            if (slot >= 0 && slot < config.getSize()) {
                List<MenuItem> itemsAtSlot = config.getItemsAtSlot(slot);
                activeItem.set(itemsAtSlot.isEmpty() ? null : itemsAtSlot.get(0));
            } else {
                activeItem.set(null);
            }
        });
    }

    public void selectSlot(int slot) {
        multiSelectedSlots.clear();
        if (slot >= 0 && slot < config.getSize()) {
            multiSelectedSlots.add(slot);
            selectedSlot.set(slot);
        } else {
            selectedSlot.set(-1);
        }
    }

    public void toggleMultiSelectSlot(int slot) {
        if (multiSelectedSlots.contains(slot)) {
            multiSelectedSlots.remove(slot);
            if (selectedSlot.get() == slot) {
                selectedSlot.set(multiSelectedSlots.isEmpty() ? -1 : multiSelectedSlots.iterator().next());
            }
        } else {
            multiSelectedSlots.add(slot);
            selectedSlot.set(slot);
        }
    }

    public MenuItem createDefaultItemAt(int slot, String material) {
        String key = "item_" + slot;
        int counter = 1;
        while (config.getItems().containsKey(key)) {
            key = "item_" + slot + "_" + counter++;
        }
        MenuItem item = new MenuItem(key, material);
        item.setSlot(slot);
        item.setPriority(0);
        commandStack.execute(new SetItemCommand(config, item));
        selectSlot(slot);
        return item;
    }

    public void changeMaterialAt(int slot, String material) {
        if (material == null || material.isBlank()) return;
        List<MenuItem> items = config.getItemsAtSlot(slot);
        if (items.isEmpty()) {
            createDefaultItemAt(slot, material.trim());
        } else {
            MenuItem base = items.get(0);
            if (base.getAssignedSlots().size() > 1) {
                List<Command> commands = new ArrayList<>();
                MenuItem modifiedBase = base.copy();
                modifiedBase.removeAssignedSlot(slot);
                commands.add(new SetItemCommand(config, modifiedBase));

                String key = "item_" + slot;
                int counter = 1;
                while (config.getItems().containsKey(key)) {
                    key = "item_" + slot + "_" + counter++;
                }
                MenuItem single = base.copy();
                single.setKey(key);
                single.setMaterial(material.trim());
                single.setSlot(slot);
                single.getSlots().clear();
                commands.add(new SetItemCommand(config, single));
                commandStack.execute(new BatchCommand("Change material at " + slot, commands));
            } else {
                MenuItem updated = base.copy();
                updated.setMaterial(material.trim());
                commandStack.execute(new SetItemCommand(config, updated));
            }
            selectSlot(slot);
        }
    }

    public void setMaterialForSelectedSlots(String material) {
        if (material == null || material.isBlank() || multiSelectedSlots.isEmpty()) return;
        List<Command> commands = new ArrayList<>();
        for (int slot : multiSelectedSlots) {
            List<MenuItem> items = config.getItemsAtSlot(slot);
            if (items.isEmpty()) {
                String key = "item_" + slot;
                int counter = 1;
                while (config.getItems().containsKey(key)) {
                    key = "item_" + slot + "_" + counter++;
                }
                MenuItem itm = new MenuItem(key, material.trim());
                itm.setSlot(slot);
                itm.setPriority(0);
                commands.add(new SetItemCommand(config, itm));
            } else {
                MenuItem base = items.get(0);
                if (base.getAssignedSlots().size() > 1 && !multiSelectedSlots.containsAll(base.getAssignedSlots())) {
                    MenuItem modifiedBase = base.copy();
                    modifiedBase.removeAssignedSlot(slot);
                    commands.add(new SetItemCommand(config, modifiedBase));

                    String key = "item_" + slot;
                    int counter = 1;
                    while (config.getItems().containsKey(key)) {
                        key = "item_" + slot + "_" + counter++;
                    }
                    MenuItem single = base.copy();
                    single.setKey(key);
                    single.setMaterial(material.trim());
                    single.setSlot(slot);
                    single.getSlots().clear();
                    commands.add(new SetItemCommand(config, single));
                } else {
                    MenuItem updated = base.copy();
                    updated.setMaterial(material.trim());
                    commands.add(new SetItemCommand(config, updated));
                }
            }
        }
        commandStack.execute(new BatchCommand("Change material to " + material, commands));
    }

    public boolean hasClipboardItem() {
        return clipboardItem != null;
    }

    public void clearSlot(int slot) {
        List<MenuItem> items = config.getItemsAtSlot(slot);
        if (items.isEmpty()) return;

        List<Command> commands = new ArrayList<>();
        for (MenuItem item : items) {
            MenuItem copy = item.copy();
            boolean removed = copy.removeAssignedSlot(slot);
            if (!removed) continue;

            if (copy.getAssignedSlots().isEmpty()) {
                commands.add(new RemoveItemCommand(config, item.getKey()));
            } else {
                commands.add(new SetItemCommand(config, copy));
            }
        }
        if (!commands.isEmpty()) {
            commandStack.execute(new BatchCommand("Clear slot " + slot, commands));
        }
        selectSlot(slot);
    }

    public void copyItemAt(int slot) {
        List<MenuItem> items = config.getItemsAtSlot(slot);
        if (!items.isEmpty()) {
            clipboardItem = items.get(0).copy();
        }
    }

    public void pasteItemAt(int slot) {
        if (clipboardItem == null) return;
        MenuItem pasted = clipboardItem.copy();
        String newKey = "copy_" + pasted.getKey() + "_" + slot;
        pasted.setKey(newKey);
        pasted.setSlot(slot);
        pasted.getSlots().clear();
        commandStack.execute(new SetItemCommand(config, pasted));
        selectSlot(slot);
    }

    public void duplicateItemAt(int slot) {
        copyItemAt(slot);
        // Find next empty slot
        for (int i = slot + 1; i < config.getSize(); i++) {
            if (config.getItemsAtSlot(i).isEmpty()) {
                pasteItemAt(i);
                return;
            }
        }
    }

    public void createLowerPriorityVariant(int slot) {
        List<MenuItem> items = config.getItemsAtSlot(slot);
        if (items.isEmpty()) return;
        MenuItem base = items.get(0);
        int nextPriority = base.getPriority() + 1;
        for (MenuItem it : items) {
            nextPriority = Math.max(nextPriority, it.getPriority() + 1);
        }

        MenuItem variant = base.copy();
        variant.setKey(base.getKey() + "_alt" + nextPriority);
        variant.setPriority(nextPriority);
        commandStack.execute(new SetItemCommand(config, variant));
        selectSlot(slot);
    }

    public void moveItem(int fromSlot, int toSlot, boolean copyOnly) {
        if (fromSlot == toSlot && !copyOnly) return;
        List<MenuItem> items = config.getItemsAtSlot(fromSlot);
        if (items.isEmpty()) return;

        MenuItem source = items.get(0);
        List<Command> commands = new ArrayList<>();

        if (!copyOnly) {
            MenuItem modifiedSource = source.copy();
            modifiedSource.removeAssignedSlot(fromSlot);
            if (modifiedSource.getAssignedSlots().isEmpty()) {
                commands.add(new RemoveItemCommand(config, source.getKey()));
            } else {
                commands.add(new SetItemCommand(config, modifiedSource));
            }
        }

        MenuItem target = source.copy();
        if (copyOnly || source.getAssignedSlots().size() > 1) {
            String newKey = source.getKey() + "_slot" + toSlot;
            int counter = 1;
            while (config.getItems().containsKey(newKey)) {
                newKey = source.getKey() + "_slot" + toSlot + "_" + counter++;
            }
            target.setKey(newKey);
        }
        target.setSlot(toSlot);
        target.getSlots().clear();
        commands.add(new SetItemCommand(config, target));

        commandStack.execute(new BatchCommand("Move item " + fromSlot + " -> " + toSlot, commands));
        selectSlot(toSlot);
    }

    public void clampSelectionToBounds() {
        if (selectedSlot.get() >= config.getSize()) {
            selectSlot(-1);
        }
        multiSelectedSlots.removeIf(s -> s >= config.getSize());
    }

    // Presets (§6.2)
    public void applyPresetBorder(String material) {
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int rows = size / cols;

        List<Command> commands = new ArrayList<>();
        MenuItem borderItem = new MenuItem("border", material != null ? material : "GRAY_STAINED_GLASS_PANE");
        borderItem.getAppearance().setDisplayName(" ");
        borderItem.setPriority(10);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    int slot = r * cols + c;
                    borderItem.getSlots().add(String.valueOf(slot));
                }
            }
        }
        commands.add(new SetItemCommand(config, borderItem));
        commandStack.execute(new BatchCommand("Apply border preset", commands));
        selectSlot(0);
    }

    public void applyPresetCheckerboard(String mat1, String mat2) {
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int rows = size / cols;

        List<Command> commands = new ArrayList<>();
        MenuItem item1 = new MenuItem("checker_a", mat1 != null ? mat1 : "BLACK_STAINED_GLASS_PANE");
        item1.getAppearance().setDisplayName(" ");
        item1.setPriority(10);

        MenuItem item2 = new MenuItem("checker_b", mat2 != null ? mat2 : "GRAY_STAINED_GLASS_PANE");
        item2.getAppearance().setDisplayName(" ");
        item2.setPriority(10);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slot = r * cols + c;
                if ((r + c) % 2 == 0) {
                    item1.getSlots().add(String.valueOf(slot));
                } else {
                    item2.getSlots().add(String.valueOf(slot));
                }
            }
        }
        commands.add(new SetItemCommand(config, item1));
        commands.add(new SetItemCommand(config, item2));
        commandStack.execute(new BatchCommand("Apply checkerboard preset", commands));
        selectSlot(0);
    }

    public void applyPresetClearAll() {
        List<Command> commands = new ArrayList<>();
        for (String key : new ArrayList<>(config.getItems().keySet())) {
            commands.add(new RemoveItemCommand(config, key));
        }
        commandStack.execute(new BatchCommand("Clear all items", commands));
        selectSlot(-1);
    }

    public void setItemAt(int slot, MenuItem itemTemplate) {
        if (itemTemplate == null) return;
        String key = "item_" + slot;
        int counter = 1;
        while (config.getItems().containsKey(key)) {
            key = "item_" + slot + "_" + counter++;
        }
        MenuItem newItem = itemTemplate.copy();
        newItem.setKey(key);
        newItem.setSlot(slot);
        newItem.getSlots().clear();
        commandStack.execute(new SetItemCommand(config, newItem));
        selectSlot(slot);
    }

    public IntegerProperty selectedSlotProperty() { return selectedSlot; }
    public int getSelectedSlot() { return selectedSlot.get(); }

    public ObservableSet<Integer> getMultiSelectedSlots() { return multiSelectedSlots; }

    public ObjectProperty<MenuItem> activeItemProperty() { return activeItem; }
    public MenuItem getActiveItem() { return activeItem.get(); }

    public MenuConfig getConfig() { return config; }
    public CommandStack getCommandStack() { return commandStack; }
}
