package imgui.impl

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.mat4x4.Mat4
import imgui.DrawData
import imgui.DrawVert
import imgui.IO
import imgui.Key_
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import uno.buffer.intBufferBig
import uno.gl.glGetVec4i
import uno.glf.semantic
import uno.glfw.GlfwWindow
import uno.glfw.glfw
import uno.gln.*
import uno.glsl.Program
import imgui.set


object GlfwGL3 {

    lateinit var window: GlfwWindow
    var time = 0.0
    val mousePressed = Array(3, { false })
    var mouseWheel = 0f
    val textureName = intBufferBig(1)

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

        GlfwGL3.window = window

        with(IO) {
            // Keyboard mapping. ImGui will use those indices to peek into the io.KeyDown[] array.
            keyMap[Key_.Tab] = GLFW_KEY_TAB
            keyMap[Key_.LeftArrow] = GLFW_KEY_LEFT
            keyMap[Key_.RightArrow] = GLFW_KEY_RIGHT
            keyMap[Key_.UpArrow] = GLFW_KEY_UP
            keyMap[Key_.DownArrow] = GLFW_KEY_DOWN
            keyMap[Key_.PageUp] = GLFW_KEY_PAGE_UP
            keyMap[Key_.PageDown] = GLFW_KEY_PAGE_DOWN
            keyMap[Key_.Home] = GLFW_KEY_HOME
            keyMap[Key_.End] = GLFW_KEY_END
            keyMap[Key_.Delete] = GLFW_KEY_DELETE
            keyMap[Key_.Backspace] = GLFW_KEY_BACKSPACE
            keyMap[Key_.Enter] = GLFW_KEY_ENTER
            keyMap[Key_.Escape] = GLFW_KEY_ESCAPE
            keyMap[Key_.A] = GLFW_KEY_A
            keyMap[Key_.C] = GLFW_KEY_C
            keyMap[Key_.V] = GLFW_KEY_V
            keyMap[Key_.X] = GLFW_KEY_X
            keyMap[Key_.Y] = GLFW_KEY_Y
            keyMap[Key_.Z] = GLFW_KEY_Z

            /* Alternatively you can set this to NULL and call ImGui::GetDrawData() after ImGui::Render() to get the
               same ImDrawData pointer.             */
            renderDrawListsFn = this@GlfwGL3::renderDrawLists
        }

        if (installCallbacks)
            TODO()

