package com.margins.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.margins.MarginsGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("The Margins 2D");
        config.setWindowedMode(960, 640);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new MarginsGame(), config);
    }
}
