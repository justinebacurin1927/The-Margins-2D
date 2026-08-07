package com.margins.rogue;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 1.8 Task 5 (Story 1.7 deferral F-09): {@code Inventory.nextOccupiedStack} — the
 * backpack-selection seam the screen cycles with. Replaces the E-key {@code firstWhere()} stopgap:
 * an item is chosen deliberately by slot, never by first-match.
 */
class InventorySelectionTest {

    @Test
    void skipsEmptySlotsBetweenSelections() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);     // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);     // slot 1
        inv.tryAdd(Supply.RAW_MEAT.ordinal(), 1); // slot 2
        inv.remove(Supply.COAL.ordinal(), 1);     // slot 1 freed — a hole
        assertEquals(2, inv.nextOccupiedStack(0), "the cycle skips the empty slot 1");
        assertEquals(2, inv.nextOccupiedStack(1), "from an empty slot it lands on the next occupied");
    }

    @Test
    void wrapsPastTheEndToTheFirstOccupied() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);        // 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);        // 1
        inv.tryAdd(Supply.RAW_MEAT.ordinal(), 1);    // 2
        inv.tryAdd(Supply.COOKED_MEAT.ordinal(), 1); // 3
        inv.remove(Supply.WOOD.ordinal(), 1);        // 0 freed
        inv.remove(Supply.RAW_MEAT.ordinal(), 1);    // 2 freed → occupied {1, 3}
        assertEquals(1, inv.nextOccupiedStack(3), "from the last occupied stack, the cycle wraps to the first");
    }

    @Test
    void returnsMinusOneWhenTheBackpackIsEmpty() {
        assertEquals(-1, new Inventory().nextOccupiedStack(0), "nothing to select in an empty backpack");
    }

    @Test
    void startsStrictlyAfterTheCurrentSelection() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1); // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1); // slot 1
        assertEquals(1, inv.nextOccupiedStack(0), "the first cycle step is strictly after the selection");
    }

    @Test
    void singleStackWrapsBackToItselfInOneLap() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1); // slot 0
        assertEquals(0, inv.nextOccupiedStack(0), "with one stack, a full lap returns to it");
    }

    @Test
    void freshSelectionLandsOnTheFirstOccupied() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);   // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);   // slot 1
        inv.remove(Supply.WOOD.ordinal(), 1);   // slot 0 freed → occupied {1}
        assertEquals(1, inv.nextOccupiedStack(-1), "a fresh (no-selection) cycle starts at the first occupied");
    }

    // --- Backward selection (review finding): the old screen backward = 7 forward steps, which
    // only equals "back" when the occupied count divides 8. previousOccupiedStack is the true
    // mirror — symmetric on any occupied count. ---

    @Test
    void backwardStepsToThePreviousOccupiedStack() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);        // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);        // slot 1
        inv.tryAdd(Supply.RAW_MEAT.ordinal(), 1);    // slot 2
        inv.remove(Supply.COAL.ordinal(), 1);        // slot 1 freed → occupied {0, 2}
        assertEquals(0, inv.previousOccupiedStack(2), "backward from the last stack lands on the one before it");
    }

    @Test
    void backwardWrapsPastTheStartToTheLastOccupied() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);        // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);        // slot 1
        inv.tryAdd(Supply.RAW_MEAT.ordinal(), 1);    // slot 2
        inv.remove(Supply.WOOD.ordinal(), 1);        // slot 0 freed → occupied {1, 2}
        assertEquals(2, inv.previousOccupiedStack(1), "backward from the first occupied wraps to the last");
    }

    @Test
    void backwardIsTheTrueMirrorOfForwardOnThreeOccupiedStacks() {
        // The bug in numbers: on {0,1,2}, 7 forward steps from 2 land on 0, not the true back (1).
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);     // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);     // slot 1
        inv.tryAdd(Supply.RAW_MEAT.ordinal(), 1); // slot 2
        assertEquals(1, inv.previousOccupiedStack(2), "backward from the last stack is the one before it");
        assertEquals(0, inv.previousOccupiedStack(1), "and continues the ring backward");
        assertEquals(2, inv.previousOccupiedStack(0), "wrapping to the last stack past the start");
        assertEquals(2, inv.nextOccupiedStack(1), "the forward mirror steps back up — [ then ] returns");
    }

    @Test
    void backwardFromFreshSelectionLandsOnTheLastOccupied() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);   // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);   // slot 1
        inv.tryAdd(Supply.RAW_MEAT.ordinal(), 1); // slot 2
        assertEquals(2, inv.previousOccupiedStack(-1), "a fresh backward press starts at the last stack");
    }

    @Test
    void neverThrowsOnNegativeFrom() {
        // Edge-review finding: (from + step) % 8 with a negative dividend produced a negative index
        // and an AIOOBE. floorMod keeps every step a valid slot, forward and back.
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 1);   // slot 0
        inv.tryAdd(Supply.COAL.ordinal(), 1);   // slot 1
        assertEquals(0, inv.nextOccupiedStack(-2), "a deeply negative from still lands on the first occupied");
        assertEquals(1, inv.previousOccupiedStack(-9), "and the backward mirror stays a valid slot (-9 ≡ ring slot 7)");
        assertEquals(-1, new Inventory().nextOccupiedStack(-3), "an empty backpack still reports nothing");
    }
}
