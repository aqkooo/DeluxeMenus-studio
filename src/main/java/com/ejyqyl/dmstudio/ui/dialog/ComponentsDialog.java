package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.preset.ComponentLibraryService;
import com.ejyqyl.dmstudio.service.preset.ComponentLibraryService.MenuComponent;
import com.ejyqyl.dmstudio.service.render.ColorCodeParser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Interactive dialog controller for inserting ready-to-use menu components (§Preset Library).
 */
public class ComponentsDialog {

    @FXML private ComboBox<String> categoryCombo;
    @FXML private ListView<MenuComponent> componentsList;
    @FXML private Label componentTitleLabel;
    @FXML private Label componentDescLabel;
    @FXML private VBox tooltipPreviewBox;
    @FXML private ListView<String> commandsList;
    @FXML private Spinner<Integer> targetSlotSpinner;
    @FXML private Button btnInsert;
    @FXML private Button btnClose;

    private BiConsumer<MenuItem, Integer> onInsert;
    private Stage stage;

    @FXML
    public void initialize() {
        String lang = I18nService.getInstance().getCurrentLanguage();

        targetSlotSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 53, 0));

        categoryCombo.getItems().add("ALL");
        categoryCombo.getItems().addAll(ComponentLibraryService.getInstance().getCategories());
        categoryCombo.setValue("ALL");

        componentsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MenuComponent item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String prefix = item.isCustom() ? "⭐ " : "📦 ";
                    setText(prefix + item.getName(lang) + " (" + item.category() + ")");
                }
            }
        });

        componentsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updatePreview(newV, lang);
        });

        categoryCombo.valueProperty().addListener((obs, oldV, newV) -> {
            List<MenuComponent> comps = ComponentLibraryService.getInstance().getComponents(newV);
            componentsList.setItems(FXCollections.observableArrayList(comps));
            if (!comps.isEmpty()) {
                componentsList.getSelectionModel().selectFirst();
            }
        });

        btnInsert.setOnAction(e -> {
            MenuComponent selected = componentsList.getSelectionModel().getSelectedItem();
            if (selected != null && onInsert != null) {
                int slot = targetSlotSpinner.getValue();
                MenuItem copy = selected.item().deepCopy();
                copy.setSlot(slot);
                copy.setSlots(null);
                onInsert.accept(copy, slot);
                if (stage != null) stage.close();
            }
        });

        btnClose.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        List<MenuComponent> all = ComponentLibraryService.getInstance().getComponents("ALL");
        componentsList.setItems(FXCollections.observableArrayList(all));
        if (!all.isEmpty()) {
            componentsList.getSelectionModel().selectFirst();
        }
    }

    private void updatePreview(MenuComponent comp, String lang) {
        if (comp == null) {
            componentTitleLabel.setText("");
            componentDescLabel.setText("");
            tooltipPreviewBox.getChildren().clear();
            commandsList.getItems().clear();
            return;
        }

        componentTitleLabel.setText(comp.getName(lang) + " [" + comp.item().getMaterial() + "]");
        componentDescLabel.setText(comp.getDescription(lang));
        if (comp.item().getSlot() != null) {
            targetSlotSpinner.getValueFactory().setValue(comp.item().getSlot());
        }

        // Live Minecraft Tooltip preview
        tooltipPreviewBox.getChildren().clear();
        String name = comp.item().getAppearance() != null ? comp.item().getAppearance().getDisplayName() : null;
        List<String> lore = comp.item().getAppearance() != null ? comp.item().getAppearance().getLore() : null;

        if (name != null && !name.isBlank()) {
            TextFlow nameFlow = new TextFlow();
            ColorCodeParser.populateTextFlow(nameFlow, name, Color.WHITE);
            tooltipPreviewBox.getChildren().add(nameFlow);
        }
        if (lore != null) {
            for (String l : lore) {
                TextFlow lineFlow = new TextFlow();
                ColorCodeParser.populateTextFlow(lineFlow, l, Color.web("#AAAAAA"));
                tooltipPreviewBox.getChildren().add(lineFlow);
            }
        }

        // Commands preview
        commandsList.getItems().clear();
        if (comp.item().getClickHandlers() != null) {
            if (comp.item().getClickHandlers().getLeftClickCommands() != null) {
                for (Action a : comp.item().getClickHandlers().getLeftClickCommands()) {
                    commandsList.getItems().add("[Left] " + a.getRawCommand());
                }
            }
            if (comp.item().getClickHandlers().getRightClickCommands() != null) {
                for (Action a : comp.item().getClickHandlers().getRightClickCommands()) {
                    commandsList.getItems().add("[Right] " + a.getRawCommand());
                }
            }
        }
    }

    public static void show(Window owner, BiConsumer<MenuItem, Integer> onInsert) {
        show(owner, -1, onInsert);
    }

    public static void show(Window owner, int defaultSlot, BiConsumer<MenuItem, Integer> onInsert) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ComponentsDialog.class.getResource("/fxml/components_dialog.fxml"),
                    I18nService.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle(I18nService.getInstance().get("tools.components.title"));
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    ComponentsDialog.class.getResource("/css/theme-dark.css").toExternalForm()
            );

            ComponentsDialog controller = loader.getController();
            controller.stage = stage;
            controller.onInsert = onInsert;
            if (defaultSlot >= 0 && defaultSlot < 54) {
                controller.targetSlotSpinner.getValueFactory().setValue(defaultSlot);
            }

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
