package com.dah.rgb.utils.timers;

public class CustomizableTimer implements Timer {
    private final Timer sourceTimer;
    private volatile double offset, lastStateChangedRawTime;
    private volatile double speed;
    private volatile boolean pausing;

    public CustomizableTimer(Timer sourceTimer) {
        this.sourceTimer = sourceTimer;
        this.speed = 1.0;
    }

    private void changeState() {
        offset = getTime();
        lastStateChangedRawTime = sourceTimer.getTime();
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isPausing() {
        return pausing;
    }

    public void setSpeed(double speed) {
        changeState();
        this.speed = speed;
    }

    public void pause() {
        changeState();
        pausing = true;
    }

    public void resume() {
        changeState();
        pausing = false;
    }

    @Override
    public double getTime() {
        var time = offset;
        if(!pausing) {
            time += (sourceTimer.getTime() - lastStateChangedRawTime) * speed;
        }
        return time;
    }
}
