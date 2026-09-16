package com.ejyqyl.dmstudio.service.registry;

import java.util.List;

/**
 * Descriptor for a DeluxeMenus requirement type.
 */
public record RequirementDescriptor(
        String id,
        String type,
        boolean allowInversion,
        String descriptionRu,
        String descriptionEn,
        List<RequirementFieldDescriptor> fields
) {
    public String getDescription(boolean russian) {
        return russian ? descriptionRu : descriptionEn;
    }
}
