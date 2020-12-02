package helpers

import glm_.vec2.Vec2
import glm_.vec4.Vec4i
import glm_.d
import glm_.f
import glm_.L
import imgui.ImGui.io
import imgui.Key
import kool.BYTES
import kool.adr
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

//-----------------------------------------------------------------------------
// [SECTION] ImGuiApp Implementation: Null
//-----------------------------------------------------------------------------

// Data
class ImGuiApp_ImplNull : ImGuiApp() {
    var lastTime = 0L

    // Functions

    override fun initCreateWindow(title: String, size: Vec2): Boolean {
        io.displaySize put size
        for (n in 0 until Key.COUNT)
            io.keyMap[n] = n

        return true
    }

    override fun newFrame(): Boolean {
        val time = timeInMicroseconds
        if (lastTime == 0L)
            lastTime = time
        io.deltaTime = ((time - lastTime).d / 1000000.0).f
        if (io.deltaTime <= 0f)
            io.deltaTime = 0.000001f
        lastTime = time

        return true
    }

    override fun captureFramebuffer(rect: Vec4i, pixels: ByteBuffer, userData: Any?): Boolean {
        MemoryUtil.memSet(pixels.adr, 0, rect.z * rect.w * Int.BYTES.L)
        return true
    }

    companion object {
        val timeInMicroseconds: Long
            get() = System.nanoTime() / 1_000
    }
}