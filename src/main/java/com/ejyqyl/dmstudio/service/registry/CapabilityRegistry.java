package com.ejyqyl.dmstudio.service.registry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Registry of DeluxeMenus capabilities and Minecraft resources loaded from JSON descriptors.
 */
public final class CapabilityRegistry {

    private static final CapabilityRegistry INSTANCE = new CapabilityRegistry();

    private final Gson gson = new Gson();
    private final List<ActionDescriptor> actions = new ArrayList<>();
    private final List<RequirementDescriptor> requirements = new ArrayList<>();
    private final List<ItemOptionDescriptor> itemOptions = new ArrayList<>();
    private final List<EnchantmentDescriptor> enchantments = new ArrayList<>();
    private final List<String> sounds = new ArrayList<>();
    private final List<MaterialDescriptor> materials = new ArrayList<>();
    private final Map<String, MaterialDescriptor> materialMap = new HashMap<>();

    private CapabilityRegistry() {
        loadRegistries();
    }

    public static CapabilityRegistry getInstance() {
        return INSTANCE;
    }

    private void loadRegistries() {
        loadActions();
        loadRequirements();
        loadItemOptions();
        loadEnchantments();
        loadSounds();
        loadMaterials();
    }

    private void loadActions() {
        Type type = new TypeToken<List<ActionDescriptor>>() {}.getType();
        List<ActionDescriptor> list = loadJsonResource("/registry/actions.json", type);
        if (list != null) actions.addAll(list);
    }

    private void loadRequirements() {
        Type type = new TypeToken<List<RequirementDescriptor>>() {}.getType();
        List<RequirementDescriptor> list = loadJsonResource("/registry/requirements.json", type);
        if (list != null) requirements.addAll(list);
    }

    private void loadItemOptions() {
        Type type = new TypeToken<List<ItemOptionDescriptor>>() {}.getType();
        List<ItemOptionDescriptor> list = loadJsonResource("/registry/item-options.json", type);
        if (list != null) itemOptions.addAll(list);
    }

    private void loadEnchantments() {
        Type type = new TypeToken<List<EnchantmentDescriptor>>() {}.getType();
        List<EnchantmentDescriptor> list = loadJsonResource("/registry/enchantments.json", type);
        if (list != null) enchantments.addAll(list);
    }

    private void loadSounds() {
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = loadJsonResource("/registry/sounds-1.21.json", type);
        if (list != null) sounds.addAll(list);
    }

    private void loadMaterials() {
        Type type = new TypeToken<List<MaterialDescriptor>>() {}.getType();
        List<MaterialDescriptor> list = loadJsonResource("/registry/materials-1.21.json", type);
        if (list != null) {
            materials.addAll(list);
            for (MaterialDescriptor mat : list) {
                materialMap.put(mat.id().toUpperCase(Locale.ROOT), mat);
            }
        }
    }

    private <T> T loadJsonResource(String path, Type type) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("Registry resource not found: " + path);
                return null;
            }
            return gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
        } catch (Exception e) {
            System.err.println("Failed to load registry resource " + path + ": " + e.getMessage());
            return null;
        }
    }

    public List<ActionDescriptor> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<RequirementDescriptor> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public List<ItemOptionDescriptor> getItemOptions() {
        return Collections.unmodifiableList(itemOptions);
    }

    public List<EnchantmentDescriptor> getEnchantments() {
        return Collections.unmodifiableList(enchantments);
    }

    public List<String> getSounds() {
        return Collections.unmodifiableList(sounds);
    }

    public List<MaterialDescriptor> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public MaterialDescriptor getMaterial(String id) {
        if (id == null) return null;
        return materialMap.get(id.toUpperCase(Locale.ROOT));
    }

    public boolean isKnownMaterial(String id) {
        if (id == null) return false;
        return materialMap.containsKey(id.toUpperCase(Locale.ROOT));
    }
}
