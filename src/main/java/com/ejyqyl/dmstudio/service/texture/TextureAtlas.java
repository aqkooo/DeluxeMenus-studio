package com.ejyqyl.dmstudio.service.texture;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages an uncompressed 16x16 item texture atlas and sprite viewports (§Texture System).
 */
public class TextureAtlas {

    public record SpriteCoord(int x, int y, int w, int h) {}

    private final Image atlasImage;
    private final Map<String, Rectangle2D> viewports;

    public TextureAtlas(Image atlasImage, Map<String, SpriteCoord> coords) {
        this.atlasImage = atlasImage;
        Map<String, Rectangle2D> map = new HashMap<>();
        if (coords != null) {
            for (Map.Entry<String, SpriteCoord> e : coords.entrySet()) {
                SpriteCoord sc = e.getValue();
                map.put(e.getKey().toUpperCase(Locale.ROOT), new Rectangle2D(sc.x(), sc.y(), sc.w(), sc.h()));
            }
        }
        this.viewports = Collections.unmodifiableMap(map);
    }

    public static TextureAtlas loadFromDirectory(Path dir) {
        try {
            Path imagePath = dir.resolve("atlas.png");
            Path indexPath = dir.resolve("atlas-index.json");
            if (!Files.exists(imagePath) || !Files.exists(indexPath)) {
                return null;
            }

            Image img;
            try (InputStream is = Files.newInputStream(imagePath)) {
                // smooth = false to avoid blur when scaling up
                img = new Image(is, 0, 0, true, false);
            }

            Type type = new TypeToken<Map<String, SpriteCoord>>() {}.getType();
            Map<String, SpriteCoord> coords;
            try (InputStream is = Files.newInputStream(indexPath);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                coords = new Gson().fromJson(reader, type);
            }

            return new TextureAtlas(img, coords);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasMaterial(String material) {
        if (material == null) return false;
        return viewports.containsKey(material.toUpperCase(Locale.ROOT));
    }

    public Rectangle2D getViewport(String material) {
        if (material == null) return null;
        return viewports.get(material.toUpperCase(Locale.ROOT));
    }

    public Image getAtlasImage() {
        return atlasImage;
    }

    public int getMaterialCount() {
        return viewports.size();
    }

    public ImageView createImageView(String material, double fitSize) {
        Rectangle2D vp = getViewport(material);
        if (vp == null || atlasImage == null) return null;

        ImageView iv = new ImageView(atlasImage);
        iv.setSmooth(false);
        iv.setViewport(vp);
        iv.setFitWidth(fitSize);
        iv.setFitHeight(fitSize);
        return iv;
    }

    public boolean updateImageView(ImageView iv, String material) {
        Rectangle2D vp = getViewport(material);
        if (vp == null || atlasImage == null) return false;

        iv.setImage(atlasImage);
        iv.setSmooth(false);
        iv.setViewport(vp);
        return true;
    }
}
