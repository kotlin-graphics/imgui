package imgui.static

import gli_.has
import gli_.hasnt
import glm_.glm
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.calcTextSize
import imgui.ImGui.clearActiveID
import imgui.ImGui.closePopupToLevel
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.end
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.findWindowByName
import imgui.ImGui.focusWindow
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isActiveIdUsingKey
import imgui.ImGui.isActiveIdUsingNavDir
import imgui.ImGui.isActiveIdUsingNavInput
import imgui.ImGui.isKeyDown
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.isMousePosValid
import imgui.ImGui.navInitWindow
import imgui.ImGui.navMoveRequestButNoResultYet
import imgui.ImGui.navMoveRequestForward
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.selectable
import imgui.ImGui.setNavIDWithRectRel
import imgui.ImGui.setNavID
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.style
import imgui.ImGui.topMostPopupModal
import imgui.api.g
import imgui.internal.*
import imgui.internal.classes.NavMoveResult
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import imgui.WindowFlag as Wf
import imgui.internal.sections.ItemFlag as If

fun navUpdate() {

    io.wantSetMousePos = false

//    if (g.NavScoringCount > 0) printf("[%05d] NavScoringCount %d for '%s' layer %d (Init:%d, Move:%d)\n", g.FrameCount, g.NavScoringCount, g.NavWindow ? g . NavWindow->Name : "NULL", g.NavLayer, g.NavInitRequest || g.NavInitResultId != 0, g.NavMoveRequest)

    // Set input source as Gamepad when buttons are pressed (as some features differs when used with Gamepad vs Keyboard)
    // (do it before we map Keyboard input!)
    val navKeyboardActive = io.configFlags has ConfigFlag.NavEnableKeyboard
    val navGamepadActive = io.configFlags has ConfigFlag.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad

    if (navGamepadActive && g.navInputSource != InputSource.NavGamepad)
        io.navInputs.also {
            if (it[NavInput.Activate] > 0f || it[NavInput.Input] > 0f || it[NavInput.Cancel] > 0f || it[NavInput.Menu] > 0f
                || it[NavInput.DpadLeft] > 0f || it[NavInput.DpadRight] > 0f || it[NavInput.DpadUp] > 0f || it[NavInput.DpadDown] > 0f
            )
                g.navInputSource = InputSource.NavGamepad
        }

    // Update Keyboard->Nav inputs mapping
    if (navKeyboardActive) {
        fun navMapKey(key: Key, navInput: NavInput) {
            if (isKeyDown(io.keyMap[key])) {
                io.navInputs[navInput] = 1f
                g.navInputSource = InputSource.NavKeyboard
            }
        }
        navMapKey(Key.Space, NavInput.Activate)
        navMapKey(Key.Enter, NavInput.Input)
        navMapKey(Key.Escape, NavInput.Cancel)
        navMapKey(Key.LeftArrow, NavInput._KeyLeft)
        navMapKey(Key.RightArrow, NavInput._KeyRight)
        navMapKey(Key.UpArrow, NavInput._KeyUp)
        navMapKey(Key.DownArrow, NavInput._KeyDown)
        if (io.keyCtrl)
            io.navInputs[NavInput.TweakSlow] = 1f
        if (io.keyShift)
            io.navInputs[NavInput.TweakFast] = 1f
        if (io.keyAlt && !io.keyCtrl) // AltGR is Alt+Ctrl, also even on keyboards without AltGR we don't want Alt+Ctrl to open menu.
            io.navInputs[NavInput._KeyMenu] = 1f
    }
    for (i in io.navInputsDownDuration.indices)
        io.navInputsDownDurationPrev[i] = io.navInputsDownDuration[i]
    for (i in io.navInputs.indices)
        io.navInputsDownDuration[i] = when (io.navInputs[i] > 0f) {
            true -> if (io.navInputsDownDuration[i] < 0f) 0f else io.navInputsDownDuration[i] + io.deltaTime
            else -> -1f
        }

    // Process navigation init request (select first/default focus)
    if (g.navInitResultId != 0 && (!g.navDisableHighlight || g.navInitRequestFromMove))
        navUpdateInitResult()
    g.navInitRequest = false
    g.navInitRequestFromMove = false
    g.navInitResultId = 0
    g.navJustMovedToId = 0

    // Process navigation move request
    if (g.navMoveRequest)
        navUpdateMoveResult()

    // When a forwarded move request failed, we restore the highlight that we disabled during the forward frame
    if (g.navMoveRequestForward == NavForward.ForwardActive) {
        assert(g.navMoveRequest)
        if (g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0)
            g.navDisableHighlight = false
        g.navMoveRequestForward = NavForward.None
    }

    // Apply application mouse position movement, after we had a chance to process move request result.
    if (g.navMousePosDirty && g.navIdIsAlive) {
        // Set mouse position given our knowledge of the navigated item position from last frame
        if (io.configFlags has ConfigFlag.NavEnableSetMousePos && io.backendFlags has BackendFlag.HasSetMousePos)
            if (!g.navDisableHighlight && g.navDisableMouseHover && g.navWindow != null) {
                io.mousePos = navCalcPreferredRefPos()
                io.mousePosPrev = Vec2(io.mousePos)
                io.wantSetMousePos = true
            }
        g.navMousePosDirty = false
    }
    g.navIdIsAlive = false
    g.navJustTabbedId = 0
//    assert(g.navLayer == 0 || g.navLayer == 1) useless on jvm

    // Store our return window (for returning from Layer 1 to Layer 0) and clear it as soon as we step back in our own Layer 0
    g.navWindow?.let {
        navSaveLastChildNavWindowIntoParent(it)
        if (it.navLastChildNavWindow != null && g.navLayer == NavLayer.Main)
            it.navLastChildNavWindow = null
    }

    // Update CTRL+TAB and Windowing features (hold Square to move/resize/etc.)
    navUpdateWindowing()

    // Set output flags for user application
    io.navActive = (navKeyboardActive || navGamepadActive) && g.navWindow?.flags?.hasnt(Wf.NoNavInputs) ?: false
    io.navVisible = (io.navActive && g.navId != 0 && !g.navDisableHighlight) || g.navWindowingTarget != null

    // Process NavCancel input (to close a popup, get back to parent, clear focus)
    if (NavInput.Cancel.isTest(InputReadMode.Pressed)) {
        IMGUI_DEBUG_LOG_NAV("[nav] ImGuiNavInput_Cancel")
        if (g.activeId != 0) {
            if (!isActiveIdUsingNavInput(NavInput.Cancel))
                clearActiveID()
        } else if (g.navWindow != null && g.navWindow!!.flags has Wf._ChildWindow && g.navWindow!!.flags hasnt Wf._Popup && g.navWindow!!.parentWindow != null) {
            // Exit child window
            val childWindow = g.navWindow!!
            val parentWindow = childWindow.parentWindow!!
            assert(childWindow.childId != 0)
            focusWindow(parentWindow)
            setNavID(childWindow.childId, NavLayer.Main, 0)
            // Reassigning with same value, we're being explicit here.
            g.navIdIsAlive = false  // -V1048
            if (g.navDisableMouseHover)
                g.navMousePosDirty = true
        } else if (g.openPopupStack.isNotEmpty()) {
            // Close open popup/menu
            if (g.openPopupStack.last().window!!.flags hasnt Wf._Modal)
                closePopupToLevel(g.openPopupStack.lastIndex, true)
        } else if (g.navLayer != NavLayer.Main)
            navRestoreLayer(NavLayer.Main)  // Leave the "menu" layer
        else {
            // Clear NavLastId for popups but keep it for regular child window so we can leave one and come back where we were
            if (g.navWindow != null && (g.navWindow!!.flags has Wf._Popup || g.navWindow!!.flags hasnt Wf._ChildWindow))
                g.navWindow!!.navLastIds[0] = 0
            g.navFocusScopeId = 0
            g.navId = 0
        }
    }

    // Process manual activation request
    g.navActivateId = 0
    g.navActivateDownId = 0
    g.navActivatePressedId = 0
    g.navInputId = 0
    if (g.navId != 0 && !g.navDisableHighlight && g.navWindowingTarget == null && g.navWindow != null && g.navWindow!!.flags hasnt Wf.NoNavInputs) {
        val activateDown = NavInput.Activate.isDown()
        val activatePressed = activateDown && NavInput.Activate.isTest(InputReadMode.Pressed)
        if (g.activeId == 0 && activatePressed)
            g.navActivateId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && activateDown)
            g.navActivateDownId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && activatePressed)
            g.navActivatePressedId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && NavInput.Input.isTest(InputReadMode.Pressed))
            g.navInputId = g.navId
    }
    g.navWindow?.let { if (it.flags has Wf.NoNavInputs) g.navDisableHighlight = true }
    if (g.navActivateId != 0)
        assert(g.navActivateDownId == g.navActivateId)
    g.navMoveRequest = false

    // Process programmatic activation request
    if (g.navNextActivateId != 0) {
        g.navInputId = g.navNextActivateId
        g.navActivatePressedId = g.navNextActivateId
        g.navActivateDownId = g.navNextActivateId
        g.navActivateId = g.navNextActivateId
    }
    g.navNextActivateId = 0

    // Initiate directional inputs request
    if (g.navMoveRequestForward == NavForward.None) {
        g.navMoveDir = Dir.None
        g.navMoveRequestFlags = NavMoveFlag.None.i
        g.navWindow?.let {
            if (g.navWindowingTarget == null && it.flags hasnt Wf.NoNavInputs) {
                val readMode = InputReadMode.Repeat
                if (!isActiveIdUsingNavDir(Dir.Left) && (NavInput.DpadLeft.isTest(readMode) || NavInput._KeyLeft.isTest(
                        readMode
                    ))
                ) g.navMoveDir = Dir.Left
                if (!isActiveIdUsingNavDir(Dir.Right) && (NavInput.DpadRight.isTest(readMode) || NavInput._KeyRight.isTest(
                        readMode
                    ))
                ) g.navMoveDir = Dir.Right
                if (!isActiveIdUsingNavDir(Dir.Up) && (NavInput.DpadUp.isTest(readMode) || NavInput._KeyUp.isTest(
                        readMode
                    ))
                ) g.navMoveDir = Dir.Up
                if (!isActiveIdUsingNavDir(Dir.Down) && (NavInput.DpadDown.isTest(readMode) || NavInput._KeyDown.isTest(
                        readMode
                    ))
                ) g.navMoveDir = Dir.Down
            }
        }
        g.navMoveDir = g.navMoveDir
    } else {
        // Forwarding previous request (which has been modified, e.g. wrap around menus rewrite the requests with a starting rectangle at the other side of the window)
        // (Preserve g.NavMoveRequestFlags, g.NavMoveClipDir which were set by the NavMoveRequestForward() function)
        assert(g.navMoveDir != Dir.None && g.navMoveDir != Dir.None)
        assert(g.navMoveRequestForward == NavForward.ForwardQueued)
        IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequestForward ${g.navMoveDir.i}")
        g.navMoveRequestForward = NavForward.ForwardActive
    }

    // Update PageUp/PageDown/Home/End scroll
    // FIXME-NAV: Consider enabling those keys even without the master ImGuiConfigFlags_NavEnableKeyboard flag?
    val navScoringRectOffsetY = when {
        navKeyboardActive -> navUpdatePageUpPageDown()
        else -> 0f
    }

    // If we initiate a movement request and have no current NavId, we initiate a InitDefautRequest that will be used as a fallback if the direction fails to find a match
    if (g.navMoveDir != Dir.None) {
        g.navMoveRequest = true
        g.navMoveRequestKeyMods = io.keyMods
        g.navMoveDirLast = g.navMoveDir
    }
    if (g.navMoveRequest && g.navId == 0) {
        IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: from move, window \"${g.navWindow!!.name}\", layer=${g.navLayer}")
        g.navInitRequest = true
        g.navInitRequestFromMove = true
        // Reassigning with same value, we're being explicit here.
        g.navInitResultId = 0    // -V1048
        g.navDisableHighlight = false
    }
    navUpdateAnyRequestFlag()

    // Scrolling
    g.navWindow?.let {

        if (it.flags hasnt Wf.NoNavInputs && g.navWindowingTarget == null) {
            // *Fallback* manual-scroll with Nav directional keys when window has no navigable item
            val scrollSpeed =
                round(it.calcFontSize() * 100 * io.deltaTime) // We need round the scrolling speed because sub-pixel scroll isn't reliably supported.
            if (it.dc.navLayerActiveMask == 0 && it.dc.navHasScroll && g.navMoveRequest) {
                if (g.navMoveDir == Dir.Left || g.navMoveDir == Dir.Right)
                    it setScrollX floor(it.scroll.x + (if (g.navMoveDir == Dir.Left) -1f else 1f) * scrollSpeed)
                if (g.navMoveDir == Dir.Up || g.navMoveDir == Dir.Down)
                    it setScrollY floor(it.scroll.y + (if (g.navMoveDir == Dir.Up) -1f else 1f) * scrollSpeed)
            }

            // *Normal* Manual scroll with NavScrollXXX keys
            // Next movement request will clamp the NavId reference rectangle to the visible area, so navigation will resume within those bounds.
            val scrollDir = getNavInputAmount2d(NavDirSourceFlag.PadLStick.i, InputReadMode.Down, 1f / 10f, 10f)
            if (scrollDir.x != 0f && it.scrollbar.x)
                it setScrollX floor(it.scroll.x + scrollDir.x * scrollSpeed)
            if (scrollDir.y != 0f)
                it setScrollY floor(it.scroll.y + scrollDir.y * scrollSpeed)
        }
    }

    // Reset search results
    g.navMoveResultLocal.clear()
    g.navMoveResultLocalVisibleSet.clear()
    g.navMoveResultOther.clear()

    // When using gamepad, we project the reference nav bounding box into window visible area.
    // This is to allow resuming navigation inside the visible area after doing a large amount of scrolling, since with gamepad every movements are relative
    // (can't focus a visible object like we can with the mouse).
    if (g.navMoveRequest && g.navInputSource == InputSource.NavGamepad && g.navLayer == NavLayer.Main) {
        val window = g.navWindow!!
        val windowRectRel = Rect(window.innerRect.min - window.pos - 1, window.innerRect.max - window.pos + 1)
        if (window.navRectRel[g.navLayer] !in windowRectRel) {
            IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequest: clamp NavRectRel")
            val pad = window.calcFontSize() * 0.5f
            windowRectRel expand Vec2(
                -min(windowRectRel.width, pad),
                -min(windowRectRel.height, pad)
            ) // Terrible approximation for the intent of starting navigation from first fully visible item
            window.navRectRel[g.navLayer] clipWithFull windowRectRel
            g.navFocusScopeId = 0
            g.navId = 0
        }
    }

    // For scoring we use a single segment on the left side our current item bounding box (not touching the edge to avoid box overlap with zero-spaced items)
    g.navWindow.let {
        val navRectRel = it?.run { Rect(navRectRel[g.navLayer]) } ?: Rect()
        g.navScoringRect.put(it?.run { Rect(navRectRel.min + it.pos, navRectRel.max + it.pos) } ?: viewportRect)
    }
    g.navScoringRect translateY navScoringRectOffsetY
    g.navScoringRect.min.x = min(g.navScoringRect.min.x + 1f, g.navScoringRect.max.x)
    g.navScoringRect.max.x = g.navScoringRect.min.x
    // Ensure if we have a finite, non-inverted bounding box here will allows us to remove extraneous abs() calls in navScoreItem().
    assert(!g.navScoringRect.isInverted)
    //g.OverlayDrawList.AddRect(g.NavScoringRectScreen.Min, g.NavScoringRectScreen.Max, IM_COL32(255,200,0,255)); // [DEBUG]
    g.navScoringCount = 0
    if (IMGUI_DEBUG_NAV_RECTS)
        g.navWindow?.let { nav ->
            for (layer in 0..1)
                getForegroundDrawList(nav).addRect(
                    nav.pos + nav.navRectRel[layer].min,
                    nav.pos + nav.navRectRel[layer].max,
                    COL32(255, 200, 0, 255)
                )  // [DEBUG]
            val col = if (!nav.hidden) COL32(255, 0, 255, 255) else COL32(255, 0, 0, 255)
            val p = navCalcPreferredRefPos()
            val buf = "${g.navLayer}".toByteArray()
            getForegroundDrawList(nav).addCircleFilled(p, 3f, col)
            getForegroundDrawList(nav).addText(null, 13f, p + Vec2(8, -4), col, buf)
        }
}

