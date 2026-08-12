package com.margins.rogue;

import com.margins.rogue.item.Supply;
import com.margins.rogue.item.Weapon;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.RepairSystem;
import com.margins.rogue.system.TurnEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.5 (FR-13, AD-13): the gear economy — SKILL-based repair consumes materials, scavenge-on-break
 * returns tier-scaled materials. The loop is lossy (the 6-repair ceiling caps a weapon's life), so no
 * infinite-gear path exists (AC-3 / AD-17).
 */
class RepairSystemTest {

    private static final int WOOD = Supply.WOOD.ordinal();
    private static final int ROPE = Supply.ROPE.ordinal();
    private static final int TOOLS = Supply.SMALL_TOOLS.ordinal();

    private RunState wieldedSpear() {
        RunState s = new RunState(1L);
        s.getWeapons().clear();
        s.getWeapons().add(new Weapon(Weapon.Category.SPEAR, 1, 2, 20));
        s.wieldNext(); // wield index 0
        return s;
    }

    @Test
    void repairConsumesMaterialsAndPermanentlyLowersMax() {
        RunState s = wieldedSpear();
        Weapon w = s.getWieldedWeapon();
        w.decay(10); // damaged: 10/20
        s.getInventory().tryAdd(WOOD, 1);
        s.getInventory().tryAdd(ROPE, 1);

        assertTrue(RepairSystem.repair(s, new ArrayList<>()));
        assertEquals(0, s.getInventory().count(WOOD), "Wood consumed");
        assertEquals(0, s.getInventory().count(ROPE), "Rope consumed");
        assertEquals(1, w.getRepairCount());
        assertTrue(w.getMaxDurability() < 20, "max permanently lowered (AD-13)");
        assertEquals(w.getMaxDurability(), w.getDurability(), "restored to the new max");
    }

    @Test
    void repairRefusedWithoutMaterials() {
        RunState s = wieldedSpear();
        s.getWieldedWeapon().decay(10);
        assertFalse(RepairSystem.repair(s, new ArrayList<>()));
        assertEquals(0, s.getWieldedWeapon().getRepairCount(), "no repair, no ceiling loss");
    }

    @Test
    void repairRefusedWhenUnarmed() {
        RunState s = new RunState(1L);
        s.getWeapons().clear();
        assertFalse(RepairSystem.repair(s, new ArrayList<>()));
    }

    @Test
    void repairRefusedWhenNotDamagedSoNoMaterialsWasted() {
        RunState s = wieldedSpear(); // full durability
        s.getInventory().tryAdd(WOOD, 1);
        s.getInventory().tryAdd(ROPE, 1);
        assertFalse(RepairSystem.repair(s, new ArrayList<>()), "a full weapon doesn't need mending");
        assertEquals(1, s.getInventory().count(WOOD), "materials not wasted on a no-op repair");
        assertEquals(0, s.getWieldedWeapon().getRepairCount(), "ceiling not spent");
    }

    @Test
    void repairRefusedBeyondRepair() {
        RunState s = wieldedSpear();
        Weapon w = s.getWieldedWeapon();
        for (int i = 0; i < Weapon.MAX_REPAIRS; i++) w.repair(5); // exhaust the 5 repairs
        w.decay(1);
        s.getInventory().tryAdd(WOOD, 1);
        s.getInventory().tryAdd(ROPE, 1);
        assertFalse(RepairSystem.repair(s, new ArrayList<>()), "the 6th is beyond repair");
        assertEquals(1, s.getInventory().count(WOOD), "no materials consumed on a refused repair");
    }

    @Test
    void scavengeYieldsTierMaterialsAndRemovesTheWeapon() {
        RunState s = new RunState(1L);
        s.getWeapons().clear();
        Weapon blade = new Weapon(Weapon.Category.BLADE, 3, 4, 5);
        blade.decay(5); // broken
        s.getWeapons().add(blade);

        assertTrue(RepairSystem.scavenge(s, new ArrayList<>()));
        assertEquals(2, s.getInventory().count(TOOLS), "a T3 blade yields 2 Small Tools (D2)");
        assertTrue(s.getWeapons().isEmpty(), "the stripped weapon is removed");
    }

    @Test
    void scavengeKeepsTheWieldedIndexValid() {
        RunState s = new RunState(1L);
        s.getWeapons().clear();
        Weapon broken = new Weapon(Weapon.Category.SPEAR, 1, 2, 1);
        broken.decay(1); // broken at index 0
        Weapon good = new Weapon(Weapon.Category.BLADE, 3, 4, 40);
        s.getWeapons().add(broken);
        s.getWeapons().add(good);
        s.setWieldedIndex(1); // wield the good one

        assertTrue(RepairSystem.scavenge(s, new ArrayList<>())); // removes index 0
        assertSame(good, s.getWieldedWeapon(), "the wielded weapon survives; its index shifted down");
        assertEquals(0, s.getWieldedIndex());
    }

    @Test
    void scavengeRefusedWhenNothingBroken() {
        RunState s = wieldedSpear(); // full, not broken
        assertFalse(RepairSystem.scavenge(s, new ArrayList<>()));
        assertEquals(1, s.getWeapons().size(), "nothing removed");
    }

    @Test
    void repairAndScavengeAreReachableViaTurnEngine() {
        RunState s = wieldedSpear();
        s.getWieldedWeapon().decay(10);
        s.getInventory().tryAdd(WOOD, 1);
        s.getInventory().tryAdd(ROPE, 1);
        new TurnEngine().advance(s, PlayerAction.repair(RoguePlayer.NORTH));
        assertEquals(1, s.getWieldedWeapon().getRepairCount(), "REPAIR action mends via TurnEngine");

        // break it, then scavenge through the engine
        Weapon w = s.getWieldedWeapon();
        w.decay(w.getDurability());
        s.breakWielded();
        int woodBefore = s.getInventory().count(WOOD);
        new TurnEngine().advance(s, PlayerAction.scavenge(RoguePlayer.NORTH));
        assertTrue(s.getWeapons().isEmpty(), "SCAVENGE action strips via TurnEngine");
        assertEquals(woodBefore + 1, s.getInventory().count(WOOD), "T1 spear yields 1 Wood");
    }
}
