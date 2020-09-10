package engine.context

import engine.core.*
import glm_.glm
import glm_.vec2.Vec2
import imgui.ID
import imgui.clamp
import imgui.hasnt
import imgui.internal.NavLayer
import imgui.internal.bezierCalc
import imgui.internal.classes.Rect
import imgui.internal.lengthSqr
import imgui.wo
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import imgui.WindowFlag as Wf

// [JVM]
fun TestContext.mouseMove(ref: String, flags: TestOpFlags = TestOpFlag.None.i) = mouseMove(TestRef(path = ref), flags)

// [JVM]
fun TestContext.mouseMove(ref: ID, flags: TestOpFlags = TestOpFlag.None.i) = mouseMove(TestRef(ref), flags)

// FIXME: Maybe ImGuiTestOpFlags_NoCheckHoveredId could be automatic if we detect that another item is active as intended?
fun TestContext.mouseMove(ref: TestRef, flags: TestOpFlags = TestOpFlag.None.i) {

    if (isError) return

    REGISTER_DEPTH {
        val g = uiContext!!
        val item = itemLocate(ref)
        val desc = TestRefDesc(ref, item)
        logDebug("MouseMove to $desc")

        if (item == null)
            return
        item.refCount++

        // Focus window before scrolling/moving so things are nicely visible
        if (flags hasnt TestOpFlag.NoFocusWindow)
            windowBringToFront(item.window)

        val window = item.window!!
        val windowInnerRPadded = Rect(window.innerClipRect)
        windowInnerRPadded expand -4f // == WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
        if (item.navLayer == NavLayer.Main && item.rectClipped !in windowInnerRPadded)
            scrollToY(ref)

        val pos = item.rectFull.center
        windowMoveToMakePosVisible(window, pos)

        // Move toward an actually visible point
        pos put item.rectClipped.center
        mouseMoveToPos(pos)

        // Focus again in case something made us lost focus (which could happen on a simple hover)
        if (flags hasnt TestOpFlag.NoFocusWindow)
            windowBringToFront(window)// , ImGuiTestOpFlags_Verbose);

        if (!abort && flags hasnt TestOpFlag.NoCheckHoveredId) {
            val hoveredId = g.hoveredIdPreviousFrame
            if (hoveredId != item.id) {
                if (window.flags hasnt Wf.NoResize && flags hasnt TestOpFlag.IsSecondAttempt) {
                    var isResizeCorner = false
                    for (n in 0..1)
                        isResizeCorner = isResizeCorner || (hoveredId == window getResizeID n)
                    if (isResizeCorner) {
                        logDebug("Obstructed by ResizeGrip, trying to resize window and trying again..")
                        val extraSize = window.calcFontSize() * 3f
                        windowResize(window.id, window.size + Vec2(extraSize))
                        mouseMove(ref, flags or TestOpFlag.IsSecondAttempt)
                        item.refCount--
                        return
                    }
                }

                ERRORF_NOHDR("Unable to Hover $desc. Expected %08X in '${item.window?.name ?: "<NULL>"}', " +
                        "HoveredId was %08X in '${g.hoveredWindow?.name ?: ""}'. Targeted position (%.1f,%.1f)",
                        item.id, hoveredId, pos.x, pos.y)
            }
        }

        item.refCount--
    }
}

