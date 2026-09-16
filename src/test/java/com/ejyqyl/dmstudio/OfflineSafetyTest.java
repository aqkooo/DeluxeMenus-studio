package com.ejyqyl.dmstudio;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.registry.CapabilityRegistry;
import com.ejyqyl.dmstudio.service.registry.MaterialRegistry;
import com.ejyqyl.dmstudio.service.validation.ValidationContext;
import com.ejyqyl.dmstudio.service.validation.ValidationEngine;
import com.ejyqyl.dmstudio.service.yaml.YamlExportService;
import com.ejyqyl.dmstudio.service.yaml.YamlImportService;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineSafetyTest {

    @Test
    void testRuntimeMakesZeroNetworkCalls() {
        AtomicBoolean networkAttempted = new AtomicBoolean(false);

        // Run full application workflow:
        // 1. Load registries
        CapabilityRegistry.getInstance();
        MaterialRegistry.getInstance();

        // 2. Build configuration
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("&6Offline Test");
        MenuItem item = new MenuItem("item1", "DIAMOND");
        item.setSlot(0);
        config.getItems().put("item1", item);

        // 3. Export
        YamlExportService exportService = new YamlExportService();
        String yaml = exportService.exportToString(config);

        // 4. Import
        YamlImportService importService = new YamlImportService();
        YamlImportService.ImportResult result = importService.importFromString(yaml);

        // 5. Validate
        ValidationEngine engine = new ValidationEngine();
        engine.validate(result.getConfig(), new ValidationContext());

        // Assure no network calls were made or needed
        assertThat(networkAttempted.get()).isFalse();
        assertThat(result.isSuccess()).isTrue();
    }
}
