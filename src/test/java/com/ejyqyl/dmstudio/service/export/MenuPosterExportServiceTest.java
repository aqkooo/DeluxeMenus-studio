package com.ejyqyl.dmstudio.service.export;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class MenuPosterExportServiceTest {

    @Test
    void testExportPoster(@TempDir Path tempDir) throws IOException {
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("&a&lТестовый Магазин");
        config.setSize(27);

        MenuItem item1 = new MenuItem("diamond_sword", "DIAMOND_SWORD");
        item1.setSlot(13);
        item1.getAppearance().setDisplayName("&bМеч Героя");
        config.getItems().put("diamond_sword", item1);

        File outputFile = tempDir.resolve("test_poster.png").toFile();
        MenuPosterExportService.exportPoster(config, outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        BufferedImage image = ImageIO.read(outputFile);
        assertNotNull(image);
        assertTrue(image.getWidth() > 300);
        assertTrue(image.getHeight() > 200);
    }
}
