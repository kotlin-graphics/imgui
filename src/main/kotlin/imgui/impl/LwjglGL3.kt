package imgui.impl

import glm_.*
import glm_.buffer.bufferBig
import glm_.buffer.free
import glm_.buffer.intBufferBig
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import gln.*
import gln.buffer.glBufferData
import gln.buffer.glBufferSubData
import gln.glf.semantic
import gln.program.Program
import gln.program.usingProgram
import gln.texture.initTexture2d
import gln.uniform.glUniform
import gln.vertexArray.glBindVertexArray
import gln.vertexArray.glVertexAttribPointer
import gln.vertexArray.withVertexArray
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.mouseCursor
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.GL_SAMPLER_BINDING
import org.lwjgl.opengl.GL33.glBindSampler
import uno.glfw.GlfwWindow
import uno.glfw.glfw


object LwjglGL3 {

    lateinit var window: GlfwWindow
    var time = 0.0
    val mouseCursors = LongArray(MouseCursor.COUNT)

    var program = -1
    var matUL = -1

    fun init(window: GlfwWindow, installCallbacks: Boolean): Boolean {

        this.window = window

        with(io) {

            // Setup back-end capabilities flags
            backendFlags = backendFlags or BackendFlag.HasMouseCursors   // We can honor GetMouseCursor() values (optional)
            backendFlags = backendFlags or BackendFlag.HasSetMousePos    // We can honor io.WantSetMousePos requests (optional, rarely used)

            // Keyboard mapping. ImGui will use those indices to peek into the io.KeysDown[] array.
            keyMap[Key.Tab] = GLFW_KEY_TAB
            keyMap[Key.LeftArrow] = GLFW_KEY_LEFT
            keyMap[Key.RightArrow] = GLFW_KEY_RIGHT
            keyMap[Key.UpArrow] = GLFW_KEY_UP
            keyMap[Key.DownArrow] = GLFW_KEY_DOWN
            keyMap[Key.PageUp] = GLFW_KEY_PAGE_UP
            keyMap[Key.PageDown] = GLFW_KEY_PAGE_DOWN
            keyMap[Key.Home] = GLFW_KEY_HOME
            keyMap[Key.End] = GLFW_KEY_END
            keyMap[Key.Insert] = GLFW_KEY_INSERT
            keyMap[Key.Delete] = GLFW_KEY_DELETE
            keyMap[Key.Backspace] = GLFW_KEY_BACKSPACE
            keyMap[Key.Space] = GLFW_KEY_SPACE
            keyMap[Key.Enter] = GLFW_KEY_ENTER
            keyMap[Key.Escape] = GLFW_KEY_ESCAPE
            keyMap[Key.A] = GLFW_KEY_A
            keyMap[Key.C] = GLFW_KEY_C
            keyMap[Key.V] = GLFW_KEY_V
            keyMap[Key.X] = GLFW_KEY_X
            keyMap[Key.Y] = GLFW_KEY_Y
            keyMap[Key.Z] = GLFW_KEY_Z
        }

        /*  Load cursors
            FIXME: GLFW doesn't expose suitable cursors for ResizeAll, ResizeNESW, ResizeNWSE. 
            We revert to arrow cursor for those.         */
        mouseCursors[MouseCursor.Arrow.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.TextInput.i] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
        mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.ResizeNS.i] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR)
        mouseCursors[MouseCursor.ResizeEW.i] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR)
        mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)

        if (installCallbacks) installCallbacks()

        return true
    }

    fun installCallbacks() {
        window.mouseButtonCallback = mouseButtonCallback
        window.scrollCallback = scrollCallback
        window.keyCallback = keyCallback
        window.charCallback = charCallback // TODO check if used (jogl doesnt have)
        imeListner.install(window.handle)
    }

    fun newFrame() {

        if (fontTexture[0] < 0) createDeviceObjects()

        // Setup display size (every frame to accommodate for window resizing)
        io.displaySize put window.size
        io.displayFramebufferScale.x = if (window.size.x > 0) window.framebufferSize.x / window.size.x.f else 0f
        io.displayFramebufferScale.y = if (window.size.y > 0) window.framebufferSize.y / window.size.y.f else 0f

        // Setup time step
        val currentTime = glfwGetTime() // TODO move to uno
        io.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        time = currentTime

        /*  Setup inputs
            (we already got mouse wheel, keyboard keys & characters from glfw callbacks polled in glfwPollEvents())
            Mouse position in screen coordinates (set to -1,-1 if no mouse / on another screen, etc.)   */
        if (window.focused)
        // Set OS mouse position if requested (only used when ConfigFlags.NavEnableSetMousePos is enabled by user)
            if (io.wantSetMousePos)
                window.cursorPos = Vec2d(io.mousePos)
            else
                io.mousePos put window.cursorPos
        else
            io.mousePos put -Float.MAX_VALUE

        repeat(3) {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            io.mouseDown[it] = mouseJustPressed[it] || window.mouseButton(it) != 0
            mouseJustPressed[it] = false
        }

        // Update OS/hardware mouse cursor if imgui isn't drawing a software cursor
        if (io.configFlags hasnt ConfigFlag.NoMouseCursorChange && window.cursor != GlfwWindow.Cursor.Disabled) {
            val cursor = mouseCursor
            if (io.mouseDrawCursor || cursor == MouseCursor.None)
                window.cursor = GlfwWindow.Cursor.Hidden
            else {
                glfwSetCursor(window.handle, if (mouseCursors[cursor.i] != 0L) mouseCursors[cursor.i] else mouseCursors[MouseCursor.Arrow.i])
                window.cursor = GlfwWindow.Cursor.Normal
            }
        }

        // Gamepad navigation mapping [BETA]
        io.navInputs.fill(0f)
        if (io.configFlags has ConfigFlag.NavEnableGamepad) {
            // Update gamepad inputs
            val buttons = window.getJoystickButtons(GLFW_JOYSTICK_1)!!
            val buttonsCount = buttons.capacity()
            val axes = window.getJoystickAxes(GLFW_JOYSTICK_1)!!
            val axesCount = axes.capacity()
            fun mapButton(nav: NavInput, button: Int) {
                if (buttonsCount > button && buttons[button] == GLFW_PRESS.b)
                    io.navInputs[nav] = 1f
            }

            fun mapAnalog(nav: NavInput, axis: Int, v0: Float, v1: Float) {
                var v = if (axesCount > axis) axes[axis] else v0
                v = (v - v0) / (v1 - v0)
                if (v > 1f) v = 1f
                if (io.navInputs[nav] < v)
                    io.navInputs[nav] = v
            }

            mapButton(NavInput.Activate, 0)     // Cross / A
            mapButton(NavInput.Cancel, 1)     // Circle / B
            mapButton(NavInput.Menu, 2)     // Square / X
            mapButton(NavInput.Input, 3)     // Triangle / Y
            mapButton(NavInput.DpadLeft, 13)    // D-Pad Left
            mapButton(NavInput.DpadRight, 11)    // D-Pad Right
            mapButton(NavInput.DpadUp, 10)    // D-Pad Up
            mapButton(NavInput.DpadDown, 12)    // D-Pad Down
            mapButton(NavInput.FocusPrev, 4)     // L1 / LB
            mapButton(NavInput.FocusNext, 5)     // R1 / RB
            mapButton(NavInput.TweakSlow, 4)     // L1 / LB
            mapButton(NavInput.TweakFast, 5)     // R1 / RB
            mapAnalog(NavInput.LStickLeft, 0, -0.3f, -0.9f)
            mapAnalog(NavInput.LStickRight, 0, +0.3f, +0.9f)
            mapAnalog(NavInput.LStickUp, 1, +0.3f, +0.9f)
            mapAnalog(NavInput.LStickDown, 1, -0.3f, -0.9f)

            if (axesCount > 0 && buttonsCount > 0)
                io.backendFlags = io.backendFlags or BackendFlag.HasGamepad
            else
                io.backendFlags = io.backendFlags wo BackendFlag.HasGamepad
        }

        /*  Start the frame. This call will update the io.wantCaptureMouse, io.wantCaptureKeyboard flag that you can use
            to dispatch inputs (or not) to your application.         */
        ImGui.newFrame()
    }

    private fun createDeviceObjects(): Boolean {

        // Backup GL state
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)

        program = glCreateProgram()
        val vertHandle = Program.createShaderFromSource(vertexShader, GL_VERTEX_SHADER)
        val fragHandle = Program.createShaderFromSource(fragmentShader, GL_FRAGMENT_SHADER)
        glAttachShader(program, vertHandle)
        glAttachShader(program, fragHandle)
        glBindAttribLocation(program, semantic.attr.POSITION, "Position")
        glBindAttribLocation(program, semantic.attr.TEX_COORD, "UV")
        glBindAttribLocation(program, semantic.attr.COLOR, "Color")
        glBindFragDataLocation(program, semantic.frag.COLOR, "outColor")
        glLinkProgram(program)
        glDetachShader(program, vertHandle)
        glDetachShader(program, fragHandle)
        glDeleteShader(vertHandle)
        glDeleteShader(fragHandle)
        usingProgram(program) {
            matUL = "mat".uniform
            "Texture".unit = semantic.sampler.DIFFUSE
        }

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

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindVertexArray(lastVertexArray)

        return checkError("createDeviceObject")
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

            vtxBuffer.free()
            vtxBuffer = bufferBig(vtxSize)
            idxBuffer.free()
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

            checkError("checkSize")

            if (DEBUG) println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    /** Build texture atlas */
    private fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        initTexture2d(fontTexture) {
            minFilter = linear
            magFilter = linear
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            image(GL_RGBA, size, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
        }

        // Store our identifier
        io.fonts.texId = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return checkError("createFontsTexture")
    }

    /** OpenGL3 Render function.
     *  (this used to be set in io.renderDrawListsFn and called by ImGui::render(), but you can now call this directly
     *  from your main loop)
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL
     *  state explicitly, in order to be able to run within any OpenGL engine that doesn't do so.   */
    fun renderDrawData(drawData: DrawData) {

        /** Avoid rendering when minimized, scale coordinates for retina displays
         *  (screen coordinates != framebuffer coordinates) */
        val fbSize = io.displaySize * io.displayFramebufferScale
        if (fbSize equal 0) return
        drawData.scaleClipRects(io.displayFramebufferScale)

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastSampler = glGetInteger(GL_SAMPLER_BINDING)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastPolygonMode = glGetInteger(GL_POLYGON_MODE)
        val lastViewport = glGetVec4i(GL_VIEWPORT)
        val lastScissorBox = glGetVec4i(GL_SCISSOR_BOX)
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
        glViewport(fbSize)
        val ortho = glm.ortho(mat, 0f, io.displaySize.x.f, io.displaySize.y.f, 0f)
        glUseProgram(program)
        glUniform(matUL, ortho)

        checkSize(drawData.cmdLists)

        glBindVertexArray(vaoName)
        glBindSampler(semantic.sampler.DIFFUSE, 0) // Rely on combined texture/sampler state.

        for (cmdList in drawData.cmdLists) {

            cmdList.vtxBuffer.forEachIndexed { i, v ->
                val offset = i * DrawVert.size
                v.pos.to(vtxBuffer, offset)
                v.uv.to(vtxBuffer, offset + Vec2.size)
                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
            }
            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
            glBufferSubData(GL_ARRAY_BUFFER, 0, cmdList._vtxWritePtr * DrawVert.size, vtxBuffer)
            cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, cmdList._idxWritePtr * Int.BYTES, idxBuffer)

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
        glViewport(lastViewport)
        glScissor(lastScissorBox)
    }

    val mouseButtonCallback = { button: Int, action: Int, _: Int ->
        if (action == GLFW_PRESS && button in 0..2)
            mouseJustPressed[button] = true
    }

    val scrollCallback = { offset: Vec2d ->
        io.mouseWheelH += offset.x.f
        io.mouseWheel += offset.y.f
    }

    val keyCallback = { key: Int, _: Int, action: Int, _: Int ->
        with(io) {
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

    val charCallback = { c: Int -> if (c in 1..65535) io.addInputCharacter(c.c) }

    fun shutdown() {

        // Destroy GLFW mouse cursors
        mouseCursors.filter { it != 0L }.forEach(::glfwDestroyCursor)

        // Destroy OpenGL objects
        invalidateDeviceObjects()
    }

    private fun invalidateDeviceObjects() {

        glDeleteVertexArrays(vaoName)
        glDeleteBuffers(bufferName)

        if (program >= 0) glDeleteProgram(program)

        if (fontTexture[0] >= 0) {
            glDeleteTextures(fontTexture)
            io.fonts.texId = -1
            fontTexture[0] = -1
        }
    }
}