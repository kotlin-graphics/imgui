package imgui.api

import imgui.*
import imgui.ImGui.beginTooltip
import imgui.ImGui.clearDragDrop
import imgui.ImGui.endTooltip
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isMouseDown
import imgui.ImGui.isMouseDragging
import imgui.ImGui.itemHoverable
import imgui.ImGui.setActiveID
import imgui.classes.Payload
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.*
import imgui.internal.sections.*
import imgui.DragDropFlag as Ddf

/** Drag and Drop
 *  - [BETA API] API may evolve!
 *  - If you stop calling BeginDragDropSource() the payload is preserved however it won't have a preview tooltip (we currently display a fallback "..." tooltip as replacement) */
interface dragAndDrop {

    /** Call when the current item is active. If this return true, you can call SetDragDropPayload() + EndDragDropSource()
     *
     *  When this returns true you need to:
     *      a) call setDragDropPayload() exactly once
     *      b) you may render the payload visual/description,
     *      c) call endDragDropSource()     */
    fun beginDragDropSource(flag: Ddf): Boolean = beginDragDropSource(flag.i)

    /** Call when the current item is active. If this return true, you can call SetDragDropPayload() + EndDragDropSource()
     *
     *  When this returns true you need to:
     *      a) call setDragDropPayload() exactly once
     *      b) you may render the payload visual/description,
     *      c) call endDragDropSource()     */
    fun beginDragDropSource(flags: DragDropFlags = 0): Boolean {

        var window: Window? = g.currentWindow!!

        val sourceDragActive: Boolean
        var sourceId: ID
        var sourceParentId: ID = 0
        val mouseButton = MouseButton.Left
        if (flags hasnt Ddf.SourceExtern) {
            sourceId = window!!.dc.lastItemId
            if (sourceId != 0 && g.activeId != sourceId) // Early out for most common case
                return false

            if (!io.mouseDown[mouseButton.i]) return false
            if (sourceId == 0) {
                /*  If you want to use beginDragDropSource() on an item with no unique identifier for interaction,
                    such as text() or image(), you need to:
                    A) Read the explanation below
                    B) Use the DragDropFlag.SourceAllowNullID flag
                     C) Swallow your programmer pride.  */
                if (flags hasnt Ddf.SourceAllowNullID) throw Error()

                // Early out
                if (window.dc.lastItemStatusFlags hasnt ItemStatusFlag.HoveredRect && (g.activeId == 0 || g.activeIdWindow !== window))
                    return false

                /*  Magic fallback (=somehow reprehensible) to handle items with no assigned ID, e.g. text(), image()
                    We build a throwaway ID based on current ID stack + relative AABB of items in window.
                    THE IDENTIFIER WON'T SURVIVE ANY REPOSITIONING OF THE WIDGET, so if your widget moves
                    your dragging operation will be canceled.
                    We don't need to maintain/call clearActiveID() as releasing the button will early out this function
                    and trigger !activeIdIsAlive. */
                window.dc.lastItemId = window.getIdFromRectangle(window.dc.lastItemRect)
                sourceId = window.dc.lastItemId
                val isHovered = itemHoverable(window.dc.lastItemRect, sourceId)
                if (isHovered && io.mouseClicked[mouseButton.i]) {
                    setActiveID(sourceId, window)
                    focusWindow(window)
                }
                if(g.activeId == sourceId) // Allow the underlying widget to display/return hovered during the mouse release frame, else we would get a flicker.
                    g.activeIdAllowOverlap = isHovered
            } else
                g.activeIdAllowOverlap = false
            if (g.activeId != sourceId)
                return false
            sourceParentId = window.idStack.last()
            sourceDragActive = isMouseDragging(mouseButton)

            // Disable navigation and key inputs while dragging
            g.activeIdUsingNavDirMask = -1
            g.activeIdUsingNavInputMask = -1
            g.activeIdUsingKeyInputMask = -1L
        } else {
            window = null
            sourceId = hashStr("#SourceExtern")
            sourceDragActive = true
        }

        if (sourceDragActive) {
            if (!g.dragDropActive) {
                assert(sourceId != 0)
                clearDragDrop()
                val payload = g.dragDropPayload
                payload.sourceId = sourceId
                payload.sourceParentId = sourceParentId
                g.dragDropActive = true
                g.dragDropSourceFlags = flags
                g.dragDropMouseButton = mouseButton
                if (payload.sourceId == g.activeId)
                    g.activeIdNoClearOnFocusLoss = true
            }

            g.dragDropSourceFrameCount = g.frameCount
            g.dragDropWithinSource = true

            if (flags hasnt Ddf.SourceNoPreviewTooltip) {
                /*  Target can request the Source to not display its tooltip (we use a dedicated flag to make this request explicit)
                    We unfortunately can't just modify the source flags and skip the call to BeginTooltip, as caller may be emitting contents.                 */
                beginTooltip()
                if (g.dragDropAcceptIdPrev != 0 && g.dragDropAcceptFlags has Ddf.AcceptNoPreviewTooltip)
                    g.currentWindow!!.apply {
                        // tooltipWindow
                        skipItems = true
                        hiddenFramesCanSkipItems = 1
                    }
            }

            if (flags hasnt Ddf.SourceNoDisableHover && flags hasnt Ddf.SourceExtern)
                window!!.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags wo ItemStatusFlag.HoveredRect

            return true
        }
        return false
    }

