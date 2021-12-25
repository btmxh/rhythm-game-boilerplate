package com.dah.rgb.utils;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.annotations.Nullable;

import java.util.function.Function;

// taken from lwjgl
public class Config<T> {
    public static final @NotNull Config<Boolean> LOG_LWJGL_MESSAGE = new Config<>("com.dah.rgb.LogLWJGLMessages", StateInit.BOOLEAN);
    public static final @NotNull Config<Integer> LWJGL_LOG_NUM_FRAME_SKIPPED = new Config<>("com.dah.rgb.LWJGLLogNumFrameSkipped", StateInit.INT);
    public static final @NotNull Config<Boolean> LWJGL_DEBUG = new Config<>("com.dah.rgb.LWJGLDebug", StateInit.BOOLEAN);
    public static final @NotNull Config<Boolean> GL_DEBUG_CALLBACK = new Config<>("com.dah.rgb.GLDebugCallback", StateInit.BOOLEAN);
    public static final @NotNull Config<Integer> STREAM_BUFFER_SIZE = new Config<>("com.dah.rgb.StreamBufferSize", StateInit.INT);
    public static final @NotNull Config<Integer> STREAM_NUM_BUFFERS = new Config<>("com.dah.rgb.StreamNumBuffers", StateInit.INT);

    private interface StateInit<T> extends Function<String, @Nullable T> {
        Config.StateInit<Boolean> BOOLEAN = property -> {
            String value = System.getProperty(property);
            return value == null ? null : Boolean.parseBoolean(value);
        };

        Config.StateInit<Integer> INT = Integer::getInteger;

        Config.StateInit<String> STRING = System::getProperty;
    }

    private final String property;

    private T state;

    Config(String property, Config.StateInit<? extends T> init) {
        this.property = property;
        this.state = init.apply(property);
    }

    public @NotNull String getProperty() {
        return property;
    }

    public void set(T value) {
        this.state = value;
    }

    public @Nullable T get() {
        return state;
    }

    public @NotNull T get(@NotNull T defaultValue) {
        T state = this.state;
        if (state == null) {
            state = defaultValue;
        }

        return state;
    }
}
