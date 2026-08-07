package com.margins.rogue.narrative;

import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.state.RunState;
import com.margins.rogue.system.PlayerAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 2.3 (FR-2): Aldric's diegetic tutorial — the passive coach.
 *
 * <p>Sequencing/matchers — each opening control is checked off by its {@link PlayerAction}
 * kind and acknowledged in the log; the six complete in any order. Hide — a move into cover
 * (adjacent to a WALL) checks off hide, a move in the open does not. Passivity — {@code onAction}
 * writes only log lines: it ticks no clock, no track, no flag, no inventory (the coach observes a
 * committed turn; it is not a turn). Skip — aborts without completing (the restart path).
 */
class TutorialControllerTest {

    private RunState run() { return new RunState(42L); }

    /** Clear the player's four orthogonal neighbours to open floor (no cover). */
    private void clearNeighbours(RunState s) {
        int px = s.getPlayer().getTileX(), py = s.getPlayer().getTileY();
        RogueTileMap m = s.getTileMap();
        m.setTile(px + 1, py, RogueTile.FLOOR);
        m.setTile(px - 1, py, RogueTile.FLOOR);
        m.setTile(px, py + 1, RogueTile.FLOOR);
        m.setTile(px, py - 1, RogueTile.FLOOR);
    }

    /** Put a blocking trunk directly east of the player (cover for the hide check). */
    private void putCover(RunState s) {
        int px = s.getPlayer().getTileX(), py = s.getPlayer().getTileY();
        s.getTileMap().setTile(px + 1, py, RogueTile.WALL);
    }

    private PlayerAction move() { return PlayerAction.move(1, 0, 3); }

    // --- begin ---

