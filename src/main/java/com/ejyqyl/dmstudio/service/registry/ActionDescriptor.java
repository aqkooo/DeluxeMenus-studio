package com.ejyqyl.dmstudio.service.registry;

/**
 * Descriptor for a DeluxeMenus action/command type.
 */
public record ActionDescriptor(
        String id,
        String prefix,
        String argumentHint,
        String descriptionRu,
        String descriptionEn,
        boolean supportsModifiers
) {
    public String getDescription(boolean russian) {
        return russian ? descriptionRu : descriptionEn;
    }
}
