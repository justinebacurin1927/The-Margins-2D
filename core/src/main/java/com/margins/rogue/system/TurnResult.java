package com.margins.rogue.system;

import java.util.ArrayList;
import java.util.List;

/**
 * What one advanced turn produced for the screen to present. Messages are in the
 * order the systems emitted them; the screen shows the last one (matching the
 * original "last setMessage wins" behavior).
 */
public class TurnResult {
    public final List<String> messages;

    public TurnResult() {
        this.messages = new ArrayList<>();
    }

    /** The message to display, or null if this turn produced none. */
    public String lastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}
