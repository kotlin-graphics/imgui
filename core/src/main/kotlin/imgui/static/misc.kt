package imgui.static

import gli_.has
import glm_.glm
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.clearActiveID
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.getStyleColorVec4
import imgui.ImGui.io
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMousePosValid
import imgui.ImGui.loadIniSettingsFromDisk
import imgui.ImGui.mouseCursor
import imgui.ImGui.parseFormatFindEnd
import imgui.ImGui.parseFormatFindStart
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.saveIniSettingsToDisk
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.api.g
import imgui.dsl.tooltip
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.classes.Window.Companion.resizeGripDef
import imgui.internal.sections.*
import kotlin.math.max
import kotlin.math.min


//-----------------------------------------------------------------------------
// [SECTION] SETTINGS
//-----------------------------------------------------------------------------

/** Called by NewFrame() */
fun updateSettings() {
    // Load settings on first frame (if not explicitly loaded manually before)
    if (!g.settingsLoaded) {
        assert(g.settingsWindows.isEmpty())
        io.iniFilename?.let(::loadIniSettingsFromDisk)
        g.settingsLoaded = true
    }

    // Save settings (with a delay after the last modification, so we don't spam disk too much)
    if (g.settingsDirtyTimer > 0f) {
        g.settingsDirtyTimer -= io.deltaTime
        if (g.settingsDirtyTimer <= 0f) {
            io.iniFilename.let {
                if (it != null)
                    saveIniSettingsToDisk(it)
                else
                    io.wantSaveIniSettings = true  // Let user know they can call SaveIniSettingsToMemory(). user will need to clear io.WantSaveIniSettings themselves.
            }
            g.settingsDirtyTimer = 0f
        }
    }
}

fun updateMouseInputs() {

    with(io) {

        // Round mouse position to avoid spreading non-rounded position (e.g. UpdateManualResize doesn't support them well)
        if (isMousePosValid(mousePos)) {
            g.lastValidMousePos = floor(mousePos)
            mousePos = Vec2(g.lastValidMousePos)
        }

        // If mouse just appeared or disappeared (usually denoted by -FLT_MAX component) we cancel out movement in MouseDelta
        if (isMousePosValid(mousePos) && isMousePosValid(mousePosPrev))
            mouseDelta = mousePos - mousePosPrev
        else
            mouseDelta put 0f
        if (mouseDelta.x != 0f || mouseDelta.y != 0f)
            g.navDisableMouseHover = false

        mousePosPrev put mousePos
        for (i in mouseDown.indices) {
            mouseClicked[i] = mouseDown[i] && mouseDownDuration[i] < 0f
            mouseReleased[i] = !mouseDown[i] && mouseDownDuration[i] >= 0f
            mouseDownDurationPrev[i] = mouseDownDuration[i]
            mouseDownDuration[i] = when {
                mouseDown[i] -> when {
                    mouseDownDuration[i] < 0f -> 0f
                    else -> mouseDownDuration[i] + deltaTime
                }
                else -> -1f
            }
            mouseDoubleClicked[i] = false
            if (mouseClicked[i]) {
                if (g.time - mouseClickedTime[i] < mouseDoubleClickTime) {
                    val deltaFromClickPos = when {
                        isMousePosValid(io.mousePos) -> io.mousePos - io.mouseClickedPos[i]
                        else -> Vec2()
                    }
                    if (deltaFromClickPos.lengthSqr < io.mouseDoubleClickMaxDist * io.mouseDoubleClickMaxDist)
                        mouseDoubleClicked[i] = true
                    mouseClickedTime[i] = -io.mouseDoubleClickTime * 2.0 // Mark as "old enough" so the third click isn't turned into a double-click
                } else
                    mouseClickedTime[i] = g.time
                mouseClickedPos[i] put mousePos
                mouseDownWasDoubleClick[i] = mouseDoubleClicked[i]
                mouseDragMaxDistanceAbs[i] put 0f
                mouseDragMaxDistanceSqr[i] = 0f
            } else if (mouseDown[i]) {
                // Maintain the maximum distance we reaching from the initial click position, which is used with dragging threshold
                val deltaFromClickPos = when {
                    isMousePosValid(io.mousePos) -> io.mousePos - io.mouseClickedPos[i]
                    else -> Vec2()
                }
                io.mouseDragMaxDistanceSqr[i] = io.mouseDragMaxDistanceSqr[i] max deltaFromClickPos.lengthSqr
                io.mouseDragMaxDistanceAbs[i].x = io.mouseDragMaxDistanceAbs[i].x max when {
                    deltaFromClickPos.x < 0f -> -deltaFromClickPos.x
                    else -> deltaFromClickPos.x
                }
                io.mouseDragMaxDistanceAbs[i].y = io.mouseDragMaxDistanceAbs[i].y max when {
                    deltaFromClickPos.y < 0f -> -deltaFromClickPos.y
                    else -> deltaFromClickPos.y
                }
                val mouseDelta = mousePos - mouseClickedPos[i]
                mouseDragMaxDistanceAbs[i].x = mouseDragMaxDistanceAbs[i].x max if (mouseDelta.x < 0f) -mouseDelta.x else mouseDelta.x
                mouseDragMaxDistanceAbs[i].y = mouseDragMaxDistanceAbs[i].y max if (mouseDelta.y < 0f) -mouseDelta.y else mouseDelta.y
                mouseDragMaxDistanceSqr[i] = mouseDragMaxDistanceSqr[i] max mouseDelta.lengthSqr
            }
            if (!mouseDown[i] && !mouseReleased[i])
                mouseDownWasDoubleClick[i] = false
            // Clicking any mouse button reactivate mouse hovering which may have been deactivated by gamepad/keyboard navigation
            if (mouseClicked[i])
                g.navDisableMouseHover = false
        }
    }
}

