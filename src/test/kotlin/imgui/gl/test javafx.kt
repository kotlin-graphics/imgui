package imgui.gl

import glm_.d
import glm_.f
import glm_.i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.io
import imgui.impl.ImplJFX
import imgui.impl.JFXColor
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.stage.Stage
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    HelloWorld_jfx()
}

class HelloWorld_jfx {
    lateinit var stage: Stage
    lateinit var scene: Scene
    lateinit var canvas: Canvas
    private var ctx: Context

    var vsync = true

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    val startTime = System.nanoTime()
    var time = 0.0

    init {
        val ready = AtomicBoolean(false)
        Platform.startup {
            canvas = Canvas(1280.0, 720.0)
            val vb = Pane(canvas)
            scene = Scene(vb)
            stage = Stage()
            stage.scene = scene
            stage.title = "OpenJFX Example"
            stage.show()
            ready.set(true)
        }
        Platform.requestNextPulse()
        ctx = Context()
        ImGui.styleColorsDark()

        setupMappings()

        while(!ready.get())
            Thread.sleep(1)

        val internalCanvas = Canvas(canvas.width, canvas.height)

        val s = ImplJFX(stage, internalCanvas)

        while(stage.isShowing) {

            vsyncCap()

            s.newFrame()
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


                    text("NavActive: ${io.navActive}, NavVisible: ${io.navVisible}")

                    //functionalProgramming.treeNode("Keyboard, Mouse & Navigation State") {
                    if (isMousePosValid()) text("Mouse pos: (%g, %g)", io.mousePos.x, io.mousePos.y)
                    else text("Mouse pos: <INVALID>")
                    text("Mouse delta: (%g, %g)", io.mouseDelta.x, io.mouseDelta.y)
                    text("Mouse down:")
                    for (i in 0 until io.mouseDown.size)
                        if (io.mouseDownDuration[i] >= 0f) {
                            sameLine()
                            text("b$i (%.02f secs)", io.mouseDownDuration[i])
                        }
                    text("Mouse clicked:")
                    for (i in 0 until io.mouseDown.size)
                        if (isMouseClicked(i)) {
                            sameLine()
                            text("b$i")
                        }
                    text("Mouse dbl-clicked:")
                    for (i in 0 until io.mouseDown.size)
                        if (isMouseDoubleClicked(i)) {
                            sameLine()
                            text("b$i")
                        }
                    text("Mouse released:")
                    for (i in 0 until io.mouseDown.size)
                        if (isMouseReleased(i)) {
                            sameLine()
                            text("b$i")
                        }
                    text("Mouse wheel: %.1f", io.mouseWheel)

                    text("Keys down:")
                    for (i in io.keysDown.indices)
                        if (io.keysDownDuration[i] >= 0f) {
                            sameLine()
                            text("$i (%.02f secs)", io.keysDownDuration[i])
                        }
                    text("Keys pressed:")
                    for (i in io.keysDown.indices)
                        if (isKeyPressed(i)) {
                            sameLine()
                            text("$i")
                        }
                    text("Keys release:")
                    for (i in io.keysDown.indices)
                        if (isKeyReleased(i)) {
                            sameLine()
                            text("$i")
                        }
                    val ctrl = if (io.keyCtrl) "CTRL " else ""
                    val shift = if (io.keyShift) "SHIFT " else ""
                    val alt = if (io.keyAlt) "ALT " else ""
                    val super_ = if (io.keySuper) "SUPER " else ""
                    text("Keys mods: $ctrl$shift$alt$super_")

                    text("NavInputs down:")
                    io.navInputs.filter { it > 0f }.forEachIndexed { i, it -> sameLine(); text("[$i] %.2f", it) }
                    text("NavInputs pressed:")
                    io.navInputsDownDuration.filter { it == 0f }.forEachIndexed { i, _ -> sameLine(); text("[$i]") }
                    text("NavInputs duration:")
                    io.navInputsDownDuration.filter { it >= 0f }.forEachIndexed { i, it -> sameLine(); text("[$i] %.2f", it) }

                    button("Hovering me sets the\nkeyboard capture flag")
                    if (isItemHovered()) captureKeyboardFromApp(true)
                    sameLine()
                    button("Holding me clears the\nthe keyboard capture flag")
                    if (isItemActive) captureKeyboardFromApp(false)
                    //}

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

            //copy to screen
            fun doCopy() {
                val wi = WritableImage(canvas.width.i, canvas.height.i)
                val r = SnapshotParameters()
                r.fill = clearColor.toJFXColor()
                internalCanvas.snapshot(r, wi)
                canvas.graphicsContext2D.drawImage(wi, 0.0, 0.0)
            }
            if (!Platform.isFxApplicationThread()) {
                val ss = AtomicBoolean(false)
                Platform.runLater {
                    doCopy()
                    ss.set(true)
                }
                while (!ss.get()) {
                }
            } else {
                doCopy()
            }
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

    private fun vsyncCap() {
        time = (System.nanoTime() - startTime).toDouble() / 1e9
        if (!vsync)
            return
        var currentTime = time
        var deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        while (deltaTime < 1.0 / 60.0) {
            currentTime = (System.nanoTime() - startTime).toDouble() / 1e9
            deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        }
        time = currentTime
    }
}

private fun Vec4.toJFXColor(): JFXColor {
    return JFXColor(r.d, g.d, b.d, a.d)
}