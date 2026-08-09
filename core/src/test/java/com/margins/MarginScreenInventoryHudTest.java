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
    void cursorWrapsBetweenTheTwoRowsWithoutChangingColumn() {
        assertEquals(5, MarginScreen.moveInventoryCursor(1, 0, 1));
        assertEquals(1, MarginScreen.moveInventoryCursor(5, 0, 1));
        assertEquals(1, MarginScreen.moveInventoryCursor(5, 0, -1));
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