fun updateMouseWheel() {

    // Reset the locked window if we move the mouse or after the timer elapses
    if (g.wheelingWindow != null) {
        g.wheelingWindowTimer -= io.deltaTime
        if (isMousePosValid() && (io.mousePos - g.wheelingWindowRefMousePos).lengthSqr > io.mouseDragThreshold * io.mouseDragThreshold)
            g.wheelingWindowTimer = 0f
        if (g.wheelingWindowTimer <= 0f) {
            g.wheelingWindow = null
            g.wheelingWindowTimer = 0f
        }
    }

    if (io.mouseWheel == 0f && io.mouseWheelH == 0f)
        return

    var window = g.wheelingWindow ?: g.hoveredWindow
    if (window == null || window.collapsed)
        return

    // Zoom / Scale window
    // FIXME-OBSOLETE: This is an old feature, it still works but pretty much nobody is using it and may be best redesigned.
    if (io.mouseWheel != 0f && io.keyCtrl && io.fontAllowUserScaling) {
        window.startLockWheeling()
        val newFontScale = glm.clamp(window.fontWindowScale + io.mouseWheel * 0.1f, 0.5f, 2.5f)
        val scale = newFontScale / window.fontWindowScale
        window.fontWindowScale = newFontScale
        if (window.flags hasnt WindowFlag._ChildWindow) {
            val offset = window.size * (1f - scale) * (io.mousePos - window.pos) / window.size
            window.setPos(window.pos + offset)
            window.size = floor(window.size * scale)
            window.sizeFull = floor(window.sizeFull * scale)
        }
        return
    }

    // Mouse wheel scrolling
    // If a child window has the ImGuiWindowFlags_NoScrollWithMouse flag, we give a chance to scroll its parent

    // Vertical Mouse Wheel scrolling
    val wheelY = if (io.mouseWheel != 0f && !io.keyShift) io.mouseWheel else 0f
    if (wheelY != 0f && !io.keyCtrl) {
        window.startLockWheeling()
        tailrec fun Window.getParent(): Window = when {
            flags has WindowFlag._ChildWindow && (scrollMax.y == 0f || (flags has WindowFlag.NoScrollWithMouse && flags hasnt WindowFlag.NoMouseInputs)) -> parentWindow!!.getParent()
            else -> this
        }
        window = g.hoveredWindow!!.getParent()
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            val maxStep = window.innerRect.height * 0.67f
            val scrollStep = floor((5 * window.calcFontSize()) min maxStep)
            window.setScrollY(window.scroll.y - wheelY * scrollStep)
        }
    }

    // Horizontal Mouse Wheel scrolling, or Vertical Mouse Wheel w/ Shift held
    val wheelX = when {
        io.mouseWheelH != 0f && !io.keyShift -> io.mouseWheelH
        io.mouseWheel != 0f && io.keyShift -> io.mouseWheel
        else -> 0f
    }
    if (wheelX != 0f && !io.keyCtrl) {
        window.startLockWheeling()
        tailrec fun Window.getParent(): Window = when {
            flags has WindowFlag._ChildWindow && (scrollMax.x == 0f || (flags has WindowFlag.NoScrollWithMouse && flags hasnt WindowFlag.NoMouseInputs)) -> parentWindow!!.getParent()
            else -> this
        }
        window = g.hoveredWindow!!.getParent()
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            val maxStep = window.innerRect.width * 0.67f
            val scrollStep = floor((2 * window.calcFontSize()) min maxStep)
            window.setScrollX(window.scroll.x - wheelX * scrollStep)
        }
    }
}

