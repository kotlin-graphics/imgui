package imgui


import glm_.vec2.Vec2
import gln.checkError
import gln.glViewport
import imgui.functionalProgramming.button
import imgui.impl.LwjglGL3
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import uno.glfw.GlfwWindow
import uno.glfw.glfw

fun main(args: Array<String>) {
    HelloWorld_lwjgl().run()
}

class HelloWorld_lwjgl {

    val window: GlfwWindow

    init {

        with(glfw) {
            init()
            windowHint {
                context.version = "3.3"
                profile = "core"
            }
        }

        window = GlfwWindow(1280, 720, "ImGui Lwjgl OpenGL3 example")

        with(window) {
            makeContextCurrent()
            glfw.swapInterval = 1   // Enable vsync
            show()
        }

        GL.createCapabilities()
    }

    fun run() {

        // Setup ImGui binding
        LwjglGL3.init(window, true)

        // Load Fonts
        // (there is a default font, this is only if you want to change it. see extra_fonts/README.txt for more details)
        //ImGuiIO& io = ImGui::GetIO();
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/ProggyTiny.ttf", 10.0f);
        //io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());

        while (window.open)
            loop()

        LwjglGL3.shutdown()

        window.destroy()
        glfw.terminate()
    }

    val clearColor = floatArrayOf(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = booleanArrayOf(false)
    var showTestWindow = booleanArrayOf(true)

    fun loop() {

        checkError("a")

        glfwPollEvents()
        LwjglGL3.newFrame()

        checkError("b")

        with(ImGui) {

            /*  1. Show a simple window
                Tip: if we don't call ImGui::Begin()/ImGui::End() the widgets appears in a window automatically
                called "Debug"             */
            run {
                text("Hello, world!")
                sliderFloat("float", f, 0f, 1f)
                colorEdit3("clear color", clearColor)
                //  You can write functions in the classical way, with if(cond) { code }
                if (button("Test Window"))
                    showTestWindow[0] = !showTestWindow[0]
                // or you can take advantage of functional programming and pass directly a lambda as last parameter
                button("Another Window") {
                    showAnotherWindow[0] = !showAnotherWindow[0]
                }
                text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / IO.framerate, IO.framerate)
            }

            /*  2. Show another simple window, this time using an explicit Begin/End pair   */
            run {
                if (showAnotherWindow[0]) {
                    begin("Another Window", showAnotherWindow)
                    text("Hello from anoter window!")
                    end()
                }
            }

            /* 3. Show the ImGui test window. Most of the sample code is in ImGui::ShowTestWindow() */
            run {
                if (showTestWindow[0]) {
                    setNextWindowPos(Vec2(650, 20), Cond.FirstUseEver)
                    showTestWindow(showTestWindow)
                }
            }

            /*val buf = CharArray(256)

            text("Hello, world %d", 123)
            button("OK"){
                // react
            }
            inputText("string", buf)
            sliderFloat("float", f, 0f, 1f)*/
        }
        checkError("c")
        // Rendering
        glViewport(window.framebufferSize)
        glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])  // TODO gln
        glClear(GL_COLOR_BUFFER_BIT)

        ImGui.render()
        window.swapBuffers()
    }

    companion object {
        val f = FloatArray(1)
    }
}