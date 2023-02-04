package imgui.static

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.findBottomMostVisibleWindowWithinBeginStack
import imgui.ImGui.getColorU32
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isNavFocusable
import imgui.ImGui.isWithinBeginStackOf
import imgui.ImGui.loadIniSettingsFromDisk
import imgui.ImGui.mainViewport
import imgui.ImGui.markIniSettingsDirty
import imgui.ImGui.ownerData
import imgui.ImGui.renderBullet
import imgui.ImGui.saveIniSettingsToDisk
import imgui.ImGui.setPos
import imgui.ImGui.setScrollX
import imgui.ImGui.setScrollY
import imgui.ImGui.testOwner
import imgui.ImGui.toKey
import imgui.ImGui.topMostAndVisiblePopupModal
import imgui.api.g
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0


// Misc

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

fun updateKeyboardInputs() {

    // Import legacy keys or verify they are not used

    // Synchronize io.KeyMods with individual modifiers io.KeyXXX bools, update aliases
    io.keyMods = mergedModsFromBools
    for (n in 0 until MouseButton.COUNT)
        updateAliasKey((MouseButton of n).toKey(), io.mouseDown[n], if (io.mouseDown[n]) 1f else 0f)
    updateAliasKey(Key.MouseWheelX, io.mouseWheelH != 0f, io.mouseWheelH)
    updateAliasKey(Key.MouseWheelY, io.mouseWheel != 0f, io.mouseWheel)

    // Clear gamepad data if disabled
    if (io.backendFlags hasnt BackendFlag.HasGamepad)
        for (i in Key.Gamepad_BEGIN until Key.Gamepad_END) {
            io.keysData[i].down = false
            io.keysData[i].analogValue = 0f
        }

    // Update keys
    for (keyData in io.keysData) {
        keyData.downDurationPrev = keyData.downDuration
        keyData.downDuration = if (keyData.down) (if (keyData.downDuration < 0f) 0f else keyData.downDuration + io.deltaTime) else -1f
    }

    // Update keys/input owner (named keys only): one entry per key
    for (keyIdx in Key.BEGIN until Key.END) {
        val key = Key of keyIdx
        val keyData = key.data
        val ownerData = key.ownerData
        ownerData.ownerCurr = ownerData.ownerNext
        if (!keyData.down) // Important: ownership is released on the frame after a release. Ensure a 'MouseDown -> CloseWindow -> MouseUp' chain doesn't lead to someone else seeing the MouseUp.
            ownerData.ownerNext = KeyOwner_None
        ownerData.lockThisFrame = ownerData.lockUntilRelease && keyData.down; ownerData.lockUntilRelease = ownerData.lockThisFrame  // Clear LockUntilRelease when key is not Down anymore
    }
}

fun updateAliasKey(key: Key, v: Boolean, analogValue: Float) {
    assert(key.isAlias)
    val keyData = key.data
    keyData.down = v
    keyData.analogValue = analogValue
}

// [Internal] Do not use directly (should read io.KeyMods instead)
val mergedModsFromBools: KeyChord
    get() {
        var keyChord: KeyChord = 0
        if (g.io.keyCtrl) keyChord /= Key.Mod_Ctrl
        if (g.io.keyShift) keyChord /= Key.Mod_Shift
        if (g.io.keyAlt) keyChord /= Key.Mod_Alt
        if (g.io.keySuper) keyChord /= Key.Mod_Super
        return keyChord
    }

