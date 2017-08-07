package imgui.impl

import glm_.*
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import imgui.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import uno.buffer.byteBufferBig
import uno.buffer.destroy
import uno.buffer.intBufferBig
import uno.glf.semantic
import uno.glfw.GlfwWindow
import uno.glfw.glfw
import uno.gln.*
import uno.glsl.Program


object LwjglGL3 {

    lateinit var window: GlfwWindow
    var time = 0.0
    val mousePressed = Array(3, { false })
    var mouseWheel = 0f

    object Buffer {
        val Vertex = 0
        val Element = 1
        val MAX = 2
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vaoName = intBufferBig(1)
    lateinit var program: ProgramA
    val fontTexture = intBufferBig(1)

    val mat = Mat4()

    fun init(window: GlfwWindow, installCallbacks: Boolean): Boolean {

        this.window = window

        with(IO) {
            // Keyboard mapping. ImGui will use those indices to peek into the io.KeyDown[] array.
            keyMap[Key.Tab] = GLFW_KEY_TAB
            keyMap[Key.LeftArrow] = GLFW_KEY_LEFT
            keyMap[Key.RightArrow] = GLFW_KEY_RIGHT
            keyMap[Key.UpArrow] = GLFW_KEY_UP
            keyMap[Key.DownArrow] = GLFW_KEY_DOWN
            keyMap[Key.PageUp] = GLFW_KEY_PAGE_UP
            keyMap[Key.PageDown] = GLFW_KEY_PAGE_DOWN
            keyMap[Key.Home] = GLFW_KEY_HOME
            keyMap[Key.End] = GLFW_KEY_END
            keyMap[Key.Delete] = GLFW_KEY_DELETE
            keyMap[Key.Backspace] = GLFW_KEY_BACKSPACE
            keyMap[Key.Enter] = GLFW_KEY_ENTER
            keyMap[Key.Escape] = GLFW_KEY_ESCAPE
            keyMap[Key.A] = GLFW_KEY_A
            keyMap[Key.C] = GLFW_KEY_C
            keyMap[Key.V] = GLFW_KEY_V
            keyMap[Key.X] = GLFW_KEY_X
            keyMap[Key.Y] = GLFW_KEY_Y
            keyMap[Key.Z] = GLFW_KEY_Z

            /* Alternatively you can set this to NULL and call ImGui::GetDrawData() after ImGui::Render() to get the
               same ImDrawData pointer.             */
            renderDrawListsFn = this@LwjglGL3::renderDrawLists
        }

        if (installCallbacks) {
            window.mouseButtonCallback = mouseButtonCallback
            window.scrollCallback = scrollCallback
            window.keyCallback = keyCallback
            window.charCallback = charCallback // TODO check if used (jogl doesnt have)
        }

        return true
    }

    var vtxSize = 1 shl 5 // 32768
    var idxSize = 1 shl 6 // 65536
    var vtxBuffer = byteBufferBig(vtxSize)
    var idxBuffer = intBufferBig(idxSize / Int.BYTES)


    fun newFrame() {

        if (fontTexture[0] == 0)
            createDeviceObjects()

        // Setup display size (every frame to accommodate for window resizing)
        IO.displaySize put window.size
        IO.displayFramebufferScale.x = if (window.size.x > 0) window.framebufferSize.x / window.size.x.f else 0f
        IO.displayFramebufferScale.y = if (window.size.y > 0) window.framebufferSize.y / window.size.y.f else 0f

        // Setup time step
        val currentTime = glfw.time
        IO.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        time = currentTime

        // Setup inputs
        // (we already got mouse wheel, keyboard keys & characters from glfw callbacks polled in glfwPollEvents())
        // Mouse position in screen coordinates (set to -1,-1 if no mouse / on another screen, etc.)
        if (window.focused)
            IO.mousePos put window.cursorPos
        else
            IO.mousePos put -1

        repeat(3) {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            IO.mouseDown[it] = mousePressed[it] || window.mouseButton(it) != 0
            mousePressed[it] = false
        }

        IO.mouseWheel = mouseWheel
        mouseWheel = 0f

        // Hide OS mouse cursor if ImGui is drawing it
        window.cursor = if (IO.mouseDrawCursor) GlfwWindow.Cursor.Hidden else GlfwWindow.Cursor.Normal

        // Start the frame
        ImGui.newFrame()
    }

    private fun createDeviceObjects(): Boolean {

        // Backup GL state
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)

        program = ProgramA("shader")

        glGenBuffers(bufferName)

        glGenVertexArrays(vaoName)
        withVertexArray(vaoName) {
            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
            glBufferData(GL_ARRAY_BUFFER, vtxSize, GL_STREAM_DRAW)
            glEnableVertexAttribArray(semantic.attr.POSITION)
            glEnableVertexAttribArray(semantic.attr.TEX_COORD)
            glEnableVertexAttribArray(semantic.attr.COLOR)

            glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
            glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
            glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize, GL_STREAM_DRAW)
        }

