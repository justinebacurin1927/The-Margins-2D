package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;
import com.margins.rogue.state.RunState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Story 2.2 (FR-1, AD-14): the paged-text intro controller, its authored content, and
 * the structural safe pause.
 *
 * <p>Sequencing — {@link IntroController} walks the pages in order and closes on the last
 * one or on skip; a closed controller is a defensive no-op. Content — {@link CorneoIntro}
 * covers the three beats as speaker-less narration. Safe pause — driving the whole intro
 * cannot tick the clock or the four survival tracks (the controller takes no RunState, so
 * the pin is genuinely structural; the enforcing branch is the screen's).
 */
class IntroControllerTest {

    private List<DialogNode> pages() {
        return List.of(
                new DialogNode("page one"),
                new DialogNode("page two"),
                new DialogNode("page three"));
    }

    // --- sequencing ---

    @Test
    void advanceWalksPagesInOrder() {
        IntroController c = new IntroController();
        c.start(pages());
        assertTrue(c.isActive());
        assertEquals("page one", c.getCurrent().text);
        c.advance();
        assertEquals("page two", c.getCurrent().text);
        c.advance();
        assertEquals("page three", c.getCurrent().text);
    }

    @Test
    void advanceOnTheLastPageClosesTheIntro() {
        IntroController c = new IntroController();
        c.start(pages());
        c.advance();
        c.advance(); // now on the last page (index 2)
        assertTrue(c.isActive(), "still open on the last page");
        c.advance(); // advancing off the last page closes
        assertFalse(c.isActive(), "the intro closes after the last page");
        assertNull(c.getCurrent(), "no page when closed");
    }

    @Test
    void endSkipsFromAnyPageImmediately() {
        IntroController c = new IntroController();
        c.start(pages());
        c.advance(); // on page two — mid-intro
        c.end();
        assertFalse(c.isActive(), "skip closes the intro from any page");
        assertNull(c.getCurrent());
    }

    @Test
    void getCurrentIsNullWhenInactive() {
        IntroController c = new IntroController();
        assertFalse(c.isActive(), "a fresh controller is not active");
        assertNull(c.getCurrent());
    }

    @Test
    void advanceAndEndOnAClosedControllerAreNoOps() {
        IntroController c = new IntroController();
        assertDoesNotThrow(c::advance, "advance on a closed controller is a no-op");
        assertDoesNotThrow(c::end, "end on a closed controller is a no-op");
        assertFalse(c.isActive());
    }

    @Test
    void startWithNoPagesLeavesTheIntroInactive() {
        IntroController c = new IntroController();
        c.start(List.of());
        assertFalse(c.isActive(), "an empty intro never opens");
        assertNull(c.getCurrent());
    }

    // --- content pin (AC-1) ---

    @Test
    void corneoIntroCoversTheThreeBeatsAsNarration() {
        List<DialogNode> pages = CorneoIntro.build();
        assertTrue(pages.size() >= 3, "at least the three beats (before / fall / hand-off)");

        StringBuilder all = new StringBuilder();
        for (DialogNode page : pages) {
            assertNull(page.speaker, "narration has no speaker (2.1 Task 4)");
            assertEquals(0, page.options.length, "intro pages are zero-option text (Decision 7)");
            assertTrue(page.effects().isEmpty(), "intro pages carry no effects (Decision 7)");
            all.append(page.text).append('\n');
        }
        String text = all.toString();
        assertTrue(text.contains("Copper Road"), "the before beat (Klein's posting / the road)");
        assertTrue(text.contains("Evermove"), "the fall beat (the invading column)");
        assertTrue(text.contains("You flee into the pines. Aldric is beside you."),
                "the hand-off frames the 1.8 seeded opening line");
    }

    // --- safe-pause pin (AC-3, AD-14) ---

    @Test
    void drivingTheWholeIntroTicksNothing() {
        // Structural pin: the controller takes no RunState, so it CANNOT tick — driving the full
        // intro leaves the clock and the four survival tracks untouched (mirrors 2.1's honesty pin).
        RunState s = new RunState(42L);
        int clock = s.getClockTurns();
        int hunger = s.getPlayer().getHunger();
        int thirst = s.getPlayer().getThirst();
        int temp = s.getPlayer().getTemperature();
        int hp = s.getPlayer().getHp();

        IntroController c = new IntroController();
        c.start(CorneoIntro.build());
        while (c.isActive()) c.advance(); // read every page to the end

        assertEquals(clock, s.getClockTurns(), "no turn is committed while the intro is open (AD-14)");
        assertEquals(hunger, s.getPlayer().getHunger(), "hunger does not tick");
        assertEquals(thirst, s.getPlayer().getThirst(), "thirst does not tick");
        assertEquals(temp, s.getPlayer().getTemperature(), "temperature does not drift");
        assertEquals(hp, s.getPlayer().getHp(), "no survival resolution runs");
    }
}
