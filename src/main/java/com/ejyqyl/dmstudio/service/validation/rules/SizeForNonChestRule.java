package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-16: 'size' option specified for an inventory type other than CHEST.
 */
public class SizeForNonChestRule implements ValidationRule {

    @Override
    public String getCode() {
        return "V-16";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        if (config.getMenuType() != null && config.getMenuType() != InventoryType.CHEST) {
            if (config.getSize() != config.getMenuType().getDefaultSize()) {
                issues.add(ValidationIssue.error(
                        "V-16",
                        "Custom 'size' (" + config.getSize() + ") cannot be specified for inventory type "
                                + config.getMenuType() + ". Only CHEST supports custom sizes.",
                        null,
                        null,
                        "size"
                ));
            }
        }
    }
}
