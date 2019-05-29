//package imgui.impl
//
//import glm_.*
//import glm_.vec2.Vec2
//import glm_.vec4.Vec4
//import imgui.ImGui.io
//import imgui.*
//import javafx.application.Platform
//import javafx.event.EventHandler
//import javafx.geometry.Point2D
//import javafx.scene.Cursor
//import javafx.scene.canvas.Canvas
//import javafx.scene.effect.BlendMode
//import javafx.scene.image.Image
//import javafx.scene.image.WritableImage
//import javafx.scene.input.*
//import javafx.scene.robot.Robot
//import javafx.scene.shape.FillRule
//import javafx.scene.shape.StrokeLineCap
//import javafx.scene.shape.StrokeLineJoin
//import javafx.stage.Stage
//import org.lwjgl.system.MemoryUtil.NULL
//
//typealias JFXColor = javafx.scene.paint.Color
//
//const val COLOR_SIZE_MASK = 0xFF
////-1 is no multiplication (each pixel * 1.0 for each component, so the original)
//const val TEXTURE_COLOR_UNMULTIPLIED = -1
//
///**
// * Make this too small, and some barycentric items will be drawn that are inefficient to draw.
// * Make it to big, and no barycentric items will be drawn. (e.g. color pickers)
// */
//var BARYCENTRIC_SIZE_THRESHOLD = 500.0
//
//class ImplJFX(val stage: Stage, var canvas: Canvas) {
//    private val startTime = System.currentTimeMillis()
//    private var time = 0.0
//
//    private lateinit var mousePressListener: EventHandler<MouseEvent>
//    private lateinit var mouseMoveListener: EventHandler<MouseEvent>
//    private lateinit var scrollListener: EventHandler<ScrollEvent>
//    private lateinit var keyListener: EventHandler<KeyEvent>
//    private lateinit var charListener: EventHandler<KeyEvent>
//
//    private var mousePos = Vec2()
//    private val mouseJustPressed = BooleanArray(5)
//    private val mouseJustReleased = BooleanArray(io.mouseDown.size) { false }
//
//    private lateinit var r: Robot
//
//    private var warnSlowTex = true
//
//    private var isInit = false
//
//    fun createDeviceObjects() {
//        if (ImGui.io.fonts.isBuilt)
//            return
//
//        setupMappings()
//
//        if (!Platform.isFxApplicationThread()) {
//            Platform.runLater {
//                r = Robot()
//            }
//            Platform.requestNextPulse()
//        } else {
//            r = Robot()
//        }
//
//        io.backendFlags = io.backendFlags or BackendFlag.HasSetMousePos
//        io.backendFlags = io.backendFlags or BackendFlag.HasMouseCursors
//
//        mousePressListener = EventHandler {
//            (if (it.eventType == MouseEvent.MOUSE_PRESSED) mouseJustPressed else mouseJustReleased)[when (it.button) {
//                MouseButton.PRIMARY -> 0
//                MouseButton.MIDDLE -> 2
//                MouseButton.SECONDARY -> 1
//                else -> return@EventHandler
//            }] = true
//        }
//
//        mouseMoveListener = EventHandler {
//            mousePos = Vec2(it.sceneX.f, it.sceneY.f)
//        }
//
//        keyListener = EventHandler {
//            val key = it.code.code
//            with(io) {
//                if (key in keysDown.indices)
//                    if (it.eventType == KeyEvent.KEY_PRESSED)
//                        keysDown[key] = true
//                    else if (it.eventType == KeyEvent.KEY_RELEASED)
//                        keysDown[key] = false
//
//                // Modifiers are not reliable across systems
//                keyCtrl = it.isControlDown
//                keyShift = it.isShiftDown
//                keyAlt = it.isAltDown
//                keySuper = it.isMetaDown
//            }
//        }
//
//        charListener = EventHandler {
//            it.character.forEach { char -> io.addInputCharacter(char) }
//        }
//
//        scrollListener = EventHandler {
//            io.mouseWheelH += it.deltaX.f / 10.0f
//            io.mouseWheel += it.deltaY.f / 10.0f
//        }
//
//        stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mousePressListener)
//        stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mousePressListener)
//        stage.addEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveListener)
//        stage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseMoveListener)
//        stage.addEventHandler(KeyEvent.KEY_PRESSED, keyListener)
//        stage.addEventHandler(KeyEvent.KEY_RELEASED, keyListener)
//        stage.addEventHandler(KeyEvent.KEY_TYPED, charListener)
//        stage.addEventHandler(ScrollEvent.ANY, scrollListener)
//
//        io.backendRendererName = "imgui impl jfx"
//        io.backendPlatformName = null
//        io.backendLanguageUserData = null
//        io.backendRendererUserData = null
//        io.backendPlatformUserData = null
//        io.setClipboardTextFn = { text ->
//            Clipboard.getSystemClipboard().setContent(mapOf(DataFormat.PLAIN_TEXT to text))
//        }
//        io.getClipboardTextFn = {
//            Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT) as String
//        }
//        io.clipboardUserData = NULL
//
//        val (pixels, size) = io.fonts.getTexDataAsAlpha8()
//
//        val createTex = WritableImage(size.x, size.y)
//
//        with(createTex.pixelWriter) {
//            for (y in 0 until size.y) {
//                for (x in 0 until size.x) {
//                    setArgb(x, y, (pixels[(y * size.x) + x].toInt() shl 24) + 0xFFFFFF)
//                }
//            }
//        }
//
//        io.fonts.texId = addTex(createTex)
//        isInit = true
//    }
//
//    private fun addTex(tex: Image): Int {
//        val ret = tex.hashCode()
//        texColorMapping[Pair(ret, TEXTURE_COLOR_UNMULTIPLIED)] = tex
//        return ret
//    }
//
//    private fun setupMappings() {
//        with(io) {
//            keyMap[Key.Tab] = KeyCode.TAB.code
//            keyMap[Key.LeftArrow] = KeyCode.LEFT.code
//            keyMap[Key.RightArrow] = KeyCode.RIGHT.code
//            keyMap[Key.UpArrow] = KeyCode.UP.code
//            keyMap[Key.DownArrow] = KeyCode.DOWN.code
//            keyMap[Key.PageUp] = KeyCode.PAGE_UP.code
//            keyMap[Key.PageDown] = KeyCode.PAGE_DOWN.code
//            keyMap[Key.Home] = KeyCode.HOME.code
//            keyMap[Key.End] = KeyCode.END.code
//            keyMap[Key.Insert] = KeyCode.INSERT.code
//            keyMap[Key.Delete] = KeyCode.DELETE.code
//            keyMap[Key.Backspace] = KeyCode.BACK_SPACE.code
//            keyMap[Key.Space] = KeyCode.SPACE.code
//            keyMap[Key.Enter] = KeyCode.ENTER.code
//            keyMap[Key.Escape] = KeyCode.ESCAPE.code
//            keyMap[Key.A] = KeyCode.A.code
//            keyMap[Key.C] = KeyCode.C.code
//            keyMap[Key.V] = KeyCode.V.code
//            keyMap[Key.X] = KeyCode.X.code
//            keyMap[Key.Y] = KeyCode.Y.code
//            keyMap[Key.Z] = KeyCode.Z.code
//        }
//    }
//
//    fun newFrame() {
//        if (!isInit)
//            createDeviceObjects()
//
//        io.displaySize.put(canvas.width, canvas.height)
//        io.displayFramebufferScale.x = 1f
//        io.displayFramebufferScale.y = 1f
//
//        val currentTime = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
//        io.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
//        time = currentTime
//
//        updateMousePos()
//        updateMouseCursor()
//
//        io.navInputs.fill(0f)
//    }
//
//    private fun updateMousePos() {
//        if (io.configFlags has ConfigFlag.NoMouseUpdate)
//            return
//
//        repeat(io.mouseDown.size) {
//            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
//                events that are shorter than 1 frame.   */
//            io.mouseDown[it] = mouseJustPressed[it] || (io.mouseDown[it] and !mouseJustReleased[it])
//            mouseJustPressed[it] = false
//            mouseJustReleased[it] = false
//        }
//
//        // Update mouse position
//        if (stage.isFocused) {
//            if (io.wantSetMousePos)
//                r.mouseMove(Point2D(stage.x + io.mousePos.x.d, stage.y + io.mousePos.y.d)) //TODO: Check if stage root is upper left or actually on canvas
//            else
//                io.mousePos put (mousePos)
//        }
//    }
//
//    private val texColorMapping = HashMap<Pair<TextureID, Int>, Image>()
//
//    private fun updateMouseCursor() {
//
//        if (io.configFlags has ConfigFlag.NoMouseCursorChange)
//            return
//
//        val imguiCursor = ImGui.mouseCursor
//        if (imguiCursor == MouseCursor.None || io.mouseDrawCursor)
//            stage.scene.cursor = Cursor.NONE
//        else {
//            // Show OS mouse cursor
//            stage.scene.cursor = when (imguiCursor) {
//                MouseCursor.None -> Cursor.NONE
//                MouseCursor.Arrow -> Cursor.DEFAULT
//                MouseCursor.TextInput -> Cursor.TEXT
//                MouseCursor.ResizeAll -> Cursor.MOVE
//                MouseCursor.ResizeNS -> Cursor.V_RESIZE
//                MouseCursor.ResizeEW -> Cursor.H_RESIZE
//                MouseCursor.ResizeNESW -> Cursor.SW_RESIZE
//                MouseCursor.ResizeNWSE -> Cursor.SE_RESIZE
//                MouseCursor.Hand -> Cursor.HAND
//            }
//        }
//    }
//
//    var xs = DoubleArray(16)
//    var ys = DoubleArray(16)
//
//    fun renderDrawData(drawData: DrawData) {
//        val gc = canvas.graphicsContext2D
//        gc.save() //save user settings
//        //set our settings
//        gc.globalBlendMode = BlendMode.SRC_OVER
//        gc.fillRule = FillRule.NON_ZERO
//        gc.lineCap = StrokeLineCap.BUTT
//        gc.lineJoin = StrokeLineJoin.MITER
//        gc.lineWidth = 0.0
//        //TODO: Remove
//        val pw = gc.pixelWriter
//
//        // Will project scissor/clipping rectangles into framebuffer space
//        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
//        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)
//
//        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
//        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
//        if (fbWidth == 0 || fbHeight == 0) return
//
//        for (cmdList in drawData.cmdLists) {
//            var idxBufferOffset = 0
//            for (cmd in cmdList.cmdBuffer) {
//                val cb = cmd.userCallback
//                if (cb != null)
//                // User callback (registered via ImDrawList::AddCallback)
//                    cb(cmdList, cmd)
//                else {
//                    //set up the clipping rectangle
//                    val clipRectX = (cmd.clipRect.x - clipOff.x) * clipScale.x
//                    val clipRectY = (cmd.clipRect.y - clipOff.y) * clipScale.y
//                    val clipRectZ = (cmd.clipRect.z - clipOff.x) * clipScale.x
//                    val clipRectW = (cmd.clipRect.w - clipOff.y) * clipScale.y
//
//                    //if we are inside the window
//                    if (clipRectX < fbWidth && clipRectY < fbHeight && clipRectZ >= 0f && clipRectW >= 0f) {
//
//                        //set up javafx scissor
//                        gc.save()
//                        gc.beginPath()
//                        gc.moveTo(clipRectX.d, clipRectY.d)
//                        gc.lineTo(clipRectX.d, clipRectW.d)
//                        gc.lineTo(clipRectZ.d, clipRectW.d)
//                        gc.lineTo(clipRectZ.d, clipRectY.d)
//                        gc.lineTo(clipRectX.d, clipRectY.d)
//                        gc.closePath()
//                        gc.clip()
//                        //scissor done
//
//                        assert(texColorMapping.containsKey(Pair(cmd.textureId!!, TEXTURE_COLOR_UNMULTIPLIED))) { "Attempted to use a texture that was not added!" }
//                        val currentTex = texColorMapping[Pair(cmd.textureId!!, TEXTURE_COLOR_UNMULTIPLIED)]!!
//                        val texPr = currentTex.pixelReader
//
//                        //for single color draws. this will be overwritten
//                        var col = JFXColor(0.0, 0.0, 0.0, 0.0)
//                        //for multi-triangle draws, when they border, how many verts there are
//                        var pos = 0
//                        //add a point to the polygon that will be drawn
//                        fun addPoint(x: Float, y: Float) {
//                            if (pos == xs.size) {
//                                if (DEBUG)
//                                    println("increase points buffer size (old ${xs.size}, new ${xs.size * 2})")
//                                val nx = DoubleArray(xs.size * 2)
//                                val ny = DoubleArray(ys.size * 2)
//                                xs.copyInto(nx)
//                                ys.copyInto(ny)
//                                xs = nx
//                                ys = ny
//                            }
//                            xs[pos] = x.d
//                            ys[pos++] = y.d
//                        }
//
//                        //in case of texture drawing, we grab a triangle in ahead and need to skip
//                        var skip = false
//                        for (tri in 0 until cmd.elemCount step 3) {
//                            if (skip) {
//                                skip = false
//                                continue
//                            }
//                            val baseIdx = tri + idxBufferOffset
//                            val idx1 = cmdList.idxBuffer[baseIdx]
//                            val vtx1 = cmdList.vtxBuffer[idx1]
//                            val vtx2 = cmdList.vtxBuffer[cmdList.idxBuffer[baseIdx + 1]]
//                            val idx3 = cmdList.idxBuffer[baseIdx + 2]
//                            val vtx3 = cmdList.vtxBuffer[idx3]
//
//                            var col1 = vtx1.col
//
//                            /**
//                             * The actual drawing function
//                             * @param onlyLast Default false. Used for when switching to different rendering modes to clear any indexed triangles
//                             */
//                            fun draw(onlyLast: Boolean = false) {
//                                if (pos != 0) { //There are vertices in the buffer to draw
//                                    gc.fill = col
//                                    gc.fillPolygon(xs, ys, pos)
//                                    pos = 0
//                                } else if (!onlyLast) { //if we are not switching modes, draw the current triangles
//                                    val color = texPr.getColor((vtx1.uv.x * currentTex.width).toInt(), (vtx1.uv.y * currentTex.height).toInt())
//                                    val x = JFXColor.rgb(
//                                            (((col1 ushr COL32_R_SHIFT) and COLOR_SIZE_MASK) * color.red).i,
//                                            (((col1 ushr COL32_G_SHIFT) and COLOR_SIZE_MASK) * color.green).i,
//                                            (((col1 ushr COL32_B_SHIFT) and COLOR_SIZE_MASK) * color.blue).i,
//                                            (((col1 ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble()) * color.opacity)
//                                    gc.fill = x
//                                    gc.fillPolygon(doubleArrayOf(vtx1.pos.x.toDouble(), vtx2.pos.x.toDouble(), vtx3.pos.x.toDouble()),
//                                            doubleArrayOf(vtx1.pos.y.toDouble(), vtx2.pos.y.toDouble(), vtx3.pos.y.toDouble()), 3)
//                                }
//                            }
//
//                            if (vtx1.uv == vtx2.uv) {
//                                //in OpenGL this is done in shaders as `color * texture(texCoord)
//
//                                /*
//                                Barycentric coordinate determination
//                                Barycentric coordinate rendering is VERY slow
//                                If the colors are all the same, it does not need to and will not be invoked.
//                                Otherwise, we check if the barycentric area is large enough to be meaningful.
//                                 */
//                                val isBary = vtx1.col != vtx2.col || vtx2.col != vtx3.col
//                                val doBary = if (isBary) {
//                                    triangleArea(vtx1.pos, vtx2.pos, vtx3.pos) >= BARYCENTRIC_SIZE_THRESHOLD &&
//                                            atLeastTwo(Math.abs(vtx1.pos.x - vtx2.pos.x) >= 2.0,
//                                            Math.abs(vtx1.pos.x - vtx3.pos.x) >= 2.0,
//                                            Math.abs(vtx3.pos.x - vtx2.pos.x) >= 2.0)
//                                } else {
//                                    false
//                                }
//
//                                if (doBary) {
//                                    //barycentric
//
//                                    draw(true) //flush old verts
//
//                                    //set up so that vt1.y >= vt2.y >= vt3.y
//                                    val vt3 = if (vtx1.pos.y > vtx2.pos.y) if (vtx1.pos.y > vtx3.pos.y) vtx1 else vtx3 else if (vtx2.pos.y > vtx3.pos.y) vtx2 else vtx3
//                                    val vt1 = if (vtx1.pos.y < vtx2.pos.y) if (vtx1.pos.y < vtx3.pos.y) vtx1 else vtx3 else if (vtx2.pos.y < vtx3.pos.y) vtx2 else vtx3
//
//                                    //process of elimination
//                                    val vt2 = if (vt1 == vtx1)
//                                        if (vt3 == vtx2)
//                                            vtx3
//                                        else
//                                            vtx2
//                                    else
//                                        if (vt1 == vtx2)
//                                            if (vt3 == vtx3)
//                                                vtx1
//                                            else
//                                                vtx3
//                                        else
//                                            if (vt3 == vtx2)
//                                                vtx1
//                                            else
//                                                vtx2
//
//                                    //set up all the constant barycentric coords for this triangle
//                                    val v0 = vt2.pos - vt1.pos
//                                    val v1 = vt3.pos - vt1.pos
//
//                                    val d00 = dotProd(v0, v0)
//                                    val d01 = dotProd(v0, v1)
//                                    val d11 = dotProd(v1, v1)
//
//                                    val denom = d00 * d11 - d01 * d01
//                                    //end setup
//
//                                    /**
//                                     * Draw the color at point [p] based on the currently set up barycentric triangle
//                                     */
//                                    fun baryColor(p: Vec2) {
//                                        //get the rest of barycentric information
//                                        val v2 = p - vt1.pos
//                                        val d20 = dotProd(v2, v0)
//                                        val d21 = dotProd(v2, v1)
//                                        val v = (d11 * d20 - d01 * d21) / denom
//                                        val w = (d00 * d21 - d01 * d20) / denom
//                                        val u = 1.0 - v - w
//                                        //turn the colors into usable colors
//                                        val c1 = vt1.col.toJFXColor()
//                                        val c2 = vt2.col.toJFXColor()
//                                        val c3 = vt3.col.toJFXColor()
//                                        //mix colors based on their involvement, and clamp to [0.0,1.0]
//                                        //u,v,w can be negative
//                                        gc.fill = JFXColor(
//                                                ((c1.red * u) + (c2.red * v) + (c3.red * w)).coerceIn(0.0, 1.0),
//                                                ((c1.green * u) + (c2.green * v) + (c3.green * w)).coerceIn(0.0, 1.0),
//                                                ((c1.blue * u) + (c2.blue * v) + (c3.blue * w)).coerceIn(0.0, 1.0),
//                                                ((c1.opacity * u) + (c2.opacity * v) + (c3.opacity * w)).coerceIn(0.0, 1.0)
//                                        )
//                                        //draw a 1x1 rectangle at `p`
//                                        gc.fillRect(p.x.d, p.y.d, 1.0, 1.0)
//                                    }
//
//                                    /**
//                                     * Draws a bottom flat triangle with barycentric color coord mixing
//                                     */
//                                    fun fillBottomFlatTriangle(vs1: Vec2, vs2: Vec2, vs3: Vec2) {
//                                        val invslope1p = (vs2.x - vs1.x) / (vs2.y - vs1.y)
//                                        val invslope2p = (vs3.x - vs1.x) / (vs3.y - vs1.y)
//
//                                        val (invslope1, invslope2) = if (invslope1p > invslope2p) Pair(invslope2p, invslope1p) else Pair(invslope1p, invslope2p)
//
//                                        var curx1 = vs1.x
//                                        var curx2 = vs1.x
//
//                                        val minY = vs1.y
//                                        val maxY = vs2.y
//
//                                        val minX = vs3.x min vs2.x min vs1.x
//                                        val maxX = vs3.x max vs2.x max vs1.y
//
//                                        //if it's a line, needs to be drawn specially so that only 1 pixel isn't drawn
//                                        if (maxY - minY > 1.0f) {
//                                            for (scanlineY in Math.round(minY).i..Math.round(maxY).i) {
//                                                for (x in Math.round(curx1).i..Math.round(curx2).i) {
//                                                    baryColor(Vec2(x, scanlineY))
//                                                }
//                                                curx1 += invslope1
//                                                curx2 += invslope2
//                                                curx1 = curx1.coerceAtLeast(minX) //if the difference in y's is less than 1, this will properly clamp the x
//                                                curx2 = curx2.coerceAtMost(maxX)  //if the difference in y's is less than 1, this will properly clamp the x
//                                            }
//                                        } else {
//                                            //average where the line goes
//                                            val scanlineY = (maxY + minY) / 2.0f
//                                            for (x in Math.round(minX).i..Math.round(maxX).i) {
//                                                //draw the color at each point on the line
//                                                baryColor(Vec2(x, scanlineY))
//                                            }
//                                        }
//                                    }
//
//                                    /**
//                                     * Draws a top flat triangle with barycentric color coord mixing
//                                     *
//                                     * For more information on the steps, similar to fillBottomFlatTriangle
//                                     */
//                                    fun fillTopFlatTriangle(vs1: Vec2, vs2: Vec2, vs3: Vec2) {
//                                        val invslope1p = (vs3.x - vs1.x) / (vs3.y - vs1.y)
//                                        val invslope2p = (vs3.x - vs2.x) / (vs3.y - vs2.y)
//
//                                        val (invslope1, invslope2) = if (invslope1p > invslope2p) Pair(invslope1p, invslope2p) else Pair(invslope2p, invslope1p)
//
//                                        var curx1 = vs3.x
//                                        var curx2 = vs3.x
//
//                                        val maxY = vs3.y
//                                        val minY = vs1.y
//
//                                        val minX = vs3.x min vs2.x min vs1.x
//                                        val maxX = vs3.x max vs2.x max vs1.y
//
//                                        if (maxY - minY > 1.0f) {
//                                            for (scanlineY in Math.round(maxY).i downTo Math.round(minY).i) {
//                                                for (x in Math.round(curx1).i..Math.round(curx2).i) {
//                                                    baryColor(Vec2(x, scanlineY))
//                                                }
//                                                curx1 -= invslope1
//                                                curx2 -= invslope2
//                                                curx1 = curx1.coerceAtLeast(minX)
//                                                curx2 = curx2.coerceAtMost(maxX)
//                                            }
//                                        } else {
//                                            val scanlineY = (maxY + minY) / 2.0f
//                                            for (x in Math.round(minX).i..Math.round(maxX).i) {
//                                                baryColor(Vec2(x, scanlineY))
//                                            }
//                                        }
//                                    }
//
//                                    //check if this is a top flat, bottom flat, or general triangle
//                                    when {
//                                        vt2.pos.y == vt3.pos.y -> {
//                                            fillBottomFlatTriangle(vt1.pos, vt2.pos, vt3.pos)
//                                        }
//                                        vt1.pos.y == vt2.pos.y -> {
//                                            fillTopFlatTriangle(vt1.pos, vt2.pos, vt3.pos)
//                                        }
//                                        else -> {
//                                            /* general case - split the triangle in a topflat and bottom-flat one */
//                                            val v4 = Vec2((vt1.pos.x + (vt2.pos.y - vt1.pos.y) / (vt3.pos.y - vt1.pos.y) * (vt3.pos.x - vt1.pos.x)), vt2.pos.y)
//                                            fillBottomFlatTriangle(vt1.pos, vt2.pos, v4)
//                                            fillTopFlatTriangle(vt2.pos, v4, vt3.pos)
//                                        }
//                                    }
//                                } else if (tri + 3 < cmd.elemCount) { //next triangle exists
//                                    //check if it borders the next triangle
//                                    val idx4 = cmdList.idxBuffer[baseIdx + 3]
//                                    val idx5 = cmdList.idxBuffer[baseIdx + 4]
//                                    if (idx4 == idx1 && idx5 == idx3) {
//                                        //borders the next triangle
//                                        val vtx6 = cmdList.vtxBuffer[cmdList.idxBuffer[baseIdx + 5]]
//                                        //if the list is empty
//                                        if (pos == 0) {
//                                            //set color to the first color
//                                            val color = texPr.getColor((vtx1.uv.x * currentTex.width).toInt(), (vtx1.uv.y * currentTex.height).toInt())
//                                            //if it is barycentric but missed the size check, get the most saturated color
//                                            if(isBary) {
//                                                col1 = if (vtx1.col.toVec4().length() > vtx2.col.toVec4().length())
//                                                    if (vtx1.col.toVec4().length() > vtx3.col.toVec4().length())
//                                                        vtx1.col
//                                                    else
//                                                        vtx3.col
//                                                else
//                                                    if (vtx2.col.toVec4().length() > vtx3.col.toVec4().length())
//                                                        vtx2.col
//                                                    else
//                                                        vtx3.col
//                                            }
//                                            col = JFXColor.rgb(
//                                                    (((col1 ushr COL32_R_SHIFT) and COLOR_SIZE_MASK) * color.red).i,
//                                                    (((col1 ushr COL32_G_SHIFT) and COLOR_SIZE_MASK) * color.green).i,
//                                                    (((col1 ushr COL32_B_SHIFT) and COLOR_SIZE_MASK) * color.blue).i,
//                                                    (((col1 ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble()) * color.opacity)
//                                            //add initial points
//                                            addPoint(vtx1.pos.x, vtx1.pos.y)
//                                            addPoint(vtx2.pos.x, vtx2.pos.y)
//                                            addPoint(vtx3.pos.x, vtx3.pos.y)
//                                        }
//                                        //add bordering point
//                                        addPoint(vtx6.pos.x, vtx6.pos.y)
//                                    } else {
//                                        //does not border the next triangle , do either draw this triangle or draw the last one
//                                        //if the last triangle bordered this one, the vertex is already in the shape, so just draw
//                                        draw()
//                                    }
//                                } else {
//                                    //no more triangles to border, just plain draw
//                                    draw()
//                                }
//                            } else {
//                                /**
//                                 * Draws textures with CPU rendering. Useful if only a part of a triangle is showing
//                                 */
//                                fun drawSlow() {
//                                    if (DEBUG && warnSlowTex) {
//                                        warnSlowTex = false
//                                        println("OpenJFX slow texture rendering has been invoked!")
//                                    }
//                                    if (vtx1.uv.y == vtx2.uv.y) {
//                                        //Top flat triangle
//                                        if (vtx3.uv.y > vtx1.uv.y) {
//                                            val minY = Math.round(vtx1.pos.y).i
//                                            val maxY = Math.round(vtx3.pos.y).i
//                                            val minYUV = vtx1.uv.y
//                                            val maxYUV = vtx3.uv.y
//                                            val yuvDiff = maxYUV - minYUV
//                                            val yDiff = maxY - minY
//                                            val minX = vtx1.pos.x.i
//                                            val maxX = vtx3.pos.x.i
//                                            val minXUV = vtx1.uv.x
//                                            val maxXUV = vtx3.uv.x
//                                            val xDiff = maxX - minX
//                                            val xuvDiff = maxXUV - minXUV
//                                            for (y in minY until maxY + 1) {
//                                                for (x in xDiff downTo (xDiff * (y.f - minY) / yDiff).i) {
//                                                    val xPct = x.d / xDiff
//                                                    val yPct = 1 - ((maxY.d - y.d) / yDiff)
//                                                    val c = texPr.getArgb((currentTex.width * (minXUV + (xPct * xuvDiff))).i, (currentTex.height * (minYUV + (yPct * yuvDiff))).i)
//                                                    if ((c and COL32_A_MASK) == 0)
//                                                        continue
//                                                    pw.setArgb(minX + x, y, c)
//                                                }
//                                            }
//                                        } else {
//                                            TODO("vtx1y == vtx2y vtx3 low")
//                                        }
//                                    } else if (vtx2.uv.y == vtx3.uv.y) {
//                                        if (vtx3.uv.y > vtx1.uv.y) {
//                                            val minY = Math.round(vtx1.pos.y).i
//                                            val maxY = Math.round(vtx3.pos.y).i
//                                            val minYUV = vtx1.uv.y
//                                            val maxYUV = vtx3.uv.y
//                                            val yuvDiff = maxYUV - minYUV
//                                            val yDiff = maxY - minY
//                                            val minX = vtx1.pos.x.i
//                                            val maxX = vtx2.pos.x.i
//                                            val minXUV = vtx1.uv.x
//                                            val maxXUV = vtx2.uv.x
//                                            val xDiff = maxX - minX
//                                            val xuvDiff = maxXUV - minXUV
//                                            for (y in minY until maxY + 1) {
//                                                for (x in 0 until (xDiff * (y.f - minY) / yDiff).i) {
//                                                    val xPct = x.d / xDiff
//                                                    val yPct = 1.0 - ((maxY.d - y.d) / yDiff)
//                                                    val c = texPr.getArgb((currentTex.width * (minXUV + (xPct * xuvDiff))).i, (currentTex.height * (minYUV + (yPct * yuvDiff))).i)
//                                                    if ((c and COL32_A_MASK) == 0)
//                                                        continue
//                                                    pw.setArgb(minX + x, y, c)
//                                                }
//                                            }
//                                        } else {
//                                            TODO("vtx2y == vtxy3 vtx3 low")
//                                        }
//                                    } else if (vtx1.uv.y == vtx3.uv.y) {
//                                        TODO("vtx1y == vtxy3 ")
//                                    } else {
//                                        TODO("no y similarities")
//                                    }
//                                }
//
//
//                                draw(true) //flush any undrawn triangles
//                                if (tri + 3 < cmd.elemCount) { //see if there's another triangle
//                                    val idx4 = cmdList.idxBuffer[baseIdx + 3]
//                                    val idx5 = cmdList.idxBuffer[baseIdx + 4]
//                                    if (idx4 == idx1 && idx5 == idx3) { //if the indices match, we assume that it's square
//                                        //get the image that is correctly colored, or compute
//                                        val cTex = texColorMapping.computeIfAbsent(Pair(cmd.textureId!!, col1)) {
//                                            if (DEBUG)
//                                                println("generating color multiplied texture (texture ${cmd.textureId!!} ${if (cmd.textureId!! == io.fonts.texId) "[font texture]" else ""}, color ${col1.toVec4() * Vec4(255)})")
//                                            val retImg = WritableImage(currentTex.width.i, currentTex.height.i)
//                                            val nipw = retImg.pixelWriter
//                                            val textCol = vtx1.col.toJFXColor()
//                                            for (x in 0 until currentTex.width.i) {
//                                                for (y in 0 until currentTex.height.i) {
//                                                    //get plain texture color
//                                                    val cColor = texPr.getColor(x, y)
//                                                    //multiply by the given color
//                                                    nipw.setColor(x, y,
//                                                            JFXColor(
//                                                                    textCol.red * cColor.red,
//                                                                    textCol.green * cColor.green,
//                                                                    textCol.blue * cColor.blue,
//                                                                    textCol.opacity * cColor.opacity
//                                                            )
//                                                    )
//                                                }
//                                            }
//                                            retImg
//                                        }
//                                        //TODO: this only works when the x and y components of vtx3 are greater than those of vtx1
//                                        gc.drawImage(cTex, (cTex.width * vtx1.uv.x).d, (cTex.height * vtx1.uv.y).d,
//                                                (cTex.width * (vtx3.uv.x - vtx1.uv.x)).d, (cTex.height * (vtx3.uv.y - vtx1.uv.y)).d,
//                                                vtx1.pos.x.d, vtx1.pos.y.d, vtx3.pos.x.d - vtx1.pos.x.d, vtx3.pos.y.d - vtx1.pos.y.d)
//                                        skip = true
//                                    } else { //not matching indices, draw the triangle
//                                        drawSlow()
//                                    }
//                                } else { //not another triangle to be square with, draw a triangle
//                                    drawSlow()
//                                }
//                            }
//                        }
//                    }
//                    gc.restore() //restore scissor state
//                }
//                idxBufferOffset += cmd.elemCount
//            }
//        }
//        gc.restore() //restore user graphicscontext state
//    }
//}
//
//fun Vec4.toJFXColor(): JFXColor {
//    return JFXColor(r.d.coerceIn(0.0, 1.0), g.d.coerceIn(0.0, 1.0), b.d.coerceIn(0.0, 1.0), a.d.coerceIn(0.0, 1.0))
//}
//
//fun Int.toJFXColor(): JFXColor {
//    return JFXColor.rgb(
//            ((this ushr COL32_R_SHIFT) and COLOR_SIZE_MASK),
//            ((this ushr COL32_G_SHIFT) and COLOR_SIZE_MASK),
//            ((this ushr COL32_B_SHIFT) and COLOR_SIZE_MASK),
//            ((this ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble())
//}
//
//fun Int.toVec4(): Vec4 {
//    return Vec4(
//            ((this ushr COL32_R_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble(),
//            ((this ushr COL32_G_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble(),
//            ((this ushr COL32_B_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble(),
//            ((this ushr COL32_A_SHIFT) and COLOR_SIZE_MASK) / COLOR_SIZE_MASK.toDouble())
//}
//
//inline fun dotProd(a: Vec2, b: Vec2) = ((a.x * b.x) + (a.y * b.y)).d
//
//inline fun triangleArea(a: Vec2, b: Vec2, c: Vec2) = Math.abs((a.x * (b.y - c.y)) + (b.x * (c.y - a.y)) + (c.x * (a.y - b.y)))
//
//inline fun atLeastTwo(a: Boolean, b: Boolean, c: Boolean): Boolean {
//    return if (a) b || c else b && c
//}