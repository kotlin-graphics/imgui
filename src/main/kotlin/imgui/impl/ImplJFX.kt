package imgui.impl

import glm_.d
import glm_.f
import glm_.i
import glm_.vec2.Vec2d
import glm_.vec4.Vec4
import imgui.ImGui.io
import imgui.*
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.BlendMode
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.shape.FillRule
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.stage.Stage
import java.util.concurrent.atomic.AtomicBoolean

typealias JFXColor = javafx.scene.paint.Color

const val COLOR_SIZE_MASK = 0xFF
const val OPACITY_MULTIPLIER = 2.0
const val OPACITY_RECIPROCAL = 1.0 / OPACITY_MULTIPLIER

class ImplJFX(val stage: Stage, val canvas: Canvas, val vsync: Boolean) {
    val internalCanvas = if(vsync) Canvas(canvas.width, canvas.height) else null

    lateinit var texture: Image
    val startTime = System.currentTimeMillis()
    var time = 0.0

    lateinit var mousePressListener: EventHandler<MouseEvent>
    lateinit var mouseMoveListener: EventHandler<MouseEvent>
    lateinit var mouseReleaseListener: EventHandler<MouseEvent>

    var mousePos = Vec2d()
    val mouseJustReleased = BooleanArray(io.mouseDown.size) { false }

