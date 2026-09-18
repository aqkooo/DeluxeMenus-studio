package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.command.CommandStack;
import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;

/**
 * ViewModel for the Item Inspector panel with two-way data bindings and undo support.
 */
public class ItemEditorViewModel {

    private final MenuConfig config;
    private final CommandStack commandStack;
    private final PreviewViewModel previewViewModel;

    private MenuItem currentItem;
    private boolean updating = false;

    // Identification
    private final StringProperty key = new SimpleStringProperty("");
    private final StringProperty material = new SimpleStringProperty("");
    private final IntegerProperty slot = new SimpleIntegerProperty(0);
    private final StringProperty slotsString = new SimpleStringProperty("");
    private final IntegerProperty priority = new SimpleIntegerProperty(0);
    private final IntegerProperty amount = new SimpleIntegerProperty(1);
    private final StringProperty dynamicAmount = new SimpleStringProperty("");
    private final IntegerProperty damage = new SimpleIntegerProperty(0);
    private final BooleanProperty update = new SimpleBooleanProperty(false);

    // Appearance
    private final StringProperty displayName = new SimpleStringProperty("");
    private final StringProperty loreText = new SimpleStringProperty("");
    private final StringProperty loreAppendMode = new SimpleStringProperty("OVERRIDE");
    private final BooleanProperty unbreakable = new SimpleBooleanProperty(false);
    private final BooleanProperty glintOverride = new SimpleBooleanProperty(false);
    private final BooleanProperty hideTooltip = new SimpleBooleanProperty(false);
    private final IntegerProperty modelData = new SimpleIntegerProperty(0);
    private final StringProperty itemModel = new SimpleStringProperty("");
    private final StringProperty rgb = new SimpleStringProperty("");

    // Flags & Enchants
    private final ObservableList<String> selectedFlags = FXCollections.observableArrayList();
    private final ObservableList<String> enchantments = FXCollections.observableArrayList();

    // Click commands
    private final StringProperty currentClickType = new SimpleStringProperty("left_click");
    private final ObservableList<Action> currentActions = FXCollections.observableArrayList();

    public ItemEditorViewModel(MenuConfig config, CommandStack commandStack, PreviewViewModel previewViewModel) {
        this.config = config;
        this.commandStack = commandStack;
        this.previewViewModel = previewViewModel;
    }

    public void bindItem(MenuItem item) {
        this.currentItem = item;
        this.updating = true;
        try {
            if (item == null) {
                key.set("");
                material.set("");
                slot.set(0);
                slotsString.set("");
                priority.set(0);
                amount.set(1);
                dynamicAmount.set("");
                damage.set(0);
                update.set(false);
                displayName.set("");
                loreText.set("");
                loreAppendMode.set("OVERRIDE");
                unbreakable.set(false);
                glintOverride.set(false);
                hideTooltip.set(false);
                modelData.set(0);
                itemModel.set("");
                rgb.set("");
                selectedFlags.clear();
                enchantments.clear();
                currentActions.clear();
                return;
            }

            key.set(item.getKey());
            material.set(item.getMaterial());
            slot.set(item.getSlot() != null ? item.getSlot() : 0);
            slotsString.set(String.join(", ", item.getSlots()));
            priority.set(item.getPriority());
            amount.set(item.getAmount());
            dynamicAmount.set(item.getDynamicAmount() != null ? item.getDynamicAmount() : "");
            damage.set(item.getDamage() != null ? item.getDamage() : 0);
            update.set(item.isUpdate());

            ItemAppearance app = item.getAppearance();
            displayName.set(app.getDisplayName() != null ? app.getDisplayName() : "");
            loreText.set(String.join("\n", app.getLore()));
            loreAppendMode.set(app.getLoreAppendMode() != null ? app.getLoreAppendMode() : "OVERRIDE");
            unbreakable.set(Boolean.TRUE.equals(app.getUnbreakable()));
            glintOverride.set(Boolean.TRUE.equals(app.getEnchantmentGlintOverride()));
            hideTooltip.set(Boolean.TRUE.equals(app.getHideTooltip()));
            modelData.set(app.getModelData() != null ? app.getModelData() : 0);
            itemModel.set(app.getItemModel() != null ? app.getItemModel() : "");
            rgb.set(app.getRgb() != null ? app.getRgb() : "");

            selectedFlags.setAll(app.getItemFlags());
            enchantments.setAll(app.getEnchantments());

            loadCurrentClickActions();
        } finally {
            this.updating = false;
        }
    }

