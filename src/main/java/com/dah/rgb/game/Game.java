package com.dah.rgb.game;

import com.dah.rgb.annotations.*;
import com.dah.rgb.scenes.SceneManager;
import com.dah.rgb.threads.impl.AssetThread;
import com.dah.rgb.threads.impl.AudioThread;
import com.dah.rgb.threads.impl.GraphicsThread;
import com.dah.rgb.utils.Config;
import com.dah.rgb.utils.logging.LWJGLLogStream;
import com.dah.rgb.utils.timers.Timer;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Game<SELF extends Game<SELF>> implements AutoCloseable {
    public static final boolean
            LWJGL_DEBUG = Config.LWJGL_DEBUG.get(true),
            LOG_LWJGL_MESSAGE = Config.LOG_LWJGL_MESSAGE.get(true);
    public static final Logger LOGGER = Logger.getLogger("rstg");
    public static final Timer GLOBAL_TIMER = Game::getCurrentTime;
    protected final @NotNull AtomicBoolean running;
    protected final @NotNull AtomicReference<Double> deltaTime;

    protected @NotNull AudioThread<SELF> audioThread;
    protected @NotNull AssetThread<SELF> assetThread;
    protected @NotNull GraphicsThread<SELF> graphicsThread;

    protected @NotNull SceneManager<SELF> sceneManager;

    @CalledInMainThread
    public Game() {
        if(LWJGL_DEBUG) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
        }
        if(LOG_LWJGL_MESSAGE) {
            Configuration.DEBUG_STREAM.set(new LWJGLLogStream());
        }
        running = new AtomicBoolean(true);
        deltaTime = new AtomicReference<>();

        audioThread = createAudioThread();
        assetThread = createAssetThread();
        graphicsThread = createGraphicsThread();

        sceneManager = createSceneManager();
    }

    @CalledInMainThread
    public void init() {
        if(!createDisplay()) {
            log(Level.SEVERE, "Failed to create display.");
        }

        audioThread.startThread();
        audioThread.waitForInitialization();

        assetThread.startThread();
        graphicsThread.startThread();

        assetThread.waitForInitialization();
        graphicsThread.waitForInitialization();

        audioThread.makeContextCurrentIfPossible();
    }

    @CalledInAnyThread
    public static double getCurrentTime() {
        return System.nanoTime() * 1e-6;
    }

    @CalledInMainThread
    protected abstract boolean createDisplay();

    @CalledInMainThread
    protected @NotNull AudioThread<SELF> createAudioThread() {
        return new AudioThread<>(self());
    }

    @CalledInMainThread
    protected @NotNull AssetThread<SELF> createAssetThread() {
        return new AssetThread<>(self());
    }

    @CalledInMainThread
    protected @NotNull GraphicsThread<SELF> createGraphicsThread() {
        return new GraphicsThread<>(self());
    }

    @CalledInMainThread
    protected abstract void pollEvents();

    @CalledInMainThread
    public void loop() {
        var last = Game.getCurrentTime();
        while (running.get()) {
            var now = Game.getCurrentTime();
            deltaTime.set(now - last);
            last = now;
            pollEvents();
            update();
        }
    }

    @Override
    @CalledInMainThread
    public void close() {
        audioThread.interrupt();
        audioThread.allowDestroy();
        assetThread.close();

        assetThread.interrupt();
        assetThread.allowDestroy();
        assetThread.close();

        graphicsThread.interrupt();
        graphicsThread.allowDestroy();
        graphicsThread.close();

        audioThread.join();
        assetThread.join();
        graphicsThread.join();
    }

    @CalledInAnyThread
    public static void log(@NotNull Level level, @NotNull String msg) {
        log(level, msg, 2);
    }

    @CalledInAnyThread
    public static void logException(@NotNull Level level, @NotNull String msg, @NotNull Throwable ex) {
        logException(level, msg, ex, 2);
    }


    @CalledInAnyThread
    public static void log(@NotNull Level level, @NotNull String msg, int skip) {
        StackWalker.getInstance()
                .walk(frames -> frames.skip(skip).findFirst())
                .ifPresentOrElse(frame -> LOGGER.logp(level, frame.getClassName(), frame.getMethodName(), msg),
                                () -> LOGGER.log(level, msg));
    }

    @CalledInAnyThread
    public static void logException(@NotNull Level level, @NotNull String msg, @NotNull Throwable ex, int skip) {
        StackWalker.getInstance()
                .walk(frames -> frames.skip(skip).findFirst())
                .ifPresentOrElse(frame -> LOGGER.logp(level, frame.getClassName(), frame.getMethodName(), msg, ex),
                                () -> LOGGER.log(level, msg, ex));
    }

    @CalledInAnyThread
    public static void logGL(@NotNull Level logLevel, @NotNull String message) {
        log(logLevel, "GL: " + message, 2);
    }

    @CalledInAnyThread
    public @NotNull AudioThread<SELF> getAudioThread() {
        return audioThread;
    }

    @CalledInAnyThread
    public @NotNull AssetThread<SELF> getAssetThread() {
        return assetThread;
    }

    @CalledInAnyThread
    public @NotNull GraphicsThread<SELF> getGraphicsThread() {
        return graphicsThread;
    }

    protected abstract @NotNull SELF self();

    @CalledInGraphicsThread
    public abstract @Nullable GLCapabilities initOpenGL(GraphicsThread<SELF> graphicsThread);

    @CalledInMainThread
    protected SceneManager<SELF> createSceneManager() {
        return new SceneManager<>();
    }

    @CalledInMainThread
    public void update() {
        sceneManager.update();
    }

    @CalledInGraphicsThread
    public void render() {
        sceneManager.render();
    }
}