    @Deprecated("Replaced by setDragDropPayload without size argument", ReplaceWith("setDragDropPayload(type, data, cond_)"))
    fun setDragDropPayload(type: String, data: Any, size: Int, cond_: Cond = Cond.None): Boolean {
        return setDragDropPayload(type, data, cond_)
    }

//    /** Type is a user defined string of maximum 32 characters. Strings starting with '_' are reserved for dear imgui internal types.
//     *  Data is copied and held by imgui.
    /** Type is a user defined string. Types starting with '_' are reserved for dear imgui internal types.
     *  Data is held by imgui.
     *  Use 'cond' to choose to submit payload on drag start or every frame */
    fun setDragDropPayload(type: String, data: Any?, cond_: Cond = Cond.None): Boolean {
        val payload = g.dragDropPayload
        val cond = if (cond_ == Cond.None) Cond.Always else cond_

        assert(type.isNotEmpty())
//        assert(type.length < 32) { "Payload type can be at most 32 characters long" }
//        assert((data != NULL && data_size > 0) || (data == NULL && data_size == 0))
        assert(cond == Cond.Always || cond == Cond.Once)
        assert(payload.sourceId != 0) { "Not called between beginDragDropSource() and endDragDropSource()" }

        if (cond == Cond.Always || payload.dataFrameCount == -1) {
            // Copy payload
            payload.dataType = type
            payload.data = data
        }
        payload.dataFrameCount = g.frameCount

        return g.dragDropAcceptFrameCount == g.frameCount || g.dragDropAcceptFrameCount == g.frameCount - 1
    }

    /** Only call EndDragDropSource() if BeginDragDropSource() returns true!    */
    fun endDragDropSource() {
        assert(g.dragDropActive)
        assert(g.dragDropWithinSource) { "Not after a BeginDragDropSource()?" }

        if (g.dragDropSourceFlags hasnt Ddf.SourceNoPreviewTooltip)
            endTooltip()

        // Discard the drag if have not called setDragDropPayload()
        if (g.dragDropPayload.dataFrameCount == -1)
            clearDragDrop()
        g.dragDropWithinSource = false
    }

