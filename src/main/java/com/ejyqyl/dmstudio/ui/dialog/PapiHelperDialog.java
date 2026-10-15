package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.registry.PapiCatalogService;
import com.ejyqyl.dmstudio.service.registry.PapiCatalogService.PapiEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive dialog controller for browsing and inserting PlaceholderAPI placeholders (§Ecosystem Integration).
 */
public class PapiHelperDialog {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TableView<PapiEntry> papiTable;
    @FXML private TableColumn<PapiEntry, String> colCategory;
    @FXML private TableColumn<PapiEntry, String> colPlaceholder;
    @FXML private TableColumn<PapiEntry, String> colDescription;
    @FXML private Button btnInsert;
    @FXML private Button btnCopy;
    @FXML private Button btnClose;

    private Consumer<String> onInsert;
    private Stage stage;

    @FXML
    public void initialize() {
        String lang = I18nService.getInstance().getCurrentLanguage();

        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));
        colPlaceholder.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().placeholder()));
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription(lang)));

        // Category filter
        categoryCombo.getItems().add("ALL");
        categoryCombo.getItems().addAll(PapiCatalogService.getInstance().getCategories());
        categoryCombo.setValue("ALL");

        searchField.textProperty().addListener((obs, oldV, newV) -> updateList());
        categoryCombo.valueProperty().addListener((obs, oldV, newV) -> updateList());

        papiTable.setRowFactory(tv -> {
            TableRow<PapiEntry> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    insertSelected();
                }
            });
            return row;
        });

        btnInsert.setOnAction(e -> insertSelected());

        btnCopy.setOnAction(e -> {
            PapiEntry selected = papiTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.placeholder());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });

        btnClose.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        updateList();
    }

    private void updateList() {
        String query = searchField.getText();
        String cat = categoryCombo.getValue();
        List<PapiEntry> results = PapiCatalogService.getInstance().search(query, cat);
        papiTable.setItems(FXCollections.observableArrayList(results));
    }

    private void insertSelected() {
        PapiEntry selected = papiTable.getSelectionModel().getSelectedItem();
        if (selected != null && onInsert != null) {
            onInsert.accept(selected.placeholder());
            if (stage != null) stage.close();
        }
    }

    public static void show(Window owner, Consumer<String> onInsert) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    PapiHelperDialog.class.getResource("/fxml/papi_helper.fxml"),
                    I18nService.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle(I18nService.getInstance().get("tools.papi.title"));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);

            Scene scene = new Scene(loader.load());
            com.ejyqyl.dmstudio.service.theme.ThemeService.getInstance().applyTheme(scene);

            PapiHelperDialog controller = loader.getController();
            controller.stage = stage;
            controller.onInsert = onInsert;

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
