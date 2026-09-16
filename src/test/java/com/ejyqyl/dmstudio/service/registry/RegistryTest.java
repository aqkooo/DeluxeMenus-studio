package com.ejyqyl.dmstudio.service.registry;

import com.ejyqyl.dmstudio.service.i18n.I18nService;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryTest {

    @Test
    void testCapabilityRegistryLoadsResources() {
        CapabilityRegistry registry = CapabilityRegistry.getInstance();

        assertThat(registry.getActions()).isNotEmpty();
        assertThat(registry.getRequirements()).isNotEmpty();
        assertThat(registry.getItemOptions()).isNotEmpty();
        assertThat(registry.getEnchantments()).isNotEmpty();
        assertThat(registry.getSounds()).isNotEmpty();
        assertThat(registry.getMaterials()).isNotEmpty();

        assertThat(registry.isKnownMaterial("DIAMOND_SWORD")).isTrue();
        assertThat(registry.isKnownMaterial("GRAY_STAINED_GLASS_PANE")).isTrue();
        assertThat(registry.getMaterial("DIAMOND_SWORD")).isNotNull();
        assertThat(registry.getMaterial("DIAMOND_SWORD").color()).isEqualTo("#4dedf0");
    }

    @Test
    void testMaterialRegistryPrefixes() {
        MaterialRegistry registry = MaterialRegistry.getInstance();

        assertThat(registry.isValidMaterial("DIAMOND")).isTrue();
        assertThat(registry.isValidMaterial("hdb-12345")).isTrue();
        assertThat(registry.isValidMaterial("head-Steve")).isTrue();
        assertThat(registry.isValidMaterial("itemsadder-custom_sword")).isTrue();
        assertThat(registry.isValidMaterial("placeholder-%custom_mat%")).isTrue();
        assertThat(registry.isValidMaterial("%some_placeholder%")).isTrue();
        assertThat(registry.isValidMaterial("armor_helmet")).isTrue();
        assertThat(registry.isValidMaterial("water_bottle")).isTrue();
        assertThat(registry.isValidMaterial("NON_EXISTENT_MATERIAL_XYZ")).isFalse();
    }

    @Test
    void testI18nService() {
        I18nService i18n = I18nService.getInstance();
        i18n.setLocale(Locale.of("ru", "RU"));
        assertThat(i18n.get("app.title")).isEqualTo("DeluxeMenus Studio");
        assertThat(i18n.get("menu.file")).isEqualTo("Файл");

        i18n.setLocale(Locale.ENGLISH);
        assertThat(i18n.get("menu.file")).isEqualTo("File");
    }
}
