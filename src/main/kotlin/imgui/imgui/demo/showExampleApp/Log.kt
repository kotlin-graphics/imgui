package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.begin_
import imgui.ImGui.button
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.logToClipboard
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.smallButton
import imgui.ImGui.textUnformatted
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object Log {

    val log = ExampleAppLog()
    var counter = 0
    val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
    val random = Random()
    val rand get() = abs(random.nextInt() / 100_000)

    /** Demonstrate creating a simple log window with basic filtering.  */
    operator fun invoke(pOpen: KMutableProperty0<Boolean>) {

        // For the demo: add a debug button before the normal log window contents
        // We take advantage of the fact that multiple calls to Begin()/End() are appending to the same window.
        setNextWindowSize(Vec2(500, 400), Cond.FirstUseEver)
        begin_("Example: Log", pOpen)
        if (smallButton("Add 5 entries"))
            for (n in 0..4) {
                val categories = arrayOf("info", "warn", "error")
                val words = arrayOf("Bumfuzzled", "Cattywampus", "Snickersnee", "Abibliophobia", "Absquatulate", "Nincompoop", "Pauciloquent")
                log.addLog("[%05d] [%s] Hello, current time is %.1f, here's a word: '%s'\n",
                        g.frameCount, categories[counter % categories.size], g.time, words[counter % words.size])
                counter++
            }
        end()

        log.draw("Example: Log", pOpen)
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()
        /** Index to lines offset. We maintain this with AddLog() calls, allowing us to have a random access on lines */
        val lineOffsets = ArrayList<Int>()
        var scrollToBottom = false

        init {
            clear()
        }

        fun addLog(fmt: String, vararg extra: Any) {
            val oldSize = buf.length
            buf.append(fmt.format(*extra))

            for(i in oldSize until buf.length) {
                if(buf[i] == '\n')
                    lineOffsets.add(i + 1)
            }
            scrollToBottom = true
        }

        fun draw(title: String, open: KMutableProperty0<Boolean>? = null) {

            if (!begin_(title, open)) {
                end()
                return
            }
            if (button("Clear")) clear()
            sameLine()
            val copy = button("Copy")
            sameLine()
            if(filter.draw("Filter", -100f)) {
                println("Filter change")
                filter.filters.clear()
                val firstNull = filter.inputBuf.indexOf('\u0000')
                if(firstNull != 0)
                    filter.filters.add(String(filter.inputBuf.copyOfRange(0, firstNull)))
            }
            separator()
            beginChild("scrolling", Vec2(0, 0), false, Wf.HorizontalScrollbar.i)
            if (copy)
                logToClipboard()

            pushStyleVar(StyleVar.ItemSpacing, Vec2(0))

            if(filter.isActive()) {
                for(line_no in 0 until lineOffsets.size) {
                    val line = buf.subSequence(lineOffsets[line_no], if(line_no + 1 < lineOffsets.size) lineOffsets[line_no + 1] - 1 else buf.length).toString()
                    if(filter.passFilter(line))
                        textUnformatted(line)
                }
            } else {
                textUnformatted(buf.toString())
                //TODO: Fix and remove above line
                /*val clipper = ListClipper()
                clipper.begin(lineOffsets.size)
                while(clipper.step()) {
                    for(line_no in clipper.display.start until clipper.display.endInclusive - 1) {
                        textUnformatted(buf.subSequence(lineOffsets[line_no], if(line_no + 1 < lineOffsets.size) lineOffsets[line_no + 1] - 1 else buf.length).toString())
                    }
                }
                clipper.end()*/
            }

            popStyleVar()

            if (scrollToBottom) setScrollHereY(1f)
            scrollToBottom = false
            endChild()
            end()
        }

        fun clear() {
            buf.setLength(0)
            lineOffsets.clear()
            lineOffsets += 0
        }
    }
}