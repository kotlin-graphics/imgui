package imgui.internal.api

import glm_.f
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.dockContextQueueUndockNode
import imgui.ImGui.dockNodeGetRootNode
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isDragDropPayloadBeingAccepted
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isPopupOpen
import imgui.ImGui.keepAliveID
import imgui.ImGui.topMostPopupModal
import imgui.api.g
import imgui.classes.ViewportFlag
import imgui.classes.wo
import imgui.internal.classes.DockNode
import imgui.internal.classes.Window
import imgui.static.findHoveredWindow
import imgui.static.updateTryMergeWindowIntoHostViewport

/** NewFrame */
internal interface newFrame {

    /** The reason this is exposed in imgui_internal.h is: on touch-based system that don't have hovering,
     *  we want to dispatch inputs to the right target (imgui vs imgui+app) */
    fun updateHoveredWindowAndCaptureFlags() {

        /*  Find the window hovered by mouse:
            - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
            - When moving a window we can skip the search, which also conveniently bypasses the fact that window.outerRectClipped
                is lagging as this point of the frame.
            - We also support the moved window toggling the NoInputs flag after moving has started in order
                to be able to detect windows below it, which is useful for e.g. docking mechanisms. */
        findHoveredWindow()
        assert(g.hoveredWindow == null || g.hoveredWindow === g.movingWindow || g.hoveredWindow!!.viewport === g.mouseViewport)

        fun nullate() {
            g.hoveredWindow = null
            g.hoveredRootWindow = null
            g.hoveredWindowUnderMovingWindow = null
        }

        // Modal windows prevents cursor from hovering behind them.
        val modalWindow = topMostPopupModal
        if (modalWindow != null)
            if (g.hoveredRootWindow?.isChildOf(modalWindow) == false)
                nullate()
        // Disabled mouse?
        if (io.configFlags has ConfigFlag.NoMouse)
            nullate()

        // We track click ownership. When clicked outside of a window the click is owned by the application and won't report hovering nor request capture even while dragging over our windows afterward.
        var mouseEarliestButtonDown = -1
        var mouseAnyDown = false
        for (i in io.mouseDown.indices) {
            if (io.mouseClicked[i])
                io.mouseDownOwned[i] = g.hoveredWindow != null || g.openPopupStack.isNotEmpty()
            mouseAnyDown = mouseAnyDown || io.mouseDown[i]
            if (io.mouseDown[i])
                if (mouseEarliestButtonDown == -1 || io.mouseClickedTime[i] < io.mouseClickedTime[mouseEarliestButtonDown])
                    mouseEarliestButtonDown = i
        }
        val mouseAvailToImgui = mouseEarliestButtonDown == -1 || io.mouseDownOwned[mouseEarliestButtonDown]

        // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
        // FIXME: For patterns of drag and drop across OS windows, we may need to rework/remove this test (first committed 311c0ca9 on 2015/02)
        val mouseDraggingExternPayload = g.dragDropActive && g.dragDropSourceFlags has DragDropFlag.SourceExtern
        if (!mouseAvailToImgui && !mouseDraggingExternPayload)
            nullate()

        // Update io.WantCaptureMouse for the user application (true = dispatch mouse info to Dear ImGui, false = dispatch mouse info to imgui + app)
        if (g.wantCaptureMouseNextFrame != -1)
            io.wantCaptureMouse = g.wantCaptureMouseNextFrame != 0
        else
            io.wantCaptureMouse = (mouseAvailToImgui && (g.hoveredWindow != null || mouseAnyDown)) || g.openPopupStack.isNotEmpty()

        // Update io.WantCaptureKeyboard for the user application (true = dispatch keyboard info to Dear ImGui, false = dispatch keyboard info to imgui + app)
        if (g.wantCaptureKeyboardNextFrame != -1)
            io.wantCaptureKeyboard = g.wantCaptureKeyboardNextFrame != 0
        else
            io.wantCaptureKeyboard = g.activeId != 0 || modalWindow != null
        if (io.navActive && io.configFlags has ConfigFlag.NavEnableKeyboard && io.configFlags hasnt ConfigFlag.NavNoCaptureKeyboard)
            io.wantCaptureKeyboard = true

        // Update io.WantTextInput flag, this is to allow systems without a keyboard (e.g. mobile, hand-held) to show a software keyboard if possible
        io.wantTextInput = if (g.wantTextInputNextFrame != -1) g.wantTextInputNextFrame != 0 else false
    }

    // StartMouseMovingWindow -> Window class

    /** We use 'undock_floating_node == false' when dragging from title bar to allow moving groups of floating nodes without undocking them.
     *  - undock_floating_node == true: when dragging from a floating node within a hierarchy, always undock the node.
     * - undock_floating_node == false: when dragging from a floating node within a hierarchy, move root window. */
    fun startMouseMovingWindowOrNode(window: Window, node: DockNode?, undockFloatingNode: Boolean) {
        var canUndockNode = false
        if (node?.visibleWindow?.flags?.hasnt(WindowFlag.NoMove) == true) {
            // Can undock if:
            // - part of a floating node hierarchy with more than one visible node (if only one is visible, we'll just move the whole hierarchy)
            // - part of a dockspace node hierarchy (trivia: undocking from a fixed/central node will create a new node and copy windows)
            val rootNode = dockNodeGetRootNode(node)
            if (rootNode.onlyNodeWithWindows != node || rootNode.centralNode != null)   // -V1051 PVS-Studio thinks node should be root_node and is wrong about that.
                if (undockFloatingNode || rootNode.isDockSpace)
                    canUndockNode = true
        }

        val clicked = isMouseClicked(MouseButton.Left)
        val dragging = isMouseDragging(MouseButton.Left, io.mouseDragThreshold * 1.7f)
        if (canUndockNode && dragging) {
            dockContextQueueUndockNode(g, node!!)
            g.activeIdClickOffset = io.mouseClickedPos[0] - node.pos
        }
        else if (!canUndockNode && (clicked || dragging) && g.movingWindow !== window) {
            window.startMouseMoving()
            g.activeIdClickOffset = io.mouseClickedPos[0] - window.rootWindow!!.pos
        }
    }

