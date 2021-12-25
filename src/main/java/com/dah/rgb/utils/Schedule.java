package com.dah.rgb.utils;

import com.dah.rgb.game.Game;

public class Schedule {
    private final double waitTo;
    private final Runnable callback;

    public Schedule(long time, Runnable callback) {
        this.waitTo = Game.getCurrentTime() + time;
        this.callback = callback;
    }

    public boolean update() {
        if(Game.getCurrentTime() >= waitTo) {
            callback.run();
            return true;
        }

        return false;
    }
}
