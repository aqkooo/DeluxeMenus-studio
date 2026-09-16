package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-18: Malformed composite string values (enchantments: 'ID;LEVEL', potion_effects: 'EFFECT;DURATION;AMPLIFIER').
 */
public class CompositeValueFormatRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-18";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            ItemAppearance app = item.getAppearance();
            if (app == null) continue;

            for (String ench : app.getEnchantments()) {
                String[] parts = ench.split(";");
                if (parts.length != 2) {
                    issues.add(ValidationIssue.error(
                            "V-18",
                            "Item '" + item.getKey() + "': Malformed enchantment '" + ench + "'. Expected format: ENCHANT_NAME;LEVEL",
                            item.getKey(),
                            item.getSlot(),
                            "enchantments"
                    ));
                } else {
                    try {
                        Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        issues.add(ValidationIssue.error(
                                "V-18",
                                "Item '" + item.getKey() + "': Enchantment level in '" + ench + "' must be an integer.",
                                item.getKey(),
                                item.getSlot(),
                                "enchantments"
                        ));
                    }
                }
            }

            for (String pot : app.getPotionEffects()) {
                String[] parts = pot.split(";");
                if (parts.length < 2 || parts.length > 3) {
                    issues.add(ValidationIssue.error(
                            "V-18",
                            "Item '" + item.getKey() + "': Malformed potion effect '" + pot + "'. Expected format: EFFECT;DURATION;AMPLIFIER",
                            item.getKey(),
                            item.getSlot(),
                            "potion_effects"
                    ));
                }
            }
        }
    }
}
