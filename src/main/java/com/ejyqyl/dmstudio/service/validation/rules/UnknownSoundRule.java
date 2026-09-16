package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.registry.SoundRegistry;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;
import java.util.Locale;

/**
 * Rule V-8: Validates that sound names in [sound] actions exist in SoundRegistry (§9.1).
 */
public class UnknownSoundRule implements ValidationRule {

    private final SoundRegistry soundRegistry = SoundRegistry.getInstance();

    @Override
    public String getCode() {
        return "V-8";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        // 1. Menu open actions
        validateActions(config.getOpenActions(), null, null, "open_commands", issues);

        // 2. Menu open requirement
        if (config.getOpenRequirement() != null) {
            validateRequirementList(config.getOpenRequirement(), null, null, "open_requirement", issues);
        }

        // 3. Item actions and requirements
        for (MenuItem item : config.getItems().values()) {
            Integer slot = item.getSlot();
            String key = item.getKey();

            if (item.getViewRequirement() != null) {
                validateRequirementList(item.getViewRequirement(), key, slot, "view_requirement", issues);
            }

            if (item.getClickHandler() != null) {
                for (Action action : item.getClickHandler().getAllActions()) {
                    checkSoundAction(action, key, slot, "click_commands", issues);
                }

                for (RequirementList reqList : item.getClickHandler().getAllRequirements()) {
                    validateRequirementList(reqList, key, slot, "click_requirement", issues);
                }
            }
        }
    }

    private void validateRequirementList(RequirementList list, String key, Integer slot, String context, List<ValidationIssue> issues) {
        if (list == null) return;
        validateActions(list.getDenyCommands(), key, slot, context + ".deny_commands", issues);
        validateActions(list.getSuccessCommands(), key, slot, context + ".success_commands", issues);

        for (Requirement req : list.getRequirements().values()) {
            if (req != null) {
                validateActions(req.getDenyCommands(), key, slot, context + ".deny_commands", issues);
                validateActions(req.getSuccessCommands(), key, slot, context + ".success_commands", issues);
            }
        }
    }

    private void validateActions(List<Action> actions, String key, Integer slot, String context, List<ValidationIssue> issues) {
        if (actions == null) return;
        for (Action action : actions) {
            checkSoundAction(action, key, slot, context, issues);
        }
    }

    private void checkSoundAction(Action action, String key, Integer slot, String context, List<ValidationIssue> issues) {
        if (action == null || action.getType() == null) return;
        String type = action.getType().trim().toLowerCase(Locale.ROOT);
        if (!type.equals("[sound]")) return;

        String arg = action.getArgument();
        if (arg == null || arg.isBlank()) {
            issues.add(ValidationIssue.warning(getCode(), "Empty sound specified in [sound] action", key, slot, context));
            return;
        }

        // Parse sound name before volume/pitch parameters or delay/chance
        String soundName = arg.trim().split("[,;\\s]+")[0].toUpperCase(Locale.ROOT);
        if (soundName.startsWith("%") && soundName.endsWith("%")) {
            return; // Placeholder, resolved at runtime
        }

        if (!soundRegistry.isValidSound(soundName)) {
            issues.add(ValidationIssue.warning(getCode(), "Unknown Minecraft sound '" + soundName + "'", key, slot, context));
        }
    }
}
