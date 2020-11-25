package app

import engine.core.TestEngineScreenCaptureFunc
import engine.core.showTestWindow
import glm_.d
import glm_.f
import imgui.ImGui
import imgui.Key
import kool.lib.fill
import kool.set
import org.lwjgl.opengl.GL11C.*
import java.nio.ByteBuffer


var showDemoWindow_ = true
var showAnotherWindow = false

var f = 0f
var counter = 0

fun mainLoopEndFrame(): Boolean {

    app.testEngine!!.showTestWindow()

    // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
    if (showDemoWindow_)
        ImGui.showDemoWindow(::showDemoWindow_)

    // 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
    ImGui.apply {

        begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

        text("This is some useful text.")               // Display some text (you can use a format strings too)
        checkbox("Demo Window", ::showDemoWindow_)      // Edit bools storing our window open/close state
        checkbox("Another Window", ::showAnotherWindow)

        sliderFloat("float", ::f, 0f, 1f)            // Edit 1 float using a slider from 0.0f to 1.0f
        colorEdit3("clear color", app.clearColor) // Edit 3 floats representing a color

        if (button("Button"))                            // Buttons return true when clicked (most widgets return true when edited/activated)
            counter++
        sameLine()
        text("counter = $counter")

        text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
        end()
    }

    // 3. Show another simple window.
    if (showAnotherWindow) {
        ImGui.begin("Another Window", ::showAnotherWindow)   // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
        ImGui.text("Hello from another window!")
        if (ImGui.button("Close Me"))
            showAnotherWindow = false
        ImGui.end()
    }

    ImGui.endFrame()

    return true
}

//-------------------------------------------------------------------------
// Backend: Null
//-------------------------------------------------------------------------

fun mainLoopNewFrameNull(): Boolean {
    val io = ImGui.io
    io.displaySize.put(1920, 1080)

    val time = System.nanoTime() / 1_000 // us
    if (app.lastTime == 0L)
        app.lastTime = time
    io.deltaTime = ((time - app.lastTime).d / 1000000.0).f
    if (io.deltaTime <= 0f)
        io.deltaTime = 0.000001f
    app.lastTime = time

    ImGui.newFrame()

    val testIo = app.testEngine!!.io
    return testIo.runningTests
}

fun mainLoopNull() {
    // Init
    ImGui.io.apply {
        fonts.build()
        for (n in 0 until Key.COUNT)
            keyMap[n] = n
    }
    app.testEngine!!.io.apply {
        configLogToTTY = true
        newFrameFunc = { _, _ -> mainLoopNewFrameNull() }
        endFrameFunc = { _, _ -> mainLoopEndFrame() }
    }
    while (true) {
        if (!mainLoopNewFrameNull()) break
        if (!mainLoopEndFrame()) break
    }
}

val captureScreenshotNull: TestEngineScreenCaptureFunc = { _, _, _, _, pixels, _ -> pixels.fill(0); true }

//-------------------------------------------------------------------------
// Backend: GLFW + OpenGL3
//------------------------------------------------------------------------

//static bool MainLoopNewFrameGLFWGL3(GLFWwindow* window)
//{
//    // Poll and handle events (inputs, window resize, etc.)
//    // You can read the io.WantCaptureMouse, io.WantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
//    // - When io.WantCaptureMouse is true, do not dispatch mouse input data to your main application.
//    // - When io.WantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
//    // Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.
//    glfwPollEvents();
//    if (glfwWindowShouldClose(window))
//    {
//        g_App.Quit = true;
//        ImGuiTestEngine_Abort(g_App.TestEngine);
//    }
//    // Start the Dear ImGui frame
//    if (!g_App.Quit)
//    {
//        ImGui_ImplOpenGL3_NewFrame();
//        ImGui_ImplGlfw_NewFrame();
//        ImGui::NewFrame();
//    }
//    return !g_App.Quit;
//}
//
//static void glfw_error_callback(int error, const char* description)
//{
//    fprintf(stderr, "Glfw Error %d: %s\n", error, description);
//}

