package com.ejyqyl.dmstudio.service.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DiffEngineTest {

    @Test
    void testIdenticalContent() {
        String yaml = "menu_title: 'Test'\nsize: 27\nitems:\n  item_1:\n    material: STONE";
        DiffEngine.DiffReport res = DiffEngine.compare(yaml, yaml);
        assertFalse(res.hasChanges());
        assertEquals(0, res.addedCount());
        assertEquals(0, res.removedCount());
        assertTrue(res.unchangedCount() > 0);
        assertTrue(res.lines().stream().allMatch(l -> l.type() == DiffEngine.DiffType.UNCHANGED));
    }

    @Test
    void testAddedLines() {
        String oldYaml = "size: 27\n";
        String newYaml = "size: 27\nitems:\n  test:\n    material: DIAMOND\n";
        DiffEngine.DiffReport res = DiffEngine.compare(oldYaml, newYaml);
        assertTrue(res.hasChanges());
        assertTrue(res.addedCount() > 0);
        assertEquals(0, res.removedCount());
    }

    @Test
    void testRemovedLines() {
        String oldYaml = "size: 27\nitems:\n  test:\n    material: DIAMOND\n";
        String newYaml = "size: 27\n";
        DiffEngine.DiffReport res = DiffEngine.compare(oldYaml, newYaml);
        assertTrue(res.hasChanges());
        assertEquals(0, res.addedCount());
        assertTrue(res.removedCount() > 0);
    }

    @Test
    void testModifiedLines() {
        String oldYaml = "line1\nline2\nline3";
        String newYaml = "line1\nline2_modified\nline3";
        DiffEngine.DiffReport res = DiffEngine.compare(oldYaml, newYaml);
        assertTrue(res.hasChanges());
        assertEquals(1, res.removedCount());
        assertEquals(1, res.addedCount());
        assertEquals(2, res.unchangedCount());
    }

    @Test
    void testNullAndEmptyInputs() {
        DiffEngine.DiffReport res = DiffEngine.compare(null, "");
        assertFalse(res.hasChanges());

        DiffEngine.DiffReport res2 = DiffEngine.compare("a\n", null);
        assertTrue(res2.hasChanges());
        assertEquals(1, res2.removedCount());
    }
}
