package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.optimizer.ConfigOptimizerService;
import com.ejyqyl.dmstudio.service.optimizer.ConfigOptimizerService.OptimizationResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CommunityMenuStressTest {

    @Test
    void testParseAndOptimizeCommunityMenus() {
        Path sampleDir = Paths.get("target", "sample_menus");
        if (!Files.exists(sampleDir)) {
            // Target samples directory not present in CI; skip cleanly
            return;
        }

        YamlImportService importer = new YamlImportService();
        ConfigOptimizerService optimizer = new ConfigOptimizerService();

        try (Stream<Path> stream = Files.list(sampleDir)) {
            stream.filter(p -> p.toString().endsWith(".yml")).limit(20).forEach(path -> {
                try {
                    YamlImportService.ImportResult importResult = importer.importFromFile(path);
                    MenuConfig config = importResult.getConfig();
                    assertNotNull(config, "Config should not be null for " + path.getFileName());

                    if (config.getItems() != null && !config.getItems().isEmpty()) {
                        int beforeCount = config.getItems().size();
                        OptimizationResult result = optimizer.optimize(config);
                        assertNotNull(result);
                        assertTrue(config.getItems().size() <= beforeCount,
                                "Optimized items count should not exceed original for " + path.getFileName());
                    }
                } catch (Exception e) {
                    fail("Failed to parse community menu " + path.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (Exception ignored) {}
    }
}
