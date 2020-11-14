package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginPopup
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endPopup
import imgui.ImGui.logToClipboard
import imgui.ImGui.openPopup
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxY
import imgui.ImGui.scrollY
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.smallButton
import imgui.ImGui.textEx
import imgui.StyleVar
import imgui.api.g
import imgui.classes.ListClipper
import imgui.classes.TextFilter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

object Log {

    val log = ExampleAppLog()
    var counter = 0
    val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
    val random = Random()
    val rand get() = abs(random.nextInt() / 100_000)

    /** Demonstrate creating a simple log window with basic filtering.  */
    operator fun invoke(pOpen: KMutableProperty0<Boolean>) {

        // For the demo: add a debug button _BEFORE_ the normal log window contents
        // We take advantage of a rarely used feature: multiple calls to Begin()/End() are appending to the _same_ window.
        setNextWindowSize(Vec2(500, 400), Cond.FirstUseEver)
        begin("Example: Log", pOpen)
        if (smallButton("[Debug] Add 5 entries")) {
            val categories = arrayOf("info", "warn", "error")
            val words = arrayOf("Bumfuzzled", "Cattywampus", "Snickersnee", "Abibliophobia", "Absquatulate", "Nincompoop", "Pauciloquent")
            for (n in 0..4) {
                val category = categories[counter % categories.size]
                val word = words[counter % words.size]
                log.addLog("[%05d] [$category] Hello, current time is %.1f, here's a word: '$word'\n", g.frameCount, g.time)
                counter++
            }
        }
        end()

        // Actually call in the regular Log helper (which will Begin() into the same window as we just did)
        log.draw("Example: Log", pOpen)
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()

        /** Index to lines offset. We maintain this with AddLog() calls. */
        val lineOffsets = ArrayList<Int>()

        /** Keep scrolling if already at the bottom. */
        var autoScroll = true

        init {
            clear()
        }

        fun addLog(fmt: String, vararg extra: Any) {
            val oldSize = buf.length
            buf.append(fmt.format(*extra))

            for (i in oldSize until buf.length) {
                if (buf[i] == '\n')
                    lineOffsets += i + 1
            }
        }

        fun draw(title: String, open: KMutableProperty0<Boolean>? = null) {

            if (!begin(title, open)) {
                end()
                return
            }

            // Options menu
            if (beginPopup("Options")) {
                checkbox("Auto-scroll", ::autoScroll)
                endPopup()
            }

            // Main window
            if (button("Options"))
                openPopup("Options")
            sameLine()
            val clear = button("Clear")
            sameLine()
            val copy = button("Copy")
            sameLine()
            filter.draw("Filter", -100f)

            separator()
            beginChild("scrolling", Vec2(0, 0), false, Wf.HorizontalScrollbar.i)

            if (clear) clear()
            if (copy) logToClipboard()

            pushStyleVar(StyleVar.ItemSpacing, Vec2(0))

            if (filter.isActive())
            // In this example we don't use the clipper when Filter is enabled.
            // This is because we don't have a random access on the result on our filter.
            // A real application processing logs with ten of thousands of entries may want to store the result of
            // search/filter.. especially if the filtering function is not trivial (e.g. reg-exp).
                for (line_no in 0 until lineOffsets.size) {
                    val line = buf.subSequence(lineOffsets[line_no], if (line_no + 1 < lineOffsets.size) lineOffsets[line_no + 1] - 1 else buf.length).toString()
                    if (filter.passFilter(line))
                        textEx(line)
                }
            else {
                // The simplest and easy way to display the entire buffer:
                //   ImGui::TextUnformatted(buf_begin, buf_end);
                // And it'll just work. TextUnformatted() has specialization for large blob of text and will fast-forward
                // to skip non-visible lines. Here we instead demonstrate using the clipper to only process lines that are
                // within the visible area.
                // If you have tens of thousands of items and their processing cost is non-negligible, coarse clipping them
                // on your side is recommended. Using ImGuiListClipper requires
                // - A) random access into your data
                // - B) items all being the  same height,
                // both of which we can handle since we an array pointing to the beginning of each line of text.
                // When using the filter (in the block of code above) we don't have random access into the data to display
                // anymore, which is why we don't use the clipper. Storing or skimming through the search result would make
                // it possible (and would be recommended if you want to search through tens of thousands of entries).
                val clipper = ListClipper()
                while (clipper.step())
                    for (lineNo in clipper.displayStart until clipper.displayEnd) {
                        val lineStart = lineOffsets[lineNo]
                        val lineEnd = if (lineNo + 1 < lineOffsets.size) lineOffsets[lineNo + 1] - 1 else buf.length
                        textEx(buf.subSequence(lineStart, lineEnd).toString())
                    }
                clipper.end()
            }

            popStyleVar()

            if (autoScroll && scrollY >= scrollMaxY)
                setScrollHereY(1f)

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