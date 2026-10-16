package com.ejyqyl.dmstudio.ui.component;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.project.WorkspaceManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Sidebar component displaying the directory tree and search of DeluxeMenus workspace files (§Workspace Explorer).
 */
public class WorkspaceExplorerView extends VBox {

    private final TextField searchField;
    private final ListView<Path> fileListView;
    private final Label workspaceLabel;
    private final ObservableList<Path> filteredList = FXCollections.observableArrayList();
    private Consumer<Path> onFileSelected;

    public WorkspaceExplorerView() {
        setSpacing(8);
        setPadding(new Insets(10));
        getStyleClass().add("properties-pane");

        // 1. Workspace Header
        HBox headerBox = new HBox(6);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        workspaceLabel = new Label(I18nService.getInstance().get("workspace.no_workspace"));
        workspaceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        HBox.setHgrow(workspaceLabel, Priority.ALWAYS);

        Button btnOpen = new Button(I18nService.getInstance().get("workspace.open_btn"));
        btnOpen.setOnAction(e -> chooseWorkspace());

        Button btnRefresh = new Button(I18nService.getInstance().get("workspace.refresh_btn"));
        btnRefresh.setOnAction(e -> WorkspaceManager.getInstance().refreshWorkspace());

        headerBox.getChildren().addAll(workspaceLabel, btnOpen, btnRefresh);

        // 2. Search / Filter Field
        searchField = new TextField();
        searchField.setPromptText(I18nService.getInstance().get("workspace.search_prompt"));
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        // 3. File List View
        fileListView = new ListView<>(filteredList);
        VBox.setVgrow(fileListView, Priority.ALWAYS);

        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    Path root = WorkspaceManager.getInstance().currentWorkspaceProperty().get();
                    String display = root != null ? root.relativize(item).toString() : item.getFileName().toString();
                    setText(display);

                    ContextMenu cm = new ContextMenu();
                    MenuItem openItem = new MenuItem(I18nService.getInstance().get("workspace.menu_open"));
                    openItem.setOnAction(ev -> {
                        if (onFileSelected != null) onFileSelected.accept(item);
                    });

                    MenuItem revealItem = new MenuItem(I18nService.getInstance().get("workspace.menu_reveal"));
                    revealItem.setOnAction(ev -> {
                        try {
                            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(item.getParent().toFile());
                        } catch (Exception ignored) {}
                    });

                    cm.getItems().addAll(openItem, revealItem);
                    setContextMenu(cm);
                }
            }
        });

        fileListView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                Path selected = fileListView.getSelectionModel().getSelectedItem();
                if (selected != null && onFileSelected != null) {
                    onFileSelected.accept(selected);
                }
            }
        });

        getChildren().addAll(headerBox, searchField, fileListView);

        // Bindings to WorkspaceManager
        WorkspaceManager wm = WorkspaceManager.getInstance();
        wm.currentWorkspaceProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                workspaceLabel.setText(newV.getFileName().toString());
                workspaceLabel.setTooltip(new Tooltip(newV.toAbsolutePath().toString()));
            } else {
                workspaceLabel.setText(I18nService.getInstance().get("workspace.no_workspace"));
                workspaceLabel.setTooltip(null);
            }
        });

        wm.getMenuFiles().addListener((javafx.collections.ListChangeListener<Path>) c -> applyFilter());
        applyFilter();
    }

    public void setOnFileSelected(Consumer<Path> onFileSelected) {
        this.onFileSelected = onFileSelected;
    }

    private void chooseWorkspace() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(I18nService.getInstance().get("menu.file.open_workspace"));
        Path current = WorkspaceManager.getInstance().currentWorkspaceProperty().get();
        if (current != null && current.toFile().isDirectory()) {
            dc.setInitialDirectory(current.toFile());
        }
        File selected = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            WorkspaceManager.getInstance().openWorkspace(selected);
        }
    }

    private void applyFilter() {
        String filter = searchField.getText() != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        ObservableList<Path> all = WorkspaceManager.getInstance().getMenuFiles();
        if (filter.isEmpty()) {
            filteredList.setAll(all);
        } else {
            filteredList.setAll(all.stream()
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(filter))
                    .toList());
        }
    }
}
