package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for importing and reverse-engineering DeluxeMenus YAML configurations (§6.4).
 */
public class YamlImportService {

    /**
     * Alias for {@link com.ejyqyl.dmstudio.service.yaml.ImportResult} for backwards compatibility.
     */
    public static class ImportResult extends com.ejyqyl.dmstudio.service.yaml.ImportResult {
        public ImportResult(MenuConfig config) {
            super(config);
        }

        public ImportResult(String errorMessage, int errorLine, int errorColumn) {
            super(errorMessage, errorLine, errorColumn);
        }
    }

    public ImportResult importFromFile(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return importFromString(content);
        } catch (IOException e) {
            return new ImportResult("Failed to read file: " + e.getMessage(), -1, -1);
        }
    }

    @SuppressWarnings("unchecked")
    public ImportResult importFromString(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return new ImportResult("Empty configuration", -1, -1);
        }

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(100);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

        Map<String, Object> root;
        try {
            Object loaded = yaml.load(yamlContent);
            if (!(loaded instanceof Map)) {
                return new ImportResult("Root element must be a YAML mapping", -1, -1);
            }
            root = (Map<String, Object>) loaded;
        } catch (YAMLException e) {
            int line = -1;
            int col = -1;
            if (e.getMessage() != null && e.getMessage().contains("line ")) {
                try {
                    String msg = e.getMessage();
                    int lIdx = msg.indexOf("line ");
                    int comma = msg.indexOf(",", lIdx);
                    line = Integer.parseInt(msg.substring(lIdx + 5, comma != -1 ? comma : lIdx + 10).trim());
                } catch (Exception ignored) {}
            }
            return new ImportResult("YAML parsing error: " + e.getMessage(), line, col);
        }

        // Support for gui_menus nesting
        if (root.containsKey("gui_menus") && root.get("gui_menus") instanceof Map<?, ?> guiMenus) {
            if (!guiMenus.isEmpty()) {
                Map.Entry<?, ?> first = guiMenus.entrySet().iterator().next();
                if (first.getValue() instanceof Map<?, ?> subMenu) {
                    root = (Map<String, Object>) subMenu;
                }
            }
        }

        MenuConfig config = new MenuConfig();
        ImportResult result = new ImportResult(config);

        parseMenuConfig(root, config, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void parseMenuConfig(Map<String, Object> map, MenuConfig config, ImportResult result) {
        Set<String> knownKeys = new HashSet<>(Set.of(
                "menu_title", "open_command", "register_command", "menu_type",
                "size", "update_interval", "open_requirement", "open_commands", "items"
        ));

        if (map.containsKey("menu_title")) {
            config.setMenuTitle(String.valueOf(map.get("menu_title")));
        }

        if (map.containsKey("open_command")) {
            Object val = map.get("open_command");
            if (val instanceof List<?> list) {
                for (Object o : list) config.getOpenCommands().add(String.valueOf(o));
            } else if (val != null) {
                config.getOpenCommands().add(String.valueOf(val));
            }
        }

        if (map.containsKey("register_command")) {
            config.setRegisterCommand(Boolean.parseBoolean(String.valueOf(map.get("register_command"))));
        }

        if (map.containsKey("menu_type")) {
            String typeStr = String.valueOf(map.get("menu_type")).toUpperCase();
            try {
                config.setMenuType(InventoryType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                result.getWarnings().add("Unknown menu_type: " + typeStr + ", defaulting to CHEST");
                config.setMenuType(InventoryType.CHEST);
            }
        }

        if (map.containsKey("size")) {
            try {
                config.setSize(Integer.parseInt(String.valueOf(map.get("size"))));
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("update_interval")) {
            try {
                config.setUpdateInterval(Integer.parseInt(String.valueOf(map.get("update_interval"))));
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("open_requirement") && map.get("open_requirement") instanceof Map<?, ?> reqMap) {
            config.setOpenRequirement(YamlRequirementParser.parseRequirementList((Map<String, Object>) reqMap));
        }

        if (map.containsKey("open_commands") && map.get("open_commands") instanceof List<?> list) {
            for (Object o : list) {
                config.getOpenActions().add(Action.parse(String.valueOf(o)));
            }
        }

        if (map.containsKey("items") && map.get("items") instanceof Map<?, ?> itemsMap) {
            for (Map.Entry<?, ?> entry : itemsMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> itemMap) {
                    MenuItem item = parseMenuItem(key, (Map<String, Object>) itemMap, result);
                    config.getItems().put(key, item);
                }
            }
        }

        // Unknown fields preserved for round-trip!
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!knownKeys.contains(entry.getKey())) {
                config.getUnknownFields().put(entry.getKey(), entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private MenuItem parseMenuItem(String key, Map<String, Object> map, ImportResult result) {
        Set<String> known = new HashSet<>(Set.of(
                "material", "slot", "slots", "priority", "amount", "dynamic_amount",
                "damage", "data", "update", "display_name", "lore", "lore_append_mode",
                "item_flags", "enchantments", "unbreakable", "enchantment_glint_override",
                "hide_tooltip", "rarity", "model_data", "model_data_component", "item_model",
                "tooltip_style", "rgb", "potion_effects", "banner_meta", "base_color",
                "trim_material", "trim_pattern", "light_level", "view_requirement",
                "hide_enchantments", "hide_attributes", "hide_unbreakable"
        ));
        for (String prefix : List.of("click", "left_click", "right_click", "shift_left_click", "shift_right_click", "middle_click")) {
            known.add(prefix + "_commands");
            known.add(prefix + "_requirement");
        }

        MenuItem item = new MenuItem(key, map.containsKey("material") ? String.valueOf(map.get("material")) : "STONE");

        if (map.containsKey("slot")) {
            try {
                item.setSlot(Integer.parseInt(String.valueOf(map.get("slot"))));
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("slots") && map.get("slots") instanceof List<?> slotsList) {
            for (Object s : slotsList) item.getSlots().add(String.valueOf(s));
        }

        if (map.containsKey("priority")) {
            try {
                item.setPriority(Integer.parseInt(String.valueOf(map.get("priority"))));
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("amount")) {
            try {
                item.setAmount(Integer.parseInt(String.valueOf(map.get("amount"))));
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("dynamic_amount")) {
            item.setDynamicAmount(String.valueOf(map.get("dynamic_amount")));
        }

        // Handle deprecated 'data' -> 'damage' (§0.1, §2.3)
        if (map.containsKey("damage")) {
            try {
                item.setDamage(Integer.parseInt(String.valueOf(map.get("damage"))));
            } catch (NumberFormatException ignored) {}
        } else if (map.containsKey("data")) {
            try {
                item.setDamage(Integer.parseInt(String.valueOf(map.get("data"))));
                result.getReplacementsApplied().add("Item '" + key + "': replaced deprecated 'data' with 'damage'");
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("update")) {
            item.setUpdate(Boolean.parseBoolean(String.valueOf(map.get("update"))));
        }



        ItemAppearance app = item.getAppearance();
        if (map.containsKey("display_name")) {
            app.setDisplayName(String.valueOf(map.get("display_name")));
        }
        if (map.containsKey("lore") && map.get("lore") instanceof List<?> loreList) {
            for (Object l : loreList) app.getLore().add(String.valueOf(l));
        }
        if (map.containsKey("lore_append_mode")) {
            app.setLoreAppendMode(String.valueOf(map.get("lore_append_mode")));
        }
        if (map.containsKey("item_flags") && map.get("item_flags") instanceof List<?> flagList) {
            for (Object f : flagList) app.getItemFlags().add(String.valueOf(f));
        }

        // Deprecated flag auto-replacements
        if (Boolean.parseBoolean(String.valueOf(map.get("hide_enchantments")))) {
            if (!app.getItemFlags().contains("HIDE_ENCHANTS")) app.getItemFlags().add("HIDE_ENCHANTS");
            result.getReplacementsApplied().add("Item '" + key + "': converted 'hide_enchantments' to 'HIDE_ENCHANTS'");
        }
        if (Boolean.parseBoolean(String.valueOf(map.get("hide_attributes")))) {
            if (!app.getItemFlags().contains("HIDE_ATTRIBUTES")) app.getItemFlags().add("HIDE_ATTRIBUTES");
            result.getReplacementsApplied().add("Item '" + key + "': converted 'hide_attributes' to 'HIDE_ATTRIBUTES'");
        }
        if (Boolean.parseBoolean(String.valueOf(map.get("hide_unbreakable")))) {
            if (!app.getItemFlags().contains("HIDE_UNBREAKABLE")) app.getItemFlags().add("HIDE_UNBREAKABLE");
            result.getReplacementsApplied().add("Item '" + key + "': converted 'hide_unbreakable' to 'HIDE_UNBREAKABLE'");
        }

        if (map.containsKey("enchantments") && map.get("enchantments") instanceof List<?> enchList) {
            for (Object e : enchList) app.getEnchantments().add(String.valueOf(e));
        }
        if (map.containsKey("unbreakable")) {
            app.setUnbreakable(Boolean.parseBoolean(String.valueOf(map.get("unbreakable"))));
        }
        if (map.containsKey("enchantment_glint_override")) {
            app.setEnchantmentGlintOverride(Boolean.parseBoolean(String.valueOf(map.get("enchantment_glint_override"))));
        }
        if (map.containsKey("hide_tooltip")) {
            app.setHideTooltip(Boolean.parseBoolean(String.valueOf(map.get("hide_tooltip"))));
        }
        if (map.containsKey("rarity")) {
            app.setRarity(String.valueOf(map.get("rarity")));
        }
        if (map.containsKey("model_data")) {
            try {
                app.setModelData(Integer.parseInt(String.valueOf(map.get("model_data"))));
            } catch (NumberFormatException ignored) {}
        }
        if (map.containsKey("model_data_component")) {
            app.setModelDataComponent(String.valueOf(map.get("model_data_component")));
        }
        if (map.containsKey("item_model")) {
            app.setItemModel(String.valueOf(map.get("item_model")));
        }
        if (map.containsKey("tooltip_style")) {
            app.setTooltipStyle(String.valueOf(map.get("tooltip_style")));
        }
        if (map.containsKey("rgb")) {
            app.setRgb(String.valueOf(map.get("rgb")));
        }
        if (map.containsKey("potion_effects") && map.get("potion_effects") instanceof List<?> potList) {
            for (Object p : potList) app.getPotionEffects().add(String.valueOf(p));
        }
        if (map.containsKey("banner_meta") && map.get("banner_meta") instanceof List<?> banList) {
            for (Object b : banList) app.getBannerMeta().add(String.valueOf(b));
        }
        if (map.containsKey("base_color")) app.setBaseColor(String.valueOf(map.get("base_color")));
        if (map.containsKey("trim_material")) app.setTrimMaterial(String.valueOf(map.get("trim_material")));
        if (map.containsKey("trim_pattern")) app.setTrimPattern(String.valueOf(map.get("trim_pattern")));
        if (map.containsKey("light_level")) {
            try {
                app.setLightLevel(Integer.parseInt(String.valueOf(map.get("light_level"))));
            } catch (NumberFormatException ignored) {}
        }

        // View requirement
        if (map.containsKey("view_requirement") && map.get("view_requirement") instanceof Map<?, ?> vMap) {
            item.setViewRequirement(YamlRequirementParser.parseRequirementList((Map<String, Object>) vMap));
        }

        // Click commands & requirements
        ClickHandler ch = item.getClickHandler();
        parseClickSection(map, "click", ch::setClickRequirement, ch.getClickCommands());
        parseClickSection(map, "left_click", ch::setLeftClickRequirement, ch.getLeftClickCommands());
        parseClickSection(map, "right_click", ch::setRightClickRequirement, ch.getRightClickCommands());
        parseClickSection(map, "shift_left_click", ch::setShiftLeftClickRequirement, ch.getShiftLeftClickCommands());
        parseClickSection(map, "shift_right_click", ch::setShiftRightClickRequirement, ch.getShiftRightClickCommands());
        parseClickSection(map, "middle_click", ch::setMiddleClickRequirement, ch.getMiddleClickCommands());

        // Unknown fields in item
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!known.contains(entry.getKey())) {
                item.getUnknownFields().put(entry.getKey(), entry.getValue());
            }
        }

        return item;
    }

    @SuppressWarnings("unchecked")
    private void parseClickSection(Map<String, Object> map, String prefix,
                                   java.util.function.Consumer<RequirementList> reqConsumer,
                                   List<Action> actionList) {
        if (map.containsKey(prefix + "_requirement") && map.get(prefix + "_requirement") instanceof Map<?, ?> reqMap) {
            reqConsumer.accept(YamlRequirementParser.parseRequirementList((Map<String, Object>) reqMap));
        }
        if (map.containsKey(prefix + "_commands") && map.get(prefix + "_commands") instanceof List<?> list) {
            for (Object o : list) {
                actionList.add(Action.parse(String.valueOf(o)));
            }
        }
    }
}
