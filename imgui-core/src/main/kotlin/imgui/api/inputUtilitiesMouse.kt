package imgui.api

import glm_.i
import glm_.vec2.Vec2
import imgui.ImGui.calcTypematicRepeatAmount
import imgui.ImGui.io
import imgui.ImGui.isMouseDragPastThreshold
import imgui.ImGui.style
import imgui.MOUSE_INVALID
import imgui.MouseCursor
import imgui.internal.classes.Rect

/** Inputs Utilities: Mouse
 *  - Mouse buttons are referred to as simple integer, and it is guaranteed that 0=Left, 1=Right, 2=Middle. */
interface inputUtilitiesMouse {

    /** is mouse button held?   */
    fun isMouseDown(button: Int): Boolean {
        assert(button in io.mouseDown.indices)
        return io.mouseDown[button]
    }

    /** did mouse button clicked? (went from !Down to Down) */
    fun isMouseClicked(button: Int, repeat: Boolean = false): Boolean {

        assert(button >= 0 && button < io.mouseDown.size)
        val t = io.mouseDownDuration[button]
        if (t == 0f)
            return true

        if (repeat && t > io.keyRepeatDelay) {
            // FIXME: 2019/05/03: Our old repeat code was wrong here and led to doubling the repeat rate, which made it an ok rate for repeat on mouse hold.
            val amount = calcTypematicRepeatAmount(t - io.deltaTime, t, io.keyRepeatDelay, io.keyRepeatRate * 0.5f)
            if (amount > 0)
                return true
        }
        return false
    }

    /** did mouse button released? (went from Down to !Down) */
    fun isMouseReleased(button: Int): Boolean =
            io.mouseReleased[button]

    /** did mouse button double-clicked? A double-click returns false in IsMouseClicked(). uses io.MouseDoubleClickTime.    */
    fun isMouseDoubleClicked(button: Int): Boolean =
            io.mouseDoubleClicked[button]

    /** is mouse dragging? if lock_threshold < -1.0f uses io.MouseDraggingThreshold */
    fun isMouseDragging(button: Int, lockThreshold: Float = -1f): Boolean {
        assert(button in io.mouseDown.indices)
        if (!io.mouseDown[button])
            return false
        return isMouseDragPastThreshold(button, lockThreshold)
    }

    /** Test if mouse cursor is hovering given rectangle
     *  NB- Rectangle is clipped by our current clip setting
     *  NB- Expand the rectangle to be generous on imprecise inputs systems (g.style.TouchExtraPadding)
     *  is mouse hovering given bounding rect (in screen space). clipped by current clipping settings, but disregarding
     *  of other consideration of focus/window ordering/popup-block.
     *
     *  is mouse hovering given bounding rect (in screen space)? clipped by current clipping settings if 'clip=true', but disregarding of other consideration of focus/window ordering/popup-block. */
    fun isMouseHoveringRect(r: Rect, clip: Boolean = true): Boolean =
            isMouseHoveringRect(r, clip)

    fun isMouseHoveringRect(rMin: Vec2, rMax: Vec2, clip: Boolean = true): Boolean {

        // Clip
        val rectClipped = Rect(rMin, rMax)
        if (clip)
            rectClipped clipWith g.currentWindow!!.clipRect

        // Expand for touch input
        val rectForTouch = Rect(rectClipped.min - style.touchExtraPadding, rectClipped.max + style.touchExtraPadding)
        return io.mousePos in rectForTouch
    }

    /** by convention we use (-FLT_MAX,-FLT_MAX) to denote that there is no mouse available  */
    fun isMousePosValid(mousePos: Vec2? = null): Boolean =
            (mousePos ?: io.mousePos) allGreaterThan MOUSE_INVALID

    /** is any mouse button held?    */
    val isAnyMouseDown: Boolean
        get() = io.mouseDown.any()

    /** shortcut to io.mousePos provided by user, to be consistent with other calls
     *  ~GetMousePos    */
    val mousePos: Vec2
        get() = io.mousePos

    /** retrieve backup of mouse position at the time of opening popup we have BeginPopup() into
     *  ~GetMousePosOnOpeningCurrentPopup   */
    val mousePosOnOpeningCurrentPopup: Vec2
        get() = Vec2(g.beginPopupStack.lastOrNull()?.openMousePos ?: io.mousePos)

    /** return the delta from the initial clicking position while the mouse button is clicked or was just released.
     *  This is locked and return 0.0f until the mouse moves past a distance threshold at least once.
     *  If lock_threshold < -1.0f uses io.MouseDraggingThreshold
     *
     *  Back-ends in theory should always keep mouse position valid when dragging even outside the client window. */
    fun getMouseDragDelta(button: Int = 0, lockThreshold_: Float = -1f): Vec2 {

        assert(button >= 0 && button < io.mouseDown.size)
        var lockThreshold = lockThreshold_
        if (lockThreshold < 0f)
            lockThreshold = io.mouseDragThreshold
        if (io.mouseDown[button] || io.mouseReleased[button])
            if (io.mouseDragMaxDistanceSqr[button] >= lockThreshold * lockThreshold)
                if (isMousePosValid(io.mousePos) && isMousePosValid(io.mouseClickedPos[button]))
                    return io.mousePos - io.mouseClickedPos[button]
        return Vec2()
    }

    fun resetMouseDragDelta(button: Int = 0) {
        assert(button in io.mouseDown.indices)
        // NB: We don't need to reset g.IO.MouseDragMaxDistanceSqr
        io.mouseClickedPos[button] = io.mousePos
    }

    var mouseCursor: MouseCursor
        /** Get desired cursor type, reset in newFrame(), this is updated during the frame. valid before render().
         *  If you use software rendering by setting io.mouseDrawCursor ImGui will render those for you
         *
         *  ~getMouseCursor  */
        get() = g.mouseCursor
        /** set desired cursor type
         *
         *  ~setMouseCursor */
        set(value) {
            g.mouseCursor = value
        }

    /** Manually override io.WantCaptureMouse flag next frame (said flag is entirely left for your application to handle). */
    fun captureMouseFromApp(capture: Boolean = true) {
        g.wantCaptureMouseNextFrame = capture.i
    }
}