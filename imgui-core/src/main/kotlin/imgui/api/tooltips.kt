package imgui.api

import imgui.ImGui.beginTooltipEx
import imgui.ImGui.currentWindowRead
import imgui.ImGui.end
import imgui.ImGui.text
import imgui.has
import imgui.internal.TooltipFlag
import imgui.WindowFlag as Wf

/** Tooltips
 *  - Tooltip are windows following the mouse which do not take focus away. */
interface tooltips {

    /** begin/append a tooltip window. to create full-featured tooltip (with any kind of items). */
    fun beginTooltip() = beginTooltipEx(Wf.None.i, TooltipFlag.None.i)

    fun endTooltip() {
        assert(currentWindowRead!!.flags has Wf._Tooltip) { "Mismatched BeginTooltip()/EndTooltip() calls" }
        end()
    }

    /** set a text-only tooltip, typically use with ImGui::IsItemHovered(). override any previous call to SetTooltip(). */
    fun setTooltip(fmt: String, vararg args: Any) {
        beginTooltipEx(Wf.None.i, TooltipFlag.OverridePreviousTooltip.i)
        text(fmt, args)
        endTooltip()
    }
}