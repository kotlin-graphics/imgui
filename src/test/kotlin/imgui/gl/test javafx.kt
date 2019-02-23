package imgui.gl

import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.io
import imgui.impl.ImplJFX
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.stage.Stage

typealias JFXColor = javafx.scene.paint.Color

fun main() {
    HelloWorld_jfx()
}

class HelloWorld_jfx {
    lateinit var stage: Stage
    lateinit var scene: Scene
    lateinit var canvas: Canvas
    private var ctx: Context

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    init {

        val t = Thread.currentThread()
        Platform.startup {
            canvas = Canvas(1280.0, 720.0)
            val vb = Pane(canvas)
            scene = Scene(vb)
            stage = Stage()
            stage.scene = scene
            stage.show()
        }
        ctx = Context()
        ImGui.styleColorsDark()

        setupMappings()

        while(!::stage.isInitialized)
            Thread.sleep(1)

        val s = ImplJFX(stage, canvas, true)

        while(stage.isShowing) {

            s.newFrame(clearColor)
            ImGui.newFrame()

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
                        begin_("Another Window", ::showAnotherWindow)
                        text("Hello from another window!")
                        if (button("Close Me"))
                            showAnotherWindow = false
                        end()
                    }
                }
            }

            // Rendering
            ImGui.render()

            s.renderDrawData(ImGui.drawData!!)
        }
    }

    private fun setupMappings() {
        with(io) {
            keyMap[Key.Tab] = KeyCode.TAB.code
            keyMap[Key.LeftArrow] = KeyCode.LEFT.code
            keyMap[Key.RightArrow] = KeyCode.RIGHT.code
            keyMap[Key.UpArrow] = KeyCode.UP.code
            keyMap[Key.DownArrow] = KeyCode.DOWN.code
            keyMap[Key.PageUp] = KeyCode.PAGE_UP.code
            keyMap[Key.PageDown] = KeyCode.PAGE_DOWN.code
            keyMap[Key.Home] = KeyCode.HOME.code
            keyMap[Key.End] = KeyCode.END.code
            keyMap[Key.Insert] = KeyCode.INSERT.code
            keyMap[Key.Delete] = KeyCode.DELETE.code
            keyMap[Key.Backspace] = KeyCode.BACK_SPACE.code
            keyMap[Key.Space] = KeyCode.SPACE.code
            keyMap[Key.Enter] = KeyCode.ENTER.code
            keyMap[Key.Escape] = KeyCode.ESCAPE.code
            keyMap[Key.A] = KeyCode.A.code
            keyMap[Key.C] = KeyCode.C.code
            keyMap[Key.V] = KeyCode.V.code
            keyMap[Key.X] = KeyCode.X.code
            keyMap[Key.Y] = KeyCode.Y.code
            keyMap[Key.Z] = KeyCode.Z.code
        }
    }
}

private fun Vec4.toRGBA(): Int {
    return ((this.r * 255.0).toInt() shl 24) or ((this.g * 255.0).toInt() shl 16) or
            ((this.b * 255.0).toInt() shl 8) or (this.a * 255.0).toInt()
}