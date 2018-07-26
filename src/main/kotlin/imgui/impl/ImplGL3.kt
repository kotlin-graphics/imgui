package imgui.impl

import glm_.*
import glm_.buffer.bufferBig
import glm_.buffer.cap
import glm_.buffer.free
import glm_.buffer.intBufferBig
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import gln.*
import gln.buffer.BufferTarget
import gln.buffer.Usage
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
import imgui.impl.LwjglGlfw.charCallback
import imgui.impl.LwjglGlfw.keyCallback
import imgui.impl.LwjglGlfw.mouseButtonCallback
import imgui.impl.LwjglGlfw.scrollCallback
import imgui.impl.windowsIme.imeListner
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL33.GL_SAMPLER_BINDING
import org.lwjgl.opengl.GL33.glBindSampler
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.glfw.*
import uno.glfw.GlfwWindow.CursorStatus

object ImplGL3 {

    var program = -1
    var matUL = -1

    fun createDeviceObjects(): Boolean {

        // Backup GL state
        // we have to save also program since we do the uniform mat and texture setup once here
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
            glBufferData(BufferTarget.Array, vtxSize, Usage.StreamDraw)
            glEnableVertexAttribArray(semantic.attr.POSITION)
            glEnableVertexAttribArray(semantic.attr.TEX_COORD)
            glEnableVertexAttribArray(semantic.attr.COLOR)

            glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
            glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
            glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferData(BufferTarget.ElementArray, idxSize, Usage.StreamDraw)
        }

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindVertexArray(lastVertexArray)

        return checkError("createDeviceObject")
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
        if (fbSize anyLessThanEqual 0) return
        drawData scaleClipRects io.displayFramebufferScale

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastSampler = glGetInteger(GL_SAMPLER_BINDING)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
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
            glBufferSubData(BufferTarget.Array, 0, cmdList._vtxWritePtr * DrawVert.size, vtxBuffer)
            cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
            glBufferSubData(BufferTarget.ElementArray, 0, cmdList._idxWritePtr * Int.BYTES, idxBuffer)

            var idxBufferOffset = 0L
            for (cmd in cmdList.cmdBuffer) {
                val cb = cmd.userCallback
                if (cb != null)
                    cb(cmdList, cmd)
                else {
                    glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
                    glScissor(cmd.clipRect.x.i, fbSize.y - cmd.clipRect.w.i, (cmd.clipRect.z - cmd.clipRect.x).i, (cmd.clipRect.w - cmd.clipRect.y).i)
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
                glBufferData(BufferTarget.Array, vtxSize, Usage.StreamDraw)
                glEnableVertexAttribArray(semantic.attr.POSITION)
                glEnableVertexAttribArray(semantic.attr.TEX_COORD)
                glEnableVertexAttribArray(semantic.attr.COLOR)

                glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
                glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
                glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.Element])
                glBufferData(BufferTarget.ElementArray, idxSize, Usage.StreamDraw)
            }

            checkError("checkSize")

            if (DEBUG) println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    fun destroyDeviceObjects() {

        glDeleteVertexArrays(vaoName)
        glDeleteBuffers(bufferName)

        if (program >= 0) glDeleteProgram(program)

        destroyFontsTexture()
    }

    private fun destroyFontsTexture() {
        if (fontTexture[0] != 0) {
            glDeleteTextures(fontTexture)
            io.fonts.texId = 0
            fontTexture[0] = 0
        }
    }
}