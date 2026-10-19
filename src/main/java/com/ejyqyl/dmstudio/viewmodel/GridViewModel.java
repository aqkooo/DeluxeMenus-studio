package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.command.CommandStack;
import com.ejyqyl.dmstudio.command.impl.BatchCommand;
import com.ejyqyl.dmstudio.command.impl.RemoveItemCommand;
import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.template.LayoutTemplate;
import com.ejyqyl.dmstudio.service.template.LayoutTemplateService;
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
        this.selectedSlot.addListener((obs, oldVal, newVal) -> refreshActiveItem());
        this.commandStack.addChangeListener(this::refreshActiveItem);
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

    public void selectAllSlots() {
        multiSelectedSlots.clear();
        for (int i = 0; i < config.getSize(); i++) {
            multiSelectedSlots.add(i);
        }
        if (config.getSize() > 0) {
            selectedSlot.set(0);
        }
    }

    public void selectRange(int fromSlot, int toSlot) {
        int min = Math.max(0, Math.min(fromSlot, toSlot));
        int max = Math.min(config.getSize() - 1, Math.max(fromSlot, toSlot));
        for (int i = min; i <= max; i++) {
            multiSelectedSlots.add(i);
        }
        selectedSlot.set(toSlot);
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

    public void refreshActiveItem() {
        int slot = selectedSlot.get();
        if (slot >= 0 && slot < config.getSize()) {
            List<MenuItem> itemsAtSlot = config.getItemsAtSlot(slot);
            activeItem.set(itemsAtSlot.isEmpty() ? null : itemsAtSlot.get(0));
        } else {
            activeItem.set(null);
        }
    }

    private String generateUniqueKey(String prefix) {
        String key = prefix;
        int counter = 1;
        while (config.getItems().containsKey(key)) {
            key = prefix + "_" + counter++;
        }
        return key;
    }

    public MenuItem createDefaultItemAt(int slot, String material) {
        MenuItem item = new MenuItem(generateUniqueKey("item_" + slot), material);
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

                MenuItem single = base.copy();
                single.setKey(generateUniqueKey("item_" + slot));
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
                MenuItem itm = new MenuItem(generateUniqueKey("item_" + slot), material.trim());
                itm.setSlot(slot);
                itm.setPriority(0);
                commands.add(new SetItemCommand(config, itm));
            } else {
                MenuItem base = items.get(0);
                if (base.getAssignedSlots().size() > 1 && !multiSelectedSlots.containsAll(base.getAssignedSlots())) {
                    MenuItem modifiedBase = base.copy();
                    modifiedBase.removeAssignedSlot(slot);
                    commands.add(new SetItemCommand(config, modifiedBase));

                    MenuItem single = base.copy();
                    single.setKey(generateUniqueKey("item_" + slot));
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

    public void setPriorityForSelectedSlots(int priority) {
        if (multiSelectedSlots.isEmpty()) return;
        List<Command> commands = new ArrayList<>();
        for (int slot : multiSelectedSlots) {
            for (MenuItem item : config.getItemsAtSlot(slot)) {
                MenuItem copy = item.copy();
                copy.setPriority(priority);
                commands.add(new SetItemCommand(config, copy));
            }
        }
        if (!commands.isEmpty()) {
            commandStack.execute(new BatchCommand("Set priority to " + priority, commands));
        }
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
            if (copy.removeAssignedSlot(slot)) {
                commands.add(copy.getAssignedSlots().isEmpty()
                        ? new RemoveItemCommand(config, item.getKey())
                        : new SetItemCommand(config, copy));
            }
        }
        if (!commands.isEmpty()) commandStack.execute(new BatchCommand("Clear slot " + slot, commands));
        selectSlot(slot);
    }

    public void clearSelectedSlots() {
        if (multiSelectedSlots.isEmpty()) return;
        List<Command> commands = new ArrayList<>();
        for (int slot : new ArrayList<>(multiSelectedSlots)) {
            for (MenuItem item : config.getItemsAtSlot(slot)) {
                MenuItem copy = item.copy();
                if (copy.removeAssignedSlot(slot)) {
                    commands.add(copy.getAssignedSlots().isEmpty()
                            ? new RemoveItemCommand(config, item.getKey())
                            : new SetItemCommand(config, copy));
                }
            }
        }
        if (!commands.isEmpty()) commandStack.execute(new BatchCommand("Clear selected slots", commands));
        selectSlot(-1);
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
        pasted.setKey("copy_" + pasted.getKey() + "_" + slot);
        pasted.setSlot(slot);
        pasted.getSlots().clear();
        commandStack.execute(new SetItemCommand(config, pasted));
        selectSlot(slot);
    }

    public void duplicateItemAt(int slot) {
        copyItemAt(slot);
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
        List<MenuItem> fromItems = config.getItemsAtSlot(fromSlot);
        if (fromItems.isEmpty()) return;

        MenuItem source = fromItems.get(0);
        List<Command> commands = new ArrayList<>();
        List<MenuItem> toItems = config.getItemsAtSlot(toSlot);

        if (!copyOnly && !toItems.isEmpty()) {
            MenuItem dest = toItems.get(0);
            MenuItem swappedDest = dest.copy();
            swappedDest.removeAssignedSlot(toSlot);
            swappedDest.setSlot(fromSlot);
            swappedDest.getSlots().clear();
            commands.add(new SetItemCommand(config, swappedDest));

            MenuItem swappedSource = source.copy();
            swappedSource.removeAssignedSlot(fromSlot);
            swappedSource.setSlot(toSlot);
            swappedSource.getSlots().clear();
            commands.add(new SetItemCommand(config, swappedSource));

            commandStack.execute(new BatchCommand("Swap items " + fromSlot + " <-> " + toSlot, commands));
            selectSlot(toSlot);
            return;
        }

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
            target.setKey(generateUniqueKey(source.getKey() + "_slot" + toSlot));
        }
        target.setSlot(toSlot);
        target.getSlots().clear();
        commands.add(new SetItemCommand(config, target));

        commandStack.execute(new BatchCommand("Move item " + fromSlot + " -> " + toSlot, commands));
        selectSlot(toSlot);
    }

    public void setItemAt(int slot, MenuItem itemTemplate) {
        if (itemTemplate == null) return;
        MenuItem newItem = itemTemplate.copy();
        newItem.setKey(generateUniqueKey("item_" + slot));
        newItem.setSlot(slot);
        newItem.getSlots().clear();
        commandStack.execute(new SetItemCommand(config, newItem));
        selectSlot(slot);
    }

    public void clampSelectionToBounds() {
        if (selectedSlot.get() >= config.getSize()) {
            selectSlot(-1);
        }
        multiSelectedSlots.removeIf(s -> s >= config.getSize());
    }

    public void applyPresetBorder(String material) {
        Optional<LayoutTemplate> tmpl = LayoutTemplateService.getInstance().getAllTemplates().stream()
                .filter(t -> "border_perimeter".equals(t.id()))
                .findFirst();
        tmpl.ifPresent(t -> commandStack.execute(LayoutTemplateService.getInstance().applyTemplate(config, t)));
        selectSlot(0);
    }

    public void applyPresetCheckerboard(String mat1, String mat2) {
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int rows = Math.max(1, (size + cols - 1) / cols);

        MenuItem a = new MenuItem("checker_a", mat1 != null ? mat1 : "BLACK_STAINED_GLASS_PANE");
        a.getAppearance().setDisplayName(" ");
        a.setPriority(10);
        MenuItem b = new MenuItem("checker_b", mat2 != null ? mat2 : "GRAY_STAINED_GLASS_PANE");
        b.getAppearance().setDisplayName(" ");
        b.setPriority(10);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slot = r * cols + c;
                if (slot < size) ((r + c) % 2 == 0 ? a : b).getSlots().add(String.valueOf(slot));
            }
        }
        commandStack.execute(new BatchCommand("Apply checkerboard preset", List.of(new SetItemCommand(config, a), new SetItemCommand(config, b))));
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

    public IntegerProperty selectedSlotProperty() { return selectedSlot; }
    public int getSelectedSlot() { return selectedSlot.get(); }
    public ObservableSet<Integer> getMultiSelectedSlots() { return multiSelectedSlots; }
    public ObjectProperty<MenuItem> activeItemProperty() { return activeItem; }
    public MenuItem getActiveItem() { return activeItem.get(); }
    public MenuConfig getConfig() { return config; }
    public CommandStack getCommandStack() { return commandStack; }
}
