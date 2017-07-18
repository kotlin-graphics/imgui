package imgui


import imgui.impl.GlfwGL3
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import uno.glfw.GlfwWindow
import uno.glfw.glfw
import uno.gln.glClearColor
import uno.gln.glViewport

fun main(args: Array<String>) {
    HelloWorld().run()
}

class HelloWorld {

    val window: GlfwWindow

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

        // Setup ImGui binding
        GlfwGL3.init(window, true)

        // Load Fonts
        // (there is a default font, this is only if you want to change it. see extra_fonts/README.txt for more details)
        //ImGuiIO& io = ImGui::GetIO();
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/ProggyClean.ttf", 13.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/ProggyTiny.ttf", 10.0f);
        //io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());

        while (window.open)
            loop()

        GlfwGL3.shutdown()

        window.destroy()
        glfw.terminate()
    }

    val clearColor = Color(114, 144, 154)

    fun loop() {


        glfwPollEvents()
        GlfwGL3.newFrame()

        with(ImGui) {
            text("Hello, world!")
            sliderFloat("float", f, 0f, 1f)
//                setNextWindowSize(Vec2(200, 100), SetCond.FirstUseEver)
//                begin("Another Window", false)
////            text("Hello")
//                end()
        }

        // Rendering
        glViewport(window.framebufferSize)
        glClearColor(clearColor.value)  // TODO gln
        glClear(GL_COLOR_BUFFER_BIT)
        ImGui.render()
        window.swapBuffers()
    }

    companion object {
        val f = FloatArray(1)
    }
}