    fun createDeviceObjects() {
        if (ImGui.io.fonts.isBuilt)
            return

        mousePressListener = EventHandler {
            val button = when (it.button) {
                MouseButton.PRIMARY -> 0
                MouseButton.MIDDLE -> 2
                MouseButton.SECONDARY -> 1
                else -> -1
            }
            if (button in 0..2)
                mouseJustPressed[button] = true
        }

        mouseReleaseListener = EventHandler {
            val button = when (it.button) {
                MouseButton.PRIMARY -> 0
                MouseButton.MIDDLE -> 2
                MouseButton.SECONDARY -> 1
                else -> -1
            }
            if (button in 0..2)
                mouseJustReleased[button] = true
        }

        mouseMoveListener = EventHandler {
            mousePos = Vec2d(it.sceneX, it.sceneY)
        }

        val keyListener = EventHandler<KeyEvent> {
            val key = it.code.code
            with(io) {
                if (key in keysDown.indices)
                    if (it.eventType == KeyEvent.KEY_PRESSED)
                        keysDown[key] = true
                    else if (it.eventType == KeyEvent.KEY_RELEASED)
                        keysDown[key] = false

                // Modifiers are not reliable across systems
                keyCtrl = it.isControlDown
                keyShift = it.isShiftDown
                keyAlt = it.isAltDown
                keySuper = it.isMetaDown
            }
        }

        stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mousePressListener)
        stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleaseListener)
        stage.addEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveListener)
        stage.addEventHandler(KeyEvent.KEY_PRESSED, keyListener)

        val (pixels, size) = io.fonts.getTexDataAsAlpha8()

        val createTex = WritableImage(size.x, size.y)

        with(createTex.pixelWriter) {
            for (y in 0 until size.y) {
                for (x in 0 until size.x) {
                    setArgb(x, y, (pixels[(y * size.x) + x].toInt() shl 24) + 0xFFFFFF)
                }
            }
        }

        texture = createTex
    }

    private var clearColor: Vec4? = null

    fun newFrame(clearColor: Vec4?) {
        this.clearColor = clearColor

        if(!::texture.isInitialized)
            createDeviceObjects()

        io.displaySize.put(canvas.width, canvas.height)
        io.displayFramebufferScale.x = 1f
        io.displayFramebufferScale.y = 1f

        val currentTime = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
        ImGui.io.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        time = currentTime

        updateMousePos()

        io.navInputs.fill(0f)

        io.backendFlags = io.backendFlags or BackendFlag.HasSetMousePos
    }

    private fun updateMousePos() {
        if (io.configFlags has ConfigFlag.NoMouseUpdate)
            return

        repeat(io.mouseDown.size) {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            io.mouseDown[it] = mouseJustPressed[it] || (io.mouseDown[it] and !mouseJustReleased[it])
            mouseJustPressed[it] = false
            mouseJustReleased[it] = false
        }


        // Update mouse position
        io.mousePos put (mousePos)
    }

    fun renderDrawData(drawData: DrawData) {
        vsyncCap(vsync)

        val gc = if (vsync) internalCanvas!!.graphicsContext2D else canvas.graphicsContext2D
        gc.save()
        gc.globalBlendMode = BlendMode.SRC_OVER
        clearScreen(gc)
        gc.fillRule = FillRule.NON_ZERO
        gc.lineCap = StrokeLineCap.BUTT
        gc.lineJoin = StrokeLineJoin.MITER
        gc.lineWidth = 0.0
        val texPr = texture.pixelReader
        val pw = gc.pixelWriter
        for (cmdList in drawData.cmdLists) {
            var idxBufferOffset = 0
            for (cmd in cmdList.cmdBuffer) {
                val cb = cmd.userCallback
                if (cb != null)
                // User callback (registered via ImDrawList::AddCallback)
                    cb(cmdList, cmd)
                else {
                    for (tri in 0 until cmd.elemCount / 3) {
                        val baseIdx = (tri * 3) + idxBufferOffset
                        val idx1 = cmdList.idxBuffer[baseIdx]
                        val vtx1 = cmdList.vtxBuffer[idx1]
                        val vtx2 = cmdList.vtxBuffer[cmdList.idxBuffer[baseIdx + 1]]
                        val idx3 = cmdList.idxBuffer[baseIdx + 2]
                        val vtx3 = cmdList.vtxBuffer[idx3]

                        val col1 = vtx1.col

                        if (vtx1.uv == vtx2.uv) {
                            //in OpenGL this is done in shaders as `color * texture(texCoord)
                            //the way this is implemented here has 2 current limitations: no new images
                            //and the colors are not currently multiplied
                            //this could be fixed
                            val x = JFXColor.rgb(
                                    (col1 ushr COL32_R_SHIFT) and COLOR_SIZE_MASK,
                                    (col1 ushr COL32_G_SHIFT) and COLOR_SIZE_MASK,
                                    (col1 ushr COL32_B_SHIFT) and COLOR_SIZE_MASK,
                                    (((col1 ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble()) * texPr.getColor((vtx1.uv.x * texture.width).toInt(), (vtx1.uv.y * texture.height).toInt()).opacity)
                            gc.fill = x
                            gc.fillPolygon(doubleArrayOf(vtx1.pos.x.toDouble(), vtx2.pos.x.toDouble(), vtx3.pos.x.toDouble()),
                                    doubleArrayOf(vtx1.pos.y.toDouble(), vtx2.pos.y.toDouble(), vtx3.pos.y.toDouble()), 3)

                            //check if it borders the next triangle
                            if (tri + 1 < cmd.elemCount / 3) {
                                val idx4 = cmdList.idxBuffer[baseIdx + 3]
                                if (idx4 == idx1) {
                                    val idx5 = cmdList.idxBuffer[baseIdx + 4]
                                    if (idx5 == idx3) {
                                        //if it borders, we need to draw a line between the two
                                        //this is done this way rather than drawing both outlines of each triangle because that will draw this line twice. this produces higher visual quality
                                        gc.stroke = JFXColor(x.red, x.green, x.blue, x.opacity / (OPACITY_MULTIPLIER * (x.red + x.green + x.blue + OPACITY_RECIPROCAL)))
                                        //if you are using this backend and see many unsightly lines, play around with OPACITY_MULTIPLIER. this configuration was chosen as it appears to work best.
                                        gc.strokeLine(vtx1.pos.x.d, vtx1.pos.y.d, vtx3.pos.x.d, vtx3.pos.y.d)
                                    }
                                }
                            }

                        } else {
                            if (vtx1.uv.y == vtx2.uv.y) {
                                //Top flat triangle
                                if (vtx3.uv.y > vtx1.uv.y) {
                                    val minY = Math.round(vtx1.pos.y).i
                                    val maxY = Math.round(vtx3.pos.y).i
                                    val minYUV = vtx1.uv.y
                                    val maxYUV = vtx3.uv.y
                                    val yuvDiff = maxYUV - minYUV
                                    val yDiff = maxY - minY
                                    val minX = vtx1.pos.x.i
                                    val maxX = vtx3.pos.x.i
                                    val minXUV = vtx1.uv.x
                                    val maxXUV = vtx3.uv.x
                                    val xDiff = maxX - minX
                                    val xuvDiff = maxXUV - minXUV
                                    for (y in minY until maxY + 1) {
                                        for (x in xDiff downTo (xDiff * (y.f - minY) / yDiff).i) {
                                            val xPct = x.d / xDiff
                                            val yPct = 1 - ((maxY.d - y.d) / yDiff)
                                            val c = texPr.getArgb((texture.width * (minXUV + (xPct * xuvDiff))).i, (texture.height * (minYUV + (yPct * yuvDiff))).i)
                                            if ((c and COL32_A_MASK) == 0)
                                                continue
                                            pw.setArgb(minX + x, y, c)
                                        }
                                    }
                                } else {
                                    TODO("vtx1y == vtx2y vtx3 low")
                                }
                            } else if (vtx2.uv.y == vtx3.uv.y) {
                                if (vtx3.uv.y > vtx1.uv.y) {
                                    val minY = Math.round(vtx1.pos.y).i
                                    val maxY = Math.round(vtx3.pos.y).i
                                    val minYUV = vtx1.uv.y
                                    val maxYUV = vtx3.uv.y
                                    val yuvDiff = maxYUV - minYUV
                                    val yDiff = maxY - minY
                                    val minX = vtx1.pos.x.i
                                    val maxX = vtx2.pos.x.i
                                    val minXUV = vtx1.uv.x
                                    val maxXUV = vtx2.uv.x
                                    val xDiff = maxX - minX
                                    val xuvDiff = maxXUV - minXUV
                                    for (y in minY until maxY + 1) {
                                        for (x in 0 until (xDiff * (y.f - minY) / yDiff).i) {
                                            val xPct = x.d / xDiff
                                            val yPct = 1.0 - ((maxY.d - y.d) / yDiff)
                                            val c = texPr.getArgb((texture.width * (minXUV + (xPct * xuvDiff))).i, (texture.height * (minYUV + (yPct * yuvDiff))).i)
                                            if ((c and COL32_A_MASK) == 0)
                                                continue
                                            pw.setArgb(minX + x, y, c)
                                        }
                                    }
                                } else {
                                    TODO("vtx2y == vtxy3 vtx3 low")
                                }
                            } else if (vtx1.uv.y == vtx3.uv.y) {
                                TODO("vtx1y == vtxy3 ")
                            } else {
                                TODO("none")
                            }
                        }
                    }
                }
                idxBufferOffset += cmd.elemCount
            }
        }
        if (vsync) {
            fun doCopy() {
                clearScreen(canvas.graphicsContext2D)
                val wi = WritableImage(canvas.width.i, canvas.height.i)
                internalCanvas!!.snapshot(SnapshotParameters(), wi)
                canvas.graphicsContext2D.drawImage(wi, 0.0, 0.0)
            }
            if (!Platform.isFxApplicationThread()) {
                val s = AtomicBoolean(false)
                Platform.runLater {
                    doCopy()
                    s.set(true)
                }
                while (!s.get()) {
                }
            } else {
                doCopy()
            }
        }
        gc.restore()
    }

    private fun clearScreen(gc: GraphicsContext) {
        if(clearColor == null)
            return
        gc.clearRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
        gc.fill = JFXColor(clearColor!!.r.d, clearColor!!.g.d, clearColor!!.b.d, clearColor!!.a.d)
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
    }

    private fun vsyncCap(b: Boolean) {
        if (!b)
            return
        var currentTime = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
        var deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        while (deltaTime < 1.0 / 60.0) {
            Thread.sleep(1)
            currentTime = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
            deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        }
    }
}

