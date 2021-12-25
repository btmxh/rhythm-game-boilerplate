package com.dah.rgb.audio;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInAudioThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.audio.io.AudioSource;
import com.dah.rgb.game.Game;
import com.dah.rgb.threads.impl.AudioThread;
import com.dah.rgb.utils.Ref;
import org.lwjgl.openal.AL11;

import java.nio.IntBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

public abstract class Audio implements AutoCloseable {
    protected @NotNull AudioThread<?> audioThread;
    protected @NotNull Ref<? extends AudioSource> audioSource;

    protected final int channels, sampleRate;
    protected final int alBufferFormat;
    protected final int sampleCount;

    protected @NotNull IntBuffer buffers;
    protected @NotNull IntBuffer sources;

    private volatile double lastALTime, lastGameTime;

    @CalledInAnyThread
    public Audio(@NotNull AudioThread<?> audioThread, @NotNull Ref<? extends AudioSource> audioSource, int numBuffers, int numSources) {
        this.audioThread = audioThread;
        this.audioSource = audioSource;

        var source = audioSource.get();
        channels = source.getChannels();
        sampleRate = source.getSampleRate();
        alBufferFormat = source.getALBufferFormat();
        sampleCount = source.getSamplesLength();

        this.buffers = memAllocInt(numBuffers);
        this.sources = memAllocInt(numSources);
    }

    @CalledInAudioThread
    protected synchronized final void createALObjects() {
        alGenBuffers(buffers);
        alGenSources(sources);
    }

    @CalledInAudioThread
    protected double getCurrentALTimeAudio(int source) {
        return alGetSourcef(source, AL11.AL_SEC_OFFSET);
    }

    @CalledInAudioThread
    public synchronized double getCurrentTimeAudio(int source) {
        var state = getPlaybackStateAudio(source);
        if(state == AudioState.STOPPED || state == AudioState.INITIAL) {
            return 0.0;
        }

        var currentALTime = getCurrentALTimeAudio(source);
        var currentGameTime = Game.getCurrentTime();
        if(currentALTime - lastALTime < 1e-3 && state != AudioState.PAUSED) {
            return currentALTime + (currentGameTime - lastGameTime);
        } else {
            lastALTime = currentALTime;
            lastGameTime = currentGameTime;
            return lastALTime;
        }
    }

    @CalledInAnyThread
    public @NotNull CompletableFuture<@NotNull Double> getCurrentTime(int source) {
        return executeQuick(() -> getCurrentTimeAudio(source));
    }

    @CalledInAudioThread
    public synchronized void init() {
        createALObjects();
    }

    @CalledInAudioThread
    protected @NotNull AudioState getPlaybackStateAudio(int source) {
        return Objects.requireNonNull(AudioState.parseAL(alGetSourcei(source, AL_SOURCE_STATE)));
    }

    @CalledInAnyThread
    public @NotNull CompletableFuture<@NotNull AudioState> getPlaybackState(int source) {
        return executeQuick(() -> getPlaybackStateAudio(source));
    }

    @CalledInAudioThread
    protected void playSource(int source) {
        alSourcePlay(source);
    }

    @CalledInAudioThread
    public int playAudio() {
        synchronized (this) {
            for (int i = 0; i < sources.remaining(); i++) {
                var source = sources.get(i);
                var state = getPlaybackStateAudio(source);

                if(state != AudioState.PLAYING && state != AudioState.PAUSED) {
                    playSource(source);
                    return source;
                }
            }
            return -1;
        }
    }

    @CalledInAudioThread
    public void pauseAudio(int source) {
        alSourcePause(source);
    }

    @CalledInAudioThread
    public void stopAudio(int source) { alSourceStop(source); }

    @CalledInAnyThread
    public @NotNull CompletableFuture<@NotNull Integer> play() {
        return executeQuick(this::playAudio);
    }

    @CalledInAnyThread
    public @NotNull CompletableFuture<Void> pause(int source) {
        return executeQuick(() -> pauseAudio(source));
    }

    @CalledInAnyThread
    public @NotNull CompletableFuture<Void> stop(int source) { return executeQuick(() -> stopAudio(source)); }

    @CalledInAudioThread
    protected void closeAudio() {
        alDeleteBuffers(buffers);
        alDeleteSources(sources);
    }

    @CalledInAnyThread
    @Override
    public synchronized void close() {
        try {
            audioSource.close();
        } catch (Exception e) {
            Game.log(Level.WARNING, "Unable to close audio source: " + audioSource);
        }
        CompletableFuture.runAsync(this::closeAudio, audioThread).join();
        memFree(sources);
        memFree(buffers);
    }

    @CalledInAnyThread
    private <T> @NotNull CompletableFuture<T> executeQuick(@NotNull Supplier<T> command) {
        return CompletableFuture.supplyAsync(command, audioThread::executeQuick);
    }

    @CalledInAnyThread
    private @NotNull CompletableFuture<Void> executeQuick(@NotNull Runnable command) {
        return CompletableFuture.runAsync(command, audioThread::executeQuick);
    }
}
