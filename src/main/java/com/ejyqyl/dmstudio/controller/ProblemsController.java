package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.model.validation.ValidationLevel;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import com.ejyqyl.dmstudio.viewmodel.ProblemsViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for Problems Panel displaying validation issues (§9.1).
 */
public class ProblemsController {

    @FXML private Label issuesSummaryLabel;
    @FXML private TableView<ValidationIssue> problemsTable;
    @FXML private TableColumn<ValidationIssue, String> colLevel;
    @FXML private TableColumn<ValidationIssue, String> colCode;
    @FXML private TableColumn<ValidationIssue, String> colMessage;
    @FXML private TableColumn<ValidationIssue, String> colSlot;

    private MainViewModel mainViewModel;
    private ProblemsViewModel problemsViewModel;

    @FXML
    public void initialize() {
        colLevel.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().level().name()));
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().code()));
        colMessage.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().message()));
        colSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().slot() != null ? String.valueOf(d.getValue().slot()) : "—"));

        // Eye-friendly badge chips for validation level
        colLevel.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().removeAll("badge-error", "badge-warning", "badge-info");
                    if ("ERROR".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-error");
                    } else if ("WARNING".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-info");
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        // Row factory for double-click navigation
        problemsTable.setRowFactory(tv -> {
            TableRow<ValidationIssue> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ValidationIssue issue = row.getItem();
                    if (issue != null && issue.slot() != null && mainViewModel != null) {
                        mainViewModel.getGridViewModel().selectSlot(issue.slot());
                    }
                }
            });
            return row;
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.problemsViewModel = mainViewModel.getProblemsViewModel();

        problemsTable.setItems(problemsViewModel.getIssues());

        updateSummary();
        problemsViewModel.errorCountProperty().addListener((obs, oldV, newV) -> updateSummary());
        problemsViewModel.warningCountProperty().addListener((obs, oldV, newV) -> updateSummary());
    }

    private void updateSummary() {
        int errors = problemsViewModel.errorCountProperty().get();
        int warnings = problemsViewModel.warningCountProperty().get();
        I18nService i18n = I18nService.getInstance();
        issuesSummaryLabel.getStyleClass().removeAll("status-success", "status-error", "status-warning");
        if (errors == 0 && warnings == 0) {
            issuesSummaryLabel.setText(i18n.get("problems.no_issues"));
            issuesSummaryLabel.getStyleClass().add("status-success");
        } else {
            issuesSummaryLabel.setText(i18n.get("problems.errors", errors) + ", " + i18n.get("problems.warnings", warnings));
            issuesSummaryLabel.getStyleClass().add(errors > 0 ? "status-error" : "status-warning");
        }
    }
}
