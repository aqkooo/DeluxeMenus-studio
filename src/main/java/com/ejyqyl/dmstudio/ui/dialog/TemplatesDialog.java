package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.template.LayoutTemplate;
import com.ejyqyl.dmstudio.service.template.LayoutTemplateService;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Visual dialog for browsing, applying, and saving menu layout templates.
 */
public class TemplatesDialog {

    public static void show(Window owner, MainViewModel viewModel) {
        if (viewModel == null || viewModel.getConfig() == null) return;

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(I18nService.getInstance().get("dialog.templates.title"));

        BorderPane root = new BorderPane();
        root.setPrefSize(720, 480);
        root.getStyleClass().add("templates-dialog-root");

        String lang = I18nService.getInstance().getCurrentLanguage();

        // Left sidebar: Category combo + Templates ListView
        VBox leftBox = new VBox(10);
        leftBox.setPadding(new Insets(14));
        leftBox.setPrefWidth(260);

        ComboBox<String> catFilter = new ComboBox<>();
        catFilter.setMaxWidth(Double.MAX_VALUE);
        catFilter.getItems().add("ALL");

        ListView<LayoutTemplate> templateList = new ListView<>();
        VBox.setVgrow(templateList, Priority.ALWAYS);

        templateList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LayoutTemplate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(item.getName(lang));
                    nameLbl.setStyle("-fx-font-weight: bold;");
                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    Label badge = new Label(item.isCustom() ? "Custom" : item.category());
                    badge.setStyle("-fx-background-color: " + (item.isCustom() ? "#7c3aed" : "#3b82f6") + "; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 10px;");
                    box.getChildren().addAll(nameLbl, sp, badge);
                    setGraphic(box);
                }
            }
        });

        Runnable refreshList = () -> {
            List<LayoutTemplate> all = LayoutTemplateService.getInstance().getAllTemplates();
            String selCat = catFilter.getValue();
            List<LayoutTemplate> filtered = all.stream()
                    .filter(t -> selCat == null || "ALL".equals(selCat) || selCat.equalsIgnoreCase(t.category()))
                    .toList();
            templateList.setItems(FXCollections.observableArrayList(filtered));
            if (!filtered.isEmpty()) templateList.getSelectionModel().selectFirst();
        };

        // Populate categories
        Set<String> cats = new java.util.LinkedHashSet<>();
        for (LayoutTemplate t : LayoutTemplateService.getInstance().getAllTemplates()) cats.add(t.category());
        catFilter.getItems().addAll(cats);
        catFilter.setValue("ALL");
        catFilter.valueProperty().addListener((obs, oldV, newV) -> refreshList.run());

        leftBox.getChildren().addAll(new Label(I18nService.getInstance().get("dialog.templates.category")), catFilter, templateList);

        // Right details area
        VBox rightBox = new VBox(14);
        rightBox.setPadding(new Insets(16));
        rightBox.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label();
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label descLabel = new Label();
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        VBox itemsPreviewBox = new VBox(6);
        itemsPreviewBox.setStyle("-fx-background-color: rgba(0,0,0,0.15); -fx-padding: 10; -fx-background-radius: 6;");
        VBox.setVgrow(itemsPreviewBox, Priority.ALWAYS);

        Button btnDeleteCustom = new Button(I18nService.getInstance().get("dialog.templates.delete"));
        btnDeleteCustom.setStyle("-fx-text-fill: #ef4444;");
        btnDeleteCustom.setVisible(false);

        templateList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) {
                titleLabel.setText("");
                descLabel.setText("");
                itemsPreviewBox.getChildren().clear();
                btnDeleteCustom.setVisible(false);
                return;
            }
            titleLabel.setText(selected.getName(lang));
            descLabel.setText(selected.getDescription(lang));
            btnDeleteCustom.setVisible(selected.isCustom());

            itemsPreviewBox.getChildren().clear();
            itemsPreviewBox.getChildren().add(new Label(I18nService.getInstance().get("dialog.templates.preview_header")));
            if (selected.staticItems() != null) {
                for (MenuItem mi : selected.staticItems()) {
                    Label l = new Label(mi.getKey() + " -> " + mi.getMaterial() + " (slot: " + mi.getSlot() + ")");
                    l.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
                    itemsPreviewBox.getChildren().add(l);
                }
            } else {
                Label l = new Label(I18nService.getInstance().get("dialog.templates.dynamic_notice"));
                l.setStyle("-fx-font-style: italic; -fx-text-fill: #60a5fa;");
                itemsPreviewBox.getChildren().add(l);
            }
        });

        btnDeleteCustom.setOnAction(e -> {
            LayoutTemplate sel = templateList.getSelectionModel().getSelectedItem();
            if (sel != null && sel.isCustom()) {
                LayoutTemplateService.getInstance().deleteCustomTemplate(sel.id());
                refreshList.run();
            }
        });

        rightBox.getChildren().addAll(titleLabel, descLabel, itemsPreviewBox, btnDeleteCustom);

        // Bottom Bar
        HBox bottomBar = new HBox(12);
        bottomBar.setPadding(new Insets(12, 16, 12, 16));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnSaveSelection = new Button(I18nService.getInstance().get("dialog.templates.save_selection"));
        btnSaveSelection.setOnAction(e -> handleSaveSelectionAsTemplate(owner, viewModel, () -> {
            refreshList.run();
        }));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        Button btnCancel = new Button(I18nService.getInstance().get("dialog.cancel"));
        btnCancel.setOnAction(e -> stage.close());

        Button btnApply = new Button(I18nService.getInstance().get("dialog.templates.apply"));
        btnApply.getStyleClass().add("primary-button");
        btnApply.setDefaultButton(true);

        btnApply.setOnAction(e -> {
            LayoutTemplate sel = templateList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Command cmd = LayoutTemplateService.getInstance().applyTemplate(viewModel.getConfig(), sel);
                viewModel.getCommandStack().execute(cmd);
                viewModel.setStatusMessage(I18nService.getInstance().get("dialog.templates.applied", sel.getName(lang)));
                stage.close();
            }
        });

        bottomBar.getChildren().addAll(btnSaveSelection, bottomSpacer, btnCancel, btnApply);

        root.setLeft(leftBox);
        root.setCenter(rightBox);
        root.setBottom(bottomBar);

        refreshList.run();

        Scene scene = new Scene(root);
        ThemeService.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void handleSaveSelectionAsTemplate(Window owner, MainViewModel viewModel, Runnable onSaved) {
        Set<Integer> selectedSlots = viewModel.getGridViewModel().getMultiSelectedSlots();
        List<MenuItem> toSave = new ArrayList<>();
        if (selectedSlots.isEmpty()) {
            if (viewModel.getGridViewModel().getSelectedSlot() >= 0) {
                toSave.addAll(viewModel.getConfig().getItemsAtSlot(viewModel.getGridViewModel().getSelectedSlot()));
            } else {
                toSave.addAll(viewModel.getConfig().getItems().values());
            }
        } else {
            for (int s : selectedSlots) {
                toSave.addAll(viewModel.getConfig().getItemsAtSlot(s));
            }
        }

        if (toSave.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18nService.getInstance().get("dialog.warning_title"));
            alert.setHeaderText(null);
            alert.setContentText(I18nService.getInstance().get("dialog.templates.no_items_to_save"));
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("My Template");
        dialog.setTitle(I18nService.getInstance().get("dialog.templates.save_title"));
        dialog.setHeaderText(I18nService.getInstance().get("dialog.templates.save_header"));
        dialog.setContentText(I18nService.getInstance().get("dialog.templates.save_prompt"));
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.isBlank()) {
                try {
                    LayoutTemplateService.getInstance().saveCustomTemplate(name.trim(), "Custom", "User saved layout", toSave);
                    if (onSaved != null) onSaved.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