/** Windowing management mode
 *  Keyboard: CTRL+Tab (change focus/move/resize), Alt (toggle menu layer)
 *  Gamepad:  Hold Menu/Square (change focus/move/resize), Tap Menu/Square (toggle menu layer) */
fun navUpdateWindowing() {

    var applyFocusWindow: Window? = null
    var applyToggleLayer = false

    val modalWindow = topMostPopupModal
    val allowWindowing = modalWindow == null
    if (!allowWindowing)
        g.navWindowingTarget = null

    // Fade out
    if (g.navWindowingTargetAnim != null && g.navWindowingTarget == null) {
        g.navWindowingHighlightAlpha = (g.navWindowingHighlightAlpha - io.deltaTime * 10f) max 0f
        if (g.dimBgRatio <= 0f && g.navWindowingHighlightAlpha <= 0f)
            g.navWindowingTargetAnim = null
    }
    // Start CTRL-TAB or Square+L/R window selection
    val startWindowingWithGamepad = allowWindowing && g.navWindowingTarget == null && NavInput.Menu.isTest(InputReadMode.Pressed)
    val startWindowingWithKeyboard = allowWindowing && g.navWindowingTarget == null && io.keyCtrl && Key.Tab.isPressed && io.configFlags has ConfigFlag.NavEnableKeyboard
    if (startWindowingWithGamepad || startWindowingWithKeyboard)
        (g.navWindow ?: findWindowNavFocusable(g.windowsFocusOrder.lastIndex, -Int.MAX_VALUE, -1))?.let {
            g.navWindowingTarget = it.rootWindow // FIXME-DOCK: Will need to use RootWindowDockStop
            g.navWindowingTargetAnim = it.rootWindow // FIXME-DOCK: Will need to use RootWindowDockStop
            g.navWindowingHighlightAlpha = 0f
            g.navWindowingTimer = 0f
            g.navWindowingToggleLayer = !startWindowingWithKeyboard
            g.navInputSource = if (startWindowingWithKeyboard) InputSource.NavKeyboard else InputSource.NavGamepad
        }

    // Gamepad update
    g.navWindowingTimer += io.deltaTime
    g.navWindowingTarget?.let {
        if (g.navInputSource == InputSource.NavGamepad) {
            /*  Highlight only appears after a brief time holding the button, so that a fast tap on PadMenu
                (to toggle NavLayer) doesn't add visual noise             */
            g.navWindowingHighlightAlpha = max(
                g.navWindowingHighlightAlpha,
                saturate((g.navWindowingTimer - NAV_WINDOWING_HIGHLIGHT_DELAY) / 0.05f)
            )

            // Select window to focus
            val focusChangeDir =
                NavInput.FocusPrev.isTest(InputReadMode.RepeatSlow).i - NavInput.FocusNext.isTest(InputReadMode.RepeatSlow).i
            if (focusChangeDir != 0) {
                navUpdateWindowingHighlightWindow(focusChangeDir)
                g.navWindowingHighlightAlpha = 1f
            }

            // Single press toggles NavLayer, long press with L/R apply actual focus on release (until then the window was merely rendered top-most)
            if (!NavInput.Menu.isDown()) {
                // Once button was held long enough we don't consider it a tap-to-toggle-layer press anymore.
                g.navWindowingToggleLayer = g.navWindowingToggleLayer and (g.navWindowingHighlightAlpha < 1f)
                if (g.navWindowingToggleLayer && g.navWindow != null)
                    applyToggleLayer = true
                else if (!g.navWindowingToggleLayer)
                    applyFocusWindow = it
                g.navWindowingTarget = null
            }
        }
    }
    // Keyboard: Focus
    g.navWindowingTarget?.let {
        if (g.navInputSource == InputSource.NavKeyboard) {
            // Visuals only appears after a brief time after pressing TAB the first time, so that a fast CTRL+TAB doesn't add visual noise
            g.navWindowingHighlightAlpha = max(
                g.navWindowingHighlightAlpha,
                saturate((g.navWindowingTimer - NAV_WINDOWING_HIGHLIGHT_DELAY) / 0.05f)
            ) // 1.0f
            if (Key.Tab.isPressed(true))
                navUpdateWindowingHighlightWindow(if (io.keyShift) 1 else -1)
            if (!io.keyCtrl)
                applyFocusWindow = g.navWindowingTarget
        }
    }

    // Keyboard: Press and Release ALT to toggle menu layer
    // FIXME: We lack an explicit IO variable for "is the imgui window focused", so compare mouse validity to detect the common case of back-end clearing releases all keys on ALT-TAB
    if (NavInput._KeyMenu.isTest(InputReadMode.Pressed))
        g.navWindowingToggleLayer = true
    if ((g.activeId == 0 || g.activeIdAllowOverlap) && g.navWindowingToggleLayer && NavInput._KeyMenu.isTest(
            InputReadMode.Released
        )
    )
        if (isMousePosValid(io.mousePos) == isMousePosValid(io.mousePosPrev))
            applyToggleLayer = true

    // Move window
    g.navWindowingTarget?.let {
        if (it.flags hasnt Wf.NoMove) {
            var moveDelta = Vec2()
            if (g.navInputSource == InputSource.NavKeyboard && !io.keyShift)
                moveDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard.i, InputReadMode.Down)
            if (g.navInputSource == InputSource.NavGamepad)
                moveDelta = getNavInputAmount2d(NavDirSourceFlag.PadLStick.i, InputReadMode.Down)
            if (moveDelta.x != 0f || moveDelta.y != 0f) {
                val NAV_MOVE_SPEED = 800f
                val moveSpeed = floor(
                    NAV_MOVE_SPEED * io.deltaTime * min(
                        io.displayFramebufferScale.x,
                        io.displayFramebufferScale.y
                    )
                ) // FIXME: Doesn't handle variable framerate very well
                it.rootWindow!!.apply { // movingWindow
                    setPos(pos + moveDelta * moveSpeed, Cond.Always)
                    markIniSettingsDirty()
                }
                g.navDisableMouseHover = true
            }
        }
    }

    // Apply final focus
    if (applyFocusWindow != null && (g.navWindow == null || applyFocusWindow !== g.navWindow!!.rootWindow)) {
        clearActiveID()
        g.navDisableHighlight = false
        g.navDisableMouseHover = true
        applyFocusWindow = navRestoreLastChildNavWindow(applyFocusWindow!!)
        closePopupsOverWindow(applyFocusWindow, false)
        focusWindow(applyFocusWindow)
        if (applyFocusWindow!!.navLastIds[0] == 0)
            navInitWindow(applyFocusWindow!!, false)

        // If the window only has a menu layer, select it directly
        if (applyFocusWindow!!.dc.navLayerActiveMask == 1 shl NavLayer.Menu)
            g.navLayer = NavLayer.Menu
    }
    applyFocusWindow?.let { g.navWindowingTarget = null }

    // Apply menu/layer toggle
    if (applyToggleLayer)
        g.navWindow?.let {
            // Move to parent menu if necessary
            var newNavWindow = it

            tailrec fun Window.getParent(): Window {
                val parent = parentWindow
                return if (parent != null && dc.navLayerActiveMask hasnt (1 shl NavLayer.Menu) && flags has Wf._ChildWindow && flags hasnt (Wf._Popup or Wf._ChildMenu)) getParent() else this
            }

            newNavWindow = newNavWindow.getParent()

            if (newNavWindow !== it) {
                val oldNavWindow = it
                focusWindow(newNavWindow)
                newNavWindow.navLastChildNavWindow = oldNavWindow
            }
            g.navDisableHighlight = false
            g.navDisableMouseHover = true
            // When entering a regular menu bar with the Alt key, we always reinitialize the navigation ID.
            val newNavLayer = when {
                it.dc.navLayerActiveMask has (1 shl NavLayer.Menu) -> NavLayer of (g.navLayer xor 1)
                else -> NavLayer.Main
            }
            navRestoreLayer(newNavLayer)
        }
}

