package imgui.internal.api

import imgui.api.g

/** Garbage collection */
interface garbageCollection {

    fun gcCompactTransientMiscBuffers() {
        g.itemFlagsStack.clear()
        g.groupStack.clear()
    }

    // -> Window class
//    IMGUI_API void          GcCompactTransientWindowBuffers(ImGuiWindow* window);
//    IMGUI_API void          GcAwakeTransientWindowBuffers(ImGuiWindow* window);
}