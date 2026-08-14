package com.margins;

import com.margins.rogue.item.Supply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarginScreenInventoryHudTest {

    @Test
    void cursorWrapsAcrossTheFourColumnGrid() {
        assertEquals(3, MarginScreen.moveInventoryCursor(0, -1, 0));
        assertEquals(0, MarginScreen.moveInventoryCursor(3, 1, 0));
        assertEquals(7, MarginScreen.moveInventoryCursor(4, -1, 0));
        assertEquals(4, MarginScreen.moveInventoryCursor(7, 1, 0));
    }

    @Test
    void cursorWrapsAcrossTheFiveRowGridWithoutChangingColumn() {
        // Story 6.1: the main store is 19 base slots → a 5-row, 4-column grid.
        assertEquals(5, MarginScreen.moveInventoryCursor(1, 0, 1));   // row 0 → row 1
        assertEquals(9, MarginScreen.moveInventoryCursor(5, 0, 1));   // row 1 → row 2
        assertEquals(17, MarginScreen.moveInventoryCursor(1, 0, -1)); // up from the top row wraps to the last row
        assertEquals(1, MarginScreen.moveInventoryCursor(17, 0, 1));  // down from the last row wraps to the top
    }

    @Test
    void inventoryUseIncludesProvisionsDocumentsAndMysteryConsumables() {
        assertTrue(MarginScreen.canUseFromInventory(Supply.COOKED_MEAT));
        assertTrue(MarginScreen.canUseFromInventory(Supply.TORN_PAGE));
        assertTrue(MarginScreen.canUseFromInventory(Supply.WRAPPED_BUNDLE));

        assertFalse(MarginScreen.canUseFromInventory(Supply.COAL));
        assertFalse(MarginScreen.canUseFromInventory(Supply.WOOD));
        assertFalse(MarginScreen.canUseFromInventory(Supply.SALT));
        assertFalse(MarginScreen.canUseFromInventory(Supply.SEALED_LETTER));
    }

    @Test
    void rottenMeatUsesAMeatSpriteRatherThanTheCheeseCell() {
        int meatIcon = MarginScreen.iconFor(Supply.RAW_MEAT.ordinal());
        assertEquals(meatIcon, MarginScreen.iconFor(Supply.HALF_ROTTEN_MEAT.ordinal()));
        assertEquals(meatIcon, MarginScreen.iconFor(Supply.SPOILED_MEAT.ordinal()));
    }

    @Test
    void menuSelectionWrapsInBothDirections() {
        assertEquals(3, MarginScreen.moveMenuSelection(0, -1, 4));
        assertEquals(0, MarginScreen.moveMenuSelection(3, 1, 4));
        assertEquals(0, MarginScreen.moveMenuSelection(7, 1, 0));
    }
}