/** Overlay displayed when using CTRL+TAB. Called by EndFrame(). */
fun navUpdateWindowingOverlay() {

    val target = g.navWindowingTarget!! // ~ assert

    if (g.navWindowingTimer < NAV_WINDOWING_LIST_APPEAR_DELAY) return

    if (g.navWindowingListWindow == null)
        g.navWindowingListWindow = findWindowByName("###NavWindowingList")
    setNextWindowSizeConstraints(Vec2(io.displaySize.x * 0.2f, io.displaySize.y * 0.2f), Vec2(Float.MAX_VALUE))
    setNextWindowPos(Vec2(io.displaySize.x * 0.5f, io.displaySize.y * 0.5f), Cond.Always, Vec2(0.5f))
    pushStyleVar(StyleVar.WindowPadding, style.windowPadding * 2f)
    val flags =
        Wf.NoTitleBar or Wf.NoFocusOnAppearing or Wf.NoResize or Wf.NoMove or Wf.NoInputs or Wf.AlwaysAutoResize or Wf.NoSavedSettings
    begin("###NavWindowingList", null, flags)
    for (n in g.windowsFocusOrder.lastIndex downTo 0) {
        val window = g.windowsFocusOrder[n]
        if (!window.isNavFocusable)
            continue
        var label = window.name
        val labelEnd = findRenderedTextEnd(label)
        if (labelEnd == -1)
            label = window.fallbackWindowName
        selectable(label, target == window)
    }
    end()
    popStyleVar()
}

