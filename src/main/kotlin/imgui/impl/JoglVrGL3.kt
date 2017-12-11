package imgui.impl

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.MouseEvent
import com.jogamp.newt.event.MouseListener
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_FILL
import com.jogamp.opengl.GL2GL3.GL_POLYGON_MODE
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3ES3.GL_SAMPLER_BINDING
import glm_.*
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4i
import gln.buf
import gln.glf.semantic
import imgui.*
import uno.buffer.bufferBig
import uno.buffer.destroy
import uno.buffer.intBufferBig
import uno.buffer.intBufferOf
import uno.glsl.Program

object JoglVrGL3 {

    lateinit var window: GLWindow
    val texSize = Vec2i()
    var time = 0.0
    val mouseJustPressed = BooleanArray(3)
    @JvmField
    var mouseWheel = 0f

    object Buffer {
        val Vertex = 0
        val Element = 1
        val MAX = 2
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vaoName = intBufferBig(1)
    lateinit var program: ProgramA
    val fontTexture = intBufferOf(-1)

    val mat = Mat4()

    fun init(window: GLWindow, texSize: Vec2i, installCallbacks: Boolean): Boolean {

        this.window = window
        this.texSize put texSize

        with(IO) {
            // Keyboard mapping. ImGui will use those indices to peek into the io.KeyDown[] array.
            keyMap[Key.Tab] = KeyEvent.VK_TAB.i
            keyMap[Key.LeftArrow] = KeyEvent.VK_LEFT.i
            keyMap[Key.RightArrow] = KeyEvent.VK_RIGHT.i
            keyMap[Key.UpArrow] = KeyEvent.VK_UP.i
            keyMap[Key.DownArrow] = KeyEvent.VK_DOWN.i
            keyMap[Key.PageUp] = KeyEvent.VK_PAGE_UP.i
            keyMap[Key.PageDown] = KeyEvent.VK_PAGE_DOWN.i
            keyMap[Key.Home] = KeyEvent.VK_HOME.i
            keyMap[Key.End] = KeyEvent.VK_END.i
            keyMap[Key.Delete] = KeyEvent.VK_DELETE.i
            keyMap[Key.Backspace] = KeyEvent.VK_BACK_SPACE.i
            keyMap[Key.Enter] = KeyEvent.VK_ENTER.i
            keyMap[Key.Escape] = KeyEvent.VK_ESCAPE.i
            keyMap[Key.A] = KeyEvent.VK_A.i
            keyMap[Key.C] = KeyEvent.VK_C.i
            keyMap[Key.V] = KeyEvent.VK_V.i
            keyMap[Key.X] = KeyEvent.VK_X.i
            keyMap[Key.Y] = KeyEvent.VK_Y.i
            keyMap[Key.Z] = KeyEvent.VK_Z.i

            /* Alternatively you can set this to NULL and call ImGui::GetDrawData() after ImGui::Render() to get the
               same ImDrawData pointer.             */
            renderDrawListsFn = this@JoglVrGL3::renderDrawLists
        }

        if (installCallbacks) {
//            window.addMouseListener(mouseCallback)
//            window.addKeyListener(keyCallback)
        }

        return true
    }

    var vtxSize = 1 shl 5 // 32768
    var idxSize = 1 shl 6 // 65536
    var vtxBuffer = bufferBig(vtxSize)
    var idxBuffer = intBufferBig(idxSize / Int.BYTES)

    val cursorPos = Vec2i()
    lateinit var gl: GL3

    fun newFrame(gl: GL3) {

        this.gl = gl

        if (fontTexture[0] < 0) gl.createDeviceObjects()

        // Setup display size (every frame to accommodate for window resizing)
        IO.displaySize put texSize
        IO.displayFramebufferScale.x = 1f //if (window.width > 0) window.framebufferSize.x / window.size.x.f else 0f
        IO.displayFramebufferScale.y = 1f //if (window.height > 0) window.framebufferSize.y / window.size.y.f else 0f

        // Setup time step
        val currentTime = System.nanoTime() / 1e9
        IO.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        time = currentTime

        /*  Setup inputs
            (we already got mouse wheel, keyboard keys & characters from glfw callbacks polled in glfwPollEvents())
            Mouse position in screen coordinates (set to -1,-1 if no mouse / on another screen, etc.)   */
//        if (window.hasFocus())
        IO.mousePos put cursorPos
//        else
//            IO.mousePos put -Float.MAX_VALUE

        repeat(3) {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            IO.mouseDown[it] = mouseJustPressed[it]
        }

        IO.mouseWheel = mouseWheel
        mouseWheel = 0f

        // Hide OS mouse cursor if ImGui is drawing it
//        window.isPointerVisible = !IO.mouseDrawCursor

        /*  Start the frame. This call will update the IO.wantCaptureMouse, IO.wantCaptureKeyboard flag that you can use
            to dispatch inputs (or not) to your application.         */
        ImGui.newFrame()
    }

