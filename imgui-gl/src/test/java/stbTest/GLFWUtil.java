/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package stbTest;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.system.MemoryStack.stackPush;

/** GLFW demo utilities. */
public final class GLFWUtil {

    private GLFWUtil() {
    }

    /**
     * Invokes the specified callbacks using the current window and framebuffer sizes of the specified GLFW window.
     *
     * @param window            the GLFW window
     * @param windowSizeCB      the window size callback, may be null
     * @param framebufferSizeCB the framebuffer size callback, may be null
     */
    public static void glfwInvoke(
        long window,
        @Nullable GLFWWindowSizeCallbackI windowSizeCB,
        @Nullable GLFWFramebufferSizeCallbackI framebufferSizeCB
    ) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            if (windowSizeCB != null) {
                glfwGetWindowSize(window, w, h);
                windowSizeCB.invoke(window, w.get(0), h.get(0));
            }

            if (framebufferSizeCB != null) {
                glfwGetFramebufferSize(window, w, h);
                framebufferSizeCB.invoke(window, w.get(0), h.get(0));
            }
        }

    }

}
