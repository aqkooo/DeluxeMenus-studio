package com.ejyqyl.dmstudio.model;

import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {

    @Test
    void testActionParseAndSerialize() {
        String raw = "[console] eco take %player_name% 500<delay=5><chance=50>";
        Action action = Action.parse(raw);

        assertThat(action.getType()).isEqualTo("[console]");
        assertThat(action.getArgument()).isEqualTo("eco take %player_name% 500");
        assertThat(action.getDelay()).isEqualTo(5);
        assertThat(action.getChance()).isEqualTo(50.0);
        assertThat(action.toSerializedString()).isEqualTo(raw);
    }

    @Test
    void testActionWithoutModifiers() {
        String raw = "[sound] ENTITY_PLAYER_LEVELUP";
        Action action = Action.parse(raw);

        assertThat(action.getType()).isEqualTo("[sound]");
        assertThat(action.getArgument()).isEqualTo("ENTITY_PLAYER_LEVELUP");
        assertThat(action.getDelay()).isNull();
        assertThat(action.getChance()).isNull();
        assertThat(action.toSerializedString()).isEqualTo(raw);
    }

    @Test
    void testRequirementInversion() {
        Requirement req = new Requirement("!has permission");
        assertThat(req.isInverted()).isTrue();
        assertThat(req.getType()).isEqualTo("has permission");
        assertThat(req.getRawType()).isEqualTo("!has permission");

        req.setInverted(false);
        assertThat(req.getRawType()).isEqualTo("has permission");
    }

    @Test
    void testMenuItemSlotRangeExpansion() {
        MenuItem item = new MenuItem("border", "GRAY_STAINED_GLASS_PANE");
        item.getSlots().add("0-3");
        item.getSlots().add("5");
        item.getSlots().add("7-8");

        Set<Integer> slots = item.getAssignedSlots();
        assertThat(slots).containsExactly(0, 1, 2, 3, 5, 7, 8);
    }

    @Test
    void testMenuConfigCopy() {
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("&aShop");
        config.getOpenCommands().add("shop");

        MenuItem item = new MenuItem("fly", "FEATHER");
        item.setSlot(13);
        config.getItems().put("fly", item);

        MenuConfig copy = config.copy();
        assertThat(copy.getMenuTitle()).isEqualTo("&aShop");
        assertThat(copy.getItems()).containsKey("fly");
        assertThat(copy.getItems().get("fly")).isNotSameAs(item);
    }

    @Test
    void testMenuItemRemoveAssignedSlot() {
        MenuItem item = new MenuItem("border", "GRAY_STAINED_GLASS_PANE");
        item.getSlots().add("0-8");
        assertThat(item.getAssignedSlots()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8);

        // Remove slot 4
        boolean removed = item.removeAssignedSlot(4);
        assertThat(removed).isTrue();
        assertThat(item.getAssignedSlots()).containsExactly(0, 1, 2, 3, 5, 6, 7, 8);
        assertThat(item.getSlots()).containsExactly("0-3", "5-8");

        // Remove non-existent slot
        assertThat(item.removeAssignedSlot(99)).isFalse();

        // Single slot item
        MenuItem single = new MenuItem("btn", "STONE");
        single.setSlot(10);
        assertThat(single.removeAssignedSlot(10)).isTrue();
        assertThat(single.getAssignedSlots()).isEmpty();
        assertThat(single.getSlot()).isNull();
    }
}
