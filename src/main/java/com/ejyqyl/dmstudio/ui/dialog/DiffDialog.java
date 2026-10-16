package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.diff.DiffEngine;
import com.ejyqyl.dmstudio.service.diff.DiffEngine.DiffLine;
import com.ejyqyl.dmstudio.service.diff.DiffEngine.DiffReport;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Visual diff viewer comparing current configuration against saved disk file (§Diff Viewer).
 */
public class DiffDialog {

    public static void show(Window owner, String fileName, String diskContent, String currentYaml, Runnable onSave) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18nService.getInstance().get("dialog.diff.title", fileName));
        dialog.setWidth(850);
        dialog.setHeight(600);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("root");

        DiffReport report = DiffEngine.compare(diskContent != null ? diskContent : "", currentYaml != null ? currentYaml : "");

        Label header = new Label(I18nService.getInstance().get("dialog.diff.header", fileName));
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label stats = new Label(I18nService.getInstance().get(
                "dialog.diff.stats", report.addedCount(), report.removedCount(), report.unchangedCount()
        ));
        stats.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        ListView<DiffLine> diffList = new ListView<>();
        diffList.getItems().addAll(report.lines());
        VBox.setVgrow(diffList, Priority.ALWAYS);

        diffList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DiffLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String prefix = switch (item.type()) {
                        case ADDED -> "+ ";
                        case REMOVED -> "- ";
                        case UNCHANGED -> "  ";
                    };
                    String lineNo = item.newLineNo() != null ? String.format("%3d: ", item.newLineNo())
                            : (item.oldLineNo() != null ? String.format("%3d: ", item.oldLineNo()) : "    : ");
                    setText(lineNo + prefix + item.text());

                    switch (item.type()) {
                        case ADDED -> setStyle("-fx-background-color: rgba(46, 160, 67, 0.25); -fx-text-fill: #57ab5a; -fx-font-family: monospace;");
                        case REMOVED -> setStyle("-fx-background-color: rgba(248, 81, 73, 0.25); -fx-text-fill: #e5534b; -fx-font-family: monospace;");
                        case UNCHANGED -> setStyle("-fx-background-color: transparent; -fx-text-fill: inherit; -fx-font-family: monospace;");
                    }
                }
            }
        });

        Button btnSave = new Button(I18nService.getInstance().get("dialog.diff.save"));
        btnSave.setDefaultButton(true);
        btnSave.setOnAction(e -> {
            if (onSave != null) onSave.run();
            dialog.close();
        });

        Button btnClose = new Button(I18nService.getInstance().get("dialog.diff.close"));
        btnClose.setCancelButton(true);
        btnClose.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, btnClose, btnSave);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, stats, diffList, new Separator(), btnBox);

        Scene scene = new Scene(root);
        ThemeService.getInstance().applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
