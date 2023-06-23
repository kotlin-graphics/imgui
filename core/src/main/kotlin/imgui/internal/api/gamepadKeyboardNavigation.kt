package imgui.internal.api

import glm_.i
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.rectRelToAbs
import imgui.ImGui.scrollToRectEx
import imgui.ImGui.setScrollY
import imgui.api.g
import imgui.api.gImGui
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import imgui.statics.navApplyItemToResult
import imgui.statics.navRestoreHighlightAfterMove
import imgui.statics.navUpdateAnyRequestFlag

// Gamepad/Keyboard Navigation
internal interface gamepadKeyboardNavigation {

    fun navInitWindow(window: Window, forceReinit: Boolean) {

        assert(window == g.navWindow)

        if (window.flags has WindowFlag.NoNavInputs) {
            g.navId = 0
            g.navFocusScopeId = window.navRootFocusScopeId
            return
        }

        var initForNav = false
        if (window === window.rootWindow || window.flags has WindowFlag._Popup || window.navLastIds[0] == 0 || forceReinit)
            initForNav = true
        IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: from NavInitWindow(), init_for_nav=${initForNav.i}, window=\"${window.name}\", layer=${g.navLayer.ordinal}")
        if (initForNav) {
            setNavID(0, g.navLayer, window.navRootFocusScopeId, Rect())
            g.navInitRequest = true
            g.navInitRequestFromMove = false
            g.navInitResultId = 0
            g.navInitResultRectRel = Rect()
            navUpdateAnyRequestFlag()
        } else {
            g.navId = window.navLastIds[0]
            g.navFocusScopeId = window.navRootFocusScopeId
        }
    }

    fun navInitRequestApplyResult() {
        // In very rare cases g.NavWindow may be null (e.g. clearing focus after requesting an init request, which does happen when releasing Alt while clicking on void)
        val navWindow = g.navWindow ?: return

        // Apply result from previous navigation init request (will typically select the first item, unless SetItemDefaultFocus() has been called)
        // FIXME-NAV: On _NavFlattened windows, g.NavWindow will only be updated during subsequent frame. Not a problem currently.
        IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: ApplyResult: NavID 0x%08X in Layer ${g.navLayer.ordinal} Window \"${navWindow.name}\"", g.navInitResultId)
        setNavID(g.navInitResultId, g.navLayer, 0, g.navInitResultRectRel)
        g.navIdIsAlive = true // Mark as alive from previous frame as we got a result
        if (g.navInitRequestFromMove)
            navRestoreHighlightAfterMove()
    }

    fun navMoveRequestButNoResultYet(): Boolean = g.navMoveScoringItems && g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0

    /** FIXME: ScoringRect is not set */
    fun navMoveRequestSubmit(moveDir: Dir, clipDir: Dir, moveFlags_: NavMoveFlags, scrollFlags: ScrollFlags) {

        var moveFlags = moveFlags_
        assert(g.navWindow != null)

        if (moveFlags has NavMoveFlag.Tabbing)
            moveFlags /= NavMoveFlag.AllowCurrentNavId

        g.navMoveSubmitted = true; g.navMoveScoringItems = true
        g.navMoveDir = moveDir
        g.navMoveDirForDebug = moveDir
        g.navMoveClipDir = clipDir
        g.navMoveFlags = moveFlags
        g.navMoveScrollFlags = scrollFlags
        g.navMoveForwardToNextFrame = false
        g.navMoveKeyMods = g.io.keyMods
        g.navMoveResultLocal.clear()
        g.navMoveResultLocalVisible.clear()
        g.navMoveResultOther.clear()
        g.navTabbingCounter = 0
        g.navTabbingResultFirst.clear()
        navUpdateAnyRequestFlag()
    }

    /** Forward will reuse the move request again on the next frame (generally with modifications done to it) */
    fun navMoveRequestForward(moveDir: Dir, clipDir: Dir, moveFlags: NavMoveFlags, scrollFlags: ScrollFlags) {

        assert(!g.navMoveForwardToNextFrame)
        navMoveRequestCancel()
        g.navMoveForwardToNextFrame = true
        g.navMoveDir = moveDir
        g.navMoveClipDir = clipDir
        g.navMoveFlags = moveFlags or NavMoveFlag.Forwarded
        g.navMoveScrollFlags = scrollFlags
    }

    fun navMoveRequestResolveWithLastItem(result: NavItemData) {
        g.navMoveScoringItems = false // Ensure request doesn't need more processing
        navApplyItemToResult(result)
        navUpdateAnyRequestFlag()
    }

    fun navMoveRequestCancel() {
        g.navMoveSubmitted = false; g.navMoveScoringItems = false
        navUpdateAnyRequestFlag()
    }

    /** Apply result from previous frame navigation directional move request. Always called from NavUpdate() */
    fun navMoveRequestApplyResult() {

        if (IMGUI_DEBUG_NAV_SCORING)
            if (g.navMoveFlags has NavMoveFlag.DebugNoResult) // [DEBUG] Scoring all items in NavWindow at all times
                return

        // Select which result to use
        var result = if (g.navMoveResultLocal.id != 0) g.navMoveResultLocal else if (g.navMoveResultOther.id != 0) g.navMoveResultOther else null

        // Tabbing forward wrap
        if (g.navMoveFlags has NavMoveFlag.Tabbing && result == null)
            if ((g.navTabbingCounter == 1 || g.navTabbingDir == 0) && g.navTabbingResultFirst.id != 0)
                result = g.navTabbingResultFirst

        // In a situation when there are no results but NavId != 0, re-enable the Navigation highlight (because g.NavId is not considered as a possible result)
        if (result == null) {
            if (g.navMoveFlags has NavMoveFlag.Tabbing)
                g.navMoveFlags /= NavMoveFlag.DontSetNavHighlight
            if (g.navId != 0 && g.navMoveFlags hasnt NavMoveFlag.DontSetNavHighlight)
                navRestoreHighlightAfterMove()
            IMGUI_DEBUG_LOG_NAV("[nav] NavMoveSubmitted but not led to a result!\n")
            return
        }

        // PageUp/PageDown behavior first jumps to the bottom/top mostly visible item, _otherwise_ use the result from the previous/next page.
        if (g.navMoveFlags has NavMoveFlag.AlsoScoreVisibleSet)
            if (g.navMoveResultLocalVisible.id != 0 && g.navMoveResultLocalVisible.id != g.navId)
                result = g.navMoveResultLocalVisible

        // Maybe entering a flattened child from the outside? In this case solve the tie using the regular scoring rules.
        if (result != g.navMoveResultOther && g.navMoveResultOther.id != 0 && g.navMoveResultOther.window!!.parentWindow === g.navWindow)
            if (g.navMoveResultOther.distBox < result.distBox || (g.navMoveResultOther.distBox == result.distBox && g.navMoveResultOther.distCenter < result.distCenter))
                result = g.navMoveResultOther
        val window = result.window!!
        assert(g.navWindow != null)

        // Scroll to keep newly navigated item fully into view.
        if (g.navLayer == NavLayer.Main) {
            val rectAbs = result.window!! rectRelToAbs result.rectRel
            scrollToRectEx(result.window!!, rectAbs, g.navMoveScrollFlags)

            if (g.navMoveFlags has NavMoveFlag.ScrollToEdgeY) {
                // FIXME: Should remove this? Or make more precise: use ScrollToRectEx() with edge?
                val scrollTarget = if (g.navMoveDir == Dir.Up) window.scrollMax.y else 0f
                window setScrollY scrollTarget
            }
        }

        if (g.navWindow !== result.window) {
            IMGUI_DEBUG_LOG_FOCUS("[focus] NavMoveRequest: SetNavWindow(\"${result.window!!.name}\")")
            g.navWindow = result.window
        }
        if (g.activeId != result.id)
            clearActiveID()
        if (g.navId != result.id) {
            // Don't set NavJustMovedToId if just landed on the same spot (which may happen with ImGuiNavMoveFlags_AllowCurrentNavId)
            g.navJustMovedToId = result.id
            g.navJustMovedToFocusScopeId = result.focusScopeId

            g.navJustMovedToKeyMods = g.navMoveKeyMods
        }

        // Focus
        IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequest: result NavID 0x%08X in Layer ${g.navLayer} Window \"${window.name}\"", result.id) // [JVM] window *is* g.navWindow!!
        setNavID(result.id, g.navLayer, result.focusScopeId, result.rectRel)

        // Tabbing: Activates Inputable or Focus non-Inputable
        if (g.navMoveFlags has NavMoveFlag.Tabbing && result.inFlags has ItemFlag.Inputable) {
            g.navNextActivateId = result.id
            g.navNextActivateFlags = ActivateFlag.PreferInput or ActivateFlag.TryToPreserveState
            g.navMoveFlags /= NavMoveFlag.DontSetNavHighlight
        }

        // Activate
        if (g.navMoveFlags has NavMoveFlag.Activate) {
            g.navNextActivateId = result.id
            g.navNextActivateFlags = none
        }

        // Enable nav highlight
        if (g.navMoveFlags hasnt NavMoveFlag.DontSetNavHighlight)
            navRestoreHighlightAfterMove()
    }


    /** Navigation wrap-around logic is delayed to the end of the frame because this operation is only valid after entire
     *  popup is assembled and in case of appended popups it is not clear which EndPopup() call is final. */
    fun navMoveRequestTryWrapping(window: Window, wrapFlags: NavMoveFlags) {
        assert(wrapFlags.isNotEmpty) { "Call with _WrapX, _WrapY, _LoopX, _LoopY" }
        // In theory we should test for NavMoveRequestButNoResultYet() but there's no point doing it, NavEndFrame() will do the same test
        if (g.navWindow === window && g.navMoveScoringItems && g.navLayer == NavLayer.Main)
            g.navMoveFlags = g.navMoveFlags or wrapFlags
    }

    // True when current work location may be scrolled horizontally when moving left / right.
    // This is generally always true UNLESS within a column. We don't have a vertical equivalent.
    fun navUpdateCurrentWindowIsScrollPushableX() {
        val g = gImGui
        val window = g.currentWindow!!
        window.dc.navIsScrollPushableX = g.currentTable == null && window.dc.currentColumns == null
    }

    /** Remotely activate a button, checkbox, tree node etc. given its unique ID. activation is queued and processed
     *  on the next frame when the item is encountered again.  */
    fun activateItem(id: ID) {
        g.navNextActivateId = id
        g.navNextActivateFlags = none
    }

    // FIXME-NAV: The existence of SetNavID vs SetFocusID vs FocusWindow() needs to be clarified/reworked.
    // In our terminology those should be interchangeable, yet right now this is super confusing.
    // Those two functions are merely a legacy artifact, so at minimum naming should be clarified.

    fun setNavWindow(window: Window?) {
        if (g.navWindow !== window) {
            IMGUI_DEBUG_LOG_FOCUS("[focus] SetNavWindow(\"${window?.name ?: "<NULL>"}\")")
            g.navWindow = window
        }
        g.navInitRequest = false; g.navMoveSubmitted = false; g.navMoveScoringItems = false
        navUpdateAnyRequestFlag()
    }

    fun setNavID(id: ID, navLayer: NavLayer, focusScopeId: ID, rectRel: Rect) { // assert(navLayer == 0 || navLayer == 1) useless on jvm
        val navWindow = g.navWindow!!
        assert(navLayer == NavLayer.Main || navLayer == NavLayer.Menu)
        g.navId = id
        g.navLayer = navLayer
        g.navFocusScopeId = focusScopeId
        navWindow.navLastIds[navLayer] = id
        navWindow.navRectRel[navLayer] = rectRel
    }
}