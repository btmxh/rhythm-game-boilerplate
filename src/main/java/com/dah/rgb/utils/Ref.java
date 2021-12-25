package com.dah.rgb.utils;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.game.Game;

import java.util.logging.Level;

public abstract sealed class Ref<T extends AutoCloseable> permits Ref.Shared, Ref.Own {
    protected final @NotNull T value;

    public Ref(@NotNull T value) {
        this.value = value;
    }

    public @NotNull T get() {
        return value;
    }

    public abstract void close();

    public static final class Shared<T extends AutoCloseable> extends Ref<T> {
        public Shared(@NotNull T value) {
            super(value);
        }

        @Override
        public void close() {

        }
    }

    public static final class Own<T extends AutoCloseable> extends Ref<T> {
        public Own(@NotNull T value) {
            super(value);
        }

        @Override
        public void close() {
            try {
                value.close();
            } catch (Exception e) {
                Game.logException(Level.WARNING, "Unable to close resource: " + value, e);
            }
        }
    }
}
