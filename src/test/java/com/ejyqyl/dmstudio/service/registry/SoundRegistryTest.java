package com.ejyqyl.dmstudio.service.registry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoundRegistryTest {

    @Test
    void testSoundRegistryValidation() {
        SoundRegistry registry = SoundRegistry.getInstance();
        assertThat(registry.getSounds()).isNotEmpty();
        assertThat(registry.isValidSound("UI_BUTTON_CLICK")).isTrue();
        assertThat(registry.isValidSound("ENTITY_PLAYER_LEVELUP")).isTrue();
        assertThat(registry.isValidSound("NON_EXISTENT_SOUND_123")).isFalse();
        assertThat(registry.isValidSound(null)).isFalse();
        assertThat(registry.isValidSound("")).isFalse();
    }
}
