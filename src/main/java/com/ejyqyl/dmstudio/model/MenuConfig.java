package com.ejyqyl.dmstudio.model;

import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;

import java.util.*;

/**
 * Top-level DeluxeMenus configuration model for a single menu.
 */
public class MenuConfig {

    private String menuTitle = "&8Menu";
    private InventoryType menuType = InventoryType.CHEST;
    private int size = 27;
    private final List<String> openCommands = new ArrayList<>();
    private boolean registerCommand = true;
    private Integer updateInterval;
    private RequirementList openRequirement;
    private final List<Action> openActions = new ArrayList<>();
    private final Map<String, MenuItem> items = new LinkedHashMap<>();
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public MenuConfig() {}

    public MenuConfig copy() {
        MenuConfig copy = new MenuConfig();
        copy.menuTitle = this.menuTitle;
        copy.menuType = this.menuType;
        copy.size = this.size;
        copy.openCommands.addAll(this.openCommands);
        copy.registerCommand = this.registerCommand;
        copy.updateInterval = this.updateInterval;
        if (this.openRequirement != null) {
            copy.openRequirement = this.openRequirement.copy();
        }
        for (Action a : this.openActions) {
            copy.openActions.add(a.copy());
        }
        for (Map.Entry<String, MenuItem> entry : this.items.entrySet()) {
            copy.items.put(entry.getKey(), entry.getValue().copy());
        }
        copy.unknownFields.putAll(this.unknownFields);
        return copy;
    }

    public List<MenuItem> getItemsAtSlot(int slot) {
        List<MenuItem> list = new ArrayList<>();
        for (MenuItem item : items.values()) {
            if (item.getAssignedSlots().contains(slot)) {
                list.add(item);
            }
        }
        list.sort(Comparator.comparingInt(MenuItem::getPriority));
        return list;
    }

    public String getMenuTitle() { return menuTitle; }
    public void setMenuTitle(String menuTitle) { this.menuTitle = menuTitle; }

    public InventoryType getMenuType() { return menuType; }
    public void setMenuType(InventoryType menuType) { this.menuType = menuType; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public List<String> getOpenCommands() { return openCommands; }

    public boolean isRegisterCommand() { return registerCommand; }
    public void setRegisterCommand(boolean registerCommand) { this.registerCommand = registerCommand; }

    public Integer getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(Integer updateInterval) { this.updateInterval = updateInterval; }

    public RequirementList getOpenRequirement() { return openRequirement; }
    public void setOpenRequirement(RequirementList openRequirement) { this.openRequirement = openRequirement; }

    public List<Action> getOpenActions() { return openActions; }

    public Map<String, MenuItem> getItems() { return items; }

    public Map<String, Object> getUnknownFields() { return unknownFields; }
}