        return true
    }

    fun createDeviceObjects(): Boolean {

        // Backup GL state
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)

        program = ProgramA("shader")

        glGenBuffers(bufferName)

        glGenVertexArrays(vaoName)
        glBindVertexArray(vaoName)
        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.Vertex])
        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.TEX_COORD)
        glEnableVertexAttribArray(semantic.attr.COLOR)

        glVertexAttribPointer(semantic.attr.POSITION, 2, GL_FLOAT, false, DrawVert.size, 0)
        glVertexAttribPointer(semantic.attr.TEX_COORD, 2, GL_FLOAT, false, DrawVert.size, Vec2.size)
        glVertexAttribPointer(semantic.attr.COLOR, 4, GL_UNSIGNED_BYTE, true, DrawVert.size, 2 * Vec2.size)

        createFontsTexture()

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindVertexArray(lastVertexArray)

        return true
    }

    class ProgramA(shader: String) : Program("$shader.vert", "$shader.frag") {
        val mat = glGetUniformLocation(name, "mat")

        init {
            glUseProgram(program)
            glUniform(glGetUniformLocation(name, "Texture"), semantic.sampler.DIFFUSE)
        }
    }

    fun createFontsTexture(): Boolean {

        // Build texture atlas

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = IO.fonts.getTexDataAsAlpha8()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        initTexture2d(fontTexture) {
            minFilter = linear
            magFilter = linear
            image(GL_RGBA, size, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
        }

        // Store our identifier
        IO.fonts.texID = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return true
    }

    fun newFrame() {

        if (textureName[0] == 0)
            createDeviceObjects()

        // Setup display size (every frame to accommodate for window resizing)
        IO.displaySize put window.size
        IO.displayFramebufferScale.x = if (window.size.x > 0) window.framebufferSize.x / window.size.x.f else 0f
        IO.displayFramebufferScale.y = if (window.size.y > 0) window.framebufferSize.y / window.size.y.f else 0f

        // Setup time step
        val currentTime = glfw.time
        IO.deltaTime = if(time > 0.0) (currentTime- time).f else 1.0f/60.0f
        time = currentTime

        // Setup inputs
        // (we already got mouse wheel, keyboard keys & characters from glfw callbacks polled in glfwPollEvents())
        // Mouse position in screen coordinates (set to -1,-1 if no mouse / on another screen, etc.)
        if (window.focused)
            IO.mousePos put window.cursorPos
        else
            IO.mousePos put -1

        repeat(3)        {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            IO.mouseDown[it] = mousePressed[it] || window.mouseButton(it) != 0
            mousePressed[it] = false
        }

        IO.mouseWheel = mouseWheel
        mouseWheel = 0.0f

        // Hide OS mouse cursor if ImGui is drawing it
        window.cursor = if(IO.mouseDrawCursor) GlfwWindow.Cursor.Hidden else GlfwWindow.Cursor.Normal

        // Start the frame
        newFrame()
    }

    /** This is the main rendering function that you have to implement and provide to ImGui (via setting up
     *  'RenderDrawListsFn' in the ImGuiIO structure)
     *  If text or lines are blurry when integrating ImGui in your engine:
     *      - in your Render function, try translating your projection matrix by (0.5f,0.5f) or (0.375f,0.375f) */
    fun renderDrawLists(drawData: DrawData) {

        /** Avoid rendering when minimized, scale coordinates for retina displays
         *  (screen coordinates != framebuffer coordinates) */
        val fbWidth = IO.displaySize.x * IO.displayFramebufferScale.x
        val fbHeight = IO.displaySize.y * IO.displayFramebufferScale.y
        if (fbWidth == 0f || fbHeight == 0f) return
        drawData.scaleClipRects(IO.displayFramebufferScale)

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
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
        glViewport(0, 0, fbWidth.i, fbHeight.i)
//        val ortho = glm.ortho(mat, 0f, IO.displaySize.x, IO.displaySize.y, 0f) // TODO glm Number
//        glUseProgram(program)
//        glUniform1i(g_AttribLocationTex, 0)
//        glUniformMatrix4fv(g_AttribLocationProjMtx, 1, GL_FALSE, & ortho_projection [0][0])
//        glBindVertexArray(g_VaoHandle)
//
//        for (int n = 0; n < drawData->CmdListsCount; n++)
//        {
//            const ImDrawList * cmd_list = draw_data->CmdLists[n]
//            const ImDrawIdx * idx_buffer_offset = 0
//
//            glBindBuffer(GL_ARRAY_BUFFER, g_VboHandle)
//            glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr) cmd_list->VtxBuffer.Size * sizeof(ImDrawVert), (const GLvoid*)cmd_list->VtxBuffer.Data, GL_STREAM_DRAW)
//
//            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, g_ElementsHandle)
//            glBufferData(GL_ELEMENT_ARRAY_BUFFER, (GLsizeiptr) cmd_list->IdxBuffer.Size * sizeof(ImDrawIdx), (const GLvoid*)cmd_list->IdxBuffer.Data, GL_STREAM_DRAW)
//
//            for (int cmd_i = 0; cmd_i < cmd_list->CmdBuffer.Size; cmd_i++)
//            {
//                const ImDrawCmd * pcmd = &cmd_list->CmdBuffer[cmd_i]
//                if (pcmd->UserCallback)
//                {
//                    pcmd ->
//                    UserCallback(cmd_list, pcmd)
//                }
//                else
//                {
//                    glBindTexture(GL_TEXTURE_2D, (GLuint)(intptr_t) pcmd->TextureId)
//                    glScissor((int) pcmd->ClipRect.x, (int)(fb_height-pcmd->ClipRect.w), (int)(pcmd->ClipRect.z-pcmd->ClipRect.x), (int)(pcmd->ClipRect.w-pcmd->ClipRect.y))
//                    glDrawElements(GL_TRIANGLES, (GLsizei) pcmd->ElemCount, sizeof(ImDrawIdx) == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT, idx_buffer_offset)
//                }
//                idx_buffer_offset += pcmd->ElemCount
//            }
//        }

        // Restore modified GL state
        glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glActiveTexture(lastActiveTexture)
        glBindVertexArray(lastVertexArray)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementArrayBuffer)
        glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha)
        glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha)
        if (lastEnableBlend) glEnable(GL_BLEND); else glDisable(GL_BLEND)
        if (lastEnableCullFace) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE)
        if (lastEnableDepthTest) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST)
        if (lastEnableScissorTest) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST)
//        glViewport(lastViewport[0], lastViewport[1], (GLsizei) last_viewport [2], (GLsizei) last_viewport [3])
//        glScissor(lastScissorBox[0], lastScissorBox[1], (GLsizei) last_scissor_box [2], (GLsizei) last_scissor_box [3])
    }
}