package imgui


import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import uno.glfw.GlfwWindow
import uno.glfw.glfw

fun main(args: Array<String>) {
    HelloWorld().run()
}

class HelloWorld {

    // The window handle
    val window: GlfwWindow
    val a = BooleanArray(5)

    init {

        with(glfw) {
            init()
            windowHint {
                context.version = "3.3"
                profile = "core"
            }
        }

        window = GlfwWindow(1280, 720, "ImGui OpenGL3 example")

        with(window) {
            makeContextCurrent()
            show()
        }

        GL.createCapabilities()
    }

    fun run() {

        println("Hello LWJGL " + Version.getVersion() + "!")

        loop()

        window.destroy()
        glfw.terminate()
    }

    private fun loop() {

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)

        // Run the rendering loop until the user has attempted to close the window or has pressed the ESCAPE key.
//        while (!glfwWindowShouldClose(window)) {
//            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer
//
//            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be invoked during this call.
//            glfwPollEvents()
//        }
    }
}