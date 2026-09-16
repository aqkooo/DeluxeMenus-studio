package com.ejyqyl.dmstudio.service.validation;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.model.validation.ValidationLevel;
import com.ejyqyl.dmstudio.service.validation.rules.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Validation engine running modular rules against a MenuConfig.
 */
public class ValidationEngine {

    private final List<ValidationRule> rules = new ArrayList<>();

    public ValidationEngine() {
        registerDefaultRules();
    }

    private void registerDefaultRules() {
        rules.add(new SlotRangeRule());
        rules.add(new PriorityCollisionRule());
        rules.add(new ViewRequirementMissingRule());
        rules.add(new UnknownMaterialRule());
        rules.add(new SlotAndSlotsRule());
        rules.add(new VersionOptionRule());
        rules.add(new UnknownSoundRule());
        rules.add(new ClickRequirementWithoutCommandsRule());
        rules.add(new ArmorTrimIncompleteRule());
        rules.add(new UpdateIntervalMissingRule());
        rules.add(new UnbalancedPlaceholderRule());
        rules.add(new MixedColorCodesRule());
        rules.add(new InvalidRegexRule());
        rules.add(new MinimumRequirementsRule());
        rules.add(new SizeForNonChestRule());
        rules.add(new MiddleClickVersionRule());
        rules.add(new CompositeValueFormatRule());
    }

    public void addRule(ValidationRule rule) {
        rules.add(rule);
    }

    public List<ValidationIssue> validate(MenuConfig config, ValidationContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (config == null) return issues;
        ValidationContext ctx = context != null ? context : new ValidationContext();

        for (ValidationRule rule : rules) {
            try {
                rule.validate(config, ctx, issues);
            } catch (Exception e) {
                issues.add(ValidationIssue.error("SYSTEM", "Error running validation rule " + rule.getCode() + ": " + e.getMessage(), null, null, null));
            }
        }

        issues.sort(Comparator.comparing(ValidationIssue::level, Comparator.comparingInt(Enum::ordinal))
                .thenComparing(issue -> issue.slot() != null ? issue.slot() : -1));

        return issues;
    }
}
