package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.migration.LegacyConfigMigratorService;
import com.ejyqyl.dmstudio.service.migration.LegacyConfigMigratorService.MigrationChange;
import com.ejyqyl.dmstudio.service.migration.LegacyConfigMigratorService.MigrationReport;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Visual dialog reviewing and confirming legacy configuration and material migrations.
 */
public class MigrationDialog {

    public static void show(Window owner, MainViewModel viewModel) {
        if (viewModel == null || viewModel.getConfig() == null) return;

        MenuConfig config = viewModel.getConfig();
        LegacyConfigMigratorService service = new LegacyConfigMigratorService();
        MigrationReport report = service.analyze(config);

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(I18nService.getInstance().get("dialog.migration.title"));

        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setPrefSize(680, 500);
        root.getStyleClass().add("migration-dialog-root");

        // Header and metrics
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(I18nService.getInstance().get("dialog.migration.header"));
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label matBadge = new Label(report.materialChangesCount() + " " + I18nService.getInstance().get("dialog.migration.badge_materials"));
        matBadge.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px;");

        Label syntaxBadge = new Label(report.syntaxChangesCount() + " " + I18nService.getInstance().get("dialog.migration.badge_syntax"));
        syntaxBadge.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px;");

        headerBox.getChildren().addAll(titleLabel, spacer, matBadge, syntaxBadge);

        // Content area
        Node contentNode;
        Button btnApply = new Button(I18nService.getInstance().get("dialog.migration.apply"));
        btnApply.getStyleClass().add("primary-button");
        btnApply.setDefaultButton(true);

        if (!report.hasChanges()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            VBox.setVgrow(emptyBox, Priority.ALWAYS);
            Label emptyLbl = new Label(I18nService.getInstance().get("dialog.migration.empty"));
            emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(emptyLbl);
            contentNode = emptyBox;
            btnApply.setDisable(true);
        } else {
            TableView<MigrationChange> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            VBox.setVgrow(table, Priority.ALWAYS);

            TableColumn<MigrationChange, String> targetCol = new TableColumn<>(I18nService.getInstance().get("dialog.migration.col_target"));
            targetCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().target()));
            targetCol.setPrefWidth(120);

            TableColumn<MigrationChange, String> descCol = new TableColumn<>(I18nService.getInstance().get("dialog.migration.col_desc"));
            descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().description()));
            descCol.setPrefWidth(220);

            TableColumn<MigrationChange, String> oldCol = new TableColumn<>(I18nService.getInstance().get("dialog.migration.col_old"));
            oldCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().oldValue()));
            oldCol.setPrefWidth(150);
            oldCol.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String val, boolean empty) {
                    super.updateItem(val, empty);
                    if (empty || val == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(val);
                        setStyle("-fx-text-fill: #ef4444; -fx-font-family: monospace;");
                    }
                }
            });

            TableColumn<MigrationChange, String> newCol = new TableColumn<>(I18nService.getInstance().get("dialog.migration.col_new"));
            newCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().newValue()));
            newCol.setPrefWidth(150);
            newCol.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String val, boolean empty) {
                    super.updateItem(val, empty);
                    if (empty || val == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(val);
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-family: monospace;");
                    }
                }
            });

            table.getColumns().addAll(targetCol, descCol, oldCol, newCol);
            table.setItems(FXCollections.observableArrayList(report.changes()));
            contentNode = table;

            btnApply.setOnAction(e -> {
                Command cmd = service.buildMigrationCommand(config, report);
                viewModel.getCommandStack().execute(cmd);
                viewModel.setStatusMessage(I18nService.getInstance().get("dialog.migration.success", report.changes().size()));
                stage.close();
            });
        }

        // Action Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button(I18nService.getInstance().get("dialog.cancel"));
        btnCancel.setOnAction(e -> stage.close());
        buttonBar.getChildren().addAll(btnCancel, btnApply);

        root.getChildren().addAll(headerBox, contentNode, buttonBar);

        Scene scene = new Scene(root);
        ThemeService.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }
}