    private fun GL3.createDeviceObjects(): Boolean {

        // Backup GL state
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL2ES3.GL_VERTEX_ARRAY_BINDING)

        program = ProgramA(this, "shader")

        glGenBuffers(Buffer.MAX, bufferName)

        glGenVertexArrays(1, vaoName)
        glBindVertexArray(vaoName[0])

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
        glBufferData(GL_ARRAY_BUFFER, vtxSize.L, null, GL_STREAM_DRAW)
        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.TEX_COORD)
        glEnableVertexAttribArray(semantic.attr.COLOR)

        glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
        glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size.L)
        glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size.L)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize.L, null, GL_STREAM_DRAW)

        glBindVertexArray(0)

        if (glGetError() != GL.GL_NO_ERROR) throw Error("render")

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindVertexArray(lastVertexArray)

        return true
    }

    private fun GL3.checkSize(draws: ArrayList<DrawList>) {

        val minVtxSize = draws.map { it.vtxBuffer.size }.sum() * DrawVert.size
        val minIdxSize = draws.map { it.idxBuffer.size }.sum() * Int.BYTES

        var newVtxSize = vtxSize
        while (newVtxSize < minVtxSize)
            newVtxSize = newVtxSize shl 1
        var newIdxSize = idxSize
        while (newIdxSize < minIdxSize)
            newIdxSize = newIdxSize shl 1

        if (newVtxSize != vtxSize || newIdxSize != idxSize) {

            vtxSize = newVtxSize
            idxSize = newIdxSize

            vtxBuffer.destroy()
            vtxBuffer = bufferBig(vtxSize)
            idxBuffer.destroy()
            idxBuffer = intBufferBig(idxSize / Int.BYTES)

            glBindVertexArray(vaoName[0])

            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
            glBufferData(GL_ARRAY_BUFFER, vtxSize.L, null, GL_STREAM_DRAW)
            glEnableVertexAttribArray(semantic.attr.POSITION)
            glEnableVertexAttribArray(semantic.attr.TEX_COORD)
            glEnableVertexAttribArray(semantic.attr.COLOR)

            glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
            glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size.L)
            glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size.L)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize.L, null, GL_STREAM_DRAW)

            glBindVertexArray(0)

            if (glGetError() != GL.GL_NO_ERROR) throw Error("render")

            if(DEBUG) println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    class ProgramA(gl: GL3, shader: String) : Program(gl, shader) {
        val mat = gl.glGetUniformLocation(name, "mat")

        init {
            with(gl) {
                glUseProgram(name)
                glUniform1i(glGetUniformLocation(name, "Texture"), semantic.sampler.DIFFUSE)
                glUseProgram(0)
            }
        }
    }

    /** Build texture atlas */
    private fun GL3.createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = IO.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        glGenTextures(1, fontTexture)
        glBindTexture(GL_TEXTURE_2D, fontTexture[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

        // Store our identifier
        IO.fonts.texId = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        if (glGetError() != GL_NO_ERROR) throw Error("createFontsTexture")

        return true
    }

    /** This is the main rendering function that you have to implement and provide to ImGui (via setting up
     *  'RenderDrawListsFn' in the ImGuiIO structure)
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL
     *  state explicitly, in order to be able to run within any OpenGL engine that doesn't do so.
     *  If text or lines are blurry when integrating ImGui in your engine: in your Render function, try translating your
     *  projection matrix by (0.5f,0.5f) or (0.375f,0.375f) */
    fun renderDrawLists(drawData: DrawData) = with(gl) {

        /** Avoid rendering when minimized, scale coordinates for retina displays
         *  (screen coordinates != framebuffer coordinates) */
        val fbSize = IO.displaySize * IO.displayFramebufferScale
        if (fbSize equal 0) return
        drawData.scaleClipRects(IO.displayFramebufferScale)

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastSampler = glGetInteger(GL_SAMPLER_BINDING)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL2ES3.GL_VERTEX_ARRAY_BINDING)
        val lastPolygonMode = glGetInteger(GL_POLYGON_MODE)
        val lastViewport = Vec4i(buf.apply { glGetIntegerv(GL_VIEWPORT, asIntBuffer()) })
        val lastScissorBox = Vec4i(buf.apply { glGetIntegerv(GL_SCISSOR_BOX, asIntBuffer()) })
        val lastBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val lastBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val lastBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val lastBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val lastBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        val lastBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        val lastEnableBlend = glIsEnabled(GL_BLEND)
        val lastEnableCullFace = glIsEnabled(GL_CULL_FACE)
        val lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_SCISSOR_TEST)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        // Setup viewport, orthographic projection matrix
        glViewport(0, 0, fbSize.x, fbSize.y)
        val ortho = glm.ortho(mat, 0f, IO.displaySize.x.f, IO.displaySize.y.f, 0f)
        glUseProgram(program.name)
        glUniformMatrix4fv(program.mat, 1, false, (ortho to buf).asFloatBuffer())

        checkSize(drawData.cmdLists)

        glBindVertexArray(vaoName[0])
        glBindSampler(semantic.sampler.DIFFUSE, 0) // Rely on combined texture/sampler state.

        for (cmdList in drawData.cmdLists) {

            cmdList.vtxBuffer.forEachIndexed { i, v ->
                val offset = i * DrawVert.size
                v.pos.to(vtxBuffer, offset)
                v.uv.to(vtxBuffer, offset + Vec2.size)
                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
            }
            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
            glBufferSubData(GL_ARRAY_BUFFER, 0, cmdList._vtxWritePtr * DrawVert.size.L, vtxBuffer)
            cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, cmdList._idxWritePtr * Int.BYTES.L, idxBuffer)

            var idxBufferOffset = 0L
            for (cmd in cmdList.cmdBuffer) {
                if (cmd.userCallback != null)
                    cmd.userCallback!!(cmdList, cmd)
                else {
                    glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
                    glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
                    glScissor(cmd.clipRect.x.i, fbSize.y - cmd.clipRect.w.i,
                            (cmd.clipRect.z - cmd.clipRect.x).i, (cmd.clipRect.w - cmd.clipRect.y).i)
                    glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, idxBufferOffset)
                }
                idxBufferOffset += cmd.elemCount * Int.BYTES
            }
        }

        if (glGetError() != GL_NO_ERROR) throw Error("createFontsTexture")

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindSampler(0, lastSampler)
        glActiveTexture(lastActiveTexture)
        glBindVertexArray(lastVertexArray)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementArrayBuffer)
        glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha)
        glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha)
        if (lastEnableBlend) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        if (lastEnableCullFace) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        if (lastEnableDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (lastEnableScissorTest) glEnable(GL_SCISSOR_TEST) else glDisable(GL_SCISSOR_TEST)
        glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode)
        glViewport(lastViewport.x, lastViewport.y, lastViewport.z, lastViewport.w)
        glScissor(lastScissorBox.x, lastScissorBox.y, lastScissorBox.z, lastScissorBox.w)
    }

    private object mouseCallback : MouseListener {

        override fun mouseReleased(e: MouseEvent) {
            if (e.button in MouseEvent.BUTTON1..MouseEvent.BUTTON3)
                mouseJustPressed[e.button.i - 1] = false
        }

        override fun mouseMoved(e: MouseEvent) {
            cursorPos.put(e.x, e.y)
        }

        override fun mouseEntered(e: MouseEvent) {}

        override fun mouseDragged(e: MouseEvent) {
            cursorPos.put(e.x, e.y)
        }

        override fun mouseClicked(e: MouseEvent) {}

        override fun mouseExited(e: MouseEvent) {}

        override fun mousePressed(e: MouseEvent) {
            if (e.button in MouseEvent.BUTTON1..MouseEvent.BUTTON3)
                mouseJustPressed[e.button.i - 1] = true
        }

        override fun mouseWheelMoved(e: MouseEvent) {
            mouseWheel += e.rotation[1] // Use fractional mouse wheel.
        }
    }

    private object keyCallback : KeyListener {
        //        (void) mods // Modifiers are not reliable across systems
        override fun keyPressed(e: KeyEvent) = with(IO) {
            if (e.keyCode <= keysDown.size) keysDown[e.keyCode.i] = true
            if (e.keyCode == KeyEvent.VK_WINDOWS) keySuper = true
            keyCtrl = e.isControlDown
            keyShift = e.isShiftDown
            keyAlt = e.isAltDown
        }

        override fun keyReleased(e: KeyEvent) = with(IO) {
            if (e.keyCode <= keysDown.size) keysDown[e.keyCode.i] = false
            if (e.keyCode == KeyEvent.VK_WINDOWS) keySuper = false
            keyCtrl = e.isControlDown
            keyShift = e.isShiftDown
            keyAlt = e.isAltDown
        }
    }

    fun shutdown(gl: GL3) {
        invalidateDeviceObjects(gl)
        window.removeMouseListener(mouseCallback)
        window.removeKeyListener(keyCallback)
        ImGui.shutdown()
    }

    private fun invalidateDeviceObjects(gl: GL3) = with(gl) {

        glDeleteVertexArrays(1, vaoName)
        glDeleteBuffers(Buffer.MAX, bufferName)

        if (::program.isInitialized) glDeleteProgram(program.name)

        if (fontTexture[0] >= 0) {
            glDeleteTextures(1, fontTexture)
            IO.fonts.texId = -1
            fontTexture[0] = -1
        }
    }
}