package imgui.impl.gl

import org.lwjgl.opengl.GL
import org.lwjgl.system.Platform

class ImplBestGL: GLInterface by getNewImpl() {
    companion object {
        private val caps = GL.getCapabilities()
        private fun getNewImpl(): GLInterface {
            TODO()
//            return when {
//                caps.OpenGL32 -> {
//                    glslVersion = 150
//                    ImplGL3()
//                }
//                caps.OpenGL30 && Platform.get() != Platform.MACOSX -> {
//                    glslVersion = 130
//                    ImplGL3()
//                }
//                caps.OpenGL20 -> {
//                    glslVersion = 110
//                    if (Platform.get() == Platform.MACOSX) ImplGL2_mac() else ImplGL2()
//                }
//                else -> throw RuntimeException("OpenGL 2 is not present on this system!")
//            }
        }
    }
}