package com.dah.rgb.audio;

import com.dah.rgb.annotations.Nullable;

import static org.lwjgl.openal.AL10.*;

public enum AudioState {
    INITIAL, PLAYING, STOPPED, PAUSED;

    public static @Nullable AudioState parseAL(int alState) {
        return switch (alState) {
            case AL_INITIAL -> INITIAL;
            case AL_PLAYING -> PLAYING;
            case AL_PAUSED -> PAUSED;
            case AL_STOPPED -> STOPPED;
            default -> null;
        };
    }
}
