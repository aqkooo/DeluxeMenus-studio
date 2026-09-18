package com.ejyqyl.dmstudio.command.impl;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;

/**
 * Command to add or update an item in MenuConfig.
 */
public class SetItemCommand implements Command {

    private final MenuConfig config;
    private final String oldKey;
    private final MenuItem newItem;
    private final MenuItem previousItem;

    public SetItemCommand(MenuConfig config, MenuItem newItem) {
        this(config, null, newItem);
    }

    public SetItemCommand(MenuConfig config, String oldKey, MenuItem newItem) {
        this.config = config;
        this.oldKey = oldKey;
        this.newItem = newItem.copy();
        String lookupKey = oldKey != null ? oldKey : newItem.getKey();
        MenuItem existing = config.getItems().get(lookupKey);
        this.previousItem = existing != null ? existing.copy() : null;
    }

    @Override
    public void execute() {
        if (oldKey != null && !oldKey.equals(newItem.getKey())) {
            config.getItems().remove(oldKey);
        }
        config.getItems().put(newItem.getKey(), newItem.copy());
    }

    @Override
    public void undo() {
        if (oldKey != null && !oldKey.equals(newItem.getKey())) {
            config.getItems().remove(newItem.getKey());
            if (previousItem != null) {
                config.getItems().put(oldKey, previousItem.copy());
            }
        } else if (previousItem != null) {
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
