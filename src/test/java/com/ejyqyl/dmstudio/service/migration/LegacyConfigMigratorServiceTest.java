package com.ejyqyl.dmstudio.service.migration;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyConfigMigratorServiceTest {

    @Test
    void testAnalyzeAndMigrateLegacyConfig() {
        MenuConfig config = new MenuConfig();
        config.getOpenCommands().addAll(List.of("/shop", "store"));

        MenuItem legacyItem = new MenuItem("donate_wool", "35");
        legacyItem.setDamage(14);
        legacyItem.setSlot(10);
        legacyItem.getAppearance().getItemFlags().add("HIDE_ENCHANTMENTS");
        config.getItems().put("donate_wool", legacyItem);

        LegacyConfigMigratorService service = new LegacyConfigMigratorService();
        LegacyConfigMigratorService.MigrationReport report = service.analyze(config);

        assertThat(report.hasChanges()).isTrue();
        assertThat(report.materialChangesCount()).isEqualTo(1);
        assertThat(report.syntaxChangesCount()).isGreaterThanOrEqualTo(2);

        Command cmd = service.buildMigrationCommand(config, report);
        cmd.execute();

        // Check updated config
        assertThat(config.getOpenCommands()).containsExactly("shop", "store");
        MenuItem migrated = config.getItems().get("donate_wool");
        assertThat(migrated.getMaterial()).isEqualTo("RED_WOOL");
        assertThat(migrated.getDamage()).isNull();
        assertThat(migrated.getAppearance().getItemFlags()).contains("HIDE_ENCHANTS");

        // Test Undo
        cmd.undo();
        assertThat(config.getOpenCommands()).containsExactly("/shop", "store");
        MenuItem reverted = config.getItems().get("donate_wool");
        assertThat(reverted.getMaterial()).isEqualTo("35");
        assertThat(reverted.getDamage()).isEqualTo(14);
    }
}
