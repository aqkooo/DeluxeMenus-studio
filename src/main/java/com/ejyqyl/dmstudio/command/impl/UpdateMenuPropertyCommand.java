package com.ejyqyl.dmstudio.command.impl;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;

import java.util.function.Consumer;

/**
 * Command to update a property on MenuConfig with undo capability.
 */
public class UpdateMenuPropertyCommand implements Command {

    private final String description;
    private final Consumer<MenuConfig> apply;
    private final Consumer<MenuConfig> revert;
    private final MenuConfig config;

    public UpdateMenuPropertyCommand(MenuConfig config, String description, Consumer<MenuConfig> apply, Consumer<MenuConfig> revert) {
        this.config = config;
        this.description = description;
        this.apply = apply;
        this.revert = revert;
    }

    @Override
    public void execute() {
        apply.accept(config);
    }

    @Override
    public void undo() {
        revert.accept(config);
    }

    @Override
    public String getDescription() {
        return description;
    }
}
