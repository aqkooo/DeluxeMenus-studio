package com.ejyqyl.dmstudio.model;

import com.ejyqyl.dmstudio.model.requirement.RequirementList;

import java.util.*;

/**
 * A menu item configuration representing an entry in the 'items:' section.
 */
public class MenuItem {

    private String key;
    private String material;
    private Integer slot;
    private final List<String> slots = new ArrayList<>();
    private int priority = 0;
    private int amount = 1;
    private String dynamicAmount;
    private Integer damage;
    private boolean update = false;

    private ItemAppearance appearance = new ItemAppearance();
    private ClickHandler clickHandler = new ClickHandler();
    private RequirementList viewRequirement;
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public MenuItem() {
        this.key = "item";
        this.material = "STONE";
    }

    public MenuItem(String key, String material) {
        this.key = key;
        this.material = material;
    }

    public MenuItem copy() {
        MenuItem copy = new MenuItem();
        copy.key = this.key;
        copy.material = this.material;
        copy.slot = this.slot;
        copy.slots.addAll(this.slots);
        copy.priority = this.priority;
        copy.amount = this.amount;
        copy.dynamicAmount = this.dynamicAmount;
        copy.damage = this.damage;
        copy.update = this.update;
        copy.appearance = this.appearance != null ? this.appearance.copy() : new ItemAppearance();
        copy.clickHandler = this.clickHandler != null ? this.clickHandler.copy() : new ClickHandler();
        copy.viewRequirement = this.viewRequirement != null ? this.viewRequirement.copy() : null;
        copy.unknownFields.putAll(this.unknownFields);
        return copy;
    }

    /**
     * Resolves all numeric slot indices assigned to this item, parsing both `slot` and `slots` ranges.
     */
    public Set<Integer> getAssignedSlots() {
        Set<Integer> result = new TreeSet<>();
        if (slot != null) {
            result.add(slot);
        }
        for (String s : slots) {
            if (s == null || s.isBlank()) continue;
            String trimmed = s.trim();
            if (trimmed.contains("-")) {
                String[] parts = trimmed.split("-");
                if (parts.length == 2) {
                    try {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        int min = Math.min(start, end);
                        int max = Math.max(start, end);
                        for (int i = min; i <= max; i++) {
                            result.add(i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    result.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public Integer getSlot() { return slot; }
    public void setSlot(Integer slot) { this.slot = slot; }

    public List<String> getSlots() { return slots; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getDynamicAmount() { return dynamicAmount; }
    public void setDynamicAmount(String dynamicAmount) { this.dynamicAmount = dynamicAmount; }

    public Integer getDamage() { return damage; }
    public void setDamage(Integer damage) { this.damage = damage; }

    public boolean isUpdate() { return update; }
    public void setUpdate(boolean update) { this.update = update; }

    public ItemAppearance getAppearance() { return appearance; }
    public void setAppearance(ItemAppearance appearance) { this.appearance = appearance; }

    public MenuItem deepCopy() { return copy(); }

    public void setSlots(List<String> newSlots) {
        this.slots.clear();
        if (newSlots != null) this.slots.addAll(newSlots);
    }

    public ClickHandler getClickHandler() { return clickHandler; }
    public void setClickHandler(ClickHandler clickHandler) { this.clickHandler = clickHandler; }

    public ClickHandler getClickHandlers() { return clickHandler; }
    public void setClickHandlers(ClickHandler clickHandler) { this.clickHandler = clickHandler; }

    public RequirementList getLeftClickRequirement() {
        return clickHandler != null ? clickHandler.getLeftClickRequirement() : null;
    }

    public RequirementList getRightClickRequirement() {
        return clickHandler != null ? clickHandler.getRightClickRequirement() : null;
    }

    public RequirementList getViewRequirement() { return viewRequirement; }
    public void setViewRequirement(RequirementList viewRequirement) { this.viewRequirement = viewRequirement; }

    public Map<String, Object> getUnknownFields() { return unknownFields; }
}
