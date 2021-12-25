package com.dah.rgb.utils;

import com.dah.rgb.annotations.CalledInAssetThread;
import com.dah.rgb.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.memFree;

public record Image(@NotNull Dimension size, boolean hasAlpha, @NotNull ByteBuffer data) implements AutoCloseable {
    @Override
    public void close() {
        memFree(data);
    }

    @CalledInAssetThread
    public static Image loadSTB(@NotNull Path path) throws IOException {
        try(var stack = MemoryStack.stackPush()) {
            var pWidth = stack.mallocInt(1);
            var pHeight = stack.mallocInt(1);
            var pComps = stack.mallocInt(1);

            var loadSuccess = stbi_info(path.toString(), pWidth, pHeight, pComps);
            if(!loadSuccess) {
                throw new IOException("Unable to load image '" + path + "': " + stbi_failure_reason());
            }
            int comps = pComps.get(0);
            if(comps < STBI_rgb) {
                // STBI_grey + 2 = STBI_rgb
                // STBI_grey_alpha + 2 = STBI_rgb_alpha
                comps += 2;
            }

            var data = stbi_load(path.toString(), pWidth, pHeight, pComps, comps);
            if(data == null) {
                throw new IOException("Unable to load image '" + path + "': " + stbi_failure_reason());
            }

            return new Image(new Dimension(pWidth.get(), pHeight.get()), comps == STBI_rgb_alpha, data);
        }
    }
}