/** Handle resize for: Resize Grips, Borders, Gamepad
 * @return [JVM] borderHelf to Boolean   */
fun updateWindowManualResize(
        window: Window, sizeAutoFit: Vec2, borderHeld_: Int, resizeGripCount: Int,
        resizeGripCol: IntArray, visibilityRect: Rect,
): Pair<Int, Boolean> {

    var borderHeld = borderHeld_

    val flags = window.flags

    if (flags has WindowFlag.NoResize || flags has WindowFlag.AlwaysAutoResize || window.autoFitFrames anyGreaterThan 0)
        return borderHeld to false
    if (!window.wasActive) // Early out to avoid running this code for e.g. an hidden implicit/fallback Debug window.
        return borderHeld to false

    var retAutoFit = false
    val resizeBorderCount = if (io.configWindowsResizeFromEdges) 4 else 0
    val gripDrawSize = floor(max(g.fontSize * 1.35f, window.windowRounding + 1f + g.fontSize * 0.2f))
    val gripHoverInnerSize = floor(gripDrawSize * 0.75f)
    val gripHoverOuterSize = if (io.configWindowsResizeFromEdges) WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS else 0f

    val posTarget = Vec2(Float.MAX_VALUE)
    val sizeTarget = Vec2(Float.MAX_VALUE)

    // Resize grips and borders are on layer 1
    window.dc.navLayerCurrent = NavLayer.Menu

    // Manual resize grips
    pushID("#RESIZE")
    for (resizeGripN in 0 until resizeGripCount) {

        val grip = resizeGripDef[resizeGripN]
        val corner = window.pos.lerp(window.pos + window.size, grip.cornerPosN)

        // Using the FlattenChilds button flag we make the resize button accessible even if we are hovering over a child window
        val resizeRect = Rect(corner - grip.innerDir * gripHoverOuterSize, corner + grip.innerDir * gripHoverInnerSize)
        if (resizeRect.min.x > resizeRect.max.x) swap(resizeRect.min::x, resizeRect.max::x)
        if (resizeRect.min.y > resizeRect.max.y) swap(resizeRect.min::y, resizeRect.max::y)

        val f = ButtonFlag.FlattenChildren or ButtonFlag.NoNavFocus
        val (_, hovered, held) = buttonBehavior(resizeRect, window.getID(resizeGripN), f)
        //GetOverlayDrawList(window)->AddRect(resize_rect.Min, resize_rect.Max, IM_COL32(255, 255, 0, 255));
        if (hovered || held)
            g.mouseCursor = if (resizeGripN has 1) MouseCursor.ResizeNESW else MouseCursor.ResizeNWSE

        if (held && g.io.mouseDoubleClicked[0] && resizeGripN == 0) {
            // Manual auto-fit when double-clicking
            sizeTarget put window.calcSizeAfterConstraint(sizeAutoFit)
            retAutoFit = true
            clearActiveID()
        } else if (held) {
            // Resize from any of the four corners
            // We don't use an incremental MouseDelta but rather compute an absolute target size based on mouse position
            // Corner of the window corresponding to our corner grip
            var cornerTarget = g.io.mousePos - g.activeIdClickOffset + (grip.innerDir * gripHoverOuterSize).lerp(grip.innerDir * -gripHoverInnerSize, grip.cornerPosN)
            val clampMin = Vec2 { if (grip.cornerPosN[it] == 1f) visibilityRect.min[it] else -Float.MAX_VALUE }
            val clampMax = Vec2 { if (grip.cornerPosN[it] == 0f) visibilityRect.max[it] else Float.MAX_VALUE }
            cornerTarget = glm.clamp(cornerTarget, clampMin, clampMax)
            window.calcResizePosSizeFromAnyCorner(cornerTarget, grip.cornerPosN, posTarget, sizeTarget)
        }
        if (resizeGripN == 0 || held || hovered)
            resizeGripCol[resizeGripN] = (if (held) Col.ResizeGripActive else if (hovered) Col.ResizeGripHovered else Col.ResizeGrip).u32
    }
    for (borderN in 0 until resizeBorderCount) {
        val borderRect = window.getResizeBorderRect(borderN, gripHoverInnerSize, WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS)
        val (_, hovered, held) = buttonBehavior(borderRect, window.getID((borderN + 4)), ButtonFlag.FlattenChildren)
        //GetOverlayDrawList(window)->AddRect(border_rect.Min, border_rect.Max, IM_COL32(255, 255, 0, 255));
        if ((hovered && g.hoveredIdTimer > WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER) || held) {
            g.mouseCursor = if (borderN has 1) MouseCursor.ResizeEW else MouseCursor.ResizeNS
            if (held)
                borderHeld = borderN
        }
        if (held) {
            var borderTarget = Vec2(window.pos)
            val borderPosN = when (borderN) {
                0 -> {
                    borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                    Vec2(0, 0)
                }
                1 -> {
                    borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                    Vec2(1, 0)
                }
                2 -> {
                    borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                    Vec2(0, 1)
                }
                3 -> {
                    borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                    Vec2(0, 0)
                }
                else -> Vec2(0, 0)
            }
            val clampMin = Vec2 { if (borderN == it + 1) visibilityRect.min[it] else -Float.MAX_VALUE }
            val clampMax = Vec2 { if (borderN == (if (it == 0) 3 else 0)) visibilityRect.max[it] else Float.MAX_VALUE }
            borderTarget = glm.clamp(borderTarget, clampMin, clampMax)
            window.calcResizePosSizeFromAnyCorner(borderTarget, borderPosN, posTarget, sizeTarget)
        }
    }
    popID()

    // Restore nav layer
    window.dc.navLayerCurrent = NavLayer.Main

    // Navigation resize (keyboard/gamepad)
    if (g.navWindowingTarget?.rootWindow === window) {
        val navResizeDelta = Vec2()
        if (g.navInputSource == InputSource.NavKeyboard && g.io.keyShift)
            navResizeDelta put getNavInputAmount2d(NavDirSourceFlag.Keyboard.i, InputReadMode.Down)
        if (g.navInputSource == InputSource.NavGamepad)
            navResizeDelta put getNavInputAmount2d(NavDirSourceFlag.PadDPad.i, InputReadMode.Down)
        if (navResizeDelta.x != 0f || navResizeDelta.y != 0f) {
            val NAV_RESIZE_SPEED = 600f
            navResizeDelta *= floor(NAV_RESIZE_SPEED * g.io.deltaTime * min(g.io.displayFramebufferScale.x, g.io.displayFramebufferScale.y))
            navResizeDelta put glm.max(navResizeDelta, visibilityRect.min - window.pos - window.size)
            g.navWindowingToggleLayer = false
            g.navDisableMouseHover = true
            resizeGripCol[0] = Col.ResizeGripActive.u32
            // FIXME-NAV: Should store and accumulate into a separate size buffer to handle sizing constraints properly, right now a constraint will make us stuck.
            sizeTarget put window.calcSizeAfterConstraint(window.sizeFull + navResizeDelta)
        }
    }

    // Apply back modified position/size to window
    if (sizeTarget.x != Float.MAX_VALUE) {
        window.sizeFull put sizeTarget
        window.markIniSettingsDirty()
    }
    if (posTarget.x != Float.MAX_VALUE) {
        window.pos = floor(posTarget)
        window.markIniSettingsDirty()
    }

    window.size put window.sizeFull

    return borderHeld to retAutoFit
}

