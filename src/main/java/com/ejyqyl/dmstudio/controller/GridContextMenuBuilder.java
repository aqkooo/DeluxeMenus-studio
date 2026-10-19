package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import com.ejyqyl.dmstudio.ui.dialog.ComponentsDialog;
import com.ejyqyl.dmstudio.ui.dialog.HeadConfigurationDialog;
import com.ejyqyl.dmstudio.ui.dialog.MaterialPickerDialog;
import com.ejyqyl.dmstudio.ui.dialog.TemplatesDialog;
import com.ejyqyl.dmstudio.view.SlotCell;
import com.ejyqyl.dmstudio.viewmodel.GridViewModel;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper constructing single-slot and multi-slot contextual action menus.
 */
public class GridContextMenuBuilder {

    public static ContextMenu build(
            SlotCell slotCell,
            int slotIndex,
            MainViewModel mainViewModel,
            GridViewModel gridViewModel,
            String activePlacingMaterial,
            java.util.function.Consumer<String> onMaterialSelected
    ) {
        ContextMenu menu = new ContextMenu();
        I18nService i18n = I18nService.getInstance();
        Set<Integer> multiSelected = gridViewModel.getMultiSelectedSlots();

        if (multiSelected.size() > 1 && multiSelected.contains(slotIndex)) {
            // Multi-selection context menu
            MenuItem countItem = new MenuItem(i18n.get("grid.context.selected_count", multiSelected.size()));
            countItem.setDisable(true);
            countItem.setStyle("-fx-font-weight: bold;");

            MenuItem fillActiveItem = createItem(
                    i18n.get("grid.context.fill_active", activePlacingMaterial),
                    activePlacingMaterial,
                    false,
                    ev -> gridViewModel.setMaterialForSelectedSlots(activePlacingMaterial)
            );

            MenuItem chooseFillItem = createItem(
                    i18n.get("grid.context.choose_fill_material"),
                    "CHEST",
                    false,
                    ev -> {
                        Window win = slotCell.getScene().getWindow();
                        MaterialPickerDialog.show(win, activePlacingMaterial, chosen -> {
                            onMaterialSelected.accept(chosen);
                            gridViewModel.setMaterialForSelectedSlots(chosen);
                        });
                    }
            );

            MenuItem setPriorityItem = createItem(
                    i18n.get("grid.context.set_priority"),
                    null,
                    false,
                    ev -> showPriorityPrompt(slotCell.getScene().getWindow(), gridViewModel)
            );

            MenuItem saveTemplateItem = createItem(
                    i18n.get("dialog.templates.save_selection"),
                    "CRAFTING_TABLE",
                    false,
                    ev -> TemplatesDialog.show(slotCell.getScene().getWindow(), mainViewModel)
            );

            MenuItem clearSelectedItem = createItem(
                    i18n.get("grid.context.clear_selected"),
                    "BARRIER",
                    false,
                    ev -> gridViewModel.clearSelectedSlots()
            );

            MenuItem deselectItem = createItem(
                    i18n.get("grid.context.deselect"),
                    null,
                    false,
                    ev -> gridViewModel.selectSlot(-1)
            );

            menu.getItems().addAll(
                    countItem,
                    new SeparatorMenuItem(),
                    fillActiveItem,
                    chooseFillItem,
                    setPriorityItem,
                    new SeparatorMenuItem(),
                    saveTemplateItem,
                    new SeparatorMenuItem(),
                    clearSelectedItem,
                    deselectItem
            );
        } else {
            // Single-slot context menu
            List<com.ejyqyl.dmstudio.model.MenuItem> items = mainViewModel.getConfig().getItemsAtSlot(slotIndex);
            boolean hasItems = !items.isEmpty();
            String currentMat = hasItems ? items.get(0).getMaterial() : "STONE";

            menu.getItems().addAll(
                    createItem(i18n.get("grid.context.choose_material"), "CHEST", false,
                            ev -> MaterialPickerDialog.show(slotCell.getScene().getWindow(), currentMat, onMaterialSelected::accept)),
                    createItem(i18n.get("grid.context.configure_head"), "PLAYER_HEAD", false,
                            ev -> HeadConfigurationDialog.show(slotCell.getScene().getWindow(), currentMat, onMaterialSelected::accept)),
                    createQuickPaletteMenu(i18n.get("grid.context.quick_palette"), slotIndex, gridViewModel),
                    createItem(i18n.get("grid.context.insert_component"), "CRAFTING_TABLE", false, ev -> {
                        ComponentsDialog.show(slotCell.getScene().getWindow(), slotIndex, (compItem, targetSlot) -> {
                            mainViewModel.getCommandStack().execute(new SetItemCommand(mainViewModel.getConfig(), compItem));
                            gridViewModel.selectSlot(targetSlot);
                        });
                    }),
                    new SeparatorMenuItem(),
                    createItem(i18n.get("grid.context.copy"), null, !hasItems, ev -> gridViewModel.copyItemAt(slotIndex)),
                    createItem(i18n.get("grid.context.paste"), null, !gridViewModel.hasClipboardItem(), ev -> gridViewModel.pasteItemAt(slotIndex)),
                    createItem(i18n.get("grid.context.duplicate"), null, !hasItems, ev -> gridViewModel.duplicateItemAt(slotIndex)),
                    createItem(i18n.get("grid.context.lower_priority"), null, !hasItems, ev -> gridViewModel.createLowerPriorityVariant(slotIndex)),
                    new SeparatorMenuItem(),
                    createItem(i18n.get("grid.context.clear"), null, !hasItems, ev -> gridViewModel.clearSlot(slotIndex))
            );
        }

        return menu;
    }

    private static MenuItem createItem(String text, String icon, boolean disabled, EventHandler<ActionEvent> action) {
        MenuItem item = new MenuItem(text);
        if (icon != null) item.setGraphic(TextureManager.getInstance().createIconNode(icon, 16));
        item.setDisable(disabled);
        item.setOnAction(action);
        return item;
    }

    private static Menu createQuickPaletteMenu(String title, int slotIndex, GridViewModel gridViewModel) {
        Menu menu = new Menu(title);
        menu.setGraphic(TextureManager.getInstance().createIconNode("GRAY_STAINED_GLASS_PANE", 16));
        String[] quickMats = {
                "BLACK_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", "WHITE_STAINED_GLASS_PANE",
                "LIME_STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
                "STONE", "OAK_PLANKS", "CHEST", "DIAMOND", "GOLD_INGOT", "EMERALD",
                "NETHERITE_SWORD", "COMPASS", "CLOCK", "BOOK", "FEATHER", "BARRIER", "PLAYER_HEAD"
        };
        for (String qm : quickMats) {
            MenuItem itm = new MenuItem(qm);
            itm.setGraphic(TextureManager.getInstance().createIconNode(qm, 16));
            itm.setOnAction(ev -> gridViewModel.changeMaterialAt(slotIndex, qm));
            menu.getItems().add(itm);
        }
        return menu;
    }

    private static void showPriorityPrompt(Window owner, GridViewModel gridViewModel) {
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle(I18nService.getInstance().get("grid.context.priority_title"));
        dialog.setHeaderText(I18nService.getInstance().get("grid.context.priority_header"));
        dialog.setContentText(I18nService.getInstance().get("grid.context.priority_prompt"));
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(val -> {
            try {
                int p = Integer.parseInt(val.trim());
                gridViewModel.setPriorityForSelectedSlots(p);
            } catch (NumberFormatException ignored) {}
        });
    }
}
