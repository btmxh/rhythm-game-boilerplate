package com.dah.rgb.scenes;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.game.Game;

public class ChildlessScene<G extends Game<G>, P extends Scene<G, ?, ?>> extends Scene<G, P, SceneManager<G>> {
    public ChildlessScene(G game, P parent) {
        super(game, parent);
    }

    @Override
    protected @NotNull SceneManager<G> createSceneManager() {
        return new SceneManager<>();
    }
}
