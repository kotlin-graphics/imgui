package imgui.imgui

import imgui.ImGui.io

/** Helpers functions to access functions pointers in ImGui::GetIO()    */
interface imgui_helpers {

    //    IMGUI_API void*         MemAlloc(size_t sz);
//    IMGUI_API void          MemFree(void* ptr);
    var clipboardText: String
        get() = io.getClipboardTextFn?.invoke() ?: ""
        set(value) {
            io.setClipboardTextFn?.invoke(value)
        }
}