    @Test
    void beginAppendsTheFirstPromptAndGoesActive() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.begin(s);
        assertTrue(t.isActive());
        assertTrue(s.getMessageLog().contains(TutorialController.Control.MOVE.prompt),
                "the first control's prompt is delivered as Aldric's line");
    }

    @Test
    void beginIsIdempotent() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.begin(s);
        int size = s.getMessageLog().size();
        t.begin(s); // second call is a no-op
        assertEquals(size, s.getMessageLog().size(), "begin fires exactly once");
    }

    @Test
    void beginAfterSkipIsANoOp() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.skip();
        t.begin(s);
        assertFalse(t.isActive(), "a skipped tutorial never begins");
    }

    // --- matchers (each control checked off + acknowledged) ---

    @Test
    void moveChecksOffMove() {
        RunState s = run();
        clearNeighbours(s);
        TutorialController t = new TutorialController();
        t.begin(s);
        t.onAction(move(), s);
        assertTrue(s.getMessageLog().contains(TutorialController.Control.MOVE.ack));
    }

    @Test
    void collectChecksOffScavenge() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.begin(s);
        t.onAction(PlayerAction.collect(3), s);
        assertTrue(s.getMessageLog().contains(TutorialController.Control.SCAVENGE.ack));
    }

    @Test
    void useChecksOffEat() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.begin(s);
        t.onAction(PlayerAction.use(0, 3), s);
        assertTrue(s.getMessageLog().contains(TutorialController.Control.EAT.ack));
    }

    @Test
    void anyCraftKindChecksOffCraft() {
        for (PlayerAction craft : new PlayerAction[]{
                PlayerAction.buildCampfire(3), PlayerAction.cook(0, 3), PlayerAction.filter(0, 3),
                PlayerAction.boil(0, 3), PlayerAction.craftTorch(3)}) {
            RunState s = run();
            TutorialController t = new TutorialController();
            t.begin(s);
            t.onAction(craft, s);
            assertTrue(s.getMessageLog().contains(TutorialController.Control.CRAFT.ack),
                    "craft kind " + craft.kind + " checks off CRAFT");
        }
    }

    @Test
    void waitChecksOffRest() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.begin(s);
        t.onAction(PlayerAction.wait(3), s);
        assertTrue(s.getMessageLog().contains(TutorialController.Control.REST.ack));
    }

    // --- hide (Decision 2: move into cover) ---

    @Test
    void moveIntoCoverChecksOffHideButAMoveInTheOpenDoesNot() {
        RunState s = run();
        clearNeighbours(s);
        TutorialController t = new TutorialController();
        t.begin(s);
        t.onAction(move(), s); // removes MOVE first (earlier in the list)

        // A move in the open must NOT check off hide.
        t.onAction(move(), s);
        assertFalse(s.getMessageLog().contains(TutorialController.Control.HIDE.ack),
                "a move with no cover adjacent does not demonstrate hide");

        // Now put a trunk beside the player: a move into cover checks off hide.
        putCover(s);
        t.onAction(move(), s);
        assertTrue(s.getMessageLog().contains(TutorialController.Control.HIDE.ack),
                "a move adjacent to cover demonstrates hide");
    }

    // --- out-of-order completion ---

    @Test
    void allSixInAnyOrderComplete() {
        RunState s = run();
        clearNeighbours(s);
        TutorialController t = new TutorialController();
        t.begin(s);
        // Scrambled order — each still checks off exactly one control.
        t.onAction(PlayerAction.wait(3), s);          // REST
        t.onAction(move(), s);                        // MOVE (open)
        t.onAction(PlayerAction.cook(0, 3), s);       // CRAFT
        t.onAction(PlayerAction.collect(3), s);       // SCAVENGE
        t.onAction(PlayerAction.use(0, 3), s);        // EAT
        assertFalse(t.isComplete(), "five learned, not yet complete");
        putCover(s);
        t.onAction(move(), s);                        // HIDE -> the sixth
        assertTrue(t.isComplete(), "all six learned");
        assertFalse(t.isActive(), "the coach goes quiet once complete");
        assertTrue(s.getMessageLog().contains(TutorialController.CLOSING), "Aldric hands the flight forward");
    }

    // --- passivity (AC-3) ---

    @Test
    void onActionTicksNothingAndMutatesNoGameState() {
        RunState s = run();
        clearNeighbours(s);
        int clock = s.getClockTurns();
        int hunger = s.getPlayer().getHunger();
        int thirst = s.getPlayer().getThirst();
        int temp = s.getPlayer().getTemperature();
        int hp = s.getPlayer().getHp();
        s.getFlagStore().set("probe", 5);
        int flag = s.getFlagStore().get("probe");

        TutorialController t = new TutorialController();
        t.begin(s);
        t.onAction(move(), s); // the coach observes — it does not advance the turn

        assertEquals(clock, s.getClockTurns(), "onAction commits no turn (TurnEngine owns ticking)");
        assertEquals(hunger, s.getPlayer().getHunger(), "hunger untouched");
        assertEquals(thirst, s.getPlayer().getThirst(), "thirst untouched");
        assertEquals(temp, s.getPlayer().getTemperature(), "temperature untouched");
        assertEquals(hp, s.getPlayer().getHp(), "HP untouched");
        assertEquals(flag, s.getFlagStore().get("probe"), "no FlagStore mutation");
    }

    @Test
    void onActionWhenInactiveIsANoOp() {
        RunState s = run();
        TutorialController t = new TutorialController();
        int size = s.getMessageLog().size();
        t.onAction(move(), s); // never begun
        assertEquals(size, s.getMessageLog().size(), "an un-begun coach says nothing");
    }

    // --- skip (the restart abort) ---

    @Test
    void skipDeactivatesWithoutCompleting() {
        RunState s = run();
        TutorialController t = new TutorialController();
        t.begin(s);
        t.skip();
        assertFalse(t.isActive(), "skip stops the coaching");
        assertFalse(t.isComplete(), "skip is an abort — it does not trip Story 2.4's completion seam");
    }

    // --- content pin ---

    @Test
    void everyControlHasAldricPromptAndAck() {
        for (TutorialController.Control c : TutorialController.Control.values()) {
            assertFalse(c.prompt.isBlank(), c + " has a prompt");
            assertFalse(c.ack.isBlank(), c + " has an acknowledgement");
            assertTrue(c.prompt.startsWith("Aldric:"), c + " prompt is Aldric's line");
            assertTrue(c.ack.startsWith("Aldric:"), c + " ack is Aldric's line");
        }
        assertTrue(TutorialController.CLOSING.startsWith("Aldric:"), "the closing line is Aldric's");
    }
}
