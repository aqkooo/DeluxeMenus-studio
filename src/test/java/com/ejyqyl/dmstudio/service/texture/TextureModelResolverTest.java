package com.ejyqyl.dmstudio.service.texture;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextureModelResolverTest {

    @Test
    void testDirectLayer0Resolution() {
        String json = "{\"textures\":{\"layer0\":\"minecraft:item/iron_sword\"}}";
        Map<String, String> models = Map.of("item/iron_sword", json);

        String result = TextureModelResolver.resolveTextureFromModel("item/iron_sword", models, 0, new HashSet<>());
        assertEquals("minecraft:item/iron_sword", result);
    }

    @Test
    void testVariableReferenceResolution() {
        String json = "{\"textures\":{\"all\":\"minecraft:block/dirt\",\"layer0\":\"#all\"}}";
        Map<String, String> models = Map.of("block/dirt", json);

        String result = TextureModelResolver.resolveTextureFromModel("block/dirt", models, 0, new HashSet<>());
        assertEquals("minecraft:block/dirt", result);
    }

    @Test
    void testOverridesResolution() {
        String compassJson = "{\"overrides\":[{\"predicate\":{\"angle\":0.0},\"model\":\"minecraft:item/compass_00\"}]}";
        String compass00Json = "{\"textures\":{\"layer0\":\"minecraft:item/compass_00\"}}";

        Map<String, String> models = Map.of(
                "item/compass", compassJson,
                "item/compass_00", compass00Json
        );

        String result = TextureModelResolver.resolveTextureFromModel("item/compass", models, 0, new HashSet<>());
        assertEquals("minecraft:item/compass_00", result);
    }

    @Test
    void testParentHierarchyResolution() {
        String parentJson = "{\"textures\":{\"layer0\":\"minecraft:item/stick\"}}";
        String childJson = "{\"parent\":\"minecraft:item/parent_stick\"}";

        Map<String, String> models = Map.of(
                "item/parent_stick", parentJson,
                "item/child_stick", childJson
        );

        String result = TextureModelResolver.resolveTextureFromModel("item/child_stick", models, 0, new HashSet<>());
        assertEquals("minecraft:item/stick", result);
    }

    @Test
    void testCycleDetection() {
        String modelA = "{\"parent\":\"minecraft:item/model_b\"}";
        String modelB = "{\"parent\":\"minecraft:item/model_a\"}";

        Map<String, String> models = Map.of(
                "item/model_a", modelA,
                "item/model_b", modelB
        );

        String result = TextureModelResolver.resolveTextureFromModel("item/model_a", models, 0, new HashSet<>());
        assertNull(result, "Cycle must be caught without infinite loop");
    }

    @Test
    void testInvalidJsonOrMissingModel() {
        Map<String, String> models = new HashMap<>();
        models.put("item/bad_json", "not a json string");

        assertNull(TextureModelResolver.resolveTextureFromModel("item/bad_json", models, 0, new HashSet<>()));
        assertNull(TextureModelResolver.resolveTextureFromModel("item/non_existent", models, 0, new HashSet<>()));
        assertNull(TextureModelResolver.resolveTextureFromModel(null, models, 0, new HashSet<>()));
        assertNull(TextureModelResolver.resolveTextureFromModel("item/depth_test", models, 13, new HashSet<>()));
    }
}
