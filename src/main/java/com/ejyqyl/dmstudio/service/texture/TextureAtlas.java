package com.ejyqyl.dmstudio.service.texture;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

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
import java.util.concurrent.ConcurrentHashMap;

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

    public static TextureAtlas loadFromClasspath() {
        try {
            InputStream imgStream = TextureAtlas.class.getResourceAsStream("/textures/atlas.png");
            InputStream indexStream = TextureAtlas.class.getResourceAsStream("/textures/atlas-index.json");
            if (imgStream == null || indexStream == null) {
                return null;
            }

            Image img = new Image(imgStream, 0, 0, true, false);
            Type type = new TypeToken<Map<String, SpriteCoord>>() {}.getType();
            Map<String, SpriteCoord> coords;
            try (InputStreamReader reader = new InputStreamReader(indexStream, StandardCharsets.UTF_8)) {
                coords = new Gson().fromJson(reader, type);
            }

            return new TextureAtlas(img, coords);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasMaterial(String material) {
        return getViewport(material) != null;
    }

    public Rectangle2D getViewport(String material) {
        if (material == null) return null;
        String upper = material.toUpperCase(Locale.ROOT);

        // 1. Direct exact match
        Rectangle2D vp = viewports.get(upper);
        if (vp != null) return vp;

        // 2. Animated / Multi-state suffixes (_00, _standby, _0, _top, _side, _front)
        vp = viewports.get(upper + "_00");
        if (vp != null) return vp;

        vp = viewports.get(upper + "_STANDBY");
        if (vp != null) return vp;

        vp = viewports.get(upper + "_0");
        if (vp != null) return vp;

        vp = viewports.get(upper + "_TOP");
        if (vp != null) return vp;

        vp = viewports.get(upper + "_SIDE");
        if (vp != null) return vp;

        vp = viewports.get(upper + "_FRONT");
        if (vp != null) return vp;

        // 3. Stained glass pane alias to stained glass block
        if (upper.endsWith("_STAINED_GLASS_PANE")) {
            String glass = upper.substring(0, upper.length() - 5);
            vp = viewports.get(glass);
            if (vp != null) return vp;
        }
        if (upper.equals("GLASS_PANE")) {
            vp = viewports.get("GLASS");
            if (vp != null) return vp;
        }

        // 4. Common material plural/singular aliases & special names
        if (upper.equals("CARROTS") && viewports.containsKey("CARROT")) return viewports.get("CARROT");
        if (upper.equals("POTATOES") && viewports.containsKey("POTATO")) return viewports.get("POTATO");
        if (upper.equals("BEETROOTS") && viewports.containsKey("BEETROOT")) return viewports.get("BEETROOT");
        if (upper.equals("ENCHANTED_GOLDEN_APPLE") && viewports.containsKey("GOLDEN_APPLE")) return viewports.get("GOLDEN_APPLE");
        if (upper.equals("EYE_OF_ENDER") && viewports.containsKey("ENDER_EYE")) return viewports.get("ENDER_EYE");
        if (upper.equals("BARREL") && viewports.containsKey("BARREL_TOP")) return viewports.get("BARREL_TOP");
        if (upper.equals("EXP_BOTTLE") && viewports.containsKey("EXPERIENCE_BOTTLE")) return viewports.get("EXPERIENCE_BOTTLE");
        if ((upper.equals("SKULL") || upper.equals("SKELETON_SKULL") || upper.equals("ZOMBIE_HEAD")) && viewports.containsKey("PLAYER_HEAD")) return viewports.get("PLAYER_HEAD");
        if (upper.equals("TOTEM") && viewports.containsKey("TOTEM_OF_UNDYING")) return viewports.get("TOTEM_OF_UNDYING");

        return null;
    }

    public Image getAtlasImage() {
        return atlasImage;
    }

    public int getMaterialCount() {
        return viewports.size();
    }

    private final Map<String, Image> spriteCache = new ConcurrentHashMap<>();

    public Image getImage(String material) {
        if (atlasImage == null || material == null) return null;
        Rectangle2D vp = getViewport(material);
        if (vp == null) return null;

        String key = (int) vp.getMinX() + "_" + (int) vp.getMinY() + "_" + (int) vp.getWidth() + "_" + (int) vp.getHeight();
        return spriteCache.computeIfAbsent(key, k -> {
            try {
                PixelReader reader = atlasImage.getPixelReader();
                if (reader != null) {
                    return new WritableImage(reader, (int) vp.getMinX(), (int) vp.getMinY(), (int) vp.getWidth(), (int) vp.getHeight());
                }
            } catch (Exception ignored) {}
            return null;
        });
    }

    public ImageView createImageView(String material, double fitSize) {
        Image img = getImage(material);
        if (img == null) {
            Rectangle2D vp = getViewport(material);
            if (vp == null || atlasImage == null) return null;
            ImageView iv = new ImageView(atlasImage);
            iv.setSmooth(false);
            iv.setViewport(vp);
            iv.setFitWidth(fitSize);
            iv.setFitHeight(fitSize);
            return iv;
        }

        ImageView iv = new ImageView(img);
        iv.setSmooth(false);
        iv.setFitWidth(fitSize);
        iv.setFitHeight(fitSize);
        return iv;
    }

    public boolean updateImageView(ImageView iv, String material) {
        Image img = getImage(material);
        if (img != null) {
            iv.setViewport(null);
            iv.setImage(img);
            iv.setSmooth(false);
            return true;
        }
        Rectangle2D vp = getViewport(material);
        if (vp == null || atlasImage == null) return false;

        iv.setImage(atlasImage);
        iv.setSmooth(false);
        iv.setViewport(vp);
        return true;
    }
}