/** Apply result from previous frame navigation directional move request */
fun navUpdateMoveResult() {

    if (g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0) {
        // In a situation when there is no results but NavId != 0, re-enable the Navigation highlight (because g.NavId is not considered as a possible result)
        if (g.navId != 0) {
            g.navDisableHighlight = false
            g.navDisableMouseHover = true
        }
        return
    }
    // Select which result to use
    var result = if (g.navMoveResultLocal.id != 0) g.navMoveResultLocal else g.navMoveResultOther

    // PageUp/PageDown behavior first jumps to the bottom/top mostly visible item, _otherwise_ use the result from the previous/next page.
    if (g.navMoveRequestFlags has NavMoveFlag.AlsoScoreVisibleSet)
        if (g.navMoveResultLocalVisibleSet.id != 0 && g.navMoveResultLocalVisibleSet.id != g.navId)
            result = g.navMoveResultLocalVisibleSet

    // Maybe entering a flattened child from the outside? In this case solve the tie using the regular scoring rules.
    if (result != g.navMoveResultOther && g.navMoveResultOther.id != 0 && g.navMoveResultOther.window!!.parentWindow === g.navWindow)
        if (g.navMoveResultOther.distBox < result.distBox || (g.navMoveResultOther.distBox == result.distBox && g.navMoveResultOther.distCenter < result.distCenter))
            result = g.navMoveResultOther
    val window = result.window!!
    assert(g.navWindow != null)
    // Scroll to keep newly navigated item fully into view.
    if (g.navLayer == NavLayer.Main) {
        val deltaScroll = Vec2()
        if (g.navMoveRequestFlags has NavMoveFlag.ScrollToEdge) {
            val scrollTarget = if (g.navMoveDir == Dir.Up) window.scrollMax.y else 0f
            deltaScroll.y = window.scroll.y - scrollTarget
            window setScrollY scrollTarget
        } else {
            val rectAbs = Rect(result.rectRel.min + window.pos, result.rectRel.max + window.pos)
            deltaScroll put window.scrollToBringRectIntoView(rectAbs)
        }

        // Offset our result position so mouse position can be applied immediately after in NavUpdate()
        result.rectRel translateX -deltaScroll.x
        result.rectRel translateY -deltaScroll.y
    }

    clearActiveID()
    g.navWindow = window
    if (g.navId != result.id) {
        // Don't set NavJustMovedToId if just landed on the same spot (which may happen with ImGuiNavMoveFlags_AllowCurrentNavId)
        g.navJustMovedToId = result.id
        g.navJustMovedToFocusScopeId = result.focusScopeId

        g.navJustMovedToKeyMods = g.navMoveRequestKeyMods
    }
    IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequest: result NavID 0x%08X in Layer ${g.navLayer} Window \"${window.name}\"", result.id) // [JVM] window *is* g.navWindow!!
    setNavIDWithRectRel(result.id, g.navLayer, result.focusScopeId, result.rectRel)
}

