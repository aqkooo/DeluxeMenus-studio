package com.ejyqyl.dmstudio.service.registry;

import java.util.List;

/**
 * Service for querying Minecraft sound names.
 */
public final class SoundRegistry {

    private static final SoundRegistry INSTANCE = new SoundRegistry();

    private final CapabilityRegistry capabilityRegistry = CapabilityRegistry.getInstance();

    private SoundRegistry() {}

    public static SoundRegistry getInstance() {
        return INSTANCE;
    }

    public List<String> getSounds() {
        return capabilityRegistry.getSounds();
    }

    public boolean isValidSound(String sound) {
        if (sound == null || sound.isBlank()) return false;
        String upper = sound.trim().toUpperCase();
        return capabilityRegistry.getSounds().contains(upper);
    }
}
