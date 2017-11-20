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

        // Load Fonts
        /*  - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use
                pushFont()/popFont() to select them.
            - addFontFromFileTTF() will return the Font so you can store it if you need to select the font among multiple.
            - If the file cannot be loaded, the function will return null. Please handle those errors in your application
                (e.g. use an assertion, or display an error and quit).
            - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling
                FontAtlas.build()/getTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
            - Read 'extra_fonts/README.txt' for more instructions and details.
            - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write
                a double backslash \\ ! */
        //ImGuiIO& io = ImGui::GetIO();
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/ProggyTiny.ttf", 10.0f);
        //ImFont* font = io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
        //IM_ASSERT(font != NULL);

        while (window.getOpen()) loop();

        lwjglGL3.shutdown();

        window.destroy();
        glfw.terminate();
    }

    private void init() {

        glfw.init();
        windowHint.getContext().setVersion("3.3");
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
    private boolean[] showTestWindow = {true};
//    private KMutableProperty0 prop = new JavaProp<>(() -> showTestWindow, res -> showTestWindow = res);

    private void loop() {

        glfw.pollEvents();
        lwjglGL3.newFrame();

            /*  1. Show a simple window
                Tip: if we don't call ImGui::Begin()/ImGui::End() the widgets appears in a window automatically
                called "Debug"             */
        imgui.text("Hello, world!");
        imgui.sliderFloat("float", f, 0f, 1f, "%.3f", 1f);
        imgui.colorEdit3("clear color", clearColor, 0);
        //  You can write functions in the classical way, with if(cond) { code }
        if (imgui.button("Test Window", new Vec2()))
            showTestWindow[0] = !showTestWindow[0];
        if (imgui.button("Another Window", new Vec2()))
            showAnotherWindow[0] = !showAnotherWindow[0];
        imgui.text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.getFramerate(), io.getFramerate());

        /*  2. Show another simple window, this time using an explicit Begin/End pair   */
        if (showAnotherWindow[0]) {
            imgui.begin("Another Window", showAnotherWindow, 0);
            imgui.text("Hello from another window!");
            imgui.end();
        }

        /* 3. Show the ImGui test window. Most of the sample code is in ImGui::ShowTestWindow() */
        if (showTestWindow[0]) {
            imgui.setNextWindowPos(new Vec2(650, 20), Cond.FirstUseEver, new Vec2());
            imgui.showTestWindow(showTestWindow);
//            imgui.showTestWindow(prop);
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
