package com.dah.rgb.audio.io;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInAssetThread;
import com.dah.rgb.annotations.CalledInAudioThread;
import com.dah.rgb.annotations.NotNull;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class VorbisAudioSource implements AudioSource {
    private final long decoder;
    private final @NotNull STBVorbisInfo info;

    @CalledInAssetThread
    VorbisAudioSource(@NotNull Path path) throws IOException {
        info = STBVorbisInfo.malloc();
        try(var stack = MemoryStack.stackPush()) {
            var error = stack.mallocInt(1);

            decoder = stb_vorbis_open_filename(path.toString(), error, null);

            if(decoder == NULL && error.get() != 0) {
                throw new IOException("Loading audio file '%s' failed".formatted(path));
            }

            stb_vorbis_get_info(decoder, info);
        }
    }

    @Override
    @CalledInAnyThread
    public int getChannels() {
        return info.channels();
    }

    @Override
    @CalledInAnyThread
    public int getSampleRate() {
        return info.sample_rate();
    }

    @Override
    @CalledInAnyThread
    public int getALBufferFormat() {
        return info.channels() == 1? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
    }

    @Override
    @CalledInAnyThread
    public int getSamplesLength() {
        return stb_vorbis_stream_length_in_samples(decoder);
    }

    @Override
    @CalledInAudioThread
    public int getSamples(@NotNull ShortBuffer pcm) {
        return stb_vorbis_get_samples_short_interleaved(decoder, getChannels(), pcm);
    }

    @Override
    @CalledInAnyThread
    public void seek(int sampleIndex) {
        stb_vorbis_seek(decoder, sampleIndex);
    }

    @Override
    @CalledInAnyThread
    public void close() {
        info.free();
        stb_vorbis_close(decoder);
    }
}
