package imgui.impl.gl

import org.lwjgl.opengl.GL
import org.lwjgl.system.Platform

class ImplBestGL: GLInterface by getNewImpl() {
    companion object {
        private val caps = GL.getCapabilities()
        private fun getNewImpl(): GLInterface {
            return when {
                caps.OpenGL32 -> {
                    gGlVersion = 150
                    ImplGL3()
                }
                caps.OpenGL30 && Platform.get() != Platform.MACOSX -> {
                    gGlVersion = 130
                    ImplGL3()
                }
                caps.OpenGL20 -> {
                    gGlVersion = 110
                    if (Platform.get() == Platform.MACOSX) ImplGL2_mac() else ImplGL2()
                }
                else -> throw RuntimeException("OpenGL 2 is not present on this system!")
            }
        }
    }
}