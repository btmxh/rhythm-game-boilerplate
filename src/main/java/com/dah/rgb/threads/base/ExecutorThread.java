package com.dah.rgb.threads.base;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInMainThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.annotations.Nullable;
import com.dah.rgb.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;

public class ExecutorThread<G extends Game<G>> extends GameThread<G> implements Executor {

    private final @NotNull ExecutionPolicy policy;
    private final @NotNull List<@NotNull Runnable> works;
    private final @Nullable Phaser lock;

    @CalledInMainThread
    public ExecutorThread(@NotNull G game, @NotNull ExecutionPolicy policy, boolean waitForWorks) {
        super(game);
        this.policy = policy;
        this.works = Collections.synchronizedList(new LinkedList<>());
        if(waitForWorks) {
            lock = new Phaser(0) {
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    return false;
                }
            };
        } else {
            lock = null;
        }
    }

    @Override
    @CalledInAnyThread
    public void execute(@NotNull Runnable command) {
        synchronized (works) {
            works.add(command);
        }
        if(lock != null && lock.getRegisteredParties() > 0) {
            lock.arriveAndDeregister();
        }
    }

    @Override
    public void loop() {
        super.loop();
        switch(policy) {
            case EXECUTE_ONE_PER_LOOP -> {
                Runnable work = null;
                synchronized (works) {
                    if(!works.isEmpty()) {
                        work = works.remove(0);
                    }
                }
                if(work != null) {
                    work.run();
                }
            }

            case EXECUTE_AS_MUCH_AS_POSSIBLE -> {
                List<Runnable> copy;
                synchronized (works) {
                    copy = new ArrayList<>(works);
                    works.clear();
                }
                for(var work : copy) {
                    if(Thread.interrupted()) {
                        return;
                    }
                    work.run();
                }
            }
        }

        if(lock != null) {
            lock.awaitAdvance(lock.register());
        }
    }

    @Override
    @CalledInMainThread
    public void close() {
        super.close();
        if(lock != null) {
            lock.forceTermination();
        }
    }

    public enum ExecutionPolicy {
        EXECUTE_ONE_PER_LOOP,
        EXECUTE_AS_MUCH_AS_POSSIBLE
    }
}
