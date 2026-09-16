package com.ejyqyl.dmstudio.service.validation;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.model.validation.ValidationIssue;
import com.ejyqyl.dmstudio.model.validation.ValidationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationEngineTest {

    private ValidationEngine engine;
    private ValidationContext context;

    @BeforeEach
    void setUp() {
        engine = new ValidationEngine();
        context = new ValidationContext("1.21", "1.14.0");
    }

    @Test
    void testSlotOutOfBounds_V1() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(30); // out of bounds
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-1") && i.level() == ValidationLevel.ERROR);
    }

    @Test
    void testPriorityCollision_V2() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        MenuItem item1 = new MenuItem("item1", "STONE");
        item1.setSlot(10);
        item1.setPriority(0);

        MenuItem item2 = new MenuItem("item2", "DIAMOND");
        item2.setSlot(10);
        item2.setPriority(0); // Collision!

        config.getItems().put("item1", item1);
        config.getItems().put("item2", item2);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-2") && i.level() == ValidationLevel.ERROR);
    }

    @Test
    void testViewRequirementMissing_V3() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        MenuItem item1 = new MenuItem("item1", "STONE");
        item1.setSlot(10);
        item1.setPriority(0); // No view requirement

        MenuItem item2 = new MenuItem("item2", "DIAMOND");
        item2.setSlot(10);
        item2.setPriority(1);

        config.getItems().put("item1", item1);
        config.getItems().put("item2", item2);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-3") && i.level() == ValidationLevel.WARNING);
    }

    @Test
    void testUnknownMaterial_V4() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "SUPER_FAKE_BLOCK_999");
        item.setSlot(0);
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-4") && i.level() == ValidationLevel.WARNING);
    }

    @Test
    void testSlotAndSlots_V6() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);
        item.getSlots().add("1-3");
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-6") && i.level() == ValidationLevel.WARNING);
    }

    @Test
    void testVersionRestrictions_V7() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);
        item.getAppearance().setItemModel("custom:ruby");
        item.getAppearance().setEnchantmentGlintOverride(true);
        item.getAppearance().setHideTooltip(true);
        config.getItems().put("item1", item);

        context.setMcVersion("1.19");
        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-7") && i.fieldName().equals("item_model"));
        assertThat(issues).anyMatch(i -> i.code().equals("V-7") && i.fieldName().equals("enchantment_glint_override"));
        assertThat(issues).anyMatch(i -> i.code().equals("V-7") && i.fieldName().equals("hide_tooltip"));
    }

    @Test
    void testClickRequirementWithoutCommands_V9() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);

        RequirementList leftReq = new RequirementList();
        leftReq.addRequirement("perm", new Requirement("has permission"));
        item.getClickHandler().setLeftClickRequirement(leftReq);

        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-9") && i.level() == ValidationLevel.ERROR);
    }

    @Test
    void testArmorTrimIncomplete_V10() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "DIAMOND_CHESTPLATE");
        item.setSlot(0);
        item.getAppearance().setTrimMaterial("AMETHYST");

        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-10") && i.level() == ValidationLevel.ERROR);
    }

    @Test
    void testUpdateIntervalMissing_V11() {
        MenuConfig config = new MenuConfig();
        config.setUpdateInterval(null);
        MenuItem item = new MenuItem("item1", "CLOCK");
        item.setSlot(0);
        item.setUpdate(true);
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-11") && i.level() == ValidationLevel.WARNING);
    }

    @Test
    void testUnbalancedPlaceholder_V12() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);
        item.getAppearance().setDisplayName("&aBalance: %vault_eco_balance");
        item.getAppearance().getLore().add("Line with %unbalanced");
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-12") && i.fieldName().equals("display_name"));
        assertThat(issues).anyMatch(i -> i.code().equals("V-12") && i.fieldName().equals("lore"));
    }

    @Test
    void testMixedColorCodes_V13() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);
        item.getAppearance().setDisplayName("&c<red>Mixed");
        item.getAppearance().getLore().add("&a<gold>Mixed in lore");
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-13") && i.fieldName().equals("display_name"));
        assertThat(issues).anyMatch(i -> i.code().equals("V-13") && i.fieldName().equals("lore"));
    }

    @Test
    void testMinimumRequirementsExceeded_V15() {
        MenuConfig config = new MenuConfig();
        RequirementList openReq = new RequirementList();
        openReq.addRequirement("req1", new Requirement("has permission"));
        openReq.setMinimumRequirements(5);
        config.setOpenRequirement(openReq);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-15") && i.level() == ValidationLevel.ERROR);
    }

    @Test
    void testSizeForNonChest_V16() {
        MenuConfig config = new MenuConfig();
        config.setMenuType(InventoryType.HOPPER);
        config.setSize(27);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-16") && i.level() == ValidationLevel.ERROR);
    }

    @Test
    void testMiddleClickOnNewMc_V17() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);
        item.getClickHandler().getMiddleClickCommands().add(new Action("[message]", "hi"));
        config.getItems().put("item1", item);

        context.setMcVersion("1.20");
        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-17") && i.level() == ValidationLevel.WARNING);
    }

    @Test
    void testCompositeValueFormat_V18() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "DIAMOND_SWORD");
        item.setSlot(0);
        item.getAppearance().getEnchantments().add("SHARPNESS_NOT_VALID");
        item.getAppearance().getPotionEffects().add("INVALID_POTION");
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-18") && i.fieldName().equals("enchantments"));
        assertThat(issues).anyMatch(i -> i.code().equals("V-18") && i.fieldName().equals("potion_effects"));
    }

    @Test
    void testUnknownSound_V8() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);
        item.getClickHandler().getClickCommands().add(new Action("[sound]", "NON_EXISTENT_SOUND_XYZ 1.0 1.0"));
        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-8") && i.level() == ValidationLevel.WARNING);
    }

    @Test
    void testInvalidRegex_V14() {
        MenuConfig config = new MenuConfig();
        MenuItem item = new MenuItem("item1", "STONE");
        item.setSlot(0);

        Requirement req = new Requirement("regex matches");
        req.setField("input", "%player_name%");
        req.setField("regex", "[invalid(regex"); // unclosed bracket and paren

        RequirementList reqList = new RequirementList();
        reqList.addRequirement("reg_test", req);
        item.setViewRequirement(reqList);

        config.getItems().put("item1", item);

        List<ValidationIssue> issues = engine.validate(config, context);
        assertThat(issues).anyMatch(i -> i.code().equals("V-14") && i.level() == ValidationLevel.ERROR);
    }
}
