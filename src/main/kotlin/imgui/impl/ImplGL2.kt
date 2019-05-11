package imgui.impl

import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4b
import gln.glGetVec4i
import gln.glScissor
import gln.glViewport
import gln.glf.semantic
import gln.objects.GlProgram
import gln.texture.glBindTexture
import gln.uniform.glUniform
import gln.vertexArray.GlVertexArray
import imgui.*
import imgui.ImGui.io
import kool.*
import org.lwjgl.opengl.GL13C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL21C.*

class ImplGL2 : GLInterface {

    var program = GlProgram(0)
    var matUL = -1

    val buffers = IntBuffer<Buffer>()

    init {
        io.backendRendererName = "imgui_impl_opengl2"
    }

    override fun shutdown() = destroyDeviceObjects()

    override fun newFrame() {
        if (fontTexture[0] == 0)
            createDeviceObjects()
    }

    fun setupRenderState(drawData: DrawData, fbWidth: Int, fbHeight: Int) {

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_SCISSOR_TEST)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        // Setup viewport, orthographic projection matrix
        // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right).
        // DisplayMin is typically (0,0) for single viewport apps.
        glViewport(0, 0, fbWidth, fbHeight)
        val orthoProjection = glm.ortho(mat, 0f, io.displaySize.x.f, io.displaySize.y.f, 0f)
        glUseProgram(program.i)
        glUniform(matUL, orthoProjection)

        if (resizeIfNeeded(drawData.cmdLists)) {
            // Bind vertex/index buffers and setup attributes for ImDrawVert [JVM] buffers are new
            glBindBuffer(GL_ARRAY_BUFFER, buffers[Buffer.Vertex])
            glBufferData(GL_ARRAY_BUFFER, vtxSize.L, GL_STREAM_DRAW)

            glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, DrawVert.size, 0)
            glVertexAttribPointer(semantic.attr.TEX_COORD, Vec2.length, GL_FLOAT, false, DrawVert.size, Vec2.size.L)
            glVertexAttribPointer(semantic.attr.COLOR, Vec4b.length, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size.L)

            glEnableVertexAttribArray(semantic.attr.POSITION)
            glEnableVertexAttribArray(semantic.attr.TEX_COORD)
            glEnableVertexAttribArray(semantic.attr.COLOR)

            glBindBuffer(GL_ARRAY_BUFFER, 0)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[Buffer.Element])
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize.L, GL_STREAM_DRAW)

            if (DEBUG)
                println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    /** OpenGL2 Render function.
     *  (this used to be set in io.RenderDrawListsFn and called by ImGui::Render(), but you can now call this directly from your main loop)
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL state explicitly,
     *  in order to be able to run within any OpenGL engine that doesn't do so. */
    override fun renderDrawData(drawData: DrawData) {

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
        if (fbWidth == 0 || fbHeight == 0) return

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
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

        // Setup desired GL state
        setupRenderState(drawData, fbWidth, fbHeight)

        // Will project scissor/clipping rectangles into framebuffer space
        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        for (cmdList in drawData.cmdLists) {

            // Upload vertex/index buffers
            cmdList.vtxBuffer.forEachIndexed { i, v ->
                val offset = i * DrawVert.size
                v.pos.to(vtxBuffer, offset)
                v.uv.to(vtxBuffer, offset + Vec2.size)
                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
            }
            cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
            glBindBuffer(GL_ARRAY_BUFFER, buffers[Buffer.Vertex])
            nglBufferSubData(GL_ARRAY_BUFFER, 0, cmdList._vtxWritePtr * DrawVert.size.L, vtxBuffer.adr)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[Buffer.Element])
            nglBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, cmdList._idxWritePtr * Int.BYTES.L, idxBuffer.adr)

            var idxBufferOffset = 0L

            for (cmd in cmdList.cmdBuffer) {

                val userCB = cmd.userCallback
                if (userCB != null) {
                    // User callback, registered via ImDrawList::AddCallback()
                    // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                    if (cmd.resetRenderState)
                        setupRenderState(drawData, fbWidth, fbHeight)
                    else
                        userCB(cmdList, cmd)
                } else {
                    // Project scissor/clipping rectangles into framebuffer space
                    val clipRectX = (cmd.clipRect.x - clipOff.x) * clipScale.x
                    val clipRectY = (cmd.clipRect.y - clipOff.y) * clipScale.y
                    val clipRectZ = (cmd.clipRect.z - clipOff.x) * clipScale.x
                    val clipRectW = (cmd.clipRect.w - clipOff.y) * clipScale.y

                    if (clipRectX < fbWidth && clipRectY < fbHeight && clipRectZ >= 0f && clipRectW >= 0f) {
                        // Apply scissor/clipping rectangle
                        glScissor(clipRectX.i, (fbHeight - clipRectW).i, (clipRectZ - clipRectX).i, (clipRectW - clipRectY).i)

                        // Bind texture, Draw
                        glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
                        glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, idxBufferOffset)
                    }
                }
                idxBufferOffset += cmd.elemCount * DrawIdx.BYTES
            }
        }

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glActiveTexture(lastActiveTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)
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

    override fun createDeviceObjects(): Boolean {

        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

        program = createProgram()
        program.used {
            matUL = "ProjMtx".uniform
            "Texture".unit = semantic.sampler.DIFFUSE
        }

        glGenBuffers(buffers)

        glBindBuffer(GL_ARRAY_BUFFER, buffers[Buffer.Vertex])
        glBufferData(GL_ARRAY_BUFFER, vtxSize.L, GL_STREAM_DRAW)
        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.TEX_COORD)
        glEnableVertexAttribArray(semantic.attr.COLOR)

        glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
        glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size.toLong())
        glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2L * Vec2.size)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[Buffer.Element])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxSize.L, GL_STREAM_DRAW)

        createFontsTexture()

        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementArrayBuffer)

        return true
    }

    /** Build texture atlas */
    override fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = ImGui.io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        glGenTextures(fontTexture)
        glBindTexture(GL_TEXTURE_2D, fontTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

        // Store our identifier
        ImGui.io.fonts.texId = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return true
    }

    override fun destroyFontsTexture() {
        if (fontTexture[0] != 0) {
            glDeleteTextures(fontTexture)
            ImGui.io.fonts.texId = 0
            fontTexture[0] = 0
        }
    }

    override fun destroyDeviceObjects() {
        glDeleteBuffers(buffers)

        if (program.i >= 0) glDeleteProgram(program.i)

        destroyFontsTexture()
    }
}