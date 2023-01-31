package imgui.api

import glm_.i


// Without IMGUI_DISABLE_OBSOLETE_KEYIO:
//   - For 'ImGuiKey key' you can use your own indices/enums according to how your backend/engine stored them in io.KeysDown[].
//   - We don't know the meaning of those value. You can use GetKeyIndex() to map a ImGuiKey_ value into the user index.
// With: IMGUI_DISABLE_OBSOLETE_KEYIO:
//   - `ImGuiKey key` will assert when key < 512 will be passed, previously reserved as user keys indices
//   - GetKeyIndex() is pass-through and therefore deprecated (gone if IMGUI_DISABLE_OBSOLETE_KEYIO is defined)
interface inputUtilitiesKeyboard {

    /** Override io.WantCaptureKeyboard flag next frame (said flag is left for your application to handle, typically when true it instructs your app to ignore inputs). e.g. force capture keyboard when your widget is being hovered. This is equivalent to setting "io.WantCaptureKeyboard = want_capture_keyboard"; after the next NewFrame() call.  */
    fun setNextFrameWantCaptureKeyboard(wantCaptureKeyboard: Boolean = true) {
        g.wantCaptureKeyboardNextFrame = wantCaptureKeyboard.i
    }
}