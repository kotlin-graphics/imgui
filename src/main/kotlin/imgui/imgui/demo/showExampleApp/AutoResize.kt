package imgui.imgui.demo.showExampleApp

import imgui.*
import imgui.ImGui.begin_
import imgui.ImGui.end
import imgui.ImGui.sliderInt
import imgui.ImGui.text
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object AutoResize {

    val lines = intArrayOf(10)

    /** Demonstrate creating a window which gets auto-resized according to its content. */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        if (!begin_("Example: Auto-resizing window", open, Wf.AlwaysAutoResize.i)) {
            end()
            return
        }

        text("Window will resize every-frame to the size of its content.\nNote that you probably don't want to " +
                "query the window size to\noutput your content because that would create a feedback loop.")
        sliderInt("Number of lines", lines, 0, 1, 20)
        for (i in 0 until lines[0])
            text(" ".repeat(i * 4) + "This is line $i") // Pad with space to extend size horizontally
        end()
    }
}