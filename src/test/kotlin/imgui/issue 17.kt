//package imgui
//
//import glm_.vec2.Vec2
//import glm_.vec4.Vec4
//import gln.checkError
//import gln.glClearColor
//import gln.glViewport
//import imgui.functionalProgramming.button
//import imgui.impl.LwjglGL3
//import org.lwjgl.opengl.GL
//import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
//import org.lwjgl.opengl.GL11.glClear
//import uno.glfw.GlfwWindow
//import uno.glfw.glfw
//
//fun main(args: Array<String>) {
//    test().run()
//}
//
//class test {
//
//    val window: GlfwWindow
//
//    init {
//
//        with(glfw) {
//            init()
//            windowHint {
//                context.version = "3.3"
//                profile = "core"
//            }
//        }
//
//        window = GlfwWindow(1280, 720, "ImGui Lwjgl OpenGL3 example")
//
//        with(window) {
//            makeContextCurrent()
//            glfw.swapInterval = 1   // Enable vsync
//            show()
//        }
//
//        GL.createCapabilities()
//    }
//
//    fun run() {
//
//        // Setup ImGui binding
//        LwjglGL3.init(window, true)
//
//        // Setup style
//        ImGui.styleColorsClassic()
//        //ImGui.styleColorsDark()
//
//        // Load Fonts
//        /*  - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use
//                pushFont()/popFont() to select them.
//            - addFontFromFileTTF() will return the Font so you can store it if you need to select the font among multiple.
//            - If the file cannot be loaded, the function will return null. Please handle those errors in your application
//                (e.g. use an assertion, or display an error and quit).
//            - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling
//                FontAtlas.build()/getTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
//            - Read 'extra_fonts/README.txt' for more instructions and details.
//            - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write
//                a double backslash \\ ! */
//        //io.Fonts->AddFontDefault();
//        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Roboto-Medium.ttf", 16.0f);
//        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Cousine-Regular.ttf", 15.0f);
//        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/DroidSans.ttf", 16.0f);
//        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/ProggyTiny.ttf", 10.0f);
////        IO.fonts.addFontFromFileTTF("extraFonts/ArialUni.ttf", 18f, glyphRanges = IO.fonts.glyphRangesJapanese)!!
//
//        while (window.isOpen) loop()
//
//        LwjglGL3.shutdown()
//
//        window.destroy()
//        glfw.terminate()
//    }
//
//    var f = 0f
//    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
//    var showAnotherWindow = false
//    var showDemo = true
//    var listboxItemCurrent = 1
//
//    var buf = Array(3) { CharArray(255) }
//    var windowOpen = BooleanArray(1)
//
//    fun loop() {
//
//        /*  You can read the IO.wantCaptureMouse, IO.wantCaptureKeyboard flags to tell if dear imgui wants to use your
//            inputs.
//            - when IO.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
//            - when Io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
//            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those
//            two flags.  */
//        glfw.pollEvents()
//        LwjglGL3.newFrame()
//
//        with(ImGui) {
//
//            val (w, h) = window.framebufferSize
//            setNextWindowPos(Vec2((w - 350) / 2, (h - 350) / 2), Cond.FirstUseEver)
//            setNextWindowSize(Vec2(350), Cond.FirstUseEver)
//            if (begin("Test", windowOpen, 0)) {
//                text("Test")
////                for (i in 0 until buf.size) {
//                text("0")
//                inputText("", buf[0], InputTextFlags.EnterReturnsTrue.i)
//                text("1")
//                inputText("", buf[1], InputTextFlags.EnterReturnsTrue.i)
////                }
//                end()
//            }
//        }
//
//        // Rendering
//        glViewport(window.framebufferSize)
//        glClearColor(clearColor)
//        glClear(GL_COLOR_BUFFER_BIT)
//
//        ImGui.render()
//        window.swapBuffers()
//
//        checkError("loop") // TODO remove
//    }
//}