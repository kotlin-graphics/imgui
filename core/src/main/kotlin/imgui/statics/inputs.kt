package imgui.static

import glm_.func.common.abs
import glm_.glm
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.data
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.mouseButtonToKey
import imgui.ImGui.ownerData
import imgui.ImGui.setPos
import imgui.ImGui.setScrollX
import imgui.ImGui.setScrollY
import imgui.ImGui.setSize
import imgui.ImGui.testOwner
import imgui.api.g
import imgui.internal.exponentialMovingAverage
import imgui.internal.floor
import imgui.internal.floorSigned
import imgui.internal.lengthSqr
import imgui.internal.sections.*

// Inputs
fun updateKeyboardInputs() {

    // Import legacy keys or verify they are not used

    // Update aliases
    for (n in 0 until MouseButton.COUNT)
        updateAliasKey(mouseButtonToKey(n), io.mouseDown[n], if (io.mouseDown[n]) 1f else 0f)
    updateAliasKey(Key.MouseWheelX, io.mouseWheelH != 0f, io.mouseWheelH)
    updateAliasKey(Key.MouseWheelY, io.mouseWheel != 0f, io.mouseWheel)

    // Synchronize io.KeyMods and io.KeyXXX values.
    // - New backends (1.87+): send io.AddKeyEvent(ImGuiMod_XXX) ->                                      -> (here) deriving io.KeyMods + io.KeyXXX from key array.
    // - Legacy backends:      set io.KeyXXX bools               -> (above) set key array from io.KeyXXX -> (here) deriving io.KeyMods + io.KeyXXX from key array.
    // So with legacy backends the 4 values will do a unnecessary back-and-forth but it makes the code simpler and future facing.
    io.keyMods = mergedModsFromKeys
    io.keyCtrl = io.keyMods has Key.Mod_Ctrl
    io.keyShift = io.keyMods has Key.Mod_Shift
    io.keyAlt = io.keyMods has Key.Mod_Alt
    io.keySuper = io.keyMods has Key.Mod_Super

    // Clear gamepad data if disabled
    if (io.backendFlags hasnt BackendFlag.HasGamepad)
        for (key in Key.Gamepad) {
            io.keysData[key.index].down = false
            io.keysData[key.index].analogValue = 0f
        }

    // Update keys
    for (keyData in io.keysData) {
        keyData.downDurationPrev = keyData.downDuration
        keyData.downDuration = if (keyData.down) (if (keyData.downDuration < 0f) 0f else keyData.downDuration + io.deltaTime) else -1f
    }

    // Update keys/input owner (named keys only): one entry per key
    for (key in Key.Named) {
        val keyData = key.data
        val ownerData = key.ownerData
        ownerData.ownerCurr = ownerData.ownerNext
        if (!keyData.down) // Important: ownership is released on the frame after a release. Ensure a 'MouseDown -> CloseWindow -> MouseUp' chain doesn't lead to someone else seeing the MouseUp.
            ownerData.ownerNext = KeyOwner_None
        ownerData.lockThisFrame = ownerData.lockUntilRelease && keyData.down; ownerData.lockUntilRelease = ownerData.lockThisFrame  // Clear LockUntilRelease when key is not Down anymore
    }

    updateKeyRoutingTable(g.keysRoutingTable)
}

