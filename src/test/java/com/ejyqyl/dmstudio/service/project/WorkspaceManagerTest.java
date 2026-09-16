package com.ejyqyl.dmstudio.service.project;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.yaml.YamlExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WorkspaceManagerTest {

    @Test
    void testWorkspaceScanAndLookup(@TempDir Path tempDir) throws IOException {
        WorkspaceManager wm = WorkspaceManager.getInstance();

        YamlExportService exporter = new YamlExportService();

        MenuConfig menu1 = new MenuConfig();
        menu1.setMenuTitle("Shop Menu");
        menu1.getOpenCommands().add("shop");
        Path file1 = tempDir.resolve("shop.yml");
        exporter.exportToFile(menu1, file1);

        MenuConfig menu2 = new MenuConfig();
        menu2.setMenuTitle("Profile Menu");
        menu2.getOpenCommands().add("profile");
        Path file2 = tempDir.resolve("profile_gui.yml");
        exporter.exportToFile(menu2, file2);

        wm.openWorkspace(tempDir.toFile());

        assertEquals(tempDir, wm.currentWorkspaceProperty().get());
        assertFalse(wm.getMenuFiles().isEmpty());

        Path foundByCmd = wm.findMenuPath("shop");
        assertNotNull(foundByCmd);
        assertEquals(file1, foundByCmd);

        Path foundByFile = wm.findMenuPath("profile_gui");
        assertNotNull(foundByFile);
        assertEquals(file2, foundByFile);

        Optional<MenuConfig> loadedOpt = wm.loadMenu("shop");
        assertTrue(loadedOpt.isPresent());
        assertEquals("Shop Menu", loadedOpt.get().getMenuTitle());

        assertNull(wm.findMenuPath("non_existent"));
        assertTrue(wm.loadMenu("non_existent").isEmpty());
    }
}
