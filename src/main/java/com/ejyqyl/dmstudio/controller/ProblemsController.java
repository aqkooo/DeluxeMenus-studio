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

        // Row factory for colors and double-click navigation
        problemsTable.setRowFactory(tv -> {
            TableRow<ValidationIssue> row = new TableRow<>() {
                @Override
                protected void updateItem(ValidationIssue item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if (item.level() == ValidationLevel.ERROR) {
                        setStyle("-fx-text-fill: #ff6b6b;");
                    } else if (item.level() == ValidationLevel.WARNING) {
                        setStyle("-fx-text-fill: #feca57;");
                    } else {
                        setStyle("-fx-text-fill: #48dbfb;");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ValidationIssue issue = row.getItem();
                    if (issue.slot() != null && mainViewModel != null) {
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
        if (errors == 0 && warnings == 0) {
            issuesSummaryLabel.setText(i18n.get("problems.no_issues"));
            issuesSummaryLabel.setStyle("-fx-text-fill: #2ecc71;");
        } else {
            issuesSummaryLabel.setText(i18n.get("problems.errors", errors) + ", " + i18n.get("problems.warnings", warnings));
            issuesSummaryLabel.setStyle(errors > 0 ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #feca57;");
        }
    }
}
