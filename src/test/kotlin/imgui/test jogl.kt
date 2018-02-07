package imgui

import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT
import com.jogamp.opengl.GL.GL_NO_ERROR
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.util.Animator
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.functionalProgramming.button
import imgui.impl.JoglGL3


fun main(args: Array<String>) {
    HelloWorld_jogl().setup()
}

class HelloWorld_jogl : GLEventListener {

    val window: GLWindow = run {

        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        GLWindow.create(glCapabilities).apply {
            title = "ImGui Jogl OpenGL3 example"
            setSize(1280, 720)
        }
    }

    val animator = Animator()

    fun setup() {

        window.addGLEventListener(this)
        window.isVisible = true

        animator.add(window)
        animator.start()


        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent) {
                animator.stop(); System.exit(0); }
        })
    }


    override fun init(drawable: GLAutoDrawable) {

        // Setup ImGui binding
        JoglGL3.init(window, true)

        // Setup style
        ImGui.styleColorsDark()
//        ImGui.styleColorsClassic()

        drawable.gl.swapInterval = 1    // Enable vsync

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
        //ImFont* font = io.Fonts->AddFontFromFileTTF("misc/fonts/ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
        //IM_ASSERT(font != NULL);
    }

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = false
    var showDemo = true

    override fun display(drawable: GLAutoDrawable) = with(drawable.gl.gL3) {

        JoglGL3.newFrame(this)

        with(ImGui) {

            /*  1. Show a simple window
                Tip: if we don't call ImGui.begin()/ImGui.end() the widgets appears in a window automatically
                called "Debug"             */
            text("Hello, world!")                             // Some text (you can use a format string too)
            sliderFloat("float", ::f, 0f, 1f)   // Edit 1 float as a slider from 0f to 1f
            colorEdit3("clear color", clearColor)           // Edit 3 floats as a color
            //  You can write functions in the classical way, with if(cond) { code }
            if (button("Demo Window"))                      // Use buttons to toggle our bools. We could use checkbox() as well.
                showDemo = !showDemo

            // or you can take advantage of functional programming and pass directly a lambda as last parameter
            button("Another Window") {
                showAnotherWindow = !showAnotherWindow
            }
            text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / IO.framerate, IO.framerate)

            /*  2. Show another simple window, this time using an explicit begin/end pair   */
            if (showAnotherWindow) {
                _begin("Another Window", ::showAnotherWindow)
                text("Hello from another window!")
                end()
            }

            /* 3. Show the ImGui demo window. Most of the sample code is in ImGui.showDemoWindow() */
            if (showDemo) {
                /*  Normally user code doesn't need/want to call this because positions are saved in .ini file anyway.
                    Here we just want to make the demo initial state a bit more friendly!                 */
                setNextWindowPos(Vec2(650, 20), Cond.FirstUseEver)
                showDemoWindow(::showDemo)
            }
        }

        // Rendering
        glViewport(0, 0, window.x, window.y)
        glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])  // TODO gln
        glClear(GL_COLOR_BUFFER_BIT)

        ImGui.render()
        if (glGetError() != GL_NO_ERROR) throw Error("display")
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {}

    override fun dispose(drawable: GLAutoDrawable) {
        JoglGL3.shutdown(drawable.gl.gL3)
    }
}