package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.pagination.PaginationService;
import com.ejyqyl.dmstudio.service.project.WorkspaceManager;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive wizard modal for generating linked multi-page menu series (§Pagination Wizard).
 */
public class PaginationWizardDialog {

    public static void show(Window owner, Path defaultDir, Consumer<MenuConfig> onFirstPageLoaded) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18nService.getInstance().get("dialog.pagination.title"));
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setPrefWidth(480);
        root.getStyleClass().add("root");

        Label header = new Label(I18nService.getInstance().get("dialog.pagination.header"));
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // 1. Base Key
        Label lblBase = new Label(I18nService.getInstance().get("dialog.pagination.base_key"));
        TextField txtBase = new TextField("shop");
        grid.add(lblBase, 0, 0);
        grid.add(txtBase, 1, 0);

        // 2. Title Pattern
        Label lblTitle = new Label(I18nService.getInstance().get("dialog.pagination.title_pattern"));
        TextField txtTitle = new TextField("&8Shop (Page {page}/{total})");
        grid.add(lblTitle, 0, 1);
        grid.add(txtTitle, 1, 1);

        // 3. Total Pages
        Label lblPages = new Label(I18nService.getInstance().get("dialog.pagination.total_pages"));
        Spinner<Integer> spPages = new Spinner<>(2, 20, 3);
        spPages.setEditable(true);
        grid.add(lblPages, 0, 2);
        grid.add(spPages, 1, 2);

        // 4. Menu Size
        Label lblSize = new Label(I18nService.getInstance().get("dialog.pagination.size"));
        ComboBox<Integer> cbSize = new ComboBox<>();
        cbSize.getItems().addAll(27, 36, 45, 54);
        cbSize.setValue(27);
        grid.add(lblSize, 0, 3);
        grid.add(cbSize, 1, 3);

        // 5. Navigation Slots
        Label lblPrev = new Label(I18nService.getInstance().get("dialog.pagination.prev_slot"));
        Spinner<Integer> spPrev = new Spinner<>(0, 26, 18);
        spPrev.setEditable(true);
        grid.add(lblPrev, 0, 4);
        grid.add(spPrev, 1, 4);

        Label lblNext = new Label(I18nService.getInstance().get("dialog.pagination.next_slot"));
        Spinner<Integer> spNext = new Spinner<>(0, 26, 26);
        spNext.setEditable(true);
        grid.add(lblNext, 0, 5);
        grid.add(spNext, 1, 5);

        Label lblInfo = new Label(I18nService.getInstance().get("dialog.pagination.info_slot"));
        Spinner<Integer> spInfo = new Spinner<>(0, 26, 22);
        spInfo.setEditable(true);
        grid.add(lblInfo, 0, 6);
        grid.add(spInfo, 1, 6);

        // Dynamic slot bounds when menu size changes
        cbSize.valueProperty().addListener((obs, oldV, newV) -> {
            int maxSlot = (newV != null ? newV : 27) - 1;
            spPrev.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxSlot, maxSlot - 8));
            spNext.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxSlot, maxSlot));
            spInfo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxSlot, maxSlot - 4));
        });

        // 6. Perimeter Border
        CheckBox cbBorder = new CheckBox(I18nService.getInstance().get("dialog.pagination.include_border"));
        cbBorder.setSelected(true);
        grid.add(cbBorder, 1, 7);

        // 7. Output Directory
        Label lblDir = new Label(I18nService.getInstance().get("dialog.pagination.target_dir"));
        TextField txtDir = new TextField();
        File initialDir = defaultDir != null ? defaultDir.toFile() : (
                WorkspaceManager.getInstance().currentWorkspaceProperty().get() != null
                        ? WorkspaceManager.getInstance().currentWorkspaceProperty().get().toFile()
                        : new File(".")
        );
        txtDir.setText(initialDir.getAbsolutePath());
        Button btnBrowse = new Button(I18nService.getInstance().get("dialog.pagination.browse"));
        btnBrowse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle(I18nService.getInstance().get("dialog.pagination.choose_dir"));
            if (initialDir.isDirectory()) dc.setInitialDirectory(initialDir);
            File f = dc.showDialog(dialog);
            if (f != null) txtDir.setText(f.getAbsolutePath());
        });
        HBox dirBox = new HBox(6, txtDir, btnBrowse);
        HBox.setHgrow(txtDir, Priority.ALWAYS);
        grid.add(lblDir, 0, 8);
        grid.add(dirBox, 1, 8);

        // Action Buttons
        Button btnGenerate = new Button(I18nService.getInstance().get("dialog.pagination.generate"));
        btnGenerate.setDefaultButton(true);
        Button btnCancel = new Button(I18nService.getInstance().get("dialog.pagination.cancel"));
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> dialog.close());

        btnGenerate.setOnAction(e -> {
            String rawBase = txtBase.getText() != null ? txtBase.getText().trim() : "shop";
            String baseKey = rawBase.isBlank() ? "shop" : rawBase;
            int pagesCount = spPages.getValue() != null ? spPages.getValue() : 3;
            int size = cbSize.getValue() != null ? cbSize.getValue() : 27;
            int prevSlot = spPrev.getValue() != null ? spPrev.getValue() : 18;
            int nextSlot = spNext.getValue() != null ? spNext.getValue() : 26;
            int infoSlot = spInfo.getValue() != null ? spInfo.getValue() : 22;
            boolean border = cbBorder.isSelected();
            String dirStr = txtDir.getText() != null ? txtDir.getText().trim() : ".";
            File outDir = new File(dirStr.isBlank() ? "." : dirStr);

            String titlePat = txtTitle.getText() != null && !txtTitle.getText().isBlank()
                    ? txtTitle.getText().trim() : "&8Shop (Page {page}/{total})";

            PaginationService.PaginationOptions opts = new PaginationService.PaginationOptions(
                    baseKey, titlePat, pagesCount, size,
                    prevSlot, nextSlot, infoSlot, "ARROW", "ARROW", "BOOK", border
            );

            PaginationService service = new PaginationService();
            List<MenuConfig> generated = service.generatePages(opts);
            try {
                service.savePagesToDirectory(generated, outDir, baseKey);
                WorkspaceManager.getInstance().refreshWorkspace();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle(I18nService.getInstance().get("dialog.info_title"));
                info.setHeaderText(null);
                info.setContentText(I18nService.getInstance().get("dialog.pagination.success", generated.size(), outDir.getName()));
                info.showAndWait();

                if (onFirstPageLoaded != null && !generated.isEmpty()) {
                    onFirstPageLoaded.accept(generated.get(0));
                }
                dialog.close();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle(I18nService.getInstance().get("dialog.error_title"));
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });

        HBox btnBox = new HBox(10, btnCancel, btnGenerate);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, grid, new Separator(), btnBox);

        Scene scene = new Scene(root);
        ThemeService.getInstance().applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
