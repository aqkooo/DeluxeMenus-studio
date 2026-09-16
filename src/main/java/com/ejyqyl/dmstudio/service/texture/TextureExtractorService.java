package com.ejyqyl.dmstudio.service.texture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts vanilla or resource-pack textures from a .jar/.zip file and builds a 16x16 PNG atlas (§Texture System).
 */
public class TextureExtractorService {

    private static final Map<String, Integer> TINT_MAP = Map.ofEntries(
            Map.entry("grass_block_top", 0x79c05a),
            Map.entry("oak_leaves", 0x59ae30),
            Map.entry("birch_leaves", 0x80a755),
            Map.entry("spruce_leaves", 0x619961),
            Map.entry("jungle_leaves", 0x59c93c),
            Map.entry("acacia_leaves", 0x48b518),
            Map.entry("dark_oak_leaves", 0x59ae30),
            Map.entry("mangrove_leaves", 0x6ea832),
            Map.entry("vine", 0x48b518),
            Map.entry("lily_pad", 0x208030)
    );

    public List<Path> findInstalledMinecraftJars() {
        List<Path> results = new ArrayList<>();
        String userHome = System.getProperty("user.home", "");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        Path versionsDir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            versionsDir = appData != null ? Path.of(appData, ".minecraft", "versions") : Path.of(userHome, "AppData", "Roaming", ".minecraft", "versions");
        } else if (os.contains("mac")) {
            versionsDir = Path.of(userHome, "Library", "Application Support", "minecraft", "versions");
        } else {
            versionsDir = Path.of(userHome, ".minecraft", "versions");
        }

        if (Files.isDirectory(versionsDir)) {
            try (var stream = Files.list(versionsDir)) {
                stream.filter(Files::isDirectory).forEach(verDir -> {
                    String name = verDir.getFileName().toString();
                    Path jar = verDir.resolve(name + ".jar");
                    if (Files.isRegularFile(jar)) {
                        results.add(jar);
                    }
                });
            } catch (IOException ignored) {}
        }

