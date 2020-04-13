package engine.context

import engine.KeyState
import engine.core.*
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.Key
import imgui.NavInput
import imgui.internal.InputSource
import imgui.internal.classes.Rect

infix fun TestContext.setInputMode(inputMode: InputSource) {

    assert(inputMode == InputSource.Mouse || inputMode == InputSource.Nav)
    this.inputMode = inputMode

    uiContext!!.apply {
        if (inputMode == InputSource.Nav) {
            navDisableHighlight = false
            navDisableMouseHover = true
        } else {
            navDisableHighlight = true
            navDisableMouseHover = false
        }
    }
}

infix fun TestContext.navKeyPress(input: NavInput) {

    assert(input != NavInput.Count)
    if (isError) return

    REGISTER_DEPTH {
        logDebug("NavInput ${input.i}")

        engine!! pushInput TestInput.fromNav(input, KeyState.Down)
        yield()
        engine!! pushInput TestInput.fromNav(input, KeyState.Up)
        yield()
        yield() // For nav code to react e.g. run a query
    }
}

// [JVM]
infix fun TestContext.navMoveTo(ref: String) = navMoveTo(TestRef(path = ref))

infix fun TestContext.navMoveTo(ref: TestRef) {

    if (isError) return

    REGISTER_DEPTH {
        val g = uiContext!!
        val item = itemLocate(ref)
        val desc = TestRefDesc(ref, item)
        logDebug("NavMove to $desc")

        if (item == null) return
        item.refCount++

        // Focus window before scrolling/moving so things are nicely visible
        windowBringToFront(item.window)

        // Teleport
        // FIXME-NAV: We should have a nav request feature that does this,
        // except it'll have to queue the request to find rect, then set scrolling, which would incur a 2 frame delay :/
        assert(!g.navMoveRequest)
        val rectRel = Rect(item.rectFull)
        val win = item.window!!
        rectRel translate Vec2(-win.pos.x, -win.pos.y)
        ImGui.setNavIDWithRectRel(item.id, item.navLayer, 0, rectRel)  // Ben- Not sure why this is needed in the coroutine case but not otherwise?
        win scrollToBringRectIntoView item.rectFull
        while (g.navMoveRequest)
            yield()

        if (!abort && g.navId != item.id)
            ERRORF_NOHDR("Unable to set NavId to $desc")

        item.refCount--
    }
}

fun TestContext.navActivate() { // Activate current selected item. Same as pressing [space].

    if (isError) return

    REGISTER_DEPTH {
        logDebug("NavActivate")

        yield()

        val framesToHold = 2 // FIXME-TESTS: <-- number of frames could be fuzzed here
        if (true) {
            // Feed gamepad nav inputs
            for (n in 0 until framesToHold) {
                engine!! pushInput TestInput.fromNav(NavInput.Activate, KeyState.Down)
                yield()
            }
            yield()
        } else {
            // Feed keyboard keys
            engine!! pushInput TestInput.fromKey(Key.Space, KeyState.Down)
            for (n in 0 until framesToHold)
                yield()
            engine!! pushInput TestInput.fromKey(Key.Space, KeyState.Up)
            yield()
            yield()
        }
    }
}

fun TestContext.navInput() {     // Press ImGuiNavInput_Input (e.g. Triangle) to turn a widget into a text input

    if (isError) return

    REGISTER_DEPTH {
        logDebug("NavInput")

        val framesToHold = 2 // FIXME-TESTS: <-- number of frames could be fuzzed here
        for (n in 0 until framesToHold) {
            engine!! pushInput TestInput.fromNav(NavInput.Input, KeyState.Down)
            yield()
        }
        yield()
    }
}