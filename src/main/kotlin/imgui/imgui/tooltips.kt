package imgui.imgui

import imgui.*
import imgui.ImGui.beginTooltipEx
import imgui.ImGui.currentWindowRead
import imgui.ImGui.end
import imgui.ImGui.textV
import imgui.WindowFlag as Wf

/** Tooltips    */
interface imgui_tooltips {

    /** begin/append a tooltip window. to create full-featured tooltip (with any kind of items). */
    fun beginTooltip() = beginTooltipEx(0, false)

    fun endTooltip() {
        assert(currentWindowRead!!.flags has Wf.Tooltip) { "Mismatched BeginTooltip()/EndTooltip() calls" }
        end()
    }

    /** set a text-only tooltip, typically use with ImGui::IsItemHovered(). overidde any previous call to SetTooltip(). */
    fun setTooltipV(fmt: String, args: Array<out Any>) {
        beginTooltipEx(0, true)
        textV(fmt, args)
        endTooltip()
    }

    fun setTooltip(fmt: String, vararg args: Any) = setTooltipV(fmt, args)
}