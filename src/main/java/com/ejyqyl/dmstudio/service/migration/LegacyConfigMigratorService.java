package com.ejyqyl.dmstudio.service.migration;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.command.impl.BatchCommand;
import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.command.impl.UpdateMenuPropertyCommand;
import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service providing detection and batch conversion of legacy Minecraft 1.12 formats and DeluxeMenus syntax.
 */
public class LegacyConfigMigratorService {

    public enum ChangeCategory {
        MATERIAL,
        DATA_REMOVAL,
        COMMAND_SYNTAX,
        FLAG_NORMALIZATION
    }

    public record MigrationChange(
            ChangeCategory category,
            String target,
            String description,
            String oldValue,
            String newValue
    ) {}

    public record MigrationReport(
            List<MigrationChange> changes,
            int materialChangesCount,
            int syntaxChangesCount
    ) {
        public boolean hasChanges() {
            return !changes.isEmpty();
        }
    }

    public MigrationReport analyze(MenuConfig config) {
        List<MigrationChange> changes = new ArrayList<>();
        int matCount = 0;
        int syntaxCount = 0;

        if (config == null) {
            return new MigrationReport(changes, 0, 0);
        }

        // 1. Check open_command format
        if (config.getOpenCommands() != null) {
            for (String cmd : config.getOpenCommands()) {
                if (cmd != null && cmd.startsWith("/")) {
                    changes.add(new MigrationChange(
                            ChangeCategory.COMMAND_SYNTAX,
                            "open_command",
                            "Remove leading slash from open_command",
                            cmd,
                            cmd.substring(1)
                    ));
                    syntaxCount++;
                }
            }
        }

        // 2. Scan Items for legacy materials, data values, and old flags
        for (MenuItem item : config.getItems().values()) {
            Integer damage = item.getDamage();
            Optional<String> modernMat = LegacyMaterialRegistry.findModernMaterial(item.getMaterial(), damage);

            if (modernMat.isPresent() && !modernMat.get().equalsIgnoreCase(item.getMaterial())) {
                changes.add(new MigrationChange(
                        ChangeCategory.MATERIAL,
                        item.getKey(),
                        "Convert legacy material to modern equivalent",
                        item.getMaterial() + (damage != null ? " (data: " + damage + ")" : ""),
                        modernMat.get()
                ));
                matCount++;

                if (damage != null && damage > 0) {
                    changes.add(new MigrationChange(
                            ChangeCategory.DATA_REMOVAL,
                            item.getKey(),
                            "Remove redundant legacy data value incorporated into material name",
                            String.valueOf(damage),
                            "none"
                    ));
                    syntaxCount++;
                }
            }

            // Flags normalization
            ItemAppearance app = item.getAppearance();
            if (app != null) {
                List<String> flags = app.getItemFlags();
                for (int i = 0; i < flags.size(); i++) {
                    String f = flags.get(i);
                    String upper = f.toUpperCase().trim();
                    String modernFlag = switch (upper) {
                        case "HIDE_ENCHANTMENTS" -> "HIDE_ENCHANTS";
                        case "HIDE_ATTRIBUTES_TAG" -> "HIDE_ATTRIBUTES";
                        case "HIDE_UNBREAKABLE_TAG" -> "HIDE_UNBREAKABLE";
                        default -> upper;
                    };
                    if (!modernFlag.equals(f)) {
                        changes.add(new MigrationChange(
                            ChangeCategory.FLAG_NORMALIZATION,
                            item.getKey(),
                            "Normalize legacy flag",
                            f,
                            modernFlag
                        ));
                        syntaxCount++;
                    }
                }
            }
        }

        return new MigrationReport(changes, matCount, syntaxCount);
    }

    public Command buildMigrationCommand(MenuConfig config, MigrationReport report) {
        List<Command> commands = new ArrayList<>();

        // Fix open commands with leading slashes
        if (config.getOpenCommands() != null) {
            List<String> sanitized = new ArrayList<>();
            boolean modified = false;
            for (String cmd : config.getOpenCommands()) {
                if (cmd != null && cmd.startsWith("/")) {
                    sanitized.add(cmd.substring(1));
                    modified = true;
                } else {
                    sanitized.add(cmd);
                }
            }
            if (modified) {
                List<String> originalOpenCmds = new ArrayList<>(config.getOpenCommands());
                commands.add(new UpdateMenuPropertyCommand(
                        config,
                        "Sanitize open commands",
                        c -> { c.getOpenCommands().clear(); c.getOpenCommands().addAll(sanitized); },
                        c -> { c.getOpenCommands().clear(); c.getOpenCommands().addAll(originalOpenCmds); }
                ));
            }
        }

        // Apply item updates
        for (MenuItem item : config.getItems().values()) {
            boolean itemChanged = false;
            MenuItem copy = item.copy();

            Integer damage = copy.getDamage();
            Optional<String> modernMat = LegacyMaterialRegistry.findModernMaterial(copy.getMaterial(), damage);

            if (modernMat.isPresent() && !modernMat.get().equalsIgnoreCase(copy.getMaterial())) {
                copy.setMaterial(modernMat.get());
                copy.setDamage(null);
                itemChanged = true;
            }

            ItemAppearance app = copy.getAppearance();
            if (app != null) {
                List<String> flags = new ArrayList<>(app.getItemFlags());
                boolean flagsChanged = false;
                for (int i = 0; i < flags.size(); i++) {
                    String f = flags.get(i);
                    String upper = f.toUpperCase().trim();
                    String modernFlag = switch (upper) {
                        case "HIDE_ENCHANTMENTS" -> "HIDE_ENCHANTS";
                        case "HIDE_ATTRIBUTES_TAG" -> "HIDE_ATTRIBUTES";
                        case "HIDE_UNBREAKABLE_TAG" -> "HIDE_UNBREAKABLE";
                        default -> upper;
                    };
                    if (!modernFlag.equals(f)) {
                        flags.set(i, modernFlag);
                        flagsChanged = true;
                    }
                }
                if (flagsChanged) {
                    app.getItemFlags().clear();
                    app.getItemFlags().addAll(flags);
                    itemChanged = true;
                }
            }

            if (itemChanged) {
                commands.add(new SetItemCommand(config, copy));
            }
        }

        return new BatchCommand("Migrate legacy configuration (" + report.changes().size() + " changes)", commands);
    }
}
