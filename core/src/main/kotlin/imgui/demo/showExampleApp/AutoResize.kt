package imgui.demo.showExampleApp

import imgui.ImGui.begin
import imgui.ImGui.end
import imgui.ImGui.text
import imgui.api.slider
import imgui.mutablePropertyAt
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

object AutoResize {

    val lines = intArrayOf(10)

    /** Demonstrate creating a window which gets auto-resized according to its content. */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        if (!begin("Example: Auto-resizing window", open, Wf.AlwaysAutoResize)) {
            end()
            return
        }

        text("""
            Window will resize every-frame to the size of its content.
            Note that you probably don't want to query the window size to
            output your content because that would create a feedback loop.""".trimIndent())
        slider("Number of lines", lines mutablePropertyAt 0, 1, 20)
        for (i in 0 until lines[0])
            text(" ".repeat(i * 4) + "This is line $i") // Pad with space to extend size horizontally
        end()
    }
}