fun updateTabFocus() {

    // Pressing TAB activate widget focus
    g.focusTabPressed = g.navWindow?.let { it.active && it.flags hasnt WindowFlag.NoNavInputs && !io.keyCtrl && Key.Tab.isPressed }
            ?: false
    if (g.activeId == 0 && g.focusTabPressed) {
        // Note that SetKeyboardFocusHere() sets the Next fields mid-frame. To be consistent we also
        // manipulate the Next fields even, even though they will be turned into Curr fields by the code below.
        g.focusRequestNextWindow = g.navWindow
        g.focusRequestNextCounterRegular = Int.MAX_VALUE
        g.focusRequestNextCounterTabStop = when {
            g.navId != 0 && g.navIdTabCounter != Int.MAX_VALUE -> g.navIdTabCounter + 1 + if (io.keyShift) -1 else 1
            else -> if (io.keyShift) -1 else 0
        }
    }

    // Turn queued focus request into current one
    g.focusRequestCurrWindow = null
    g.focusRequestCurrCounterRegular = Int.MAX_VALUE
    g.focusRequestCurrCounterTabStop = Int.MAX_VALUE
    g.focusRequestNextWindow?.let { window ->
        g.focusRequestCurrWindow = window
        if (g.focusRequestNextCounterRegular != Int.MAX_VALUE && window.dc.focusCounterRegular != -1)
            g.focusRequestCurrCounterRegular = modPositive(g.focusRequestNextCounterRegular, window.dc.focusCounterRegular + 1)
        if (g.focusRequestNextCounterTabStop != Int.MAX_VALUE && window.dc.focusCounterTabStop != -1)
            g.focusRequestCurrCounterTabStop = modPositive(g.focusRequestNextCounterTabStop, window.dc.focusCounterTabStop + 1)
        g.focusRequestNextWindow = null
        g.focusRequestNextCounterRegular = Int.MAX_VALUE
        g.focusRequestNextCounterTabStop = Int.MAX_VALUE
    }

    g.navIdTabCounter = Int.MAX_VALUE
}

