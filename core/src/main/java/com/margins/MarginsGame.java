package com.margins;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.margins.asset.Assets;
import com.margins.rogue.RogueGameScreen;
import com.margins.screen.TitleScreen;

public class MarginsGame extends Game {
    public TitleScreen titleScreen;
    public RogueGameScreen gameScreen;

    @Override
    public void create() {
        Assets.load();
        titleScreen = new TitleScreen(this);
        gameScreen = new RogueGameScreen(this);
        setScreen(titleScreen);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        if (dt > 0.1f) dt = 0.1f;
        super.render();
    }

    @Override
    public void dispose() {
        Assets.dispose();
    }
}
