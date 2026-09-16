package com.ejyqyl.dmstudio.viewmodel;

import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.model.validation.ValidationLevel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * ViewModel for the Problems panel (§9.1).
 */
public class ProblemsViewModel {

    private final ObservableList<ValidationIssue> issues = FXCollections.observableArrayList();
    private final IntegerProperty errorCount = new SimpleIntegerProperty(0);
    private final IntegerProperty warningCount = new SimpleIntegerProperty(0);

    public void updateIssues(List<ValidationIssue> newIssues) {
        issues.setAll(newIssues);
        int errors = 0;
        int warnings = 0;
        for (ValidationIssue issue : newIssues) {
            if (issue.level() == ValidationLevel.ERROR) errors++;
            else if (issue.level() == ValidationLevel.WARNING) warnings++;
        }
        errorCount.set(errors);
        warningCount.set(warnings);
    }

    public ObservableList<ValidationIssue> getIssues() { return issues; }
    public IntegerProperty errorCountProperty() { return errorCount; }
    public IntegerProperty warningCountProperty() { return warningCount; }
}
