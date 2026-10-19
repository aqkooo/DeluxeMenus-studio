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
        dialog.setTitle(I18nService.getInstance().get("dialog.diff.title"));
        dialog.setWidth(900);
        dialog.setHeight(620);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("root");

        DiffReport report = DiffEngine.compare(
                diskContent != null ? diskContent : "",
                currentYaml != null ? currentYaml : ""
        );

        // 1. Header with file info and stat badges
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label(I18nService.getInstance().get("dialog.diff.header", fileName));
        headerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        Label badgeAdded = createBadge(I18nService.getInstance().get("dialog.diff.added", report.addedCount()),
                "rgba(46, 160, 67, 0.18)", "#2ea043");
        Label badgeRemoved = createBadge(I18nService.getInstance().get("dialog.diff.removed", report.removedCount()),
                "rgba(248, 81, 73, 0.18)", "#cf222e");
        Label badgeUnchanged = createBadge(I18nService.getInstance().get("dialog.diff.unchanged", report.unchangedCount()),
                "rgba(140, 149, 159, 0.16)", "#787f8f");

        headerBox.getChildren().addAll(headerTitle, badgeAdded, badgeRemoved, badgeUnchanged);

        // 2. Diff Lines List
        ListView<DiffLine> diffList = new ListView<>();
        diffList.getItems().addAll(report.lines());
        VBox.setVgrow(diffList, Priority.ALWAYS);

        Label emptyPlaceholder = new Label(I18nService.getInstance().get("dialog.diff.no_changes"));
        emptyPlaceholder.setStyle("-fx-font-size: 13px; -fx-text-fill: #787f8f;");
        diffList.setPlaceholder(emptyPlaceholder);

        diffList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DiffLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String oldCol = item.oldLineNo() != null ? String.format("%4d", item.oldLineNo()) : "    ";
                    String newCol = item.newLineNo() != null ? String.format("%4d", item.newLineNo()) : "    ";
                    String sign = switch (item.type()) {
                        case ADDED -> "+";
                        case REMOVED -> "-";
                        case UNCHANGED -> " ";
                    };
                    String lineText = oldCol + " " + newCol + "  " + sign + "  " + item.text();
                    setText(lineText);

                    switch (item.type()) {
                        case ADDED -> setStyle("-fx-background-color: rgba(46, 160, 67, 0.18); -fx-text-fill: #2ea043; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px;");
                        case REMOVED -> setStyle("-fx-background-color: rgba(248, 81, 73, 0.18); -fx-text-fill: #cf222e; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px;");
                        case UNCHANGED -> setStyle("-fx-background-color: transparent; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px;");
                    }
                }
            }
        });

        // 3. Bottom Action Bar
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

        root.getChildren().addAll(headerBox, diffList, new Separator(), btnBox);

        Scene scene = new Scene(root);
        ThemeService.getInstance().applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static Label createBadge(String text, String bg, String fg) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 12px;");
        return badge;
    }
}
