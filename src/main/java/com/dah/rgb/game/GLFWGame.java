package com.dah.rgb.game;

import com.dah.rgb.annotations.CalledInAnyThread;
import com.dah.rgb.annotations.CalledInGraphicsThread;
import com.dah.rgb.annotations.CalledInMainThread;
import com.dah.rgb.annotations.NotNull;
import com.dah.rgb.threads.impl.GraphicsThread;
import com.dah.rgb.utils.Dimension;
import org.joml.Vector2f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class GLFWGame<SELF extends GLFWGame<SELF>> extends Game<SELF> {
    protected final @NotNull AtomicReference<Dimension> framebufferSize;
    protected final @NotNull AtomicReference<Vector2f> contextScale;
    protected final @NotNull String title;

    protected boolean glfwInit;
    protected long window;

    public GLFWGame(int width, int height, @NotNull String title) {
        this.framebufferSize = new AtomicReference<>(new Dimension(width, height));
        this.contextScale = new AtomicReference<>(new Vector2f(1.0f));
        this.title = title;
    }

    @Override
    @CalledInMainThread
    protected boolean createDisplay() {
        if(!(glfwInit = glfwInit())) {
            log(Level.SEVERE, "Unable to initialize GLFW context");
            return false;
        }

        var size = framebufferSize.get();
        if(LWJGL_DEBUG) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }
        window = glfwCreateWindow(size.width(), size.height(), title, NULL, NULL);

        if(window == NULL) {
            log(Level.SEVERE, "Unable to create GLFW window");
            return false;
        }

        glfwSetWindowCloseCallback(window, w -> running.set(false));
        glfwSetFramebufferSizeCallback(window, (w, width, height) -> framebufferSize.set(new Dimension(width, height)));
        glfwSetWindowContentScaleCallback(window, (w, xscale, yscale) -> contextScale.set(new Vector2f(xscale, yscale)));

        return true;
    }

    @Override
    @CalledInMainThread
    protected void pollEvents() {
        glfwWaitEventsTimeout(1e-3);
    }

    @Override
    @CalledInMainThread
    public void close() {
        super.close();
        if(window != NULL) {
            Callbacks.glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
        if(glfwInit) {
            glfwTerminate();
        }
    }

    @CalledInAnyThread
    public long getWindow() {
        return window;
    }

    @CalledInAnyThread
    public Dimension getFramebufferSize() {
        return framebufferSize.get();
    }

    @CalledInAnyThread
    public Vector2f getContextScale() {
        return contextScale.get();
    }

    @Override
    @CalledInGraphicsThread
    public GLCapabilities initOpenGL(GraphicsThread<SELF> graphicsThread) {
        glfwMakeContextCurrent(window);
        var gl = GL.createCapabilities();
        glfwSwapInterval(1);

        glfwSetWindowRefreshCallback(window, w -> {
            var dimension = getFramebufferSize();
            if(dimension.width() != 0 && dimension.height() != 0) {
                graphicsThread.awaitVSync();
            }
        });
        return gl;
    }

    @Override
    public void render() {
        var dimension = getFramebufferSize();
        glViewport(0, 0, dimension.width(), dimension.height());
        glClearColor(0.0f, 0.0f, 1.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        super.render();
        glfwSwapBuffers(window);
    }
}
