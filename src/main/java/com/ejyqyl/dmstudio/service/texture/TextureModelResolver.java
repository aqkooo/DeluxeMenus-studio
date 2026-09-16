package com.ejyqyl.dmstudio.service.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

/**
 * Resolves Minecraft JSON model hierarchies to their primary texture path.
 */
public class TextureModelResolver {

    public static String resolveTextureFromModel(String modelPath, Map<String, String> allModels, int depth, Set<String> visited) {
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
}
