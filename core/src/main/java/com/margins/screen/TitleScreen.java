package com.margins.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.margins.MarginsGame;

public class TitleScreen implements Screen {
    private MarginsGame game;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture bg;
    private float timer;

    public TitleScreen(MarginsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        bg = new Texture("sprites/title-screen.jpeg");
        bg.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        timer = 0;
    }

    @Override
    public void render(float delta) {
        timer += delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(bg, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        font.setColor(1, 1, 1, 1);
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        font.draw(batch, "The Margins", w / 2f - 100, h / 2f + 60);

        float blink = (float) Math.sin(timer * 3f) * 0.3f + 0.7f;
        font.setColor(1, 1, 1, blink);
        font.getData().setScale(0.8f);
        font.draw(batch, "Press ENTER to start", w / 2f - 80, h / 2f - 20);
        font.getData().setScale(1.5f);

        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(game.gameScreen);
        }
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        bg.dispose();
    }
}
