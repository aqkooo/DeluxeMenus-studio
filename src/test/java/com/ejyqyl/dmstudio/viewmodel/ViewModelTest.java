package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.command.CommandStack;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ViewModelTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started
        }
    }

    @Test
    void testGridViewModelPresetsAndOperations() {
        MenuConfig config = new MenuConfig();
        CommandStack stack = new CommandStack();
        GridViewModel gridVm = new GridViewModel(config, stack);

        gridVm.applyPresetBorder("GRAY_STAINED_GLASS_PANE");
        assertThat(config.getItems()).containsKey("border");
        MenuItem border = config.getItems().get("border");
        assertThat(border.getSlots()).isNotEmpty();

        gridVm.applyPresetClearAll();
        assertThat(config.getItems()).isEmpty();

        gridVm.applyPresetCheckerboard("BLACK_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE");
        assertThat(config.getItems()).containsKeys("checker_a", "checker_b");

        gridVm.applyPresetClearAll();
        gridVm.createDefaultItemAt(13, "DIAMOND");
        assertThat(config.getItems()).containsKey("item_13");
        assertThat(config.getItems().get("item_13").getMaterial()).isEqualTo("DIAMOND");

        gridVm.createLowerPriorityVariant(13);
        List<MenuItem> itemsAt13 = config.getItemsAtSlot(13);
        assertThat(itemsAt13).hasSize(2);
        assertThat(itemsAt13.get(1).getPriority()).isGreaterThan(itemsAt13.get(0).getPriority());

        gridVm.moveItem(13, 20, false);
        assertThat(config.getItemsAtSlot(13)).hasSize(1);
        assertThat(config.getItemsAtSlot(20)).hasSize(1);

        // Test changeMaterialAt
        gridVm.changeMaterialAt(20, "NETHERITE_SWORD");
        assertThat(config.getItemsAtSlot(20).get(0).getMaterial()).isEqualTo("NETHERITE_SWORD");

        // Test setMaterialForSelectedSlots
        gridVm.selectSlot(1);
        gridVm.toggleMultiSelectSlot(2);
        gridVm.setMaterialForSelectedSlots("CHEST");
        assertThat(config.getItemsAtSlot(1).get(0).getMaterial()).isEqualTo("CHEST");
        assertThat(config.getItemsAtSlot(2).get(0).getMaterial()).isEqualTo("CHEST");

        // Test multi-slot item isolation in clearSlot
        gridVm.applyPresetClearAll();
        gridVm.applyPresetBorder("GRAY_STAINED_GLASS_PANE");
        assertThat(config.getItemsAtSlot(0)).isNotEmpty();
        assertThat(config.getItemsAtSlot(1)).isNotEmpty();
        gridVm.clearSlot(0);
        assertThat(config.getItemsAtSlot(0)).isEmpty();
        assertThat(config.getItemsAtSlot(1)).isNotEmpty();

        // Test multi-slot item detachment in moveItem
        gridVm.moveItem(1, 13, false);
        assertThat(config.getItemsAtSlot(1)).isEmpty();
        assertThat(config.getItemsAtSlot(13)).isNotEmpty();
        assertThat(config.getItemsAtSlot(13).get(0).getSlot()).isEqualTo(13);
        assertThat(config.getItemsAtSlot(2)).isNotEmpty();

        // Test no-op moveItem when from == to
        int cmdCountBefore = stack.getUndoCount();
        gridVm.moveItem(13, 13, false);
        assertThat(stack.getUndoCount()).isEqualTo(cmdCountBefore);

        // Test clampSelectionToBounds
        config.setSize(54);
        gridVm.selectSlot(50);
        assertThat(gridVm.getSelectedSlot()).isEqualTo(50);
        config.setSize(27);
        gridVm.clampSelectionToBounds();
        assertThat(gridVm.getSelectedSlot()).isEqualTo(-1);
    }

    @Test
    void testItemEditorViewModelBindingAndCommit() {
        MenuConfig config = new MenuConfig();
        CommandStack stack = new CommandStack();
        PreviewViewModel previewVm = new PreviewViewModel(config);
        ItemEditorViewModel editorVm = new ItemEditorViewModel(config, stack, previewVm);

        MenuItem item = new MenuItem("test_key", "STONE");
        item.setSlot(5);
        config.getItems().put("test_key", item);

        editorVm.bindItem(item);
        assertThat(editorVm.keyProperty().get()).isEqualTo("test_key");
        assertThat(editorVm.materialProperty().get()).isEqualTo("STONE");
        assertThat(editorVm.slotProperty().get()).isEqualTo(5);

        editorVm.materialProperty().set("EMERALD");
        editorVm.displayNameProperty().set("&aEmerald Gem");
        editorVm.commitChanges();

        MenuItem updated = config.getItems().get("test_key");
        assertThat(updated.getMaterial()).isEqualTo("EMERALD");
        assertThat(updated.getAppearance().getDisplayName()).isEqualTo("&aEmerald Gem");

        // Test creating new item from scratch when currentItem is null (like typing in empty inspector)
        editorVm.bindItem(null);
        editorVm.keyProperty().set("menu");
        editorVm.materialProperty().set("BLUE_WOOL");
        editorVm.slotProperty().set(0);
        editorVm.slotsStringProperty().set("0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27");
        editorVm.commitChanges();

        assertThat(config.getItems()).containsKey("menu");
        MenuItem created = config.getItems().get("menu");
        assertThat(created.getMaterial()).isEqualTo("BLUE_WOOL");
        assertThat(created.getSlots()).contains("0", "1", "2", "3", "18");
        assertThat(config.getItemsAtSlot(0)).contains(created);
        assertThat(config.getItemsAtSlot(18)).contains(created);
    }

    @Test
    void testProblemsViewModelCounting() {
        ProblemsViewModel vm = new ProblemsViewModel();
        List<ValidationIssue> issues = List.of(
                ValidationIssue.error("V-1", "Error 1", "item1", 10, "slot"),
                ValidationIssue.warning("V-4", "Warn 1", "item1", 10, "material"),
                ValidationIssue.warning("V-6", "Warn 2", "item1", 10, "slots")
        );
        vm.updateIssues(issues);

        assertThat(vm.errorCountProperty().get()).isEqualTo(1);
        assertThat(vm.warningCountProperty().get()).isEqualTo(2);
        assertThat(vm.getIssues()).hasSize(3);
    }

    @Test
    void testMainViewModelNewMenuAndValidation() {
        MainViewModel mainVm = new MainViewModel();
        assertThat(mainVm.getConfig()).isNotNull();

        mainVm.newMenu();
        assertThat(mainVm.getConfig().getMenuTitle()).isEqualTo("&8Custom Menu");

        mainVm.runValidation();
        assertThat(mainVm.getProblemsViewModel().getIssues()).isEmpty(); // Default menu is clean
    }
}
