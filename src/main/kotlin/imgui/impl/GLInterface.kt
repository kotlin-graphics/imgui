package imgui.impl

import glm_.BYTES
import imgui.DrawData
import imgui.DrawList
import imgui.DrawVert
import kool.ByteBuffer
import kool.IntBuffer
import kool.free

interface GLInterface {

    fun shutdown()
    fun newFrame()
    fun renderDrawData(drawData: DrawData)

    // Called by Init/NewFrame/Shutdown
    fun createFontsTexture(): Boolean
    fun destroyFontsTexture()
    fun createDeviceObjects(): Boolean
    fun destroyDeviceObjects()

    private tailrec fun incrementIfNeeded(size: Int, minSize: Int): Int = when {
        size < minSize -> incrementIfNeeded(size shl 1, minSize)
        else -> size
    }

    fun resizeIfNeeded(draws: ArrayList<DrawList>): Boolean {

        val minVtxSize = draws.map { it.vtxBuffer.size }.sum() * DrawVert.size
        val minIdxSize = draws.map { it.idxBuffer.size }.sum() * Int.BYTES

        val newVtxSize = incrementIfNeeded(vtxSize, minVtxSize)
        val newIdxSize = incrementIfNeeded(idxSize, minIdxSize)

        return when {

            newVtxSize != vtxSize || newIdxSize != idxSize -> {

                vtxSize = newVtxSize
                idxSize = newIdxSize

                vtxBuffer.free()
                vtxBuffer = ByteBuffer(vtxSize)
                idxBuffer.free()
                idxBuffer = IntBuffer(idxSize / Int.BYTES)

                true
            }
            else -> false
        }
    }
}