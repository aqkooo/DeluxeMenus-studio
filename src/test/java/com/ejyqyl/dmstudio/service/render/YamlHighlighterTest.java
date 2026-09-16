package com.ejyqyl.dmstudio.service.render;

import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class YamlHighlighterTest {

    @Test
    void testComputeHighlighting() {
        String yaml = "menu_title: 'Shop'\n"
                + "size: 27\n"
                + "register_command: true\n"
                + "# Comment here\n"
                + "open_command: [console] menu\n"
                + "lore: '%player_name%'\n";

        StyleSpans<Collection<String>> spans = YamlHighlighter.computeHighlighting(yaml);
        assertNotNull(spans);
        assertTrue(spans.getSpanCount() > 0);
    }

    @Test
    void testEmptyAndNull() {
        StyleSpans<Collection<String>> spansEmpty = YamlHighlighter.computeHighlighting("");
        assertNotNull(spansEmpty);
        assertEquals(1, spansEmpty.getSpanCount());

        StyleSpans<Collection<String>> spansNull = YamlHighlighter.computeHighlighting(null);
        assertNotNull(spansNull);
        assertEquals(1, spansNull.getSpanCount());
    }
}