fun updateMouseInputs() {

    with(io) {

        // Round mouse position to avoid spreading non-rounded position (e.g. UpdateManualResize doesn't support them well)
        if (isMousePosValid(mousePos)) {
            g.mouseLastValidPos put floorSigned(mousePos)
            mousePos = Vec2(g.mouseLastValidPos)
        }

        // If mouse just appeared or disappeared (usually denoted by -FLT_MAX component) we cancel out movement in MouseDelta
        if (isMousePosValid(mousePos) && isMousePosValid(mousePosPrev))
            mouseDelta = mousePos - mousePosPrev
        else
            mouseDelta put 0f

        // If mouse moved we re-enable mouse hovering in case it was disabled by gamepad/keyboard. In theory should use a >0.0f threshold but would need to reset in everywhere we set this to true.
        if (mouseDelta.x != 0f || mouseDelta.y != 0f)
            g.navDisableMouseHover = false

        mousePosPrev put mousePos
        for (i in mouseDown.indices) {
            mouseClicked[i] = mouseDown[i] && mouseDownDuration[i] < 0f
            mouseClickedCount[i] = 0 // Will be filled below
            mouseReleased[i] = !mouseDown[i] && mouseDownDuration[i] >= 0f
            mouseDownDurationPrev[i] = mouseDownDuration[i]
            mouseDownDuration[i] = when {
                mouseDown[i] -> when {
                    mouseDownDuration[i] < 0f -> 0f
                    else -> mouseDownDuration[i] + deltaTime
                }
                else -> -1f
            }
            if (mouseClicked[i]) {
                var isRepeatedClick = false
                if (g.time - mouseClickedTime[i] < mouseDoubleClickTime) {
                    val deltaFromClickPos = when {
                        isMousePosValid(mousePos) -> mousePos - mouseClickedPos[i]
                        else -> Vec2()
                    }
                    if (deltaFromClickPos.lengthSqr < mouseDoubleClickMaxDist * mouseDoubleClickMaxDist)
                        isRepeatedClick = true
                }
                if (isRepeatedClick)
                    mouseClickedLastCount[i]++
                else
                    mouseClickedLastCount[i] = 1
                mouseClickedTime[i] = g.time
                mouseClickedPos[i] put mousePos
                mouseClickedCount[i] = mouseClickedLastCount[i]
                mouseDragMaxDistanceSqr[i] = 0f
            } else if (mouseDown[i]) {
                // Maintain the maximum distance we reaching from the initial click position, which is used with dragging threshold
                val deltaSqrClickPos = if (isMousePosValid(mousePos)) (mousePos - mouseClickedPos[i]).lengthSqr else 0f
                io.mouseDragMaxDistanceSqr[i] = mouseDragMaxDistanceSqr[i] max deltaSqrClickPos
            }

            // We provide io.MouseDoubleClicked[] as a legacy service
            mouseDoubleClicked[i] = mouseClickedCount[i] == 2

            // Clicking any mouse button reactivate mouse hovering which may have been deactivated by gamepad/keyboard navigation
            if (mouseClicked[i])
                g.navDisableMouseHover = false
        }
    }
}

fun lockWheelingWindow(window: Window?) {
    g.wheelingWindowReleaseTimer = if (window != null) WINDOWS_MOUSE_WHEEL_SCROLL_LOCK_TIMER else 0f
    if (g.wheelingWindow === window)
        return
    IMGUI_DEBUG_LOG_IO("LockWheelingWindow() \"${window?.name ?: "NULL"}\"")
    g.wheelingWindow = window
    g.wheelingWindowRefMousePos put io.mousePos
}

fun updateMouseWheel() {

    // Reset the locked window if we move the mouse or after the timer elapses
    if (g.wheelingWindow != null) {
        g.wheelingWindowReleaseTimer -= io.deltaTime
        if (isMousePosValid() && (io.mousePos - g.wheelingWindowRefMousePos).lengthSqr > io.mouseDragThreshold * io.mouseDragThreshold)
            g.wheelingWindowReleaseTimer = 0f
        lockWheelingWindow(null)
    }

    val wheel = Vec2(if (Key.MouseWheelX testOwner KeyOwner_None) g.io.mouseWheelH else 0f,
                     if (Key.MouseWheelY testOwner KeyOwner_None) g.io.mouseWheel else 0f)
    if (wheel.x == 0f && wheel.y == 0f)
        return

    //IMGUI_DEBUG_LOG("MouseWheel X:%.3f Y:%.3f\n", wheel_x, wheel_y);
    val mouseWindow = g.wheelingWindow ?: g.hoveredWindow
    if (mouseWindow == null || mouseWindow.collapsed)
        return

    // Zoom / Scale window
    // FIXME-OBSOLETE: This is an old feature, it still works but pretty much nobody is using it and may be best redesigned.
    if (wheel.y != 0f && io.keyCtrl && io.fontAllowUserScaling) {
        lockWheelingWindow(mouseWindow)
        val window = mouseWindow
        val newFontScale = glm.clamp(window.fontWindowScale + io.mouseWheel * 0.1f, 0.5f, 2.5f)
        val scale = newFontScale / window.fontWindowScale
        window.fontWindowScale = newFontScale
        if (window === window.rootWindow) {
            val offset = window.size * (1f - scale) * (io.mousePos - window.pos) / window.size
            window.setPos(window.pos + offset)
            window.size = floor(window.size * scale)
            window.sizeFull = floor(window.sizeFull * scale)
        }
        return
    }
    if (g.io.keyCtrl)
        return

    // Mouse wheel scrolling
    // As a standard behavior holding SHIFT while using Vertical Mouse Wheel triggers Horizontal scroll instead
    // (we avoid doing it on OSX as it the OS input layer handles this already)
    val swapAxis = g.io.keyShift && !g.io.configMacOSXBehaviors
    if (swapAxis) {
        wheel.x = wheel.y
        wheel.y = 0f
    }

    // Vertical Mouse Wheel scrolling
    // Bubble up into parent window if:
    // - a child window doesn't allow any scrolling.
    // - a child window doesn't need scrolling because it is already at the edge for the direction we are going in.
    // - a child window has the ImGuiWindowFlags_NoScrollWithMouse flag.
    if (wheel.y != 0f) {
        var window = mouseWindow
        while (window!!.flags has WindowFlag._ChildWindow && (window.scrollMax.y == 0f || (window.flags has WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs)))
            window = window.parentWindow
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            lockWheelingWindow(mouseWindow)
            val maxStep = window.innerRect.height * 0.67f
            val scrollStep = floor((5 * window.calcFontSize()) min maxStep)
            window.setScrollY(window.scroll.y - wheel.y * scrollStep)
        }
    }

    // Horizontal Mouse Wheel scrolling, or Vertical Mouse Wheel w/ Shift held
    if (wheel.x != 0f) {
        var window = mouseWindow
        while (window!!.flags has WindowFlag._ChildWindow && (window.scrollMax.x == 0f || (window.flags has WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs)))
            window = window.parentWindow
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            lockWheelingWindow(mouseWindow)
            val maxStep = window.innerRect.width * 0.67f
            val scrollStep = floor((2 * window.calcFontSize()) min maxStep)
            window.setScrollX(window.scroll.x - wheel.x * scrollStep)
        }
    }
}

