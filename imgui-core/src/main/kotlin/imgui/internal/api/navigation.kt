package imgui.internal.api

import gli_.has
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.io
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.*
import imgui.static.navUpdateAnyRequestFlag
import kotlin.math.max

/** Navigation */
internal interface navigation {

    fun navInitWindow(window: Window, forceReinit: Boolean) {

        assert(window == g.navWindow)
        var initForNav = false
        if (window.flags hasnt WindowFlag.NoNavInputs)
            if (window.flags hasnt WindowFlag._ChildWindow || window.flags has WindowFlag._Popup || window.navLastIds[0] == 0 || forceReinit)
                initForNav = true
        //IMGUI_DEBUG_LOG("[Nav] NavInitWindow() init_for_nav=%d, window=\"%s\", layer=%d\n", init_for_nav, window->Name, g.NavLayer)
        if (initForNav) {
            setNavId(0, g.navLayer, 0)
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

    fun navMoveRequestButNoResultYet(): Boolean = g.navMoveRequest && g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0

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

        if (g.navWindow !== window || !navMoveRequestButNoResultYet() || g.navMoveRequestForward != NavForward.None || g.navLayer != NavLayer.Main)
            return
        assert(moveFlags != 0) // No points calling this with no wrapping
        val bbRel = window.navRectRel[0]

        var clipDir = g.navMoveDir
        if (g.navMoveDir == Dir.Left && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
            bbRel.min.x = max(window.sizeFull.x, window.contentSize.x + window.windowPadding.x * 2f) - window.scroll.x
            bbRel.max.x = bbRel.min.x
            if (moveFlags has NavMoveFlag.WrapX) {
                bbRel translateY -bbRel.height
                clipDir = Dir.Up
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Right && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
            bbRel.min.x = -window.scroll.x
            bbRel.max.x = -window.scroll.x
            if (moveFlags has NavMoveFlag.WrapX) {
                bbRel translateY +bbRel.height
                clipDir = Dir.Down
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Up && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
            bbRel.min.y = max(window.sizeFull.y, window.contentSize.y + window.windowPadding.y * 2f) - window.scroll.y
            bbRel.max.y = bbRel.min.y
            if (moveFlags has NavMoveFlag.WrapY) {
                bbRel translateX -bbRel.width
                clipDir = Dir.Left
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Down && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
            bbRel.min.y = -window.scroll.y
            bbRel.max.y = -window.scroll.y
            if (moveFlags has NavMoveFlag.WrapY) {
                bbRel translateX +bbRel.width
                clipDir = Dir.Right
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
    }

    fun getNavInputAmount(n: NavInput, mode: InputReadMode): Float {    // TODO -> NavInput?

        val i = n.i
        if (mode == InputReadMode.Down) return io.navInputs[i] // Instant, read analog input (0.0f..1.0f, as provided by user)

        val t = io.navInputsDownDuration[i]
        return when {
            // Return 1.0f when just released, no repeat, ignore analog input.
            t < 0f && mode == InputReadMode.Released -> if (io.navInputsDownDurationPrev[i] >= 0f) 1f else 0f
            t < 0f -> 0f
            else -> when (mode) {
                // Return 1.0f when just pressed, no repeat, ignore analog input.
                InputReadMode.Pressed -> if (t == 0f) 1 else 0
                InputReadMode.Repeat -> calcTypematicRepeatAmount(t - io.deltaTime, t, io.keyRepeatDelay * 0.72f, io.keyRepeatRate * 0.8f)
                InputReadMode.RepeatSlow -> calcTypematicRepeatAmount(t - io.deltaTime, t, io.keyRepeatDelay * 1.25f, io.keyRepeatRate * 2f)
                InputReadMode.RepeatFast -> calcTypematicRepeatAmount(t - io.deltaTime, t, io.keyRepeatDelay * 0.72f, io.keyRepeatRate * 0.3f)
                else -> 0
            }.f
        }
    }

    /** @param dirSources: NavDirSourceFlag    */
    fun getNavInputAmount2d(dirSources: NavDirSourceFlags, mode: InputReadMode, slowFactor: Float = 0f, fastFactor: Float = 0f): Vec2 {
        val delta = Vec2()
        if (dirSources has NavDirSourceFlag.Keyboard)
            delta += Vec2(getNavInputAmount(NavInput._KeyRight, mode) - getNavInputAmount(NavInput._KeyLeft, mode),
                    getNavInputAmount(NavInput._KeyDown, mode) - getNavInputAmount(NavInput._KeyUp, mode))
        if (dirSources has NavDirSourceFlag.PadDPad)
            delta += Vec2(getNavInputAmount(NavInput.DpadRight, mode) - getNavInputAmount(NavInput.DpadLeft, mode),
                    getNavInputAmount(NavInput.DpadDown, mode) - getNavInputAmount(NavInput.DpadUp, mode))
        if (dirSources has NavDirSourceFlag.PadLStick)
            delta += Vec2(getNavInputAmount(NavInput.LStickRight, mode) - getNavInputAmount(NavInput.LStickLeft, mode),
                    getNavInputAmount(NavInput.LStickDown, mode) - getNavInputAmount(NavInput.LStickUp, mode))
        if (slowFactor != 0f && NavInput.TweakSlow.isDown())
            delta *= slowFactor
        if (fastFactor != 0f && NavInput.TweakFast.isDown())
            delta *= fastFactor
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
    fun setNavId(id: ID, navLayer: NavLayer, focusScopeId: Int) {
        // assert(navLayer == 0 || navLayer == 1) useless on jvm
        g.navId = id
        g.navFocusScopeId = focusScopeId
        g.navWindow!!.navLastIds[navLayer] = id
    }

    fun setNavIDWithRectRel(id: ID, navLayer: NavLayer, focusScopeId: Int, rectRel: Rect) {
        setNavId(id, navLayer, focusScopeId)
        g.navWindow!!.navRectRel[navLayer] put rectRel
        g.navMousePosDirty = true
        g.navDisableHighlight = false
        g.navDisableMouseHover = true
    }
}