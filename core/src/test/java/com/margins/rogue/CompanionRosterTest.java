package com.margins.rogue;

import com.margins.rogue.state.FlagStore;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 5.1 (FR-15, AD-10, AD-7): the companion is a full tile-agent with its own Status, and the
 * roster of four keeps exactly one active positioned member while the other three are abstract
 * FlagStore/Bond entries (no body, no tick, no noise until activated).
 */
class CompanionRosterTest {

    // --- AC-1: the active companion carries its own HP + condition Status (AD-3) ---

    @Test
    void theActiveCompanionHpIsWoundableHealableAndIncapacitable() {
        Companion c = new RunState(1L).getActiveCompanion();
        assertNotNull(c, "a run starts with one active companion");
        int max = c.getMaxHp();

        c.takeDamage(3);
        assertEquals(max - 3, c.getHp(), "woundable");

        c.heal(2);
        assertEquals(max - 1, c.getHp(), "healable");

        c.heal(999);
        assertEquals(max, c.getHp(), "heal clamps at the companion's own max");

        c.takeDamage(max);
        assertTrue(c.isIncapacitated(), "0 HP is incapacitated, not the player's death");
        assertFalse(c.isAlive());
        c.heal(1);
        assertFalse(c.isIncapacitated(), "an incapacitated companion can be healed back up");
    }

    @Test
    void theCompanionsConditionStateIsItsOwnNotThePlayers() {
        RunState s = new RunState(1L);
        Companion c = s.getActiveCompanion();

        c.addCondition(Companion.Condition.PANICKED);
        assertTrue(c.hasCondition(Companion.Condition.PANICKED), "the companion carries its own condition");
        assertFalse(c.hasCondition(Companion.Condition.WOUNDED), "unset conditions stay clear");

        // AD-3: the companion's Status is authoritative on the companion — the player is untouched.
        assertFalse(s.getPlayer().isDelirious(), "a companion condition does not leak onto the player");

        c.removeCondition(Companion.Condition.PANICKED);
        assertFalse(c.hasCondition(Companion.Condition.PANICKED), "conditions clear independently");
    }

    // --- AC-2: roster of four; one positioned agent, three abstract Bond entries ---

    @Test
    void exactlyOneCompanionIsPositionedTheOthersAreAbstract() {
        RunState s = new RunState(1L);
        assertEquals(1, s.getCompanions().size(), "only one companion has a body (AD-10)");
        assertEquals(CompanionId.ALDRIC, s.getActiveCompanionId(), "Aldric is the canon starting companion");

        // The other three roster members have no positioned body — they exist only as Bond entries.
        for (CompanionId id : new CompanionId[]{CompanionId.MARA, CompanionId.OLD_FEN, CompanionId.YENNA}) {
            boolean positioned = s.getCompanions().stream().anyMatch(c -> c.getId() == id);
            assertFalse(positioned, id + " has no tile-agent body until activated");
        }
    }

    @Test
    void activatingARosterMemberSwapsTheSinglePositionedBody() {
        RunState s = new RunState(1L);
        assertEquals(CompanionId.ALDRIC, s.getActiveCompanionId());

        s.activateCompanion(CompanionId.MARA);
        assertEquals(1, s.getCompanions().size(), "still exactly one positioned body (AD-10)");
        assertEquals(CompanionId.MARA, s.getActiveCompanionId(), "Mara now holds the party slot");
        assertFalse(CompanionId.MARA.isCombatant(), "Mara is a non-combatant (FR-15)");
    }

    @Test
    void perCompanionBondIsIndependent() {
        FlagStore fs = new RunState(1L).getFlagStore();
        fs.adjustBond(CompanionId.ALDRIC, 2);
        fs.adjustBond(CompanionId.MARA, -3);

        assertEquals(2, fs.getBond(CompanionId.ALDRIC), "Aldric's Bond is its own key");
        assertEquals(-3, fs.getBond(CompanionId.MARA), "Mara's Bond is a separate key");
        assertEquals(0, fs.getBond(CompanionId.YENNA), "an untouched member sits at the neutral baseline");
        assertEquals(2, fs.getBond(), "the no-arg accessor addresses Aldric (the default)");
    }

    @Test
    void abstractMembersHaveNoBodyAndDoNotTick() {
        // AC-2 / D4: an inactive roster member is not in getCompanions(), so CompanionSystem.follow,
        // the survival tick, and NoiseEvents structurally cannot reach it — proven by its absence.
        RunState s = new RunState(1L);
        s.getFlagStore().adjustBond(CompanionId.OLD_FEN, 5); // Old Fen exists as a Bond entry
        assertEquals(5, s.getFlagStore().getBond(CompanionId.OLD_FEN));
        assertTrue(s.getCompanions().stream().noneMatch(c -> c.getId() == CompanionId.OLD_FEN),
                "an abstract member has Bond but never a positioned body that could tick or make noise");
    }
}