/** ~updateWindowManualResize
 *  Handle resize for: Resize Grips, Borders, Gamepad
 *  Return true when using auto-fit (double-click on resize grip)
 *  @return [JVM] borderHelf to Boolean   */
fun Window.updateManualResize(sizeAutoFit: Vec2, borderHeld_: Int, resizeGripCount: Int,
                       resizeGripCol: IntArray, visibilityRect: Rect): Pair<Int, Boolean> {

    var borderHeld = borderHeld_

    val flags = flags

    if (flags has WindowFlag.NoResize || flags has WindowFlag.AlwaysAutoResize || autoFitFrames anyGreaterThan 0)
        return borderHeld to false
    if (!wasActive) // Early out to avoid running this code for e.g. a hidden implicit/fallback Debug window.
        return borderHeld to false

    var retAutoFit = false
    val resizeBorderCount = if (io.configWindowsResizeFromEdges) 4 else 0
    val gripDrawSize = floor(kotlin.math.max(g.fontSize * 1.35f, windowRounding + 1f + g.fontSize * 0.2f))
    val gripHoverInnerSize = floor(gripDrawSize * 0.75f)
    val gripHoverOuterSize = if (io.configWindowsResizeFromEdges) WINDOWS_HOVER_PADDING else 0f

    val posTarget = Vec2(Float.MAX_VALUE)
    val sizeTarget = Vec2(Float.MAX_VALUE)

    // Resize grips and borders are on layer 1
    dc.navLayerCurrent = NavLayer.Menu

    // Manual resize grips
    ImGui.pushID("#RESIZE")
    for (resizeGripN in 0 until resizeGripCount) {

        val def = Window.resizeGripDef[resizeGripN]
        val corner = pos.lerp(pos + size, def.cornerPosN)

        // Using the FlattenChilds button flag we make the resize button accessible even if we are hovering over a child window
        val resizeRect = Rect(corner - def.innerDir * gripHoverOuterSize, corner + def.innerDir * gripHoverInnerSize)
        if (resizeRect.min.x > resizeRect.max.x) swap(resizeRect.min::x, resizeRect.max::x)
        if (resizeRect.min.y > resizeRect.max.y) swap(resizeRect.min::y, resizeRect.max::y)
        val resizeGripId = getID(resizeGripN) // == GetWindowResizeCornerID()
        ImGui.itemAdd(resizeRect, resizeGripId, null, ItemFlag.NoNav.i)
        val (_, hovered, held) = ImGui.buttonBehavior(resizeRect, resizeGripId, ButtonFlag.FlattenChildren or ButtonFlag.NoNavFocus)
        //GetOverlayDrawList(window)->AddRect(resize_rect.Min, resize_rect.Max, IM_COL32(255, 255, 0, 255));
        if (hovered || held)
            g.mouseCursor = if (resizeGripN has 1) MouseCursor.ResizeNESW else MouseCursor.ResizeNWSE

        if (held && g.io.mouseClickedCount[0] == 2 && resizeGripN == 0) {
            // Manual auto-fit when double-clicking
            sizeTarget put calcSizeAfterConstraint(sizeAutoFit)
            retAutoFit = true
            ImGui.clearActiveID()
        } else if (held) {
            // Resize from any of the four corners
            // We don't use an incremental MouseDelta but rather compute an absolute target size based on mouse position
            // Corner of the window corresponding to our corner grip
            val clampMin = Vec2 { if (def.cornerPosN[it] == 1f) visibilityRect.min[it] else -Float.MAX_VALUE }
            val clampMax = Vec2 { if (def.cornerPosN[it] == 0f) visibilityRect.max[it] else Float.MAX_VALUE }
            var cornerTarget = g.io.mousePos - g.activeIdClickOffset + (def.innerDir * gripHoverOuterSize).lerp(def.innerDir * -gripHoverInnerSize, def.cornerPosN)
            cornerTarget = glm.clamp(cornerTarget, clampMin, clampMax)
            calcResizePosSizeFromAnyCorner(cornerTarget, def.cornerPosN, posTarget, sizeTarget)
        }

        // Only lower-left grip is visible before hovering/activating
        if (resizeGripN == 0 || held || hovered)
            resizeGripCol[resizeGripN] = (if (held) Col.ResizeGripActive else if (hovered) Col.ResizeGripHovered else Col.ResizeGrip).u32
    }
    for (borderN in 0 until resizeBorderCount) {
        val def = Window.resizeBorderDef[borderN]
        val axis = if (borderN == Dir.Left.i || borderN == Dir.Right.i) Axis.X else Axis.Y
        val borderRect = getResizeBorderRect(borderN, gripHoverInnerSize, WINDOWS_HOVER_PADDING)
        val borderId = getID(borderN + 4) // == GetWindowResizeBorderID()
        ImGui.itemAdd(borderRect, borderId, null, ItemFlag.NoNav.i)
        val (_, hovered, held) = ImGui.buttonBehavior(borderRect, borderId, ButtonFlag.FlattenChildren or ButtonFlag.NoNavFocus)
        //GetOverlayDrawList(window)->AddRect(border_rect.Min, border_rect.Max, IM_COL32(255, 255, 0, 255));
        if ((hovered && g.hoveredIdTimer > WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER) || held) {
            g.mouseCursor = if (axis == Axis.X) MouseCursor.ResizeEW else MouseCursor.ResizeNS
            if (held)
                borderHeld = borderN
        }
        if (held) {
            val clampMin = Vec2(if (borderN == Dir.Right.i) visibilityRect.min.x else -Float.MAX_VALUE, if (borderN == Dir.Down.i) visibilityRect.min.y else -Float.MAX_VALUE)
            val clampMax = Vec2(if (borderN == Dir.Left.i) visibilityRect.max.x else +Float.MAX_VALUE, if (borderN == Dir.Up.i) visibilityRect.max.y else +Float.MAX_VALUE)
            var borderTarget = Vec2(pos)
            borderTarget[axis] = g.io.mousePos[axis] - g.activeIdClickOffset[axis] + WINDOWS_HOVER_PADDING
            borderTarget = glm.clamp(borderTarget, clampMin, clampMax)
            calcResizePosSizeFromAnyCorner(borderTarget, def.segmentN1 min def.segmentN2, posTarget, sizeTarget)
        }
    }
    ImGui.popID()

    // Restore nav layer
    dc.navLayerCurrent = NavLayer.Main

    // Navigation resize (keyboard/gamepad)
    // FIXME: This cannot be moved to NavUpdateWindowing() because CalcWindowSizeAfterConstraint() need to callback into user.
    // Not even sure the callback works here.
    if (g.navWindowingTarget?.rootWindow === this) {
        val navResizeDir = Vec2()
        if (g.navInputSource == InputSource.Keyboard && g.io.keyShift)
            navResizeDir put ImGui.getKeyVector2d(Key.LeftArrow, Key.RightArrow, Key.UpArrow, Key.DownArrow)
        if (g.navInputSource == InputSource.Gamepad)
            navResizeDir put ImGui.getKeyVector2d(Key.GamepadDpadLeft, Key.GamepadDpadRight, Key.GamepadDpadUp, Key.GamepadDpadDown)
        if (navResizeDir.x != 0f || navResizeDir.y != 0f) {
            val NAV_RESIZE_SPEED = 600f
            val resizeStep = floor(NAV_RESIZE_SPEED * g.io.deltaTime * kotlin.math.min(g.io.displayFramebufferScale.x, g.io.displayFramebufferScale.y))
            g.navWindowingAccumDeltaSize += navResizeDir * resizeStep
            g.navWindowingAccumDeltaSize put (g.navWindowingAccumDeltaSize max (visibilityRect.min - pos - size)) // We need Pos+Size >= visibility_rect.Min, so Size >= visibility_rect.Min - Pos, so size_delta >= visibility_rect.Min - window->Pos - window->Size
            g.navWindowingToggleLayer = false
            g.navDisableMouseHover = true
            resizeGripCol[0] = Col.ResizeGripActive.u32
            val accumFloored = floor(g.navWindowingAccumDeltaSize)
            if (accumFloored.x != 0f || accumFloored.y != 0f) {
                // FIXME-NAV: Should store and accumulate into a separate size buffer to handle sizing constraints properly, right now a constraint will make us stuck.
                sizeTarget put calcSizeAfterConstraint(sizeFull + navResizeDir)
                g.navWindowingAccumDeltaSize -= accumFloored
            }
        }
    }

    // Apply back modified position/size to window
    if (sizeTarget.x != Float.MAX_VALUE) {
        sizeFull put sizeTarget
        markIniSettingsDirty()
    }
    if (posTarget.x != Float.MAX_VALUE) {
        pos = floor(posTarget)
        markIniSettingsDirty()
    }

    size put sizeFull

    return borderHeld to retAutoFit
}

