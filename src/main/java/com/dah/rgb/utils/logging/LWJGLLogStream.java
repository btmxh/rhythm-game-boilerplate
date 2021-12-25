package com.dah.rgb.utils.logging;

import com.dah.rgb.game.Game;
import com.dah.rgb.utils.Config;

import java.util.logging.Level;

public class LWJGLLogStream extends LogStream {
    public static final int LWJGL_LOG_NUM_FRAME_SKIPPED = Config.LWJGL_LOG_NUM_FRAME_SKIPPED.get(2);
    public LWJGLLogStream() {
        super(msg -> {
            msg = msg.trim();
            switch(msg) {
                case "", "[LWJGL]" -> {}
                default -> Game.log(Level.INFO, "LWJGL: " + msg, LWJGL_LOG_NUM_FRAME_SKIPPED);
            }
        });
    }
}
