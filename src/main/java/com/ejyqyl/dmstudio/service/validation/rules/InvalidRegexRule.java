package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Rule V-14: Validates regular expressions in 'regex matches' requirements (§9.1).
 */
public class InvalidRegexRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-14";
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
        if (list == null || list.getRequirements() == null) return;
        for (Requirement req : list.getRequirements().values()) {
            if (req == null) continue;
            String type = req.getType();
            if (type != null && type.trim().equalsIgnoreCase("regex matches")) {
                Object regexObj = req.getField("regex");
                if (regexObj == null || regexObj.toString().isBlank()) {
                    issues.add(ValidationIssue.error(
                            getCode(),
                            contextName + ": 'regex matches' requirement is missing 'regex' pattern.",
                            itemKey,
                            slot,
                            "regex"
                    ));
                    continue;
                }
                String patternStr = regexObj.toString();
                try {
                    Pattern.compile(patternStr);
                } catch (PatternSyntaxException e) {
                    issues.add(ValidationIssue.error(
                            getCode(),
                            contextName + ": Invalid regex pattern '" + patternStr + "': " + e.getDescription(),
                            itemKey,
                            slot,
                            "regex"
                    ));
                }
            }
        }
    }
}