// ~RenderWindowOuterBorders
fun Window.renderOuterBorders() {

    val rounding = windowRounding
    val borderSize = windowBorderSize
    if (borderSize > 0f && flags hasnt WindowFlag.NoBackground) drawList.addRect(pos, pos + size, Col.Border.u32, rounding, 0, borderSize)

    val borderHeld = resizeBorderHeld
    if (borderHeld != -1) {
        val def = Window.resizeBorderDef[borderHeld]
        val borderR = getResizeBorderRect(borderHeld, rounding, 0f)
        drawList.apply {
            pathArcTo(borderR.min.lerp(borderR.max, def.segmentN1) + Vec2(0.5f) + def.innerDir * rounding,
                      rounding,
                      def.outerAngle - glm.PIf * 0.25f,
                      def.outerAngle)
            pathArcTo(borderR.min.lerp(borderR.max, def.segmentN2) + Vec2(0.5f) + def.innerDir * rounding,
                      rounding,
                      def.outerAngle,
                      def.outerAngle + glm.PIf * 0.25f)
            pathStroke(Col.SeparatorActive.u32, 0, 2f max borderSize) // Thicker than usual
        }
    }
    if (ImGui.style.frameBorderSize > 0f && flags hasnt WindowFlag.NoTitleBar) {
        val y = pos.y + titleBarHeight - 1
        drawList.addLine(Vec2(pos.x + borderSize, y),
                         Vec2(pos.x + size.x - borderSize, y),
                         Col.Border.u32,
                         ImGui.style.frameBorderSize)
    }
}

