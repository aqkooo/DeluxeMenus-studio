package com.ejyqyl.dmstudio.command;

import com.ejyqyl.dmstudio.command.impl.BatchCommand;
import com.ejyqyl.dmstudio.command.impl.RemoveItemCommand;
import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandStackTest {

    @Test
    void testExecuteUndoRedo() {
        MenuConfig config = new MenuConfig();
        CommandStack stack = new CommandStack();

        MenuItem item = new MenuItem("test", "DIRT");
        item.setSlot(0);

        stack.execute(new SetItemCommand(config, item));
        assertThat(config.getItems()).containsKey("test");
        assertThat(stack.canUndo()).isTrue();
        assertThat(stack.canRedo()).isFalse();
        assertThat(stack.isDirty()).isTrue();

        stack.undo();
        assertThat(config.getItems()).doesNotContainKey("test");
        assertThat(stack.canUndo()).isFalse();
        assertThat(stack.canRedo()).isTrue();

        stack.redo();
        assertThat(config.getItems()).containsKey("test");
    }

    @Test
    void testBatchCommandAtomicUndo() {
        MenuConfig config = new MenuConfig();
        CommandStack stack = new CommandStack();

        MenuItem item1 = new MenuItem("item1", "STONE");
        MenuItem item2 = new MenuItem("item2", "DIAMOND");

        Command cmd1 = new SetItemCommand(config, item1);
        Command cmd2 = new SetItemCommand(config, item2);
        BatchCommand batch = new BatchCommand("Add items", List.of(cmd1, cmd2));

        stack.execute(batch);
        assertThat(config.getItems()).containsKeys("item1", "item2");

        stack.undo();
        assertThat(config.getItems()).doesNotContainKeys("item1", "item2");

        stack.redo();
        assertThat(config.getItems()).containsKeys("item1", "item2");
    }

    @Test
    void testDirtyTrackingWithMarkSaved() {
        MenuConfig config = new MenuConfig();
        CommandStack stack = new CommandStack();

        stack.execute(new SetItemCommand(config, new MenuItem("item1", "STONE")));
        assertThat(stack.isDirty()).isTrue();

        stack.markSaved();
        assertThat(stack.isDirty()).isFalse();

        stack.execute(new SetItemCommand(config, new MenuItem("item2", "DIAMOND")));
        assertThat(stack.isDirty()).isTrue();

        stack.undo();
        assertThat(stack.isDirty()).isFalse(); // Back at saved checkpoint!
    }

    @Test
    void testUpdateMenuPropertyCommand() {
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("Original Title");
        CommandStack stack = new CommandStack();

        Command cmd = new com.ejyqyl.dmstudio.command.impl.UpdateMenuPropertyCommand(
                config, "Change title",
                c -> c.setMenuTitle("New Title"),
                c -> c.setMenuTitle("Original Title")
        );

        stack.execute(cmd);
        assertThat(config.getMenuTitle()).isEqualTo("New Title");
        assertThat(cmd.getDescription()).isEqualTo("Change title");

        stack.undo();
        assertThat(config.getMenuTitle()).isEqualTo("Original Title");

        stack.redo();
        assertThat(config.getMenuTitle()).isEqualTo("New Title");
    }

    @Test
    void testRemoveItemCommand() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        config.getItems().put("item1", item);
        CommandStack stack = new CommandStack();

        Command removeCmd = new RemoveItemCommand(config, "item1");
        stack.execute(removeCmd);
        assertThat(config.getItems()).doesNotContainKey("item1");

        stack.undo();
        assertThat(config.getItems()).containsKey("item1");
        assertThat(config.getItems().get("item1").getMaterial()).isEqualTo("STONE");
    }
}
