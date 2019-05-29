package imgui.impl

import glm_.BYTES
import imgui.DrawData
import imgui.DrawList
import imgui.DrawVert
import kool.ByteBuffer
import kool.IntBuffer
import kool.free
import kool.lim

interface GLInterface {

    fun shutdown()
    fun newFrame()
    fun renderDrawData(drawData: DrawData)

    // Called by Init/NewFrame/Shutdown
    fun createFontsTexture(): Boolean
    fun destroyFontsTexture()
    fun createDeviceObjects(): Boolean
    fun destroyDeviceObjects()
}