fun mainLoop() {
    // Setup window
//    glfwSetErrorCallback(glfw_error_callback);
//    if (!glfwInit())
//        return;
//
//    // Decide GL+GLSL versions
//    #if __APPLE__
//    // GL 3.2 + GLSL 150
//    const char* glsl_version = "#version 150";
//    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
//    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);  // 3.2+ only
//    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);            // Required on Mac
//    #else
//    // GL 3.0 + GLSL 130
//    const char* glsl_version = "#version 130";
//    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
//    //glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);  // 3.2+ only
//    //glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);            // 3.0+ only
//    #endif
//
//    // Create window with graphics context
//    GLFWwindow* window = glfwCreateWindow(1280, 720, "Dear ImGui GLFW+OpenGL3 example", NULL, NULL);
//    if (window == NULL)
//        return;
//    glfwMakeContextCurrent(window);
//
//    // Initialize OpenGL loader
//    bool err = gl3wInit() != 0;
//    if (err)
//    {
//        fprintf(stderr, "Failed to initialize OpenGL loader!\n");
//        return;
//    }
//
//    // Setup Platform/Renderer bindings
//    ImGui_ImplGlfw_InitForOpenGL(window, true);
//    ImGui_ImplOpenGL3_Init(glsl_version);
//
//    // Init
//    ImGuiIO& io = ImGui::GetIO();
//    io.Fonts->Build();
//
//    ImGuiTestEngineIO& test_io = ImGuiTestEngine_GetIO(g_App.TestEngine);
//    test_io.UserData = window;
//    test_io.ConfigLogToTTY = true;
//    test_io.NewFrameFunc = [](ImGuiTestEngine*, void* window) { return MainLoopNewFrameGLFWGL3((GLFWwindow *)window); };
//    test_io.EndFrameFunc = [](ImGuiTestEngine*, void* window)
//    {
//        if (!MainLoopEndFrame())
//            return false;
//        ImGuiTestEngineIO& test_io = ImGuiTestEngine_GetIO(g_App.TestEngine);
//        #if 0
//        // Super fast mode doesn't render/present
//        if (test_io.RunningTests && test_io.ConfigRunFast)
//            return true;
//        #endif
//        ImGui::Render();
//        ImGuiIO& io = ImGui::GetIO();
//        #ifdef IMGUI_HAS_VIEWPORT
//            if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable)
//        {
//            GLFWwindow* backup_current_context = glfwGetCurrentContext();
//            ImGui::UpdatePlatformWindows();
//            ImGui::RenderPlatformWindowsDefault();
//            glfwMakeContextCurrent(backup_current_context);
//        }
//        #endif
//        glfwSwapInterval(((test_io.RunningTests && test_io.ConfigRunFast) || test_io.ConfigNoThrottle) ? 0 : 1); // Enable vsync
//        glViewport(0, 0, (int)io.DisplaySize.x, (int)io.DisplaySize.y);
//        glClearColor(g_App.ClearColor.x, g_App.ClearColor.y, g_App.ClearColor.z, g_App.ClearColor.w);
//        glClear(GL_COLOR_BUFFER_BIT);
//        ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
//        glfwSwapBuffers((GLFWwindow *)window);
//        return true;
//    };
//
//    while (1)
//    {
//        if (!MainLoopNewFrameGLFWGL3(window))
//            break;
//        if (!test_io.EndFrameFunc(nullptr, window))
//            break;
//    }
//
//    // Cleanup
//    ImGui_ImplOpenGL3_Shutdown();
//    ImGui_ImplGlfw_Shutdown();
//
//    glfwDestroyWindow(window);
//    glfwTerminate();
}

val captureFramebufferScreenshot: TestEngineScreenCaptureFunc =
        { x: Int, y: Int, w: Int, h: Int, pixels: ByteBuffer, _: Any? ->
            val y2 = ImGui.io.displaySize.y - (y + h)
            glPixelStorei(GL_PACK_ALIGNMENT, 1)
            glReadPixels(x, y2, w, h, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

            // Flip vertically
            val comp = 4
            val stride = w * comp
            val lineTmp = ByteArray(stride)
            var lineA = 0
            var lineB = stride * (h - 1)
            while (lineA < lineB) {
                repeat(stride) { lineTmp[it] = pixels[lineA + it] }
                repeat(stride) { pixels[lineA + it] = pixels[lineB + it] }
                repeat(stride) { pixels[lineB + it] = lineTmp[it] }
                lineA += stride
                lineB -= stride
            }
            true
        }
