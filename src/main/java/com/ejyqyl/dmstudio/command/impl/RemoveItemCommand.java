package com.ejyqyl.dmstudio.command.impl;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;

/**
 * Command to remove an item from MenuConfig.
 */
public class RemoveItemCommand implements Command {

    private final MenuConfig config;
    private final String itemKey;
    private final MenuItem removedItem;

    public RemoveItemCommand(MenuConfig config, String itemKey) {
        this.config = config;
        this.itemKey = itemKey;
        MenuItem existing = config.getItems().get(itemKey);
        this.removedItem = existing != null ? existing.copy() : null;
    }

    @Override
    public void execute() {
        config.getItems().remove(itemKey);
    }

    @Override
    public void undo() {
        if (removedItem != null) {
            config.getItems().put(itemKey, removedItem.copy());
        }
    }

    @Override
    public String getDescription() {
        return "Remove item: " + itemKey;
    }
}
