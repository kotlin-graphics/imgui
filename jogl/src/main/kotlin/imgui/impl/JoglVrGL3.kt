package imgui.impl

//import com.jogamp.newt.event.KeyEvent
//import com.jogamp.newt.event.KeyListener
//import com.jogamp.newt.event.MouseEvent
//import com.jogamp.newt.event.MouseListener
//import com.jogamp.newt.opengl.GLWindow
//import com.jogamp.opengl.GL
//import com.jogamp.opengl.GL2ES3
//import com.jogamp.opengl.GL2ES3.*
//import com.jogamp.opengl.GL2GL3.GL_FILL
//import com.jogamp.opengl.GL2GL3.GL_POLYGON_MODE
//import com.jogamp.opengl.GL3
//import com.jogamp.opengl.GL3ES3.GL_SAMPLER_BINDING
//import glm_.*
//import glm_.vec2.Vec2
//import glm_.vec2.Vec2i
//import glm_.vec4.Vec4i
//import gln.glf.semantic
//import imgui.*
//import imgui.ImGui.io
//import imgui.ImGui.mouseCursor
//import kool.*
//import kool.set
//import kool.stak
//import org.lwjgl.system.MemoryStack
//
//object JoglVrGL3 {
//
//    lateinit var window: GLWindow
//    val texSize = Vec2i()
//    var time = 0.0
//
//    lateinit var program: JoglProgram
//
//    fun init(window: GLWindow, texSize: Vec2i, installCallbacks: Boolean): Boolean {
//
//        JoglVrGL3.window = window
//        JoglVrGL3.texSize put texSize
//
//        with(io) {
//
//            // Setup backend capabilities flags
//            backendFlags = backendFlags or BackendFlag.HasMouseCursors   // We can honor GetMouseCursor() values (optional)
//            backendFlags = backendFlags or BackendFlag.HasSetMousePos    // We can honor io.WantSetMousePos requests (optional, rarely used)
//
//            // Keyboard mapping. Dear ImGui will use those indices to peek into the io.KeysDown[] array.
//            keyMap[Key.Tab] = KeyEvent.VK_TAB.i
//            keyMap[Key.LeftArrow] = KeyEvent.VK_LEFT.i
//            keyMap[Key.RightArrow] = KeyEvent.VK_RIGHT.i
//            keyMap[Key.UpArrow] = KeyEvent.VK_UP.i
//            keyMap[Key.DownArrow] = KeyEvent.VK_DOWN.i
//            keyMap[Key.PageUp] = KeyEvent.VK_PAGE_UP.i
//            keyMap[Key.PageDown] = KeyEvent.VK_PAGE_DOWN.i
//            keyMap[Key.Home] = KeyEvent.VK_HOME.i
//            keyMap[Key.End] = KeyEvent.VK_END.i
//            keyMap[Key.Insert] = KeyEvent.VK_INSERT.i
//            keyMap[Key.Delete] = KeyEvent.VK_DELETE.i
//            keyMap[Key.Backspace] = KeyEvent.VK_BACK_SPACE.i
//            keyMap[Key.Space] = KeyEvent.VK_SPACE.i
//            keyMap[Key.Enter] = KeyEvent.VK_ENTER.i
//            keyMap[Key.Escape] = KeyEvent.VK_ESCAPE.i
//            keyMap[Key.A] = KeyEvent.VK_A.i
//            keyMap[Key.C] = KeyEvent.VK_C.i
//            keyMap[Key.V] = KeyEvent.VK_V.i
//            keyMap[Key.X] = KeyEvent.VK_X.i
//            keyMap[Key.Y] = KeyEvent.VK_Y.i
//            keyMap[Key.Z] = KeyEvent.VK_Z.i
//        }
//
//        if (installCallbacks) {
////            window.addMouseListener(mouseCallback)
////            window.addKeyListener(keyCallback)
//        }
//
//        return true
//    }
//
//    val cursorPos = Vec2i()
//    lateinit var gl: GL3
//
//    fun newFrame(gl: GL3) {
//
//        JoglVrGL3.gl = gl
//
//        if (fontTexture[0] <= 0) gl.createDeviceObjects()
//
//        assert(io.fonts.isBuilt) { "Font atlas needs to be built" }
//
//        // Setup display size (every frame to accommodate for window resizing)
//        io.displaySize put texSize
//        io.displayFramebufferScale.x = 1f //if (window.width > 0) window.framebufferSize.x / window.size.x.f else 0f
//        io.displayFramebufferScale.y = 1f //if (window.height > 0) window.framebufferSize.y / window.size.y.f else 0f
//
//        // Setup time step
//        val currentTime = System.nanoTime() / 1e9
//        io.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
//        time = currentTime
//
//        updateMousePosAndButtons()
//        updateMouseCursor()
//
//        // Gamepad navigation mapping [BETA] ... TODO
//
//        /*  Start the frame. This call will update the io.wantCaptureMouse, io.wantCaptureKeyboard flag that you can use
//            to dispatch inputs (or not) to your application.         */
//        ImGui.newFrame()
//    }
//
//    private fun updateMousePosAndButtons() {
//
//        repeat(io.mouseDown.size) {
//            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
//                events that are shorter than 1 frame.   */
//            io.mouseDown[it] = mouseJustPressed[it]
//            mouseJustPressed[it] = false
//        }
//
//        // Update mouse position
//        val mousePosBackup = Vec2i(io.mousePos)
//        io.mousePos put -Float.MAX_VALUE
//        if (window.hasFocus())
//            if (io.wantSetMousePos)
//                window.warpPointer(mousePosBackup.x, mousePosBackup.y)
//            else
//                io.mousePos put cursorPos
//    }
//
//    private fun updateMouseCursor() {
//
//        if (io.configFlags has ConfigFlag.NoMouseCursorChange || !window.isPointerVisible)
//            return
//
//        val imguiCursor = mouseCursor
//        if (imguiCursor == MouseCursor.None || io.mouseDrawCursor)
//        // Hide OS mouse cursor if imgui is drawing it or if it wants no cursor
//            window.isPointerVisible = false
//        else {
//            // Show OS mouse cursor
//            // FIXME-PLATFORM: Unfocused windows seems to fail changing the mouse cursor with GLFW 3.2, but 3.3 works here.
////            window.cursor = when (mouseCursors[imguiCursor.i]) {
////                0L -> mouseCursors[MouseCursor.Arrow.i]
////                else -> mouseCursors[imguiCursor.i]
////            }
//            window.isPointerVisible = true
//        }
//    }
//
//    private fun GL3.createDeviceObjects(): Boolean {
//
//        // Backup GL state
//        // we have to save also program since we do the uniform mat and texture setup once here
//        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
//        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
//        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
//        val lastVertexArray = glGetInteger(GL2ES3.GL_VERTEX_ARRAY_BINDING)
//
//        program = JoglProgram(this, vertexShader, fragmentShader)
//
//        glGenBuffers(Buffer.MAX, bufferName)
//
//        glGenVertexArrays(1, vaoName)
//        glBindVertexArray(vaoName[0])
//
//        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
//        glBufferData(GL_ARRAY_BUFFER, vtxSize.L, null, GL_STREAM_DRAW)
//        glEnableVertexAttribArray(semantic.attr.POSITION)
//        glEnableVertexAttribArray(semantic.attr.TEX_COORD)
//        glEnableVertexAttribArray(semantic.attr.COLOR)
//
//        glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
//        glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size.L)
//        glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size.L)
//
//        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
//        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize.L, null, GL_STREAM_DRAW)
//
//        glBindVertexArray(0)
//
//        if (glGetError() != GL.GL_NO_ERROR) throw Error("render")
//
//        createFontsTexture()
//
//        // Restore modified GL state
//        glUseProgram(lastProgram)
//        glBindTexture(GL_TEXTURE_2D, lastTexture)
//        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
//        glBindVertexArray(lastVertexArray)
//
//        return true
//    }
//
//    private fun GL3.checkSize(draws: ArrayList<DrawList>) {
//
//        val minVtxSize = draws.map { it.vtxBuffer.size }.sum() * DrawVert.size
//        val minIdxSize = draws.map { it.idxBuffer.size }.sum() * Int.BYTES
//
//        var newVtxSize = vtxSize
//        while (newVtxSize < minVtxSize)
//            newVtxSize = newVtxSize shl 1
//        var newIdxSize = idxSize
//        while (newIdxSize < minIdxSize)
//            newIdxSize = newIdxSize shl 1
//
//        if (newVtxSize != vtxSize || newIdxSize != idxSize) {
//
//            vtxSize = newVtxSize
//            idxSize = newIdxSize
//
//            vtxBuffer.free()
//            vtxBuffer = ByteBuffer(vtxSize)
//            idxBuffer.free()
//            idxBuffer = IntBuffer(idxSize / Int.BYTES)
//
//            glBindVertexArray(vaoName[0])
//
//            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
//            glBufferData(GL_ARRAY_BUFFER, vtxSize.L, null, GL_STREAM_DRAW)
//            glEnableVertexAttribArray(semantic.attr.POSITION)
//            glEnableVertexAttribArray(semantic.attr.TEX_COORD)
//            glEnableVertexAttribArray(semantic.attr.COLOR)
//
//            glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
//            glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size.L)
//            glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size.L)
//
//            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
//            glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize.L, null, GL_STREAM_DRAW)
//
//            glBindVertexArray(0)
//
//            if (glGetError() != GL.GL_NO_ERROR) throw Error("render")
//
//            if (DEBUG) println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
//        }
//    }
//
//    /** Build texture atlas */
//    private fun GL3.createFontsTexture(): Boolean {
//
//        /*  Load as RGBA 32-bit (75% of the memory is wasted, but default font is so small) because it is more likely
//            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
//            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
//        val (pixels, size) = io.fonts.getTexDataAsRGBA32()
//
//        // Upload texture to graphics system
//        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
//
//        glGenTextures(1, fontTexture)
//        glBindTexture(GL_TEXTURE_2D, fontTexture[0])
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
//        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
//
//        // Store our identifier
//        io.fonts.texId = fontTexture[0]
//
//        // Restore state
//        glBindTexture(GL_TEXTURE_2D, lastTexture)
//
//        if (glGetError() != GL_NO_ERROR) throw Error("createFontsTexture")
//
//        return true
//    }
//
//    /** OpenGL3 Render function.
//     *  (this used to be set in io.renderDrawListsFn and called by ImGui::render(), but you can now call this directly
//     *  from your main loop)
//     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL
//     *  state explicitly, in order to be able to run within any OpenGL engine that doesn't do so.   */
//    fun renderDrawData(drawData: DrawData) = with(gl) {
//        val stack = MemoryStack.stackPush()
//
//        /** Avoid rendering when minimized, scale coordinates for retina displays
//         *  (screen coordinates != framebuffer coordinates) */
//        val fbSize = io.displaySize * io.displayFramebufferScale
//        if (fbSize anyLessThanEqual 0) return@with
//        drawData scaleClipRects io.displayFramebufferScale
//
//        // Backup GL state
//        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
//        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
//        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
//        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
//        val lastSampler = glGetInteger(GL_SAMPLER_BINDING)
//        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
//        val lastVertexArray = glGetInteger(GL2ES3.GL_VERTEX_ARRAY_BINDING)
//        val lastPolygonMode = glGetInteger(GL_POLYGON_MODE)
//        val lastViewport = Vec4i(stak { s -> s.callocInt(4).also { glGetIntegerv(GL_VIEWPORT, it) } })
//        val lastScissorBox = Vec4i(stak { s -> s.callocInt(4).also { glGetIntegerv(GL_SCISSOR_BOX, it) } })
//        val lastBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
//        val lastBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
//        val lastBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
//        val lastBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
//        val lastBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
//        val lastBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
//        val lastEnableBlend = glIsEnabled(GL_BLEND)
//        val lastEnableCullFace = glIsEnabled(GL_CULL_FACE)
//        val lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
//        val lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)
//
//        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
//        glEnable(GL_BLEND)
//        glBlendEquation(GL_FUNC_ADD)
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
//        glDisable(GL_CULL_FACE)
//        glDisable(GL_DEPTH_TEST)
//        glEnable(GL_SCISSOR_TEST)
//        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
//
//        // Setup viewport, orthographic projection matrix
//        glViewport(0, 0, fbSize.x, fbSize.y)
//        val ortho = glm.ortho(mat, 0f, io.displaySize.x.f, io.displaySize.y.f, 0f)
//        glUseProgram(program.name)
//
//        glUniformMatrix4fv(program.mat, 1, false, ortho.toFloatBuffer(stack))
//
//
//        checkSize(drawData.cmdLists)
//
//        glBindVertexArray(vaoName[0])
//        glBindSampler(semantic.sampler.DIFFUSE, 0) // Rely on combined texture/sampler state.
//
//        for (cmdList in drawData.cmdLists) {
//
//            cmdList.vtxBuffer.forEachIndexed { i, v ->
//                val offset = i * DrawVert.size
//                v.pos.to(vtxBuffer, offset)
//                v.uv.to(vtxBuffer, offset + Vec2.size)
//                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
//            }
//            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
//            glBufferSubData(GL_ARRAY_BUFFER, 0, cmdList._vtxWritePtr * DrawVert.size.L, vtxBuffer)
//            cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
//            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
//            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, cmdList._idxWritePtr * Int.BYTES.L, idxBuffer)
//
//            var idxBufferOffset = 0L
//            for (cmd in cmdList.cmdBuffer) {
//                if (cmd.userCallback != null)
//                    cmd.userCallback!!(cmdList, cmd)
//                else {
//                    glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
//                    glScissor(cmd.clipRect.x.i, fbSize.y - cmd.clipRect.w.i, (cmd.clipRect.z - cmd.clipRect.x).i, (cmd.clipRect.w - cmd.clipRect.y).i)
//                    glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, idxBufferOffset)
//                }
//                idxBufferOffset += cmd.elemCount * Int.BYTES
//            }
//        }
//
//        if (glGetError() != GL_NO_ERROR) throw Error("createFontsTexture")
//
//        // Restore modified GL state
//        glUseProgram(lastProgram)
//        glBindTexture(GL_TEXTURE_2D, lastTexture)
//        glBindSampler(0, lastSampler)
//        glActiveTexture(lastActiveTexture)
//        glBindVertexArray(lastVertexArray)
//        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
//        glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha)
//        glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha)
//        if (lastEnableBlend) glEnable(GL_BLEND) else glDisable(GL_BLEND)
//        if (lastEnableCullFace) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
//        if (lastEnableDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
//        if (lastEnableScissorTest) glEnable(GL_SCISSOR_TEST) else glDisable(GL_SCISSOR_TEST)
//        glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode)
//        glViewport(lastViewport.x, lastViewport.y, lastViewport.z, lastViewport.w)
//        glScissor(lastScissorBox.x, lastScissorBox.y, lastScissorBox.z, lastScissorBox.w)
//
//        stack.pop()
//    }
//
//    private object mouseCallback : MouseListener {
//
//        override fun mouseReleased(e: MouseEvent) {
//            if (e.button in MouseEvent.BUTTON1..MouseEvent.BUTTON3)
//                mouseJustPressed[e.button.i - 1] = false
//        }
//
//        override fun mouseMoved(e: MouseEvent) {
//            cursorPos.put(e.x, e.y)
//        }
//
//        override fun mouseEntered(e: MouseEvent) {}
//
//        override fun mouseDragged(e: MouseEvent) {
//            cursorPos.put(e.x, e.y)
//        }
//
//        override fun mouseClicked(e: MouseEvent) {}
//
//        override fun mouseExited(e: MouseEvent) {}
//
//        override fun mousePressed(e: MouseEvent) {
//            if (e.button in MouseEvent.BUTTON1..MouseEvent.BUTTON3)
//                mouseJustPressed[e.button.i - 1] = true
//        }
//
//        override fun mouseWheelMoved(e: MouseEvent) {
//            io.mouseWheel += e.rotation[1]
//            io.mouseWheelH += e.rotation[0] // unchecked
//        }
//    }
//
//    private object keyCallback : KeyListener {
//        //        (void) mods // Modifiers are not reliable across systems
//        override fun keyPressed(e: KeyEvent) = with(io) {
//            if (e.keyCode <= keysDown.size) keysDown[e.keyCode.i] = true
//            if (e.keyCode == KeyEvent.VK_WINDOWS) keySuper = true
//            keyCtrl = e.isControlDown
//            keyShift = e.isShiftDown
//            keyAlt = e.isAltDown
//        }
//
//        override fun keyReleased(e: KeyEvent) = with(io) {
//            if (e.keyCode <= keysDown.size) keysDown[e.keyCode.i] = false
//            if (e.keyCode == KeyEvent.VK_WINDOWS) keySuper = false
//            keyCtrl = e.isControlDown
//            keyShift = e.isShiftDown
//            keyAlt = e.isAltDown
//        }
//    }
//
//    fun shutdown(gl: GL3) {
//        destroyDeviceObjects(gl)
//        window.removeMouseListener(mouseCallback)
//        window.removeKeyListener(keyCallback)
//    }
//
//    private fun destroyDeviceObjects(gl: GL3) = with(gl) {
//
//        glDeleteVertexArrays(1, vaoName)
//        glDeleteBuffers(Buffer.MAX, bufferName)
//
//        if (program.name >= 0) glDeleteProgram(program.name)
//
//        destroyFontsTexture(gl)
//    }
//
//    private fun destroyFontsTexture(gl: GL3) {
//        if (fontTexture[0] != 0) {
//            gl.glDeleteTextures(1, fontTexture)
//            io.fonts.texId = 0
//            fontTexture[0] = 0
//        }
//    }
//}