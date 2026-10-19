package com.ejyqyl.dmstudio.service.template;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutTemplateServiceTest {

    @Test
    void testBuiltInTemplatesExist() {
        List<LayoutTemplate> list = LayoutTemplateService.getInstance().getAllTemplates();
        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(t -> "border_perimeter".equals(t.id()))).isTrue();
        assertThat(list.stream().anyMatch(t -> "bottom_navigation".equals(t.id()))).isTrue();
        assertThat(list.stream().anyMatch(t -> "confirm_dialog".equals(t.id()))).isTrue();
        assertThat(list.stream().anyMatch(t -> "player_profile".equals(t.id()))).isTrue();
    }

    @Test
    void testApplyAndUndoPerimeterBorder() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        LayoutTemplate border = LayoutTemplateService.getInstance().getAllTemplates().stream()
                .filter(t -> "border_perimeter".equals(t.id()))
                .findFirst()
                .orElseThrow();

        Command cmd = LayoutTemplateService.getInstance().applyTemplate(config, border);
        cmd.execute();

        assertThat(config.getItems()).containsKey("border");
        MenuItem borderItem = config.getItems().get("border");
        assertThat(borderItem.getSlots()).isNotEmpty();

        // Undo
        cmd.undo();
        assertThat(config.getItems()).doesNotContainKey("border");
    }

    @Test
    void testApplyConfirmationPrompt() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        LayoutTemplate confirm = LayoutTemplateService.getInstance().getAllTemplates().stream()
                .filter(t -> "confirm_dialog".equals(t.id()))
                .findFirst()
                .orElseThrow();

        Command cmd = LayoutTemplateService.getInstance().applyTemplate(config, confirm);
        cmd.execute();

        assertThat(config.getItems()).containsKey("btn_confirm");
        assertThat(config.getItems()).containsKey("btn_cancel");
        assertThat(config.getItems()).containsKey("item_info");
    }
}
