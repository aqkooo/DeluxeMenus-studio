package com.ejyqyl.dmstudio.service.registry;

import java.util.List;

/**
 * Descriptor for a DeluxeMenus item option.
 */
public record ItemOptionDescriptor(
        String key,
        String type,
        boolean required,
        String defaultValue,
        String sinceVersion,
        String deprecatedSince,
        String group,
        String descriptionRu,
        List<String> values
) {
}
