package imgui.api

import imgui.ImGui.beginTooltipEx
import imgui.ImGui.currentWindowRead
import imgui.ImGui.end
import imgui.ImGui.text
import imgui.has
import imgui.internal.sections.TooltipFlag
import imgui.WindowFlag as Wf

/** Tooltips
 *  - Tooltips are windows following the mouse. They do not take focus away. */
interface tooltips {

    /** begin/append a tooltip window. to create full-featured tooltip (with any kind of items). */
    fun beginTooltip(): Boolean = beginTooltipEx()

    /** only call EndTooltip() if BeginTooltip() returns true! */
    fun endTooltip() {
        assert(currentWindowRead!!.flags has Wf._Tooltip) { "Mismatched BeginTooltip()/EndTooltip() calls" }
        end()
    }

    /** set a text-only tooltip, typically use with ImGui::IsItemHovered(). override any previous call to SetTooltip(). */
    fun setTooltip(fmt: String, vararg args: Any) {
        if (!beginTooltipEx(TooltipFlag.OverridePrevious))
            return
        text(fmt, *args)
        endTooltip()
    }
}