package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YamlImportTest {

    @Test
    void testRoundTripPreservesDataAndUnknownFields() {
        String yaml = """
                menu_title: '&#55ff55&lServer Menu'
                open_command:
                  - test
                register_command: true
                menu_type: CHEST
                size: 27
                custom_unknown_top_field: 'extra_data_value'
                items:
                  'my_item':
                    material: DIAMOND_SWORD
                    slot: 4
                    priority: 0
                    display_name: '&bSword'
                    custom_fork_field: 12345
                    left_click_commands:
                      - '[message] &aClicked!'
                """;

        YamlImportService importService = new YamlImportService();
        YamlImportService.ImportResult result = importService.importFromString(yaml);

        assertThat(result.isSuccess()).isTrue();
        MenuConfig config = result.getConfig();
        assertThat(config.getMenuTitle()).isEqualTo("&#55ff55&lServer Menu");
        assertThat(config.getSize()).isEqualTo(27);
        assertThat(config.getUnknownFields()).containsEntry("custom_unknown_top_field", "extra_data_value");

        MenuItem item = config.getItems().get("my_item");
        assertThat(item).isNotNull();
        assertThat(item.getMaterial()).isEqualTo("DIAMOND_SWORD");
        assertThat(item.getSlot()).isEqualTo(4);
        assertThat(item.getUnknownFields()).containsEntry("custom_fork_field", 12345);

        // Export and re-import
        YamlExportService exportService = new YamlExportService();
        String exported = exportService.exportToString(config);

        YamlImportService.ImportResult roundTripResult = importService.importFromString(exported);
        assertThat(roundTripResult.isSuccess()).isTrue();
        MenuConfig roundTripConfig = roundTripResult.getConfig();

        assertThat(roundTripConfig.getUnknownFields()).containsEntry("custom_unknown_top_field", "extra_data_value");
        MenuItem roundTripItem = roundTripConfig.getItems().get("my_item");
        assertThat(roundTripItem.getUnknownFields()).containsEntry("custom_fork_field", 12345);
        assertThat(roundTripItem.getMaterial()).isEqualTo("DIAMOND_SWORD");
    }

    @Test
    void testDeprecatedOptionsAutoMigration() {
        String yaml = """
                menu_title: 'Legacy Menu'
                items:
                  'old_item':
                    material: WOOL
                    slot: 0
                    data: 14
                    hide_enchantments: true
                    hide_attributes: true
                """;

        YamlImportService importService = new YamlImportService();
        YamlImportService.ImportResult result = importService.importFromString(yaml);

        assertThat(result.isSuccess()).isTrue();
        MenuItem item = result.getConfig().getItems().get("old_item");
        assertThat(item.getDamage()).isEqualTo(14); // data -> damage
        assertThat(item.getAppearance().getItemFlags()).contains("HIDE_ENCHANTS", "HIDE_ATTRIBUTES");
        assertThat(result.getReplacementsApplied()).isNotEmpty();
    }

    @Test
    void testInvalidYamlSyntaxHandledGracefully() {
        String badYaml = """
                menu_title: 'Unclosed quotes
                items:
                  broken: [unclosed
                """;

        YamlImportService importService = new YamlImportService();
        YamlImportService.ImportResult result = importService.importFromString(badYaml);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("YAML parsing error");
    }
}
