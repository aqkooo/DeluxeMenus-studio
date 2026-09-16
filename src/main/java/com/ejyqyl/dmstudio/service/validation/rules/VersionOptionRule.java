package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-7: Option unavailable on target Minecraft version.
 */
public class VersionOptionRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-7";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            ItemAppearance app = item.getAppearance();
            if (app == null) continue;

            if (app.getItemModel() != null && !context.isMcAtLeast("1.21.2")) {
                issues.add(ValidationIssue.warning(
                        "V-7",
                        "Item '" + item.getKey() + "': 'item_model' is only supported in Minecraft 1.21.2+",
                        item.getKey(),
                        item.getSlot(),
                        "item_model"
                ));
            }

            if (Boolean.TRUE.equals(app.getEnchantmentGlintOverride()) && !context.isMcAtLeast("1.20.5")) {
                issues.add(ValidationIssue.warning(
                        "V-7",
                        "Item '" + item.getKey() + "': 'enchantment_glint_override' is only supported in Minecraft 1.20.5+",
                        item.getKey(),
                        item.getSlot(),
                        "enchantment_glint_override"
                ));
            }

            if (Boolean.TRUE.equals(app.getHideTooltip()) && !context.isMcAtLeast("1.20.5")) {
                issues.add(ValidationIssue.warning(
                        "V-7",
                        "Item '" + item.getKey() + "': 'hide_tooltip' is only supported in Minecraft 1.20.5+",
                        item.getKey(),
                        item.getSlot(),
                        "hide_tooltip"
                ));
            }
        }
    }
}
