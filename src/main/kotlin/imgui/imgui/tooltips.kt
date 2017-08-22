package imgui.imgui

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.F32_TO_INT8_SAT
import imgui.ImGui.begin
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endTooltip
import imgui.ImGui.findWindowByName
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textV
import imgui.Context as g

/** Tooltips    */
interface imgui_tooltips {

    companion object {

        /** Not exposed publicly as BeginTooltip() because bool parameters are evil. Let's see if other needs arise first.  */
        fun beginTooltipEx(overridePreviousTooltip: Boolean) {

            var windowName = "##Tooltip%02d".format(style.locale, g.tooltipOverrideCount)
            if (overridePreviousTooltip)
                findWindowByName(windowName)?.let {
                    if (it.active) {
                        // Hide previous tooltips. We can't easily "reset" the content of a window so we create a new one.
                        it.hiddenFrames = 1
                        windowName = "##Tooltip%02d".format(++g.tooltipOverrideCount)
                    }
                }
            begin(windowName, null, WindowFlags.Tooltip or WindowFlags.NoTitleBar or WindowFlags.NoMove or
                    WindowFlags.NoResize or WindowFlags.NoSavedSettings or WindowFlags.AlwaysAutoResize)
        }

        fun colorTooltip(col: FloatArray, flags: Int) {

            val cr = F32_TO_INT8_SAT(col[0])
            val cg = F32_TO_INT8_SAT(col[1])
            val cb = F32_TO_INT8_SAT(col[2])
            val ca = if (flags has ColorEditFlags.Alpha) F32_TO_INT8_SAT(col[3]) else 255
            beginTooltipEx(true)
            val window = currentWindow
            val sz = Vec2(g.fontSize * 3)
            window.drawList.addRectFilled(window.dc.cursorPos, window.dc.cursorPos + sz, COL32(cr, cg, cb, 255))
            dummy(sz)
            sameLine()
            if (flags has ColorEditFlags.Alpha)
                text("#%02X%02X%02X%02X\nR:$cr, G:$cg, B:$cb, A:$ca\n(%.3f, %.3f, %.3f, %.3f)", cr, cg, cb, ca, col[0], col[1], col[2], col[3])
            else
                text("#%02X%02X%02X\nR: $cr, G: $cg, B: $cb\n(%.3f, %.3f, %.3f)", cr, cg, cb, col[0], col[1], col[2])
            endTooltip()
        }
    }

    fun setTooltipV(fmt: String, args: Array<out Any>) {
        beginTooltipEx(true)
        textV(fmt, args)
        endTooltip()
    }

    fun setTooltip(fmt: String, vararg args: Any) = setTooltipV(fmt, args)

    /** begin/append a tooltip window. to create full-featured tooltip (with any kind of contents). */
    fun beginTooltip() = beginTooltipEx(false)

    fun endTooltip() {
        assert(currentWindowRead!!.flags has WindowFlags.Tooltip)   // Mismatched BeginTooltip()/EndTooltip() calls
        end()
    }
}