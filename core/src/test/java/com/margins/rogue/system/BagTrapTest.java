package com.margins.rogue.system;

import com.margins.rogue.RoguePlayer;
import com.margins.rogue.item.Bag;
import com.margins.rogue.item.BagTrap;
import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 6.2 AC-2: a trapped bag (dart/fire/freeze) fires its effect, then breaks — 75% of its
 * contents drop (recoverable), 25% are lost.
 */
class BagTrapTest {

    // --- Each trap's effect (reuses the existing player-harm surface) ---

    @Test
    void dartTrapDealsRawDamage() {
        RoguePlayer p = new RoguePlayer(0, 0, null);
        int hp = p.getHp();
        List<String> messages = new ArrayList<>();
        BagTrap.DART.fire(p, messages);
        assertTrue(p.getHp() < hp, "the dart wounds Klein");
        assertFalse(messages.isEmpty(), "the trap announces itself (observation discipline)");
    }

    @Test
    void fireTrapHeatsAndBurns() {
        RoguePlayer p = new RoguePlayer(0, 0, null);
        int hp = p.getHp();
        int temp = p.getTemperature();
        BagTrap.FIRE.fire(p, new ArrayList<>());
        assertTrue(p.getTemperature() > temp, "the flare drives the exposure meter up");
        assertTrue(p.getHp() < hp, "and burns");
    }

    @Test
    void freezeTrapChills() {
        RoguePlayer p = new RoguePlayer(0, 0, null);
        int temp = p.getTemperature();
        BagTrap.FREEZE.fire(p, new ArrayList<>());
        assertTrue(p.getTemperature() < temp, "the frost drives the exposure meter down");
    }

    @Test
    void anUntrappedBagNeverFires() {
        RoguePlayer p = new RoguePlayer(0, 0, null);
        int hp = p.getHp();
        List<String> messages = new ArrayList<>();
        BagTrap.NONE.fire(p, messages);
        assertEquals(hp, p.getHp(), "NONE is inert");
        assertTrue(messages.isEmpty(), "and silent");
    }

    // --- The break + 75/25 spill (AC-2) ---

    @Test
    void aSprungTrapBreaksTheBagAndLoses25PercentOfTheOverflow() {
        RunState s = new RunState(4L);
        Inventory inv = s.getInventory();                          // starting untrapped bag → capacity 23
        // Ready a second, TRAPPED found bag on top → capacity 27, so breaking it overflows the top 4 slots.
        inv.tryAdd(Supply.TRAVELERS_PACK.ordinal(), 1);
        assertTrue(inv.readyBagFromStore(Supply.TRAVELERS_PACK.ordinal(), BagTrap.DART));
        int cap = inv.mainSlotCapacity();
        assertEquals(Inventory.MAIN_BASE_SLOTS + 2 * Supply.TRAVELERS_PACK.storageSlotBonus(), cap);
        for (int i = 0; i < cap; i++) inv.tryAdd(2000 + i, 8);     // 8 items per stack — 75% = 6 kept, 2 lost

        Bag trapped = inv.getStorageBags().get(1);                 // the DART bag
        assertTrue(trapped.isTrapped());
        int floorBefore = s.getFloorItems().size();
        int hp = s.getPlayer().getHp();

        BagSystem.fireTrap(s, trapped, new ArrayList<>());         // deterministic (no RNG gate)

        int overflowStacks = cap - (Inventory.MAIN_BASE_SLOTS + Supply.TRAVELERS_PACK.storageSlotBonus()); // 27 → 23 = 4
        assertEquals(1, inv.storageItemCount(), "only the sprung bag broke; the starting pack remains");
        assertTrue(s.getPlayer().getHp() < hp, "the dart effect fired on the break");
        assertEquals(floorBefore + overflowStacks, s.getFloorItems().size(),
                "each overflow stack drops its recoverable remainder");
        // 75% of 8 = 6 recoverable per stack; 2 lost per stack.
        int droppedFromTrap = 0;
        for (int i = s.getFloorItems().size() - overflowStacks; i < s.getFloorItems().size(); i++) {
            droppedFromTrap += s.getFloorItems().get(i).count;
        }
        assertEquals(overflowStacks * 6, droppedFromTrap, "75% of each 8-stack (6) is recoverable on the ground");
    }

    @Test
    void aTrapBreakKeepsSmallStacksWholeInsteadOfDestroyingThem() {
        // Review fix: the loss is a floor of the LOST share, so a 1-count stack on a 75%-recoverable
        // trap loses nothing (25% of 1 floors to 0) instead of being destroyed whole.
        RunState s = new RunState(4L);
        Inventory inv = s.getInventory();                          // starting bag → capacity 23
        inv.tryAdd(Supply.TRAVELERS_PACK.ordinal(), 1);
        assertTrue(inv.readyBagFromStore(Supply.TRAVELERS_PACK.ordinal(), BagTrap.DART)); // → 27
        int cap = inv.mainSlotCapacity();
        for (int i = 0; i < cap; i++) inv.tryAdd(3000 + i, 1);     // ONE item per stack

        int overflowStacks = cap - (Inventory.MAIN_BASE_SLOTS + Supply.TRAVELERS_PACK.storageSlotBonus()); // 4
        int floorBefore = s.getFloorItems().size();
        BagSystem.fireTrap(s, inv.getStorageBags().get(1), new ArrayList<>());

        assertEquals(floorBefore + overflowStacks, s.getFloorItems().size(), "each single-item stack still drops");
        int dropped = 0;
        for (int i = s.getFloorItems().size() - overflowStacks; i < s.getFloorItems().size(); i++) {
            dropped += s.getFloorItems().get(i).count;
        }
        assertEquals(overflowStacks, dropped, "single-item stacks survive a 75% trap — floor(25% of 1) = 0 lost");
    }
}
