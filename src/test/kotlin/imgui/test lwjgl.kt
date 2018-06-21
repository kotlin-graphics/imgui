package imgui


import glm_.vec2.Vec2
import glm_.vec4.Vec4
import gln.checkError
import gln.glClearColor
import gln.glViewport
import imgui.impl.LwjglGL3
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
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
                context.version = "3.2"
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
//         glslVersion = 330 // set here your desidered glsl version
        val ctx = Context()
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableKeyboard  // Enable Keyboard Controls
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableGamepad   // Enable Gamepad Controls
        LwjglGL3.init(window, true)

        // Setup style
        ImGui.styleColorsDark()
//        ImGui.styleColorsClassic()

        // Load Fonts
        /*  - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use
                pushFont()/popFont() to select them.
            - addFontFromFileTTF() will return the Font so you can store it if you need to select the font among multiple.
            - If the file cannot be loaded, the function will return null. Please handle those errors in your application
                (e.g. use an assertion, or display an error and quit).
            - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling
                FontAtlas.build()/getTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
            - Read 'misc/fonts/README.txt' for more instructions and details.
            - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write
                a double backslash \\ ! */
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
//        IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 18f, glyphRanges = IO.fonts.glyphRangesJapanese)!!
//        val a = IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 18f)!!
//        val b = IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 30f)!!

        window.loop(this::loop)

        LwjglGL3.shutdown()
        ctx.destroy()

        window.destroy()
        glfw.terminate()
    }

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    fun loop() {

        /*  You can read the IO.wantCaptureMouse, IO.wantCaptureKeyboard flags to tell if dear imgui wants to use your
            inputs.
            - when IO.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - when Io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those
            two flags.  */
        LwjglGL3.newFrame()

        with(ImGui) {

            text("Hello, world!")                                // Display some text (you can use a format string too)
            sliderFloat("float", ::f, 0f, 1f)       // Edit 1 float using a slider from 0.0f to 1.0f
            colorEdit3("clear color", clearColor)               // Edit 3 floats representing a color

            checkbox("Demo Window", ::showDemo)                 // Edit bools storing our windows open/close state
            checkbox("Another Window", ::showAnotherWindow)

            if (button("Button"))                               // Buttons return true when clicked (NB: most widgets return true when edited/activated)
                counter++

            /* Or you can take advantage of functional programming and pass directly a lambda as last parameter:

                button("Button") { counter++ }
            */

            sameLine()
            text("counter = $counter")

            text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.framerate, io.framerate)

            // 2. Show another simple window. In most cases you will use an explicit begin/end pair to name your windows.
            if (showAnotherWindow) {
                begin_("Another Window", ::showAnotherWindow)
                text("Hello from another window!")
                if (button("Close Me"))
                    showAnotherWindow = false
                end()
            }

            /*  3. Show the ImGui demo window. Most of the sample code is in imgui.showDemoWindow().
                Read its code to learn more about Dear ImGui!  */
            if (showDemo) {
                /*  Normally user code doesn't need/want to call this because positions are saved in .ini file anyway.
                    Here we just want to make the demo initial state a bit more friendly!                 */
                setNextWindowPos(Vec2(650, 20), Cond.FirstUseEver)
                showDemoWindow(::showDemo)
            }
        }

        // Rendering
        glViewport(window.framebufferSize)
        glClearColor(clearColor)
        glClear(GL_COLOR_BUFFER_BIT)

        ImGui.render()
        LwjglGL3.renderDrawData(ImGui.drawData!!)

        checkError("loop") // TODO remove in production
    }
}