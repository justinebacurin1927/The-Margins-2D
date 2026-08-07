package com.margins.rogue.narrative;

import com.margins.dialog.DialogNode;

import java.util.List;

/**
 * The Act 0 text intro — the Fall of Corneo (FR-1, PRD §4.1), authored as a small
 * ordered list of narration pages. Sequenced by {@link IntroController} and presented
 * by {@code MarginScreen} in the bottom-log surface (AD-14/AD-15). This is the
 * {@code SampleDialog} slot repurposed as the real, shippable intro (Story 2.2
 * Decision 6): pure text, no options, no effects, no flags (Decision 7).
 *
 * <p>Every page is a zero-option {@link DialogNode} with a null speaker — third-person
 * narration carries no speaker prefix (Story 2.1 Task 4's lesson). The three beats are
 * the <em>before</em> (Klein's posting, his two duties, Magdalene's letter, Aldric), the
 * <em>fall</em> (the horn, the Evermove column, the Sense-user, Corneo burning), and the
 * <em>hand-off</em> (Klein and Aldric fleeing into the pines) — the last page frames the
 * 1.8 seeded opening line, which the run's message log already holds when control frees.
 */
public final class CorneoIntro {

    private CorneoIntro() {}

    public static List<DialogNode> build() {
        return List.of(
                // --- Before ---
                new DialogNode(
                        "They posted Klein to Corneo — a walled town at the far edge of the kingdom, "
                        + "where the Copper Road runs out of the hills and the maps stop caring. A border "
                        + "knight's whole duty in two lines: keep the road open, and keep the town safe."),
                new DialogNode(
                        "It was quiet work. Magdalene's letters came west with the copper carts, and Klein "
                        + "read each one twice. Home was that direction, past the hills. Aldric drilled beside "
                        + "him and drank beside him and never once made the wait feel long."),
                // --- Fall ---
                new DialogNode(
                        "The horn went up at midmorning — the wrong horn, too long, from the road side. On the "
                        + "Copper Road, where the copper carts should have been, a column of the Evermove was "
                        + "coming down out of the hills, banners and iron, unhurried."),
                new DialogNode(
                        "The garrison formed on the wall and did not hold. Among the Evermove walked one who "
                        + "did not carry a blade. He only looked at the men on the gate, and one by one they "
                        + "set their spears down and forgot why they had ever raised them."),
                new DialogNode(
                        "The gate opened itself. Corneo began to burn — the granary first, then the roofs, "
                        + "then the whole of it, a heat you could feel from the wall-walk. There was no line "
                        + "left to hold and no order left to give."),
                // --- Hand-off ---
                new DialogNode(
                        "So Klein ran, and Aldric ran with him, down off the wall and out through the north "
                        + "postern into the trees, the fire at their backs turning the low clouds orange. They "
                        + "did not stop until the sound of it was gone.\n\n"
                        + "You flee into the pines. Aldric is beside you."));
    }
}
