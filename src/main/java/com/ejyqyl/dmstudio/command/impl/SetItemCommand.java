package com.ejyqyl.dmstudio.command.impl;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;

/**
 * Command to add or update an item in MenuConfig.
 */
public class SetItemCommand implements Command {

    private final MenuConfig config;
    private final MenuItem newItem;
    private final MenuItem previousItem;

    public SetItemCommand(MenuConfig config, MenuItem newItem) {
        this.config = config;
        this.newItem = newItem.copy();
        MenuItem existing = config.getItems().get(newItem.getKey());
        this.previousItem = existing != null ? existing.copy() : null;
    }

    @Override
    public void execute() {
        config.getItems().put(newItem.getKey(), newItem.copy());
    }

    @Override
    public void undo() {
        if (previousItem != null) {
            config.getItems().put(previousItem.getKey(), previousItem.copy());
        } else {
            config.getItems().remove(newItem.getKey());
        }
    }

    @Override
    public String getDescription() {
        return "Set item: " + newItem.getKey();
    }
}
