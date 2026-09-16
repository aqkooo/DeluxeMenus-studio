package com.ejyqyl.dmstudio.service.simulator;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.service.simulator.MenuSimulatorEngine.SimulationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MenuSimulatorEngineTest {

    @Test
    void testResolveVisibleItemByPriorityAndViewRequirement() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        // Item 1: Priority 0 (Highest precedence), but requires permission 'group.vip'
        MenuItem vipItem = new MenuItem("vip_btn", "DIAMOND");
        vipItem.setSlot(10);
        vipItem.setPriority(0);
        Requirement vipReq = new Requirement("has permission");
        vipReq.setPermission("group.vip");
        RequirementList viewList = new RequirementList();
        viewList.getRequirements().put("check_vip", vipReq);
        vipItem.setViewRequirement(viewList);

        // Item 2: Priority 1 (Lower precedence, fallback for non-VIP)
        MenuItem defaultItem = new MenuItem("default_btn", "COAL");
        defaultItem.setSlot(10);
        defaultItem.setPriority(1);

        config.getItems().put("vip_btn", vipItem);
        config.getItems().put("default_btn", defaultItem);

        SimulatedPlayerState player = new SimulatedPlayerState();

        // 1. Player without 'group.vip' should see fallback (COAL)
        MenuSimulatorEngine engineNonVip = new MenuSimulatorEngine(config, player);
        MenuItem visible1 = engineNonVip.resolveVisibleItem(10);
        assertNotNull(visible1);
        assertEquals("default_btn", visible1.getKey());

        // 2. Grant 'group.vip' -> player should see VIP item (DIAMOND)
        player.addPermission("group.vip");
        MenuSimulatorEngine engineVip = new MenuSimulatorEngine(config, player);
        MenuItem visible2 = engineVip.resolveVisibleItem(10);
        assertNotNull(visible2);
        assertEquals("vip_btn", visible2.getKey());
    }

    @Test
    void testSimulatedClickWithMoneyRequirement() {
        MenuConfig config = new MenuConfig();
        config.setSize(27);

        MenuItem shopItem = new MenuItem("buy_sword", "IRON_SWORD");
        shopItem.setSlot(13);

        Requirement moneyReq = new Requirement("has money");
        moneyReq.setAmount(250.0);

        RequirementList clickReq = new RequirementList();
        clickReq.getRequirements().put("check_money", moneyReq);
        clickReq.getDenyCommands().add(Action.parse("[message] &cНедостаточно денег!"));

        ClickHandler handler = new ClickHandler();
        handler.setClickRequirement(clickReq);
        handler.getLeftClickCommands().add(Action.parse("[take_money] 250"));
        handler.getLeftClickCommands().add(Action.parse("[message] &aПокупка успешна!"));
        shopItem.setClickHandlers(handler);

        config.getItems().put("buy_sword", shopItem);

        // 1. Poor player ($100) -> Denied
        SimulatedPlayerState poorPlayer = new SimulatedPlayerState();
        poorPlayer.setVaultBalance(100.0);
        MenuSimulatorEngine engine1 = new MenuSimulatorEngine(config, poorPlayer);

        SimulationResult resDenied = engine1.handleClick(13, true);
        assertFalse(resDenied.success());
        assertEquals(100.0, poorPlayer.getVaultBalance());
        assertTrue(resDenied.messages().get(0).contains("Недостаточно денег"));

        // 2. Rich player ($500) -> Accepted and money deducted
        SimulatedPlayerState richPlayer = new SimulatedPlayerState();
        richPlayer.setVaultBalance(500.0);
        MenuSimulatorEngine engine2 = new MenuSimulatorEngine(config, richPlayer);

        SimulationResult resAccepted = engine2.handleClick(13, true);
        assertTrue(resAccepted.success());
        assertEquals(250.0, richPlayer.getVaultBalance());
        assertTrue(resAccepted.messages().get(0).contains("Покупка успешна"));
        assertTrue(resAccepted.shouldRefresh());
    }
}
