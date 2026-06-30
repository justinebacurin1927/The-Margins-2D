package com.margins.dialog;

public class DialogNode {
    public String text;
    public DialogOption[] options;

    public DialogNode(String text, DialogOption... options) {
        this.text = text;
        this.options = options;
    }

    public static class DialogOption {
        public String label;
        public DialogNode next;

        public DialogOption(String label, DialogNode next) {
            this.label = label;
            this.next = next;
        }
    }
}
