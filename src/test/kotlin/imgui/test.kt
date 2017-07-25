package imgui


import glm_.vec2.Vec2
import imgui.impl.GlfwGL3
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import uno.glfw.GlfwWindow
import uno.glfw.glfw
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

        clearColor = Color(114, 144, 154).run { floatArrayOf(value.x, value.y, value.z) }

        while (window.open)
            loop()

        GlfwGL3.shutdown()

        window.destroy()
        glfw.terminate()
    }

    lateinit var clearColor: FloatArray
    var showAnotherWindow = booleanArrayOf(false)
    var showTestWindow = booleanArrayOf(false)

    fun loop() {


        glfwPollEvents()
        GlfwGL3.newFrame()

        with(ImGui) {

            /*  1. Show a simple window
                Tip: if we don't call ImGui::Begin()/ImGui::End() the widgets appears in a window automatically
                called "Debug"             */
            run {
                text("Hello, world!")
                sliderFloat("float", f, 0f, 1f)
                colorEdit3("clear color", clearColor)
                button("Test Window") { showTestWindow[0] = !showTestWindow[0] }
                button("Another Window") { showAnotherWindow[0] = !showAnotherWindow[0] }
                text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / IO.framerate, IO.framerate)
            }

            /*  2. Show another simple window, this time using an explicit Begin/End pair   */
            run {
                if (showAnotherWindow[0]) {
                    setNextWindowSize(Vec2(200, 100), SetCond.FirstUseEver)
                    begin("Another Window", showAnotherWindow)
                    text("Hello")
                    end()
                }
            }

            /* 3. Show the ImGui test window. Most of the sample code is in ImGui::ShowTestWindow() */
            run {
                if (showTestWindow[0]) {
                    setNextWindowPos(Vec2(650, 20), SetCond.FirstUseEver)
                    showTestWindow(showTestWindow)
                }
            }
        }

        // Rendering
        glViewport(window.framebufferSize)
        glClearColor(clearColor[0], clearColor[1], clearColor[2], 1f)  // TODO gln
        glClear(GL_COLOR_BUFFER_BIT)
        ImGui.render()
        window.swapBuffers()
    }

    companion object {
        val f = FloatArray(1)
    }
}