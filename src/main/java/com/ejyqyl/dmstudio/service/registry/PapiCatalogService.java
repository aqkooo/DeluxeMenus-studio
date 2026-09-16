package com.ejyqyl.dmstudio.service.registry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Offline catalog service for PlaceholderAPI placeholders (§Ecosystem Integration).
 */
public class PapiCatalogService {

    public record PapiEntry(
            String category,
            String placeholder,
            String description_ru,
            String description_en,
            String example
    ) {
        public String getDescription(String lang) {
            return "ru".equalsIgnoreCase(lang) ? description_ru : description_en;
        }
    }

    private static final PapiCatalogService INSTANCE = new PapiCatalogService();

    public static PapiCatalogService getInstance() {
        return INSTANCE;
    }

    private final List<PapiEntry> entries = new ArrayList<>();
    private final Set<String> categories = new LinkedHashSet<>();

    private PapiCatalogService() {
        loadCatalog();
    }

    private void loadCatalog() {
        try (InputStream is = getClass().getResourceAsStream("/registry/papi-catalog.json")) {
            if (is != null) {
                Gson gson = new Gson();
                List<PapiEntry> list = gson.fromJson(
                        new InputStreamReader(is, StandardCharsets.UTF_8),
                        new TypeToken<List<PapiEntry>>() {}.getType()
                );
                if (list != null) {
                    entries.addAll(list);
                    for (PapiEntry entry : list) {
                        if (entry.category() != null) {
                            categories.add(entry.category());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Safe fallback if file missing
        }
    }

    public List<PapiEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public List<PapiEntry> search(String query, String category) {
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        return entries.stream()
                .filter(e -> category == null || category.equalsIgnoreCase("ALL") || category.equalsIgnoreCase(e.category()))
                .filter(e -> q.isEmpty()
                        || e.placeholder().toLowerCase(Locale.ROOT).contains(q)
                        || (e.description_ru() != null && e.description_ru().toLowerCase(Locale.ROOT).contains(q))
                        || (e.description_en() != null && e.description_en().toLowerCase(Locale.ROOT).contains(q)))
                .collect(Collectors.toList());
    }
}
