package imgui.impl.gl

import imgui.DrawData

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