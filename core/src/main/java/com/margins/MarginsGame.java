package com.margins;

import com.badlogic.gdx.Game;

/**
 * Entry point for <b>The Margin</b>. The old prototype's art and render layer were
 * removed; this boots the new SPD-style {@link MarginScreen} — a first playable
 * vertical slice built on the kept, headless core under {@code com.margins.rogue}
 * (turn engine, tilemap, FOV, detection, noise, save, companion, inventory).
 *
 * <p>Next: the real Herois presentation + the survival systems from the design
 * bible (see {@code The Margin - Remake/}).
 */
public class MarginsGame extends Game {

    @Override
    public void create() {
        setScreen(new MarginScreen());
    }
}