/** ~RenderWindowDecorations
 *  Draw background and borders
 *  Draw and handle scrollbars */
fun Window.renderDecorations(titleBarRect: Rect, titleBarIsHighlight: Boolean, resizeGripCount: Int, resizeGripCol: IntArray, resizeGripDrawSize: Float) {

    // Ensure that ScrollBar doesn't read last frame's SkipItems
    assert(beginCount == 0)
    skipItems = false

    // Draw window + handle manual resize
    // As we highlight the title bar when want_focus is set, multiple reappearing windows will have their title bar highlighted on their reappearing frame.
    if (collapsed) { // Title bar only
        val backupBorderSize = ImGui.style.frameBorderSize
        g.style.frameBorderSize = windowBorderSize
        val titleBarCol =
            if (titleBarIsHighlight && !g.navDisableHighlight) Col.TitleBgActive else Col.TitleBgCollapsed
        ImGui.renderFrame(titleBarRect.min, titleBarRect.max, titleBarCol.u32, true, windowRounding)
        ImGui.style.frameBorderSize = backupBorderSize
    } else { // Window background
        if (flags hasnt WindowFlag.NoBackground) {
            var bgCol = bgColorIdx.u32
            var overrideAlpha = false
            val alpha = when {
                g.nextWindowData.flags has NextWindowDataFlag.HasBgAlpha -> {
                    overrideAlpha = true
                    g.nextWindowData.bgAlphaVal
                }
                else -> 1f
            }
            if (overrideAlpha) bgCol = (bgCol and COL32_A_MASK.inv()) or (F32_TO_INT8_SAT(alpha) shl COL32_A_SHIFT)
            drawList.addRectFilled(pos + Vec2(0f, titleBarHeight), pos + size, bgCol, windowRounding,
                                   if (flags has WindowFlag.NoTitleBar) 0 else DrawFlag.RoundCornersBottom.i)
        }

        // Title bar
        if (flags hasnt WindowFlag.NoTitleBar) {
            val titleBarCol = if (titleBarIsHighlight) Col.TitleBgActive else Col.TitleBg
            drawList.addRectFilled(titleBarRect.min, titleBarRect.max, titleBarCol.u32, windowRounding, DrawFlag.RoundCornersTop.i)
        }

        // Menu bar
        if (flags has WindowFlag.MenuBar) {
            val menuBarRect = menuBarRect()
            menuBarRect clipWith rect() // Soft clipping, in particular child window don't have minimum size covering the menu bar so this is useful for them.
            val rounding = if (flags has WindowFlag.NoTitleBar) windowRounding else 0f
            drawList.addRectFilled(menuBarRect.min + Vec2(windowBorderSize, 0f),
                                   menuBarRect.max - Vec2(windowBorderSize, 0f),
                                   Col.MenuBarBg.u32, rounding, DrawFlag.RoundCornersTop.i)
            if (ImGui.style.frameBorderSize > 0f && menuBarRect.max.y < pos.y + size.y)
                drawList.addLine(menuBarRect.bl, menuBarRect.br, Col.Border.u32, ImGui.style.frameBorderSize)
        }

        // Scrollbars
        if (scrollbar.x) ImGui.scrollbar(Axis.X)
        if (scrollbar.y) ImGui.scrollbar(Axis.Y)

        // Render resize grips (after their input handling so we don't have a frame of latency)
        if (flags hasnt WindowFlag.NoResize) repeat(resizeGripCount) { resizeGripN ->
            val grip = Window.resizeGripDef[resizeGripN]
            val corner = pos.lerp(pos + size, grip.cornerPosN)
            with(drawList) {
                pathLineTo(corner + grip.innerDir * (if (resizeGripN has 1) Vec2(windowBorderSize, resizeGripDrawSize) else Vec2(resizeGripDrawSize, windowBorderSize)))
                pathLineTo(corner + grip.innerDir * (if (resizeGripN has 1) Vec2(resizeGripDrawSize, windowBorderSize) else Vec2(windowBorderSize, resizeGripDrawSize)))
                pathArcToFast(Vec2(corner.x + grip.innerDir.x * (windowRounding + windowBorderSize),
                                   corner.y + grip.innerDir.y * (windowRounding + windowBorderSize)),
                              windowRounding, grip.angleMin12, grip.angleMax12)
                pathFillConvex(resizeGripCol[resizeGripN])
            }
        }

        // Borders
        renderOuterBorders()
    }
}

