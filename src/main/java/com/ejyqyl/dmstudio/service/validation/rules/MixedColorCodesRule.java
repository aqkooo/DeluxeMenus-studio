package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule V-13: Mixing MiniMessage tags (e.g. &lt;red&gt;) and legacy color codes (&amp;c) in the same string.
 */
public class MixedColorCodesRule implements ValidationRule {

    private static final Pattern LEGACY_PATTERN = Pattern.compile("&[0-9a-fk-orA-FK-OR]");
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<(?:red|blue|green|yellow|gold|bold|italic|white|gray|black)>");

    @Override
    public String getCode() {
        return "V-13";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            ItemAppearance app = item.getAppearance();
            if (app == null) continue;

            if (isMixed(app.getDisplayName())) {
                issues.add(ValidationIssue.warning(
                        "V-13",
                        "Item '" + item.getKey() + "': Mixed MiniMessage and legacy color codes in display_name.",
                        item.getKey(),
                        item.getSlot(),
                        "display_name"
                ));
            }

            for (String line : app.getLore()) {
                if (isMixed(line)) {
                    issues.add(ValidationIssue.warning(
                            "V-13",
                            "Item '" + item.getKey() + "': Mixed MiniMessage and legacy color codes in lore line: " + line,
                            item.getKey(),
                            item.getSlot(),
                            "lore"
                    ));
                }
            }
        }
    }

    private boolean isMixed(String text) {
        if (text == null) return false;
        return LEGACY_PATTERN.matcher(text).find() && MINIMESSAGE_PATTERN.matcher(text).find();
    }
}
