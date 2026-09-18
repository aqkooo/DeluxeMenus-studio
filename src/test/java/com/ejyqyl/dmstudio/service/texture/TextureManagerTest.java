package com.ejyqyl.dmstudio.service.texture;

import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureManagerTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testCreateIconNodeFallback() {
        TextureManager manager = TextureManager.getInstance();
        assertNotNull(manager);

        Node stoneIcon = manager.createIconNode("STONE", 32);
        assertNotNull(stoneIcon);
        assertTrue(stoneIcon instanceof javafx.scene.image.ImageView, "STONE should be an ImageView, but was " + stoneIcon.getClass());

        Node glassPaneIcon = manager.createIconNode("GRAY_STAINED_GLASS_PANE", 32);
        assertNotNull(glassPaneIcon);
        assertTrue(glassPaneIcon instanceof javafx.scene.image.ImageView, "GRAY_STAINED_GLASS_PANE should be an ImageView");

        Node chestIcon = manager.createIconNode("CHEST", 32);
        assertNotNull(chestIcon);
        assertTrue(chestIcon instanceof javafx.scene.image.ImageView, "CHEST should be an ImageView");

        Node emptyIcon = manager.createIconNode("AIR", 32);
        assertNotNull(emptyIcon);

        Node customIcon = manager.createIconNode("itemsadder-ruby_sword", 32);
        assertNotNull(customIcon);

        Node clockIcon = manager.createIconNode("CLOCK", 32);
        assertTrue(clockIcon instanceof javafx.scene.image.ImageView, "CLOCK should be an ImageView");

        Node compassIcon = manager.createIconNode("COMPASS", 32);
        assertTrue(compassIcon instanceof javafx.scene.image.ImageView, "COMPASS should be an ImageView");

        Node swordIcon = manager.createIconNode("NETHERITE_SWORD", 32);
        assertTrue(swordIcon instanceof javafx.scene.image.ImageView, "NETHERITE_SWORD should be an ImageView");

        Node headIcon = manager.createIconNode("PLAYER_HEAD", 32);
        assertTrue(headIcon instanceof javafx.scene.image.ImageView, "PLAYER_HEAD should be an ImageView");

        Node expBottle = manager.createIconNode("EXP_BOTTLE", 32);
        assertTrue(expBottle instanceof javafx.scene.image.ImageView, "EXP_BOTTLE should be an ImageView");
    }
}
