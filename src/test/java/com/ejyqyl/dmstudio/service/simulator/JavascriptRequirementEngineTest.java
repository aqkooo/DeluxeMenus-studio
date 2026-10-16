package com.ejyqyl.dmstudio.service.simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JavascriptRequirementEngineTest {

    private JavascriptRequirementEngine engine;
    private SimulatedPlayerState player;

    @BeforeEach
    void setUp() {
        engine = new JavascriptRequirementEngine();
        player = new SimulatedPlayerState();
        player.setPlayerName("Player1");
        player.setVaultBalance(500.0);
        player.setPlaceholder("player_level", "42");
        player.setPlaceholder("rank", "VIP");
    }

    @Test
    void testBasicNumericComparisons() {
        assertTrue(JavascriptRequirementEngine.evaluate("10 > 5", player));
        assertTrue(JavascriptRequirementEngine.evaluate("10 >= 10", player));
        assertTrue(JavascriptRequirementEngine.evaluate("5 < 10", player));
        assertTrue(JavascriptRequirementEngine.evaluate("5 <= 5", player));
        assertTrue(JavascriptRequirementEngine.evaluate("100 == 100", player));
        assertTrue(JavascriptRequirementEngine.evaluate("100 != 200", player));

        assertFalse(JavascriptRequirementEngine.evaluate("5 > 10", player));
        assertFalse(JavascriptRequirementEngine.evaluate("10 < 5", player));
        assertFalse(JavascriptRequirementEngine.evaluate("5 == 6", player));
    }

    @Test
    void testPrefixHandling() {
        assertTrue(JavascriptRequirementEngine.evaluate("javascript: 50 > 20", player));
        assertTrue(JavascriptRequirementEngine.evaluate("js: 100 == 100", player));
    }

    @Test
    void testPlaceholderResolution() {
        assertTrue(JavascriptRequirementEngine.evaluate("%player_level% > 40", player));
        assertFalse(JavascriptRequirementEngine.evaluate("%player_level% > 50", player));
        assertTrue(JavascriptRequirementEngine.evaluate("%vault_eco_balance% >= 500", player));
        assertFalse(JavascriptRequirementEngine.evaluate("%vault_eco_balance% > 1000", player));
    }

    @Test
    void testStringComparisons() {
        assertTrue(JavascriptRequirementEngine.evaluate("'VIP' == 'VIP'", player));
        assertTrue(JavascriptRequirementEngine.evaluate("\"VIP\" == \"VIP\"", player));
        assertTrue(JavascriptRequirementEngine.evaluate("'%rank%' == 'VIP'", player));
        assertFalse(JavascriptRequirementEngine.evaluate("'%rank%' == 'ADMIN'", player));
        assertTrue(JavascriptRequirementEngine.evaluate("'%rank%' != 'GUEST'", player));
    }

    @Test
    void testLogicalCombinations() {
        assertTrue(JavascriptRequirementEngine.evaluate("10 > 5 && 20 > 10", player));
        assertFalse(JavascriptRequirementEngine.evaluate("10 > 5 && 20 < 10", player));
        assertTrue(JavascriptRequirementEngine.evaluate("10 < 5 || 20 > 10", player));
        assertFalse(JavascriptRequirementEngine.evaluate("10 < 5 || 20 < 10", player));
        assertTrue(JavascriptRequirementEngine.evaluate("!(10 < 5)", player));
    }

    @Test
    void testInvalidExpressionsAndEdgeCases() {
        assertFalse(JavascriptRequirementEngine.evaluate(null, player));
        assertFalse(JavascriptRequirementEngine.evaluate("", player));
        assertFalse(JavascriptRequirementEngine.evaluate("   ", player));
        assertFalse(JavascriptRequirementEngine.evaluate("invalid syntax !!! @@", player));
        assertFalse(JavascriptRequirementEngine.evaluate("10 >", player));
    }
}
