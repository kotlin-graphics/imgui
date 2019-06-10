package imgui.impl

import imgui.DrawData
import org.lwjgl.opengl.GL
import org.lwjgl.system.Platform
import uno.glfw.glfw

class ImplBestGL: GLInterface {
    private val internalImpl: GLInterface

    init {
        val caps = GL.getCapabilities()
        internalImpl = when {
            caps.OpenGL32 -> {
                glslVersion = 150
                ImplGL3()
            }
            caps.OpenGL30 && Platform.get() != Platform.MACOSX -> {
                glslVersion = 130
                ImplGL3()
            }
            caps.OpenGL20 -> {
                glslVersion = 110
                if (Platform.get() == Platform.MACOSX) ImplGL2_mac() else ImplGL2()
            }
            else -> throw RuntimeException("OpenGL 2 is not present on this system!")
        }
    }

    override fun shutdown() = internalImpl.shutdown()
    override fun newFrame() = internalImpl.newFrame()
    override fun renderDrawData(drawData: DrawData) = internalImpl.renderDrawData(drawData)
    override fun createFontsTexture() = internalImpl.createFontsTexture()
    override fun destroyFontsTexture() = internalImpl.destroyFontsTexture()
    override fun createDeviceObjects() = internalImpl.createDeviceObjects()
    override fun destroyDeviceObjects() = internalImpl.destroyDeviceObjects()
}