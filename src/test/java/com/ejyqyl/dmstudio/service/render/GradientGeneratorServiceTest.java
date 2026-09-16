package com.ejyqyl.dmstudio.service.render;

import com.ejyqyl.dmstudio.service.render.GradientGeneratorService.FormatMode;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GradientGeneratorServiceTest {

    @Test
    void testDeluxeMenusHexGradient() {
        String result = GradientGeneratorService.generateGradient(
                "VIP",
                Color.web("#FF0000"),
                null,
                Color.web("#0000FF"),
                FormatMode.DELUXE_MENUS_HEX,
                true, false, false, false
        );

        assertNotNull(result);
        assertTrue(result.contains("&#ff0000&lV"));
        assertTrue(result.contains("&#0000ff&lP"));
    }

    @Test
    void testMiniMessageGradient() {
        String result = GradientGeneratorService.generateGradient(
                "Shop",
                Color.web("#FF0000"),
                Color.web("#00FF00"),
                Color.web("#0000FF"),
                FormatMode.MINI_MESSAGE,
                true, true, false, false
        );

        assertNotNull(result);
        assertTrue(result.startsWith("<gradient:ff0000:00ff00:0000ff><b><i>Shop</i></b></gradient>"));
    }

    @Test
    void testSpigotLegacyHexGradient() {
        String result = GradientGeneratorService.generateGradient(
                "AB",
                Color.web("#FFFFFF"),
                null,
                Color.web("#000000"),
                FormatMode.SPIGOT_LEGACY_HEX,
                false, false, false, false
        );

        assertNotNull(result);
        assertTrue(result.contains("&x&f&f&f&f&f&fA"));
        assertTrue(result.contains("&x&0&0&0&0&0&0B"));
    }

    @Test
    void testEmptyString() {
        String result = GradientGeneratorService.generateGradient(
                "",
                Color.RED, null, Color.BLUE,
                FormatMode.DELUXE_MENUS_HEX,
                false, false, false, false
        );
        assertEquals("", result);
    }
}
