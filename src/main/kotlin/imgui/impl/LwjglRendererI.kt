package imgui.impl

import imgui.DrawData

interface LwjglRendererI {
    fun createDeviceObjects(): Boolean
    fun renderDrawData(drawData: DrawData)
    fun destroyDeviceObjects()
}