fun updateMouseInputs() {

    with(io) {

        // Round mouse position to avoid spreading non-rounded position (e.g. UpdateManualResize doesn't support them well)
        if (isMousePosValid(mousePos)) {
            mousePos put floorSigned(mousePos); g.mouseLastValidPos put mousePos
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

fun updateMouseWheel() {

    // Reset the locked window if we move the mouse or after the timer elapses.
    // FIXME: Ideally we could refactor to have one timer for "changing window w/ same axis" and a shorter timer for "changing window or axis w/ other axis" (#3795)
    if (g.wheelingWindow != null) {
        g.wheelingWindowReleaseTimer -= io.deltaTime
        if (isMousePosValid() && (io.mousePos - g.wheelingWindowRefMousePos).lengthSqr > io.mouseDragThreshold * io.mouseDragThreshold)
            g.wheelingWindowReleaseTimer = 0f
        if (g.wheelingWindowReleaseTimer <= 0f)
            lockWheelingWindow(null, 0f)
    }

    val wheel = Vec2(if (Key.MouseWheelX testOwner KeyOwner_None) g.io.mouseWheelH else 0f,
                     if (Key.MouseWheelY testOwner KeyOwner_None) g.io.mouseWheel else 0f)

    //IMGUI_DEBUG_LOG("MouseWheel X:%.3f Y:%.3f\n", wheel_x, wheel_y);
    val mouseWindow = g.wheelingWindow ?: g.hoveredWindow
    if (mouseWindow == null || mouseWindow.collapsed)
        return

    // Zoom / Scale window
    // FIXME-OBSOLETE: This is an old feature, it still works but pretty much nobody is using it and may be best redesigned.
    if (wheel.y != 0f && io.keyCtrl && io.fontAllowUserScaling) {
        lockWheelingWindow(mouseWindow, wheel.y)
        val window = mouseWindow
        val newFontScale = glm.clamp(window.fontWindowScale + io.mouseWheel * 0.1f, 0.5f, 2.5f)
        val scale = newFontScale / window.fontWindowScale
        window.fontWindowScale = newFontScale
        if (window === window.rootWindow) {
            val offset = window.size * (1f - scale) * (io.mousePos - window.pos) / window.size
            window.setPos(window.pos + offset)
            window.setSize( floor(window.size * scale))
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

    // Maintain a rough average of moving magnitude on both axises
    // FIXME: should by based on wall clock time rather than frame-counter
    g.wheelingAxisAvg.x = exponentialMovingAverage(g.wheelingAxisAvg.x, wheel.x.abs, 30)
    g.wheelingAxisAvg.y = exponentialMovingAverage(g.wheelingAxisAvg.y, wheel.y.abs, 30)

    // In the rare situation where FindBestWheelingWindow() had to defer first frame of wheeling due to ambiguous main axis, reinject it now.
    wheel += g.wheelingWindowWheelRemainder
    g.wheelingWindowWheelRemainder put 0f
    if (wheel.x == 0f && wheel.y == 0f)
        return

    // Mouse wheel scrolling: find target and apply
    // - don't renew lock if axis doesn't apply on the window.
    // - select a main axis when both axises are being moved.
    (g.wheelingWindow ?: findBestWheelingWindow(wheel))?.let { window ->
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            val doScroll = arrayOf(wheel.x != 0f && window.scrollMax.x != 0f, wheel.y != 0f && window.scrollMax.y != 0f)
            // [JVM] critical to use Axis set/get operator which rely on `i` rather than others which instead rely on `ordinal`
            if (doScroll[Axis.X] && doScroll[Axis.Y])
                doScroll[if (g.wheelingAxisAvg.x > g.wheelingAxisAvg.y) Axis.Y else Axis.X] = false
            if (doScroll[Axis.X]) {
                lockWheelingWindow(window, wheel.x)
                val maxStep = window.innerRect.width * 0.67f
                val scrollStep = floor((2 * window.calcFontSize()) min maxStep)
                window.setScrollX(window.scroll.x - wheel.x * scrollStep)
            }
            if (doScroll[Axis.Y]) {
                lockWheelingWindow(window, wheel.y)
                val maxStep = window.innerRect.height * 0.67f
                val scrollStep = floor((5 * window.calcFontSize()) min maxStep)
                window.setScrollY(window.scroll.y - wheel.y * scrollStep)
            }
        }
    }
}

// Rewrite routing data buffers to strip old entries + sort by key to make queries not touch scattered data.
//   Entries   D,A,B,B,A,C,B     --> A,A,B,B,B,C,D
//   Index     A:1 B:2 C:5 D:0   --> A:0 B:2 C:5 D:6
// See 'Metrics->Key Owners & Shortcut Routing' to visualize the result of that operation.
fun updateKeyRoutingTable(rt: KeyRoutingTable) {
    rt.entriesNext.clear()
    for (key in Key.Named) {
        val newRoutingStartIdx = rt.entriesNext.size
        var oldRoutingIdx = rt.index[key.index]
        while (oldRoutingIdx != -1) {
            val routingEntry = rt.entries[oldRoutingIdx].apply {
                routingCurr = routingNext // Update entry
                routingNext = KeyOwner_None
                routingNextScore = 255
            }
            if (routingEntry.routingCurr == KeyOwner_None) {
                oldRoutingIdx = routingEntry.nextEntryIndex
                continue
            }
            rt.entriesNext += routingEntry // Write alive ones into new buffer

            // Apply routing to owner if there's no owner already (RoutingCurr == None at this point)
            if (routingEntry.mods == g.io.keyMods) {
                val ownerData = key.ownerData
                if (ownerData.ownerCurr == KeyOwner_None)
                    ownerData.ownerCurr = routingEntry.routingCurr
            }
            oldRoutingIdx = routingEntry.nextEntryIndex
        }

        // Rewrite linked-list
        rt.index[key] = if (newRoutingStartIdx < rt.entriesNext.size) newRoutingStartIdx else -1
        for (n in newRoutingStartIdx until rt.entriesNext.size)
            rt.entriesNext[n].nextEntryIndex = if (n + 1 < rt.entriesNext.size) n + 1 else -1
    }
    val swap = ArrayList(rt.entries) // Swap new and old indexes
    rt.entries.clear()
    rt.entries += rt.entriesNext // Swap new and old indexes
    rt.entriesNext.clear()
    rt.entriesNext += swap
}