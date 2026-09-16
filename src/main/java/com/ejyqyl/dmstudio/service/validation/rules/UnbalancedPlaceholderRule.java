package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-12: Unbalanced '%' placeholder symbols in strings.
 */
public class UnbalancedPlaceholderRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-12";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            ItemAppearance app = item.getAppearance();
            if (app == null) continue;

            if (isUnbalanced(app.getDisplayName())) {
                issues.add(ValidationIssue.warning(
                        "V-12",
                        "Item '" + item.getKey() + "': Unbalanced '%' in display_name.",
                        item.getKey(),
                        item.getSlot(),
                        "display_name"
                ));
            }

            for (String line : app.getLore()) {
                if (isUnbalanced(line)) {
                    issues.add(ValidationIssue.warning(
                            "V-12",
                            "Item '" + item.getKey() + "': Unbalanced '%' in lore line: " + line,
                            item.getKey(),
                            item.getSlot(),
                            "lore"
                    ));
                }
            }
        }
    }

    private boolean isUnbalanced(String text) {
        if (text == null) return false;
        long count = text.chars().filter(ch -> ch == '%').count();
        return count % 2 != 0;
    }
}
