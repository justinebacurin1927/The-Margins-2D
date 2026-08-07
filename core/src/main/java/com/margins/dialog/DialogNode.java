package com.margins.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One dialogue node (FR-6, AD-14): the speaker line + up to N numbered choices a
 * scene shows. Pure content model — no libGDX (AD-2), no rogue dependencies. A
 * node may carry {@link DialogEffect}s (Bond, item give/take, disposition,
 * flags) that fire when the node is entered; the controller executes them and
 * emits their SPD-tone lines to the message log (observation discipline).
 *
 * <p>Choices are either ungated (always advance to {@code next}) or gated on a
 * stat ({@link DialogOption#isGated()}): a passed check routes to the success
 * branch, a failed check to the failure branch (FR-19 — VOICE primary, occasional
 * INS).
 */
public class DialogNode {
    public String text;
    /** The speaker line, or null for narration (no speaker prefix). */
    public String speaker;
    public DialogOption[] options;

    private final List<DialogEffect> effects = new ArrayList<>();

    public DialogNode(String text, DialogOption... options) {
        this.text = text;
        this.options = options;
    }

    /** Set the speaker line shown above this node's text (null = narration). */
    public DialogNode withSpeaker(String speaker) {
        this.speaker = speaker;
        return this;
    }

    /** Attach a node-entry effect (FR-19, AC-2): fired when this node is entered. */
    public DialogNode withEffect(DialogEffect effect) {
        effects.add(effect);
        return this;
    }

    /** The node-entry effects, in order (unmodifiable view — authored once at build time). */
    public List<DialogEffect> effects() {
        return Collections.unmodifiableList(effects);
    }

    /** Attach a flag effect fired when this node is entered (FR-8, AD-7) — the SET_FLAG
     *  convenience; delegates to the effect list. */
    public DialogNode withFlag(String key, int value) {
        return withEffect(new DialogEffect.SetFlag(key, value));
    }

    public static class DialogOption {
        public String label;
        public DialogNode next;             // followed when ungated, or on a passed gate (success branch)
        public GateStat gatedStat;          // null = ungated; non-null = gated on this stat (FR-19)
        public int gateThreshold;           // gated: stat >= threshold passes
        public DialogNode failNext;         // followed when a gated check fails (may be null → scene ends)

        public DialogOption(String label, DialogNode next) {
            this.label = label;
            this.next = next;
        }

        /** A choice gated by a stat check (FR-19): pass (stat >= threshold) → successNext, fail → failNext. */
        public DialogOption(String label, GateStat gatedStat, int gateThreshold, DialogNode successNext, DialogNode failNext) {
            this.label = label;
            this.gatedStat = gatedStat;
            this.gateThreshold = gateThreshold;
            this.next = successNext;
            this.failNext = failNext;
        }

        public boolean isGated() {
            return gatedStat != null;
        }
    }
}
