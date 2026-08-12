package com.margins.rogue;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.margins.rogue.item.FloorItem;
import com.margins.rogue.item.Weapon;
import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.CombatSystem;
import com.margins.rogue.system.PlayerAction;
import com.margins.rogue.system.TurnEngine;
import com.margins.rogue.system.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 4.4 (FR-13): weapons wired into a run — combat damage, per-action wear, break→unarmed, the
 * reachable WIELD action, and AD-6 persistence. Complements the pure-model {@code WeaponTest}.
 */
class WeaponDurabilityTest {

    /** Mirrors SaveService.json() incl. the new weapons element type, so the round-trip matches production. */
    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setElementType(RunState.class, "enemies", RogueEnemy.class);
        json.setElementType(RunState.class, "floorItems", FloorItem.class);
        json.setElementType(RunState.class, "companions", Companion.class);
        json.setElementType(RunState.class, "weapons", Weapon.class);
        json.setElementType(FlagStore.class, "flags", Integer.class);
        return json;
    }

    @Test
    void newRunStartsUnarmedWithTheStarterSet() {
        RunState s = new RunState(1L);
        assertEquals(-1, s.getWieldedIndex(), "unwielded by default (base-STR combat, AC-3)");
        assertNull(s.getWieldedWeapon());
        assertEquals(3, s.getWeapons().size(), "the minimal set: Spear T1, Blade T3, Bow T5");
    }

    @Test
    void unarmedDamageIsExactlyBaseStr() {
        RunState s = new RunState(1L);
        assertEquals(s.getPlayer().getStr(), CombatSystem.attackDamage(s),
                "nothing wielded → base STR (no combat regression, AC-3)");
    }

    @Test
    void wieldingAddsTheTierBonusAndBrokenRevertsToStr() {
        RunState s = new RunState(1L);
        Weapon w = s.wieldNext();
        assertNotNull(w);
        assertEquals(s.getPlayer().getStr() + w.damageBonus(), CombatSystem.attackDamage(s),
                "a wielded weapon adds its tier bonus (D3)");

        List<String> msgs = new ArrayList<>();
        int guard = 0;
        while (s.getWieldedWeapon() != null && guard++ < 10_000) {
            CombatSystem.decayWielded(s, msgs);
        }
        assertNull(s.getWieldedWeapon(), "worn to breaking → reverted to unarmed");
        assertEquals(-1, s.getWieldedIndex());
        assertTrue(msgs.stream().anyMatch(m -> m.contains("breaks")), "the break is observed once in the log");
        assertEquals(s.getPlayer().getStr(), CombatSystem.attackDamage(s), "unarmed after the break → base STR");
    }

    @Test
    void attackWearsTheWeaponEvenOnAWhiff() {
        RunState s = new RunState(1L);
        s.getEnemies().clear(); // swing into empty air
        s.wieldNext();
        int before = s.getWieldedWeapon().getDurability();
        new TurnEngine().advance(s, PlayerAction.attack(RoguePlayer.NORTH));
        assertEquals(before - Weapon.DECAY_PER_ACTION, s.getWieldedWeapon().getDurability(),
                "a swing spends durability per action (AC-1), hit or miss");
    }

    @Test
    void blockWearsTheWeapon() {
        RunState s = new RunState(1L);
        s.wieldNext();
        int before = s.getWieldedWeapon().getDurability();
        new TurnEngine().advance(s, PlayerAction.block(RoguePlayer.NORTH));
        assertEquals(before - Weapon.DECAY_PER_ACTION, s.getWieldedWeapon().getDurability(),
                "a block wears the weapon too (AC-1)");
    }

    @Test
    void wieldActionCyclesAndSkipsBrokenGear() {
        RunState s = new RunState(1L);
        s.getWeapons().clear();
        Weapon a = new Weapon(Weapon.Category.SPEAR, 1, 2, 5);
        Weapon b = new Weapon(Weapon.Category.BLADE, 3, 4, 5);
        s.getWeapons().add(a);
        s.getWeapons().add(b);

        assertSame(a, s.wieldNext());
        assertSame(b, s.wieldNext());
        assertSame(a, s.wieldNext(), "cycles back to the first");
        a.decay(100); // break a
        assertSame(b, s.wieldNext(), "skips the broken weapon");
    }

    @Test
    void wieldActionRefusesWhenNothingUsable() {
        RunState s = new RunState(1L);
        s.getWeapons().clear();
        TurnResult r = new TurnEngine().advance(s, PlayerAction.wield(RoguePlayer.NORTH));
        assertEquals(-1, s.getWieldedIndex(), "nothing to wield → still unarmed");
        assertTrue(r.messages.stream().anyMatch(m -> m.toLowerCase().contains("nothing to wield")),
                "the refusal is observed");
    }

    @Test
    void wieldActionReadiesViaTheTurnEngine() {
        RunState s = new RunState(1L);
        TurnResult r = new TurnEngine().advance(s, PlayerAction.wield(RoguePlayer.NORTH));
        assertEquals(0, s.getWieldedIndex(), "the WIELD action readies the first weapon");
        assertTrue(r.messages.stream().anyMatch(m -> m.startsWith("You ready the")), "the wield is observed");
    }

    @Test
    void weaponStateSurvivesRoundTrip() {
        RunState s = new RunState(42L);
        s.wieldNext();                       // wield index 0
        s.getWieldedWeapon().decay(3);       // some wear
        s.getWeapons().get(1).repair(5);     // repair another so max/repairCount are non-default
        int dur = s.getWieldedWeapon().getDurability();
        int max1 = s.getWeapons().get(1).getMaxDurability();
        int rc1 = s.getWeapons().get(1).getRepairCount();

        RunState loaded = json().fromJson(RunState.class, json().toJson(s));
        loaded.restoreAfterLoad();

        assertEquals(3, loaded.getWeapons().size());
        assertEquals(0, loaded.getWieldedIndex(), "the wielded reference persists");
        assertEquals(dur, loaded.getWieldedWeapon().getDurability(), "durability round-trips");
        assertEquals(max1, loaded.getWeapons().get(1).getMaxDurability(), "lowered max round-trips");
        assertEquals(rc1, loaded.getWeapons().get(1).getRepairCount(), "repair count round-trips");
        assertEquals(Weapon.Category.SPEAR, loaded.getWeapons().get(0).getCategory(), "category is a real enum, not a String");
    }

    @Test
    void preWeaponSaveLoadsUnarmedWithNoCombatRegression() {
        // A pre-4.4 save has no weapon fields. libGDX constructs RunState via its (seeded) constructor,
        // so a field-ABSENT load inherits the constructor's state — the authored starter set + the
        // wieldedIndex default (-1). That's the same documented "field-absent inherits ctor state" wart
        // as weather/identifyMap, and here it is benign: the run loads UNARMED, so combat is base STR
        // (the AC-3 no-regression guarantee) and the migration is a harmless deterministic starter set.
        RunState withData = new RunState(7L);
        withData.wieldNext();
        JsonValue root = new JsonReader().parse(json().toJson(withData));
        root.remove("weapons");       // emulate a save written before the field existed
        root.remove("wieldedIndex");

        RunState fromOld = json().fromJson(RunState.class, root.toJson(JsonWriter.OutputType.json));
        fromOld.restoreAfterLoad();

        assertNotNull(fromOld.getWeapons(), "field-initialized → non-null on an old save (AD-6)");
        assertEquals(-1, fromOld.getWieldedIndex(), "a pre-4.4 save loads unarmed (nothing wielded)");
        assertNull(fromOld.getWieldedWeapon());
        assertEquals(fromOld.getPlayer().getStr(), CombatSystem.attackDamage(fromOld),
                "unarmed → base-STR combat, no regression (the AC-3 guarantee)");
    }
}
