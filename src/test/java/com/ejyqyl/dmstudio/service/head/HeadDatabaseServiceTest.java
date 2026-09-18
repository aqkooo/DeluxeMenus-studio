package com.ejyqyl.dmstudio.service.head;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HeadDatabaseServiceTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testCatalogLoaded() {
        HeadDatabaseService service = HeadDatabaseService.getInstance();
        assertNotNull(service);

        List<HeadEntry> heads = service.getAllHeads();
        assertFalse(heads.isEmpty(), "Catalog should contain heads");
        assertTrue(heads.size() >= 30, "Should contain at least 30 popular heads");

        List<String> categories = service.getCategories();
        assertTrue(categories.contains("ALL"));
        assertTrue(categories.contains("NAVIGATION"));
        assertTrue(categories.contains("SYMBOLS"));
    }

    @Test
    void testFindByIdAndValue() {
        HeadDatabaseService service = HeadDatabaseService.getInstance();

        Optional<HeadEntry> greenArrow = service.findById("7129");
        assertTrue(greenArrow.isPresent());
        assertEquals("NAVIGATION", greenArrow.get().category());
        assertNotNull(greenArrow.get().value());

        // Test with hdb- prefix
        Optional<HeadEntry> withPrefix = service.findById("hdb-7129");
        assertTrue(withPrefix.isPresent());
        assertEquals(greenArrow.get(), withPrefix.get());

        // Test find by value
        Optional<HeadEntry> byVal = service.findByValue(greenArrow.get().value());
        assertTrue(byVal.isPresent());
        assertEquals("7129", byVal.get().id());

        // Test find by basehead- prefix
        Optional<HeadEntry> byBasehead = service.findByValue("basehead-" + greenArrow.get().value());
        assertTrue(byBasehead.isPresent());
        assertEquals("7129", byBasehead.get().id());
    }

    @Test
    void testSearchAndFilter() {
        HeadDatabaseService service = HeadDatabaseService.getInstance();

        List<HeadEntry> navHeads = service.getByCategory("NAVIGATION");
        assertFalse(navHeads.isEmpty());
        assertTrue(navHeads.stream().allMatch(h -> "NAVIGATION".equalsIgnoreCase(h.category())));

        List<HeadEntry> searchResultsRu = service.search("стрелка", true);
        assertFalse(searchResultsRu.isEmpty());

        List<HeadEntry> searchResultsEn = service.search("arrow", false);
        assertFalse(searchResultsEn.isEmpty());

        List<HeadEntry> searchById = service.search("7129", false);
        assertEquals(1, searchById.size());
    }

    @Test
    void testCreateHeadIconNode() {
        HeadDatabaseService service = HeadDatabaseService.getInstance();

        Node hdbIcon = service.createHeadIcon("hdb-7129", 32);
        assertNotNull(hdbIcon);
        assertTrue(hdbIcon instanceof ImageView);
        assertEquals(32, ((ImageView) hdbIcon).getFitWidth());

        HeadEntry entry = service.findById("7129").orElseThrow();
        Node baseheadIcon = service.createHeadIcon("basehead-" + entry.value(), 32);
        assertNotNull(baseheadIcon);
        assertTrue(baseheadIcon instanceof ImageView);

        Node steveIcon = service.createHeadIcon("head-Steve", 32);
        assertNotNull(steveIcon);

        Node playerHeadIcon = service.createHeadIcon("PLAYER_HEAD", 32);
        assertNotNull(playerHeadIcon);
    }

    @Test
    void testTextureHashExtractionAndUrlConversion() {
        String testUrl = "http://textures.minecraft.net/texture/eb93f9c74c053047820e1784f87edc1eaa71add9d4f8b19b7ece1ff01b1f228";
        String base64 = HeadDatabaseService.urlToBaseheadValue(testUrl);
        assertNotNull(base64);
        assertFalse(base64.isBlank());

        String extractedHash = HeadDatabaseService.extractTextureHash(base64);
        assertEquals("eb93f9c74c053047820e1784f87edc1eaa71add9d4f8b19b7ece1ff01b1f228", extractedHash);
    }
}
