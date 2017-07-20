package imgui.imgui

import glm_.glm
import glm_.vec2.Vec2
import imgui.IO
import imgui.ImGui.currentWindowRead
import imgui.Key
import imgui.Style
import imgui.internal.Rect
import imgui.Context as g


//IMGUI_API int           GetKeyIndex(ImGuiKey imgui_key);
//IMGUI_API bool          IsKeyDown(int user_key_index);                                      // is key being held. == io.KeysDown[user_key_index]. note that imgui doesn't know the semantic of each entry of io.KeyDown[]. Use your own indices/enums according to how your backend/engine stored them into KeyDown[]!

//IMGUI_API bool          IsKeyReleased(int user_key_index);                                  // was key released (went from Down to !Down)..
//IMGUI_API bool          IsMouseDown(int button);                                            // is mouse button held

/** did mouse button clicked (went from !Down to Down)  */
fun isMouseClicked(button: Int, repeat: Boolean = false): Boolean {

    assert(button >= 0 && button < IO.mouseDown.size)
    val t = IO.mouseDownDuration[button]
    if (t == 0f)
        return true

    if (repeat && t > IO.keyRepeatDelay) {
        val delay = IO.keyRepeatDelay
        val rate = IO.keyRepeatRate
        if ((glm.mod(t - delay, rate) > rate * 0.5f) != (glm.mod(t - delay - IO.deltaTime, rate) > rate * 0.5f))
            return true
    }
    return false
}
//IMGUI_API bool          IsMouseDoubleClicked(int button);                                   // did mouse button double-clicked. a double-click returns false in IsMouseClicked(). uses io.MouseDoubleClickTime.
//IMGUI_API bool          IsMouseReleased(int button);                                        // did mouse button released (went from Down to !Down)
//IMGUI_API bool          IsMouseHoveringWindow();                                            // is mouse hovering current window ("window" in API names always refer to current window). disregarding of any consideration of being blocked by a popup. (unlike IsWindowHovered() this will return true even if the window is blocked because of a popup)
//IMGUI_API bool          IsMouseHoveringAnyWindow();                                         // is mouse hovering any visible window

/** Test if mouse cursor is hovering given rectangle
 *  NB- Rectangle is clipped by our current clip setting
 *  NB- Expand the rectangle to be generous on imprecise inputs systems (g.Style.TouchExtraPadding)
 *  is mouse hovering given bounding rect (in screen space). clipped by current clipping settings. disregarding of
 *  consideration of focus/window ordering/blocked by a popup.  */
fun isMouseHoveringRect(r: Rect, clip: Boolean = true) = isMouseHoveringRect(r.min, r.max, clip)

fun isMouseHoveringRect(rMin: Vec2, rMax: Vec2, clip: Boolean = true): Boolean {

    val window = currentWindowRead!!

    // Clip
    val rectClipped = Rect(rMin, rMax)
    if (clip)
        rectClipped.clip(window.clipRect)

    // Expand for touch input
    val rectForTouch = Rect(rectClipped.min - Style.touchExtraPadding, rectClipped.max + Style.touchExtraPadding)
    return rectForTouch.contains(IO.mousePos)
}
//IMGUI_API bool          IsMouseDragging(int button = 0, float lock_threshold = -1.0f);      // is mouse dragging. if lock_threshold < -1.0f uses io.MouseDraggingThreshold
//IMGUI_API ImVec2        GetMousePos();                                                      // shortcut to ImGui::GetIO().MousePos provided by user, to be consistent with other calls
//IMGUI_API ImVec2        GetMousePosOnOpeningCurrentPopup();                                 // retrieve backup of mouse positioning at the time of opening popup we have BeginPopup() into

/** dragging amount since clicking. if lockThreshold < -1.0f uses io.MouseDraggingThreshold    */
fun getMouseDragDelta(button: Int = 0, lockThreshold: Float = -1f): Vec2 {

    assert(button >= 0 && button < IO.mouseDown.size)
    var lockThreshold = lockThreshold
    if (lockThreshold < 0f)
        lockThreshold = IO.mouseDragThreshold
    if (IO.mouseDown[button])
        if (IO.mouseDragMaxDistanceSqr[button] >= lockThreshold * lockThreshold)
            return IO.mousePos - IO.mouseClickedPos[button] // Assume we can only get active with left-mouse button (at the moment).
    return Vec2()
}
//IMGUI_API void          ResetMouseDragDelta(int button = 0);                                //
//IMGUI_API ImGuiMouseCursor GetMouseCursor();                                                // get desired cursor type, reset in ImGui::NewFrame(), this updated during the frame. valid before Render(). If you use software rendering by setting io.MouseDrawCursor ImGui will render those for you
//IMGUI_API void          SetMouseCursor(ImGuiMouseCursor type);                              // set desired cursor type
//IMGUI_API void          CaptureKeyboardFromApp(bool capture = true);                        // manually override io.WantCaptureKeyboard flag next frame (said flag is entirely left for your application handle). e.g. force capture keyboard when your widget is being hovered.
//IMGUI_API void          CaptureMouseFromApp(bool capture = true);                           // manually override io.WantCaptureMouse flag next frame (said flag is entirely left for your application handle).