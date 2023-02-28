package imgui.internal.api

import glm_.has
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.data
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isAbove
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isPopupOpen
import imgui.ImGui.isWithinBeginStackOf
import imgui.ImGui.keepAliveID
import imgui.ImGui.setActiveIdUsingAllKeyboardKeys
import imgui.ImGui.setPos
import imgui.ImGui.topMostPopupModal
import imgui.api.g
import imgui.internal.BitArray
import imgui.internal.classes.DebugLogFlag
import imgui.internal.classes.Window
import imgui.internal.classes.has
import imgui.internal.sections.IMGUI_DEBUG_LOG_IO
import imgui.internal.sections.InputEvent
import imgui.static.findHoveredWindow

/** NewFrame */
internal interface newFrame {

    // Process input queue
    // We always call this with the value of 'bool g.IO.ConfigInputTrickleEventQueue'.
    // - trickle_fast_inputs = false : process all events, turn into flattened input state (e.g. successive down/up/down/up will be lost)
    // - trickle_fast_inputs = true  : process as many events as possible (successive down/up/down/up will be trickled over several frames so nothing is lost) (new feature in 1.87)
    fun updateInputEvents(trickleFastInputs: Boolean) {

        // Only trickle chars<>key when working with InputText()
        // FIXME: InputText() could parse event trail?
        // FIXME: Could specialize chars<>keys trickling rules for control keys (those not typically associated to characters)
        val trickleInterleavedKeysAndText = trickleFastInputs && g.wantTextInputNextFrame == 1

        var mouseMoved = false
        var mouseWheeled = false
        var keyChanged = false
        var textInputed = false
        var mouseButtonChanged = 0x00
        val keyChangedMask = BitArray(Key.COUNT)

        var eventN = 0
        while (eventN < g.inputEventsQueue.size) {
            val e = g.inputEventsQueue[eventN]
            when (e) {
                is InputEvent.MousePos -> {
                    // Trickling Rule: Stop processing queued events if we already handled a mouse button change
                    val eventPos = Vec2(e.posX, e.posY)
                    if (trickleFastInputs && (mouseButtonChanged != 0 || mouseWheeled || keyChanged || textInputed))
                        break
                    io.mousePos put eventPos
                    mouseMoved = true
                }
                is InputEvent.MouseButton -> {
                    // Trickling Rule: Stop processing queued events if we got multiple action on the same button
                    val button = MouseButton of e.button
                    //                    assert(button >= 0 && button < ImGuiMouseButton_COUNT)
                    if (trickleFastInputs && ((mouseButtonChanged has (1 shl button.i)) || mouseWheeled))
                        break
                    io.mouseDown[button.i] = e.down
                    mouseButtonChanged = mouseButtonChanged or (1 shl button.i)
                }
                is InputEvent.MouseWheel -> {
                    // Trickling Rule: Stop processing queued events if we got multiple action on the event
                    if (trickleFastInputs && (mouseMoved || mouseButtonChanged != 0))
                        break
                    io.mouseWheelH += e.wheelX
                    io.mouseWheel += e.wheelY
                    mouseWheeled = true
                }
                is InputEvent.Key -> {
                    // Trickling Rule: Stop processing queued events if we got multiple action on the same button
                    val key = e.key
                    assert(key != Key.None)
                    val keyData = key.data
                    val keyDataIndex = g.io.keysData.indexOf(keyData)
                    if (trickleFastInputs && keyData.down != e.down && (keyChangedMask testBit keyDataIndex || textInputed || mouseButtonChanged != 0))
                        break
                    keyData.down = e.down
                    keyData.analogValue = e.analogValue
                    keyChanged = true
                    keyChangedMask setBit keyDataIndex

                    // Allow legacy code using io.KeysDown[GetKeyIndex()] with new backends
                }
                is InputEvent.Text -> {
                    // Trickling Rule: Stop processing queued events if keys/mouse have been interacted with
                    if (trickleFastInputs && ((keyChanged && trickleInterleavedKeysAndText) || mouseButtonChanged != 0 || mouseMoved || mouseWheeled))
                        break
                    val c = e.char
                    io.inputQueueCharacters += if (c.code <= UNICODE_CODEPOINT_MAX) c else Char(UNICODE_CODEPOINT_INVALID)
                    if (trickleInterleavedKeysAndText)
                        textInputed = true
                }
                is InputEvent.AppFocused -> {
                    // We intentionally overwrite this and process in NewFrame(), in order to give a chance
                    // to multi-viewports backends to queue AddFocusEvent(false) + AddFocusEvent(true) in same frame.
                    val focusLost = !e.focused
                    io.appFocusLost = focusLost
                }
            }
            eventN++
        }

        // Record trail (for domain-specific applications wanting to access a precise trail)
        //if (event_n != 0) IMGUI_DEBUG_LOG_IO("Processed: %d / Remaining: %d\n", event_n, g.InputEventsQueue.Size - event_n);
        for (n in 0 until eventN)
            g.inputEventsTrail += g.inputEventsQueue[n]

        // [DEBUG]
        if (!IMGUI_DISABLE_DEBUG_TOOLS)
            if (eventN != 0 && g.debugLogFlags has DebugLogFlag.EventIO)
                for (n in g.inputEventsQueue.indices)
                    debugPrintInputEvent(if (n < eventN) "Processed" else "Remaining", g.inputEventsQueue[n])

        // Remaining events will be processed on the next frame
        if (eventN == g.inputEventsQueue.size)
            g.inputEventsQueue.clear()
        else
            for (i in 0 until eventN)
                g.inputEventsQueue.removeFirst()

        // Clear buttons state when focus is lost
        // - this is useful so e.g. releasing Alt after focus loss on Alt-Tab doesn't trigger the Alt menu toggle.
        // - we clear in EndFrame() and not now in order allow application/user code polling this flag
        //   (e.g. custom backend may want to clear additional data, custom widgets may want to react with a "canceling" event).
        if (g.io.appFocusLost)
            g.io.clearInputKeys()
    }

