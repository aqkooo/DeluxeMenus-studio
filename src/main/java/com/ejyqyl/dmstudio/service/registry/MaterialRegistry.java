package com.ejyqyl.dmstudio.service.registry;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Service for checking and querying Minecraft materials and DeluxeMenus material prefixes.
 */
public final class MaterialRegistry {

    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "head-", "basehead-", "texture-", "hdb-", "itemsadder-",
            "oraxen-", "nexo-", "mmoitems-", "executableitems-",
            "executableblocks-", "simpleitemgenerator-", "placeholder-"
    );

    private static final Set<String> SPECIAL_MATERIALS = Set.of(
            "main_hand", "off_hand", "armor_helmet", "armor_chestplate",
            "armor_leggings", "armor_boots", "water_bottle", "air"
    );

    private final CapabilityRegistry capabilityRegistry = CapabilityRegistry.getInstance();

    private MaterialRegistry() {}

    public static MaterialRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if the material string is valid (either vanilla material, valid prefix, special token, or placeholder).
     */
    public boolean isValidMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Check placeholders
        if (trimmed.startsWith("%") && trimmed.endsWith("%") && trimmed.length() > 2) {
            return true;
        }

        // Check special tokens
        if (SPECIAL_MATERIALS.contains(lower)) {
            return true;
        }

        // Check prefixes (e.g. hdb-12345, itemsadder-custom_sword)
        for (String prefix : ALLOWED_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }

        // Check vanilla materials
        return capabilityRegistry.isKnownMaterial(trimmed);
    }

    public List<MaterialDescriptor> getMaterials() {
        return capabilityRegistry.getMaterials();
    }

    public MaterialDescriptor getMaterial(String id) {
        return capabilityRegistry.getMaterial(id);
    }
}
