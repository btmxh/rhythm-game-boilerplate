package com.dah.rgb.scenes;

import com.dah.rgb.game.Game;

public class NullScene<G extends Game<G>, M extends SceneManager<G>> extends ChildlessScene<G, NullScene<G, SceneManager<G>>> {
    public NullScene(G game, NullScene<G, SceneManager<G>> parent) {
        super(game, parent);
        throw new AssertionError();
    }
}
