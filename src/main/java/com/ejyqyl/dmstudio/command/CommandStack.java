package com.ejyqyl.dmstudio.command;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Manages the undo and redo history for editing operations.
 */
public class CommandStack {

    private final int maxHistory;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedo = new SimpleBooleanProperty(false);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    private Command savedCheckpoint = null;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public CommandStack() {
        this(50);
    }

    public CommandStack(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public void execute(Command command) {
        command.execute();
        if (undoStack.size() >= maxHistory) {
            undoStack.removeLast();
        }
        undoStack.push(command);
        redoStack.clear();
        updateProperties();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        updateProperties();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        updateProperties();
        return true;
    }

    public void markSaved() {
        savedCheckpoint = undoStack.peek();
        updateProperties();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        savedCheckpoint = null;
        updateProperties();
    }

    private void updateProperties() {
        canUndo.set(!undoStack.isEmpty());
        canRedo.set(!redoStack.isEmpty());
        Command current = undoStack.peek();
        dirty.set(current != savedCheckpoint);
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    public BooleanProperty canUndoProperty() { return canUndo; }
    public BooleanProperty canRedoProperty() { return canRedo; }
    public BooleanProperty dirtyProperty() { return dirty; }

    public boolean canUndo() { return canUndo.get(); }
    public boolean canRedo() { return canRedo.get(); }
    public boolean isDirty() { return dirty.get(); }

    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }
}
