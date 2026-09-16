package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.*;

/**
 * Rule V-3: Two items on the same slot with different priorities, but the higher priority item
 * (lowest priority number) has no view_requirement, meaning the lower item will never be shown.
 */
public class ViewRequirementMissingRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-3";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (int slot = 0; slot < config.getSize(); slot++) {
            List<MenuItem> items = config.getItemsAtSlot(slot);
            if (items.size() > 1) {
                // items is sorted ascending by priority (0 first)
                MenuItem highest = items.get(0);
                if (highest.getViewRequirement() == null || highest.getViewRequirement().isEmpty()) {
                    issues.add(ValidationIssue.warning(
                            "V-3",
                            "Slot " + slot + ": Item '" + highest.getKey() + "' has highest priority "
                                    + highest.getPriority() + " without view_requirement, shadowing lower priority items",
                            highest.getKey(),
                            slot,
                            "view_requirement"
                    ));
                }
            }
        }
    }
}
