package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin_
import imgui.ImGui.beginChild
import imgui.ImGui.button
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.frameCount
import imgui.ImGui.io
import imgui.ImGui.logToClipboard
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.style
import imgui.ImGui.textUnformatted
import java.util.*
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object Log {

    val log = ExampleAppLog()
    var lastTime = -1.0
    val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
    val random = Random()
    val rand get() = abs(random.nextInt() / 100_000)

    /** Demonstrate creating a simple log window with basic filtering.  */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        // Demo: add random items (unless Ctrl is held)
        val time = g.time
        if (time - lastTime >= 0.2f && !io.keyCtrl) {
            val s = randomWords[rand % randomWords.size]
            val t = "%.1f".format(style.locale, time)
            log.addLog("[$s] Hello, time is $t, frame count is $frameCount\n")
            lastTime = time
        }
        log.draw("Example: Log (Filter not yet implemented)", open)
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()// TODO
        //        ImVector<int>       LineOffsets;        // Index to lines offset
        var scrollToBottom = false

        fun addLog(fmt: String) {
            buf.append(fmt)
            scrollToBottom = true
        }

        fun draw(title: String, open: KMutableProperty0<Boolean>? = null) {

            setNextWindowSize(Vec2(500, 400), Cond.FirstUseEver)
            if(!begin_(title, open)) {
                end()
                return
            }
            if (button("Clear")) clear()
            sameLine()
            val copy = button("Copy")
            sameLine()
            filter.draw("Filter", -100f)
            separator()
            beginChild("scrolling", Vec2(0, 0), false, Wf.HorizontalScrollbar.i)
            if (copy) logToClipboard()

//      TODO      if (Filter.IsActive())
//            {
//                const char* buf_begin = Buf.begin()
//                const char* line = buf_begin
//                for (int line_no = 0; line != NULL; line_no++)
//                {
//                    const char* line_end = (line_no < LineOffsets.Size) ? buf_begin + LineOffsets[line_no] : NULL
//                    if (Filter.PassFilter(line, line_end))
//                        ImGui::TextUnformatted(line, line_end)
//                    line = line_end && line_end[1] ? line_end + 1 : NULL
//                }
//            }
//            else
            textUnformatted(buf.toString())

            if (scrollToBottom) setScrollHereY(1f)
            scrollToBottom = false
            endChild()
            end()
        }

        fun clear() = buf.setLength(0)
    }
}