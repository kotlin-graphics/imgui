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


/** Inputs Utilities: Keyboard
 *  - For 'int user_key_index' you can use your own indices/enums according to how your backend/engine stored them in io.KeysDown[].
 *  - We don't know the meaning of those value. You can use GetKeyIndex() to map a ImGuiKey_ value into the user index. */
interface inputUtilitiesKeyboard {

    fun getKeyIndex(imguiKey: Int): Int = io.keyMap[imguiKey]

    /** is key being held. == io.KeysDown[user_key_index].  */
    fun isKeyDown(userKeyIndex: Int): Boolean = if (userKeyIndex < 0) false else io.keysDown[userKeyIndex]

    /** uses user's key indices as stored in the keys_down[] array. if repeat=true.
     *  uses io.KeyRepeatDelay / KeyRepeatRate
     *
     *  was key pressed (went from !Down to Down)? if repeat=true, uses io.KeyRepeatDelay / KeyRepeatRate   */
    fun isKeyPressed(userKeyIndex: Int, repeat: Boolean = true): Boolean = when {
        userKeyIndex < 0 -> false
        else -> {
            val t = io.keysDownDuration[userKeyIndex]
            when {
                t == 0f -> true
                repeat && t > io.keyRepeatDelay -> getKeyPressedAmount(userKeyIndex, io.keyRepeatDelay, io.keyRepeatRate) > 0
                else -> false
            }
        }
    }

    /** was key released (went from Down to !Down)?    */
    fun isKeyReleased(userKeyIndex: Int): Boolean = if (userKeyIndex < 0) false else io.keysDownDurationPrev[userKeyIndex] >= 0f && !io.keysDown[userKeyIndex]

    /** Uses provided repeat rate/delay. return a count, most often 0 or 1 but might be >1 if RepeatRate is small enough
     *  that DeltaTime > RepeatRate */
    fun getKeyPressedAmount(keyIndex: Int, repeatDelay: Float, repeatRate: Float): Int {
        if (keyIndex < 0) return 0
        assert(keyIndex in 0 until io.keysDown.size)
        val t = io.keysDownDuration[keyIndex]
        return calcTypematicRepeatAmount(t - io.deltaTime, t, repeatDelay, repeatRate)
    }

    /** Manually override io.wantCaptureKeyboard flag next frame (said flag is entirely left for your application to handle).
     *  e.g. force capture keyboard when your widget is being hovered.  */
    fun captureKeyboardFromApp(capture: Boolean = true) {
        g.wantCaptureKeyboardNextFrame = capture.i
    }
}