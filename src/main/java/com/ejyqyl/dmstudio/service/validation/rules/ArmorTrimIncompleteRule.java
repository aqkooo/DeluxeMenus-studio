package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-10: trim_material without trim_pattern or vice versa. Both are required together.
 */
public class ArmorTrimIncompleteRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-10";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            ItemAppearance app = item.getAppearance();
            if (app == null) continue;

            boolean hasMat = app.getTrimMaterial() != null && !app.getTrimMaterial().isBlank();
            boolean hasPat = app.getTrimPattern() != null && !app.getTrimPattern().isBlank();

            if (hasMat != hasPat) {
                issues.add(ValidationIssue.error(
                        "V-10",
                        "Item '" + item.getKey() + "': Both 'trim_material' and 'trim_pattern' must be defined together.",
                        item.getKey(),
                        item.getSlot(),
                        "trim"
                ));
            }
        }
    }
}
