package imgui.examples;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;
import glm_.vec4.Vec4;
import imgui.Cond;
import imgui.ConfigFlag;
import imgui.ImGui;
import imgui.MutableProperty0;
import imgui.classes.Context;
import imgui.classes.IO;
import imgui.impl.gl.ImplGL3;
import imgui.impl.glfw.ImplGlfw;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import uno.glfw.GlfwWindow;
import uno.glfw.VSync;

import static gln.GlnKt.glClearColor;
import static gln.GlnKt.glViewport;
import static imgui.ImguiKt.DEBUG;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static uno.glfw.windowHint.Profile.core;

public class OpenGL3 {

    // The window handle
    private GlfwWindow window;
    private Context ctx;

    private uno.glfw.glfw glfw = uno.glfw.glfw.INSTANCE;
    private uno.glfw.windowHint windowHint = uno.glfw.windowHint.INSTANCE;
    private ImGui imgui = ImGui.INSTANCE;
    private IO io;


    private float[] f = {0f};
    private Vec4 clearColor = new Vec4(0.45f, 0.55f, 0.6f, 1f);
    // Java users can use both a MutableProperty0 or a Boolean Array
    private MutableProperty0<Boolean> showAnotherWindow = new MutableProperty0<>(false);
    private boolean[] showDemo = {true};
    private int[] counter = {0};

    private ImplGlfw implGlfw;
    private ImplGL3 implGl3;

    public static void main(String[] args) {
        new OpenGL3();
    }

    private OpenGL3() {

        // Setup window
        GLFW.glfwSetErrorCallback((error, description) -> System.out.println("Glfw Error " + error + ": " + description));
        glfw.init();
        windowHint.setDebug(DEBUG);

        // Decide GL+GLSL versions
        if (Platform.get() == Platform.MACOSX) { // GL 3.2 + GLSL 150

//            setGlslVersionString(150);
            windowHint.getContext().setVersion("3.2");
            windowHint.setProfile(core);     // 3.2+ only
            windowHint.setForwardComp(true); // Required on Mac

        } else {   // GL 3.0 + GLSL 130

//            setGlslVersionString(130);
            windowHint.getContext().setVersion("3.0");
            //profile = core      // 3.2+ only
            //forwardComp = true  // 3.0+ only
        }

        // Create window with graphics context
        window = new GlfwWindow(1280, 720, "Dear ImGui GLFW+OpenGL3 OpenGL example", null, new Vec2i(30), true);
        window.makeContextCurrent();
        glfw.setSwapInterval(VSync.ON);   // Enable vsync

        // Initialize OpenGL loader
        GL.createCapabilities();

        // Setup Dear ImGui context
        ctx = new Context();
        io.setConfigFlags(io.getConfigFlags() | ConfigFlag.NavEnableKeyboard.i);    // Enable Keyboard Controls
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableGamepad            // Enable Gamepad Controls
        io.setConfigFlags(io.getConfigFlags() | ConfigFlag.DockingEnable.i);        // Enable Docking
        io.setConfigFlags(io.getConfigFlags() | ConfigFlag.ViewportsEnable.i);      // Enable Multi-Viewport / Platform Windows
        //io.ConfigViewportsNoAutoMerge = true;
        //io.ConfigViewportsNoTaskBarIcon = true;

        // Setup Dear ImGui style
        imgui.styleColorsDark(null);
//        imgui.styleColorsClassic(null)

        // Setup Platform/Renderer backends
        implGlfw = new ImplGlfw(window, true, null);
        implGl3 = new ImplGL3();

        io = imgui.getIo();

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
        window.loop((MemoryStack stack) -> mainLoop());

        implGlfw.shutdown();
        implGl3.shutdown();
        ctx.destroy();

        window.destroy();
        glfw.terminate();
    }

    private void mainLoop() {

        // Start the Dear ImGui frame
        implGl3.newFrame();
        implGlfw.newFrame();

        imgui.newFrame();

        imgui.text("Hello, world!");                                // Display some text (you can use a format string too)
        // TODO
//        imgui.sliderFloat("float", f, 0, 0f, 1f, "%.3f", 1f);       // Edit 1 float using a slider from 0.0f to 1.0f
        imgui.colorEdit3("clear color", clearColor, 0);               // Edit 3 floats representing a color

        imgui.checkbox("Demo Window", showDemo);                 // Edit bools storing our windows open/close state
        imgui.checkbox("Another Window", showAnotherWindow);

        if (imgui.button("Button", new Vec2())) // Buttons return true when clicked (NB: most widgets return true when edited/activated)
            counter[0]++;

        imgui.sameLine(0f, -1f);
        imgui.text("counter = " + counter[0]);

        imgui.text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.getFramerate(), io.getFramerate());

        // 2. Show another simple window. In most cases you will use an explicit begin/end pair to name the window.
        if (showAnotherWindow.get()) {
            imgui.begin("Another Window", showAnotherWindow, 0);
            imgui.text("Hello from another window!");
            if (imgui.button("Close Me", new Vec2()))
                showAnotherWindow.set(false);
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
        imgui.render();
        glViewport(window.getFramebufferSize());
        glClearColor(clearColor);
        glClear(GL_COLOR_BUFFER_BIT);
        implGl3.renderDrawData(imgui.getDrawData());
    }
}
