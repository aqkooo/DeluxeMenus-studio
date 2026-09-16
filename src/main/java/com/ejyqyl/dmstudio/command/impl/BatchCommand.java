package com.ejyqyl.dmstudio.command.impl;

import com.ejyqyl.dmstudio.command.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite command that groups multiple operations into a single undo/redo transaction.
 */
public class BatchCommand implements Command {

    private final String description;
    private final List<Command> commands = new ArrayList<>();

    public BatchCommand(String description, List<Command> commands) {
        this.description = description;
        if (commands != null) {
            this.commands.addAll(commands);
        }
    }

    @Override
    public void execute() {
        for (Command cmd : commands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        List<Command> reversed = new ArrayList<>(commands);
        Collections.reverse(reversed);
        for (Command cmd : reversed) {
            cmd.undo();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
}
