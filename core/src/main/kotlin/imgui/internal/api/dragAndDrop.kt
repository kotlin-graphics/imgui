package imgui.internal.api

import glm_.vec2.Vec2
import imgui.Col
import imgui.DragDropFlag
import imgui.ID
import imgui.ImGui
import imgui.ImGui.isMouseHoveringRect
import imgui.api.g
import imgui.internal.classes.Rect
import kool.lib.fill
import java.nio.ByteBuffer

internal interface dragAndDrop {

    val isDragDropActive: Boolean
        get() = g.dragDropActive
    fun beginDragDropTargetCustom(bb: Rect, id: ID): Boolean {
        if (!g.dragDropActive) return false

        assert(!g.dragDropWithinTarget)

        val window = g.currentWindow!!
        val hoveredWindow = g.hoveredWindowUnderMovingWindow
        if (hoveredWindow == null || window.rootWindow !== hoveredWindow.rootWindow)
            return false
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

    /** FIXME-DRAGDROP: Settle on a proper default visuals for drop target. */
    fun renderDragDropTargetRect(bb: Rect) =
        ImGui.windowDrawList.addRect(bb.min - Vec2(3.5f), bb.max + Vec2( 3.5f), Col.DragDropTarget.u32, 0f, 0, 2f)
}