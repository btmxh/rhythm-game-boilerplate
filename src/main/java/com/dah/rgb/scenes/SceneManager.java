package com.dah.rgb.scenes;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.annotations.Nullable;
import com.dah.rgb.game.Game;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class SceneManager<G extends Game<G>> {
    private @Nullable Scene<G, ?, ?> currentScene;

    public SceneManager() {
        this.currentScene = null;
    }

    public void setCurrentScene(@Nullable Scene<G, ?, ?> currentScene) {
        applyToCurrentScene(Scene::hide);
        if(currentScene != null) {
            currentScene.show();
        }
        this.currentScene = currentScene;
    }

    public void render() {
        applyToCurrentScene(Scene::render);
    }

    public void update() {
        applyToCurrentScene(Scene::update);
    }

    private void applyToCurrentScene(@NotNull Consumer<@NotNull Scene<G, ?, ?>> callback) {
        var currentScene = this.currentScene;
        if(currentScene != null) {
            callback.accept(currentScene);
        }
    }
}
