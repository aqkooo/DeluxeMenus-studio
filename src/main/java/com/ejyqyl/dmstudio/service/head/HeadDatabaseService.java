package com.ejyqyl.dmstudio.service.head;

import com.ejyqyl.dmstudio.service.render.ItemIconProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton service providing offline HeadDatabase catalog, Base64 parsing, and head icon rendering (§HeadDatabase Support).
 */
public class HeadDatabaseService {

    private static final HeadDatabaseService INSTANCE = new HeadDatabaseService();

    private final List<HeadEntry> allHeads = new ArrayList<>();
    private final Map<String, HeadEntry> byId = new HashMap<>();
    private final Map<String, HeadEntry> byValue = new HashMap<>();
    private final Map<String, HeadEntry> byHash = new HashMap<>();
    private final Map<String, List<HeadEntry>> byCategory = new LinkedHashMap<>();
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private Image defaultHeadImage;

    private HeadDatabaseService() {
        loadCatalog();
    }

    public static HeadDatabaseService getInstance() {
        return INSTANCE;
    }

    private void loadCatalog() {
        try (InputStream is = getClass().getResourceAsStream("/heads/heads.json")) {
            if (is == null) return;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<List<HeadEntry>>() {}.getType();
                List<HeadEntry> loaded = new Gson().fromJson(reader, type);
                if (loaded != null) {
                    for (HeadEntry entry : loaded) {
                        allHeads.add(entry);
                        if (entry.id() != null) byId.put(entry.id().trim(), entry);
                        if (entry.value() != null) byValue.put(entry.value().trim(), entry);
                        if (entry.textureHash() != null) byHash.put(entry.textureHash().trim().toLowerCase(Locale.ROOT), entry);

                        String cat = entry.category() != null ? entry.category().trim().toUpperCase(Locale.ROOT) : "MISC";
                        byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(entry);
                    }
                }
            }
        } catch (Exception e) {
            // Log or fallback
        }

        HeadEntry steve = byId.get("1");
        if (steve != null) {
            defaultHeadImage = getHeadImage(steve);
        }
    }

    public List<HeadEntry> getAllHeads() {
        return Collections.unmodifiableList(allHeads);
    }

    public List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        cats.add("ALL");
        cats.addAll(byCategory.keySet());
        return cats;
    }

    public List<HeadEntry> getByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("ALL")) {
            return getAllHeads();
        }
        return Collections.unmodifiableList(byCategory.getOrDefault(category.toUpperCase(Locale.ROOT), Collections.emptyList()));
    }

    public List<HeadEntry> search(String query, boolean isRu) {
        if (query == null || query.isBlank()) return getAllHeads();
        String q = query.trim().toLowerCase(Locale.ROOT);
        return allHeads.stream().filter(h ->
                (h.id() != null && h.id().toLowerCase(Locale.ROOT).contains(q))
                        || (h.nameRu() != null && h.nameRu().toLowerCase(Locale.ROOT).contains(q))
                        || (h.nameEn() != null && h.nameEn().toLowerCase(Locale.ROOT).contains(q))
                        || (h.category() != null && h.category().toLowerCase(Locale.ROOT).contains(q))
        ).toList();
    }

    public Optional<HeadEntry> findById(String id) {
        if (id == null) return Optional.empty();
        String cleanId = id.toLowerCase(Locale.ROOT).startsWith("hdb-") ? id.substring(4).trim() : id.trim();
        return Optional.ofNullable(byId.get(cleanId));
    }

    public Optional<HeadEntry> findByValue(String value) {
        if (value == null) return Optional.empty();
        String cleanVal = value.startsWith("basehead-") ? value.substring(9).trim() : value.trim();
        HeadEntry match = byValue.get(cleanVal);
        if (match != null) return Optional.of(match);

        String hash = extractTextureHash(cleanVal);
        if (hash != null) {
            return Optional.ofNullable(byHash.get(hash.toLowerCase(Locale.ROOT)));
        }
        return Optional.empty();
    }

    public Image getHeadImage(HeadEntry entry) {
        if (entry == null) return defaultHeadImage;
        if (entry.iconPngBase64() == null || entry.iconPngBase64().isBlank()) {
            return defaultHeadImage;
        }

        return imageCache.computeIfAbsent(entry.id() != null ? entry.id() : entry.iconPngBase64(), k -> {
            try {
                byte[] bytes = Base64.getDecoder().decode(entry.iconPngBase64());
                return new Image(new ByteArrayInputStream(bytes), 0, 0, true, false);
            } catch (Exception e) {
                return defaultHeadImage;
            }
        });
    }

    public Node createHeadIcon(String rawMaterial, double size) {
        if (rawMaterial == null || rawMaterial.isBlank()) {
            return createImageView(defaultHeadImage, size);
        }

        String mat = rawMaterial.trim();
        String lower = mat.toLowerCase(Locale.ROOT);

        if (lower.startsWith("hdb-")) {
            String id = mat.substring(4).trim();
            HeadEntry entry = byId.get(id);
            if (entry != null) {
                return createImageView(getHeadImage(entry), size);
            }
        } else if (lower.startsWith("basehead-")) {
            String val = mat.substring(9).trim();
            Optional<HeadEntry> entryOpt = findByValue(val);
            if (entryOpt.isPresent()) {
                return createImageView(getHeadImage(entryOpt.get()), size);
            }
        } else if (lower.startsWith("head-")) {
            String player = mat.substring(5).trim();
            if (player.equalsIgnoreCase("alex")) {
                HeadEntry alex = byId.get("2");
                if (alex != null) return createImageView(getHeadImage(alex), size);
            } else {
                HeadEntry steve = byId.get("1");
                if (steve != null) return createImageView(getHeadImage(steve), size);
            }
        }

        if (defaultHeadImage != null) {
            return createImageView(defaultHeadImage, size);
        }
        return ItemIconProvider.createIcon("PLAYER_HEAD", size);
    }

    private ImageView createImageView(Image img, double size) {
        if (img == null) return null;
        ImageView iv = new ImageView(img);
        iv.setSmooth(false);
        if (size > 0) {
            iv.setFitWidth(size);
            iv.setFitHeight(size);
        }
        return iv;
    }

    public static String extractTextureHash(String base64OrUrl) {
        if (base64OrUrl == null || base64OrUrl.isBlank()) return null;
        String s = base64OrUrl.trim();
        if (s.contains("/texture/")) {
            return s.substring(s.lastIndexOf("/texture/") + 9).trim();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(s);
            String json = new String(decoded, StandardCharsets.UTF_8);
            int idx = json.indexOf("/texture/");
            if (idx != -1) {
                int start = idx + 9;
                int end = start;
                while (end < json.length() && Character.isLetterOrDigit(json.charAt(end))) {
                    end++;
                }
                return json.substring(start, end);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String urlToBaseheadValue(String url) {
        if (url == null || url.isBlank()) return "";
        String cleanUrl = url.trim();
        if (!cleanUrl.startsWith("http")) {
            cleanUrl = "http://textures.minecraft.net/texture/" + cleanUrl;
        }
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + cleanUrl + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
