package com.margins.rogue;

import com.margins.rogue.item.Inventory;
import com.margins.rogue.item.Supply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 6.1 (FR-20): the hybrid slot + weight inventory. AC-1 — a 19-slot main store expandable by
 * readied storage bags, plus always-available Quick-Access gear/artifact slots. AC-2 — STR-scaled
 * weight capacity with encumbrance.
 */
class InventoryCapacityTest {

    // A distinct main-store filler type per slot (opaque non-negative ids, never the bag ordinal).
    private static int filler(int i) {
        int t = i;
        return t >= Supply.TRAVELERS_PACK.ordinal() ? t + 1 : t;
    }

    // --- AC-1: main-store slots + storage expansion ---

    @Test
    void mainStoreHolds19BaseStacksBeforeAnyBag() {
        Inventory inv = new Inventory();
        assertEquals(19, Inventory.MAIN_BASE_SLOTS);
        assertEquals(Inventory.MAIN_BASE_SLOTS, inv.mainSlotCapacity(), "no bag readied → base capacity");
        for (int i = 0; i < Inventory.MAIN_BASE_SLOTS; i++) {
            assertEquals(Inventory.AddResult.ADDED, inv.tryAdd(filler(i), 1), "each of the 19 base stacks lands");
        }
        assertTrue(inv.isBackpackFull(), "19 distinct stacks fill the base store");
        assertEquals(Inventory.AddResult.BACKPACK_FULL, inv.tryAdd(filler(99), 1),
                "the 20th distinct stack is refused at base capacity (nothing lost)");
    }

