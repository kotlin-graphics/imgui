package imgui.impl.gl

import glm_.L
import glm_.d
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4b
import gln.*
import gln.glf.semantic
import gln.texture.glBindTexture
import imgui.DEBUG
import imgui.ImGui
import imgui.ImGui.io
import imgui.L
import imgui.impl.glfw.ImplGlfw
import imgui.internal.DrawData
import imgui.internal.DrawVert
import kool.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_MODULATE
import org.lwjgl.opengl.GL11.GL_TEXTURE_ENV
import org.lwjgl.opengl.GL11.GL_TEXTURE_ENV_MODE
import org.lwjgl.opengl.GL11.glGetTexEnvi
import org.lwjgl.opengl.GL11.glTexEnvi
import org.lwjgl.opengl.GL13C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL14C.GL_FUNC_ADD
import org.lwjgl.opengl.GL14C.glBlendEquation
import org.lwjgl.opengl.GL20C.*

class ImplGL2 : GLInterface {

    /** ~ImGui_ImplOpenGL2_Init */
    init {
        assert(io.backendRendererUserData == null) { "Already initialized a renderer backend!" }

        // Setup backend capabilities flags
        io.backendRendererName = "imgui_impl_opengl2"
    }

    override fun shutdown() {
        destroyDeviceObjects()
        io.backendRendererName = null
        io.backendRendererUserData = null
    }

    override fun newFrame() {
        assert(ImplGlfw.Companion.data != null) { "Did you call ImGui_ImplGlfw_InitForXXX()?" }
        if (data.fontTexture[0] == 0)
            createDeviceObjects()
    }

    fun setupRenderState(drawData: DrawData, fbWidth: Int, fbHeight: Int) {

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        //glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA); // In order to composite our output buffer we need to preserve alpha
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_STENCIL_TEST)
        glDisable(GL11.GL_LIGHTING)
        glDisable(GL11.GL_COLOR_MATERIAL)
        glEnable(GL_SCISSOR_TEST)
        GL11.glEnableClientState(GL_VERTEX_ARRAY)
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY)
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY)
        glEnable(GL_TEXTURE_2D)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE)

        // you may need to backup/reset/restore other state, e.g. for current shader using the commented lines below.
        // (DO NOT MODIFY THIS FILE! Add the code in your calling function)
        //   GLint last_program;
        //   glGetIntegerv(GL_CURRENT_PROGRAM, &last_program);
        //   glUseProgram(0);
        //   ImGui_ImplOpenGL2_RenderDrawData(...);
        //   glUseProgram(last_program)
        // There are potentially many more states you could need to clear/setup that we can't access from default headers.
        // e.g. glBindBuffer(GL_ARRAY_BUFFER, 0), glDisable(GL_TEXTURE_CUBE_MAP).

        // Setup viewport, orthographic projection matrix
        // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right).
        // DisplayPos is (0,0) for single viewport apps.
        glViewport(0, 0, fbWidth, fbHeight)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        drawData.run { GL11.glOrtho(displayPos.x.d, displayPos.x.d + displaySize.x, displayPos.y.d + displaySize.y, displayPos.y.d, -1.0, +1.0) }
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
    }

    /** OpenGL2 Render function.
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL state explicitly.
     *  This is in order to be able to run within an OpenGL engine that doesn't do so. */
    override fun renderDrawData(drawData: DrawData) {

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
        if (fbWidth == 0 || fbHeight == 0) return

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastPolygonMode = glGetVec2i(GL_POLYGON_MODE)
        val lastViewport = glGetVec4i(GL_VIEWPORT)
        val lastScissorBox = glGetVec4i(GL_SCISSOR_BOX)
        val lastShadeModel = glGetInteger(GL11.GL_SHADE_MODEL)
        val lastTexEnvMode = glGetTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE)
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL_COLOR_BUFFER_BIT or GL11.GL_TRANSFORM_BIT)

        // Setup desired GL state
        setupRenderState(drawData, fbWidth, fbHeight)

        // Will project scissor/clipping rectangles into framebuffer space
        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        for (cmdList in drawData.cmdLists) {

            // Upload vertex/index buffers
            GL11.glVertexPointer(Vec2.length, GL_FLOAT, DrawVert.SIZE, cmdList.vtxBuffer.data.adr.L + 0)
            GL11.glTexCoordPointer(Vec2.length, GL_FLOAT, DrawVert.SIZE, cmdList.vtxBuffer.data.adr.L + Vec2.size)
            GL11.glColorPointer(Vec4b.length, GL_UNSIGNED_BYTE, DrawVert.SIZE, cmdList.vtxBuffer.data.adr.L + Vec2.size * 2)

            val idxBufferOffset = cmdList.idxBuffer.adr.L

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
                    val clipMin = Vec2((cmd.clipRect.x - clipOff.x) * clipScale.x, (cmd.clipRect.y - clipOff.y) * clipScale.y)
                    val clipMax = Vec2((cmd.clipRect.z - clipOff.x) * clipScale.x, (cmd.clipRect.w - clipOff.y) * clipScale.y)
                    if (clipMax.x <= clipMin.x || clipMax.y <= clipMin.y)
                        continue

                    // Apply scissor/clipping rectangle (Y is inverted in OpenGL)
                    glScissor(clipMin.x.i, (fbHeight.f - clipMax.y).i, (clipMax.x - clipMin.x).i, (clipMax.y - clipMin.y).i)

                    // Bind texture, Draw
                    glBindTexture(GL_TEXTURE_2D, cmd.texID!!.i)
                    glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, idxBufferOffset + cmd.idxOffset)
                }
            }
        }

        // Restore modified GL state
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY)
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
        GL11.glDisableClientState(GL_VERTEX_ARRAY)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glActiveTexture(lastActiveTexture)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glPopAttrib()
        glPolygonMode(GL_FRONT, lastPolygonMode[0]); glPolygonMode(GL_BACK, lastPolygonMode[1])
        glViewport(lastViewport)
        glScissor(lastScissorBox)
        GL11.glShadeModel(lastShadeModel)
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, lastTexEnvMode)
    }

    override fun createDeviceObjects(): Boolean = createFontsTexture()

    /** Build texture atlas */
    override fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bit (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = ImGui.io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        // (Bilinear sampling is required by default. Set 'io.Fonts->Flags |= ImFontAtlasFlags_NoBakedLines' or 'style.AntiAliasedLinesUseTex = false' to allow point/nearest sampling)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        glGenTextures(data.fontTexture)
        glBindTexture(GL_TEXTURE_2D, data.fontTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

        // Store our identifier
        ImGui.io.fonts.texID = data.fontTexture[0].L

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return when {
            DEBUG -> checkError("mainLoop")
            else -> true
        }
    }

    override fun destroyFontsTexture() {
        if (data.fontTexture[0] != 0) {
            glDeleteTextures(data.fontTexture)
            ImGui.io.fonts.texID = 0
            data.fontTexture[0] = 0
        }
    }

    override fun destroyDeviceObjects() = destroyFontsTexture()

    companion object {
        object data {
            val fontTexture = IntBuffer(1)
        }
    }
}