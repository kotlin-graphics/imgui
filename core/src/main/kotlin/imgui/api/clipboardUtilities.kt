package imgui.api

import imgui.ImGui.io
import imgui.classes.Context

/** Clipboard Utilities
 *  - Also see the LogToClipboard() function to capture GUI into clipboard, or easily output text data to the clipboard. */
interface clipboardUtilities {

    var clipboardText: String
        /** ~GetClipboardText */
        get() = io.getClipboardTextFn?.invoke(io.clipboardUserData as? Context) ?: ""
        /** ~SetClipboardText */
        set(value) {
            io.setClipboardTextFn?.invoke(io.clipboardUserData as? Context, value)
        }
}