val Window.bgColorIdx: Col
    get() = when {
        flags has (WindowFlag._Tooltip or WindowFlag._Popup) -> Col.PopupBg
        flags has WindowFlag._ChildWindow -> Col.ChildBg
        else -> Col.WindowBg
    }

/** ~RenderWindowTitleBarContents
 *  Render title text, collapse button, close button */
fun Window.renderTitleBarContents(titleBarRect: Rect, name: String, pOpen: KMutableProperty0<Boolean>?) {

    val hasCloseButton = pOpen != null
    val hasCollapseButton = flags hasnt WindowFlag.NoCollapse && ImGui.style.windowMenuButtonPosition != Dir.None

    // Close & Collapse button are on the Menu NavLayer and don't default focus (unless there's nothing else on that layer)
    // FIXME-NAV: Might want (or not?) to set the equivalent of ImGuiButtonFlags_NoNavFocus so that mouse clicks on standard title bar items don't necessarily set nav/keyboard ref?
    val itemFlagsBackup = g.currentItemFlags
    g.currentItemFlags = g.currentItemFlags or ItemFlag.NoNavDefaultFocus
    dc.navLayerCurrent = NavLayer.Menu

    // Layout buttons
    // FIXME: Would be nice to generalize the subtleties expressed here into reusable code.
    var padL = ImGui.style.framePadding.x
    var padR = ImGui.style.framePadding.x
    val buttonSz = g.fontSize
    val closeButtonPos = Vec2()
    val collapseButtonPos = Vec2()
    if (hasCloseButton) {
        padR += buttonSz
        closeButtonPos.put(titleBarRect.max.x - padR - ImGui.style.framePadding.x, titleBarRect.min.y)
    }
    if (hasCollapseButton && ImGui.style.windowMenuButtonPosition == Dir.Right) {
        padR += buttonSz
        collapseButtonPos.put(titleBarRect.max.x - padR - ImGui.style.framePadding.x, titleBarRect.min.y)
    }
    if (hasCollapseButton && ImGui.style.windowMenuButtonPosition == Dir.Left) {
        collapseButtonPos.put(titleBarRect.min.x + padL - ImGui.style.framePadding.x, titleBarRect.min.y)
        padL += buttonSz
    }

    // Collapse button (submitting first so it gets priority when choosing a navigation init fallback)
    if (hasCollapseButton)
        if (ImGui.collapseButton(getID("#COLLAPSE"), collapseButtonPos))
            wantCollapseToggle = true // Defer actual collapsing to next frame as we are too far in the Begin() function

    // Close button
    if (hasCloseButton)
        if (ImGui.closeButton(getID("#CLOSE"), closeButtonPos))
            pOpen!!.set(false)

    dc.navLayerCurrent = NavLayer.Main
    g.currentItemFlags = itemFlagsBackup

    // Title bar text (with: horizontal alignment, avoiding collapse/close button, optional "unsaved document" marker)
    // FIXME: Refactor text alignment facilities along with RenderText helpers, this is too much code..
    val UNSAVED_DOCUMENT_MARKER = "*"
    val markerSizeX = if (flags has WindowFlag.UnsavedDocument) buttonSz * 0.8f else 0f
    val textSize = ImGui.calcTextSize(name, hideTextAfterDoubleHash = true) + Vec2(markerSizeX, 0f)

    // As a nice touch we try to ensure that centered title text doesn't get affected by visibility of Close/Collapse button,
    // while uncentered title text will still reach edges correctly.
    if (padL > ImGui.style.framePadding.x) padL += ImGui.style.itemInnerSpacing.x
    if (padR > ImGui.style.framePadding.x) padR += ImGui.style.itemInnerSpacing.x
    if (ImGui.style.windowTitleAlign.x > 0f && ImGui.style.windowTitleAlign.x < 1f) {
        val centerness = saturate(1f - abs(ImGui.style.windowTitleAlign.x - 0.5f) * 2f) // 0.0f on either edges, 1.0f on center
        val padExtend = min(max(padL, padR), titleBarRect.width - padL - padR - textSize.x)
        padL = padL max (padExtend * centerness)
        padR = padR max (padExtend * centerness)
    }

    val layoutR = Rect(titleBarRect.min.x + padL, titleBarRect.min.y, titleBarRect.max.x - padR, titleBarRect.max.y)
    val clipR = Rect(layoutR.min.x, layoutR.min.y, (layoutR.max.x + ImGui.style.itemInnerSpacing.x) min titleBarRect.max.x, layoutR.max.y)
    if (flags has WindowFlag.UnsavedDocument) {
        val markerPos = Vec2(clamp(layoutR.min.x + (layoutR.width - textSize.x) * ImGui.style.windowTitleAlign.x + textSize.x, layoutR.min.x, layoutR.max.x),
                             (layoutR.min.y + layoutR.max.y) * 0.5f)
        if (markerPos.x > layoutR.min.x) {
            drawList.renderBullet(markerPos, Col.Text.u32)
            clipR.max.x = clipR.max.x min (markerPos.x - (markerSizeX * 0.5f).i)
        }
    }
    //if (g.IO.KeyShift) window->DrawList->AddRect(layout_r.Min, layout_r.Max, IM_COL32(255, 128, 0, 255)); // [DEBUG]
    //if (g.IO.KeyCtrl) window->DrawList->AddRect(clip_r.Min, clip_r.Max, IM_COL32(255, 128, 0, 255)); // [DEBUG]
    ImGui.renderTextClipped(layoutR.min, layoutR.max, name, textSize, ImGui.style.windowTitleAlign, clipR)
}

