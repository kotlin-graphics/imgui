package imgui.internal.api

import imgui.api.g
import imgui.ImGui.tableGcCompactSettings
import imgui.internal.classes.Window
import imgui.reserve
import kool.cap

/** Garbage collection */
interface garbageCollection {

    fun gcCompactTransientMiscBuffers() {
        g.itemFlagsStack.clear()
        g.groupStack.clear()
        tableGcCompactSettings()
    }

    /** Free up/compact internal window buffers, we can use this when a window becomes unused.
     *  Not freed:
     *  - ImGuiWindow, ImGuiWindowSettings, Name, StateStorage, ColumnsStorage (may hold useful data)
     *  This should have no noticeable visual effect. When the window reappear however, expect new allocation/buffer growth/copy cost.
     *
     *  ~gcCompactTransientWindowBuffers */
    fun Window.gcCompactTransientBuffers() {
        memoryCompacted = true
        memoryDrawListIdxCapacity = drawList.idxBuffer.cap
        memoryDrawListVtxCapacity = drawList.vtxBuffer.cap
        idStack.clear()
        drawList._clearFreeMemory()
        dc.apply {
            childWindows.clear()
            itemWidthStack.clear()
            textWrapPosStack.clear()
        }
    }

    /** ~GcAwakeTransientWindowBuffers */
    fun Window.gcAwakeTransientBuffers() { // We stored capacity of the ImDrawList buffer to reduce growth-caused allocation/copy when awakening.
        // The other buffers tends to amortize much faster.
        memoryCompacted = false
        drawList.apply {
            idxBuffer = idxBuffer reserve memoryDrawListIdxCapacity
            vtxBuffer = vtxBuffer reserve memoryDrawListVtxCapacity
        }
        memoryDrawListIdxCapacity = 0
        memoryDrawListVtxCapacity = 0
    }
}