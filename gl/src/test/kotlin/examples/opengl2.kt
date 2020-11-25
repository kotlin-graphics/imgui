package examples


//import org.lwjgl.util.remotery.Remotery
//import org.lwjgl.util.remotery.RemoteryGL
import glm_.vec4.Vec4
import gln.checkError
import gln.glClearColor
import gln.glViewport
import imgui.DEBUG
import imgui.ImGui
import imgui.classes.Context
import imgui.impl.gl.ImplGL2
import imgui.impl.glfw.ImplGlfw
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.system.MemoryStack
import uno.glfw.GlfwWindow
import uno.glfw.VSync
import uno.glfw.glfw


fun main() {
    ImGuiOpenGL2()
}

private class ImGuiOpenGL2 {

    val window: GlfwWindow
    val ctx: Context

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
    // Our state
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    val implGlfw: ImplGlfw
    val implGl2: ImplGL2

//    val rmt = MemoryUtil.memAllocPointer(1).also { Remotery.rmt_CreateGlobalInstance(it) }

    init {

        // Setup window
        glfw {
            errorCallback = { error, description -> println("Glfw Error $error: $description") }
            init()
            windowHint { debug = DEBUG }
        }

        // Create window with graphics context
        window = GlfwWindow(1280, 720, "Dear ImGui GLFW+OpenGL3 OpenGL example")
        window.makeContextCurrent()
        glfw.swapInterval = VSync.ON   // Enable vsync

        // Initialize OpenGL loader
        GL.createCapabilities()

        // Setup Dear ImGui context
        ctx = Context()
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableKeyboard  // Enable Keyboard Controls
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableGamepad   // Enable Gamepad Controls

        // Setup Dear ImGui style
        ImGui.styleColorsDark()
//        ImGui.styleColorsClassic()

        // Setup Platform/Renderer backends
        implGlfw = ImplGlfw(window, true)
        implGl2 = ImplGL2()

//        RemoteryGL.rmt_BindOpenGL()

        // Load Fonts
        /*  - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use
                pushFont()/popFont() to select them.
            - addFontFromFileTTF() will return the Font so you can store it if you need to select the font among multiple.
            - If the file cannot be loaded, the function will return null. Please handle those errors in your application
                (e.g. use an assertion, or display an error and quit).
            - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling
                FontAtlas.build()/getTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
            - Read 'docs/FONTS.txt' for more instructions and details.
            - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write
                a double backslash \\ ! */
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
//        ImGui.io.fonts.addFontFromFileTTF("extraFonts/ArialUni.ttf", 16f, glyphRanges = imgui.font.glyphRanges.japanese)!!
//        val a = IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 18f)!!
//        val b = IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 30f)!!

        /*  Main loop
            This automatically also polls events, swaps buffers and resets the appBuffer

            Poll and handle events (inputs, window resize, etc.)
            You can read the io.wantCaptureMouse, io.wantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
            - When io.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - When io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.          */
        window.loop(::mainLoop)

        implGl2.shutdown()
        implGlfw.shutdown()
        ctx.destroy()

//        Remotery.rmt_DestroyGlobalInstance(rmt.get(0))

        window.destroy()
        glfw.terminate()
    }

    fun mainLoop(stack: MemoryStack) {

//        RemoteryGL.rmt_BeginOpenGLSample("imgui", null)

        // Start the Dear ImGui frame
        implGl2.newFrame()
        implGlfw.newFrame()

        ImGui.run {

            newFrame()

            // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
            if (showDemo)
                showDemoWindow(::showDemo)

            // 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
            run {

                begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

                text("This is some useful text.")                // Display some text (you can use a format strings too)
                checkbox("Demo Window", ::showDemo)             // Edit bools storing our window open/close state
                checkbox("Another Window", ::showAnotherWindow)

                sliderFloat("float", ::f, 0f, 1f)   // Edit 1 float using a slider from 0.0f to 1.0f
                colorEdit3("clear color", clearColor)           // Edit 3 floats representing a color

                if (button("Button"))                           // Buttons return true when clicked (most widgets return true when edited/activated)
                    counter++

                /*  Or you can take advantage of functional programming and pass directly a lambda as last parameter:
                    button("Button") { counter++ }                */

                sameLine()
                text("counter = $counter")

                text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.framerate, io.framerate)

                end()

                // 3. Show another simple window.
                if (showAnotherWindow) {
                    // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
                    begin("Another Window", ::showAnotherWindow)
                    text("Hello from another window!")
                    if (button("Close Me"))
                        showAnotherWindow = false
                    end()
                }
            }

            // Rendering
            render()
            glViewport(window.framebufferSize)
            glClearColor(clearColor)
            glClear(GL_COLOR_BUFFER_BIT)

            implGl2.renderDrawData(drawData!!)
        }

        if (DEBUG)
            checkError("mainLoop")

//        RemoteryGL.rmt_EndOpenGLSample()
    }
}