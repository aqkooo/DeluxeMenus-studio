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
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TextureExtractorServiceTest {

    @Test
    void testRecursiveParentModelResolution() {
        TextureExtractorService service = new TextureExtractorService();

        // Synthetic parent model defining layer0
        String parentJson = "{\"textures\":{\"layer0\":\"minecraft:item/custom_sword\"}}";

        // Synthetic child model with NO textures, only parent link
        String childJson = "{\"parent\":\"minecraft:item/custom_parent\"}";

        Map<String, String> models = Map.of(
                "item/custom_parent", parentJson,
                "item/custom_sword_child", childJson
        );

        String resolved = service.resolveTextureFromModel("item/custom_sword_child", models, 0, new HashSet<>());
        assertNotNull(resolved, "Should resolve layer0 from parent model");
        assertEquals("minecraft:item/custom_sword", resolved);
    }

    @Test
    void testExtractAndBuildAtlasWithParentHierarchy(@TempDir Path tempDir) throws IOException {
        Path mockZip = tempDir.resolve("test-resource-pack.zip");
        Path outputDir = tempDir.resolve("extracted");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(mockZip))) {
            // Add texture
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 0xFF00FF00);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);

            zos.putNextEntry(new ZipEntry("assets/minecraft/textures/item/diamond_pickaxe.png"));
            zos.write(baos.toByteArray());
            zos.closeEntry();

            // Parent model has layer0
            String parentModel = "{\"textures\":{\"layer0\":\"minecraft:item/diamond_pickaxe\"}}";
            zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/handheld_parent.json"));
            zos.write(parentModel.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Child model only links to parent
            String childModel = "{\"parent\":\"minecraft:item/handheld_parent\"}";
            zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/diamond_pickaxe.json"));
            zos.write(childModel.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        TextureExtractorService service = new TextureExtractorService();
        boolean success = service.extractAndBuildAtlas(mockZip, outputDir, msg -> {});
        assertTrue(success);

        TextureAtlas atlas = TextureAtlas.loadFromDirectory(outputDir);
        assertNotNull(atlas);
        assertTrue(atlas.hasMaterial("DIAMOND_PICKAXE"), "Child model must resolve via parent hierarchy");
    }

    @Test
    void testBuildAtlasFromDirectoryWithAliases(@TempDir Path tempDir) throws IOException {
        Path itemDir = tempDir.resolve("item");
        Path outputDir = tempDir.resolve("out_atlas");
        Files.createDirectories(itemDir);

        // Add clock_00.png and crossbow_standby.png
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFFFF0000);
        ImageIO.write(img, "PNG", itemDir.resolve("clock_00.png").toFile());
        ImageIO.write(img, "PNG", itemDir.resolve("crossbow_standby.png").toFile());

        TextureExtractorService service = new TextureExtractorService();
        boolean success = service.buildAtlasFromDirectory(itemDir, outputDir, msg -> {});
        assertTrue(success);

        TextureAtlas atlas = TextureAtlas.loadFromDirectory(outputDir);
        assertNotNull(atlas);
        assertTrue(atlas.hasMaterial("CLOCK"), "CLOCK must be mapped via clock_00 alias");
        assertTrue(atlas.hasMaterial("CROSSBOW"), "CROSSBOW must be mapped via crossbow_standby alias");
    }

    @Test
    void testFindInstalledMinecraftJars() {
        TextureExtractorService service = new TextureExtractorService();
        assertNotNull(service.findInstalledMinecraftJars());
    }
}
