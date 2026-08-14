package com.margins.rogue.system;

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
 * Story 6.2 AC-1: a readied bag wears down and, at 0 durability, breaks — its slots vanish (capacity
 * shrinks) and the overflow spills to the ground, fully recoverable (drop-or-leave, no loss).
 */
class BagDurabilityTest {

    @Test
    void aBagWearsDownAndBreaksAtZeroDurability() {
        Bag b = new Bag(Supply.TRAVELERS_PACK, 3, BagTrap.NONE);
        assertFalse(b.isBroken());
        b.decay(1);
        assertEquals(2, b.getDurability());
        assertFalse(b.isBroken());
        b.decay(5); // clamped at 0
        assertEquals(0, b.getDurability());
        assertTrue(b.isBroken());
    }

    @Test
    void breakingABagShrinksCapacityAndReturnsTheOverflowStacks() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.TRAVELERS_PACK.ordinal(), 1);
        inv.equip(Supply.TRAVELERS_PACK.ordinal());            // readied → capacity 19 + 4 = 23
        int cap = inv.mainSlotCapacity();
        assertEquals(Inventory.MAIN_BASE_SLOTS + Supply.TRAVELERS_PACK.storageSlotBonus(), cap);
        for (int i = 0; i < cap; i++) inv.tryAdd(1000 + i, 1); // fill every usable stack
        assertTrue(inv.isBackpackFull());

        Bag bag = inv.getStorageBags().get(0);
        List<int[]> overflow = inv.breakBag(bag);

        assertEquals(0, inv.storageItemCount(), "the broken bag is removed");
        assertEquals(Inventory.MAIN_BASE_SLOTS, inv.mainSlotCapacity(), "capacity shrank back to the base");
        assertEquals(cap - Inventory.MAIN_BASE_SLOTS, overflow.size(),
                "the stacks beyond the shrunk capacity are the overflow (4 slots)");
    }

    @Test
    void aWornBagBreaksOnAHitAndSpillsEverythingRecoverable() {
        RunState s = new RunState(3L);
        Inventory inv = s.getInventory();                       // Klein starts wearing his pack → capacity 23
        int cap = inv.mainSlotCapacity();
        for (int i = 0; i < cap; i++) inv.tryAdd(1000 + i, 1);  // fill it so 4 stacks overflow on break

        Bag bag = inv.getStorageBags().get(0);
        bag.decay(bag.getDurability() - 1);                    // one hit from breaking
        int floorBefore = s.getFloorItems().size();

        BagSystem.onPlayerHit(s, new ArrayList<>());            // decays 1 → 0 → break → spill 100%

        assertEquals(0, inv.storageItemCount(), "the worn-out pack broke");
        assertEquals(Inventory.MAIN_BASE_SLOTS, inv.mainSlotCapacity(), "capacity shrank to the base");
        assertEquals(floorBefore + (cap - Inventory.MAIN_BASE_SLOTS), s.getFloorItems().size(),
                "every overflow stack spilled to the ground — a durability break loses nothing");
    }

    @Test
    void anIntactBagJustWearsOnAHitWithoutBreaking() {
        RunState s = new RunState(3L);
        Bag bag = s.getInventory().getStorageBags().get(0);
        int before = bag.getDurability();
        BagSystem.onPlayerHit(s, new ArrayList<>());
        assertEquals(before - BagSystem.BAG_DECAY_PER_HIT, bag.getDurability(), "one hit wears one durability");
        assertEquals(1, s.getInventory().storageItemCount(), "an intact bag stays readied");
    }
}
