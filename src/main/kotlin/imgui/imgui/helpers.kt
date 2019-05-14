package imgui.imgui

import imgui.ImGui.io

/** Clipboard Utilities (also see the LogToClipboard() function to capture or output text data to the clipboard)    */
interface imgui_clipboardUtilities {

    var clipboardText: String
        /** ~GetClipboardText */
        get() = io.getClipboardTextFn?.invoke() ?: ""
        /** ~SetClipboardText */
        set(value) {
            io.setClipboardTextFn?.invoke(value)
        }
}