    /** Call after submitting an item that may receive an item.
     *  If this returns true, you can call acceptDragDropPayload() + endDragDropTarget()
     *
     *  We don't use beginDragDropTargetCustom() and duplicate its code because:
     *  1) we use lastItemRectHoveredRect which handles items that pushes a temporarily clip rectangle in their code.
     *      Calling beginDragDropTargetCustom(LastItemRect) would not handle them.
     *  2) and it's faster. as this code may be very frequently called, we want to early out as fast as we can.
     *  Also note how the HoveredWindow test is positioned differently in both functions (in both functions we optimize
     *  for the cheapest early out case)    */
    fun beginDragDropTarget(): Boolean {
        if (!g.dragDropActive) return false

        val window = g.currentWindow!!
        if (window.dc.lastItemStatusFlags hasnt ItemStatusFlag.HoveredRect) return false
        val hoveredWindow = g.hoveredWindowUnderMovingWindow
        if (hoveredWindow == null || window.rootWindow !== hoveredWindow.rootWindow) return false

        val displayRect = when {
            window.dc.lastItemStatusFlags has ItemStatusFlag.HasDisplayRect -> window.dc.lastItemDisplayRect
            else -> window.dc.lastItemRect
        }
        var id = window.dc.lastItemId
        if (id == 0)
            id = window.getIdFromRectangle(displayRect) // [JVM] save to pass the reference
        if (g.dragDropPayload.sourceId == id) return false

        assert(!g.dragDropWithinTarget)
        g.dragDropTargetRect put displayRect
        g.dragDropTargetId = id
        g.dragDropWithinTarget = true
        return true
    }

    /** Accept contents of a given type. If DragDropFlag.AcceptBeforeDelivery is set you can peek into the payload
     *  before the mouse button is released. */
    fun acceptDragDropPayload(type: String, flags: DrawListFlags = 0): Payload? {
        val window = g.currentWindow!!
        val payload = g.dragDropPayload
        assert(g.dragDropActive) { "Not called between BeginDragDropTarget() and EndDragDropTarget() ?" }
        assert(payload.dataFrameCount != -1) { "Forgot to call EndDragDropTarget() ?" }
        if (type.isNotEmpty() && !payload.isDataType(type)) return null

        /*  Accept smallest drag target bounding box, this allows us to nest drag targets conveniently without ordering constraints.
            NB: We currently accept NULL id as target. However, overlapping targets requires a unique ID to function!         */
        val wasAcceptedPreviously = g.dragDropAcceptIdPrev == g.dragDropTargetId
        val r = Rect(g.dragDropTargetRect)
        val rSurface = r.width * r.height
        if (rSurface <= g.dragDropAcceptIdCurrRectSurface) {
            g.dragDropAcceptFlags = flags
            g.dragDropAcceptIdCurr = g.dragDropTargetId
            g.dragDropAcceptIdCurrRectSurface = rSurface
        }

        // Render default drop visuals
        payload.preview = wasAcceptedPreviously
        if (flags hasnt Ddf.AcceptNoDrawDefaultRect && payload.preview) {
            // FIXME-DRAG: Settle on a proper default visuals for drop target.
            r expand 3.5f
            val pushClipRect = r !in window.clipRect
            if (pushClipRect) window.drawList.pushClipRect(r.min - 1, r.max + 1)
            window.drawList.addRect(r.min, r.max, Col.DragDropTarget.u32, 0f, 0.inv(), 2f)
            if (pushClipRect) window.drawList.popClipRect()
        }

        g.dragDropAcceptFrameCount = g.frameCount
        // For extern drag sources affecting os window focus, it's easier to just test !isMouseDown() instead of isMouseReleased()
        payload.delivery = wasAcceptedPreviously && !isMouseDown(g.dragDropMouseButton)
        if (!payload.delivery && flags hasnt Ddf.AcceptBeforeDelivery) return null

        return payload
    }

    /** We don't really use/need this now, but added it for the sake of consistency and because we might need it later.
     *  Only call EndDragDropTarget() if BeginDragDropTarget() returns true!    */
    fun endDragDropTarget() {
        assert(g.dragDropActive)
        assert(g.dragDropWithinTarget)
        g.dragDropWithinTarget = false
    }

    /** ~GetDragDropPayload */
    val dragDropPayload: Payload?
        get() = g.dragDropPayload.takeIf { g.dragDropActive }
}