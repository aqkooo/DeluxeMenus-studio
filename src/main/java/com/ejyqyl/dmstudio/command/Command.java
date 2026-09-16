package com.ejyqyl.dmstudio.command;

/**
 * Interface for commands in the Undo/Redo stack.
 */
public interface Command {
    void execute();
    void undo();
    String getDescription();
}
