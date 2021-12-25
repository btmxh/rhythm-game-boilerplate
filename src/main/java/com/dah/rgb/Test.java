package com.dah.rgb;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.audio.Music;
import com.dah.rgb.game.GLFWGame;
import com.dah.rgb.scenes.SceneManager;
import com.dah.rgb.utils.Ref;

import java.io.IOException;
import java.nio.file.Paths;

public class Test {
    static class TestGame extends GLFWGame<TestGame> {
        private Music audio;
        public TestGame(int width, int height, @NotNull String title) {
            super(width, height, title);
        }

        @Override
        protected @NotNull TestGame self() {
            return this;
        }

        @Override
        public void init() {
            super.init();
            assetThread.loadAudioVorbisSTB(Paths.get("E:\\dev\\rstg\\audio.ogg"))
                    .thenApply(Ref.Own::new)
                    .thenComposeAsync(ref -> audioThread.initMusic(ref), audioThread)
                    .thenAcceptAsync(music -> (audio = music).playAudio(), audioThread);
        }

        @Override
        public void close() {
            if(audio != null) {
                audio.stop(audio.getSource()).join();
                audio.close();
            }
            super.close();
        }
    }

    public static void main(String[] args) {
        try(var game = new TestGame(1280, 720, "cosi")) {
            game.init();
            game.loop();
        }
    }
}
