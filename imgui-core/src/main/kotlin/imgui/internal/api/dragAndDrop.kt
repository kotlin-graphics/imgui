package imgui.internal.api

import imgui.DragDropFlag
import imgui.ID
import imgui.ImGui.isMouseHoveringRect
import imgui.api.g
import imgui.internal.classes.Rect
import kool.lib.fill
import java.nio.ByteBuffer

internal interface dragAndDrop {

    fun beginDragDropTargetCustom(bb: Rect, id: ID): Boolean {
        if (!g.dragDropActive) return false

        assert(!g.dragDropWithinTarget)

        val window = g.currentWindow!!
        g.hoveredWindowUnderMovingWindow.let { if (it == null || window.rootWindow != it.rootWindow) return false }
        assert(id != 0)
        if (!isMouseHoveringRect(bb) || id == g.dragDropPayload.sourceId)
            return false
        if (window.skipItems) return false

        assert(!g.dragDropWithinTarget)
        g.dragDropTargetRect put bb
        g.dragDropTargetId = id
        g.dragDropWithinTarget = true
        return true
    }

    fun clearDragDrop() = with(g) {
        dragDropActive = false
        dragDropPayload.clear()
        dragDropAcceptFlags = DragDropFlag.None.i
        dragDropAcceptIdPrev = 0
        dragDropAcceptIdCurr = 0
        dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE
        dragDropAcceptFrameCount = -1

        g.dragDropPayloadBufHeap = ByteBuffer.allocate(0)
        g.dragDropPayloadBufLocal.fill(0)
    }

    val isDragDropPayloadBeingAccepted: Boolean
        get() = g.dragDropActive && g.dragDropAcceptIdPrev != 0
}