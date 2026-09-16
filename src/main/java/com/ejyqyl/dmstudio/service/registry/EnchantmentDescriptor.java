package com.ejyqyl.dmstudio.service.registry;

/**
 * Descriptor for a Minecraft enchantment.
 */
public record EnchantmentDescriptor(
        String id,
        int maxLevel,
        String type
) {
}