fun renderDimmedBackgroundBehindWindow(window: Window, col: Int) {
    if (col hasnt COL32_A_MASK)
        return

    val viewport = mainViewport as ViewportP
    val viewportRect = viewport.mainRect

    // Draw behind window by moving the draw command at the FRONT of the draw list
    run {
        // We've already called AddWindowToDrawData() which called DrawList->ChannelsMerge() on DockNodeHost windows,
        // and draw list have been trimmed already, hence the explicit recreation of a draw command if missing.
        // FIXME: This is creating complication, might be simpler if we could inject a drawlist in drawdata at a given position and not attempt to manipulate ImDrawCmd order.
        val drawList = window.rootWindow!!.drawList
        if (drawList.cmdBuffer.isEmpty())
            drawList.addDrawCmd()
        drawList.pushClipRect(viewportRect.min - 1, viewportRect.max + 1, false) // Ensure ImDrawCmd are not merged
        drawList.addRectFilled(viewportRect.min, viewportRect.max, col)
        val cmd = drawList.cmdBuffer.last()
        assert(cmd.elemCount == 6)
        drawList.cmdBuffer.pop()
        drawList.cmdBuffer += cmd
        drawList.popClipRect()
        drawList.addDrawCmd() // We need to create a command as CmdBuffer.back().IdxOffset won't be correct if we append to same command.
    }
}

