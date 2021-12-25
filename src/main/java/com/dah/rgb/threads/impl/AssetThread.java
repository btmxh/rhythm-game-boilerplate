package com.dah.rgb.threads.impl;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInAssetThread;
import com.dah.rgb.annotations.CalledInMainThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.audio.io.AudioSource;
import com.dah.rgb.audio.io.VorbisAudioSource;
import com.dah.rgb.game.Game;
import com.dah.rgb.threads.base.ExecutorThread;
import com.dah.rgb.utils.Image;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class AssetThread<G extends Game<G>> extends ExecutorThread<G> {
    @CalledInMainThread
    public AssetThread(G game) {
        super(game, ExecutionPolicy.EXECUTE_AS_MUCH_AS_POSSIBLE, true);
    }

    @CalledInAnyThread
    public <T> CompletableFuture<T> load(@NotNull Load<T> load) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return load.load();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, this);
    }

    @CalledInAnyThread
    public CompletableFuture<Image> loadImageSTB(@NotNull Path path) {
        return load(() -> Image.loadSTB(path));
    }

    @CalledInAnyThread
    public CompletableFuture<AudioSource> loadAudioVorbisSTB(Path path) {
        return load(() -> AudioSource.loadVorbisSTB(path));
    }

    public interface Load<T> {
        @CalledInAssetThread
        T load() throws Exception;
    }
}
