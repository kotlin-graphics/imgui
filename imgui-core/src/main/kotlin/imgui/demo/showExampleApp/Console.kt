package imgui.demo.showExampleApp

import glm_.b
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginPopup
import imgui.ImGui.beginPopupContextWindow
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endPopup
import imgui.ImGui.frameHeightWithSpacing
import imgui.ImGui.inputText
import imgui.ImGui.logFinish
import imgui.ImGui.logToClipboard
import imgui.ImGui.menuItem
import imgui.ImGui.openPopup
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxY
import imgui.ImGui.scrollY
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.smallButton
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.classes.InputTextCallbackData
import imgui.classes.TextFilter
import imgui.dsl.popupContextItem
import imgui.internal.textStr
import uno.kotlin.getValue
import uno.kotlin.setValue
import java.util.*
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf

object Console {

    /** Demonstrating creating a simple console window, with scrolling, filtering, completion and history.
     *  For the console example, here we are using a more C++ like approach of declaring a class to hold the data and
     *  the functions.  */
    val console = ExampleAppConsole()

    operator fun invoke(open: KMutableProperty0<Boolean>) = console.draw("Example: Console", open)

    class ExampleAppConsole {
        val inputBuf = ByteArray(256)
        val items = ArrayList<String>()
        val commands = arrayListOf("HELP", "HISTORY", "CLEAR", "CLASSIFY") // "classify" is only here to provide an example of "C"+[tab] completing to "CL" and displaying matches.
        val history = ArrayList<String>()
        /** -1: new line, 0..History.Size-1 browsing history. */
        var historyPos = -1
        val filter = TextFilter()
        var autoScroll = true
        var scrollToBottom = false

        init {
            addLog("Welcome to Dear ImGui!")
        }

        fun clearLog() = items.clear()

        fun addLog(fmt: String, vararg args: Any) {
            items += fmt.format(*args)
        }

        fun draw(title: String, pOpen: KMutableProperty0<Boolean>) {

            var open by pOpen

            setNextWindowSize(Vec2(520, 600), Cond.FirstUseEver)
            if (!begin(title, pOpen)) {
                end()
                return
            }

            /*  As a specific feature guaranteed by the library, after calling begin() the last Item represent the title bar.
                So e.g. isItemHovered() will return true when hovering the title bar. */
            // Here we create a context menu only available from the title bar.
            popupContextItem { if (menuItem("Close Console")) open = false }

            textWrapped("This example implements a console with basic coloring, completion and history. A more elaborate implementation may want to store entries along with extra data such as timestamp, emitter, etc.");
            textWrapped("Enter 'HELP' for help, press TAB to use text completion.")

            if (smallButton("Add Dummy Text")) {
                addLog("%d some text", items.size); addLog("some more text"); addLog("display very important message here!"); }
            sameLine()
            if (smallButton("Add Dummy Error"))
                addLog("[error] something went wrong")
            sameLine()
            if (smallButton("Clear"))
                clearLog()
            sameLine()
            val copyToClipboard = smallButton("Copy")
            //var t = 0.0; if (ImGui.time - t > 0.02f) { t = ImGui.time; addLog("Spam %f", t); }

            separator()

            // Options menu
            if (beginPopup("Options")) {
                checkbox("Auto-scroll", ::autoScroll)
                endPopup()
            }

            // Options, Filter
            if (button("Options"))
                openPopup("Options")
            sameLine()
            filter.draw("Filter (\"incl,-excl\") (\"error\")", 180f)
            separator()

            val footerHeightToReserve = style.itemSpacing.y + frameHeightWithSpacing
            beginChild("ScrollingRegion", Vec2(0, -footerHeightToReserve), false, Wf.HorizontalScrollbar.i)

            if (beginPopupContextWindow()) {
                if (selectable("Clear"))
                    clearLog()
                endPopup()
            }

            // Display every line as a separate entry so we can change their color or add custom widgets. If you only want raw text you can use ImGui::TextUnformatted(log.begin(), log.end());
            // NB- if you have thousands of entries this approach may be too inefficient and may require user-side clipping to only process visible items.
            // You can seek and display only the lines that are visible using the ImGuiListClipper helper, if your elements are evenly spaced and you have cheap random access to the elements.
            // To use the clipper we could replace the 'for (int i = 0; i < Items.Size; i++)' loop with:
            //     ImGuiListClipper clipper(Items.Size);
            //     while (clipper.Step())
            //         for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
            // However, note that you can not use this code as is if a filter is active because it breaks the 'cheap random-access' property. We would need random-access on the post-filtered list.
            // A typical application wanting coarse clipping and filtering may want to pre-compute an array of indices that passed the filtering test, recomputing this array when user changes the filter,
            // and appending newly elements as they are inserted. This is left as a task to the user until we can manage to improve this example code!
            // If your items are of variable size you may want to implement code similar to what ImGuiListClipper does. Or split your data into fixed height items to allow random-seeking into your list.
            pushStyleVar(StyleVar.ItemSpacing, Vec2(4, 1)) // Tighten spacing
            if (copyToClipboard)
                logToClipboard()

            for (i in items) {
                if (!filter.passFilter(i))
                    continue

                // Normally you would store more information in your item (e.g. make Items[] an array of structure, store color/type etc.)
                var popColor = false
                if ("[error]" in i) {
                    pushStyleColor(Col.Text, Vec4(1f, 0.4f, 0.4f, 1f))
                    popColor = true
                } else if (i.startsWith("# ")) {
                    pushStyleColor(Col.Text, Vec4(1f, 0.8f, 0.6f, 1f))
                    popColor = true
                }
                textUnformatted(i)
                if (popColor)
                    popStyleColor()
            }
            if (copyToClipboard)
                logFinish()
            if (scrollToBottom || (autoScroll && scrollY >= scrollMaxY))
                setScrollHereY(1f)
            scrollToBottom = false

            popStyleVar()
            endChild()
            separator()

            var reclaimFocus = false
            if (inputText("Input", inputBuf, Itf.EnterReturnsTrue.i or Itf.CallbackCompletion.i or Itf.CallbackHistory.i, textEditCallbackStub, this)) {
                val s = inputBuf.cStr.trimEnd()
                if(s.isNotEmpty())
                    execCommand(s)
                reclaimFocus = true
            }

            setItemDefaultFocus()
            if (reclaimFocus)
                setKeyboardFocusHere(-1)

            end()
        }

