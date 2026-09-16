package com.ejyqyl.dmstudio.service.texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TextureExtractorServiceTest {

    @Test
    void testExtractAndBuildAtlas(@TempDir Path tempDir) throws IOException {
        Path mockZip = tempDir.resolve("test-resource-pack.zip");
        Path outputDir = tempDir.resolve("extracted");

        // Create a mock zip with items and models
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(mockZip))) {
            // Add a texture
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 0xFF00FF00); // Green
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);

            zos.putNextEntry(new ZipEntry("assets/minecraft/textures/item/diamond_sword.png"));
            zos.write(baos.toByteArray());
            zos.closeEntry();

            // Add model pointing to layer0
            String jsonModel = "{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"minecraft:item/diamond_sword\"}}";
            zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/diamond_sword.json"));
            zos.write(jsonModel.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        TextureExtractorService service = new TextureExtractorService();
        boolean success = service.extractAndBuildAtlas(mockZip, outputDir, msg -> {});
        assertTrue(success);

        assertTrue(Files.exists(outputDir.resolve("atlas.png")));
        assertTrue(Files.exists(outputDir.resolve("atlas-index.json")));

        TextureAtlas atlas = TextureAtlas.loadFromDirectory(outputDir);
        assertNotNull(atlas);
        assertTrue(atlas.hasMaterial("DIAMOND_SWORD"));
    }

    @Test
    void testBuildAtlasFromDirectory(@TempDir Path tempDir) throws IOException {
        Path itemDir = tempDir.resolve("item");
        Path outputDir = tempDir.resolve("out_atlas");
        Files.createDirectories(itemDir);

        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFFFF0000); // Red
        ImageIO.write(img, "PNG", itemDir.resolve("apple.png").toFile());

        TextureExtractorService service = new TextureExtractorService();
        boolean success = service.buildAtlasFromDirectory(itemDir, outputDir, msg -> {});
        assertTrue(success);

        TextureAtlas atlas = TextureAtlas.loadFromDirectory(outputDir);
        assertNotNull(atlas);
        assertTrue(atlas.hasMaterial("APPLE"));
        assertEquals(1, atlas.getMaterialCount());
    }

    @Test
    void testFindInstalledMinecraftJars() {
        TextureExtractorService service = new TextureExtractorService();
        assertNotNull(service.findInstalledMinecraftJars());
    }
}
