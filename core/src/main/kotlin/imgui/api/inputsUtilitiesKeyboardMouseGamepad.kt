package imgui.api

import glm_.i
import imgui.ImGui
import imgui.ImGui.data
import imgui.ImGui.isDown
import imgui.ImGui.isPressed
import imgui.ImGui.isReleased
import imgui.Key
import imgui.internal.classes.InputFlag
import imgui.internal.sections.KeyOwner_Any


// Inputs Utilities: Keyboard/Mouse/Gamepad
// - the ImGuiKey enum contains all possible keyboard, mouse and gamepad inputs (e.g. ImGuiKey_A, ImGuiKey_MouseLeft, ImGuiKey_GamepadDpadUp...).
// - before v1.87, we used ImGuiKey to carry native/user indices as defined by each backends. About use of those legacy ImGuiKey values:
//  - without IMGUI_DISABLE_OBSOLETE_KEYIO (legacy support): you can still use your legacy native/user indices (< 512) according to how your backend/engine stored them in io.KeysDown[], but need to cast them to ImGuiKey.
//  - with    IMGUI_DISABLE_OBSOLETE_KEYIO (this is the way forward): any use of ImGuiKey will assert with key < 512. GetKeyIndex() is pass-through and therefore deprecated (gone if IMGUI_DISABLE_OBSOLETE_KEYIO is defined).
interface inputsUtilitiesKeyboardMouseGamepad {

    /** ~IsKeyDown
     *
     *  is key being held.
     *
     *  Note that Dear ImGui doesn't know the meaning/semantic of ImGuiKey from 0..511: they are legacy native keycodes.
     *  Consider transitioning from 'IsKeyDown(MY_ENGINE_KEY_A)' (<1.87) to IsKeyDown(A) (>= 1.87) */
    val Key.isDown: Boolean
        get() = isDown(KeyOwner_Any)

    /** [JVM] ~IsKeyPressed
     *
     *  uses user's key indices as stored in the keys_down[] array. if repeat=true.
     *  uses io.KeyRepeatDelay / KeyRepeatRate
     *
     *  was key pressed (went from !Down to Down)? if repeat=true, uses io.KeyRepeatDelay / KeyRepeatRate */
    infix fun Key.isPressed(repeat: Boolean): Boolean = isPressed(KeyOwner_Any, if (repeat) InputFlag.Repeat.i else InputFlag.None.i)

    /** ~IsKeyPressed() */
    val Key.isPressed: Boolean
        get() = isPressed(true)

    /** ~IsKeyReleased
     *
     *  was key released (went from Down to !Down)?    */
    val Key.isReleased: Boolean
        get() = isReleased(KeyOwner_Any)

    /** ~getKeyPressedAmount
     *
     *  Uses provided repeat rate/delay. return a count, most often 0 or 1 but might be >1 if RepeatRate is small enough
     *  that DeltaTime > RepeatRate
     *
     *  Return value representing the number of presses in the last time period, for the given repeat rate
     *  (most often returns 0 or 1. The result is generally only >1 when RepeatRate is smaller than DeltaTime, aka large DeltaTime or fast RepeatRate) */
    fun Key.getPressedAmount(repeatDelay: Float, repeatRate: Float): Int {
        if (!data.down) // In theory this should already be encoded as (DownDuration < 0.0f), but testing this facilitates eating mechanism (until we finish work on key ownership)
            return 0
        val t = ImGui.io.keysData[i].downDuration
        return ImGui.calcTypematicRepeatAmount(t - ImGui.io.deltaTime, t, repeatDelay, repeatRate)
    }

    /** Override io.WantCaptureKeyboard flag next frame (said flag is left for your application to handle, typically when true it instructs your app to ignore inputs). e.g. force capture keyboard when your widget is being hovered. This is equivalent to setting "io.WantCaptureKeyboard = want_capture_keyboard"; after the next NewFrame() call.  */
    fun setNextFrameWantCaptureKeyboard(wantCaptureKeyboard: Boolean = true) {
        g.wantCaptureKeyboardNextFrame = wantCaptureKeyboard.i
    }
}