package imgui.static

import gli_.has
import gli_.hasnt
import glm_.*
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
import imgui.ImGui.getKeyVector2d
import imgui.ImGui.io
import imgui.ImGui.isActiveIdUsingKey
import imgui.ImGui.isActiveIdUsingNavDir
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.isMousePosValid
import imgui.ImGui.mainViewport
import imgui.ImGui.navInitRequestApplyResult
import imgui.ImGui.navInitWindow
import imgui.ImGui.navMoveRequestApplyResult
import imgui.ImGui.navMoveRequestButNoResultYet
import imgui.ImGui.navMoveRequestForward
import imgui.ImGui.navMoveRequestResolveWithLastItem
import imgui.ImGui.navMoveRequestSubmit
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.selectable
import imgui.ImGui.setNavID
import imgui.ImGui.setNavWindow
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.style
import imgui.ImGui.topMostPopupModal
import imgui.api.g
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import imgui.WindowFlag as Wf
import imgui.internal.sections.ItemFlag as If

fun navUpdate() {

    io.wantSetMousePos = false

    //if (g.NavScoringDebugCount > 0) IMGUI_DEBUG_LOG_NAV("[nav] NavScoringDebugCount %d for '%s' layer %d (Init:%d, Move:%d)\n", g.NavScoringDebugCount, g.NavWindow ? g.NavWindow->Name : "NULL", g.NavLayer, g.NavInitRequest || g.NavInitResultId != 0, g.NavMoveRequest);

    // Set input source based on which keys are last pressed (as some features differs when used with Gamepad vs Keyboard)
    // FIXME-NAV: Now that keys are separated maybe we can get rid of NavInputSource?
    val navGamepadActive = io.configFlags has ConfigFlag.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad
    val navGamepadKeysToChangeSource = listOf(Key.GamepadFaceRight, Key.GamepadFaceLeft, Key.GamepadFaceUp, Key.GamepadFaceDown, Key.GamepadDpadRight, Key.GamepadDpadLeft, Key.GamepadDpadUp, Key.GamepadDpadDown)
    if (navGamepadActive)
        for (key in navGamepadKeysToChangeSource)
            if (key.isDown)
                g.navInputSource = InputSource.Gamepad
    val navKeyboardActive = io.configFlags has ConfigFlag.NavEnableKeyboard
    if (navKeyboardActive) {
        fun navMapKey(key: Key, navInput: NavInput) {
            if (key.isDown) {
                io.navInputs[navInput] = 1f
                g.navInputSource = InputSource.Keyboard
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

    }
    for (i in io.navInputsDownDuration.indices)
        io.navInputsDownDurationPrev[i] = io.navInputsDownDuration[i]
    for (i in io.navInputs.indices)
        io.navInputsDownDuration[i] = when (io.navInputs[i] > 0f) {
            true -> if (io.navInputsDownDuration[i] < 0f) 0f else io.navInputsDownDuration[i] + io.deltaTime
            else -> -1f
        }

    // Process navigation init request (select first/default focus)
    if (g.navInitResultId != 0)
        navInitRequestApplyResult()
    g.navInitRequest = false
    g.navInitRequestFromMove = false
    g.navInitResultId = 0
    g.navJustMovedToId = 0

    // Process navigation move request
    if (g.navMoveSubmitted)
        navMoveRequestApplyResult()
    g.navTabbingCounter = 0
    g.navMoveSubmitted = false; g.navMoveScoringItems = false

    // Schedule mouse position update (will be done at the bottom of this function, after 1) processing all move requests and 2) updating scrolling)
    var setMousePos = false
    if (g.navMousePosDirty && g.navIdIsAlive)
        if (!g.navDisableHighlight && g.navDisableMouseHover && g.navWindow != null)
            setMousePos = true
    g.navMousePosDirty = false
    // [JVM] useless
    //    IM_ASSERT(g.NavLayer == ImGuiNavLayer_Main || g.NavLayer == ImGuiNavLayer_Menu)

    // Store our return window (for returning from Menu Layer to Main Layer) and clear it as soon as we step back in our own Layer 0
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
    navUpdateCancelRequest()

    // Process manual activation request
    g.navActivateId = 0; g.navActivateDownId = 0; g.navActivatePressedId = 0; g.navActivateInputId = 0
    g.navActivateFlags = ActivateFlag.None.i
    if (g.navId != 0 && !g.navDisableHighlight && g.navWindowingTarget == null && g.navWindow != null && g.navWindow!!.flags hasnt Wf.NoNavInputs) {
        val activateDown = (navKeyboardActive && Key.Space.isDown) || (navGamepadActive && Key._NavGamepadActivate.isDown)
        val activatePressed = activateDown && ((navKeyboardActive && Key.Space isPressed false) || (navGamepadActive && Key._NavGamepadActivate isPressed false))
        val inputDown = (navKeyboardActive && Key.Enter.isDown) || (navGamepadActive && Key._NavGamepadInput.isDown)
        val inputPressed = inputDown && ((navKeyboardActive && Key.Enter isPressed false) || (navGamepadActive && Key._NavGamepadInput isPressed false))
        if (g.activeId == 0 && activatePressed) {
            g.navActivateId = g.navId
            g.navActivateFlags = ActivateFlag.PreferTweak.i
        }
        if ((g.activeId == 0 || g.activeId == g.navId) && inputPressed) {
            g.navActivateInputId = g.navId
            g.navActivateFlags = ActivateFlag.PreferInput.i
        }
        if ((g.activeId == 0 || g.activeId == g.navId) && activateDown)
            g.navActivateDownId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && activatePressed)
            g.navActivatePressedId = g.navId
    }
    g.navWindow?.let { if (it.flags has Wf.NoNavInputs) g.navDisableHighlight = true }
    if (g.navActivateId != 0)
        assert(g.navActivateDownId == g.navActivateId)

    // Process programmatic activation request
    // FIXME-NAV: Those should eventually be queued (unlike focus they don't cancel each others)
    if (g.navNextActivateId != 0) {
        if (g.navNextActivateFlags has ActivateFlag.PreferInput)
            g.navActivateInputId = g.navNextActivateId
        else
            g.navActivateId = g.navNextActivateId; g.navActivateDownId = g.navNextActivateId; g.navActivatePressedId = g.navNextActivateId
        g.navActivateFlags = g.navNextActivateFlags
    }
    g.navNextActivateId = 0

    // Process move requests
    navUpdateCreateMoveRequest()
    if (g.navMoveDir == Dir.None)
        navUpdateCreateTabbingRequest()
    navUpdateAnyRequestFlag()
    g.navIdIsAlive = false

    // Scrolling
    val navWindow = g.navWindow
    if (navWindow != null && navWindow.flags hasnt Wf.NoNavInputs && g.navWindowingTarget == null) {
        // *Fallback* manual-scroll with Nav directional keys when window has no navigable item
        val window = navWindow
        val scrollSpeed = round(window.calcFontSize() * 100 * io.deltaTime) // We need round the scrolling speed because sub-pixel scroll isn't reliably supported.
        val moveDir = g.navMoveDir
        if (window.dc.navLayersActiveMask == 0x00 && window.dc.navHasScroll && moveDir != Dir.None) {
            if (moveDir == Dir.Left || moveDir == Dir.Right)
                window.setScrollX(floor(window.scroll.x + (if (moveDir == Dir.Left) -1f else +1f) * scrollSpeed))
            if (moveDir == Dir.Up || moveDir == Dir.Down)
                window.setScrollY(floor(window.scroll.y + (if (moveDir == Dir.Up) -1f else +1f) * scrollSpeed))
        }

        // *Normal* Manual scroll with LStick
        // Next movement request will clamp the NavId reference rectangle to the visible area, so navigation will resume within those bounds.
        if (navGamepadActive) {
            val scrollDir = getKeyVector2d(Key.GamepadLStickLeft, Key.GamepadLStickRight, Key.GamepadLStickUp, Key.GamepadLStickDown)
            val tweakFactor = if (Key._NavGamepadTweakSlow.isDown) 1f / 10f else if (Key._NavGamepadTweakFast.isDown) 10f else 1f
            if (scrollDir.x != 0f && window.scrollbar.x)
                window.setScrollX(floor(window.scroll.x + scrollDir.x * scrollSpeed * tweakFactor))
            if (scrollDir.y != 0f)
                window.setScrollY(floor(window.scroll.y + scrollDir.y * scrollSpeed * tweakFactor))
        }
    }

    // Always prioritize mouse highlight if navigation is disabled
    if (!navKeyboardActive && !navGamepadActive) {
        g.navDisableHighlight = true
        g.navDisableMouseHover = true; setMousePos = false
    }

    // Update mouse position if requested
    // (This will take into account the possibility that a Scroll was queued in the window to offset our absolute mouse position before scroll has been applied)
    if (setMousePos && io.configFlags has ConfigFlag.NavEnableSetMousePos && io.backendFlags has BackendFlag.HasSetMousePos) {
        io.mousePos = navCalcPreferredRefPos(); io.mousePosPrev put io.mousePos
        io.wantSetMousePos = true
        //IMGUI_DEBUG_LOG_IO("SetMousePos: (%.1f,%.1f)\n", io.MousePos.x, io.MousePos.y);
    }

    // [DEBUG]
    g.navScoringDebugCount = 0
    //    #if IMGUI_DEBUG_NAV_RECTS
    //    if (g.NavWindow) {
    //        ImDrawList * draw_list = GetForegroundDrawList(g.NavWindow)
    //        if (1) { for (int layer = 0; layer < 2; layer++) { ImRect r = WindowRectRelToAbs(g.NavWindow, g.NavWindow->NavRectRel[layer]); draw_list->AddRect(r.Min, r.Max, IM_COL32(255,200,0,255)); } } // [DEBUG]
    //        if (1) { ImU32 col =(!g.NavWindow->Hidden) ? IM_COL32(255, 0, 255, 255) : IM_COL32(255, 0, 0, 255); ImVec2 p = NavCalcPreferredRefPos (); char buf [32]; ImFormatString(buf, 32, "%d", g.NavLayer); draw_list->AddCircleFilled(p, 3.0f, col); draw_list->AddText(NULL, 13.0f, p+ImVec2(8, -4), col, buf); }
    //    }
    //    #endif
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
    // Start CTRL+TAB or Square+L/R window selection
    val navGamepadActive = io.configFlags has ConfigFlag.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad
    val navKeyboardActive = io.configFlags has ConfigFlag.NavEnableKeyboard
    val startWindowingWithGamepad = allowWindowing && navGamepadActive && g.navWindowingTarget == null && Key._NavGamepadMenu.isPressed(false)
    val startWindowingWithKeyboard = allowWindowing && g.navWindowingTarget == null && io.keyCtrl && Key.Tab.isPressed(false) // Note: enabled even without NavEnableKeyboard!
    if (startWindowingWithGamepad || startWindowingWithKeyboard)
        (g.navWindow ?: findWindowNavFocusable(g.windowsFocusOrder.lastIndex, -Int.MAX_VALUE, -1))?.let {
            g.navWindowingTarget = it.rootWindow; g.navWindowingTargetAnim = it.rootWindow
            g.navWindowingTimer = 0f; g.navWindowingHighlightAlpha = 0f
            g.navWindowingAccumDeltaPos put 0f; g.navWindowingAccumDeltaSize put 0f
            g.navWindowingToggleLayer = startWindowingWithGamepad // Gamepad starts toggling layer
            g.navInputSource = if (startWindowingWithKeyboard) InputSource.Keyboard else InputSource.Gamepad
        }

    // Gamepad update
    g.navWindowingTimer += io.deltaTime
    g.navWindowingTarget?.let {
        if (g.navInputSource == InputSource.Gamepad) {
            /*  Highlight only appears after a brief time holding the button, so that a fast tap on PadMenu
                (to toggle NavLayer) doesn't add visual noise             */
            g.navWindowingHighlightAlpha = max(g.navWindowingHighlightAlpha, saturate((g.navWindowingTimer - NAV_WINDOWING_HIGHLIGHT_DELAY) / 0.05f))

            // Select window to focus
            val focusChangeDir = Key.GamepadL1.isPressed.i - Key.GamepadR1.isPressed.i
            if (focusChangeDir != 0) {
                navUpdateWindowingHighlightWindow(focusChangeDir)
                g.navWindowingHighlightAlpha = 1f
            }

            // Single press toggles NavLayer, long press with L/R apply actual focus on release (until then the window was merely rendered top-most)
            if (!Key._NavGamepadMenu.isDown) {
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
        if (g.navInputSource == InputSource.Keyboard) {
            // Visuals only appears after a brief time after pressing TAB the first time, so that a fast CTRL+TAB doesn't add visual noise
            g.navWindowingHighlightAlpha = g.navWindowingHighlightAlpha max saturate((g.navWindowingTimer - NAV_WINDOWING_HIGHLIGHT_DELAY) / 0.05f) // 1.0f
            if (Key.Tab isPressed true)
                navUpdateWindowingHighlightWindow(if (io.keyShift) 1 else -1)
            if (!io.keyCtrl)
                applyFocusWindow = g.navWindowingTarget
        }
    }

    // Keyboard: Press and Release ALT to toggle menu layer
    // - Testing that only Alt is tested prevents Alt+Shift or AltGR from toggling menu layer.
    // - AltGR is normally Alt+Ctrl but we can't reliably detect it (not all backends/systems/layout emit it as Alt+Ctrl). But even on keyboards without AltGR we don't want Alt+Ctrl to open menu anyway.
    if (navKeyboardActive && Key.ModAlt.isPressed) {
        g.navWindowingToggleLayer = true
        g.navInputSource = InputSource.Keyboard
    }
    if (g.navWindowingToggleLayer && g.navInputSource == InputSource.Keyboard) {
        // We cancel toggling nav layer when any text has been typed (generally while holding Alt). (See #370)
        // We cancel toggling nav layer when other modifiers are pressed. (See #4439)
        if (io.inputQueueCharacters.isNotEmpty() || io.keyCtrl || io.keyShift || io.keySuper)
            g.navWindowingToggleLayer = false

        // Apply layer toggle on release
        // Important: as before version <18314 we lacked an explicit IO event for focus gain/loss, we also compare mouse validity to detect old backends clearing mouse pos on focus loss.
        if (Key.ModAlt.isReleased && g.navWindowingToggleLayer)
            if (g.activeId == 0 || g.activeIdAllowOverlap)
                if (isMousePosValid(io.mousePos) == isMousePosValid(io.mousePosPrev))
                    applyToggleLayer = true
        if (!Key.ModAlt.isDown)
            g.navWindowingToggleLayer = false
    }

    // Move window
    g.navWindowingTarget?.let {
        if (it.flags hasnt Wf.NoMove) {
            var navMoveDir = Vec2()
            if (g.navInputSource == InputSource.Keyboard && !io.keyShift)
                navMoveDir put getKeyVector2d(Key.LeftArrow, Key.RightArrow, Key.UpArrow, Key.DownArrow)
            if (g.navInputSource == InputSource.Gamepad)
                navMoveDir put getKeyVector2d(Key.GamepadLStickLeft, Key.GamepadLStickRight, Key.GamepadLStickUp, Key.GamepadLStickDown)
            if (navMoveDir.x != 0f || navMoveDir.y != 0f) {
                val NAV_MOVE_SPEED = 800f
                val moveStep = NAV_MOVE_SPEED * io.deltaTime * min(io.displayFramebufferScale.x, io.displayFramebufferScale.y) // FIXME: Doesn't handle variable framerate very well
                g.navWindowingAccumDeltaPos += navMoveDir * moveStep
                g.navDisableMouseHover = true
                val accumFloored = floor(g.navWindowingAccumDeltaPos)
                if (accumFloored.x != 0f || accumFloored.y != 0f)
                    it.rootWindow!!.apply { // movingWindow
                        setPos(pos + accumFloored, Cond.Always)
                        g.navWindowingAccumDeltaPos -= accumFloored
                    }
            }
        }
    }

    // Apply final focus
    if (applyFocusWindow != null && (g.navWindow == null || applyFocusWindow !== g.navWindow!!.rootWindow)) {
        clearActiveID()
        navRestoreHighlightAfterMove()
        applyFocusWindow = navRestoreLastChildNavWindow(applyFocusWindow!!)
        closePopupsOverWindow(applyFocusWindow, false)
        focusWindow(applyFocusWindow)
        if (applyFocusWindow!!.navLastIds[0] == 0)
            navInitWindow(applyFocusWindow!!, false)

        // If the window has ONLY a menu layer (no main layer), select it directly
        // Use NavLayersActiveMaskNext since windows didn't have a chance to be Begin()-ed on this frame,
        // so CTRL+Tab where the keys are only held for 1 frame will be able to use correct layers mask since
        // the target window as already been previewed once.
        // FIXME-NAV: This should be done in NavInit.. or in FocusWindow... However in both of those cases,
        // we won't have a guarantee that windows has been visible before and therefore NavLayersActiveMask*
        // won't be valid.
        if (applyFocusWindow!!.dc.navLayersActiveMaskNext == 1 shl NavLayer.Menu)
            g.navLayer = NavLayer.Menu
    }
    applyFocusWindow?.let { g.navWindowingTarget = null }

    // Apply menu/layer toggle
    if (applyToggleLayer)
        clearActiveID()

    g.navWindow?.let {
        // Move to parent menu if necessary
        var newNavWindow = it

        tailrec fun Window.getParent(): Window { // TODO, now we can use construct `parent?.`..
            val parent = parentWindow
            return when {
                parent != null && dc.navLayersActiveMask hasnt (1 shl NavLayer.Menu) && flags has Wf._ChildWindow && flags hasnt (Wf._Popup or Wf._ChildMenu) -> parent.getParent()
                else -> this
            }
        }

        newNavWindow = newNavWindow.getParent()

        if (newNavWindow !== it) {
            val oldNavWindow = it
            focusWindow(newNavWindow)
            newNavWindow.navLastChildNavWindow = oldNavWindow
        }

        // Toggle layer
        val newNavLayer = when {
            it.dc.navLayersActiveMask has (1 shl NavLayer.Menu) -> NavLayer of (g.navLayer xor 1)
            else -> NavLayer.Main
        }
        if (newNavLayer != g.navLayer) {
            // Reinitialize navigation when entering menu bar with the Alt key (FIXME: could be a properly of the layer?)
            if (newNavLayer == NavLayer.Menu)
                g.navWindow!!.navLastIds[newNavLayer] = 0
            navRestoreLayer(newNavLayer)
            navRestoreHighlightAfterMove()
        }
    }
}

/** Overlay displayed when using CTRL+TAB. Called by EndFrame(). */
fun navUpdateWindowingOverlay() {

    val target = g.navWindowingTarget!! // ~ assert

    if (g.navWindowingTimer < NAV_WINDOWING_LIST_APPEAR_DELAY) return

    if (g.navWindowingListWindow == null)
        g.navWindowingListWindow = findWindowByName("###NavWindowingList")
    val viewport = mainViewport
    setNextWindowSizeConstraints(Vec2(viewport.size.x * 0.2f, viewport.size.y * 0.2f), Vec2(Float.MAX_VALUE, Float.MAX_VALUE))
    setNextWindowPos(viewport.center, Cond.Always, Vec2(0.5f))
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

/** Process NavCancel input (to close a popup, get back to parent, clear focus)
 *  FIXME: In order to support e.g. Escape to clear a selection we'll need:
 *  - either to store the equivalent of ActiveIdUsingKeyInputMask for a FocusScope and test for it.
 *  - either to move most/all of those tests to the epilogue/end functions of the scope they are dealing with (e.g. exit child window in EndChild()) or in EndFrame(), to allow an earlier intercept */
fun navUpdateCancelRequest() {

    val navGamepadActive = g.io.configFlags has ConfigFlag.NavEnableGamepad && g.io.backendFlags has BackendFlag.HasGamepad
    val navKeyboardActive = g.io.configFlags has ConfigFlag.NavEnableKeyboard
    if (!(navKeyboardActive && Key.Escape.isPressed(false)) && !(navGamepadActive && Key._NavGamepadCancel.isPressed(false)))
        return

    IMGUI_DEBUG_LOG_NAV("[nav] NavUpdateCancelRequest")
    val navWindow = g.navWindow
    if (g.activeId != 0) {
        if (!isActiveIdUsingKey(Key.Escape) && !isActiveIdUsingKey(Key._NavGamepadCancel))
            clearActiveID()
    } else if (g.navLayer != NavLayer.Main) {
        // Leave the "menu" layer
        navRestoreLayer(NavLayer.Main)
        navRestoreHighlightAfterMove()
    } else if (navWindow != null && navWindow !== navWindow.rootWindow && navWindow.flags hasnt Wf._Popup && navWindow.parentWindow != null) {
        // Exit child window
        val childWindow = navWindow
        val parentWindow = navWindow.parentWindow!!
        assert(childWindow.childId != 0)
        val childRect = childWindow.rect()
        focusWindow(parentWindow)
        setNavID(childWindow.childId, NavLayer.Main, 0, parentWindow rectAbsToRel childRect)
        navRestoreHighlightAfterMove()
    } else if (g.openPopupStack.isNotEmpty() && g.openPopupStack.last().window!!.flags has Wf._Modal)
    // Close open popup/menu
        closePopupToLevel(g.openPopupStack.lastIndex, true)
    else {
        // Clear NavLastId for popups but keep it for regular child window so we can leave one and come back where we were
        if (navWindow != null && (navWindow.flags has Wf._Popup || navWindow.flags hasnt Wf._ChildWindow))
            navWindow.navLastIds[0] = 0
        g.navId = 0; g.navFocusScopeId = 0
    }
}

fun navUpdateCreateMoveRequest() {

    val window = g.navWindow
    val navGamepadActive = io.configFlags has ConfigFlag.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad
    val navKeyboardActive = io.configFlags has ConfigFlag.NavEnableKeyboard

    if (g.navMoveForwardToNextFrame) {
        // Forwarding previous request (which has been modified, e.g. wrap around menus rewrite the requests with a starting rectangle at the other side of the window)
        // (preserve most state, which were already set by the NavMoveRequestForward() function)
        assert(g.navMoveDir != Dir.None && g.navMoveClipDir != Dir.None)
        assert(g.navMoveFlags has NavMoveFlag.Forwarded)
        IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequestForward ${g.navMoveDir.i}")
    } else {
        // Initiate directional inputs request
        g.navMoveDir = Dir.None
        g.navMoveFlags = NavMoveFlag.None.i
        g.navMoveScrollFlags = ScrollFlag.None.i
        if (window != null && g.navWindowingTarget == null && window.flags hasnt Wf.NoNavInputs) {
            val repeatMode = InputFlag.Repeat or InputFlag.RepeatRateNavMove
            if (!isActiveIdUsingNavDir(Dir.Left) && ((navGamepadActive && Key.GamepadDpadLeft.isPressedEx(repeatMode)) || (navKeyboardActive && Key.LeftArrow.isPressedEx(repeatMode))))
                g.navMoveDir = Dir.Left
            if (!isActiveIdUsingNavDir(Dir.Right) && ((navGamepadActive && Key.GamepadDpadRight.isPressedEx(repeatMode)) || (navKeyboardActive && Key.RightArrow.isPressedEx(repeatMode))))
                g.navMoveDir = Dir.Right
            if (!isActiveIdUsingNavDir(Dir.Up) && ((navGamepadActive && Key.GamepadDpadUp.isPressedEx(repeatMode)) || (navKeyboardActive && Key.UpArrow.isPressedEx(repeatMode))))
                g.navMoveDir = Dir.Up
            if (!isActiveIdUsingNavDir(Dir.Down) && ((navGamepadActive && Key.GamepadDpadDown.isPressedEx(repeatMode)) || (navKeyboardActive && Key.DownArrow.isPressedEx(repeatMode))))
                g.navMoveDir = Dir.Down
        }
        g.navMoveDir = g.navMoveDir
        g.navScoringNoClipRect.put(+Float.MAX_VALUE, -Float.MIN_VALUE)
    }

    // Update PageUp/PageDown/Home/End scroll
    // FIXME-NAV: Consider enabling those keys even without the master ImGuiConfigFlags_NavEnableKeyboard flag?
    var scoringRectOffsetY = 0f
    if (window != null && g.navMoveDir == Dir.None && navKeyboardActive)
        scoringRectOffsetY = navUpdatePageUpPageDown()
    if (scoringRectOffsetY != 0f) {
        g.navScoringNoClipRect put window!!.innerRect
        g.navScoringNoClipRect translateY scoringRectOffsetY
    }

    // [DEBUG] Always send a request
    if (IMGUI_DEBUG_NAV_SCORING) {
        if (io.keyCtrl && Key.C.isPressed)
            g.navMoveDirForDebug = Dir.of((g.navMoveDirForDebug.i + 1) and 3)
        if (io.keyCtrl && g.navMoveDir == Dir.None) {
            g.navMoveDir = g.navMoveDirForDebug
            g.navMoveFlags /= NavMoveFlag.DebugNoResult
        }
    }

    // Submit
    g.navMoveForwardToNextFrame = false
    if (g.navMoveDir != Dir.None)
        navMoveRequestSubmit(g.navMoveDir, g.navMoveClipDir, g.navMoveFlags, g.navMoveScrollFlags)

    // Moving with no reference triggers an init request (will be used as a fallback if the direction fails to find a match)
    if (g.navMoveSubmitted && g.navId == 0) {
        IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: from move, window \"${window?.name ?: "<NULL>"}\", layer=${g.navLayer}")
        g.navInitRequest = true
        g.navInitRequestFromMove = true
        g.navInitResultId = 0
        g.navDisableHighlight = false
    }

    // When using gamepad, we project the reference nav bounding box into window visible area.
    // This is to allow resuming navigation inside the visible area after doing a large amount of scrolling, since with gamepad all movements are relative
    // (can't focus a visible object like we can with the mouse).
    if (g.navMoveSubmitted && g.navInputSource == InputSource.Gamepad && g.navLayer == NavLayer.Main && window != null) { // && (g.NavMoveFlags & ImGuiNavMoveFlags_Forwarded))
        val clampX = g.navMoveFlags hasnt (NavMoveFlag.LoopX or NavMoveFlag.WrapX)
        val clampY = g.navMoveFlags hasnt (NavMoveFlag.LoopY or NavMoveFlag.WrapY)
        val innerRectRel = window rectAbsToRel Rect(window.innerRect.min - 1, window.innerRect.max + 1)
        if ((clampX || clampY) && window.navRectRel[g.navLayer] !in innerRectRel) {
            //IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequest: clamp NavRectRel for gamepad move")
            val padX = innerRectRel.width min (window.calcFontSize() * 0.5f)
            val padY = innerRectRel.height min (window.calcFontSize() * 0.5f) // Terrible approximation for the intent of starting navigation from first fully visible ite
            innerRectRel.min.x = if (clampX) (innerRectRel.min.x + padX) else -Float.MAX_VALUE
            innerRectRel.max.x = if (clampX) (innerRectRel.max.x - padX) else +Float.MAX_VALUE
            innerRectRel.min.y = if (clampY) (innerRectRel.min.y + padY) else -Float.MAX_VALUE
            innerRectRel.max.y = if (clampY) (innerRectRel.max.y - padY) else +Float.MAX_VALUE
            window.navRectRel[g.navLayer] clipWithFull innerRectRel
            g.navId = 0
        }
    }

    // For scoring we use a single segment on the left side our current item bounding box (not touching the edge to avoid box overlap with zero-spaced items)
    val scoringRect = Rect()
    if (window != null) {
        val navRectRel = if (!window.navRectRel[g.navLayer].isInverted) window.navRectRel[g.navLayer] else Rect()
        scoringRect.put(window rectRelToAbs navRectRel)
        scoringRect translateY scoringRectOffsetY
        scoringRect.min.x = (scoringRect.min.x + 1f) min scoringRect.max.x
        scoringRect.max.x = scoringRect.min.x
        assert(!scoringRect.isInverted) { "Ensure if we have a finite, non-inverted bounding box here will allow us to remove extraneous ImFabs() calls in NavScoreItem()." }
        //GetForegroundDrawList()->AddRect(scoring_rect.Min, scoring_rect.Max, IM_COL32(255,200,0,255)); // [DEBUG]
        //if (!g.NavScoringNoClipRect.IsInverted()) { GetForegroundDrawList()->AddRect(g.NavScoringNoClipRect.Min, g.NavScoringNoClipRect.Max, IM_COL32(255, 200, 0, 255)); } // [DEBUG]
    }
    g.navScoringRect put scoringRect
    g.navScoringNoClipRect add scoringRect
}

fun navUpdateCreateTabbingRequest() {

    val window = g.navWindow
    assert(g.navMoveDir == Dir.None)
    if (window == null || g.navWindowingTarget != null || window.flags has Wf.NoNavInputs)
        return

    val tabPressed = Key.Tab isPressed true && !isActiveIdUsingKey(Key.Tab) && !g.io.keyCtrl && !g.io.keyAlt
    if (!tabPressed)
        return

    // Initiate tabbing request
    // (this is ALWAYS ENABLED, regardless of ImGuiConfigFlags_NavEnableKeyboard flag!)
    // Initially this was designed to use counters and modulo arithmetic, but that could not work with unsubmitted items (list clipper). Instead we use a strategy close to other move requests.
    // See NavProcessItemForTabbingRequest() for a description of the various forward/backward tabbing cases with and without wrapping.
    //// FIXME: We use (g.ActiveId == 0) but (g.NavDisableHighlight == false) might be righter once we can tab through anything
    g.navTabbingDir = if (g.io.keyShift) -1 else if (g.activeId == 0) 0 else +1
    val scrollFlags = if (window.appearing) ScrollFlag.KeepVisibleEdgeX or ScrollFlag.AlwaysCenterY else ScrollFlag.KeepVisibleEdgeX or ScrollFlag.KeepVisibleEdgeY
    val clipDir = if (g.navTabbingDir < 0) Dir.Up else Dir.Down
    navMoveRequestSubmit(Dir.None, clipDir, NavMoveFlag.Tabbing.i, scrollFlags) // FIXME-NAV: Once we refactor tabbing, add LegacyApi flag to not activate non-inputable.
    g.navTabbingCounter = -1
}

/** Handle PageUp/PageDown/Home/End keys
 *  Called from NavUpdateCreateMoveRequest() which will use our output to create a move request
 *  FIXME-NAV: This doesn't work properly with NavFlattened siblings as we use NavWindow rectangle for reference
 *  FIXME-NAV: how to get Home/End to aim at the beginning/end of a 2D grid? */
fun navUpdatePageUpPageDown(): Float {

    val window = g.navWindow!!
    if (window.flags has Wf.NoNavInputs || g.navWindowingTarget != null || g.navLayer != NavLayer.Main)
        return 0f

    val pageUpHeld = Key.PageUp.isDown && !isActiveIdUsingKey(Key.PageUp)
    val pageDownHeld = Key.PageDown.isDown && !isActiveIdUsingKey(Key.PageDown)
    val homePressed = Key.Home.isPressed && !isActiveIdUsingKey(Key.Home)
    val endPressed = Key.End.isPressed && !isActiveIdUsingKey(Key.End)
    if (pageUpHeld == pageDownHeld && homePressed == endPressed) // Proceed if either (not both) are pressed, otherwise early out
        return 0f

    if (g.navLayer != NavLayer.Main)
        navRestoreLayer(NavLayer.Main)

    if (window.dc.navLayersActiveMask == 0x00 && window.dc.navHasScroll) {
        // Fallback manual-scroll when window has no navigable item
        when {
            Key.PageUp isPressed true -> window.setScrollY(window.scroll.y - window.innerRect.height)
            Key.PageDown isPressed true -> window.setScrollY(window.scroll.y + window.innerRect.height)
            homePressed -> window setScrollY 0f
            endPressed -> window setScrollY window.scrollMax.y
        }
    } else {
        val navRectRel = window.navRectRel[g.navLayer]
        val pageOffsetY = 0f max (window.innerRect.height - window.calcFontSize() * 1f + navRectRel.height)
        var navScoringRectOffsetY = 0f
        if (Key.PageUp isPressed true) {
            navScoringRectOffsetY = -pageOffsetY
            g.navMoveDir =
                Dir.Down // Because our scoring rect is offset up, we request the down direction (so we can always land on the last item)
            g.navMoveClipDir = Dir.Up
            g.navMoveFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.AlsoScoreVisibleSet
        } else if (Key.PageDown isPressed true) {
            navScoringRectOffsetY = +pageOffsetY
            g.navMoveDir =
                Dir.Up // Because our scoring rect is offset down, we request the up direction (so we can always land on the last item)
            g.navMoveClipDir = Dir.Down
            g.navMoveFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.AlsoScoreVisibleSet
        } else if (homePressed) {
            // FIXME-NAV: handling of Home/End is assuming that the top/bottom most item will be visible with Scroll.y == 0/ScrollMax.y
            // Scrolling will be handled via the ImGuiNavMoveFlags_ScrollToEdgeY flag, we don't scroll immediately to avoid scrolling happening before nav result.
            // Preserve current horizontal position if we have any.
            navRectRel.min.y = 0f; navRectRel.max.y = 0f
            if (navRectRel.isInverted) {
                navRectRel.min.x = 0f; navRectRel.max.x = 0f
            }
            g.navMoveDir = Dir.Down
            g.navMoveFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.ScrollToEdgeY
            // FIXME-NAV: MoveClipDir left to _None, intentional?
        } else if (endPressed) {
            navRectRel.min.y = window.contentSize.y; navRectRel.max.y = window.contentSize.y
            if (navRectRel.isInverted) {
                navRectRel.min.x = 0f
                navRectRel.max.x = 0f
            }
            g.navMoveDir = Dir.Up
            g.navMoveFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.ScrollToEdgeY
            // FIXME-NAV: MoveClipDir left to _None, intentional?
        }
        return navScoringRectOffsetY
    }
    return 0f
}

fun navUpdateAnyRequestFlag() {
    g.navAnyRequest = g.navMoveScoringItems || g.navInitRequest || (IMGUI_DEBUG_NAV_SCORING && g.navWindow != null)
    if (g.navAnyRequest)
        assert(g.navWindow != null)
}

fun navUpdateCreateWrappingRequest() {

    val window = g.navWindow!!

    var doForward = false
    val bbRel = Rect(window.navRectRel[g.navLayer])
    var clipDir = g.navMoveDir
    val moveFlags = g.navMoveFlags
    if (g.navMoveDir == Dir.Left && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
        bbRel.min.x = window.contentSize.x + window.windowPadding.x; bbRel.max.x = bbRel.min.x

        if (moveFlags has NavMoveFlag.WrapX) {
            bbRel translateY -bbRel.height // Previous row
            clipDir = Dir.Up
        }
        doForward = true
    }
    if (g.navMoveDir == Dir.Right && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
        bbRel.max.x = -window.windowPadding.x
        bbRel.min.x = bbRel.max.x
        if (moveFlags has NavMoveFlag.WrapX) {
            bbRel translateY +bbRel.height // Next row
            clipDir = Dir.Down
        }
        doForward = true
    }
    if (g.navMoveDir == Dir.Up && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
        bbRel.min.y = window.contentSize.y + window.windowPadding.y; bbRel.max.y = bbRel.min.y
        if (moveFlags has NavMoveFlag.WrapY) {
            bbRel translateX -bbRel.width // Previous column
            clipDir = Dir.Left
        }
        doForward = true
    }
    if (g.navMoveDir == Dir.Down && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
        bbRel.min.y = -window.windowPadding.y; bbRel.max.y = bbRel.min.y
        if (moveFlags has NavMoveFlag.WrapY) {
            bbRel translateX +bbRel.width // Next column
            clipDir = Dir.Right
        }
        doForward = true
    }
    if (!doForward)
        return
    window.navRectRel[g.navLayer] = bbRel
    navMoveRequestForward(g.navMoveDir, clipDir, moveFlags, g.navMoveScrollFlags)
}

fun navEndFrame() {

    // Show CTRL+TAB list window
    if (g.navWindowingTarget != null)
        navUpdateWindowingOverlay()

    // Perform wrap-around in menus
    // FIXME-NAV: Wrap may need to apply a weight bias on the other axis. e.g. 4x4 grid with 2 last items missing on last item won't handle LoopY/WrapY correctly.
    // FIXME-NAV: Wrap (not Loop) support could be handled by the scoring function and then WrapX would function without an extra frame.
    val wantedFlags = NavMoveFlag.WrapX or NavMoveFlag.LoopX or NavMoveFlag.WrapY or NavMoveFlag.LoopY
    if (g.navWindow != null && navMoveRequestButNoResultYet() && g.navMoveFlags has wantedFlags && g.navMoveFlags hasnt NavMoveFlag.Forwarded)
        navUpdateCreateWrappingRequest()
}


/** Scoring function for gamepad/keyboard directional navigation. Based on https://gist.github.com/rygorous/6981057  */
fun navScoreItem(result: NavItemData): Boolean {

    val window = g.currentWindow!!
    if (g.navLayer != window.dc.navLayerCurrent) return false

    // Current modified source rect (NB: we've applied max.x = min.x in navUpdate() to inhibit the effect of having varied item width)
    val curr = Rect(g.navScoringRect)
    // FIXME: Those are not good variables names
    val cand = g.lastItemData.navRect   // Current item nav rectangle
    g.navScoringDebugCount++

    // When entering through a NavFlattened border, we consider child window items as fully clipped for scoring
    if (window.parentWindow === g.navWindow) {
        assert((window.flags or g.navWindow!!.flags) has Wf._NavFlattened)
        if (!cand.overlaps(window.clipRect))
            return false
        cand clipWithFull window.clipRect // This allows the scored item to not overlap other candidates in the parent window
    }

    /*  We perform scoring on items bounding box clipped by the current clipping rectangle on the other axis
        (clipping on our movement axis would give us equal scores for all clipped items)
        For example, this ensures that items in one column are not reached when moving vertically from items in another column. */
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
        quadrant = if (g.lastItemData.id < g.navId) Dir.Left else Dir.Right

    if (IMGUI_DEBUG_NAV_SCORING)
        if (isMouseHoveringRect(cand)) {
            val buf =
                "dbox (%.2f,%.2f->%.4f)\ndcen (%.2f,%.2f->%.4f)\nd (%.2f,%.2f->%.4f)\nnav WENS${g.navMoveDir}, quadrant WENS$quadrant"
                        .format(style.locale, dbX, dbY, distBox, dcX, dcY, distCenter, dax, day, distAxial).toByteArray()
            getForegroundDrawList(window).apply {
                addRect(curr.min, curr.max, COL32(255, 200, 0, 100))
                addRect(cand.min, cand.max, COL32(255, 255, 0, 200))
                addRectFilled(cand.max - Vec2(4), cand.max + calcTextSize(buf, 0) + Vec2(4), COL32(40, 0, 0, 150))
                addText(cand.max, 0.inv(), buf.cStr)
            }
        } else if (io.keyCtrl) { // Hold to preview score in matching quadrant. Press C to rotate.
            if (quadrant == g.navMoveDir) {
                val buf = "%.0f/%.0f".format(style.locale, distBox, distCenter).toByteArray()
                getForegroundDrawList(window).apply {
                    addRectFilled(cand.min, cand.max, COL32(255, 0, 0, 200))
                    addText(cand.min, COL32(255), buf.cStr)
                }
            }
        }

    // Is it in the quadrant we're interested in moving to?
    var newBest = false
    val moveDir = g.navMoveDir
    if (quadrant == moveDir) {
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
                // Still tied! we need to be extra-careful to make sure everything gets linked properly. We consistently break ties by symbolically moving "later" items
                // (with higher index) to the right/downwards by an infinitesimal amount since we the current "best" button already (so it must have a lower index),
                // this is fairly easy. This rule ensures that all buttons with dx==dy==0 will end up being linked in order of appearance along the x axis.
                val db = if (moveDir == Dir.Up || moveDir == Dir.Down) dbY else dbX
                if (db < 0f) // moving bj to the right/down decreases distance
                    newBest = true
            }
        }
    }

    // Axial check: if 'curr' has no link at all in some direction and 'cand' lies roughly in that direction, add a tentative link. This will only be kept if no "real" matches
    // are found, so it only augments the graph produced by the above method using extra links. (important, since it doesn't guarantee strong connectedness)
    // This is just to avoid buttons having no links in a particular direction when there's a suitable neighbor. you get good graphs without this too.
    // 2017/09/29: FIXME: This now currently only enabled inside menu bars, ideally we'd disable it everywhere. Menus in particular need to catch failure. For general navigation it feels awkward.
    // Disabling it may lead to disconnected graphs when nodes are very spaced out on different axis. Perhaps consider offering this as an option?
    if (result.distBox == Float.MAX_VALUE && distAxial < result.distAxial)  // Check axial match
        if (g.navLayer == NavLayer.Menu && g.navWindow!!.flags hasnt Wf._ChildMenu)
            if ((moveDir == Dir.Left && dax < 0f) || (moveDir == Dir.Right && dax > 0f) || (moveDir == Dir.Up && day < 0f) || (moveDir == Dir.Down && day > 0f)) {
                result.distAxial = distAxial
                newBest = true
            }

    return newBest
}

fun navApplyItemToResult(result: NavItemData) {
    val window = g.currentWindow!!
    result.window = window
    result.id = g.lastItemData.id
    result.focusScopeId = window.dc.navFocusScopeIdCurrent
    result.inFlags = g.lastItemData.inFlags
    result.rectRel put (window rectAbsToRel g.lastItemData.navRect)
}

/** We get there when either navId == id, or when g.navAnyRequest is set (which is updated by navUpdateAnyRequestFlag above)
 *  // This is called after LastItemData is set. */
fun navProcessItem() {

    val window = g.currentWindow!!
    val id = g.lastItemData.id
    val navBb = g.lastItemData.navRect
    val itemFlags = g.lastItemData.inFlags

    // Process Init Request
    if (g.navInitRequest && g.navLayer == window.dc.navLayerCurrent && itemFlags hasnt If.Disabled) {
        // Even if 'ImGuiItemFlags_NoNavDefaultFocus' is on (typically collapse/close button) we record the first ResultId so they can be used as a fallback
        val candidateForNavDefaultFocus = itemFlags hasnt If.NoNavDefaultFocus
        if (candidateForNavDefaultFocus || g.navInitResultId == 0) {
            g.navInitResultId = id
            g.navInitResultRectRel = window rectAbsToRel navBb
        }
        if (candidateForNavDefaultFocus) {
            g.navInitRequest = false // Found a match, clear request
            navUpdateAnyRequestFlag()
        }
    }

    // Process Move Request (scoring for navigation)
    // FIXME-NAV: Consider policy for double scoring (scoring from NavScoringRect + scoring from a rect wrapped according to current wrapping policy)     */
    if (g.navMoveScoringItems) {

        val isTabStop = itemFlags has If.Inputable && itemFlags hasnt (If.NoTabStop or If.Disabled)
        val isTabbing = g.navMoveFlags hasnt NavMoveFlag.Tabbing
        if (isTabbing) {
            if (isTabStop || g.navMoveFlags has NavMoveFlag.FocusApi)
                navProcessItemForTabbingRequest(id)
        } else if ((g.navId != id || g.navMoveFlags has NavMoveFlag.AllowCurrentNavId) && itemFlags hasnt (If.Disabled or If.NoNav)) {
            val result = if (window === g.navWindow) g.navMoveResultLocal else g.navMoveResultOther

            if (!isTabbing) {
                if (navScoreItem(result))
                    navApplyItemToResult(result)

                // Features like PageUp/PageDown need to maintain a separate score for the visible set of items.
                val VISIBLE_RATIO = 0.7f
                if (g.navMoveFlags has NavMoveFlag.AlsoScoreVisibleSet && window.clipRect overlaps navBb)
                    if (clamp(navBb.max.y, window.clipRect.min.y, window.clipRect.max.y) - clamp(navBb.min.y, window.clipRect.min.y, window.clipRect.max.y) >= (navBb.max.y - navBb.min.y) * VISIBLE_RATIO)
                        if (navScoreItem(g.navMoveResultLocalVisible))
                            navApplyItemToResult(g.navMoveResultLocalVisible)
            }
        }
    }

    // Update window-relative bounding box of navigated item
    if (g.navId == id) {
        if (g.navWindow !== window)
            setNavWindow(window) // Always refresh g.NavWindow, because some operations such as FocusItem() may not have a window.
        g.navLayer = window.dc.navLayerCurrent
        g.navFocusScopeId = window.dc.navFocusScopeIdCurrent
        g.navIdIsAlive = true
        window.navRectRel[window.dc.navLayerCurrent] = window rectAbsToRel navBb    // Store item bounding box (relative to window position)
    }
}

// Handle "scoring" of an item for a tabbing/focusing request initiated by NavUpdateCreateTabbingRequest().
// Note that SetKeyboardFocusHere() API calls are considered tabbing requests!
// - Case 1: no nav/active id:    set result to first eligible item, stop storing.
// - Case 2: tab forward:         on ref id set counter, on counter elapse store result
// - Case 3: tab forward wrap:    set result to first eligible item (preemptively), on ref id set counter, on next frame if counter hasn't elapsed store result. // FIXME-TABBING: Could be done as a next-frame forwarded request
// - Case 4: tab backward:        store all results, on ref id pick prev, stop storing
// - Case 5: tab backward wrap:   store all results, on ref id if no result keep storing until last // FIXME-TABBING: Could be done as next-frame forwarded requested
fun navProcessItemForTabbingRequest(id: ID) {
    // Always store in NavMoveResultLocal (unlike directional request which uses NavMoveResultOther on sibling/flattened windows)
    val result = g.navMoveResultLocal
    if (g.navTabbingDir == +1) {
        // Tab Forward or SetKeyboardFocusHere() with >= 0
        if (g.navTabbingResultFirst.id == 0)
            navApplyItemToResult(g.navTabbingResultFirst)
        if (--g.navTabbingCounter == 0)
            navMoveRequestResolveWithLastItem(result)
        else if (g.navId == id)
            g.navTabbingCounter = 1
    } else if (g.navTabbingDir == -1) {
        // Tab Backward
        if (g.navId == id) {
            if (result.id != 0) {
                g.navMoveScoringItems = false
                navUpdateAnyRequestFlag()
            }
        } else
            navApplyItemToResult(result)
    } else if (g.navTabbingDir == 0)
    // Tab Init
        if (g.navTabbingResultFirst.id == 0)
            navMoveRequestResolveWithLastItem(g.navTabbingResultFirst)
}

fun navCalcPreferredRefPos(): Vec2 {
    val window = g.navWindow
    return if (g.navDisableHighlight || !g.navDisableMouseHover || window == null) {
        // Mouse (we need a fallback in case the mouse becomes invalid after being used)
        // The +1.0f offset when stored by OpenPopupEx() allows reopening this or another popup (same or another mouse button) while not moving the mouse, it is pretty standard.
        // In theory we could move that +1.0f offset in OpenPopupEx()
        val p = if (isMousePosValid(io.mousePos)) io.mousePos else g.mouseLastValidPos
        Vec2(p.x + 1f, p.y)
    } else {
        // When navigation is active and mouse is disabled, pick a position around the bottom left of the currently navigated item
        // Take account of upcoming scrolling (maybe set mouse pos should be done in EndFrame?)
        val rectRel = window rectRelToAbs window.navRectRel[g.navLayer]
        if (window.lastFrameActive != g.frameCount && (window.scrollTarget.x != Float.MAX_VALUE || window.scrollTarget.y != Float.MAX_VALUE)) {
            val nextScroll = window.calcNextScrollFromScrollTargetAndClamp()
            rectRel translate (window.scroll - nextScroll)
        }
        val pos = Vec2(rectRel.min.x + min(style.framePadding.x * 4, rectRel.width),
                       rectRel.max.y - min(style.framePadding.y, rectRel.height))
        val viewport = mainViewport
        floor(glm.clamp(pos, viewport.pos, viewport.pos + viewport.size)) // ImFloor() is important because non-integer mouse position application in backend might be lossy and result in undesirable non-zero delta.
    }
}

/** FIXME: This could be replaced by updating a frame number in each window when (window == NavWindow) and (NavLayer == 0).
 *  This way we could find the last focused window among our children. It would be much less confusing this way? */
fun navSaveLastChildNavWindowIntoParent(navWindow: Window?) {
    var parent = navWindow
    while (parent != null && parent.rootWindow !== parent && parent.flags hasnt (Wf._Popup or Wf._ChildMenu))
        parent = parent.parentWindow
    if (parent != null && parent !== navWindow)
        parent.navLastChildNavWindow = navWindow
}

fun navRestoreLayer(layer: NavLayer) {
    if (layer == NavLayer.Main) {
        val prevNavWindow = g.navWindow
        g.navWindow = navRestoreLastChildNavWindow(g.navWindow!!) // FIXME-NAV: Should clear ongoing nav requests?
        if (prevNavWindow != null)
            IMGUI_DEBUG_LOG_FOCUS("[focus] NavRestoreLayer: from \"${prevNavWindow.name}\" to SetNavWindow(\"${g.navWindow!!.name}\")")
    }
    val window = g.navWindow!!
    if (window.navLastIds[layer] != 0)
        setNavID(window.navLastIds[layer], layer, 0, window.navRectRel[layer])
    else {
        g.navLayer = layer
        navInitWindow(window, true)
    }
    g.navDisableHighlight = false
    g.navDisableMouseHover = true; g.navMousePosDirty = true
}

fun navRestoreHighlightAfterMove() {
    g.navDisableHighlight = false
    g.navDisableMouseHover = true; g.navMousePosDirty = true
}

/** Restore the last focused child.
 *  Call when we are expected to land on the Main Layer (0) after FocusWindow()    */
fun navRestoreLastChildNavWindow(window: Window) = window.navLastChildNavWindow?.takeIf { it.wasActive } ?: window

fun findWindowFocusIndex(window: Window): Int {
    val order = window.focusOrder
    assert(window.rootWindow === window) { "No child window (not testing _ChildWindow because of docking)" }
    assert(g.windowsFocusOrder[order] == window)
    return order
}

// static spare functions

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
    else -> { // FIXME: PageUp/PageDown are leaving move_dir == None
        r.min.x = glm.clamp(r.min.x, clipRect.min.x, clipRect.max.x)
        r.max.x = glm.clamp(r.max.x, clipRect.min.x, clipRect.max.x)
    }
}