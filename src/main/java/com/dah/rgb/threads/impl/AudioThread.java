package com.dah.rgb.threads.impl;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInAudioThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.audio.Music;
import com.dah.rgb.audio.io.AudioSource;
import com.dah.rgb.game.Game;
import com.dah.rgb.threads.base.ExecutorThread;
import com.dah.rgb.utils.Ref;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.EXTThreadLocalContext.alcSetThreadContext;
import static org.lwjgl.system.MemoryUtil.NULL;

public class AudioThread<G extends Game<G>> extends ExecutorThread<G> {
    private final List<Music> musics;
    private long device, context;
    private ALCCapabilities alc;
    private ALCapabilities al;

    public AudioThread(@NotNull G game) {
        super(game, ExecutionPolicy.EXECUTE_ONE_PER_LOOP, false);
        musics = Collections.synchronizedList(new ArrayList<>());
    }

    @CalledInAnyThread
    public void executeQuick(@NotNull Runnable command) {
        if(alc.ALC_EXT_thread_local_context && initializationSuccess.get()) {
            command.run();
        } else {
            execute(command);
        }
    }

    @Override
    @CalledInAudioThread
    public boolean init() {
        int error;
        device = alcOpenDevice((ByteBuffer) null);
        if(device == NULL) {
            Game.log(Level.SEVERE, "Failed to open OpenAL device");
            return false;
        }
        alc = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        if(context == NULL | (error = alcGetError(device)) != ALC_NO_ERROR) {
            Game.log(Level.SEVERE, "Failed to create OpenAL context: " + alc + ". Error code: " + error);
            return false;
        }
        alcMakeContextCurrent(context);
        al = AL.createCapabilities(alc);
        return true;
    }

    @Override
    @CalledInAudioThread
    public void loop() {
        super.loop();
        musics.removeIf(music -> !music.update());
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Game.logException(Level.INFO, thread.getName() + " was interrupted", e);
        }
    }

    @Override
    @CalledInAudioThread
    public void closeInThread() throws Exception {
        super.closeInThread();
        alcSetThreadContext(NULL);
        alcMakeContextCurrent(NULL);
        if(context != NULL) alcDestroyContext(context);
        if(device != NULL)  alcCloseDevice(device);
    }

    @CalledInAnyThread
    public ALCCapabilities getALCCapabilities() {
        return alc;
    }

    @CalledInAnyThread
    public ALCapabilities getALCapabilities() {
        return al;
    }

    @Override
    protected String threadName() {
        return "AudioThread";
    }

    @CalledInAnyThread
    public boolean supportsThreadLocalContext() {
        return alc.ALC_EXT_thread_local_context;
    }

    @CalledInAnyThread
    public void makeContextCurrent() {
        alcSetThreadContext(context);
        AL.setCurrentThread(al);
    }

    @CalledInAnyThread
    public boolean makeContextCurrentIfPossible() {
        if(supportsThreadLocalContext()) {
            makeContextCurrent();
            return true;
        } else {
            return false;
        }
    }

    @CalledInAnyThread
    public void makeContextNotCurrent() {
        alcSetThreadContext(NULL);
        AL.setCurrentThread(null);
    }

    @CalledInAnyThread
    public boolean makeContextNotCurrentIfPossible() {
        if(supportsThreadLocalContext()) {
            makeContextNotCurrent();
            return true;
        } else {
            return false;
        }
    }

    @CalledInAnyThread
    public Music newMusic(Ref<? extends AudioSource> audioSource) {
        return new Music(this, audioSource);
    }

    @CalledInAnyThread
    public CompletableFuture<Music> initMusic(Ref<? extends AudioSource> audioSource) {
        return CompletableFuture.supplyAsync(() -> {
            var music = newMusic(audioSource);
            music.init();
            return music;
        }, this);
    }

    public void addMusicToUpdateList(Music music) {
        musics.add(music);
    }

    public void removeMusicFromUpdateList(Music music) {
        musics.remove(music);
    }
}
