package imgui.impl

import glm_.d
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.ImGui.io
import imgui.*
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.BlendMode
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.robot.Robot
import javafx.scene.shape.FillRule
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.stage.Stage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

typealias JFXColor = javafx.scene.paint.Color

const val COLOR_SIZE_MASK = 0xFF

class ImplJFX(val stage: Stage, val canvas: Canvas) {
    private lateinit var texture: Image
    private val startTime = System.currentTimeMillis()
    private var time = 0.0

    private lateinit var mousePressListener: EventHandler<MouseEvent>
    private lateinit var mouseMoveListener: EventHandler<MouseEvent>

    private var mousePos = Vec2()
    private val mouseJustReleased = BooleanArray(io.mouseDown.size) { false }

    private lateinit var r: Robot

    private var warnSlowTex = true

    fun createDeviceObjects() {
        if (ImGui.io.fonts.isBuilt)
            return

        if(!Platform.isFxApplicationThread()) {
            Platform.runLater {
                r = Robot()
            }
            Platform.requestNextPulse()
        } else {
            r = Robot()
        }

        mousePressListener = EventHandler {
            (if (it.eventType == MouseEvent.MOUSE_PRESSED) mouseJustPressed else mouseJustReleased)[when (it.button) {
                MouseButton.PRIMARY -> 0
                MouseButton.MIDDLE -> 2
                MouseButton.SECONDARY -> 1
                else -> return@EventHandler
            }] = true
        }

        mouseMoveListener = EventHandler {
            mousePos = Vec2(it.sceneX.f, it.sceneY.f)
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
        stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mousePressListener)
        stage.addEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveListener)
        stage.addEventHandler(KeyEvent.KEY_PRESSED, keyListener)

