package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YamlExportGoldenTest {

    @Test
    void testGoldenSpecificationExampleExport() {
        MenuConfig config = new MenuConfig();
        config.setMenuTitle("&#55ff55&lСерверный Магазин");
        config.getOpenCommands().addAll(List.of("shop", "store"));
        config.setRegisterCommand(true);
        config.setMenuType(InventoryType.CHEST);
        config.setSize(27);
        config.setUpdateInterval(20);

        // open_requirement
        RequirementList openReq = new RequirementList();
        Requirement notBanned = new Requirement("!has permission");
        notBanned.setField("permission", "server.shop.blocked");
        notBanned.getDenyCommands().add(new Action("[message]", "&cДоступ к магазину закрыт."));
        openReq.addRequirement("not_banned", notBanned);
        config.setOpenRequirement(openReq);

        // border item
        MenuItem border = new MenuItem("border", "GRAY_STAINED_GLASS_PANE");
        border.getSlots().addAll(List.of("0-8", "18-26"));
        border.getAppearance().setDisplayName(" ");
        border.setPriority(10);
        config.getItems().put("border", border);

        // donate_fly item
        MenuItem donateFly = new MenuItem("donate_fly", "FEATHER");
        donateFly.setSlot(13);
        donateFly.setPriority(0);
        donateFly.setUpdate(true);
        donateFly.getAppearance().setDisplayName("&#ffaa00&lВозможность полета (/fly)");
        donateFly.getAppearance().getLore().addAll(List.of(
                "&7Позволяет летать в пределах",
                "&7основного мира выживания.",
                "",
                "&fЦена: &a500 монет",
                "&7Ваш баланс: &a%vault_eco_balance%",
                "&eКликните для покупки!"
        ));
        donateFly.getAppearance().getItemFlags().addAll(List.of("HIDE_ATTRIBUTES", "HIDE_ENCHANTS"));
        donateFly.getAppearance().setEnchantmentGlintOverride(true);

        // view_requirement
        RequirementList viewReq = new RequirementList();
        Requirement noFly = new Requirement("!has permission");
        noFly.setField("permission", "group.fly");
        viewReq.addRequirement("no_fly_yet", noFly);
        donateFly.setViewRequirement(viewReq);

        // left_click_requirement
        RequirementList leftReq = new RequirementList();
        Requirement enoughMoney = new Requirement("has money");
        enoughMoney.setField("amount", 500);
        leftReq.addRequirement("enough_money", enoughMoney);
        leftReq.getDenyCommands().addAll(List.of(
                new Action("[message]", "&cНедостаточно средств."),
                new Action("[sound]", "ENTITY_VILLAGER_NO")
        ));
        donateFly.getClickHandler().setLeftClickRequirement(leftReq);

        // left_click_commands
        donateFly.getClickHandler().getLeftClickCommands().addAll(List.of(
                new Action("[console]", "eco take %player_name% 500"),
                new Action("[console]", "lp user %player_name% permission set group.fly true"),
                new Action("[sound]", "ENTITY_PLAYER_LEVELUP"),
                new Action("[message]", "&aПокупка совершена!", 5, null),
                new Action("[refresh]", "")
        ));
        config.getItems().put("donate_fly", donateFly);

        // donate_fly_owned
        MenuItem donateFlyOwned = new MenuItem("donate_fly_owned", "FEATHER");
        donateFlyOwned.setSlot(13);
        donateFlyOwned.setPriority(1);
        donateFlyOwned.getAppearance().setDisplayName("&a&lПолёт уже приобретён");
        donateFlyOwned.getAppearance().getLore().add("&7Используйте &f/fly");
        donateFlyOwned.getAppearance().getItemFlags().add("HIDE_ATTRIBUTES");
        config.getItems().put("donate_fly_owned", donateFlyOwned);

        YamlExportService exportService = new YamlExportService();
        String yamlString = exportService.exportToString(config);

        System.out.println("=== EXPORTED YAML ===");
        System.out.println(yamlString);

        // Verify that parsing this exported YAML with standard SnakeYAML preserves complete data integrity
        Yaml parser = new Yaml();
        Map<String, Object> parsed = parser.load(yamlString);

        assertThat(parsed).containsKey("menu_title");
        assertThat(parsed.get("menu_title")).isEqualTo("&#55ff55&lСерверный Магазин");
        assertThat(parsed.get("size")).isEqualTo(27);
        assertThat(parsed.get("update_interval")).isEqualTo(20);

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) parsed.get("items");
        assertThat(items).containsKeys("border", "donate_fly", "donate_fly_owned");

        @SuppressWarnings("unchecked")
        Map<String, Object> fly = (Map<String, Object>) items.get("donate_fly");
        assertThat(fly.get("material")).isEqualTo("FEATHER");
        assertThat(fly.get("slot")).isEqualTo(13);
        assertThat(fly.get("priority")).isEqualTo(0);
        assertThat(fly.get("update")).isEqualTo(true);
        assertThat(fly.get("enchantment_glint_override")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<String> leftCmds = (List<String>) fly.get("left_click_commands");
        assertThat(leftCmds).contains("[message] &aПокупка совершена!<delay=5>", "[refresh]");
    }
}
