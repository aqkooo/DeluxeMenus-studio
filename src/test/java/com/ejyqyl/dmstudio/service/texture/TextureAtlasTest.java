package com.ejyqyl.dmstudio.service.texture;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextureAtlasTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testAtlasCoordinates() {
        WritableImage mockImg = new WritableImage(64, 64);
        Map<String, TextureAtlas.SpriteCoord> coords = Map.of(
                "STONE", new TextureAtlas.SpriteCoord(0, 0, 16, 16),
                "DIAMOND_SWORD", new TextureAtlas.SpriteCoord(16, 0, 16, 16)
        );

        TextureAtlas atlas = new TextureAtlas(mockImg, coords);
        assertEquals(2, atlas.getMaterialCount());
        assertTrue(atlas.hasMaterial("stone"));
        assertTrue(atlas.hasMaterial("DIAMOND_SWORD"));
        assertFalse(atlas.hasMaterial("NETHER_STAR"));

        Rectangle2D vp = atlas.getViewport("stone");
        assertNotNull(vp);
        assertEquals(0, vp.getMinX());
        assertEquals(0, vp.getMinY());
        assertEquals(16, vp.getWidth());
        assertEquals(16, vp.getHeight());

        assertNotNull(atlas.createImageView("diamond_sword", 32));
    }
}
