package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YamlComprehensiveTest {

    @TempDir
    Path tempDir;

    @Test
    void testFullFeatureSetExportAndImport() throws IOException {
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("&8Complex &cTest Menu");
        config.getOpenCommands().add("openmenu");
        config.setMenuType(InventoryType.CHEST);
        config.setSize(54);
        config.setUpdateInterval(10);
        config.getOpenActions().add(new Action("[message]", "Welcome!"));

        // Open requirement with full options
        RequirementList openReq = new RequirementList();
        Requirement req1 = new Requirement("has permission");
        req1.setField("permission", "menu.use");
        req1.setOptional(true);
        req1.getDenyCommands().add(new Action("[message]", "No permission!"));
        req1.getSuccessCommands().add(new Action("[sound]", "ENTITY_PLAYER_LEVELUP"));

        openReq.addRequirement("perm_check", req1);
        openReq.setMinimumRequirements(1);
        openReq.setStopAtSuccess(true);
        openReq.getDenyCommands().add(new Action("[close]", ""));
        openReq.getSuccessCommands().add(new Action("[broadcast]", "%player_name% opened menu"));
        config.setOpenRequirement(openReq);

        // Comprehensive MenuItem
        MenuItem item = new MenuItem("super_item", "DIAMOND_CHESTPLATE");
        item.setSlot(22);
        item.setPriority(2);
        item.setAmount(5);
        item.setDynamicAmount("%player_level%");
        item.setDamage(150);
        item.setUpdate(true);

        ItemAppearance app = item.getAppearance();
        app.setDisplayName("&b&lLegendary Armor");
        app.getLore().addAll(List.of("&7Line 1", "&eLine 2"));
        app.setLoreAppendMode("TOP");
        app.getItemFlags().addAll(List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        app.getEnchantments().add("PROTECTION;4");
        app.setUnbreakable(true);
        app.setEnchantmentGlintOverride(true);
        app.setHideTooltip(true);
        app.setRarity("RARE");
        app.setModelData(1001);
        app.setModelDataComponent("custom_armor");
        app.setItemModel("items:ruby_chestplate");
        app.setTooltipStyle("custom:epic");
        app.setRgb("255,100,50");
        app.getPotionEffects().add("SPEED;100;1");
        app.getBannerMeta().add("RED;STRIPE_TOP");
        app.setBaseColor("BLUE");
        app.setTrimMaterial("AMETHYST");
        app.setTrimPattern("VEX");
        app.setLightLevel(15);

        // Click sections
        item.getClickHandler().getRightClickCommands().add(new Action("[console]", "give %player_name% diamond 1"));
        item.getClickHandler().getShiftLeftClickCommands().add(new Action("[player]", "spawn"));
        item.getClickHandler().getShiftRightClickCommands().add(new Action("[message]", "Shift right clicked"));
        item.getClickHandler().getMiddleClickCommands().add(new Action("[sound]", "BLOCK_NOTE_BLOCK_PLING"));

        config.getItems().put("super_item", item);

        // Export to string and to file
        YamlExportService exportService = new YamlExportService();
        String yaml = exportService.exportToString(config);

        Path exportFile = tempDir.resolve("menu.yml");
        exportService.exportToFile(config, exportFile);
        assertThat(exportFile).exists();

        // Import from file
        YamlImportService importService = new YamlImportService();
        YamlImportService.ImportResult result = importService.importFromFile(exportFile);

        assertThat(result.isSuccess()).isTrue();
        MenuConfig loaded = result.getConfig();

        assertThat(loaded.getMenuTitle()).isEqualTo("&8Complex &cTest Menu");
        assertThat(loaded.getSize()).isEqualTo(54);
        assertThat(loaded.getUpdateInterval()).isEqualTo(10);
        assertThat(loaded.getOpenCommands()).containsExactly("openmenu");
        assertThat(loaded.getOpenActions()).hasSize(1);

        // Check loaded open_requirement
        assertThat(loaded.getOpenRequirement()).isNotNull();
        assertThat(loaded.getOpenRequirement().getMinimumRequirements()).isEqualTo(1);
        assertThat(loaded.getOpenRequirement().getStopAtSuccess()).isTrue();
        assertThat(loaded.getOpenRequirement().getDenyCommands()).hasSize(1);
        assertThat(loaded.getOpenRequirement().getSuccessCommands()).hasSize(1);

        // Check loaded item
        MenuItem loadedItem = loaded.getItems().get("super_item");
        assertThat(loadedItem).isNotNull();
        assertThat(loadedItem.getMaterial()).isEqualTo("DIAMOND_CHESTPLATE");
        assertThat(loadedItem.getSlot()).isEqualTo(22);
        assertThat(loadedItem.getPriority()).isEqualTo(2);
        assertThat(loadedItem.getAmount()).isEqualTo(5);
        assertThat(loadedItem.getDynamicAmount()).isEqualTo("%player_level%");
        assertThat(loadedItem.getDamage()).isEqualTo(150);
        assertThat(loadedItem.isUpdate()).isTrue();

        ItemAppearance loadedApp = loadedItem.getAppearance();
        assertThat(loadedApp.getDisplayName()).isEqualTo("&b&lLegendary Armor");
        assertThat(loadedApp.getLore()).containsExactly("&7Line 1", "&eLine 2");
        assertThat(loadedApp.getLoreAppendMode()).isEqualTo("TOP");
        assertThat(loadedApp.getItemFlags()).contains("HIDE_ENCHANTS", "HIDE_ATTRIBUTES");
        assertThat(loadedApp.getEnchantments()).contains("PROTECTION;4");
        assertThat(loadedApp.getUnbreakable()).isTrue();
        assertThat(loadedApp.getEnchantmentGlintOverride()).isTrue();
        assertThat(loadedApp.getHideTooltip()).isTrue();
        assertThat(loadedApp.getRarity()).isEqualTo("RARE");
        assertThat(loadedApp.getModelData()).isEqualTo(1001);
        assertThat(loadedApp.getModelDataComponent()).isEqualTo("custom_armor");
        assertThat(loadedApp.getItemModel()).isEqualTo("items:ruby_chestplate");
        assertThat(loadedApp.getTooltipStyle()).isEqualTo("custom:epic");
        assertThat(loadedApp.getRgb()).isEqualTo("255,100,50");
        assertThat(loadedApp.getPotionEffects()).contains("SPEED;100;1");
        assertThat(loadedApp.getBannerMeta()).contains("RED;STRIPE_TOP");
        assertThat(loadedApp.getBaseColor()).isEqualTo("BLUE");
        assertThat(loadedApp.getTrimMaterial()).isEqualTo("AMETHYST");
        assertThat(loadedApp.getTrimPattern()).isEqualTo("VEX");
        assertThat(loadedApp.getLightLevel()).isEqualTo(15);

        // Click commands
        assertThat(loadedItem.getClickHandler().getRightClickCommands()).hasSize(1);
        assertThat(loadedItem.getClickHandler().getShiftLeftClickCommands()).hasSize(1);
        assertThat(loadedItem.getClickHandler().getShiftRightClickCommands()).hasSize(1);
        assertThat(loadedItem.getClickHandler().getMiddleClickCommands()).hasSize(1);
    }
}