        fun execCommand(cmdLine: String) {
            addLog("# $cmdLine\n")

            // Insert into history. First find match and delete it so it can be pushed to the back. This isn't trying to be smart or optimal.
            historyPos = -1
            history -= cmdLine
            history += cmdLine

            // Process command
            when (cmdLine.toUpperCase()) {
                "CLEAR" -> clearLog()
                "HELP" -> {
                    addLog("Commands:")
                    commands.forEach { addLog("- $it") }
                }
                "HISTORY" -> {
                    val first = history.size - 10
                    for (i in (if (first > 0) first else 0) until history.size)
                        addLog("%3d: ${history[i]}\n", i)
                }
                else -> addLog("Unknown command: '$cmdLine'\n")
            }

            // On command input, we scroll to bottom even if AutoScroll==false
            scrollToBottom = true
        }

        val textEditCallbackStub: InputTextCallback = { data: InputTextCallbackData ->
            (data.userData as ExampleAppConsole).inputTextCallback(data)
        }

        fun inputTextCallback(data: TextEditCallbackData): Boolean {
            when (data.eventFlag) {
                imgui.InputTextFlag.CallbackCompletion.i -> {
                    val wordEnd = data.cursorPos
                    var wordStart = wordEnd
                    while (wordStart > 0) {
                        val c = data.buf[wordStart]
                        if (c == ' '.b || c == '\t'.b || c == ','.b || c == ';'.b)
                            break
                        wordStart--
                    }

                    val word = data.buf.copyOfRange(wordStart, wordEnd).cStr
                    val candidates = ArrayList<String>()
                    for (c in commands) {
                        if (c.startsWith(word))
                            candidates.add(c)
                    }
                    when {
                        candidates.isEmpty() -> addLog("No match for \"%s\"!\n", word)
                        candidates.size == 1 -> {
                            data.deleteChars(wordStart, wordEnd)
                            data.insertChars(data.cursorPos, candidates[0])
                            data.insertChars(data.cursorPos, " ")
                        }
                        else -> {
                            var matchLen = wordEnd - wordStart
                            while (true) {
                                var c = 0.toChar()
                                var allCandidatesMatch = true

                                var i = 0
                                while ((i < candidates.size) and allCandidatesMatch) {
                                    if (i == 0)
                                        c = candidates[i][matchLen].toUpperCase()
                                    else if ((c == 0.toChar()) or (c != candidates[i][matchLen].toUpperCase()))
                                        allCandidatesMatch = false
                                    ++i
                                }
                                if (!allCandidatesMatch)
                                    break
                                matchLen++
                            }

                            if (matchLen > 0) {
                                data.deleteChars(wordStart, wordEnd - wordStart)
                                data.insertChars(data.cursorPos, candidates[0].toByteArray(), matchLen)
                            }
                        }
                    }
                }
                Itf.CallbackHistory.i -> {
                    val prevHistoryPos = historyPos
                    if (data.eventKey == Key.UpArrow) {
                        if (historyPos == -1)
                            historyPos = history.size - 1
                        else
                            --historyPos
                    } else if (data.eventKey == Key.DownArrow) {
                        if (historyPos != -1) {
                            if (++historyPos >= history.size)
                                --historyPos
                        }
                    }

                    // A better implementation would preserve the data on the current input line along with cursor position.
                    if (prevHistoryPos != historyPos) {
                        val historyStr = if (historyPos >= 0) history[historyPos] else ""
                        data.deleteChars(0, data.bufTextLen)
                        data.insertChars(0, historyStr)
                    }
                }
            }
            return false
        }
    }
}