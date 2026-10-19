package com.ejyqyl.dmstudio.service.migration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMaterialRegistryTest {

    @Test
    void testNumericWithColonData() {
        Optional<String> redWool = LegacyMaterialRegistry.findModernMaterial("35:14", null);
        assertThat(redWool).contains("RED_WOOL");

        Optional<String> grayPane = LegacyMaterialRegistry.findModernMaterial("160:7", null);
        assertThat(grayPane).contains("GRAY_STAINED_GLASS_PANE");

        Optional<String> redDye = LegacyMaterialRegistry.findModernMaterial("351:1", null);
        assertThat(redDye).contains("RED_DYE");

        Optional<String> playerHead = LegacyMaterialRegistry.findModernMaterial("397:3", null);
        assertThat(playerHead).contains("PLAYER_HEAD");
    }

    @Test
    void testNumericWithSeparateDataParam() {
        Optional<String> emerald = LegacyMaterialRegistry.findModernMaterial("388", null);
        assertThat(emerald).contains("EMERALD");

        Optional<String> limeWool = LegacyMaterialRegistry.findModernMaterial("35", 5);
        assertThat(limeWool).contains("LIME_WOOL");

        Optional<String> granite = LegacyMaterialRegistry.findModernMaterial("1", 1);
        assertThat(granite).contains("GRANITE");
    }

    @Test
    void testLegacyNamedMaterials() {
        Optional<String> crafting = LegacyMaterialRegistry.findModernMaterial("WORKBENCH", null);
        assertThat(crafting).contains("CRAFTING_TABLE");

        Optional<String> woodenSword = LegacyMaterialRegistry.findModernMaterial("WOOD_SWORD", null);
        assertThat(woodenSword).contains("WOODEN_SWORD");

        Optional<String> expBottle = LegacyMaterialRegistry.findModernMaterial("EXP_BOTTLE", null);
        assertThat(expBottle).contains("EXPERIENCE_BOTTLE");

        Optional<String> skullItem = LegacyMaterialRegistry.findModernMaterial("SKULL_ITEM", 3);
        assertThat(skullItem).contains("PLAYER_HEAD");
    }

    @Test
    void testModernOrUnknownMaterials() {
        Optional<String> modern = LegacyMaterialRegistry.findModernMaterial("NETHERITE_SWORD", null);
        assertThat(modern).isEmpty();

        Optional<String> unknown = LegacyMaterialRegistry.findModernMaterial("NON_EXISTING_ITEM", null);
        assertThat(unknown).isEmpty();
    }
}