    /** The reason this is exposed in imgui_internal.h is: on touch-based system that don't have hovering,
     *  we want to dispatch inputs to the right target (imgui vs imgui+app) */
    fun updateHoveredWindowAndCaptureFlags() {

        g.windowsHoverPadding = g.style.touchExtraPadding max Vec2(WINDOWS_HOVER_PADDING)

        // Find the window hovered by mouse:
        // - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
        // - When moving a window we can skip the search, which also conveniently bypasses the fact that window->WindowRectClipped is lagging as this point of the frame.
        // - We also support the moved window toggling the NoInputs flag after moving has started in order to be able to detect windows below it, which is useful for e.g. docking mechanisms.
        var clearHoveredWindows = false
        findHoveredWindow()

        // Modal windows prevents mouse from hovering behind them.
        val modalWindow = topMostPopupModal
        val hovered = g.hoveredWindow
        if (modalWindow != null && hovered != null && !(hovered.rootWindow!!.isWithinBeginStackOf(modalWindow)))
            clearHoveredWindows = true
        // Disabled mouse?
        if (io.configFlags has ConfigFlag.NoMouse)
            clearHoveredWindows = true

        // We track click ownership. When clicked outside of a window the click is owned by the application and
        // won't report hovering nor request capture even while dragging over our windows afterward.
        val hasOpenPopup = g.openPopupStack.isNotEmpty()
        val hasOpenModal = modalWindow != null
        var mouseEarliestDown = -1
        var mouseAnyDown = false
        for (i in io.mouseDown.indices) {
            if (io.mouseClicked[i]) {
                io.mouseDownOwned[i] = g.hoveredWindow != null || hasOpenPopup
                io.mouseDownOwnedUnlessPopupClose[i] = g.hoveredWindow != null || hasOpenModal
            }
            mouseAnyDown = mouseAnyDown || io.mouseDown[i]
            if (io.mouseDown[i])
                if (mouseEarliestDown == -1 || io.mouseClickedTime[i] < io.mouseClickedTime[mouseEarliestDown])
                    mouseEarliestDown = i
        }
        val mouseAvail = mouseEarliestDown == -1 || io.mouseDownOwned[mouseEarliestDown]
        val mouseAvailUnlessPopupClose = mouseEarliestDown == -1 || io.mouseDownOwnedUnlessPopupClose[mouseEarliestDown]

        // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
        // FIXME: For patterns of drag and drop across OS windows, we may need to rework/remove this test (first committed 311c0ca9 on 2015/02)
        val mouseDraggingExternPayload = g.dragDropActive && g.dragDropSourceFlags has DragDropFlag.SourceExtern
        if (!mouseAvail && !mouseDraggingExternPayload)
            clearHoveredWindows = true

        if (clearHoveredWindows) {
            g.hoveredWindow = null
            g.hoveredWindowUnderMovingWindow = null
        }

        // Update io.WantCaptureMouse for the user application (true = dispatch mouse info to Dear ImGui only, false = dispatch mouse to Dear ImGui + underlying app)
        // Update io.WantCaptureMouseAllowPopupClose (experimental) to give a chance for app to react to popup closure with a drag
        if (g.wantCaptureMouseNextFrame != -1) {
            io.wantCaptureMouse = g.wantCaptureMouseNextFrame != 0; io.wantCaptureMouseUnlessPopupClose = g.wantCaptureMouseNextFrame != 0
        } else {
            io.wantCaptureMouse = (mouseAvail && (g.hoveredWindow != null || mouseAnyDown)) || hasOpenPopup
            io.wantCaptureMouseUnlessPopupClose = (mouseAvailUnlessPopupClose && (g.hoveredWindow != null || mouseAnyDown)) || hasOpenModal
        }

        // Update io.WantCaptureKeyboard for the user application (true = dispatch keyboard info to Dear ImGui only, false = dispatch keyboard info to Dear ImGui + underlying app)
        if (g.wantCaptureKeyboardNextFrame != -1)
            io.wantCaptureKeyboard = g.wantCaptureKeyboardNextFrame != 0
        else
            io.wantCaptureKeyboard = g.activeId != 0 || modalWindow != null
        if (io.navActive && io.configFlags has ConfigFlag.NavEnableKeyboard && io.configFlags hasnt ConfigFlag.NavNoCaptureKeyboard)
            io.wantCaptureKeyboard = true

        // Update io.WantTextInput flag, this is to allow systems without a keyboard (e.g. mobile, hand-held) to show a software keyboard if possible
        io.wantTextInput = if (g.wantTextInputNextFrame != -1) g.wantTextInputNextFrame != 0 else false
    }

