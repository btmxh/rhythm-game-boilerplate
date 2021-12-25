package com.dah.rgb.threads.base;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInMainThread;
import com.dah.rgb.game.Game;
import com.dah.rgb.threads.impl.AudioThread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public abstract class GameThread<G extends Game<G>> implements Runnable, AutoCloseable {
    protected G game;
    protected CountDownLatch afterInit, beforeDestroy;
    protected Thread thread;
    protected AtomicReference<Double> deltaTime;
    protected AtomicReference<Phase> currentPhase;
    protected AtomicBoolean initializationSuccess;

    public GameThread(G game) {
        this.game = game;

        afterInit = new CountDownLatch(1);
        beforeDestroy = new CountDownLatch(1);

        deltaTime = new AtomicReference<>();
        currentPhase = new AtomicReference<>(Phase.INIT);
        initializationSuccess = new AtomicBoolean();
    }

    // CalledInTheThread
    public void run() {
        var initResult = init();
        if(!initResult) {
            thread.interrupt();
        }
        initializationSuccess.set(initResult);

        afterInit.countDown();
        currentPhase.set(Phase.LOOP);

        var lastLoopTime = Game.getCurrentTime();
        while(!Thread.interrupted()) {
            var now = Game.getCurrentTime();
            deltaTime.set(now - lastLoopTime);
            lastLoopTime = now;
            loop();
        }

        currentPhase.set(Phase.DESTROY);
        try {
            beforeDestroy.await();
        } catch (InterruptedException e) {
            Game.logException(Level.INFO, thread.getName() + " was interrupted before CountDownLatch 'beforeDestroy' done awaiting", e);
        }

        try {
            closeInThread();
        } catch (Exception e)  {
            Game.logException(Level.INFO, thread.getName() + " caught an exception while disposing native resources", e);
        }
    }

    // CalledInTheThread
    public boolean init() {
        if(!(this instanceof AudioThread<?>)) {
            var audioThread = game.getAudioThread();
            audioThread.waitForInitialization();
            audioThread.makeContextCurrentIfPossible();
        }
        return true;
    }

    // CalledInTheThread
    public void loop() {
    }

    @Override
    @CalledInMainThread
    public void close() {

    }

    // CalledInTheThread
    public void closeInThread() throws Exception {
        if(!(this instanceof AudioThread<?>)) {
            game.getAudioThread().makeContextNotCurrentIfPossible();
        }
    }

    @CalledInMainThread
    public void startThread() {
        var name = threadName();
        if(name != null) {
            thread = new Thread(this, name);
        } else {
            thread = new Thread(this);
        }
        thread.start();
    }

    protected String threadName() {
        return null;
    }

    @CalledInAnyThread
    public Phase getCurrentPhase() {
        return currentPhase.get();
    }

    @CalledInAnyThread
    public void waitForInitialization() {
        try {
            afterInit.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Game.logException(Level.INFO, Thread.currentThread().getName() + " got interrupted while waiting for " + thread.getName() + " to finish initialization.", e);
        }
    }

    @CalledInAnyThread
    public void interrupt() {
        thread.interrupt();
    }

    @CalledInAnyThread
    public void allowDestroy() {
        beforeDestroy.countDown();
    }

    @CalledInAnyThread
    public void join() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Game.logException(Level.INFO, Thread.currentThread().getName() + " got interrupted while waiting for " + thread.getName() + " to finish.", e);
        }
    }

    public G getGame() {
        return game;
    }

    public enum Phase {
        INIT,
        LOOP,
        DESTROY
    }
}