fun navUpdateInitResult() {
    // In very rare cases g.NavWindow may be null (e.g. clearing focus after requesting an init request, which does happen when releasing Alt while clicking on void)
    val nav = g.navWindow ?: return

    // Apply result from previous navigation init request (will typically select the first item, unless SetItemDefaultFocus() has been called)
    IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: result NavID 0x%08X in Layer ${g.navLayer} Window \"${nav.name}\"", g.navInitResultId)
    if (g.navInitRequestFromMove)
        setNavIDWithRectRel(g.navInitResultId, g.navLayer, 0, g.navInitResultRectRel)
    else
        setNavID(g.navInitResultId, g.navLayer, 0)
    nav.navRectRel[g.navLayer] = g.navInitResultRectRel
}

/** Handle PageUp/PageDown/Home/End keys */
fun navUpdatePageUpPageDown(): Float {

    val window = g.navWindow
    if (g.navMoveDir != Dir.None || window == null)
        return 0f
    if (window.flags has Wf.NoNavInputs || g.navWindowingTarget != null || g.navLayer != NavLayer.Main)
        return 0f

    val pageUpHeld = Key.PageUp.isDown && !isActiveIdUsingKey(Key.PageUp)
    val pageDownHeld = Key.PageDown.isDown && !isActiveIdUsingKey(Key.PageDown)
    val homePressed = Key.Home.isPressed && !isActiveIdUsingKey(Key.Home)
    val endPressed = Key.End.isPressed && !isActiveIdUsingKey(Key.End)
    if (pageUpHeld != pageDownHeld || homePressed != endPressed) { // If either (not both) are pressed

        if (window.dc.navLayerActiveMask == 0x00 && window.dc.navHasScroll) {
            // Fallback manual-scroll when window has no navigable item
            when {
                Key.PageUp.isPressed(true) -> window.setScrollY(window.scroll.y - window.innerRect.height)
                Key.PageDown.isPressed(true) -> window.setScrollY(window.scroll.y + window.innerRect.height)
                homePressed -> window setScrollY 0f
                endPressed -> window setScrollY window.scrollMax.y
            }
        } else {
            val navRectRel = window.navRectRel[g.navLayer]
            val pageOffsetY = 0f max (window.innerRect.height - window.calcFontSize() * 1f + navRectRel.height)
            var navScoringRectOffsetY = 0f
            if (Key.PageUp.isPressed(true)) {
                navScoringRectOffsetY = -pageOffsetY
                g.navMoveDir =
                    Dir.Down // Because our scoring rect is offset up, we request the down direction (so we can always land on the last item)
                g.navMoveClipDir = Dir.Up
                g.navMoveRequestFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.AlsoScoreVisibleSet
            } else if (Key.PageDown.isPressed(true)) {
                navScoringRectOffsetY = +pageOffsetY
                g.navMoveDir =
                    Dir.Up // Because our scoring rect is offset down, we request the up direction (so we can always land on the last item)
                g.navMoveClipDir = Dir.Down
                g.navMoveRequestFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.AlsoScoreVisibleSet
            } else if (homePressed) {
                // FIXME-NAV: handling of Home/End is assuming that the top/bottom most item will be visible with Scroll.y == 0/ScrollMax.y
                // Scrolling will be handled via the ImGuiNavMoveFlags_ScrollToEdge flag, we don't scroll immediately to avoid scrolling happening before nav result.
                // Preserve current horizontal position if we have any.
                navRectRel.min.y = -window.scroll.y
                navRectRel.max.y = -window.scroll.y
                if (navRectRel.isInverted) {
                    navRectRel.min.x = 0f
                    navRectRel.max.x = 0f
                }
                g.navMoveDir = Dir.Down
                g.navMoveRequestFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.ScrollToEdge
            } else if (endPressed) {
                navRectRel.min.y = window.scrollMax.y + window.sizeFull.y - window.scroll.y
                navRectRel.max.y = window.scrollMax.y + window.sizeFull.y - window.scroll.y
                if (navRectRel.isInverted) {
                    navRectRel.min.x = 0f
                    navRectRel.max.x = 0f
                }
                g.navMoveDir = Dir.Up
                g.navMoveRequestFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.ScrollToEdge
            }
            return navScoringRectOffsetY
        }
    }
    return 0f
}

