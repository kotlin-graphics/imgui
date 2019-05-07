package imgui.impl

import gli_.gl
import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4ub
import gln.*
import gln.BufferTarget.Companion.ARRAY
import gln.BufferTarget.Companion.ELEMENT_ARRAY
import gln.ShaderType.Companion.FRAGMENT_SHADER
import gln.ShaderType.Companion.VERTEX_SHADER
import gln.TextureTarget.Companion._2D
import gln.Usage.Companion.STREAM_DRAW
import gln.glf.semantic
import gln.objects.*
import gln.texture.TexFilter
import gln.uniform.glUniform
import gln.vertexArray.GlVertexArray
import gln.vertexArray.glVertexAttribPointer
import imgui.*
import imgui.ImGui.io
import imgui.imgui.g
import kool.*
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.system.Platform
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImplGL3 : LwjglRendererI {
    companion object {
        fun renderDrawData(drawData: DrawData) = LwjglGlfw.instance.renderDrawData(drawData)
    }

    val hasGL33 = GL.getCapabilities().OpenGL33

    var program = GlProgram(0)
    var matUL = -1
    var CLIP_ORIGIN = false && Platform.get() != Platform.MACOSX

    val mouseJustPressed = BooleanArray(5)
    val buffers = GlBuffers<Buffer>()
    var vao = GlVertexArray()
    var fontTexture = GlTexture()

    override fun createDeviceObjects(): Boolean {

        // this shall be in init, but since we dont have it because we do differently about the glsl version we do this here
        io.backendRendererName = "imgui impl opengl3"

        // Backup GL state
        // we have to save also program since we do the uniform mat and texture setup once here
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArrayObject = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

        program = GlProgram.create().apply {
            val vertHandle = GlShader.createFromSource(VERTEX_SHADER, vertexShader)
            val fragHandle = GlShader.createFromSource(FRAGMENT_SHADER, fragmentShader)
            attach(vertHandle)
            attach(fragHandle)
            bindAttribLocation(semantic.attr.POSITION, "Position")
            bindAttribLocation(semantic.attr.TEX_COORD, "UV")
            bindAttribLocation(semantic.attr.COLOR, "Color")
            bindFragDataLocation(semantic.frag.COLOR, "outColor")
            link()
            detach(vertHandle)
            detach(fragHandle)
            vertHandle.delete()
            fragHandle.delete()
        }
        program.use {
            matUL = "mat".uniform
            "Texture".unit = semantic.sampler.DIFFUSE
        }

        GlVertexArray.gen(::vao).bound {

            buffers.gen {

                Buffer.Vertex.bound(ARRAY) {

                    data(vtxSize, STREAM_DRAW)

                    glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, DrawVert.size, 0)
                    glVertexAttribPointer(semantic.attr.TEX_COORD, Vec2.length, GL_FLOAT, false, DrawVert.size, Vec2.size)
                    glVertexAttribPointer(semantic.attr.COLOR, Vec4ub.length, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)
                }
                glEnableVertexAttribArray(semantic.attr.POSITION)
                glEnableVertexAttribArray(semantic.attr.TEX_COORD)
                glEnableVertexAttribArray(semantic.attr.COLOR)

                Buffer.Element.bind(ELEMENT_ARRAY) {
                    data(idxSize, STREAM_DRAW)
                }
            }
        }

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)
        glBindVertexArray(lastVertexArrayObject)

        return checkError("createDeviceObject")
    }

    /** Build texture atlas */
    private fun createFontsTexture(): Boolean {

        if (io.fonts.isBuilt)
            return true

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        GlTexture.gen(::fontTexture).bound(_2D) {
            minMagFilter = TexFilter.LINEAR
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            image(gl.InternalFormat.RGBA8_UNORM, size, gl.ExternalFormat.RGBA, gl.TypeFormat.U8, pixels)
        }

        // Store our identifier
        io.fonts.texId = fontTexture.name

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return checkError("createFontsTexture")
    }

    /** OpenGL3 Render function.
     *  (this used to be set in io.renderDrawListsFn and called by ImGui::render(), but you can now call this directly
     *  from your main loop)
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL
     *  state explicitly, in order to be able to run within any OpenGL engine that doesn't do so.   */
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
        val lastSampler = if (hasGL33) glGetInteger(GL33C.GL_SAMPLER_BINDING) else 0
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
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
        val clipOriginLowerLeft = when {
            CLIP_ORIGIN && glGetInteger(GL45C.GL_CLIP_ORIGIN) == GL_UPPER_LEFT -> false // Support for GL 4.5's glClipControl(GL_UPPER_LEFT)
            else -> true
        }

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_SCISSOR_TEST)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        // Setup viewport, orthographic projection matrix
        glViewport(0, 0, fbWidth, fbHeight)
        val ortho = glm.ortho(mat, 0f, io.displaySize.x.f, io.displaySize.y.f, 0f)
        glUseProgram(program.i)
        glUniform(matUL, ortho)

        checkSize(drawData.cmdLists)

        vao.bind()
        if (hasGL33)
            GL33C.glBindSampler(semantic.sampler.DIFFUSE, 0) // Rely on combined texture/sampler state.

        // Will project scissor/clipping rectangles into framebuffer space
        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        for (cmdList in drawData.cmdLists) {

            cmdList.vtxBuffer.forEachIndexed { i, v ->
                val offset = i * DrawVert.size
                v.pos.to(vtxBuffer, offset)
                v.uv.to(vtxBuffer, offset + Vec2.size)
                vtxBuffer.putInt(offset + Vec2.size * 2, v.col)
//                val r = v.col ushr 24
//                val g = (v.col ushr 16) and 0xff
//                val b = (v.col ushr 8) and 0xff
//                val a = v.col and 0xff
//                println("vertex[$i] = pos(${v.pos.x}, ${v.pos.y}), uv(${v.uv.x}, ${v.uv.y}), col($r, $g, $b, $a)")
            }
            // Upload vertex/index buffers
            buffers {
                Buffer.Vertex.bind(ARRAY) {
                    subData(0, cmdList._vtxWritePtr * DrawVert.size, vtxBuffer)
                }
                cmdList.idxBuffer.forEachIndexed { i, idx -> idxBuffer[i] = idx }
                Buffer.Element.bind(ELEMENT_ARRAY) {
                    subData(0, cmdList._idxWritePtr * Int.BYTES, idxBuffer)
                }
            }
            var idxBufferOffset = 0L
            for (cmd in cmdList.cmdBuffer) {
                cmd.userCallback?.invoke(cmdList, cmd) ?: run {
                    // Project scissor/clipping rectangles into framebuffer space
                    val clipRectX = (cmd.clipRect.x - clipOff.x) * clipScale.x
                    val clipRectY = (cmd.clipRect.y - clipOff.y) * clipScale.y
                    val clipRectZ = (cmd.clipRect.z - clipOff.x) * clipScale.x
                    val clipRectW = (cmd.clipRect.w - clipOff.y) * clipScale.y

                    if (clipRectX < fbWidth && clipRectY < fbHeight && clipRectZ >= 0f && clipRectW >= 0f) {
                        // Apply scissor/clipping rectangle
                        if (clipOriginLowerLeft)
                            glScissor(clipRectX.i, (fbHeight - clipRectW).i, (clipRectZ - clipRectX).i, (clipRectW - clipRectY).i)
                        else
                            glScissor(clipRectX.i, clipRectY.i, clipRectZ.i, clipRectW.i) // Support for GL 4.5 rarely used glClipControl(GL_UPPER_LEFT)

                        // Bind texture, Draw
                        glBindTexture(GL_TEXTURE_2D, cmd.textureId!!)
                        glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, idxBufferOffset)
                    }
                }
                idxBufferOffset += cmd.elemCount * Int.BYTES
            }
        }

        checkError("render")

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        if (hasGL33)
            GL33C.glBindSampler(0, lastSampler)
        glActiveTexture(lastActiveTexture)
        glBindVertexArray(lastVertexArray)
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

    private fun debugSave(fbWidth: Int, fbHeight: Int) {
        if (g.frameCount % 60 == 0) {

            glReadBuffer(GL11C.GL_BACK)
            // no more alignment problems by texture updating
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)  // upload
            glPixelStorei(GL_PACK_ALIGNMENT, 1)  // download

            val colorBufferImg = BufferedImage(fbWidth, fbHeight, BufferedImage.TYPE_INT_ARGB)
            val graphicsColor = colorBufferImg.graphics

            val buffer = ByteBuffer(fbWidth * fbHeight * 4)
            glReadPixels(0, 0, fbWidth, fbHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

            var i = 0
            for (h in 0 until fbHeight) {
                for (w in 0 until fbWidth) {

                    val iR = buffer[i + 0].toUnsignedInt
                    val iG = buffer[i + 1].toUnsignedInt
                    val iB = buffer[i + 2].toUnsignedInt
                    val iA = buffer[i + 3].toInt() and 0xff

                    graphicsColor.color = Color(iR, iG, iB, iA)
                    graphicsColor.fillRect(w, fbHeight - h - 1, 1, 1) // height - h is for flipping the image
                    i += 4
                }
            }

            val imgNameColor = "whate_(${System.currentTimeMillis()}).png"
            ImageIO.write(colorBufferImg, "png", File("""C:\Users\gbarbieri\Pictures\$imgNameColor"""))
            graphicsColor.dispose()
            buffer.free()
        }
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
            vtxBuffer = ByteBuffer(vtxSize)
            idxBuffer.free()
            idxBuffer = IntBuffer(idxSize / Int.BYTES)

            val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
            val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
            val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

            vao.bound {

                buffers {

                    Buffer.Vertex.bound(ARRAY) {

                        data(vtxSize, STREAM_DRAW)

                        glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
                        glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
                        glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)
                    }
                    glEnableVertexAttribArray(semantic.attr.POSITION)
                    glEnableVertexAttribArray(semantic.attr.TEX_COORD)
                    glEnableVertexAttribArray(semantic.attr.COLOR)

                    Buffer.Element.bind(ELEMENT_ARRAY) {
                        data(idxSize, STREAM_DRAW)
                    }
                }
            }

            glBindVertexArray(lastVertexArray)
            glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)

            checkError("checkSize")

            if (DEBUG) println("new buffers sizes, vtx: $vtxSize, idx: $idxSize")
        }
    }

    override fun destroyDeviceObjects() {

        vao.delete()
        buffers.delete()

        if (program.isValid) program.delete()

        destroyFontsTexture()
    }

    private fun destroyFontsTexture() {
        if (fontTexture.isValid) {
            fontTexture.delete()
            io.fonts.texId = 0
            fontTexture = GlTexture()
        }
    }
}