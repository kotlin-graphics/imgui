package imgui.api

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginTooltipEx
import imgui.ImGui.currentWindowRead
import imgui.ImGui.end
import imgui.ImGui.io
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.style
import imgui.ImGui.textV
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
    fun setTooltipV(fmt: String, args: Array<out Any>) {
        beginTooltipEx(Wf.None.i, TooltipFlag.OverridePreviousTooltip.i)
        textV(fmt, args)
        endTooltip()
    }

    fun setTooltip(fmt: String, vararg args: Any) = setTooltipV(fmt, args)
}