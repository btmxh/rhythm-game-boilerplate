package com.dah.rgb.scenes;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.game.Game;

public abstract class Scene<G extends Game<G>, P extends Scene<G, ?, ?>, M extends SceneManager<G>> {
    protected final @NotNull G game;
    protected final @NotNull P parent;
    protected final @NotNull M sceneManager;

    public Scene(@NotNull G game, @NotNull P parent) {
        this.game = game;
        this.parent = parent;
        this.sceneManager = createSceneManager();
    }

    protected abstract @NotNull M createSceneManager();

    public @NotNull G getGame() {
        return game;
    }

    public @NotNull P getParent() {
        return parent;
    }

    public void render() {
        sceneManager.render();
    }

    public void update() {
        sceneManager.update();
    }

    public void hide() {

    }

    public void show() {

    }
}
