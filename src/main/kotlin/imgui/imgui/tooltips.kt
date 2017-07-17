package imgui.imgui

import imgui.ImGui
import imgui.WindowFlags
import imgui.or
import imgui.ImGui.getCurrentWindowRead
import imgui.has

/** Tooltips    */
interface imgui_tooltips {

//    IMGUI_API void          SetTooltip(const char* fmt, ...) IM_PRINTFARGS(1);                  // set tooltip under mouse-cursor, typically use with ImGui::IsHovered(). last call wins
//    IMGUI_API void          SetTooltipV(const char* fmt, va_list args);

    /** use to create full-featured tooltip windows that aren't just text   */
    fun beginTooltip()    {
        val flags = WindowFlags.Tooltip or WindowFlags.NoTitleBar or WindowFlags.NoMove or WindowFlags.NoResize or
                WindowFlags.NoSavedSettings or WindowFlags.AlwaysAutoResize
        ImGui.begin("##Tooltip", null, flags)
    }

    fun endTooltip() {
        assert(getCurrentWindowRead()!!.flags has WindowFlags.Tooltip)   // Mismatched BeginTooltip()/EndTooltip() calls
        ImGui.end()
    }
}