/** [DEBUG] Item picker tool - start with DebugStartItemPicker() - useful to visually select an item and break into its call-stack. */
fun updateDebugToolItemPicker() {

    g.debugItemPickerBreakId = 0
    if (g.debugItemPickerActive) {

        val hoveredId = g.hoveredIdPreviousFrame
        mouseCursor = MouseCursor.Hand
        if (Key.Escape.isPressed)
            g.debugItemPickerActive = false
        if (isMouseClicked(MouseButton.Left) && hoveredId != 0) {
            g.debugItemPickerBreakId = hoveredId
            g.debugItemPickerActive = false
        }
        setNextWindowBgAlpha(0.6f)
        tooltip {
            text("HoveredId: 0x%08X", hoveredId)
            text("Press ESC to abort picking.")
            textColored(getStyleColorVec4(if (hoveredId != 0) Col.Text else Col.TextDisabled), "Click to break in debugger!")
        }
    }
}

// truly miscellaneous

// FIXME-OPT O(N)
fun findWindowNavFocusable(iStart: Int, iStop: Int, dir: Int): Window? {
    var i = iStart
    while (i in g.windowsFocusOrder.indices && i != iStop) {
        if (g.windowsFocusOrder[i].isNavFocusable)
            return g.windowsFocusOrder[i]
        i += dir
    }
    return null
}

fun navUpdateWindowingHighlightWindow(focusChangeDir: Int) {

    val target = g.navWindowingTarget!!
    if (target.flags has WindowFlag._Modal) return

    val iCurrent = findWindowFocusIndex(target)
    val windowTarget = findWindowNavFocusable(iCurrent + focusChangeDir, -Int.MAX_VALUE, focusChangeDir)
            ?: findWindowNavFocusable(if (focusChangeDir < 0) g.windowsFocusOrder.lastIndex else 0, iCurrent, focusChangeDir)
    // Don't reset windowing target if there's a single window in the list
    windowTarget?.let {
        g.navWindowingTarget = it
        g.navWindowingTargetAnim = it
    }
    g.navWindowingToggleLayer = false
}

/** FIXME-LEGACY: Prior to 1.61 our DragInt() function internally used floats and because of this the compile-time default value
 *  for format was "%.0f".
 *  Even though we changed the compile-time default, we expect users to have carried %f around, which would break
 *  the display of DragInt() calls.
 *  To honor backward compatibility we are rewriting the format string, unless IMGUI_DISABLE_OBSOLETE_FUNCTIONS is enabled.
 *  What could possibly go wrong?! */
fun patchFormatStringFloatToInt(fmt: String): String {
    if (fmt == "%.0f") // Fast legacy path for "%.0f" which is expected to be the most common case.
        return "%d"
    val fmtStart = parseFormatFindStart(fmt)    // Find % (if any, and ignore %%)
    // Find end of format specifier, which itself is an exercise of confidence/recklessness (because snprintf is dependent on libc or user).
    val fmtEnd = parseFormatFindEnd(fmt, fmtStart)
    if (fmtEnd > fmtStart && fmt[fmtEnd - 1] == 'f') {
        if (fmtStart == 0 && fmtEnd == fmt.length)
            return "%d"
        return fmt.substring(0, fmtStart) + "%d" + fmt.substring(fmtEnd, fmt.length)
    }
    return fmt
}