package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-11: Item has 'update: true' but the menu does not configure an 'update_interval'.
 */
public class UpdateIntervalMissingRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-11";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        boolean hasUpdateInterval = config.getUpdateInterval() != null && config.getUpdateInterval() > 0;
        if (!hasUpdateInterval) {
            for (MenuItem item : config.getItems().values()) {
                if (item.isUpdate()) {
                    issues.add(ValidationIssue.warning(
                            "V-11",
                            "Item '" + item.getKey() + "' has 'update: true', but menu has no 'update_interval' configured.",
                            item.getKey(),
                            item.getSlot(),
                            "update"
                    ));
                }
            }
        }
    }
}
