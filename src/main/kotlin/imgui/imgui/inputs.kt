package imgui.imgui

import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.ImGui.style
import imgui.IO
import imgui.ImGui.calcTypematicPressedRepeatAmount
import imgui.ImGui.io
import imgui.MOUSE_INVALID
import imgui.g
import imgui.internal.Rect


interface imgui_inputs {

    fun getKeyIndex(imguiKey: Int) = io.keyMap[imguiKey]

    /** is key being held. == io.KeysDown[user_key_index]. note that imgui doesn't know the semantic of each entry of io.KeyDown[].
     *  Use your own indices/enums according to how your back-end/engine stored them into KeyDown[]! */
    fun isKeyDown(userKeyIndex: Int) = if (userKeyIndex < 0) false else io.keysDown[userKeyIndex]

    /** uses user's key indices as stored in the keys_down[] array. if repeat=true.
     *  uses io.KeyRepeatDelay / KeyRepeatRate  */
    fun isKeyPressed(userKeyIndex: Int, repeat: Boolean = true) = if (userKeyIndex < 0) false
    else {
        val t = io.keysDownDuration[userKeyIndex]
        when {
            t == 0f -> true
            repeat && t > io.keyRepeatDelay -> getKeyPressedAmount(userKeyIndex, io.keyRepeatDelay, io.keyRepeatRate) > 0
            else -> false
        }
    }

    /** was key released (went from Down to !Down)..    */
    fun isKeyReleased(userKeyIndex: Int) = if (userKeyIndex < 0) false else io.keysDownDurationPrev[userKeyIndex] >= 0f && !io.keysDown[userKeyIndex]

    /** Uses provided repeat rate/delay. return a count, most often 0 or 1 but might be >1 if RepeatRate is small enough
     *  that DeltaTime > RepeatRate */
    fun getKeyPressedAmount(keyIndex: Int, repeatDelay: Float, repeatRate: Float): Int {
        if (keyIndex < 0) return 0
        assert(keyIndex in 0 until io.keysDown.size)
        val t = io.keysDownDuration[keyIndex]
        return calcTypematicPressedRepeatAmount(t, t - io.deltaTime, repeatDelay, repeatRate)
    }

    /** is mouse button held */
    fun isMouseDown(button: Int): Boolean {
        assert(button in io.mouseDown.indices)
        return io.mouseDown[button]
    }

    /** did mouse button clicked (went from !Down to Down)  */
    fun isMouseClicked(button: Int, repeat: Boolean = false): Boolean {

        assert(button >= 0 && button < io.mouseDown.size)
        val t = io.mouseDownDuration[button]
        if (t == 0f)
            return true

        if (repeat && t > io.keyRepeatDelay) {
            val delay = io.keyRepeatDelay
            val rate = io.keyRepeatRate
            if ((glm.mod(t - delay, rate) > rate * 0.5f) != (glm.mod(t - delay - io.deltaTime, rate) > rate * 0.5f))
                return true
        }
        return false
    }

    /** did mouse button double-clicked. a double-click returns false in IsMouseClicked(). uses io.MouseDoubleClickTime.    */
    fun isMouseDoubleClicked(button: Int) = io.mouseDoubleClicked[button]

    /** did mouse button released (went from Down to !Down) */
    fun isMouseReleased(button: Int) = io.mouseReleased[button]

    /** is mouse dragging. if lock_threshold < -1.0f uses io.MouseDraggingThreshold */
    fun isMouseDragging(button: Int = 0, lockThreshold: Float = -1f): Boolean {
        if (!io.mouseDown[button]) return false
        val lockThreshold = if (lockThreshold < 0f) io.mouseDragThreshold else lockThreshold
        return io.mouseDragMaxDistanceSqr[button] >= lockThreshold * lockThreshold
    }

    /** Test if mouse cursor is hovering given rectangle
     *  NB- Rectangle is clipped by our current clip setting
     *  NB- Expand the rectangle to be generous on imprecise inputs systems (g.style.TouchExtraPadding)
     *  is mouse hovering given bounding rect (in screen space). clipped by current clipping settings. disregarding of
     *  consideration of focus/window ordering/blocked by a popup.  */
    fun isMouseHoveringRect(r: Rect, clip: Boolean = true) = isMouseHoveringRect(r.min, r.max, clip)

    fun isMouseHoveringRect(rMin: Vec2, rMax: Vec2, clip: Boolean = true): Boolean {

        val window = g.currentWindow!!

        // Clip
        val rectClipped = Rect(rMin, rMax)
        if (clip)
            rectClipped.clipWith(window.clipRect)

        // Expand for touch input
        val rectForTouch = Rect(rectClipped.min - style.touchExtraPadding, rectClipped.max + style.touchExtraPadding)
        return rectForTouch contains io.mousePos
    }

    /** We typically use ImVec2(-FLT_MAX,-FLT_MAX) to denote an invalid mouse position  */
    fun isMousePosValid(mousePos: Vec2? = null) = (mousePos ?: io.mousePos) greaterThan MOUSE_INVALID

    /** shortcut to io.mousePos provided by user, to be consistent with other calls */
    val mousePos get() = io.mousePos

    /** retrieve backup of mouse positioning at the time of opening popup we have BeginPopup() into */
    val mousePosOnOpeningCurrentPopup get() = Vec2(g.currentPopupStack.lastOrNull()?.openMousePos ?: io.mousePos)

    /** dragging amount since clicking. if lockThreshold < -1.0f uses io.MouseDraggingThreshold
     *  NB: This is only valid if isMousePosValid(). Back-ends in theory should always keep mouse position valid
     *  when dragging even outside the client window. */
    fun getMouseDragDelta(button: Int = 0, lockThreshold: Float = -1f): Vec2 {

        assert(button >= 0 && button < io.mouseDown.size)
        var lockThreshold = lockThreshold
        if (lockThreshold < 0f)
            lockThreshold = io.mouseDragThreshold
        if (io.mouseDown[button])
            if (io.mouseDragMaxDistanceSqr[button] >= lockThreshold * lockThreshold)
                return io.mousePos - io.mouseClickedPos[button] // Assume we can only get active with left-mouse button (at the moment).
        return Vec2()
    }

    fun resetMouseDragDelta(button: Int = 0) = io.mouseClickedPos.get(button).put(io.mousePos) // NB: We don't need to reset g.io.MouseDragMaxDistanceSqr

    var mouseCursor
        /** Get desired cursor type, reset in newFrame(), this is updated during the frame. valid before render().
         *  If you use software rendering by setting io.mouseDrawCursor ImGui will render those for you */
        get() = g.mouseCursor
        /** set desired cursor type */
        set(value) {
            g.mouseCursor = value
        }

    /** Manually override io.wantCaptureKeyboard flag next frame (said flag is entirely left for your application handle).
     *  e.g. force capture keyboard when your widget is being hovered.  */
    fun captureKeyboardFromApp(capture: Boolean = true) {
        g.wantCaptureKeyboardNextFrame = capture.i
    }

    /** Manually override io.WantCaptureMouse flag next frame (said flag is entirely left for your application handle). */
    fun captureMouseFromApp(capture: Boolean = true) {
        g.wantCaptureMouseNextFrame = capture.i
    }
}