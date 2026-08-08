package com.margins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarginScreenStructureLayerTest {

    @Test
    void exteriorApronDoesNotCountAsIndoorGrounding() {
        assertFalse(MarginScreen.isOldHouseInterior(0));
        assertFalse(MarginScreen.isOldHouseInterior(14));
        assertFalse(MarginScreen.isOldHouseInterior(9 * 15 + 8));
        assertFalse(MarginScreen.isOldHouseInterior(4 * 15));
    }

    @Test
    void insetHouseBodyCountsAsIndoorGrounding() {
        assertTrue(MarginScreen.isOldHouseInterior(1 * 15 + 1));
        assertTrue(MarginScreen.isOldHouseInterior(4 * 15 + 8));
        assertTrue(MarginScreen.isOldHouseInterior(8 * 15 + 13));
    }
}
