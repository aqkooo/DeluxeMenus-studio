package com.ejyqyl.dmstudio.service.registry;

import com.ejyqyl.dmstudio.service.registry.PapiCatalogService.PapiEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PapiCatalogServiceTest {

    @Test
    void testCatalogLoadingAndSearch() {
        PapiCatalogService service = PapiCatalogService.getInstance();
        assertNotNull(service);

        List<PapiEntry> all = service.getAll();
        assertFalse(all.isEmpty());
        assertTrue(all.size() >= 20);

        assertTrue(service.getCategories().contains("Player"));
        assertTrue(service.getCategories().contains("Vault"));

        List<PapiEntry> balanceResults = service.search("balance", "ALL");
        assertFalse(balanceResults.isEmpty());
        assertTrue(balanceResults.stream().anyMatch(e -> e.placeholder().contains("vault_eco_balance")));

        List<PapiEntry> playerResults = service.search("", "Player");
        assertFalse(playerResults.isEmpty());
        assertTrue(playerResults.stream().allMatch(e -> "Player".equalsIgnoreCase(e.category())));
    }
}
