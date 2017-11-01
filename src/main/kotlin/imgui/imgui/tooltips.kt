package imgui.imgui

import imgui.*
import imgui.ImGui.beginTooltipEx
import imgui.ImGui.currentWindowRead
import imgui.ImGui.end
import imgui.ImGui.textV
import imgui.Context as g
import imgui.WindowFlags as Wf

/** Tooltips    */
interface imgui_tooltips {

    fun setTooltipV(fmt: String, args: Array<out Any>) {
        beginTooltipEx(0,true)
        textV(fmt, args)
        endTooltip()
    }

    fun setTooltip(fmt: String, vararg args: Any) = setTooltipV(fmt, args)

    /** begin/append a tooltip window. to create full-featured tooltip (with any kind of contents). */
    fun beginTooltip() = beginTooltipEx(0, false)

    fun endTooltip() {
        assert(currentWindowRead!!.flags has Wf.Tooltip)   // Mismatched BeginTooltip()/EndTooltip() calls
        end()
    }
}