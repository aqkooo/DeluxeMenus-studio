package com.ejyqyl.dmstudio.service.texture;

import com.ejyqyl.dmstudio.service.render.ItemIconProvider;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Singleton service coordinating item textures from PNG atlas with procedural fallbacks (§Texture System).
 */
public class TextureManager {

    private static final TextureManager INSTANCE = new TextureManager();

    private final Path defaultTexturesDir;
    private TextureAtlas currentAtlas;

    private TextureManager() {
        String userHome = System.getProperty("user.home", ".");
        this.defaultTexturesDir = Path.of(userHome, ".deluxemenus-studio", "textures");
        reloadAtlas();
    }

    public static TextureManager getInstance() {
        return INSTANCE;
    }

    public Path getDefaultTexturesDir() {
        return defaultTexturesDir;
    }

    public void reloadAtlas() {
        this.currentAtlas = TextureAtlas.loadFromDirectory(defaultTexturesDir);
        if (currentAtlas == null) {
            java.nio.file.Path localItemDir = java.nio.file.Path.of("item");
            if (java.nio.file.Files.isDirectory(localItemDir)) {
                boolean built = new TextureExtractorService().buildAtlasFromDirectory(
                        localItemDir, defaultTexturesDir, msg -> System.out.println("[TextureManager] " + msg)
                );
                if (built) {
                    this.currentAtlas = TextureAtlas.loadFromDirectory(defaultTexturesDir);
                }
            }
        }
    }

    public boolean hasAtlas() {
        return currentAtlas != null && currentAtlas.getMaterialCount() > 0;
    }

    public TextureAtlas getCurrentAtlas() {
        return currentAtlas;
    }

    public Node createIconNode(String rawMaterial, double size) {
        if (rawMaterial == null || rawMaterial.isBlank() || rawMaterial.equalsIgnoreCase("AIR")) {
            return ItemIconProvider.createIcon("AIR", size);
        }

        String trimmed = rawMaterial.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Custom plugin prefixes offline
        if (lower.startsWith("itemsadder-") || lower.startsWith("oraxen-")
                || lower.startsWith("nexo-") || lower.startsWith("head-")
                || lower.startsWith("basehead-") || lower.startsWith("hdb-")
                || (trimmed.startsWith("%") && trimmed.endsWith("%"))) {
            return ItemIconProvider.createIcon(rawMaterial, size);
        }

        if (currentAtlas != null && currentAtlas.hasMaterial(trimmed)) {
            ImageView iv = currentAtlas.createImageView(trimmed, size);
            if (iv != null) return iv;
        }

        // Fallback to procedural vector badge
        return ItemIconProvider.createIcon(rawMaterial, size);
    }

    public boolean applyToImageView(ImageView iv, String rawMaterial) {
        if (iv == null || rawMaterial == null || rawMaterial.isBlank()) return false;
        String trimmed = rawMaterial.trim();

        if (currentAtlas != null && currentAtlas.hasMaterial(trimmed)) {
            return currentAtlas.updateImageView(iv, trimmed);
        }
        return false;
    }
}
