package imgui.imgui

import imgui.*
import imgui.ImGui.currentWindowRead
import java.util.*
import imgui.Context as g

/** Tooltips    */
interface imgui_tooltips {

    /** set tooltip under mouse-cursor, typically use with ImGui::IsHovered(). last call wins
     *
     * Tooltip is stored and turned into a BeginTooltip()/EndTooltip() sequence at the end of the frame.
     * Each call override previous value.*/
    fun setTooltip(fmt: String, vararg values: Any) {
        g.tooltip = fmt.format(Style.locale, *values)
    }

    /** use to create full-featured tooltip windows that aren't just text   */
    fun beginTooltip()    {
        val flags = WindowFlags.Tooltip or WindowFlags.NoTitleBar or WindowFlags.NoMove or WindowFlags.NoResize or
                WindowFlags.NoSavedSettings or WindowFlags.AlwaysAutoResize
        ImGui.begin("##Tooltip", null, flags)
    }

    fun endTooltip() {
        assert(currentWindowRead!!.flags has WindowFlags.Tooltip)   // Mismatched BeginTooltip()/EndTooltip() calls
        ImGui.end()
    }
}