package com.dah.rgb.audio.io;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInAssetThread;
import com.dah.rgb.annotations.CalledInAudioThread;
import com.dah.rgb.annotations.NotNull;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.nio.file.Path;

public interface AudioSource extends AutoCloseable {
    @CalledInAnyThread
    int getChannels();

    @CalledInAnyThread
    int getSampleRate();

    @CalledInAnyThread
    int getALBufferFormat();

    @CalledInAnyThread
    int getSamplesLength();

    @CalledInAudioThread
    int getSamples(@NotNull ShortBuffer pcm);

    @CalledInAnyThread
    void seek(int sampleIndex);

    @CalledInAnyThread
    void close();

    @CalledInAssetThread
    static AudioSource loadVorbisSTB(@NotNull Path path) throws IOException {
        return new VorbisAudioSource(path);
    }
}