    public void commitChanges() {
        if (updating) return;

        String oldKey = currentItem != null ? currentItem.getKey() : null;

        if (currentItem == null) {
            String k = key.get() == null ? "" : key.get().trim();
            String mat = material.get() == null ? "" : material.get().trim();
            if (k.isBlank() && mat.isBlank()) {
                return;
            }
            if (k.isBlank()) {
                k = "item_" + slot.get();
                key.set(k);
            }
            if (mat.isBlank()) {
                mat = "STONE";
                material.set(mat);
            }
            currentItem = new MenuItem(k, mat);
            currentItem.setSlot(slot.get());
        }

        MenuItem item = currentItem.copy();
        item.setKey(key.get().trim());
        item.setMaterial(material.get().trim());
        item.setSlot(slot.get());
        item.getSlots().clear();
        String slotsRaw = slotsString.get().trim();
        if (!slotsRaw.isBlank()) {
            String[] parts = slotsRaw.split("[,;\\s]+");
            for (String p : parts) {
                if (!p.isBlank()) item.getSlots().add(p.trim());
            }
        }
        item.setPriority(priority.get());
        item.setAmount(amount.get());
        item.setDynamicAmount(dynamicAmount.get().isBlank() ? null : dynamicAmount.get());
        item.setDamage(damage.get() > 0 ? damage.get() : null);
        item.setUpdate(update.get());

        ItemAppearance app = item.getAppearance();
        app.setDisplayName(displayName.get().isEmpty() ? null : displayName.get());
        app.getLore().clear();
        if (!loreText.get().isBlank()) {
            String[] lines = loreText.get().split("\n", -1);
            app.getLore().addAll(Arrays.asList(lines));
        }
        app.setLoreAppendMode(loreAppendMode.get().equals("OVERRIDE") ? null : loreAppendMode.get());
        app.getItemFlags().clear();
        app.getItemFlags().addAll(selectedFlags);
        app.getEnchantments().clear();
        app.getEnchantments().addAll(enchantments);
        app.setUnbreakable(unbreakable.get() ? true : null);
        app.setEnchantmentGlintOverride(glintOverride.get() ? true : null);
        app.setHideTooltip(hideTooltip.get() ? true : null);
        app.setModelData(modelData.get() > 0 ? modelData.get() : null);
        app.setItemModel(itemModel.get().isBlank() ? null : itemModel.get());
        app.setRgb(rgb.get().isBlank() ? null : rgb.get());

        saveCurrentClickActions(item);

        commandStack.execute(new SetItemCommand(config, oldKey, item));
        this.currentItem = item;
        previewViewModel.requestUpdate();
    }

    public void loadCurrentClickActions() {
        if (currentItem == null) {
            currentActions.clear();
            return;
        }
        ClickHandler ch = currentItem.getClickHandler();
        List<Action> list = switch (currentClickType.get()) {
            case "click" -> ch.getClickCommands();
            case "right_click" -> ch.getRightClickCommands();
            case "shift_left_click" -> ch.getShiftLeftClickCommands();
            case "shift_right_click" -> ch.getShiftRightClickCommands();
            case "middle_click" -> ch.getMiddleClickCommands();
            default -> ch.getLeftClickCommands();
        };
        currentActions.setAll(list);
    }

    public void saveCurrentClickActions(MenuItem target) {
        ClickHandler ch = target.getClickHandler();
        List<Action> list = switch (currentClickType.get()) {
            case "click" -> ch.getClickCommands();
            case "right_click" -> ch.getRightClickCommands();
            case "shift_left_click" -> ch.getShiftLeftClickCommands();
            case "shift_right_click" -> ch.getShiftRightClickCommands();
            case "middle_click" -> ch.getMiddleClickCommands();
            default -> ch.getLeftClickCommands();
        };
        list.clear();
        for (Action a : currentActions) {
            list.add(a.copy());
        }
    }

    public void addAction(Action action) {
        currentActions.add(action);
        commitChanges();
    }

    public void removeAction(int index) {
        if (index >= 0 && index < currentActions.size()) {
            currentActions.remove(index);
            commitChanges();
        }
    }

    public MenuItem getCurrentItem() { return currentItem; }
    public StringProperty keyProperty() { return key; }
    public StringProperty materialProperty() { return material; }
    public IntegerProperty slotProperty() { return slot; }
    public StringProperty slotsStringProperty() { return slotsString; }
    public IntegerProperty priorityProperty() { return priority; }
    public IntegerProperty amountProperty() { return amount; }
    public StringProperty dynamicAmountProperty() { return dynamicAmount; }
    public IntegerProperty damageProperty() { return damage; }
    public BooleanProperty updateProperty() { return update; }
    public StringProperty displayNameProperty() { return displayName; }
    public StringProperty loreTextProperty() { return loreText; }
    public StringProperty loreAppendModeProperty() { return loreAppendMode; }
    public BooleanProperty unbreakableProperty() { return unbreakable; }
    public BooleanProperty glintOverrideProperty() { return glintOverride; }
    public BooleanProperty hideTooltipProperty() { return hideTooltip; }
    public IntegerProperty modelDataProperty() { return modelData; }
    public StringProperty itemModelProperty() { return itemModel; }
    public StringProperty rgbProperty() { return rgb; }
    public ObservableList<String> getSelectedFlags() { return selectedFlags; }
    public ObservableList<String> getEnchantments() { return enchantments; }
    public StringProperty currentClickTypeProperty() { return currentClickType; }
    public ObservableList<Action> getCurrentActions() { return currentActions; }
}
