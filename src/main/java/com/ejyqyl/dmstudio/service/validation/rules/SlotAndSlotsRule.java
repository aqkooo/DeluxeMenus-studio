package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-6: Both 'slot' and 'slots' specified on the same item.
 */
public class SlotAndSlotsRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-6";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            if (item.getSlot() != null && !item.getSlots().isEmpty()) {
                issues.add(ValidationIssue.warning(
                        "V-6",
                        "Item '" + item.getKey() + "' has both 'slot' and 'slots' defined. Prefer using one.",
                        item.getKey(),
                        item.getSlot(),
                        "slots"
                ));
            }
        }
    }
}
