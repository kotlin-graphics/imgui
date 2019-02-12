package imgui.impl

import gln.checkError
import gln.objects.GlProgram
import gln.texture.initTexture2d
import imgui.DrawData
import imgui.ImGui
import kool.IntBuffer
import kool.set
import org.lwjgl.opengl.GL21C.*

class ImplGL2: LwjglRendererI {

    var program = GlProgram(0)
    var matUL = -1
    var CLIP_ORIGIN = false

    val mouseJustPressed = BooleanArray(5)
    val bufferName = IntBuffer<Buffer>()
    val vaoName = IntBuffer(1)
    val fontTexture = IntBuffer(1)

    override fun createDeviceObjects(): Boolean {

        // this shall be in init, but since we dont have it because we do differently about the glsl version we do this here
        ImGui.io.backendRendererName = "imgui impl opengl2"

        return createFontsTexture()
    }

    private fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bits (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = ImGui.io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        initTexture2d(fontTexture) {
            minFilter = linear
            magFilter = linear
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            image(GL_RGBA, size, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
        }

        // Store our identifier
        ImGui.io.fonts.texId = fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture)

        return checkError("createFontsTexture")
    }

    private fun destroyFontsTexture() {
        if (fontTexture[0] != 0) {
            glDeleteTextures(fontTexture)
            ImGui.io.fonts.texId = 0
            fontTexture[0] = 0
        }
    }

    override fun destroyDeviceObjects() {
        destroyFontsTexture()
    }

    override fun renderDrawData(drawData: DrawData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}