package imgui.imgui

import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.internal.clearActiveID
import imgui.internal.focusWindow
import imgui.internal.keepAliveID
import imgui.internal.lengthSqr
import imgui.Context as g

fun newFrame() {

    // Check user data
    assert(IO.deltaTime >= 0.0f)               // Need a positive DeltaTime (zero is tolerated but will cause some timing issues)
    assert(IO.displaySize.x >= 0.0f && IO.displaySize.y >= 0.0f)
    assert(IO.fonts.fonts.size > 0)           // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
    assert(IO.fonts.fonts[0].isLoaded)     // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
    assert(Style.curveTessellationTol > 0.0f)  // Invalid style setting

    if (!g.initialized) {
        // Initialize on first frame TODO
//        g.LogClipboard = (ImGuiTextBuffer*)ImGui::MemAlloc(sizeof(ImGuiTextBuffer))
//        IM_PLACEMENT_NEW(g.LogClipboard) ImGuiTextBuffer()
//
//        IM_ASSERT(g.Settings.empty())
//        LoadIniSettingsFromDisk(g.IO.IniFilename)
//        g.Initialized = true
    }

    setCurrentFont(getDefaultFont())
    assert(g.font.isLoaded)

    g.time += IO.deltaTime
    g.frameCount += 1
    g.tooltip = ""
    g.overlayDrawList.clear()
    g.overlayDrawList.pushTextureID(IO.fonts.texID)
    g.overlayDrawList.pushClipRectFullScreen()

    // Mark rendering data as invalid to prevent user who may have a handle on it to use it
    g.renderDrawData.valid = false
    g.renderDrawData.cmdLists.clear()
    g.renderDrawData.totalIdxCount = 0
    g.renderDrawData.totalVtxCount = 0
    g.renderDrawData.cmdListsCount = 0

    // Update inputs state
    if (IO.mousePos.x < 0 && IO.mousePos.y < 0)
        IO.mousePos put -9999.0f
    // if mouse just appeared or disappeared (negative coordinate) we cancel out movement in MouseDelta
    if ((IO.mousePos.x < 0 && IO.mousePos.y < 0) || (IO.mousePosPrev.x < 0 && IO.mousePosPrev.y < 0))
        IO.mouseDelta put 0f
    else
        IO.mouseDelta = IO.mousePos - IO.mousePosPrev
    IO.mousePosPrev = IO.mousePos

    for (i in IO.mouseDown.indices) {
        IO.mouseClicked[i] = IO.mouseDown[i] && IO.mouseDownDuration[i] < 0f
        IO.mouseReleased[i] = !IO.mouseDown[i] && IO.mouseDownDuration[i] >= 0f
        IO.mouseDownDurationPrev[i] = IO.mouseDownDuration[i]
        IO.mouseDownDuration[i] =
                if (IO.mouseDown[i]) {
                    if (IO.mouseDownDuration[i] < 0f) 0f
                    else IO.mouseDownDuration[i] + IO.deltaTime
                } else -1.0f
        IO.mouseDoubleClicked[i] = false
        if (IO.mouseClicked[i]) {
            if (g.time - IO.mouseClickedTime[i] < IO.mouseDoubleClickTime) {
                if (lengthSqr(IO.mousePos - IO.mouseClickedPos[i]) < IO.mouseDoubleClickMaxDist * IO.mouseDoubleClickMaxDist)
                    IO.mouseDoubleClicked[i] = true
                IO.mouseClickedTime[i] = -Float.MAX_VALUE   // so the third click isn't turned into a double-click
            } else
                IO.mouseClickedTime[i] = g.time
            IO.mouseClickedPos[i] = IO.mousePos
            IO.mouseDragMaxDistanceSqr[i] = 0f
        } else if (IO.mouseDown[i])
            IO.mouseDragMaxDistanceSqr[i] = glm.max(IO.mouseDragMaxDistanceSqr[i], lengthSqr(IO.mousePos - IO.mouseClickedPos[i]))
    }
    for (i in IO.keysDownDuration.indices)
        IO.keysDownDurationPrev[i] = IO.keysDownDuration[i]
    for (i in IO.keysDown.indices)
        IO.keysDownDuration[i] =
                if (IO.keysDown[i])
                    if (IO.keysDownDuration[i] < 0f) 0f
                    else IO.keysDownDuration[i] + IO.deltaTime
                else -1f

    // Calculate frame-rate for the user, as a purely luxurious feature
    g.framerateSecPerFrameAccum += IO.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
    g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = IO.deltaTime
    g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
    IO.framerate = 1.0f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size)

    // Clear reference to active widget if the widget isn't alive anymore
    g.hoveredIdPreviousFrame = g.hoveredId
    g.hoveredId = 0
    g.hoveredIdAllowOverlap = false
    if (!g.activeIdIsAlive && g.activeIdPreviousFrame == g.activeId && g.activeId != 0)
        clearActiveID()
    g.activeIdPreviousFrame = g.activeId
    g.activeIdIsAlive = false
    g.activeIdIsJustActivated = false

    // Handle user moving window (at the beginning of the frame to avoid input lag or sheering). Only valid for root windows.
    if (g.movedWindowMoveId != 0 && g.movedWindowMoveId == g.activeId) {
        keepAliveID(g.movedWindowMoveId)
        assert(g.movedWindow != null)
        assert(g.movedWindow!!.rootWindow.moveId == g.movedWindowMoveId)
        if (IO.mouseDown[0]) {
            if (g.movedWindow!!.flags hasnt WindowFlags_.NoMove) {
                g.movedWindow!!.posF plus_ IO.mouseDelta
                if (g.movedWindow!!.flags hasnt WindowFlags_.NoSavedSettings && (IO.mouseDelta.x != 0.0f || IO.mouseDelta.y != 0.0f))
                    markIniSettingsDirty()
            }
            focusWindow(g.movedWindow)
        } else {
            clearActiveID()
            g.movedWindow = null
            g.movedWindowMoveId = 0
        }
    } else {
        g.movedWindow = null
        g.movedWindowMoveId = 0
    }

    // Delay saving settings so we don't spam disk too much
    if (g.settingsDirtyTimer > 0.0f) {
        g.settingsDirtyTimer -= IO.deltaTime
        if (g.settingsDirtyTimer <= 0.0f)
            TODO() //SaveIniSettingsToDisk(g.IO.IniFilename)
    }

    // Find the window we are hovering. Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow
    g.hoveredWindow = g.movedWindow ?: findHoveredWindow(IO.mousePos, false)
    if (g.hoveredWindow != null && g.hoveredWindow!!.flags has WindowFlags_.ChildWindow)
        g.hoveredRootWindow = g.hoveredWindow!!.rootWindow
    else
        g.hoveredRootWindow = g.movedWindow?.rootWindow ?: findHoveredWindow(IO.mousePos, true)

    val modalWindow = getFrontMostModalRootWindow()
    if (modalWindow != null) {
        g.modalWindowDarkeningRatio = glm.min(g.modalWindowDarkeningRatio + IO.deltaTime * 6f, 1f)
        var window = g.hoveredRootWindow
        while (window != null && window != modalWindow)
            window = window.parentWindow
        if (window == null)
            g.hoveredWindow = null
        g.hoveredRootWindow = null
    } else
        g.modalWindowDarkeningRatio = 0.0f

    /** Are we using inputs? Tell user so they can capture/discard the inputs away from the rest of their application.
    When clicking outside of a window we assume the click is owned by the application and won't request capture.
    We need to track click ownership.   */
    var mouseEarliestButtonDown = -1
    var mouseAnyDown = false
    for (i in IO.mouseDown.indices) {
        if (IO.mouseClicked[i])
            IO.mouseDownOwned[i] = g.hoveredWindow != null || g.openPopupStack.isNotEmpty()
        mouseAnyDown = mouseAnyDown || IO.mouseDown[i]
        if (IO.mouseDown[i])
            if (mouseEarliestButtonDown == -1 || IO.mouseClickedTime[mouseEarliestButtonDown] > IO.mouseClickedTime[i])
                mouseEarliestButtonDown = i
    }
    val mouseAvailToImgui = mouseEarliestButtonDown == -1 || IO.mouseDownOwned[mouseEarliestButtonDown]
    if (g.captureMouseNextFrame != -1)
        IO.wantCaptureMouse = g.captureMouseNextFrame != 0
    else
        IO.wantCaptureMouse = (mouseAvailToImgui && (g.hoveredWindow != null || mouseAnyDown)) || g.activeId != 0 ||
                g.openPopupStack.isNotEmpty()
    IO.wantCaptureKeyboard =
            if (g.captureKeyboardNextFrame != -1) g.captureKeyboardNextFrame != 0
            else g.activeId != 0
    IO.wantTextInput = g.activeId != 0 //&& g.inputTextState.id == g.activeId TODO
    g.mouseCursor = MouseCursor_.Arrow
    g.captureKeyboardNextFrame = -1
    g.captureMouseNextFrame = -1
    g.osImePosRequest put 1f // OS Input Method Editor showing on top-left of our window by default

    // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
    if (!mouseAvailToImgui) {
        g.hoveredRootWindow = null
        g.hoveredWindow = null
    }

    // Scale & Scrolling
    if (g.hoveredWindow != null && IO.mouseWheel != 0f && !g.hoveredWindow!!.collapsed) {
        val window = g.hoveredWindow!!
        if (IO.keyCtrl) {
            if (IO.fontAllowUserScaling) {
                // Zoom / Scale window
                val newFontScale = glm.clamp(window.fontWindowScale + IO.mouseWheel * 0.10f, 0.50f, 2.50f)
                val scale = newFontScale / window.fontWindowScale
                window.fontWindowScale = newFontScale

                val offset = window.size * (1.0f - scale) * (IO.mousePos - window.pos) / window.size
                window.pos plus_ offset
                window.posF plus_ offset
                window.size times_ scale
                window.sizeFull times_ scale
            }
        } else if (window.flags hasnt WindowFlags_.NoScrollWithMouse) {
            // Scroll
            val scrollLines = if (window.flags has WindowFlags_.ComboBox) 3 else 5
            setWindowScrollY(window, window.scroll.y - IO.mouseWheel * window.calcFontSize() * scrollLines)
        }
    }

    /*  Pressing TAB activate widget focus
        NB: Don't discard FocusedWindow if it isn't active, so that a window that go on/off programatically won't lose
        its keyboard focus.     */
    if (g.activeId == 0 && g.focusedWindow != null && g.focusedWindow!!.active && isKeyPressedMap(Key_.Tab, false))
        g.focusedWindow!!.focusIdxTabRequestNext = 0

    // Mark all windows as not visible
    var i = 0
    while (i != g.windows.size) {
        val window = g.windows[i]
        window.wasActive = window.active
        window.active = false
        window.accessed = false
        i++
    }

    // Closing the focused window restore focus to the first active root window in descending z-order
    if (g.focusedWindow != null && !g.focusedWindow!!.wasActive)
        for (i in g.windows.size - 1 downTo 0)
            if (g.windows[i].wasActive && g.windows[i].flags hasnt WindowFlags_.ChildWindow) {
                focusWindow(g.windows[i])
                break
            }

    /*  No window should be open at the beginning of the frame.
        But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an
        explicit clear. */
    g.currentWindowStack.clear()
    g.currentPopupStack.clear()
    closeInactivePopups()

    // Create implicit window - we will only render it if the user has added something to it.
    setNextWindowSize(Vec2(400, 400), SetCond_.FirstUseEver)
    begin("Debug")
}