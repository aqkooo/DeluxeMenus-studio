package com.ejyqyl.dmstudio.service.optimizer;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.optimizer.ConfigOptimizerService.OptimizationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigOptimizerServiceTest {

    @Test
    void testCompressSlotListContiguous() {
        List<Integer> slots = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);
        List<String> result = ConfigOptimizerService.compressSlotList(slots);

        assertEquals(1, result.size());
        assertEquals("0-8", result.get(0));
    }

    @Test
    void testCompressSlotListDisjoint() {
        List<Integer> slots = List.of(0, 1, 2, 5, 10, 11, 12);
        List<String> result = ConfigOptimizerService.compressSlotList(slots);

        assertEquals(3, result.size());
        assertEquals("0-2", result.get(0));
        assertEquals("5", result.get(1));
        assertEquals("10-12", result.get(2));
    }

    @Test
    void testOptimizeConfigMergingIdenticalItems() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        // Create 3 identical border items at slots 0, 1, 2
        MenuItem i1 = new MenuItem("border_0", "GRAY_STAINED_GLASS_PANE");
        i1.setSlot(0);
        i1.getAppearance().setDisplayName(" ");

        MenuItem i2 = new MenuItem("border_1", "GRAY_STAINED_GLASS_PANE");
        i2.setSlot(1);
        i2.getAppearance().setDisplayName(" ");

        MenuItem i3 = new MenuItem("border_2", "GRAY_STAINED_GLASS_PANE");
        i3.setSlot(2);
        i3.getAppearance().setDisplayName(" ");

        // Also add an item with an uncompressed slots list
        MenuItem filler = new MenuItem("filler", "BLACK_STAINED_GLASS_PANE");
        filler.setSlots(new ArrayList<>(List.of("18", "19", "20", "21", "22")));

        config.getItems().put("border_0", i1);
        config.getItems().put("border_1", i2);
        config.getItems().put("border_2", i3);
        config.getItems().put("filler", filler);

        ConfigOptimizerService optimizer = new ConfigOptimizerService();
        OptimizationResult result = optimizer.optimize(config);

        assertNotNull(result);
        assertEquals(2, result.mergedItemsCount());
        assertTrue(result.compressedRangesCount() >= 1);

        // Verify config now has merged items
        assertEquals(2, config.getItems().size());
        MenuItem mergedBorder = config.getItems().get("border_0");
        assertNotNull(mergedBorder);
        assertEquals(List.of("0-2"), mergedBorder.getSlots());

        MenuItem compressedFiller = config.getItems().get("filler");
        assertNotNull(compressedFiller);
        assertEquals(List.of("18-22"), compressedFiller.getSlots());
    }
}
