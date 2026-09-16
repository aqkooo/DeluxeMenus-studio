package com.ejyqyl.dmstudio.service.preset;

import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.preset.ComponentLibraryService.MenuComponent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentLibraryServiceTest {

    @Test
    void testBuiltInComponents() {
        ComponentLibraryService service = ComponentLibraryService.getInstance();
        assertNotNull(service);

        List<MenuComponent> all = service.getComponents("ALL");
        assertFalse(all.isEmpty());

        assertTrue(all.stream().anyMatch(c -> "nav_back".equals(c.id())));
        assertTrue(all.stream().anyMatch(c -> "nav_close".equals(c.id())));
        assertTrue(all.stream().anyMatch(c -> "balance_card".equals(c.id())));

        List<MenuComponent> navComps = service.getComponents("Navigation");
        assertFalse(navComps.isEmpty());
        assertTrue(navComps.stream().allMatch(c -> "Navigation".equals(c.category())));
    }

    @Test
    void testSaveUserPreset() throws IOException {
        ComponentLibraryService service = ComponentLibraryService.getInstance();

        MenuItem item = new MenuItem("my_custom_button", "NETHER_STAR");
        item.getAppearance().setDisplayName("&6Супер Звезда");

        service.saveUserPreset("Моя Звезда", "Favorites", item);

        List<MenuComponent> custom = service.getComponents("Favorites");
        assertFalse(custom.isEmpty());
        assertTrue(custom.stream().anyMatch(c -> "Моя Звезда".equals(c.name_ru())));
    }
}