    /** Handle mouse moving window
     *  Note: moving window with the navigation keys (Square + d-pad / CTRL+TAB + Arrows) are processed in NavUpdateWindowing()
     *  FIXME: We don't have strong guarantee that g.MovingWindow stay synched with g.ActiveId == g.MovingWindow->MoveId.
     *  This is currently enforced by the fact that BeginDragDropSource() is setting all g.ActiveIdUsingXXXX flags to inhibit navigation inputs,
     *  but if we should more thoroughly test cases where g.ActiveId or g.MovingWindow gets changed and not the other. */
    fun updateMouseMovingWindowNewFrame() {

        val mov = g.movingWindow
        if (mov != null) {
            /*  We actually want to move the root window. g.movingWindow === window we clicked on
                (could be a child window).
                We track it to preserve Focus and so that generally activeIdWindow === movingWindow and
                activeId == movingWindow.moveId for consistency.    */
            keepAliveID(g.activeId)
            assert(mov.rootWindow != null)
            val movingWindow = mov.rootWindow!!
            if (io.mouseDown[0] && isMousePosValid(io.mousePos)) {
                val pos = io.mousePos - g.activeIdClickOffset
                if (movingWindow.pos.x.f != pos.x || movingWindow.pos.y.f != pos.y) {
                    movingWindow.markIniSettingsDirty()
                    movingWindow.setPos(pos, Cond.Always)
                    if (movingWindow.viewportOwned) // Synchronize viewport immediately because some overlays may relies on clipping rectangle before we Begin() into the window.
                        movingWindow.viewport!!.pos put pos
                }
                focusWindow(mov)
            } else {
                // Try to merge the window back into the main viewport.
                // This works because MouseViewport should be != MovingWindow->Viewport on release (as per code in UpdateViewports)
                if (g.configFlagsCurrFrame has ConfigFlag.ViewportsEnable)
                    updateTryMergeWindowIntoHostViewport(movingWindow, g.mouseViewport!!)

                // Restore the mouse viewport so that we don't hover the viewport _under_ the moved window during the frame we released the mouse button.
                if (!isDragDropPayloadBeingAccepted)
                    g.mouseViewport = movingWindow.viewport

                // Clear the NoInput window flag set by the Viewport system
                movingWindow.viewport!!.apply { flags = flags wo ViewportFlag.NoInputs }

                clearActiveID()
                g.movingWindow = null
            }
        } else
        /*  When clicking/dragging from a window that has the _NoMove flag, we still set the ActiveId in order
            to prevent hovering others.                 */
            if (g.activeIdWindow?.moveId == g.activeId) {
                keepAliveID(g.activeId)
                if (!io.mouseDown[0])
                    clearActiveID()
            }
    }

    /** Initiate moving window, handle left-click and right-click focus
     *  Handle left-click and right-click focus. */
    fun updateMouseMovingWindowEndFrame() {

        if (g.activeId != 0 || g.hoveredId != 0) return

        // Unless we just made a window/popup appear
        if (g.navWindow?.appearing == true) return

        // Click on void to focus window and start moving
        // (after we're done with all our widgets, so e.g. clicking on docking tab-bar which have set HoveredId already and not get us here!)
        if (io.mouseClicked[0]) {
            // Handle the edge case of a popup being closed while clicking in its empty space.
            // If we try to focus it, FocusWindow() > ClosePopupsOverWindow() will accidentally close any parent popups because they are not linked together any more.
            val hoveredWindow = g.hoveredWindow
            val rootWindow = hoveredWindow?.rootWindowDockStop
            val isClosedPopup = rootWindow != null && rootWindow.flags has WindowFlag._Popup && !isPopupOpen(rootWindow.popupId, PopupFlag.AnyPopupLevel.i)

            if (rootWindow != null && !isClosedPopup) {
                hoveredWindow.startMouseMoving()
                if (io.configWindowsMoveFromTitleBarOnly)
                    if (rootWindow.flags hasnt WindowFlag.NoTitleBar || rootWindow.dockIsActive)
                        if (io.mouseClickedPos[0] !in rootWindow.titleBarRect())
                            g.movingWindow = null
            }
            else if (rootWindow != null && g.navWindow != null && topMostPopupModal == null)
                // Clicking on void disable focus
                focusWindow()
        }

        /*  With right mouse button we close popups without changing focus based on where the mouse is aimed
            Instead, focus will be restored to the window under the bottom-most closed popup.
            (The left mouse button path calls FocusWindow on the hovered window, which will lead NewFrame->ClosePopupsOverWindow to trigger)    */
        if (io.mouseClicked[1]) {
            // Find the top-most window between HoveredWindow and the top-most Modal Window.
            // This is where we can trim the popup stack.
            val modal = topMostPopupModal
            var hoveredWindowAboveModal = false
            if (modal == null)
                hoveredWindowAboveModal = true
            var i = g.windows.lastIndex
            while (i >= 0 && !hoveredWindowAboveModal) {
                val window = g.windows[i]
                if (window === modal)
                    break
                if (window === g.hoveredWindow)
                    hoveredWindowAboveModal = true
                i--
            }
            closePopupsOverWindow(if (hoveredWindowAboveModal) g.hoveredWindow else modal, true)
        }
    }
}