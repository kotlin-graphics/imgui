package imgui;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;
import glm_.vec4.Vec4;
import imgui.impl.LwjglGL3;
import kotlin.Unit;
import org.lwjgl.opengl.GL;
import uno.glfw.GlfwWindow;
import uno.glfw.windowHint;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.system.MemoryUtil.NULL;
import uno.glfw.windowHint.Profile;

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
    private IO io;


    private float[] f = {0f};
    private Vec4 clearColor = new Vec4(0.45f, 0.55f, 0.6f, 1f);
    private boolean[] showAnotherWindow = {false};
    private boolean[] showDemo = {true};
    private int[] counter = {0};


    public void run() {

        glfw.init("3.2", Profile.core, true);

        window = new GlfwWindow(1280, 720, "ImGui Lwjgl OpenGL3 example", NULL, new Vec2i(Integer.MIN_VALUE), true);
        window.init(true);

        glfw.setSwapInterval(1);    // Enable vsync

        // Setup ImGui binding
        //setGlslVersion(330); // set here your desidered glsl version
        Context ctx = new Context(null);
        lwjglGL3.init(window, true);

        io = imgui.getIo();

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

        /*  Main loop
            This automatically also polls events, swaps buffers and resets the appBuffer

            Poll and handle events (inputs, window resize, etc.)
            You can read the io.wantCaptureMouse, io.wantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
            - When io.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - When io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.          */
        window.loop(() -> {
            mainLoop();
            return Unit.INSTANCE;
        });

        lwjglGL3.shutdown();
        ContextKt.destroy(ctx);

        glfw.terminate();
    }

    private void mainLoop() {

        // Start the Dear ImGui frame
        lwjglGL3.newFrame();


        imgui.text("Hello, world!");                                // Display some text (you can use a format string too)
        imgui.sliderFloat("float", f, 0f, 1f, "%.3f", 1f);       // Edit 1 float using a slider from 0.0f to 1.0f
        imgui.colorEdit3("clear color", clearColor, 0);               // Edit 3 floats representing a color

        imgui.checkbox("Demo Window", showDemo);                 // Edit bools storing our windows open/close state
        imgui.checkbox("Another Window", showAnotherWindow);

        if (imgui.button("Button", new Vec2()))                               // Buttons return true when clicked (NB: most widgets return true when edited/activated)
            counter[0]++;

        imgui.sameLine(0f, -1f);
        imgui.text("counter = $counter");

        imgui.text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.getFramerate(), io.getFramerate());

        // 2. Show another simple window. In most cases you will use an explicit begin/end pair to name the window.
        if (showAnotherWindow[0]) {
            imgui.begin("Another Window", showAnotherWindow, 0);
            imgui.text("Hello from another window!");
            if (imgui.button("Close Me", new Vec2()))
                showAnotherWindow[0] = false;
            imgui.end();
        }

        /*  3. Show the ImGui demo window. Most of the sample code is in imgui.showDemoWindow().
                Read its code to learn more about Dear ImGui!  */
        if (showDemo[0]) {
            /*  Normally user code doesn't need/want to call this because positions are saved in .ini file anyway.
                    Here we just want to make the demo initial state a bit more friendly!                 */
            imgui.setNextWindowPos(new Vec2(650, 20), Cond.FirstUseEver, new Vec2());
            imgui.showDemoWindow(showDemo);
        }

        // Rendering
        gln.GlnKt.glViewport(window.getFramebufferSize());
        gln.GlnKt.glClearColor(clearColor);
        glClear(GL_COLOR_BUFFER_BIT);

        imgui.render();
        lwjglGL3.renderDrawData(imgui.getDrawData());

        gln.GlnKt.checkError("loop", true); // TODO remove
    }
}
