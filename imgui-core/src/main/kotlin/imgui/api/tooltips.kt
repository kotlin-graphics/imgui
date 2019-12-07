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
import imgui.WindowFlag as Wf

/** Tooltips
 *  - Tooltip are windows following the mouse which do not take focus away. */
interface tooltips {

    /** begin/append a tooltip window. to create full-featured tooltip (with any kind of items). */
    fun beginTooltip() = when {
        g.dragDropWithinSourceOrTarget -> {
            // The default tooltip position is a little offset to give space to see the context menu (it's also clamped within the current viewport/monitor)
            // In the context of a dragging tooltip we try to reduce that offset and we enforce following the cursor.
            // Whatever we do we want to call SetNextWindowPos() to enforce a tooltip position and disable clipping the tooltip without our display area, like regular tooltip do.
            //ImVec2 tooltip_pos = g.IO.MousePos - g.ActiveIdClickOffset - g.Style.WindowPadding;
            val tooltipPos = io.mousePos + Vec2(16 * style.mouseCursorScale, 8 * style.mouseCursorScale)
            setNextWindowPos(tooltipPos)
            setNextWindowBgAlpha(style.colors[Col.PopupBg].w * 0.6f)
            //PushStyleVar(ImGuiStyleVar_Alpha, g.Style.Alpha * 0.60f); // This would be nice but e.g ColorButton with checkboard has issue with transparent colors :(
            beginTooltipEx(0, true)
        }
        else -> beginTooltipEx(0, false)
    }

    fun endTooltip() {
        assert(currentWindowRead!!.flags has Wf._Tooltip) { "Mismatched BeginTooltip()/EndTooltip() calls" }
        end()
    }

    /** set a text-only tooltip, typically use with ImGui::IsItemHovered(). override any previous call to SetTooltip(). */
    fun setTooltipV(fmt: String, args: Array<out Any>) {
        if (g.dragDropWithinSourceOrTarget)
            beginTooltip()
        else
            beginTooltipEx(0, true)
        textV(fmt, args)
        endTooltip()
    }

    fun setTooltip(fmt: String, vararg args: Any) = setTooltipV(fmt, args)
}