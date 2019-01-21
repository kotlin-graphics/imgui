package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.begin_
import imgui.ImGui.button
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.logToClipboard
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.smallButton
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
//                log.addLog("[%05d] [%s] Hello, current time is %.1f, here's a word: '%s'\n",
//                        frameCount, categories[counter % IM_ARRAYSIZE(categories)], ImGui::GetTime(), words[counter % IM_ARRAYSIZE(words)]);
                counter++
            }
        end()

        log.draw("Example: Log (Filter not yet implemented)", pOpen)
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()// TODO
        /** Index to lines offset. We maintain this with AddLog() calls, allowing us to have a random access on lines */
        val lineOffsets = ArrayList<Int>()
        var scrollToBottom = false

        fun addLog(fmt: String) {
            buf.append(fmt)
//            for (int new_size = Buf.size(); old_size < new_size; old_size++)
//            if (Buf[old_size] == '\n')
//                LineOffsets.push_back(old_size);
//            LineOffsets.push_back(old_size + 1);
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
            filter.draw("Filter", -100f)
            separator()
            beginChild("scrolling", Vec2(0, 0), false, Wf.HorizontalScrollbar.i)
            if (copy)
                logToClipboard()

//            ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0, 0));
//            const char* buf = Buf.begin();
//            const char* buf_end = Buf.end();
//            if (Filter.IsActive())
//            {
//                for (int line_no = 0; line_no < LineOffsets.Size; line_no++)
//                {
//                    const char* line_start = buf + LineOffsets[line_no];
//                    const char* line_end = (line_no + 1 < LineOffsets.Size) ? (buf + LineOffsets[line_no + 1] - 1) : buf_end;
//                    if (Filter.PassFilter(line_start, line_end))
//                        ImGui::TextUnformatted(line_start, line_end);
//                }
//            }
//            else
//            {
//                // The simplest and easy way to display the entire buffer:
//                //   ImGui::TextUnformatted(buf_begin, buf_end);
//                // And it'll just work. TextUnformatted() has specialization for large blob of text and will fast-forward to skip non-visible lines.
//                // Here we instead demonstrate using the clipper to only process lines that are within the visible area.
//                // If you have tens of thousands of items and their processing cost is non-negligible, coarse clipping them on your side is recommended.
//                // Using ImGuiListClipper requires A) random access into your data, and B) items all being the  same height,
//                // both of which we can handle since we an array pointing to the beginning of each line of text.
//                // When using the filter (in the block of code above) we don't have random access into the data to display anymore, which is why we don't use the clipper.
//                // Storing or skimming through the search result would make it possible (and would be recommended if you want to search through tens of thousands of entries)
//                ImGuiListClipper clipper;
//                clipper.Begin(LineOffsets.Size);
//                while (clipper.Step())
//                {
//                    for (int line_no = clipper.DisplayStart; line_no < clipper.DisplayEnd; line_no++)
//                    {
//                        const char* line_start = buf + LineOffsets[line_no];
//                        const char* line_end = (line_no + 1 < LineOffsets.Size) ? (buf + LineOffsets[line_no + 1] - 1) : buf_end;
//                        ImGui::TextUnformatted(line_start, line_end);
//                    }
//                }
//                clipper.End();
//            }
//            ImGui::PopStyleVar();

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