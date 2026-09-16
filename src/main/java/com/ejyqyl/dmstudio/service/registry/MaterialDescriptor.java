package com.ejyqyl.dmstudio.service.registry;

/**
 * Descriptor for a Minecraft material with visual metadata.
 */
public record MaterialDescriptor(
        String id,
        String category,
        String abbr,
        String color
) {
}
