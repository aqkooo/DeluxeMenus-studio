package com.ejyqyl.dmstudio.service.render;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ColorCodeParserTest {

    @Test
    void testStripColor() {
        String raw = "&aShop &cMenu &#55ff55Title &lBold";
        String stripped = ColorCodeParser.stripColor(raw);
        assertThat(stripped).isEqualTo("Shop Menu Title Bold");
        assertThat(ColorCodeParser.stripColor(null)).isEmpty();
    }

    @Test
    void testParseToNodes() {
        String input = "&aHello &cWorld &#ffaa00Gold";
        List<Text> nodes = ColorCodeParser.parseToNodes(input);

        assertThat(nodes).hasSize(3);
        assertThat(nodes.get(0).getText()).isEqualTo("Hello ");
        assertThat(nodes.get(0).getFill()).isEqualTo(Color.web("#55FF55"));

        assertThat(nodes.get(1).getText()).isEqualTo("World ");
        assertThat(nodes.get(1).getFill()).isEqualTo(Color.web("#FF5555"));

        assertThat(nodes.get(2).getText()).isEqualTo("Gold");
        assertThat(nodes.get(2).getFill()).isEqualTo(Color.web("#FFAA00"));
    }

    @Test
    void testAllFormattingModifiers() {
        String input = "&lBold &oItalic &nUnderline &mStrike &rReset &0Black&1Blue&2Green&3Aqua&4Red&5Purple&6Gold&7Gray&8DarkGray&9Blue&bAqua&dPink&eYellow&fWhite";
        List<Text> nodes = ColorCodeParser.parseToNodes(input);
        assertThat(nodes).isNotEmpty();

        TextFlow flow = new TextFlow();
        ColorCodeParser.populateTextFlow(flow, input, Color.WHITE);
        assertThat(flow.getChildren()).isNotEmpty();
    }

    @Test
    void testEmptyAndNull() {
        assertThat(ColorCodeParser.parseToNodes(null)).isEmpty();
        assertThat(ColorCodeParser.parseToNodes("")).isEmpty();
    }
}
