package imgui.internal.api

import glm_.f
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isPopupOpen
import imgui.ImGui.keepAliveID
import imgui.ImGui.topMostPopupModal
import imgui.api.g
import imgui.static.findHoveredWindow

/** NewFrame */
internal interface newFrame {

    /** The reason this is exposed in imgui_internal.h is: on touch-based system that don't have hovering,
     *  we want to dispatch inputs to the right target (imgui vs imgui+app) */
    fun updateHoveredWindowAndCaptureFlags() {

        // Find the window hovered by mouse:
        // - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
        // - When moving a window we can skip the search, which also conveniently bypasses the fact that window->WindowRectClipped is lagging as this point of the frame.
        // - We also support the moved window toggling the NoInputs flag after moving has started in order to be able to detect windows below it, which is useful for e.g. docking mechanisms.
        var clearHoveredWindows = false
        findHoveredWindow()

        // Modal windows prevents mouse from hovering behind them.
        val modalWindow = topMostPopupModal
        val hovered = g.hoveredRootWindow
        if (modalWindow != null && hovered != null && !hovered.isChildOf(modalWindow))
            clearHoveredWindows = true
        // Disabled mouse?
        if (io.configFlags has ConfigFlag.NoMouse)
            clearHoveredWindows = true

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
            clearHoveredWindows = true

        if(clearHoveredWindows) {
            g.hoveredWindow = null
            g.hoveredRootWindow = null
            g.hoveredWindowUnderMovingWindow = null
        }

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
                }
                focusWindow(mov)
            } else {
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

        // Click on empty space to focus window and start moving (after we're done with all our widgets)
        if (io.mouseClicked[0]) {
            // Handle the edge case of a popup being closed while clicking in its empty space.
            // If we try to focus it, FocusWindow() > ClosePopupsOverWindow() will accidentally close any parent popups because they are not linked together any more.
            val rootWindow = g.hoveredRootWindow
            val isClosedPopup = rootWindow != null && rootWindow.flags has WindowFlag._Popup && !isPopupOpen(rootWindow.popupId, PopupFlag.AnyPopupLevel.i)

            if (rootWindow != null && !isClosedPopup) {
                g.hoveredWindow!!.startMouseMoving()

                // Cancel moving if clicked outside of title bar
                if (io.configWindowsMoveFromTitleBarOnly && rootWindow.flags hasnt WindowFlag.NoTitleBar)
                    if (io.mouseClickedPos[0] !in rootWindow.titleBarRect())
                        g.movingWindow = null

                // Cancel moving if clicked over an item which was disabled or inhibited by popups (note that we know HoveredId == 0 already)
                if (g.hoveredIdDisabled)
                    g.movingWindow = null
            }
            else if (rootWindow == null && g.navWindow != null && topMostPopupModal == null)
                focusWindow()  // Clicking on void disable focus
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