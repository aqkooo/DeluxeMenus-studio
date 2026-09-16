package com.ejyqyl.dmstudio.service.registry;

import java.util.List;

/**
 * Descriptor for a requirement field.
 */
public record RequirementFieldDescriptor(
        String name,
        String type,
        String labelRu,
        String labelEn,
        boolean required
) {
    public String getLabel(boolean russian) {
        return russian ? labelRu : labelEn;
    }
}
