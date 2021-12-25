package com.dah.rgb.audio;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInAudioThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.audio.io.AudioSource;
import com.dah.rgb.threads.impl.AudioThread;
import com.dah.rgb.utils.Config;
import com.dah.rgb.utils.Ref;
import org.lwjgl.system.MemoryStack;

import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SAMPLE_OFFSET;
import static org.lwjgl.openal.SOFTDeviceClock.AL_SAMPLE_OFFSET_CLOCK_SOFT;
import static org.lwjgl.openal.SOFTSourceLatency.AL_SAMPLE_OFFSET_LATENCY_SOFT;
import static org.lwjgl.openal.SOFTSourceLatency.alGetSourcei64vSOFT;
import static org.lwjgl.system.MemoryUtil.memAllocShort;
import static org.lwjgl.system.MemoryUtil.memFree;

public class Music extends Audio {
    private static final int STREAM_BUFFER_SIZE = Config.STREAM_BUFFER_SIZE.get(8 * 1024);
    private static final int STREAM_NUM_BUFFERS = Config.STREAM_NUM_BUFFERS.get(2);

    private final @NotNull ShortBuffer pcm;
    private final @NotNull AtomicBoolean loop;
    private final @NotNull AtomicInteger sampleIndex;
    private volatile int bufferOffset, audioOffset, lastOffset;

    @CalledInAnyThread
    public Music(@NotNull AudioThread<?> audioThread, @NotNull Ref<? extends AudioSource> audioSource) {
        super(audioThread, audioSource, STREAM_NUM_BUFFERS, 1);
        pcm = memAllocShort(STREAM_BUFFER_SIZE);
        loop = new AtomicBoolean(false);
        sampleIndex = new AtomicInteger();
    }

    @CalledInAnyThread
    public void setLoop(boolean loop) {
        this.loop.set(loop);
    }

    @Override
    @CalledInAudioThread
    public synchronized void init() {
        super.createALObjects();
        for(int i = 0; i < STREAM_NUM_BUFFERS; i++) {
            if(stream(buffers.get(i)) == 0) {
                break;
            }
        }

        for (int i = 0; i < sources.remaining(); i++) {
            var source = sources.get(i);
            alSourceQueueBuffers(source, buffers);
        }
    }

    @Override
    public int playAudio() {
        var source = super.playAudio();
        audioThread.addMusicToUpdateList(this);
        return source;
    }

    @Override
    public void stopAudio(int source) {
        super.stopAudio(source);
        audioThread.removeMusicFromUpdateList(this);
    }

    @CalledInAudioThread
    private synchronized int stream(int buffer) {
        int sampleCount = 0;

        while(sampleCount < STREAM_BUFFER_SIZE) {
            pcm.position(sampleCount);
            int samplesPerChannel = audioSource.get().getSamples(pcm);
            if(samplesPerChannel <= 0) {
                break;
            }

            sampleCount += samplesPerChannel * channels;
        }

        if(sampleCount > 0) {
            pcm.position(0);
            pcm.limit(sampleCount);
            alBufferData(buffer, alBufferFormat, pcm, sampleRate);
            pcm.limit(STREAM_BUFFER_SIZE);
        }

        return sampleCount;
    }

    public synchronized boolean update() {
        for(int i = 0; i < sources.remaining(); i++) {
            var source = sources.get(i);
            int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);

            for (int j = 0; j < processed; j++) {
                bufferOffset += STREAM_BUFFER_SIZE / channels;
                int buffer = alSourceUnqueueBuffers(source);

                if (stream(buffer) == 0) {
                    boolean exit = true;

                    if (loop.get()) {
                        rewind();
                        bufferOffset = audioOffset = lastOffset = 0;
                        exit = stream(buffer) == 0;
                    }

                    if (exit) {
                        return false;
                    }
                }

                alSourceQueueBuffers(source, buffer);
            }

            if (processed == STREAM_NUM_BUFFERS) {
                alSourcePlay(source);
            }
        }

        updateSampleIndex();
        return true;
    }

    @CalledInAnyThread
    public synchronized void seek(int sampleIndex) {
        if(sampleIndex < 0) {
            sampleIndex = 0;
        } else if(sampleIndex >= sampleCount)  {
            sampleIndex = sampleCount - 1;
        }
        audioSource.get().seek(sampleIndex);
        this.sampleIndex.set(sampleIndex);
    }

    @CalledInAnyThread
    public void seekForward(float sec) {
        seek((int) (sampleIndex.get() + sec * sampleRate));
    }

    @CalledInAnyThread
    public void seekBackward(float sec) {
        seekForward(-sec);
    }

    @CalledInAnyThread
    public void rewind() {
        seek(0);
    }

    private synchronized void updateSampleIndex() {
        audioOffset = bufferOffset + getALSampleOffset();
        sampleIndex.addAndGet(audioOffset - lastOffset);
        lastOffset = audioOffset;
    }

    private int getALSampleOffset() {
        var source = getSource();
        if(audioThread.getALCapabilities().AL_SOFT_source_latency) {
            try(var stack = MemoryStack.stackPush()) {
                var data = stack.mallocLong(2);
                alGetSourcei64vSOFT(source, AL_SAMPLE_OFFSET_LATENCY_SOFT, data);
                var offset = data.get(0);
                return (int) (offset >>> 32);
            }
        }
        if(audioThread.getALCCapabilities().ALC_SOFT_device_clock) {
            try(var stack = MemoryStack.stackPush()) {
                var data = stack.mallocLong(2);
                alGetSourcei64vSOFT(source, AL_SAMPLE_OFFSET_CLOCK_SOFT, data);
                var offset = data.get(0);
                return (int) (offset >>> 32);
            }
        }
        return alGetSourcei(source, AL_SAMPLE_OFFSET);
    }

    public int getSource() {
        return sources.get(0);
    }

    @Override
    protected double getCurrentALTimeAudio(int source) {
        updateSampleIndex();
        return (double) sampleIndex.get() / sampleRate;
    }

    @Override
    public synchronized void close() {
        super.close();
        memFree(pcm);
    }
}