    @Test
    void readyingAStorageBagRaisesMainCapacityByItsMergedBonus() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.TRAVELERS_PACK.ordinal(), 1);
        int base = inv.mainSlotCapacity();
        assertTrue(inv.equip(Supply.TRAVELERS_PACK.ordinal()), "the bag readies into a storage slot");
        assertEquals(1, inv.storageItemCount(), "one bag is readied");
        assertEquals(base + Supply.TRAVELERS_PACK.storageSlotBonus(), inv.mainSlotCapacity(),
                "the readied bag expands the main store by its slot bonus");
    }

    @Test
    void expandedCapacityLetsTheStoreAcceptMoreStacks() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.TRAVELERS_PACK.ordinal(), 1);
        inv.equip(Supply.TRAVELERS_PACK.ordinal()); // +4 slots → 23
        int cap = inv.mainSlotCapacity();
        for (int i = 0; i < cap; i++) {
            assertEquals(Inventory.AddResult.ADDED, inv.tryAdd(filler(i), 1), "each stack lands up to the expanded capacity");
        }
        assertTrue(inv.isBackpackFull(), "the expanded store fills at its raised capacity");
        assertEquals(Inventory.AddResult.BACKPACK_FULL, inv.tryAdd(filler(cap), 1),
                "tryAdd refuses only at the CURRENT (expanded) capacity, not the fixed base");
    }

    @Test
    void storageItemsAreCappedAtTheMaxBagCount() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.TRAVELERS_PACK.ordinal(), Inventory.MAX_STORAGE_ITEMS + 2);
        for (int i = 0; i < Inventory.MAX_STORAGE_ITEMS; i++) {
            assertTrue(inv.equip(Supply.TRAVELERS_PACK.ordinal()), "bag " + (i + 1) + " readies");
        }
        assertEquals(Inventory.MAX_STORAGE_ITEMS, inv.storageItemCount(), "readied bags cap at MAX_STORAGE_ITEMS");
        assertFalse(inv.equip(Supply.TRAVELERS_PACK.ordinal()), "a bag past the cap is refused (all storage slots full)");
        assertEquals(Inventory.MAIN_BASE_SLOTS + Inventory.MAX_STORAGE_ITEMS * Supply.TRAVELERS_PACK.storageSlotBonus(),
                inv.mainSlotCapacity(), "the merged bonus is the sum of all readied bags");
    }

    @Test
    void quickAccessRoutesGearToGearSlotsAndArtifactSlotsStayAvailable() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.FOLDED_CLOTH.ordinal(), 1); // isQuickGear
        assertTrue(inv.equip(Supply.FOLDED_CLOTH.ordinal()), "a gear item readies");
        assertEquals(Supply.FOLDED_CLOTH.ordinal(), inv.equippedType(0), "gear routes into a gear slot");
        assertEquals(0, inv.count(Supply.FOLDED_CLOTH.ordinal()), "readying consumes it from the main store");
        // The 3 artifact slots are always available even though no current supply fills them (AC-1).
        for (int s = 0; s < Inventory.QUICK_ARTIFACT_SLOTS; s++) {
            assertEquals(-1, inv.quickArtifactType(s), "artifact slots exist and start empty");
        }
        assertEquals(3, Inventory.QUICK_ARTIFACT_SLOTS);
        assertEquals(5, Inventory.QUICK_GEAR_SLOTS);
    }

    @Test
    void theSelectionRingStillWrapsAtTheNewSlotCount() {
        // The count-agnostic ring finds occupied stacks regardless of the larger store.
        Inventory inv = new Inventory();
        inv.tryAdd(filler(0), 1); // slot 0
        inv.tryAdd(filler(1), 1); // slot 1
        assertEquals(1, inv.nextOccupiedStack(0), "forward steps to the next occupied");
        assertEquals(0, inv.nextOccupiedStack(1), "and wraps the whole ring back to the first");
        assertEquals(0, inv.previousOccupiedStack(1), "backward is the true mirror");
    }

    // --- AC-2: weight, STR capacity, encumbrance ---

    @Test
    void totalWeightSumsCountTimesPerItemWeight() {
        Inventory inv = new Inventory();
        inv.tryAdd(Supply.WOOD.ordinal(), 3);          // weight 2 each → 6
        inv.tryAdd(Supply.SEALED_WATERSKIN.ordinal(), 2); // weight 1 each → 2
        assertEquals(3 * Supply.WOOD.weight() + 2 * Supply.SEALED_WATERSKIN.weight(), inv.totalWeight());
    }

    @Test
    void carryCapacityScalesWithStr() {
        Inventory inv = new Inventory();
        assertEquals(Inventory.BASE_CAPACITY + 5 * Inventory.STR_CAPACITY_FACTOR, inv.carryCapacity(5));
        assertTrue(inv.carryCapacity(8) > inv.carryCapacity(5), "more STR → more capacity");
    }

    @Test
    void isEncumberedFlipsExactlyAtCapacity() {
        Inventory inv = new Inventory();
        int str = 5;
        int cap = inv.carryCapacity(str); // 10 + 20 = 30
        // One stack of weight-1 items (opaque id → weight 1) counted up to exactly capacity — no need
        // for many slots. At capacity is not over; one more onto the same stack tips it over.
        inv.tryAdd(2000, cap);
        assertEquals(cap, inv.totalWeight(), "weight sits exactly at capacity");
        assertFalse(inv.isEncumbered(str), "at capacity is not over");
        inv.tryAdd(2000, 1);
        assertEquals(cap + 1, inv.totalWeight());
        assertTrue(inv.isEncumbered(str), "one past capacity is encumbered");
    }

    @Test
    void capacityDropsWithTheBacterialStrPenaltySoEncumbranceCanTighten() {
        // AC-2 reads RoguePlayer.getStr(), which folds the Story-1.7 STR penalty — a weakened Klein
        // is encumbered sooner at the same load.
        RoguePlayer healthy = new RoguePlayer(0, 0, null);
        int healthyStr = healthy.getStr(); // base 5
        RoguePlayer sick = new RoguePlayer(0, 0, null);
        sick.beginBacterial(); // Nausea → STR ×0.70
        int sickStr = sick.getStr();
        assertTrue(sickStr < healthyStr, "Nausea lowers effective STR");

        Inventory inv = new Inventory();
        assertTrue(inv.carryCapacity(sickStr) < inv.carryCapacity(healthyStr),
                "the lower effective STR yields a lower carry capacity");
    }
}
