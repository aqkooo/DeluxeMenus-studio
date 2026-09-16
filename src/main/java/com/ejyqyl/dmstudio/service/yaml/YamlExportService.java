package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for serializing MenuConfig into clean, deterministic DeluxeMenus YAML.
 */
public class YamlExportService {

    private final Yaml yaml;

    public YamlExportService() {
        this.yaml = YamlDumperFactory.createYaml();
    }

    public String exportToString(MenuConfig config) {
        Map<String, Object> root = buildRootMap(config);
        return yaml.dump(root);
    }

    public void exportToFile(MenuConfig config, Path targetFile) throws IOException {
        String content = exportToString(config);
        // UTF-8 without BOM, UNIX line endings (Specification §6.5 & §7 NFR-6)
        content = content.replace("\r\n", "\n");
        Files.writeString(targetFile, content, StandardCharsets.UTF_8);
    }

    public Map<String, Object> buildRootMap(MenuConfig config) {
        Map<String, Object> root = new LinkedHashMap<>();

        // 1. menu_title
        if (config.getMenuTitle() != null) {
            root.put("menu_title", config.getMenuTitle());
        }

        // 2. open_command
        if (!config.getOpenCommands().isEmpty()) {
            if (config.getOpenCommands().size() == 1) {
                root.put("open_command", config.getOpenCommands().get(0));
            } else {
                root.put("open_command", new ArrayList<>(config.getOpenCommands()));
            }
        }

        // 3. register_command
        root.put("register_command", config.isRegisterCommand());

        // 4. menu_type
        if (config.getMenuType() != null) {
            root.put("menu_type", config.getMenuType().name());
        }

        // 5. size (only for CHEST)
        if (config.getMenuType() == InventoryType.CHEST) {
            root.put("size", config.getSize());
        }

        // 6. update_interval
        if (config.getUpdateInterval() != null && config.getUpdateInterval() > 0) {
            root.put("update_interval", config.getUpdateInterval());
        }

        // 7. open_requirement
        if (config.getOpenRequirement() != null && !config.getOpenRequirement().isEmpty()) {
            root.put("open_requirement", buildRequirementListMap(config.getOpenRequirement()));
        }

        // 8. open_commands
        if (!config.getOpenActions().isEmpty()) {
            root.put("open_commands", serializeActions(config.getOpenActions()));
        }

        // 9. items
        if (!config.getItems().isEmpty()) {
            Map<String, Object> itemsMap = new LinkedHashMap<>();
            for (Map.Entry<String, MenuItem> entry : config.getItems().entrySet()) {
                itemsMap.put(entry.getKey(), buildItemMap(entry.getValue()));
            }
            root.put("items", itemsMap);
        }

        // 10. unknownFields
        root.putAll(config.getUnknownFields());

        return root;
    }

    private Map<String, Object> buildItemMap(MenuItem item) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (item.getMaterial() != null) {
            map.put("material", item.getMaterial());
        }

        // slot or slots
        if (!item.getSlots().isEmpty()) {
            map.put("slots", new ArrayList<>(item.getSlots()));
        } else if (item.getSlot() != null) {
            map.put("slot", item.getSlot());
        }

        // priority (0 is highest)
        map.put("priority", item.getPriority());

        if (item.getAmount() > 1) {
            map.put("amount", item.getAmount());
        }
        if (item.getDynamicAmount() != null && !item.getDynamicAmount().isBlank()) {
            map.put("dynamic_amount", item.getDynamicAmount());
        }
        if (item.getDamage() != null) {
            map.put("damage", item.getDamage());
        }
        if (item.isUpdate()) {
            map.put("update", true);
        }

