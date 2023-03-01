package imgui.api

import imgui.classes.Context


/** -----------------------------------------------------------------------------
 *      Context
 *  -----------------------------------------------------------------------------
 *
 *  Current context pointer. Implicitly used by all ImGui functions. Always assumed to be != null.
 *  ImGui::CreateContext() will automatically set this pointer if it is NULL. Change to a different context by calling ImGui::SetCurrentContext().
 *  1) Important: globals are not shared across DLL boundaries! If you use DLLs or any form of hot-reloading: you will need to call
 *      SetCurrentContext() (with the pointer you got from CreateContext) from each unique static/DLL boundary, and after each hot-reloading.
 *      In your debugger, add GImGui to your watch window and notice how its value changes depending on which location you are currently stepping into.
 *  2) Important: Dear ImGui functions are not thread-safe because of this pointer.
 *      If you want thread-safety to allow N threads to access N different contexts, you can:
 *      - Change this variable to use thread local storage so each thread can refer to a different context, in imconfig.h:
 *          struct ImGuiContext;
 *          extern thread_local ImGuiContext* MyImGuiTLS;
 *          #define GImGui MyImGuiTLS
 *      And then define MyImGuiTLS in one of your cpp file. Note that thread_local is a C++11 keyword, earlier C++ uses compiler-specific keyword.
 *     - Future development aim to make this context pointer explicit to all calls. Also read https://github.com/ocornut/imgui/issues/586
 *     - If you need a finite number of contexts, you may compile and use multiple instances of the ImGui code from different namespace.    */
val g: Context
    get() = gImGui

/** ~GetCurrentContext/SetCurrentContext */
lateinit var gImGui: Context
val gImGuiNullable: Context? get() = if(::gImGui.isInitialized) gImGui else null