        io.backendRendererName = "imgui impl jfx"
        io.backendPlatformName = null
        io.backendLanguageUserData = null
        io.backendRendererUserData = null
        io.backendPlatformUserData = null
        io.setClipboardTextFn = { _, text ->
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
        io.getClipboardTextFn = { _ ->
            Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        }
        io.clipboardUserData = NUL

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

    fun newFrame() {
        if (!::texture.isInitialized)
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
        if (io.wantSetMousePos)
            TODO("Need to find window position") //r.mouseMove(Point2D(io.mousePos.x.d, io.mousePos.y.d))
        else
            io.mousePos put (mousePos)
    }

    var xs = DoubleArray(16)
    var ys = DoubleArray(16)

    fun renderDrawData(drawData: DrawData) {
        val gc = canvas.graphicsContext2D
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
                    var col = JFXColor(0.0, 0.0, 0.0, 0.0)
                    var pos = 0
                    fun addPoint(x: Float, y: Float) {
                        if (pos == xs.size) {
                            if (DEBUG)
                                println("increase points buffer size (old ${xs.size}, new ${xs.size * 2})")
                            val nx = DoubleArray(xs.size * 2)
                            val ny = DoubleArray(ys.size * 2)
                            xs.copyInto(nx)
                            ys.copyInto(ny)
                            xs = nx
                            ys = ny
                        }
                        xs[pos] = x.d
                        ys[pos++] = y.d
                    }

                    var skip = false
                    for (tri in 0 until cmd.elemCount step 3) {
                        if (skip) {
                            skip = false
                            continue
                        }
                        val baseIdx = tri + idxBufferOffset
                        val idx1 = cmdList.idxBuffer[baseIdx]
                        val vtx1 = cmdList.vtxBuffer[idx1]
                        val vtx2 = cmdList.vtxBuffer[cmdList.idxBuffer[baseIdx + 1]]
                        val idx3 = cmdList.idxBuffer[baseIdx + 2]
                        val vtx3 = cmdList.vtxBuffer[idx3]

                        val col1 = vtx1.col

                        fun draw() {
                            if (pos != 0) {
                                gc.fill = col
                                gc.fillPolygon(xs, ys, pos)
                                pos = 0
                            } else {
                                val color = texPr.getColor((vtx1.uv.x * texture.width).toInt(), (vtx1.uv.y * texture.height).toInt())
                                val x = JFXColor.rgb(
                                        (((col1 ushr COL32_R_SHIFT) and COLOR_SIZE_MASK) * color.red).i,
                                        (((col1 ushr COL32_G_SHIFT) and COLOR_SIZE_MASK) * color.green).i,
                                        (((col1 ushr COL32_B_SHIFT) and COLOR_SIZE_MASK) * color.blue).i,
                                        (((col1 ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble()) * color.opacity)
                                gc.fill = x
                                gc.fillPolygon(doubleArrayOf(vtx1.pos.x.toDouble(), vtx2.pos.x.toDouble(), vtx3.pos.x.toDouble()),
                                        doubleArrayOf(vtx1.pos.y.toDouble(), vtx2.pos.y.toDouble(), vtx3.pos.y.toDouble()), 3)
                            }
                        }

                        if (vtx1.uv == vtx2.uv) {
                            //in OpenGL this is done in shaders as `color * texture(texCoord)
                            //the way this is implemented here has 2 current limitations: no new images
                            //and the colors are not currently multiplied
                            //this could be fixed

                            //check if it borders the next triangle
                            if (tri + 3 < cmd.elemCount) {
                                val idx4 = cmdList.idxBuffer[baseIdx + 3]
                                val idx5 = cmdList.idxBuffer[baseIdx + 4]
                                if (idx4 == idx1 && idx5 == idx3) {
                                    val vtx6 = cmdList.vtxBuffer[cmdList.idxBuffer[baseIdx + 5]]
                                    if (pos == 0) {
                                        col = JFXColor.rgb(
                                                (col1 ushr COL32_R_SHIFT) and COLOR_SIZE_MASK,
                                                (col1 ushr COL32_G_SHIFT) and COLOR_SIZE_MASK,
                                                (col1 ushr COL32_B_SHIFT) and COLOR_SIZE_MASK,
                                                (((col1 ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble()) * texPr.getColor((vtx1.uv.x * texture.width).toInt(), (vtx1.uv.y * texture.height).toInt()).opacity)
                                        addPoint(vtx1.pos.x, vtx1.pos.y)
                                        addPoint(vtx2.pos.x, vtx2.pos.y)
                                        addPoint(vtx3.pos.x, vtx3.pos.y)
                                    }
                                    addPoint(vtx6.pos.x, vtx6.pos.y)
                                } else {
                                    draw()
                                }
                            } else {
                                draw()
                            }
                        } else {
                            fun drawSlow() {
                                if(DEBUG && !warnSlowTex) {
                                    warnSlowTex = false
                                    println("OpenJFX slow texture rendering has been invoked!")
                                }
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
                            if (tri + 3 < cmd.elemCount) {
                                val idx4 = cmdList.idxBuffer[baseIdx + 3]
                                val idx5 = cmdList.idxBuffer[baseIdx + 4]
                                if (idx4 == idx1 && idx5 == idx3) {
                                    //TODO: this only works when the x and y components of vtx3 are greater than those of vtx1
                                    gc.drawImage(texture, (texture.width * vtx1.uv.x).d, (texture.height * vtx1.uv.y).d,
                                            (texture.width * (vtx3.uv.x - vtx1.uv.x)).d, (texture.height * (vtx3.uv.y - vtx1.uv.y)).d,
                                            vtx1.pos.x.d, vtx1.pos.y.d, vtx3.pos.x.d - vtx1.pos.x.d, vtx3.pos.y.d - vtx1.pos.y.d)
                                    skip = true
                                } else {
                                    drawSlow()
                                }
                            } else {
                                drawSlow()
                            }
                        }
                    }
                }
                idxBufferOffset += cmd.elemCount
            }
        }
        gc.restore()
    }

    private fun clearScreen(gc: GraphicsContext) {
        gc.clearRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
    }
}

