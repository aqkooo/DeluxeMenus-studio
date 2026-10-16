package com.ejyqyl.dmstudio.service.pagination;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaginationServiceTest {

    private final PaginationService service = new PaginationService();

    @Test
    void testSinglePageGeneration() {
        PaginationService.PaginationOptions opts = PaginationService.PaginationOptions.defaults("shop", 1);
        List<MenuConfig> pages = service.generatePages(opts);
        assertEquals(1, pages.size());

        MenuConfig page1 = pages.get(0);
        assertEquals(27, page1.getSize());
        assertFalse(page1.getItems().containsKey("nav_prev"));
        assertFalse(page1.getItems().containsKey("nav_next"));
        assertTrue(page1.getItems().containsKey("nav_info"));
    }

    @Test
    void testMultiplePagesLinking() {
        PaginationService.PaginationOptions opts = PaginationService.PaginationOptions.defaults("catalog", 3);
        List<MenuConfig> pages = service.generatePages(opts);
        assertEquals(3, pages.size());

        // Page 1: Has Next, No Prev
        MenuConfig p1 = pages.get(0);
        assertFalse(p1.getItems().containsKey("nav_prev"));
        assertTrue(p1.getItems().containsKey("nav_next"));
        assertEquals("catalog_2", p1.getItems().get("nav_next").getClickHandlers().getLeftClickCommands().get(0).getArgument());

        // Page 2: Has Prev and Next
        MenuConfig p2 = pages.get(1);
        assertTrue(p2.getItems().containsKey("nav_prev"));
        assertTrue(p2.getItems().containsKey("nav_next"));
        assertEquals("catalog_1", p2.getItems().get("nav_prev").getClickHandlers().getLeftClickCommands().get(0).getArgument());
        assertEquals("catalog_3", p2.getItems().get("nav_next").getClickHandlers().getLeftClickCommands().get(0).getArgument());

        // Page 3: Has Prev, No Next
        MenuConfig p3 = pages.get(2);
        assertTrue(p3.getItems().containsKey("nav_prev"));
        assertFalse(p3.getItems().containsKey("nav_next"));
        assertEquals("catalog_2", p3.getItems().get("nav_prev").getClickHandlers().getLeftClickCommands().get(0).getArgument());
    }

    @Test
    void testBorderInclusion() {
        PaginationService.PaginationOptions withBorder = new PaginationService.PaginationOptions(
                "paged", "&8Menu {page}/{total}", 2, 27, 18, 26, 22, "ARROW", "ARROW", "BOOK", true
        );
        List<MenuConfig> pagesWith = service.generatePages(withBorder);
        assertTrue(pagesWith.get(0).getItems().containsKey("border"));

        PaginationService.PaginationOptions withoutBorder = new PaginationService.PaginationOptions(
                "paged", "&8Menu {page}/{total}", 2, 27, 18, 26, 22, "ARROW", "ARROW", "BOOK", false
        );
        List<MenuConfig> pagesWithout = service.generatePages(withoutBorder);
        assertFalse(pagesWithout.get(0).getItems().containsKey("border"));
    }
}
