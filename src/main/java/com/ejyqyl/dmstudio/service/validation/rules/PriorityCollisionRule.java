package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.*;

/**
 * Rule V-2: Two items on the same slot with identical priority.
 */
public class PriorityCollisionRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-2";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        Map<Integer, Map<Integer, List<String>>> slotToPriorityToItems = new HashMap<>();

        for (MenuItem item : config.getItems().values()) {
            for (int slot : item.getAssignedSlots()) {
                slotToPriorityToItems
                        .computeIfAbsent(slot, k -> new HashMap<>())
                        .computeIfAbsent(item.getPriority(), k -> new ArrayList<>())
                        .add(item.getKey());
            }
        }

        for (Map.Entry<Integer, Map<Integer, List<String>>> slotEntry : slotToPriorityToItems.entrySet()) {
            int slot = slotEntry.getKey();
            for (Map.Entry<Integer, List<String>> prioEntry : slotEntry.getValue().entrySet()) {
                List<String> itemKeys = prioEntry.getValue();
                if (itemKeys.size() > 1) {
                    issues.add(ValidationIssue.error(
                            "V-2",
                            "Collision on slot " + slot + ": multiple items (" + String.join(", ", itemKeys)
                                    + ") have identical priority " + prioEntry.getKey(),
                            itemKeys.get(0),
                            slot,
                            "priority"
                    ));
                }
            }
        }
    }
}
