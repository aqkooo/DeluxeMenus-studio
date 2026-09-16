package com.ejyqyl.dmstudio.service.preset;

import com.ejyqyl.dmstudio.model.MenuItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing built-in and user-defined preset components (§Preset Library).
 */
public class ComponentLibraryService {

    public record MenuComponent(
            String id,
            String category,
            String name_ru,
            String name_en,
            String description_ru,
            String description_en,
            MenuItem item,
            boolean isCustom
    ) {
        public String getName(String lang) {
            return "ru".equalsIgnoreCase(lang) ? name_ru : name_en;
        }

        public String getDescription(String lang) {
            return "ru".equalsIgnoreCase(lang) ? description_ru : description_en;
        }
    }

    private static final ComponentLibraryService INSTANCE = new ComponentLibraryService();

    public static ComponentLibraryService getInstance() {
        return INSTANCE;
    }

    private final List<MenuComponent> components = new ArrayList<>();
    private final Path userPresetsDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ComponentLibraryService() {
        userPresetsDir = Paths.get(System.getProperty("user.home"), ".deluxemenus-studio", "presets");
        reload();
    }

    public synchronized void reload() {
        components.clear();
        loadBuiltIn();
        loadUserPresets();
    }

    private void loadBuiltIn() {
        try (InputStream is = getClass().getResourceAsStream("/presets/components.json")) {
            if (is != null) {
                List<Map<String, Object>> list = gson.fromJson(
                        new InputStreamReader(is, StandardCharsets.UTF_8),
                        new TypeToken<List<Map<String, Object>>>() {}.getType()
                );
                if (list != null) {
                    for (Map<String, Object> map : list) {
                        String id = (String) map.get("id");
                        String cat = (String) map.get("category");
                        String nRu = (String) map.get("name_ru");
                        String nEn = (String) map.get("name_en");
                        String dRu = (String) map.get("description_ru");
                        String dEn = (String) map.get("description_en");
                        Object itemRaw = map.get("item");
                        MenuItem item = gson.fromJson(gson.toJsonTree(itemRaw), MenuItem.class);
                        components.add(new MenuComponent(id, cat, nRu, nEn, dRu, dEn, item, false));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadUserPresets() {
        if (!Files.exists(userPresetsDir)) return;
        try (var stream = Files.list(userPresetsDir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    MenuComponent c = gson.fromJson(r, MenuComponent.class);
                    if (c != null) {
                        components.add(new MenuComponent(c.id(), c.category(), c.name_ru(), c.name_en(),
                                c.description_ru(), c.description_en(), c.item(), true));
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public synchronized List<MenuComponent> getComponents(String category) {
        if (category == null || category.equalsIgnoreCase("ALL")) {
            return new ArrayList<>(components);
        }
        return components.stream()
                .filter(c -> category.equalsIgnoreCase(c.category()))
                .collect(Collectors.toList());
    }

    public synchronized Set<String> getCategories() {
        return components.stream()
                .map(MenuComponent::category)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public synchronized void saveUserPreset(String name, String category, MenuItem item) throws IOException {
        Files.createDirectories(userPresetsDir);
        String id = "custom_" + UUID.randomUUID().toString().substring(0, 8);
        MenuComponent comp = new MenuComponent(id, category, name, name, "", "", item.deepCopy(), true);
        Path target = userPresetsDir.resolve(id + ".json");
        try (Writer w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            gson.toJson(comp, w);
        }
        components.add(comp);
    }
}
