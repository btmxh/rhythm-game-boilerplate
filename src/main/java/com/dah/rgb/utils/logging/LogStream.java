package com.dah.rgb.utils.logging;

import com.dah.rgb.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;

public class LogStream extends PrintStream {
    public LogStream(@NotNull Consumer<String> logger) {
        super(new ByteArrayOutputStream(){
            @Override
            public void flush() throws IOException {
                super.flush();
                logger.accept(toString());
                reset();
            }
        }, true);
    }
}
