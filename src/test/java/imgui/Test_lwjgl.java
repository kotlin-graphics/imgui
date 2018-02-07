package imgui;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import imgui.impl.LwjglGL3;
import org.lwjgl.opengl.GL;
import uno.glfw.GlfwWindow;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Test_lwjgl {

    public static void main(String[] args) {
        new Test_lwjgl().run();
    }

    // The window handle
    private GlfwWindow window;
    private uno.glfw.glfw glfw = uno.glfw.glfw.INSTANCE;
    private uno.glfw.windowHint windowHint = uno.glfw.windowHint.INSTANCE;
    private LwjglGL3 lwjglGL3 = LwjglGL3.INSTANCE;
    private ImGui imgui = ImGui.INSTANCE;
    private IO io = IO.INSTANCE;

    public void run() {

        init();

        // Setup ImGui binding
        lwjglGL3.init(window, true);

        // Setup style
        imgui.styleColorsDark(null);
//        imgui.styleColorsClassic(null);

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
        //ImGuiIO& io = ImGui::GetIO();
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
//        Font font = io.getFonts().addFontFromFileTTF("misc/fonts/ArialUni.ttf", 18f, new FontConfig(), io.getFonts().getGlyphRangesJapanese());
//        assert (font != null);

        while (window.isOpen()) loop();

        lwjglGL3.shutdown();

        window.destroy();
        glfw.terminate();
    }

    private void init() {

        glfw.init();
        windowHint.getContext().setVersion("3.2");
        windowHint.setProfile("core");

        window = new GlfwWindow(1280, 720, "ImGui Lwjgl OpenGL3 example");

        window.makeContextCurrent();
        glfw.setSwapInterval(1);    // Enable vsync
        window.show();

        /*  This line is critical for LWJGL's interoperation with GLFW's OpenGL context, or any context that is
            managed externally.
            LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance and
            makes the OpenGL bindings available for use.    */
        GL.createCapabilities();
    }

    private float[] f = {0f};
    private Vec4 clearColor = new Vec4(0.45f, 0.55f, 0.6f, 1f);
    private boolean[] showAnotherWindow = {false};
    private boolean[] showDemoWindow = {true};
//    private KMutableProperty0 prop = new JavaProp<>(() -> showDemoWindow, res -> showDemoWindow = res);

    private void loop() {

        /*  You can read the IO.wantCaptureMouse, IO.wantCaptureKeyboard flags to tell if dear imgui wants to use your
            inputs.
            - when IO.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - when Io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those
            two flags.  */
        glfw.pollEvents();
        lwjglGL3.newFrame();

            /*  1. Show a simple window.
                Tip: if we don't call ImGui::Begin()/ImGui::End() the widgets appears in a window automatically
                called "Debug"             */
        imgui.text("Hello, world!");
        imgui.sliderFloat("float", f, 0f, 1f, "%.3f", 1f);
        imgui.colorEdit3("clear color", clearColor, 0);
        //  You can write functions in the classical way, with if(cond) { code }
        if (imgui.button("Test Window", new Vec2()))
            showDemoWindow[0] = !showDemoWindow[0];
        if (imgui.button("Another Window", new Vec2()))
            showAnotherWindow[0] = !showAnotherWindow[0];
        imgui.text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.getFramerate(), io.getFramerate());

        /*  2. Show another simple window. In most cases you will use an explicit begin/end pair to name the window.*/
        if (showAnotherWindow[0]) {
            imgui.begin("Another Window", showAnotherWindow, 0);
            imgui.text("Hello from another window!");
            imgui.end();
        }

        /* 3. Show the ImGui demo window. Most of the sample code is in imgui.showDemoWindow(). */
        if (showDemoWindow[0]) {
            /*  Normally user code doesn't need/want to call this because positions are saved in .ini file anyway.
                    Here we just want to make the demo initial state a bit more friendly!                 */
            imgui.setNextWindowPos(new Vec2(650, 20), Cond.FirstUseEver, new Vec2());
            imgui.showDemoWindow(showDemoWindow);
//            imgui.showDemoWindow(prop);
        }

            /*val buf = CharArray(256)

            text("Hello, world %d", 123)
            button("OK"){
                // react
            }
            inputText("string", buf)
            sliderFloat("float", f, 0f, 1f)*/

        // Rendering
        gln.GlnKt.glViewport(window.getFramebufferSize());
        gln.GlnKt.glClearColor(clearColor);
        glClear(GL_COLOR_BUFFER_BIT);

        imgui.render();
        window.swapBuffers();

        gln.GlnKt.checkError("loop", true); // TODO remove
    }
}