        // Appearance
        ItemAppearance app = item.getAppearance();
        if (app != null) {
            if (app.getDisplayName() != null && !app.getDisplayName().isEmpty()) {
                map.put("display_name", app.getDisplayName());
            }
            if (!app.getLore().isEmpty()) {
                map.put("lore", new ArrayList<>(app.getLore()));
            }
            if (app.getLoreAppendMode() != null) {
                map.put("lore_append_mode", app.getLoreAppendMode());
            }
            if (!app.getItemFlags().isEmpty()) {
                map.put("item_flags", new ArrayList<>(app.getItemFlags()));
            }
            if (!app.getEnchantments().isEmpty()) {
                map.put("enchantments", new ArrayList<>(app.getEnchantments()));
            }
            if (Boolean.TRUE.equals(app.getUnbreakable())) {
                map.put("unbreakable", true);
            }
            if (Boolean.TRUE.equals(app.getEnchantmentGlintOverride())) {
                map.put("enchantment_glint_override", true);
            }
            if (Boolean.TRUE.equals(app.getHideTooltip())) {
                map.put("hide_tooltip", true);
            }
            if (app.getRarity() != null) {
                map.put("rarity", app.getRarity());
            }
            if (app.getModelData() != null) {
                map.put("model_data", app.getModelData());
            }
            if (app.getModelDataComponent() != null && !app.getModelDataComponent().isBlank()) {
                map.put("model_data_component", app.getModelDataComponent());
            }
            if (app.getItemModel() != null && !app.getItemModel().isBlank()) {
                map.put("item_model", app.getItemModel());
            }
            if (app.getTooltipStyle() != null && !app.getTooltipStyle().isBlank()) {
                map.put("tooltip_style", app.getTooltipStyle());
            }
            if (app.getRgb() != null && !app.getRgb().isBlank()) {
                map.put("rgb", app.getRgb());
            }
            if (!app.getPotionEffects().isEmpty()) {
                map.put("potion_effects", new ArrayList<>(app.getPotionEffects()));
            }
            if (!app.getBannerMeta().isEmpty()) {
                map.put("banner_meta", new ArrayList<>(app.getBannerMeta()));
            }
            if (app.getBaseColor() != null) {
                map.put("base_color", app.getBaseColor());
            }
            if (app.getTrimMaterial() != null) {
                map.put("trim_material", app.getTrimMaterial());
            }
            if (app.getTrimPattern() != null) {
                map.put("trim_pattern", app.getTrimPattern());
            }
            if (app.getLightLevel() != null) {
                map.put("light_level", app.getLightLevel());
            }
            map.putAll(app.getUnknownFields());
        }

        // View requirement
        if (item.getViewRequirement() != null && !item.getViewRequirement().isEmpty()) {
            map.put("view_requirement", buildRequirementListMap(item.getViewRequirement()));
        }

        // Click actions and requirements
        ClickHandler ch = item.getClickHandler();
        if (ch != null) {
            writeClickSection(map, "click", ch.getClickRequirement(), ch.getClickCommands());
            writeClickSection(map, "left_click", ch.getLeftClickRequirement(), ch.getLeftClickCommands());
            writeClickSection(map, "right_click", ch.getRightClickRequirement(), ch.getRightClickCommands());
            writeClickSection(map, "shift_left_click", ch.getShiftLeftClickRequirement(), ch.getShiftLeftClickCommands());
            writeClickSection(map, "shift_right_click", ch.getShiftRightClickRequirement(), ch.getShiftRightClickCommands());
            writeClickSection(map, "middle_click", ch.getMiddleClickRequirement(), ch.getMiddleClickCommands());
        }

        map.putAll(item.getUnknownFields());
        return map;
    }

    private void writeClickSection(Map<String, Object> map, String prefix, RequirementList req, List<Action> actions) {
        if (req != null && !req.isEmpty()) {
            map.put(prefix + "_requirement", buildRequirementListMap(req));
        }
        if (actions != null && !actions.isEmpty()) {
            map.put(prefix + "_commands", serializeActions(actions));
        }
    }

    private Map<String, Object> buildRequirementListMap(RequirementList list) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!list.getRequirements().isEmpty()) {
            Map<String, Object> reqsMap = new LinkedHashMap<>();
            for (Map.Entry<String, Requirement> entry : list.getRequirements().entrySet()) {
                reqsMap.put(entry.getKey(), buildRequirementMap(entry.getValue()));
            }
            map.put("requirements", reqsMap);
        }
        if (list.getMinimumRequirements() != null) {
            map.put("minimum_requirements", list.getMinimumRequirements());
        }
        if (Boolean.TRUE.equals(list.getStopAtSuccess())) {
            map.put("stop_at_success", true);
        }
        if (!list.getDenyCommands().isEmpty()) {
            map.put("deny_commands", serializeActions(list.getDenyCommands()));
        }
        if (!list.getSuccessCommands().isEmpty()) {
            map.put("success_commands", serializeActions(list.getSuccessCommands()));
        }
        map.putAll(list.getUnknownFields());
        return map;
    }

    private Map<String, Object> buildRequirementMap(Requirement req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", req.getRawType());
        map.putAll(req.getFields());
        if (Boolean.TRUE.equals(req.getOptional())) {
            map.put("optional", true);
        }
        if (!req.getDenyCommands().isEmpty()) {
            map.put("deny_commands", serializeActions(req.getDenyCommands()));
        }
        if (!req.getSuccessCommands().isEmpty()) {
            map.put("success_commands", serializeActions(req.getSuccessCommands()));
        }
        map.putAll(req.getUnknownFields());
        return map;
    }

    private List<String> serializeActions(List<Action> actions) {
        List<String> list = new ArrayList<>(actions.size());
        for (Action a : actions) {
            list.add(a.toSerializedString());
        }
        return list;
    }
}