        checkError("createDeviceObject")

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindVertexArray(lastVertexArray)

        return true
    }

    private fun checkSize(draws: ArrayList<DrawList>) {

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
            vtxBuffer = byteBufferBig(vtxSize)
            idxBuffer.destroy()
            idxBuffer = intBufferBig(idxSize / Int.BYTES)

            withVertexArray(vaoName) {

                glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
                glBufferData(GL_ARRAY_BUFFER, vtxSize, GL_STREAM_DRAW)
                glEnableVertexAttribArray(semantic.attr.POSITION)
                glEnableVertexAttribArray(semantic.attr.TEX_COORD)
                glEnableVertexAttribArray(semantic.attr.COLOR)

                glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
                glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
                glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize, GL_STREAM_DRAW)
            }

            checkError("render")

            println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    class ProgramA(shader: String) : Program("$shader.vert", "$shader.frag") {
        val mat = glGetUniformLocation(name, "mat")

        init {
            usingProgram(name) { "Texture".unit = semantic.sampler.DIFFUSE }
        }
    }

    /** Build texture atlas */
    private fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = IO.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        initTexture2d(fontTexture) {
            minFilter = linear
            magFilter = linear
            image(GL_RGBA, size, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
        }

        // Store our identifier
        IO.fonts.texId = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        checkError("createFontsTexture")

        return true
    }

    /** This is the main rendering function that you have to implement and provide to ImGui (via setting up
     *  'RenderDrawListsFn' in the ImGuiIO structure)
     *  If text or lines are blurry when integrating ImGui in your engine:
     *      - in your Render function, try translating your projection matrix by (0.5f,0.5f) or (0.375f,0.375f) */
    fun renderDrawLists(drawData: DrawData) {

        /** Avoid rendering when minimized, scale coordinates for retina displays
         *  (screen coordinates != framebuffer coordinates) */
        val fbSize = IO.displaySize * IO.displayFramebufferScale
        if (fbSize.x == 0 || fbSize.y == 0) return
        drawData.scaleClipRects(IO.displayFramebufferScale)

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val lastBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val lastBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val lastBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val lastBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        val lastBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        val lastViewport = glGetVec4i(GL_VIEWPORT)
        val lastScissorBox = glGetVec4i(GL_SCISSOR_BOX)
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

        // Setup viewport, orthographic projection matrix
        glViewport(fbSize)
        val ortho = glm.ortho(mat, 0f, IO.displaySize.x.f, IO.displaySize.y.f, 0f)
        glUseProgram(program)
        glUniform(program.mat, ortho)

        checkSize(drawData.cmdLists)

        glBindVertexArray(vaoName)

        for (cmdList in drawData.cmdLists) {

            cmdList.vtxBuffer.forEachIndexed { i, v ->
                val offset = i * DrawVert.size
                v.pos.to(vtxBuffer, offset)
                v.uv.to(vtxBuffer, offset + Vec2.size)
                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
            }
            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
            glBufferSubData(GL_ARRAY_BUFFER, 0, vtxBuffer)
            cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, idxBuffer)

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

        checkError("render")

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
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
        glViewport(lastViewport)
        glScissor(lastScissorBox)
    }

    private val mouseButtonCallback = { button: Int, action: Int, _: Int ->
        if (action == GLFW_PRESS && button in 0 .. 2)
            mousePressed[button] = true
    }

    private val scrollCallback = { _: Double, yOffset: Double ->
        mouseWheel += yOffset.f // Use fractional mouse wheel, 1.0 unit 5 lines.
    }

    private val keyCallback = { key: Int, _: Int, action: Int, _: Int ->
        with(IO) {
            if (key in keysDown.indices)
                if (action == GLFW_PRESS)
                    keysDown[key] = true
                else if (action == GLFW_RELEASE)
                    keysDown[key] = false

//        (void) mods // Modifiers are not reliable across systems
            keyCtrl = keysDown[GLFW_KEY_LEFT_CONTROL] || keysDown[GLFW_KEY_RIGHT_CONTROL]
            keyShift = keysDown[GLFW_KEY_LEFT_SHIFT] || keysDown[GLFW_KEY_RIGHT_SHIFT]
            keyAlt = keysDown[GLFW_KEY_LEFT_ALT] || keysDown[GLFW_KEY_RIGHT_ALT]
            keySuper = keysDown[GLFW_KEY_LEFT_SUPER] || keysDown[GLFW_KEY_RIGHT_SUPER]
        }
    }

    private val charCallback = { c: Int -> if (c in 1..65535) IO.addInputCharacter(c.c) }

    fun shutdown() {
        invalidateDeviceObjects()
        ImGui.shutdown()
    }

    private fun invalidateDeviceObjects() {

        glDeleteVertexArrays(vaoName)
        glDeleteBuffers(bufferName)

        glDeleteProgram(program)

        if (fontTexture[0] != 0) {
            glDeleteTextures(fontTexture)
            IO.fonts.texId = 0
        }
    }
}