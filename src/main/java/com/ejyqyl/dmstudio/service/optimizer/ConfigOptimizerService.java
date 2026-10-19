package com.ejyqyl.dmstudio.service.optimizer;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.i18n.I18nService;

import java.util.*;

/**
 * Service optimizing DeluxeMenus configurations: compresses slot lists into ranges and cleans redundant fields (§Utilities).
 */
public class ConfigOptimizerService {

    public record OptimizationResult(
            int mergedItemsCount,
            int compressedRangesCount,
            int strippedFieldsCount,
            List<String> changeLog
    ) {}

    /**
     * Optimizes the given menu configuration in-place.
     */
    public OptimizationResult optimize(MenuConfig config) {
        if (config == null || config.getItems() == null || config.getItems().isEmpty()) {
            return new OptimizationResult(0, 0, 0, List.of(I18nService.getInstance().get("tools.optimizer.no_items")));
        }

        List<String> log = new ArrayList<>();
        int mergedCount = 0;
        int compressedRanges = 0;
        int strippedFields = 0;

        // Step 1: Strip empty/redundant fields from all items
        for (MenuItem item : config.getItems().values()) {
            if (item.getAppearance() != null) {
                if (item.getAppearance().getDisplayName() != null && item.getAppearance().getDisplayName().isEmpty()) {
                    item.getAppearance().setDisplayName(null);
                    strippedFields++;
                }
                if (item.getAppearance().getLoreAppendMode() != null && item.getAppearance().getLoreAppendMode().isBlank()) {
                    item.getAppearance().setLoreAppendMode(null);
                    strippedFields++;
                }
            }
            if (item.getUnknownFields() != null && !item.getUnknownFields().isEmpty()) {
                int before = item.getUnknownFields().size();
                item.getUnknownFields().entrySet().removeIf(e -> e.getValue() == null || "".equals(e.getValue()));
                strippedFields += (before - item.getUnknownFields().size());
            }
        }

        // Step 2: Compress slots inside individual items
        for (MenuItem item : config.getItems().values()) {
            List<String> currentSlots = item.getSlots();
            if (currentSlots != null && currentSlots.size() > 1) {
                List<Integer> parsedSlots = parseSlots(item);
                List<String> compressed = compressSlotList(parsedSlots);
                if (compressed.size() < currentSlots.size()) {
                    compressedRanges += (currentSlots.size() - compressed.size());
                    item.setSlots(compressed);
                    item.setSlot(null);
                    log.add(I18nService.getInstance().get("tools.optimizer.compressed", item.getKey(), currentSlots.size(), compressed.size()));
                }
            }
        }

        // Step 3: Group identical items across the menu and merge them
        Map<String, List<MenuItem>> propertyGroups = new LinkedHashMap<>();
        for (MenuItem item : config.getItems().values()) {
            String signature = computeSignature(item);
            propertyGroups.computeIfAbsent(signature, k -> new ArrayList<>()).add(item);
        }

        Map<String, MenuItem> newItemsMap = new LinkedHashMap<>();
        for (List<MenuItem> group : propertyGroups.values()) {
            if (group.size() == 1) {
                MenuItem single = group.get(0);
                newItemsMap.put(single.getKey(), single);
            } else {
                // Merge multiple identical items into one
                MenuItem primary = group.get(0);
                Set<Integer> allSlots = new TreeSet<>();
                for (MenuItem item : group) {
                    allSlots.addAll(parseSlots(item));
                }

                List<String> compressed = compressSlotList(new ArrayList<>(allSlots));
                if (compressed.size() == 1 && !compressed.get(0).contains("-")) {
                    primary.setSlot(Integer.parseInt(compressed.get(0)));
                    primary.setSlots(null);
                } else {
                    primary.setSlots(compressed);
                    primary.setSlot(null);
                }

                newItemsMap.put(primary.getKey(), primary);
                mergedCount += (group.size() - 1);
                log.add(I18nService.getInstance().get("tools.optimizer.merged", group.size(), primary.getKey(), compressed));
            }
        }

        config.setItems(newItemsMap);

        return new OptimizationResult(mergedCount, compressedRanges, strippedFields, log);
    }

    private List<Integer> parseSlots(MenuItem item) {
        Set<Integer> set = new TreeSet<>();
        if (item.getSlot() != null) {
            set.add(item.getSlot());
        }
        if (item.getSlots() != null) {
            for (String s : item.getSlots()) {
                if (s.contains("-")) {
                    String[] parts = s.split("-");
                    try {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                            set.add(i);
                        }
                    } catch (Exception ignored) {}
                } else {
                    try {
                        set.add(Integer.parseInt(s.trim()));
                    } catch (Exception ignored) {}
                }
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> compressSlotList(List<Integer> slots) {
        if (slots == null || slots.isEmpty()) return List.of();

        List<Integer> sorted = new ArrayList<>(new TreeSet<>(slots));
        List<String> result = new ArrayList<>();

        int i = 0;
        while (i < sorted.size()) {
            int start = sorted.get(i);
            int end = start;

            while (i + 1 < sorted.size() && sorted.get(i + 1) == end + 1) {
                end = sorted.get(i + 1);
                i++;
            }

            if (start == end) {
                result.add(String.valueOf(start));
            } else if (end == start + 1) {
                result.add(String.valueOf(start));
                result.add(String.valueOf(end));
            } else {
                result.add(start + "-" + end);
            }
            i++;
        }

        return result;
    }

    private String computeSignature(MenuItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append(Objects.toString(item.getMaterial(), "")).append("|");
        sb.append(item.getPriority()).append("|");
        sb.append(item.getAmount()).append("|");
        sb.append(item.isUpdate()).append("|");
        if (item.getAppearance() != null) {
            sb.append(Objects.toString(item.getAppearance().getDisplayName(), "")).append("|");
            sb.append(Objects.toString(item.getAppearance().getLore(), "")).append("|");
            sb.append(Objects.toString(item.getAppearance().getEnchantments(), "")).append("|");
            sb.append(Objects.toString(item.getAppearance().getItemFlags(), "")).append("|");
        }
        if (item.getClickHandlers() != null) {
            sb.append(Objects.toString(item.getClickHandlers().getLeftClickCommands(), "")).append("|");
            sb.append(Objects.toString(item.getClickHandlers().getRightClickCommands(), "")).append("|");
        }
        if (item.getViewRequirement() != null) {
            sb.append(Objects.toString(item.getViewRequirement().getRequirements(), "")).append("|");
        }
        return sb.toString();
    }
}
