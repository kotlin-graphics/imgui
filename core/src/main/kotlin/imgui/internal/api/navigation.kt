package imgui.internal.api

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.io
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import imgui.static.navUpdateAnyRequestFlag

// Gamepad/Keyboard Navigation
internal interface navigation {

    fun navInitWindow(window: Window, forceReinit: Boolean) {

        assert(window == g.navWindow)

        if (window.flags has WindowFlag.NoNavInputs) {
            g.navId = 0; g.navFocusScopeId = 0
            return
        }

        var initForNav = false
        if (window === window.rootWindow || window.flags has WindowFlag._Popup || window.navLastIds[0] == 0 || forceReinit)
            initForNav = true
        IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: from NavInitWindow(), init_for_nav=$initForNav, window=\"${window.name}\", layer=${g.navLayer}")
        if (initForNav) {
            setNavID(0, g.navLayer, 0, Rect())
            g.navInitRequest = true
            g.navInitRequestFromMove = false
            g.navInitResultId = 0
            g.navInitResultRectRel = Rect()
            navUpdateAnyRequestFlag()
        } else {
            g.navId = window.navLastIds[0]
            g.navFocusScopeId = 0
        }
    }

    fun navMoveRequestButNoResultYet(): Boolean = g.navMoveScoringItems && g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0

    /** FIXME: ScoringRect is not set */
    fun navMoveRequestSubmit(moveDir: Dir, clipDir: Dir, moveFlags: NavMoveFlags) {
        assert(g.navWindow != null)
        g.navMoveSubmitted = true; g.navMoveScoringItems = true
        g.navMoveDir = moveDir
        g.navMoveClipDir = clipDir
        g.navMoveFlags = moveFlags
        g.navMoveForwardToNextFrame = false
        g.navMoveKeyMods = g.io.keyMods
        g.navMoveResultLocal.clear()
        g.navMoveResultLocalVisible.clear()
        g.navMoveResultOther.clear()
    }

    /** Forward will reuse the move request again on the next frame (generally with modifications done to it) */
    fun navMoveRequestForward(moveDir: Dir, clipDir: Dir, moveFlags: NavMoveFlags) {

        assert(!g.navMoveForwardToNextFrame)
        navMoveRequestCancel()
        g.navMoveForwardToNextFrame = true
        g.navMoveDir = moveDir
        g.navMoveDir = clipDir
        g.navMoveFlags = moveFlags or NavMoveFlag.Forwarded
    }

    fun navMoveRequestCancel() {
        g.navMoveSubmitted = false; g.navMoveScoringItems = false
        navUpdateAnyRequestFlag()
    }

    /** Apply result from previous frame navigation directional move request. Always called from NavUpdate() */
    fun navMoveRequestApplyResult() {

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
            val deltaScroll = Vec2()
            if (g.navMoveFlags has NavMoveFlag.ScrollToEdge) {
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

            g.navJustMovedToKeyMods = g.navMoveKeyMods
        }
        IMGUI_DEBUG_LOG_NAV("[nav] NavMoveRequest: result NavID 0x%08X in Layer ${g.navLayer} Window \"${window.name}\"", result.id) // [JVM] window *is* g.navWindow!!
        setNavID(result.id, g.navLayer, result.focusScopeId, result.rectRel)
        g.navDisableHighlight = false
        g.navDisableMouseHover = true; g.navMousePosDirty = true
    }


    /** Navigation wrap-around logic is delayed to the end of the frame because this operation is only valid after entire
     *  popup is assembled and in case of appended popups it is not clear which EndPopup() call is final. */
    fun navMoveRequestTryWrapping(window: Window, wrapFlags: NavMoveFlags) {
        assert(wrapFlags != 0) { "Call with _WrapX, _WrapY, _LoopX, _LoopY" }
        // In theory we should test for NavMoveRequestButNoResultYet() but there's no point doing it, NavEndFrame() will do the same test
        if (g.navWindow === window && g.navMoveScoringItems && g.navLayer == NavLayer.Main)
            g.navMoveFlags = g.navMoveFlags or wrapFlags
    }

    fun getNavInputAmount(n: NavInput, mode: InputReadMode): Float {    // TODO -> NavInput?

        val i = n.i
        if (mode == InputReadMode.Down) return io.navInputs[i] // Instant, read analog input (0.0f..1.0f, as provided by user)

        val t = io.navInputsDownDuration[i]
        return when { // Return 1.0f when just released, no repeat, ignore analog input.
            t < 0f && mode == InputReadMode.Released -> if (io.navInputsDownDurationPrev[i] >= 0f) 1f else 0f
            t < 0f -> 0f
            else -> when (mode) { // Return 1.0f when just pressed, no repeat, ignore analog input.
                InputReadMode.Pressed -> if (t == 0f) 1 else 0
                InputReadMode.Repeat -> calcTypematicRepeatAmount(t - io.deltaTime,
                                                                  t,
                                                                  io.keyRepeatDelay * 0.72f,
                                                                  io.keyRepeatRate * 0.8f)
                InputReadMode.RepeatSlow -> calcTypematicRepeatAmount(t - io.deltaTime,
                                                                      t,
                                                                      io.keyRepeatDelay * 1.25f,
                                                                      io.keyRepeatRate * 2f)
                InputReadMode.RepeatFast -> calcTypematicRepeatAmount(t - io.deltaTime,
                                                                      t,
                                                                      io.keyRepeatDelay * 0.72f,
                                                                      io.keyRepeatRate * 0.3f)
                else -> 0
            }.f
        }
    }

    /** @param dirSources: NavDirSourceFlag    */
    fun getNavInputAmount2d(dirSources: NavDirSourceFlags,
                            mode: InputReadMode,
                            slowFactor: Float = 0f,
                            fastFactor: Float = 0f): Vec2 {
        val delta = Vec2()
        if (dirSources has NavDirSourceFlag.Keyboard) delta += Vec2(getNavInputAmount(NavInput._KeyRight,
                                                                                      mode) - getNavInputAmount(NavInput._KeyLeft, mode),
                                                                    getNavInputAmount(NavInput._KeyDown, mode) - getNavInputAmount(NavInput._KeyUp, mode))
        if (dirSources has NavDirSourceFlag.PadDPad) delta += Vec2(getNavInputAmount(NavInput.DpadRight,
                                                                                     mode) - getNavInputAmount(NavInput.DpadLeft, mode),
                                                                   getNavInputAmount(NavInput.DpadDown, mode) - getNavInputAmount(NavInput.DpadUp, mode))
        if (dirSources has NavDirSourceFlag.PadLStick) delta += Vec2(getNavInputAmount(NavInput.LStickRight,
                                                                                       mode) - getNavInputAmount(NavInput.LStickLeft, mode),
                                                                     getNavInputAmount(NavInput.LStickDown, mode) - getNavInputAmount(NavInput.LStickUp, mode))
        if (slowFactor != 0f && NavInput.TweakSlow.isDown()) delta *= slowFactor
        if (fastFactor != 0f && NavInput.TweakFast.isDown()) delta *= fastFactor
        return delta
    }

    /** t0 = previous time (e.g.: g.Time - g.IO.DeltaTime)
     *  t1 = current time (e.g.: g.Time)
     *  An event is triggered at:
     *  t = 0.0f     t = repeat_delay,    t = repeat_delay + repeat_rate*N */
    fun calcTypematicRepeatAmount(t0: Float, t1: Float, repeatDelay: Float, repeatRate: Float) = when {
        t1 == 0f -> 1
        t0 >= t1 -> 0
        else -> when {
            repeatRate <= 0f -> (t0 < repeatDelay && t1 >= repeatDelay).i
            else -> {
                val countT0 = if (t0 < repeatDelay) -1 else ((t0 - repeatDelay) / repeatRate).i
                val countT1 = if (t1 < repeatDelay) -1 else ((t1 - repeatDelay) / repeatRate).i
                countT1 - countT0 // count
            }
        }
    }

    /** Remotely activate a button, checkbox, tree node etc. given its unique ID. activation is queued and processed
     *  on the next frame when the item is encountered again.  */
    fun activateItem(id: ID) {
        g.navNextActivateId = id
    }

    /** FIXME-NAV: The existence of SetNavID vs SetFocusID properly needs to be clarified/reworked. */
    fun setNavID(id: ID, navLayer: NavLayer, focusScopeId: ID, rectRel: Rect) { // assert(navLayer == 0 || navLayer == 1) useless on jvm
        val navWindow = g.navWindow!!
        assert(navLayer == NavLayer.Main || navLayer == NavLayer.Menu)
        g.navId = id
        g.navLayer = navLayer
        g.navFocusScopeId = focusScopeId
        navWindow.navLastIds[navLayer] = id
        navWindow.navRectRel[navLayer] = rectRel
        //g.NavDisableHighlight = false;
        //g.NavDisableMouseHover = g.NavMousePosDirty = true;
    }
}