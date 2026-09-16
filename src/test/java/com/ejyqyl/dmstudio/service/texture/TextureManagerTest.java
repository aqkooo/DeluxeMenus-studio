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

        Node emptyIcon = manager.createIconNode("AIR", 32);
        assertNotNull(emptyIcon);

        Node customIcon = manager.createIconNode("itemsadder-ruby_sword", 32);
        assertNotNull(customIcon);
    }
}
