package imgui


import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import uno.buffer.intBufferBig
import java.awt.Font
import java.awt.Shape
import java.awt.font.FontRenderContext

fun main(args: Array<String>) {
    HelloWorld().run()
}

class HelloWorld {

    // The window handle
    private var window = 0L

    init {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
        }

        val pWidth = intBufferBig(1) // int*
        val pHeight = intBufferBig(1) // int*

        // Get the window size passed to glfwCreateWindow
        glfwGetWindowSize(window, pWidth, pHeight)

        // Get the resolution of the primary monitor
        val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

        // Center the window
        glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
        )

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)

        val fontUrl = ClassLoader.getSystemResource("ProggyClean.ttf")
        val font = Font.createFont(Font.TRUETYPE_FONT, fontUrl.openStream()).deriveFont(Font.PLAIN, 20f)
        val glyphs = font.createGlyphVector(FontRenderContext(null, false, false), "a")
        val metric = glyphs.getGlyphMetrics(0)
        val outline: Shape = glyphs.getOutline(0f, 0f)
    }

    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")

        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null).free()
    }

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's OpenGL context, or any context that is managed
        // externally.
        // LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance and
        // makes the OpenGL bindings available for use.
        GL.createCapabilities()

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)

        // Run the rendering loop until the user has attempted to close the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be invoked during this call.
            glfwPollEvents()
        }
    }
}