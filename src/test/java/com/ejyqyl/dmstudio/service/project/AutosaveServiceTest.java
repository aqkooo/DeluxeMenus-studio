package com.ejyqyl.dmstudio.service.project;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutosaveServiceTest {

    private AutosaveService autosaveService;

    @BeforeEach
    void setUp() {
        autosaveService = new AutosaveService();
        autosaveService.clearDraft();
    }

    @AfterEach
    void tearDown() {
        autosaveService.stop();
        autosaveService.clearDraft();
    }

    @Test
    void testSaveAndLoadDraft() throws Exception {
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("&cDraft Title");
        MenuItem item = new MenuItem("draft_item", "GOLD_INGOT");
        item.setSlot(5);
        config.getItems().put("draft_item", item);

        autosaveService.saveDraft(config);
        assertThat(autosaveService.hasDraft()).isTrue();

        MenuConfig loaded = autosaveService.loadDraft();
        assertThat(loaded).isNotNull();
        assertThat(loaded.getMenuTitle()).isEqualTo("&cDraft Title");
        assertThat(loaded.getItems()).containsKey("draft_item");

        autosaveService.clearDraft();
        assertThat(autosaveService.hasDraft()).isFalse();
    }
}