    /** ~ StartMouseMovingWindow */
    fun Window.startMouseMoving() {
        // Set ActiveId even if the _NoMove flag is set. Without it, dragging away from a window with _NoMove would activate hover on other windows.
        // We _also_ call this when clicking in a window empty space when io.ConfigWindowsMoveFromTitleBarOnly is set, but clear g.MovingWindow afterward.
        // This is because we want ActiveId to be set even when the window is not permitted to move.
        focusWindow(this)
        ImGui.setActiveID(moveId, this)
        g.navDisableHighlight = true
        g.activeIdClickOffset = g.io.mouseClickedPos[0] - rootWindow!!.pos
        g.activeIdNoClearOnFocusLoss = true
        setActiveIdUsingAllKeyboardKeys()

        val canMoveWindow = flags hasnt WindowFlag.NoMove && rootWindow!!.flags hasnt WindowFlag.NoMove
        if (canMoveWindow) g.movingWindow = this
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
                movingWindow.setPos(pos, Cond.Always)
                focusWindow(mov)
            } else {
                g.movingWindow = null
                clearActiveID()
            }
        } else
        // When clicking/dragging from a window that has the _NoMove flag, we still set the ActiveId in order to prevent hovering others.
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

        // Click on empty space to focus window and start moving
        // (after we're done with all our widgets)
        if (io.mouseClicked[0]) {
            // Handle the edge case of a popup being closed while clicking in its empty space.
            // If we try to focus it, FocusWindow() > ClosePopupsOverWindow() will accidentally close any parent popups because they are not linked together any more.
            val rootWindow = g.hoveredWindow?.rootWindow
            val isClosedPopup = rootWindow != null && rootWindow.flags has WindowFlag._Popup && !isPopupOpen(rootWindow.popupId, PopupFlag.AnyPopupLevel.i)

            if (rootWindow != null && !isClosedPopup) {
                g.hoveredWindow!!.startMouseMoving() //-V595

                // Cancel moving if clicked outside of title bar
                if (io.configWindowsMoveFromTitleBarOnly && rootWindow.flags hasnt WindowFlag.NoTitleBar)
                    if (io.mouseClickedPos[0] !in rootWindow.titleBarRect())
                        g.movingWindow = null

                // Cancel moving if clicked over an item which was disabled or inhibited by popups (note that we know HoveredId == 0 already)
                if (g.hoveredIdDisabled)
                    g.movingWindow = null
            } else if (rootWindow == null && g.navWindow != null && topMostPopupModal == null)
                focusWindow()  // Clicking on void disable focus
        }

        // With right mouse button we close popups without changing focus based on where the mouse is aimed
        // Instead, focus will be restored to the window under the bottom-most closed popup.
        // (The left mouse button path calls FocusWindow on the hovered window, which will lead NewFrame->ClosePopupsOverWindow to trigger)
        if (io.mouseClicked[1]) {
            // Find the top-most window between HoveredWindow and the top-most Modal Window.
            // This is where we can trim the popup stack.
            val modal = topMostPopupModal
            val hoveredWindow = g.hoveredWindow
            val hoveredWindowAboveModal = hoveredWindow != null && (modal == null || hoveredWindow isAbove modal)
            closePopupsOverWindow(if (hoveredWindowAboveModal) g.hoveredWindow else modal, true)
        }
    }

    companion object {
        fun debugPrintInputEvent(prefix: String, e: InputEvent) = when (e) {
            is InputEvent.MousePos -> when {
                e.posX == -Float.MAX_VALUE && e.posY == -Float.MAX_VALUE -> IMGUI_DEBUG_LOG_IO("%s: MousePos (-FLT_MAX, -FLT_MAX)\n", prefix)
                else -> IMGUI_DEBUG_LOG_IO("$prefix: MousePos (%.1f, %.1f)", e.posX, e.posY)
            }
            is InputEvent.MouseButton -> IMGUI_DEBUG_LOG_IO("$prefix: MouseButton ${e.button} ${if (e.down) "Down" else "Up"}")
            is InputEvent.MouseWheel -> IMGUI_DEBUG_LOG_IO("$prefix: MouseWheel (%.3f, %.3f)", e.wheelX, e.wheelY)
            is InputEvent.Key -> IMGUI_DEBUG_LOG_IO("$prefix: Key \"${e.key.name}\" ${if (e.down) "Down" else "Up"}")
            is InputEvent.Text -> IMGUI_DEBUG_LOG_IO("$prefix: Text: ${e.char} (U+%08X)", e.char.code)
            is InputEvent.AppFocused -> IMGUI_DEBUG_LOG_IO("$prefix: AppFocused ${e.focused.i}")
        }
    }
}