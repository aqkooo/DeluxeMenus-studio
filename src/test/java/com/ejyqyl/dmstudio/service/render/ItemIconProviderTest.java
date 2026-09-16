package com.ejyqyl.dmstudio.service.render;

import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemIconProviderTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started
        }
    }

    @Test
    void testCreateIconForVariousMaterials() {
        Node stoneNode = ItemIconProvider.createIcon("STONE", 32);
        assertThat(stoneNode).isNotNull();

        Node diamondNode = ItemIconProvider.createIcon("DIAMOND_SWORD", 32);
        assertThat(diamondNode).isNotNull();

        Node placeholderNode = ItemIconProvider.createIcon("%vault_eco_balance%", 32);
        assertThat(placeholderNode).isNotNull();

        Node hdbNode = ItemIconProvider.createIcon("hdb-12345", 32);
        assertThat(hdbNode).isNotNull();

        Node iaNode = ItemIconProvider.createIcon("itemsadder-ruby_sword", 32);
        assertThat(iaNode).isNotNull();

        Node oraxenNode = ItemIconProvider.createIcon("oraxen-custom", 32);
        assertThat(oraxenNode).isNotNull();

        Node nexoNode = ItemIconProvider.createIcon("nexo-gun", 32);
        assertThat(nexoNode).isNotNull();

        Node headNode = ItemIconProvider.createIcon("head-Notch", 32);
        assertThat(headNode).isNotNull();

        Node airNode = ItemIconProvider.createIcon("AIR", 32);
        assertThat(airNode).isNotNull();

        Node nullNode = ItemIconProvider.createIcon(null, 32);
        assertThat(nullNode).isNotNull();
    }
}
