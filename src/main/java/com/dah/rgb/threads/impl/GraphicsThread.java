package com.dah.rgb.threads.impl;

import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.game.Game;
import com.dah.rgb.threads.base.ExecutorThread;
import com.dah.rgb.utils.Config;
import com.dah.rgb.utils.Image;
import org.lwjgl.opengl.*;
import org.lwjgl.system.Callback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.logging.Level;

import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;
import static org.lwjgl.opengl.GL30.GL_CONTEXT_FLAGS;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GraphicsThread<G extends Game<G>> extends ExecutorThread<G> {
    public static final boolean GL_DEBUG_CALLBACK = Config.GL_DEBUG_CALLBACK.get(true);
    private final Phaser waitForVSync;
    private Callback debugCallback;
    private GLCapabilities gl;

    public GraphicsThread(@NotNull G game) {
        super(game, ExecutionPolicy.EXECUTE_ONE_PER_LOOP, false);
        waitForVSync = new Phaser() {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                return false;
            }
        };
    }

    @Override
    public boolean init() {
        if(!super.init()) {
            return false;
        }

        gl = game.initOpenGL(this);
        if(gl == null) {
            return false;
        }

        Game.log(Level.INFO, "OpenGL capabilities created");
        Game.log(Level.INFO, "OpenGL version: " + glGetString(GL_VERSION));
        Game.log(Level.INFO, "GLSL version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
        Game.log(Level.INFO, "OpenGL renderer: " + glGetString(GL_RENDERER));
        Game.log(Level.INFO, "OpenGL vendor: " + glGetString(GL_VENDOR));

        if(GL_DEBUG_CALLBACK) {
            debugCallback = setUpDebugCallback();
        }
        return true;
    }

    private Callback setUpDebugCallback() {
        if (gl.OpenGL43) {
            Game.logGL(Level.INFO, "Using OpenGL 4.3 for error logging.");
            var proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam)
                    -> Game.logGL(getDebugLevel(severity), GLDebugMessageCallback.getMessage(length, message)));
            GL43.glDebugMessageCallback(proc, NULL);
            if ((glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                Game.logGL(Level.WARNING, "A non-debug context may not produce any debug output.");
                glEnable(GL_DEBUG_OUTPUT);
                // filtering specific errors
                glDebugMessageControl(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DONT_CARE, 131185, false);
            }
            return proc;
        }

        if (gl.GL_KHR_debug) {
            Game.logGL(Level.INFO, "Using KHR_debug for error logging.");
            var proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam)
                    -> Game.logGL(getDebugLevel(severity), GLDebugMessageCallback.getMessage(length, message)));
            KHRDebug.glDebugMessageCallback(proc, NULL);
            if (gl.OpenGL30 && (glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                Game.logGL(Level.WARNING, "A non-debug context may not produce any debug output.");
                glEnable(GL_DEBUG_OUTPUT);
            }
            return proc;
        }

        if (gl.GL_ARB_debug_output) {
            Game.logGL(Level.INFO, "Using ARB_debug_output for error logging.");
            var proc = GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam)
                    -> Game.logGL(getDebugLevel(severity), GLDebugMessageCallback.getMessage(length, message)));
            ARBDebugOutput.glDebugMessageCallbackARB(proc, NULL);
            return proc;
        }

        if (gl.GL_AMD_debug_output) {
            Game.logGL(Level.INFO, "Using AMD_debug_output for error logging.");
            var proc = GLDebugMessageAMDCallback.create((id, category, severity, length, message, userParam)
                    -> Game.logGL(getDebugLevel(severity), GLDebugMessageCallback.getMessage(length, message)));
            AMDDebugOutput.glDebugMessageCallbackAMD(proc, NULL);
            return proc;
        }

        Game.logGL(Level.INFO, "No debug output implementation is available.");
        return null;
    }

    private Level getDebugLevel(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> Level.SEVERE;
            case GL_DEBUG_SEVERITY_MEDIUM, GL_DEBUG_SEVERITY_LOW -> Level.WARNING;
            default -> Level.INFO;
        };
    }

    private void signalVSync() {
        if(waitForVSync.getRegisteredParties() != 0) {
            glFinish();
            waitForVSync.arriveAndDeregister();
        }
    }

    public void awaitVSync() {
        waitForVSync.awaitAdvance(waitForVSync.register());
    }

    @Override
    public void loop() {
        super.loop();
        game.render();
        signalVSync();
    }

    @Override
    public void closeInThread() throws Exception {
        super.closeInThread();
        GL.setCapabilities(null);
        glfwMakeContextCurrent(NULL);
        if (debugCallback != null) {
            debugCallback.free();
        }
    }

    public int createGLTextureGraphics(@NotNull Image image) {
        var texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        var internalFormat = image.hasAlpha()? GL_RGBA8 : GL_RGB8;
        var format = image.hasAlpha()? GL_RGBA : GL_RGB;
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, image.size().width(), image.size().height(), 0,
                format, GL_UNSIGNED_BYTE, image.data());
        return texture;
    }

    public int createDefaultGLTextureGraphics(@NotNull Image image) {
        var texture = createGLTextureGraphics(image);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        return texture;
    }

    public @NotNull CompletableFuture<@NotNull Integer> createGLTexture(@NotNull Image image) {
        return CompletableFuture.supplyAsync(() -> createGLTextureGraphics(image), this);
    }

    public @NotNull CompletableFuture<@NotNull Integer> createDefaultGLTexture(@NotNull Image image) {
        return CompletableFuture.supplyAsync(() -> createDefaultGLTextureGraphics(image), this);
    }
}