fun navUpdateAnyRequestFlag() {
    g.navAnyRequest = g.navMoveRequest || g.navInitRequest || (IMGUI_DEBUG_NAV_SCORING && g.navWindow != null)
    if (g.navAnyRequest)
        assert(g.navWindow != null)
}


fun navEndFrame() {

    // Show CTRL+TAB list window
    if (g.navWindowingTarget != null)
        navUpdateWindowingOverlay()

    // Perform wrap-around in menus
    val window = g.navWrapRequestWindow
    val moveFlags: NavMoveFlags = g.navWrapRequestFlags
    if (window != null && g.navWindow === window && navMoveRequestButNoResultYet() &&
        g.navMoveRequestForward == NavForward.None && g.navLayer == NavLayer.Main
    ) {

        assert(moveFlags != 0) // No points calling this with no wrapping
        val bbRel = Rect(window.navRectRel[0])

        var clipDir = g.navMoveDir
        if (g.navMoveDir == Dir.Left && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
            bbRel.max.x = max(window.sizeFull.x, window.contentSize.x + window.windowPadding.x * 2f) - window.scroll.x
            bbRel.min.x = bbRel.max.x

            if (moveFlags has NavMoveFlag.WrapX) {
                bbRel.translateY(-bbRel.height)
                clipDir = Dir.Up
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Right && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
            bbRel.max.x = -window.scroll.x
            bbRel.min.x = bbRel.max.x
            if (moveFlags has NavMoveFlag.WrapX) {
                bbRel.translateY(+bbRel.height)
                clipDir = Dir.Down
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Up && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
            bbRel.max.y = max(window.sizeFull.y, window.contentSize.y + window.windowPadding.y * 2f) - window.scroll.y
            bbRel.min.y = bbRel.max.y
            if (moveFlags has NavMoveFlag.WrapY) {
                bbRel.translateX(-bbRel.width)
                clipDir = Dir.Left
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Down && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
            bbRel.max.y = -window.scroll.y
            bbRel.min.y = bbRel.max.y
            if (moveFlags has NavMoveFlag.WrapY) {
                bbRel.translateX(+bbRel.width)
                clipDir = Dir.Right
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
    }
}


/** Scoring function for gamepad/keyboard directional navigation. Based on https://gist.github.com/rygorous/6981057  */
fun navScoreItem(result: NavMoveResult, cand: Rect): Boolean {

    val window = g.currentWindow!!
    if (g.navLayer != window.dc.navLayerCurrent) return false

    // Current modified source rect (NB: we've applied max.x = min.x in navUpdate() to inhibit the effect of having varied item width)
    val curr = Rect(g.navScoringRect)
    g.navScoringCount++

    // When entering through a NavFlattened border, we consider child window items as fully clipped for scoring
    if (window.parentWindow === g.navWindow) {
        assert((window.flags or g.navWindow!!.flags) has Wf._NavFlattened)
        if (!cand.overlaps(window.clipRect))
            return false
        cand clipWithFull window.clipRect // This allows the scored item to not overlap other candidates in the parent window
    }

    /*  We perform scoring on items bounding box clipped by the current clipping rectangle on the other axis
        (clipping on our movement axis would give us equal scores for all clipped items)
        For example, this ensure that items in one column are not reached when moving vertically from items in another column. */
    navClampRectToVisibleAreaForMoveDir(g.navMoveDir, cand, window.clipRect)

    // Compute distance between boxes
    // FIXME-NAV: Introducing biases for vertical navigation, needs to be removed.
    var dbX = navScoreItemDistInterval(cand.min.x, cand.max.x, curr.min.x, curr.max.x)
    // Scale down on Y to keep using box-distance for vertically touching items
    val dbY = navScoreItemDistInterval(
        lerp(cand.min.y, cand.max.y, 0.2f), lerp(cand.min.y, cand.max.y, 0.8f),
        lerp(curr.min.y, curr.max.y, 0.2f), lerp(curr.min.y, curr.max.y, 0.8f)
    )
    if (dbY != 0f && dbX != 0f)
        dbX = dbX / 1000f + if (dbX > 0f) 1f else -1f
    val distBox = abs(dbX) + abs(dbY)

    // Compute distance between centers (this is off by a factor of 2, but we only compare center distances with each other so it doesn't matter)
    val dcX = (cand.min.x + cand.max.x) - (curr.min.x + curr.max.x)
    val dcY = (cand.min.y + cand.max.y) - (curr.min.y + curr.max.y)
    val distCenter = abs(dcX) + abs(dcY) // L1 metric (need this for our connectedness guarantee)

    // Determine which quadrant of 'curr' our candidate item 'cand' lies in based on distance
    val quadrant: Dir
    var dax = 0f
    var day = 0f
    var distAxial = 0f
    if (dbX != 0f || dbY != 0f) {
        // For non-overlapping boxes, use distance between boxes
        dax = dbX
        day = dbY
        distAxial = distBox
        quadrant = getDirQuadrantFromDelta(dbX, dbY)
    } else if (dcX != 0f || dcY != 0f) {
        // For overlapping boxes with different centers, use distance between centers
        dax = dcX
        day = dcY
        distAxial = distCenter
        quadrant = getDirQuadrantFromDelta(dcX, dcY)
    }
    /* Degenerate case: two overlapping buttons with same center, break ties arbitrarily (note that lastItemId here is
        really the _previous_ item order, but it doesn't matter)     */
    else
        quadrant = if (window.dc.lastItemId < g.navId) Dir.Left else Dir.Right

    if (IMGUI_DEBUG_NAV_SCORING)
        if (isMouseHoveringRect(cand)) {
            val buf =
                "dbox (%.2f,%.2f->%.4f)\ndcen (%.2f,%.2f->%.4f)\nd (%.2f,%.2f->%.4f)\nnav WENS${g.navMoveDir}, quadrant WENS$quadrant"
                    .format(style.locale, dbX, dbY, distBox, dcX, dcY, distCenter, dax, day, distAxial).toByteArray()
            getForegroundDrawList(window).apply {
                addRect(curr.min, curr.max, COL32(255, 200, 0, 100))
                addRect(cand.min, cand.max, COL32(255, 255, 0, 200))
                addRectFilled(cand.max - Vec2(4), cand.max + calcTextSize(buf, 0) + Vec2(4), COL32(40, 0, 0, 150))
                addText(io.fontDefault, 13f, cand.max, 0.inv(), buf)
            }
        } else if (io.keyCtrl) { // Hold to preview score in matching quadrant. Press C to rotate.
            if (Key.C.isPressed) {
                g.navMoveDirLast = Dir.of((g.navMoveDirLast.i + 1) and 3)
                io.keysDownDuration[io.keyMap[Key.C]] = 0.01f
            }
            if (quadrant == g.navMoveDir) {
                val buf = "%.0f/%.0f".format(style.locale, distBox, distCenter).toByteArray()
                getForegroundDrawList(window).apply {
                    addRectFilled(cand.min, cand.max, COL32(255, 0, 0, 200))
                    addText(io.fontDefault, 13f, cand.min, COL32(255), buf)
                }
            }
        }

    // Is it in the quadrant we're interesting in moving to?
    var newBest = false
    if (quadrant == g.navMoveDir) {
        // Does it beat the current best candidate?
        if (distBox < result.distBox) {
            result.distBox = distBox
            result.distCenter = distCenter
            return true
        }
        if (distBox == result.distBox) {
            // Try using distance between center points to break ties
            if (distCenter < result.distCenter) {
                result.distCenter = distCenter
                newBest = true
            } else if (distCenter == result.distCenter) {
                /*  Still tied! we need to be extra-careful to make sure everything gets linked properly.
                    We consistently break ties by symbolically moving "later" items (with higher index) to the
                    right/downwards by an infinitesimal amount since we the current "best" button already
                    (so it must have a lower index), this is fairly easy.
                    This rule ensures that all buttons with dx == dy == 0 will end up being linked in order
                    of appearance along the x axis. */
                val db = if (g.navMoveDir == Dir.Up || g.navMoveDir == Dir.Down) dbY else dbX
                if (db < 0f) // moving bj to the right/down decreases distance
                    newBest = true
            }
        }
    }

    /*  Axial check: if 'curr' has no link at all in some direction and 'cand' lies roughly in that direction,
        add a tentative link. This will only be kept if no "real" matches are found, so it only augments the graph
        produced by the above method using extra links. (important, since it doesn't guarantee strong connectedness)
        This is just to avoid buttons having no links in a particular direction when there's a suitable neighbor.
        You get good graphs without this too.
        2017/09/29: FIXME: This now currently only enabled inside menu bars, ideally we'd disable it everywhere.
        Menus in particular need to catch failure. For general navigation it feels awkward.
        Disabling it may lead to disconnected graphs when nodes are very spaced out on different axis.
        Perhaps consider offering this as an option?    */
    if (result.distBox == Float.MAX_VALUE && distAxial < result.distAxial)  // Check axial match
        if (g.navLayer == NavLayer.Menu && g.navWindow!!.flags hasnt Wf._ChildMenu)
            if ((g.navMoveDir == Dir.Left && dax < 0f) || (g.navMoveDir == Dir.Right && dax > 0f) ||
                (g.navMoveDir == Dir.Up && day < 0f) || (g.navMoveDir == Dir.Down && day > 0f)
            ) {
                result.distAxial = distAxial
                newBest = true
            }

    return newBest
}

/** We get there when either navId == id, or when g.navAnyRequest is set (which is updated by navUpdateAnyRequestFlag above)    */
fun navProcessItem(window: Window, navBb: Rect, id: ID) {

    //if (!g.io.NavActive)  // [2017/10/06] Removed this possibly redundant test but I am not sure of all the side-effects yet. Some of the feature here will need to work regardless of using a _NoNavInputs flag.
    //    return;

    val itemFlags = window.dc.itemFlags
    val navBbRel = Rect(navBb.min - window.pos, navBb.max - window.pos)

    // Process Init Request
    if (g.navInitRequest && g.navLayer == window.dc.navLayerCurrent) {
        // Even if 'ImGuiItemFlags_NoNavDefaultFocus' is on (typically collapse/close button) we record the first ResultId so they can be used as a fallback
        if (itemFlags hasnt If.NoNavDefaultFocus || g.navInitResultId == 0) {
            g.navInitResultId = id
            g.navInitResultRectRel = navBbRel
        }
        if (itemFlags hasnt If.NoNavDefaultFocus) {
            g.navInitRequest = false // Found a match, clear request
            navUpdateAnyRequestFlag()
        }
    }

    /*  Process Move Request (scoring for navigation)
        FIXME-NAV: Consider policy for double scoring
        (scoring from NavScoringRectScreen + scoring from a rect wrapped according to current wrapping policy)     */
    if ((g.navId != id || g.navMoveRequestFlags has NavMoveFlag.AllowCurrentNavId) && itemFlags hasnt (If.Disabled or If.NoNav)) {
        var result by if (window === g.navWindow) g::navMoveResultLocal else g::navMoveResultOther
        val newBest = when {
            IMGUI_DEBUG_NAV_SCORING -> {  // [DEBUG] Score all items in NavWindow at all times
                if (!g.navMoveRequest) g.navMoveDir = g.navMoveDirLast
                navScoreItem(result, navBb) && g.navMoveRequest
            }
            else -> g.navMoveRequest && navScoreItem(result, navBb)
        }
        if (newBest) {
            result.window = window
            result.id = id
            result.focusScopeId = window.dc.navFocusScopeIdCurrent
            result.rectRel put navBbRel
        }

        // Features like PageUp/PageDown need to maintain a separate score for the visible set of items.
        val VISIBLE_RATIO = 0.7f
        if (g.navMoveRequestFlags has NavMoveFlag.AlsoScoreVisibleSet && window.clipRect overlaps navBb)
            if (glm.clamp(navBb.max.y, window.clipRect.min.y, window.clipRect.max.y) -
                glm.clamp(
                    navBb.min.y,
                    window.clipRect.min.y,
                    window.clipRect.max.y
                ) >= (navBb.max.y - navBb.min.y) * VISIBLE_RATIO
            )
                if (navScoreItem(g.navMoveResultLocalVisibleSet, navBb))
                    result = g.navMoveResultLocalVisibleSet.also {
                        it.window = window
                        it.id = id
                        it.focusScopeId = window.dc.navFocusScopeIdCurrent
                        it.rectRel = navBbRel
                    }
    }

    // Update window-relative bounding box of navigated item
    if (g.navId == id) {
        g.navWindow =
            window    // Always refresh g.NavWindow, because some operations such as FocusItem() don't have a window.
        g.navLayer = window.dc.navLayerCurrent
        g.navFocusScopeId = window.dc.navFocusScopeIdCurrent
        g.navIdIsAlive = true
        g.navIdTabCounter = window.dc.focusCounterTabStop
        window.navRectRel[window.dc.navLayerCurrent] =
            navBbRel    // Store item bounding box (relative to window position)
    }
}

fun navCalcPreferredRefPos(): Vec2 {
    if (g.navDisableHighlight || !g.navDisableMouseHover || g.navWindow == null) {
        // Mouse (we need a fallback in case the mouse becomes invalid after being used)
        if (isMousePosValid(io.mousePos))
            return Vec2(io.mousePos)
        return Vec2(g.lastValidMousePos)
    } else {
        // When navigation is active and mouse is disabled, decide on an arbitrary position around the bottom left of the currently navigated item.
        val rectRel = g.navWindow!!.navRectRel[g.navLayer]
        val pos = g.navWindow!!.pos + Vec2(
            rectRel.min.x + min(style.framePadding.x * 4, rectRel.width),
            rectRel.max.y - min(style.framePadding.y, rectRel.height)
        )
        val visibleRect = viewportRect
        return glm.floor(
            glm.clamp(
                pos,
                visibleRect.min,
                visibleRect.max
            )
        )   // ImFloor() is important because non-integer mouse position application in back-end might be lossy and result in undesirable non-zero delta.
    }
}

/** FIXME: This could be replaced by updating a frame number in each window when (window == NavWindow) and (NavLayer == 0).
 *  This way we could find the last focused window among our children. It would be much less confusing this way? */
fun navSaveLastChildNavWindowIntoParent(navWindow: Window?) {

    tailrec fun Window.getParent(): Window {
        val parent = parentWindow
        return when {
            parent != null && flags has Wf._ChildWindow && flags hasnt (Wf._Popup or Wf._ChildMenu) -> parent.getParent()
            else -> this
        }
    }

    navWindow?.getParent()?.let { if (it !== navWindow) it.navLastChildNavWindow = navWindow }
}

/** Restore the last focused child.
 *  Call when we are expected to land on the Main Layer (0) after FocusWindow()    */
fun navRestoreLastChildNavWindow(window: Window) = window.navLastChildNavWindow?.takeIf { it.wasActive } ?: window

// FIXME-OPT O(N)
fun findWindowFocusIndex(window: Window): Int {
    var i = g.windowsFocusOrder.lastIndex
    while (i >= 0) {
        if (g.windowsFocusOrder[i] == window)
            return i
        i--
    }
    return -1
}

// static spare functions

fun navRestoreLayer(layer: NavLayer) {

    g.navLayer = layer
    if (layer == NavLayer.Main)
        g.navWindow = navRestoreLastChildNavWindow(g.navWindow!!)
    val window = g.navWindow!!
    if (layer == NavLayer.Main && window.navLastIds[0] != 0)
        setNavIDWithRectRel(window.navLastIds[0], layer, 0, window.navRectRel[0])
    else
        navInitWindow(window, true)
}

fun navScoreItemDistInterval(a0: Float, a1: Float, b0: Float, b1: Float) = when {
    a1 < b0 -> a1 - b0
    b1 < a0 -> a0 - b1
    else -> 0f
}

fun navClampRectToVisibleAreaForMoveDir(moveDir: Dir, r: Rect, clipRect: Rect) = when (moveDir) {
    Dir.Left, Dir.Right -> {
        r.min.y = glm.clamp(r.min.y, clipRect.min.y, clipRect.max.y)
        r.max.y = glm.clamp(r.max.y, clipRect.min.y, clipRect.max.y)
    }
    else -> {
        r.min.x = glm.clamp(r.min.x, clipRect.min.x, clipRect.max.x)
        r.max.x = glm.clamp(r.max.x, clipRect.min.x, clipRect.max.x)
    }
}