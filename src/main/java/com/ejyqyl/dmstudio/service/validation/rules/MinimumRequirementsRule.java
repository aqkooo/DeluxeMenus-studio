package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-15: 'minimum_requirements' is greater than the total number of requirements in the block.
 */
public class MinimumRequirementsRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-15";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        if (config.getOpenRequirement() != null) {
            checkRequirementList("Menu open_requirement", null, null, config.getOpenRequirement(), issues);
        }

        for (MenuItem item : config.getItems().values()) {
            if (item.getViewRequirement() != null) {
                checkRequirementList("Item view_requirement", item.getKey(), item.getSlot(), item.getViewRequirement(), issues);
            }
            ClickHandler ch = item.getClickHandler();
            if (ch != null) {
                checkRequirementList("Item click_requirement", item.getKey(), item.getSlot(), ch.getClickRequirement(), issues);
                checkRequirementList("Item left_click_requirement", item.getKey(), item.getSlot(), ch.getLeftClickRequirement(), issues);
                checkRequirementList("Item right_click_requirement", item.getKey(), item.getSlot(), ch.getRightClickRequirement(), issues);
                checkRequirementList("Item shift_left_click_requirement", item.getKey(), item.getSlot(), ch.getShiftLeftClickRequirement(), issues);
                checkRequirementList("Item shift_right_click_requirement", item.getKey(), item.getSlot(), ch.getShiftRightClickRequirement(), issues);
                checkRequirementList("Item middle_click_requirement", item.getKey(), item.getSlot(), ch.getMiddleClickRequirement(), issues);
            }
        }
    }

    private void checkRequirementList(String contextName, String itemKey, Integer slot, RequirementList list, List<ValidationIssue> issues) {
        if (list == null) return;
        Integer min = list.getMinimumRequirements();
        if (min != null && min > list.getRequirements().size()) {
            issues.add(ValidationIssue.error(
                    "V-15",
                    contextName + ": minimum_requirements (" + min + ") exceeds total requirements count (" + list.getRequirements().size() + ").",
                    itemKey,
                    slot,
                    "minimum_requirements"
            ));
        }
    }
}
