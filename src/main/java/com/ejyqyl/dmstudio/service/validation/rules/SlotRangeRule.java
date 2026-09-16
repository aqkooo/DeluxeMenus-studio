package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;
import java.util.Set;

/**
 * Rule V-1: Slot out of menu bounds (slot &lt; 0 or slot &gt;= menu size).
 */
public class SlotRangeRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-1";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        int maxSize = config.getSize();
        for (MenuItem item : config.getItems().values()) {
            Set<Integer> assigned = item.getAssignedSlots();
            for (int s : assigned) {
                if (s < 0 || s >= maxSize) {
                    issues.add(ValidationIssue.error(
                            "V-1",
                            "Slot " + s + " is out of bounds for menu size " + maxSize,
                            item.getKey(),
                            s,
                            "slot"
                    ));
                }
            }
        }
    }
}
