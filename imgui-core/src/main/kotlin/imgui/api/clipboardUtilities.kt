package imgui.api

import imgui.ImGui.io

/** Clipboard Utilities (also see the LogToClipboard() function to capture or output text data to the clipboard)    */
interface clipboardUtilities {

    var clipboardText: String
        /** ~GetClipboardText */
        get() = io.getClipboardTextFn?.invoke(io.clipboardUserData) ?: ""
        /** ~SetClipboardText */
        set(value) {
            io.setClipboardTextFn?.invoke(io.clipboardUserData, value)
        }
}