fun TestContext.mouseMoveToPos(target: Vec2) {

    val g = uiContext!!
    if (isError) return

    REGISTER_DEPTH {
        logDebug("MouseMove from (%.0f,%.0f) to (%.0f,%.0f)", inputs!!.mousePosValue.x, inputs!!.mousePosValue.y, target.x, target.y)

        if (engineIO!!.configRunFast) {
            inputs!!.mousePosValue put target
            engine!!.yield()
            engine!!.yield()
            return
        }

        // Simulate slower movements. We use a slightly curved movement to make the movement look less robotic.

        // Calculate some basic parameters
        val startPos = inputs!!.mousePosValue
        val delta = target - startPos
        val length2 = delta.lengthSqr
        val length = if (length2 > 0.0001f) sqrt(length2) else 1f
        val invLength = 1f / length

        // Calculate a vector perpendicular to the motion delta
        val perp = Vec2(delta.y, -delta.x) * invLength

        // Calculate how much wobble we want, clamped to max out when the delta is 100 pixels (shorter movements get less wobble)
        val positionOffsetMagnitude = clamp(length, 1f, 100f) * engineIO!!.mouseWobble

        // Wobble positions, using a sine wave based on position as a cheap way to get a deterministic offset
        val intermediatePosA = startPos + (delta * 0.3f)
        val intermediatePosB = startPos + (delta * 0.6f)
        intermediatePosA += perp * sin(intermediatePosA.y * 0.1f) * positionOffsetMagnitude
        intermediatePosB += perp * cos(intermediatePosB.y * 0.1f) * positionOffsetMagnitude

        // We manipulate Inputs->MousePosValue without reading back from g.IO.MousePos because the later is rounded.
        // To handle high framerate it is easier to bypass this rounding.
        var currentDist = 0f // Our current distance along the line (in pixels)
        while (true) {
            val moveSpeed = engineIO!!.mouseSpeed * g.io.deltaTime

            //if (g.IO.KeyShift)
            //    move_speed *= 0.1f;

            currentDist += moveSpeed // Move along the line

            // Calculate a parametric position on the direct line that we will use for the curve
            var t = currentDist * invLength
            t = clamp(t, 0f, 1f)
            t = 1f - (cos(t * glm.πf) + 1f) * 0.5f // Generate a smooth curve with acceleration/deceleration

            //ImGui::GetOverlayDrawList()->AddCircle(target, 10.0f, IM_COL32(255, 255, 0, 255));

            if (t >= 1f) {
                inputs!!.mousePosValue put target
                engine!!.yield()
                engine!!.yield()
                return
            } else {
                // Use a bezier curve through the wobble points
                inputs!!.mousePosValue put bezierCalc(startPos, intermediatePosA, intermediatePosB, target, t)
                //ImGui::GetOverlayDrawList()->AddBezierCurve(start_pos, intermediate_pos_a, intermediate_pos_b, target, IM_COL32(255,0,0,255), 1.0f);
                engine!!.yield()
            }
        }
    }
}
//void MouseMoveToPosInsideWindow (ImVec2 * pos, ImGuiWindow* window)

// TODO: click time argument (seconds and/or frames)
fun TestContext.mouseClick(button: Int = 0) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("MouseClick $button")

        // Make sure mouse buttons are released
        assert(inputs!!.mouseButtonsValue == 0)

        yield()

        // Press
        uiContext!!.io.mouseClickedTime[button] = -Double.MAX_VALUE // Prevent accidental double-click from happening ever
        inputs!!.mouseButtonsValue = 1 shl button
        yield()
        inputs!!.mouseButtonsValue = 0
        yield()
        yield() // Give a frame for items to react
    }
}

// TODO: click time argument (seconds and/or frames)
fun TestContext.mouseDoubleClick(button: Int = 0) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("MouseDoubleClick $button")

        yield()
        uiContext!!.io.mouseClickedTime[button] = -Double.MAX_VALUE // Prevent accidental double-click followed by single click
        for (n in 0..1) {
            inputs!!.mouseButtonsValue = 1 shl button
            yield()
            inputs!!.mouseButtonsValue = 0
            yield()
        }
        yield() // Give a frame for items to react
    }
}

fun TestContext.mouseDown(button: Int = 0) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("MouseDown $button")

        uiContext!!.io.mouseClickedTime[button] = -Double.MAX_VALUE // Prevent accidental double-click from happening ever
        inputs!!.mouseButtonsValue = 1 shl button
        yield()
    }
}

fun TestContext.mouseUp(button: Int = 0) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("MouseUp $button")

        inputs!!.mouseButtonsValue = inputs!!.mouseButtonsValue wo (1 shl button)
        yield()
    }
}


fun TestContext.mouseLiftDragThreshold(button: Int = 0) {

    if (isError) return

    uiContext!!.io.apply {
        mouseDragMaxDistanceAbs[button] put mouseDragThreshold
        mouseDragMaxDistanceSqr[button] = mouseDragThreshold * mouseDragThreshold * 2
    }
}