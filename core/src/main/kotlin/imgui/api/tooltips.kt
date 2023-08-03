package imgui.api

import imgui.HoveredFlag
import imgui.ImGui.beginTooltipEx
import imgui.ImGui.currentWindowRead
import imgui.ImGui.end
import imgui.ImGui.isItemHovered
import imgui.ImGui.text
import imgui.has
import imgui.internal.sections.TooltipFlag
import imgui.none
import imgui.WindowFlag as Wf

// Tooltips
// - Tooltips are windows following the mouse. They do not take focus away.
// - A tooltip window can contain items of any types. SetTooltip() is a shortcut for the 'if (BeginTooltip()) { Text(...); EndTooltip(); }' idiom.
interface tooltips {

    /** begin/append a tooltip window. */
    fun beginTooltip(): Boolean = beginTooltipEx()

    /** only call EndTooltip() if BeginTooltip()/BeginItemTooltip() returns true! */
    fun endTooltip() {
        assert(currentWindowRead!!.flags has Wf._Tooltip) { "Mismatched BeginTooltip()/EndTooltip() calls" }
        end()
    }

    /** set a text-only tooltip. Often used after a ImGui::IsItemHovered() check. Override any previous call to SetTooltip(). */
    fun setTooltip(fmt: String, vararg args: Any) {
        if (!beginTooltipEx(TooltipFlag.OverridePrevious))
            return
        text(fmt, *args)
        endTooltip()
    }

    // Tooltips: helpers for showing a tooltip when hovering an item
    // - BeginItemTooltip() is a shortcut for the 'if (IsItemHovered(ImGuiHoveredFlags_Tooltip) && BeginTooltip())' idiom.
    // - SetItemTooltip() is a shortcut for the 'if (IsItemHovered(ImGuiHoveredFlags_Tooltip)) { SetTooltip(...); }' idiom.
    // - Where 'ImGuiHoveredFlags_Tooltip' itself is a shortcut to use 'style.HoverFlagsForTooltipMouse' or 'style.HoverFlagsForTooltipNav' depending on active input type. For mouse it defaults to 'ImGuiHoveredFlags_Stationary | ImGuiHoveredFlags_DelayShort'.

    /** begin/append a tooltip window if preceding item was hovered. */
    fun beginItemTooltip(): Boolean {
        if (!isItemHovered(HoveredFlag.ForTooltip))
            return false
        return beginTooltipEx(none, none)
    }

    /** Shortcut to use 'style.HoverFlagsForTooltipMouse' or 'style.HoverFlagsForTooltipNav'.
     *  Defaults to == ImGuiHoveredFlags_Stationary | ImGuiHoveredFlags_DelayShort when using the mouse.
     *
     *  set a text-only tooltip if preceeding item was hovered. override any previous call to SetTooltip().
     */
    fun setItemTooltip(fmt: String, vararg args: String) {
        if (isItemHovered(HoveredFlag.ForTooltip))
            setTooltip(fmt, *args)
    }
}