package imgui.api

import glm_.i
import imgui.ImGui.calcTypematicRepeatAmount
import imgui.ImGui.io
import imgui.Key
import imgui.static.getKeyDataIndexInternal


// Without IMGUI_DISABLE_OBSOLETE_KEYIO:
//   - For 'ImGuiKey key' you can use your own indices/enums according to how your backend/engine stored them in io.KeysDown[].
//   - We don't know the meaning of those value. You can use GetKeyIndex() to map a ImGuiKey_ value into the user index.
// With: IMGUI_DISABLE_OBSOLETE_KEYIO:
//   - `ImGuiKey key` will assert when key < 512 will be passed, previously reserved as user keys indices
//   - GetKeyIndex() is pass-through and therefore deprecated (gone if IMGUI_DISABLE_OBSOLETE_KEYIO is defined)
interface inputUtilitiesKeyboard {

    /** Uses provided repeat rate/delay. return a count, most often 0 or 1 but might be >1 if RepeatRate is small enough
     *  that DeltaTime > RepeatRate */
    fun getKeyPressedAmount(key: Int, repeatDelay: Float, repeatRate: Float): Int {
        assert(key >= Key.BEGIN && key < Key.END) { "Support for user key indices was dropped in favor of ImGuiKey. Please update backend & user code." }

        val keyIndex = getKeyDataIndexInternal(key)
        if (keyIndex < 0)
            return 0
        assert(keyIndex in 0 until io.keysData.size)
        val t = io.keysData[keyIndex].downDuration
        return calcTypematicRepeatAmount(t - io.deltaTime, t, repeatDelay, repeatRate)
    }

    /** Manually override io.wantCaptureKeyboard flag next frame (said flag is entirely left for your application to handle).
     *  e.g. force capture keyboard when your widget is being hovered.  */
    fun captureKeyboardFromApp(capture: Boolean = true) {
        g.wantCaptureKeyboardNextFrame = capture.i
    }
}