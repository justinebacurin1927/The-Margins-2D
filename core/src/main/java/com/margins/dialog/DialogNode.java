package com.margins.dialog;

public class DialogNode {
    public String text;
    public DialogOption[] options;
    public String setFlagKey;        // when non-null, entering this node writes this flag (FR-8, AD-7)
    public int setFlagValue = 1;

    public DialogNode(String text, DialogOption... options) {
        this.text = text;
        this.options = options;
    }

    /** Attach a flag effect fired when this node is entered (scene sets a flag, FR-8). */
    public DialogNode withFlag(String key, int value) {
        this.setFlagKey = key;
        this.setFlagValue = value;
        return this;
    }

    public static class DialogOption {
        public String label;
        public DialogNode next;               // followed when ungated, or on a passed INSTINCT check (success branch)
        public int instinctThreshold = -1;    // -1 = ungated; >= 0 = gated on player.instinct >= threshold (AD-8, FR-7)
        public DialogNode failNext;           // followed when a gated check fails (may be null → scene ends)

        public DialogOption(String label, DialogNode next) {
            this.label = label;
            this.next = next;
        }

        /** A choice gated by an INSTINCT Check (FR-7): pass → successNext, fail → failNext. */
        public DialogOption(String label, int instinctThreshold, DialogNode successNext, DialogNode failNext) {
            this.label = label;
            this.instinctThreshold = instinctThreshold;
            this.next = successNext;
            this.failNext = failNext;
        }

        public boolean isGated() {
            return instinctThreshold >= 0;
        }
    }
}
