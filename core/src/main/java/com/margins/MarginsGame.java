package com.margins;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;

/**
 * Entry point. Cleared to a minimal runnable shell for <b>The Margin</b> rebuild —
 * the old prototype's screens and art assets were removed. The reusable, headless
 * core still lives under {@code com.margins.rogue} (turn engine, tilemap, FOV,
 * detection, noise, save, companion, inventory) for the new game to build on.
 *
 * <p>Next: a new SPD-style presentation layer + the survival systems from the
 * design bible (see {@code The Margin - Remake/}).
 */
public class MarginsGame extends Game {

    @Override
    public void create() {
        // Blank placeholder screen — dark "Herois night" clear colour — until the
        // new game screen is built. Keeps the app runnable with zero art.
        setScreen(new ScreenAdapter() {
            @Override
            public void render(float delta) {
                Gdx.gl.glClearColor(0.06f, 0.07f, 0.06f, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            }
        });
    }
}
