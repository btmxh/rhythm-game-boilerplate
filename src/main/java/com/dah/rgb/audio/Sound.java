package com.dah.rgb.audio;

import com.dah.rgb.annotations.CalledInAudioThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.audio.io.AudioSource;
import com.dah.rgb.threads.impl.AudioThread;
import com.dah.rgb.utils.Ref;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.system.MemoryUtil.memFree;

public class Sound extends Audio {
    public Sound(@NotNull AudioThread<?> audioThread, @NotNull Ref<AudioSource> audioSource, int numSources) {
        super(audioThread, audioSource, 1, numSources);
    }

    @Override
    @CalledInAudioThread
    public synchronized void init() {
        super.init();
        var pcm = MemoryUtil.memAllocShort(sampleCount);
        audioSource.get().getSamples(pcm);
        var buffer = buffers.get(0);
        alBufferData(buffer, alBufferFormat, pcm, sampleRate);
        memFree(pcm);
        for (int i = 0; i < sources.remaining(); i++) {
            alSourcei(sources.get(i), AL_BUFFER, buffer);
        }
    }
}
