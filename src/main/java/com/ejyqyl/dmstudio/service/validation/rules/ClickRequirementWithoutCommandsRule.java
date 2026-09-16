package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-9: Click requirement specified without corresponding click commands section.
 */
public class ClickRequirementWithoutCommandsRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-9";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            ClickHandler ch = item.getClickHandler();
            if (ch == null) continue;

            check(item, "click", ch.getClickRequirement(), ch.getClickCommands(), issues);
            check(item, "left_click", ch.getLeftClickRequirement(), ch.getLeftClickCommands(), issues);
            check(item, "right_click", ch.getRightClickRequirement(), ch.getRightClickCommands(), issues);
            check(item, "shift_left_click", ch.getShiftLeftClickRequirement(), ch.getShiftLeftClickCommands(), issues);
            check(item, "shift_right_click", ch.getShiftRightClickRequirement(), ch.getShiftRightClickCommands(), issues);
            check(item, "middle_click", ch.getMiddleClickRequirement(), ch.getMiddleClickCommands(), issues);
        }
    }

    private void check(MenuItem item, String type, RequirementList req, List<Action> actions, List<ValidationIssue> issues) {
        if (req != null && !req.isEmpty() && (actions == null || actions.isEmpty())) {
            issues.add(ValidationIssue.error(
                    "V-9",
                    "Item '" + item.getKey() + "': " + type + "_requirement is set, but " + type + "_commands is empty. DeluxeMenus will ignore the requirement.",
                    item.getKey(),
                    item.getSlot(),
                    type + "_commands"
            ));
        }
    }
}
