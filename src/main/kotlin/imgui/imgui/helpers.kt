package imgui.imgui

import imgui.ImGui.io

/** Helpers functions to access functions pointers in ImGui::GetIO()    */
interface imgui_helpers {

    //    IMGUI_API void*         MemAlloc(size_t sz);
//    IMGUI_API void          MemFree(void* ptr);
//    IMGUI_API const char*   GetClipboardText();
    fun setClipboardText(text: String) = io.setClipboardTextFn?.invoke(io.clipboardUserData, text)
}