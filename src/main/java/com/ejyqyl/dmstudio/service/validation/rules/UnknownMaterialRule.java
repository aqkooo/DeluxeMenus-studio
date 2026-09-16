package com.ejyqyl.dmstudio.service.validation.rules;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.service.registry.MaterialRegistry;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationRule;

import java.util.List;

/**
 * Rule V-4: Unknown material for selected Minecraft version.
 */
public class UnknownMaterialRule implements ValidationRule {

    private final MaterialRegistry materialRegistry = MaterialRegistry.getInstance();

    @Override
    public String getCode() {
        return "V-4";
    }

    @Override
    public void validate(MenuConfig config, ValidationContext context, List<ValidationIssue> issues) {
        for (MenuItem item : config.getItems().values()) {
            String mat = item.getMaterial();
            if (mat == null || !materialRegistry.isValidMaterial(mat)) {
                issues.add(ValidationIssue.warning(
                        "V-4",
                        "Unknown material '" + mat + "'. Verify Minecraft server version or custom item prefix.",
                        item.getKey(),
                        item.getSlot(),
                        "material"
                ));
            }
        }
    }
}