fun renderDimmedBackgrounds() {
    val modalWindow = topMostAndVisiblePopupModal
    if (g.dimBgRatio <= 0f && g.navWindowingHighlightAlpha <= 0f)
        return
    val dimBgForModal = modalWindow != null
    val dimBgForWindowList = g.navWindowingTargetAnim?.active == true
    if (!dimBgForModal && !dimBgForWindowList)
        return

    if (dimBgForModal) {
        // Draw dimming behind modal or a begin stack child, whichever comes first in draw order.
        val dimBehindWindow = modalWindow!!.findBottomMostVisibleWindowWithinBeginStack()
        renderDimmedBackgroundBehindWindow(dimBehindWindow, getColorU32(Col.ModalWindowDimBg, g.dimBgRatio))
    } else if (dimBgForWindowList) {
        val navWindowingTargetAnim = g.navWindowingTargetAnim!!
        // Draw dimming behind CTRL+Tab target window
        renderDimmedBackgroundBehindWindow(navWindowingTargetAnim, getColorU32(Col.NavWindowingDimBg, g.dimBgRatio))

        // Draw border around CTRL+Tab target window
        val window = navWindowingTargetAnim
        val viewport = mainViewport
        val distance = g.fontSize
        val bb = window.rect()
        bb expand distance
        if (bb.width >= viewport.size.x && bb.height >= viewport.size.y)
            bb expand (-distance - 1f) // If a window fits the entire viewport, adjust its highlight inward
        if (window.drawList.cmdBuffer.isEmpty())
            window.drawList.addDrawCmd()
        window.drawList.pushClipRect(viewport.pos, viewport.pos + viewport.size)
        window.drawList.addRect(bb.min, bb.max, getColorU32(Col.NavWindowingHighlight, g.navWindowingHighlightAlpha), window.windowRounding, 0, 3f)
        window.drawList.popClipRect()
    }
}

// When a modal popup is open, newly created windows that want focus (i.e. are not popups and do not specify ImGuiWindowFlags_NoFocusOnAppearing)
// should be positioned behind that modal window, unless the window was created inside the modal begin-stack.
// In case of multiple stacked modals newly created window honors begin stack order and does not go below its own modal parent.
// - Window             // FindBlockingModal() returns Modal1
//   - Window           //                  .. returns Modal1
//   - Modal1           //                  .. returns Modal2
//      - Window        //                  .. returns Modal2
//          - Window    //                  .. returns Modal2
//          - Modal2    //                  .. returns Modal2
fun findBlockingModal(window: Window): Window? {

    if (g.openPopupStack.isEmpty())
        return null

    // Find a modal that has common parent with specified window. Specified window should be positioned behind that modal.
    for (i in g.openPopupStack.lastIndex downTo 0) {
        val popupWindow = g.openPopupStack[i].window
        if (popupWindow == null || popupWindow.flags hasnt WindowFlag._Modal)
            continue
        if (!popupWindow.active || popupWindow.wasActive) // Check WasActive, because this code may run before popup renders on current frame, also check Active to handle newly created windows.
            continue
        if (window isWithinBeginStackOf popupWindow)      // Window is rendered over last modal, no render order change needed.
            break
        var parent = popupWindow.parentWindowInBeginStack!!.rootWindow
        while (parent != null) {
            if (window isWithinBeginStackOf parent)
                return popupWindow                        // Place window above its begin stack parent.
            parent = parent.parentWindowInBeginStack!!.rootWindow
        }
    }
    return null
}

// UpdateWindowManualResize,
// RenderWindowOuterBorders,
// RenderWindowDecorations,
// RenderWindowTitleBarContents -> window class

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
        g.navWindowingAccumDeltaPos put 0f; g.navWindowingAccumDeltaSize put 0f
    }
    g.navWindowingToggleLayer = false
}

inline fun <reified T : InputEvent> findLatestInputEvent(arg: Int = -1): T? {
    for (n in g.inputEventsQueue.lastIndex downTo 0) {
        val e = g.inputEventsQueue[n]
        if (e !is T)
            continue
        if (e is InputEvent.Key && e.key.i != arg)
            continue
        if (e is InputEvent.MouseButton && e.button != arg)
            continue
        return e
    }
    return null
}