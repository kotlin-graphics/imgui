package imgui.internal.api

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.io
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import imgui.static.navUpdateAnyRequestFlag

/** Gamepad/Keyboard Navigation */
internal interface navigation {

    fun navInitWindow(window: Window, forceReinit: Boolean) {

        assert(window == g.navWindow)
        var initForNav = false
        if (window.flags hasnt WindowFlag.NoNavInputs) if (window.flags hasnt WindowFlag._ChildWindow || window.flags has WindowFlag._Popup || window.navLastIds[0] == 0 || forceReinit) initForNav =
            true
        IMGUI_DEBUG_LOG_NAV("[nav] NavInitRequest: from NavInitWindow(), init_for_nav=$initForNav, window=\"${window.name}\", layer=${g.navLayer}")
        if (initForNav) {
            setNavID(0, g.navLayer, 0)
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

    fun navMoveRequestButNoResultYet(): Boolean =
        g.navMoveRequest && g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0

    fun navMoveRequestCancel() {
        g.navMoveRequest = false
        navUpdateAnyRequestFlag()
    }

    fun navMoveRequestForward(moveDir: Dir, clipDir: Dir, bbRel: Rect, moveFlags: NavMoveFlags) {

        assert(g.navMoveRequestForward == NavForward.None)
        navMoveRequestCancel()
        g.navMoveDir = moveDir
        g.navMoveDir = clipDir
        g.navMoveRequestForward = NavForward.ForwardQueued
        g.navMoveRequestFlags = moveFlags
        g.navWindow!!.navRectRel[g.navLayer] = bbRel
    }

    fun navMoveRequestTryWrapping(window: Window, moveFlags: NavMoveFlags) {

        // Navigation wrap-around logic is delayed to the end of the frame because this operation is only valid after entire
        // popup is assembled and in case of appended popups it is not clear which EndPopup() call is final.
        g.navWrapRequestWindow = window
        g.navWrapRequestFlags = moveFlags
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

    /** FIXME-NAV: Refactor those functions into a single, more explicit one. */
    fun setNavID(id: ID,
                 navLayer: NavLayer,
                 focusScopeId: ID) { // assert(navLayer == 0 || navLayer == 1) useless on jvm
        g.navId = id
        g.navFocusScopeId = focusScopeId
        g.navWindow!!.navLastIds[navLayer] = id
    }

    fun setNavIDWithRectRel(id: ID, navLayer: NavLayer, focusScopeId: ID, rectRel: Rect) {
        setNavID(id, navLayer, focusScopeId)
        g.navWindow!!.navRectRel[navLayer] put rectRel
        g.navMousePosDirty = true
        g.navDisableHighlight = false
        g.navDisableMouseHover = true
    }
}