package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-17: 'middle_click_commands' used when target Minecraft version is 1.18+.
 */
public class MiddleClickVersionRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-17";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        if (context.isMcAtLeast("1.18")) {
            for (MenuItem item : config.getItems().values()) {
                ClickHandler ch = item.getClickHandler();
                if (ch != null && !ch.getMiddleClickCommands().isEmpty()) {
                    issues.add(ValidationIssue.warning(
                            "V-17",
                            "Item '" + item.getKey() + "': 'middle_click_commands' is non-functional in Minecraft 1.18+ (removed by Mojang).",
                            item.getKey(),
                            item.getSlot(),
                            "middle_click_commands"
                    ));
                }
            }
        }
    }
}