        results.sort((a, b) -> b.getFileName().toString().compareToIgnoreCase(a.getFileName().toString()));
        return results;
    }

    public boolean buildAtlasFromDirectory(Path sourceDir, Path targetDir, Consumer<String> statusLogger) {
        if (!Files.isDirectory(sourceDir)) return false;

        try {
            Map<String, BufferedImage> resolvedImages = new LinkedHashMap<>();
            try (var stream = Files.list(sourceDir)) {
                stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).forEach(p -> {
                    String fn = p.getFileName().toString();
                    String base = fn.substring(0, fn.length() - 4).toUpperCase(Locale.ROOT);
                    try {
                        BufferedImage img = ImageIO.read(p.toFile());
                        if (img != null) {
                            resolvedImages.put(base, ensure16x16(img));
                        }
                    } catch (Exception ignored) {}
                });
            }

            if (resolvedImages.isEmpty()) return false;

            // Register aliases for base animated materials
            Map<String, BufferedImage> aliases = new HashMap<>();
            for (Map.Entry<String, BufferedImage> entry : resolvedImages.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("_00")) {
                    String base = key.substring(0, key.length() - 3);
                    if (!resolvedImages.containsKey(base)) aliases.putIfAbsent(base, entry.getValue());
                } else if (key.endsWith("_STANDBY")) {
                    String base = key.substring(0, key.length() - 8);
                    if (!resolvedImages.containsKey(base)) aliases.putIfAbsent(base, entry.getValue());
                } else if (key.endsWith("_0")) {
                    String base = key.substring(0, key.length() - 2);
                    if (!resolvedImages.containsKey(base)) aliases.putIfAbsent(base, entry.getValue());
                }
            }

            int cols = 64;
            int total = resolvedImages.size();
            int rows = (total + cols - 1) / cols;
            int atlasWidth = cols * 16;
            int atlasHeight = Math.max(16, rows * 16);

            BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = atlas.createGraphics();

            Map<String, TextureAtlas.SpriteCoord> indexMap = new LinkedHashMap<>();
            int idx = 0;
            for (Map.Entry<String, BufferedImage> e : resolvedImages.entrySet()) {
                int col = idx % cols;
                int row = idx / cols;
                int x = col * 16;
                int y = row * 16;

                g2.drawImage(e.getValue(), x, y, null);
                indexMap.put(e.getKey(), new TextureAtlas.SpriteCoord(x, y, 16, 16));
                idx++;
            }
            g2.dispose();

            // Link aliases to the same sprite coordinates without extra space
            for (Map.Entry<String, BufferedImage> al : aliases.entrySet()) {
                String base = al.getKey();
                if (indexMap.containsKey(base + "_00")) {
                    indexMap.put(base, indexMap.get(base + "_00"));
                } else if (indexMap.containsKey(base + "_STANDBY")) {
                    indexMap.put(base, indexMap.get(base + "_STANDBY"));
                } else if (indexMap.containsKey(base + "_0")) {
                    indexMap.put(base, indexMap.get(base + "_0"));
                }
            }

            Files.createDirectories(targetDir);
            Path outAtlas = targetDir.resolve("atlas.png");
            Path outIndex = targetDir.resolve("atlas-index.json");

            ImageIO.write(atlas, "PNG", outAtlas.toFile());
            Files.writeString(outIndex, new Gson().toJson(indexMap), StandardCharsets.UTF_8);

            statusLogger.accept("Directory atlas built: " + indexMap.size() + " materials indexed in " + outAtlas);
            return true;
        } catch (Exception e) {
            statusLogger.accept("Directory atlas build failed: " + e.getMessage());
            return false;
        }
    }

    public boolean extractAndBuildAtlas(Path archivePath, Path targetDir, Consumer<String> statusLogger) {
        if (!Files.isRegularFile(archivePath)) return false;

        Map<String, byte[]> rawPngs = new HashMap<>();
        Map<String, String> allModels = new HashMap<>();
        List<String> itemModelNames = new ArrayList<>();

        try {
            statusLogger.accept("Reading archive: " + archivePath.getFileName());
            try (InputStream fis = Files.newInputStream(archivePath);
                 ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

                ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();

                    if (name.startsWith("assets/minecraft/textures/") && name.endsWith(".png")) {
                        String rel = name.substring("assets/minecraft/textures/".length());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                        }
                        rawPngs.put(rel, baos.toByteArray());
                    } else if (name.startsWith("assets/minecraft/models/") && name.endsWith(".json")) {
                        String modelPath = name.substring("assets/minecraft/models/".length(), name.length() - 5);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                        }
                        String jsonStr = baos.toString(StandardCharsets.UTF_8);
                        allModels.put(modelPath, jsonStr);
                        allModels.put("minecraft:" + modelPath, jsonStr);
                        if (modelPath.startsWith("item/")) {
                            itemModelNames.add(modelPath.substring("item/".length()));
                        }
                    }
                }
            }

            statusLogger.accept("Found " + rawPngs.size() + " textures and " + allModels.size() + " models");
            if (rawPngs.isEmpty()) return false;

            Map<String, BufferedImage> resolvedImages = new LinkedHashMap<>();

            // 1. Process via item models with recursive parent resolution
            for (String itemName : itemModelNames) {
                String targetTexture = resolveTextureFromModel("item/" + itemName, allModels, 0, new HashSet<>());
                if (targetTexture != null) {
                    byte[] data = findTextureBytes(rawPngs, targetTexture);
                    if (data != null) {
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                            BufferedImage img = ImageIO.read(bais);
                            if (img != null) {
                                String cleanTexName = targetTexture.contains("/")
                                        ? targetTexture.substring(targetTexture.lastIndexOf('/') + 1) : targetTexture;
                                if (TINT_MAP.containsKey(cleanTexName)) {
                                    applyTint(img, TINT_MAP.get(cleanTexName));
                                }
                                resolvedImages.put(itemName.toUpperCase(Locale.ROOT), ensure16x16(img));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 2. Direct fallback from item and block textures
            for (Map.Entry<String, byte[]> pe : rawPngs.entrySet()) {
                String path = pe.getKey();
                String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                if (!filename.endsWith(".png")) continue;
                String base = filename.substring(0, filename.length() - 4).toUpperCase(Locale.ROOT);

                if (!resolvedImages.containsKey(base)) {
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(pe.getValue())) {
                        BufferedImage img = ImageIO.read(bais);
                        if (img != null) {
                            String lower = base.toLowerCase(Locale.ROOT);
                            if (TINT_MAP.containsKey(lower)) {
                                applyTint(img, TINT_MAP.get(lower));
                            }
                            resolvedImages.put(base, ensure16x16(img));
                        }
                    } catch (Exception ignored) {}
                }
            }

            statusLogger.accept("Building atlas from " + resolvedImages.size() + " sprites...");
            if (resolvedImages.isEmpty()) return false;

            int cols = 64;
            int total = resolvedImages.size();
            int rows = (total + cols - 1) / cols;
            int atlasWidth = cols * 16;
            int atlasHeight = Math.max(16, rows * 16);

            BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = atlas.createGraphics();

            Map<String, TextureAtlas.SpriteCoord> indexMap = new LinkedHashMap<>();
            int idx = 0;
            for (Map.Entry<String, BufferedImage> e : resolvedImages.entrySet()) {
                int col = idx % cols;
                int row = idx / cols;
                int x = col * 16;
                int y = row * 16;

                g2.drawImage(e.getValue(), x, y, null);
                indexMap.put(e.getKey(), new TextureAtlas.SpriteCoord(x, y, 16, 16));
                idx++;
            }
            g2.dispose();

            Files.createDirectories(targetDir);
            Path outAtlas = targetDir.resolve("atlas.png");
            Path outIndex = targetDir.resolve("atlas-index.json");

            ImageIO.write(atlas, "PNG", outAtlas.toFile());
            Files.writeString(outIndex, new Gson().toJson(indexMap), StandardCharsets.UTF_8);

            statusLogger.accept("Atlas saved: " + outAtlas + " (" + resolvedImages.size() + " materials)");
            return true;
        } catch (Exception e) {
            statusLogger.accept("Extraction failed: " + e.getMessage());
            return false;
        }
    }

    public String resolveTextureFromModel(String modelPath, Map<String, String> allModels, int depth, Set<String> visited) {
        if (depth > 12 || modelPath == null || visited.contains(modelPath)) return null;
        visited.add(modelPath);

        String clean = modelPath.startsWith("minecraft:") ? modelPath.substring("minecraft:".length()) : modelPath;
        String jsonStr = allModels.get(clean);
        if (jsonStr == null) jsonStr = allModels.get("item/" + clean);
        if (jsonStr == null) jsonStr = allModels.get("block/" + clean);
        if (jsonStr == null) return null;

        try {
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
            Map<String, String> localTextures = new HashMap<>();

            if (root.has("textures")) {
                JsonObject texObj = root.getAsJsonObject("textures");
                for (String key : texObj.keySet()) {
                    if (texObj.get(key).isJsonPrimitive()) {
                        localTextures.put(key, texObj.get(key).getAsString());
                    }
                }
            }

            // Check priority keys in textures
            String candidate = null;
            for (String key : List.of("layer0", "all", "side", "top", "front", "end", "texture")) {
                if (localTextures.containsKey(key)) {
                    candidate = localTextures.get(key);
                    break;
                }
            }
            if (candidate == null && !localTextures.isEmpty()) {
                candidate = localTextures.values().iterator().next();
            }

            // Resolve local variable references like #texture or #all
            if (candidate != null && candidate.startsWith("#")) {
                String varName = candidate.substring(1);
                candidate = localTextures.get(varName);
            }

            if (candidate != null && !candidate.startsWith("#")) {
                return candidate;
            }

            // If not found, check overrides (e.g. clock, compass)
            if (root.has("overrides")) {
                JsonArray overrides = root.getAsJsonArray("overrides");
                if (overrides.size() > 0 && overrides.get(0).isJsonObject()) {
                    JsonObject firstOv = overrides.get(0).getAsJsonObject();
                    if (firstOv.has("model")) {
                        String ovModel = firstOv.get("model").getAsString();
                        String ovTex = resolveTextureFromModel(ovModel, allModels, depth + 1, visited);
                        if (ovTex != null) return ovTex;
                    }
                }
            }

            // Climb up the parent hierarchy with cycle detection
            if (root.has("parent")) {
                String parent = root.get("parent").getAsString();
                String parentTex = resolveTextureFromModel(parent, allModels, depth + 1, visited);
                if (parentTex != null) {
                    if (parentTex.startsWith("#")) {
                        String varName = parentTex.substring(1);
                        if (localTextures.containsKey(varName)) {
                            return localTextures.get(varName);
                        }
                    } else {
                        return parentTex;
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private byte[] findTextureBytes(Map<String, byte[]> rawPngs, String targetTexture) {
        String clean = targetTexture.startsWith("minecraft:") ? targetTexture.substring("minecraft:".length()) : targetTexture;
        String fullPath = clean + ".png";
        if (rawPngs.containsKey(fullPath)) return rawPngs.get(fullPath);

        String fn = clean.contains("/") ? clean.substring(clean.lastIndexOf('/') + 1) : clean;
        if (rawPngs.containsKey("item/" + fn + ".png")) return rawPngs.get("item/" + fn + ".png");
        if (rawPngs.containsKey("block/" + fn + ".png")) return rawPngs.get("block/" + fn + ".png");

        // Try state suffixes e.g. clock_00, compass_00, crossbow_standby
        if (rawPngs.containsKey("item/" + fn + "_00.png")) return rawPngs.get("item/" + fn + "_00.png");
        if (rawPngs.containsKey("item/" + fn + "_standby.png")) return rawPngs.get("item/" + fn + "_standby.png");
        if (rawPngs.containsKey("item/" + fn + "_0.png")) return rawPngs.get("item/" + fn + "_0.png");
        if (rawPngs.containsKey("block/" + fn + "_all.png")) return rawPngs.get("block/" + fn + "_all.png");
        if (rawPngs.containsKey("block/" + fn + "_top.png")) return rawPngs.get("block/" + fn + "_top.png");
        if (rawPngs.containsKey("block/" + fn + "_side.png")) return rawPngs.get("block/" + fn + "_side.png");

        return null;
    }

    private BufferedImage ensure16x16(BufferedImage src) {
        if (src.getWidth() == 16 && src.getHeight() == 16) return src;
        BufferedImage res = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = res.createGraphics();
        g.drawImage(src, 0, 0, 16, 16, 0, 0, 16, 16, null);
        g.dispose();
        return res;
    }

    private void applyTint(BufferedImage img, int tintRgb) {
        int tr = (tintRgb >> 16) & 0xFF;
        int tg = (tintRgb >> 8) & 0xFF;
        int tb = tintRgb & 0xFF;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgba = img.getRGB(x, y);
                int a = (rgba >> 24) & 0xFF;
                if (a == 0) continue;
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = rgba & 0xFF;

                r = (r * tr) / 255;
                g = (g * tg) / 255;
                b = (b * tb) / 255;

                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }
}
