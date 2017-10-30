package imgui

import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.util.Animator
import glm_.vec2.Vec2
import imgui.functionalProgramming.button
import imgui.impl.JoglGL3
import uno.gln.jogl.glViewport


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

        drawable.gl.swapInterval = 1    // Enable vsync

        // Load Fonts
        // (there is a default font, this is only if you want to change it. see extra_fonts/README.txt for more details)
        //ImGuiIO& io = ImGui::GetIO();
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../extra_fonts/ProggyTiny.ttf", 10.0f);
        //io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
    }

    val clearColor = floatArrayOf(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = booleanArrayOf(false)
    var showTestWindow = booleanArrayOf(false)

    override fun display(drawable: GLAutoDrawable) = with(drawable.gl.gL3) {

        JoglGL3.newFrame(this)

        with(ImGui) {

            /*  1. Show a simple window
                Tip: if we don't call ImGui.begin()/ImGui.end() the widgets appears in a window automatically
                called "Debug"             */
            run {
                text("Hello, world!")
                sliderFloat("float", f, 0f, 1f)
                colorEdit3("clear color", clearColor)
                //  You can write functions in the classical way, with if(cond) { code }
                if (button("Test Window")) {
                    showTestWindow[0] = !showTestWindow[0]
                }
                // or you can take advantage of functional programming and pass directly a lambda as last parameter
                button("Another Window") {
                    showAnotherWindow[0] = !showAnotherWindow[0]
                }
                text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / IO.framerate, IO.framerate)
            }

            /*  2. Show another simple window, this time using an explicit begin/end pair   */
            run {
                if (showAnotherWindow[0]) {
                    begin("Another Window", showAnotherWindow)
                    text("Hello from anoter window!")
                    end()
                }
            }

            /* 3. Show the ImGui test window. Most of the sample code is in ImGui.showTestWindow() */
            run {
                if (showTestWindow[0]) {
                    setNextWindowPos(Vec2(650, 20), Cond.FirstUseEver)
                    showTestWindow(showTestWindow)
                }
            }
        }

        // Rendering
        glViewport(window.x, window.y)
        glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])  // TODO gln
        glClear(GL_COLOR_BUFFER_BIT)
        ImGui.render()
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {}

    override fun dispose(drawable: GLAutoDrawable) {
        JoglGL3.shutdown(drawable.gl.gL3)
    }

    companion object {
        val f = FloatArray(1)
    }
}