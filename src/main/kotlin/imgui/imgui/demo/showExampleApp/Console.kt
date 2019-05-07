package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginPopup
import imgui.ImGui.beginPopupContextWindow
import imgui.ImGui.begin_
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endPopup
import imgui.ImGui.getStyleColorVec4
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
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.textEx
import imgui.ImGui.textWrapped
import imgui.functionalProgramming.popupContextItem
import imgui.internal.textStr
import uno.kotlin.getValue
import uno.kotlin.setValue
import java.util.*
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object Console {

    /** Demonstrating creating a simple console window, with scrolling, filtering, completion and history.
     *  For the console example, here we are using a more C++ like approach of declaring a class to hold the data and
     *  the functions.  */
    val console = ExampleAppConsole()

    operator fun invoke(open: KMutableProperty0<Boolean>) = console.draw("Example: Console", open)

    class ExampleAppConsole {
        val inputBuf = CharArray(256)
        val items = ArrayList<String>()
        val commands = ArrayList<String>(Arrays.asList("HELP", "HISTORY", "CLEAR", "CLASSIFY"))
        val history = ArrayList<String>()
        /** -1: new line, 0..History.Size-1 browsing history. */
        var historyPos = -1
        val filter = TextFilter()
        var autoScroll = true
        var scrollToBottom = true

        init {
            addLog("Welcome to Dear ImGui!")
        }

        fun clearLog() {
            items.clear()
            scrollToBottom = true
        }

        fun addLog(fmt: String, vararg args: Any) {
            items.add(fmt.format(*args))
            if (autoScroll)
                scrollToBottom = true
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

            if (ImGui.smallButton("Add Dummy Text")) {
                addLog("%d some text", items.size); addLog("some more text"); addLog("display very important message here!"); }
            sameLine()
            if (ImGui.smallButton("Add Dummy Error")) {
                addLog("[error] something went wrong"); }
            sameLine()
            if (ImGui.smallButton("Clear")) {
                clearLog(); }
            sameLine()
            val copyToClipboard = ImGui.smallButton("Copy")
            sameLine()
            if (ImGui.smallButton("Scroll to bottom")) {
                scrollToBottom = true
            }
            //var t = 0.0; if (ImGui.time - t > 0.02f) { t = ImGui.time; addLog("Spam %f", t); }

            separator()

            // Options menu
            if (beginPopup("Options")) {
                if (checkbox("Auto-scroll", ::autoScroll))
                    if (autoScroll)
                        scrollToBottom = true
                endPopup()
            }

            // Options, Filter
            if (button("Options"))
                openPopup("Options")
            sameLine()
            filter.draw("Filter (\"incl,-excl\") (\"error\")", 180f)
            separator()

            val footerHeightToReserve = ImGui.style.itemSpacing.y + ImGui.frameHeightWithSpacing
            beginChild("ScrollingRegion", Vec2(0, -footerHeightToReserve), false, imgui.WindowFlag.HorizontalScrollbar.i)

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
            pushStyleVar(StyleVar.ItemSpacing, Vec2(4, 1))
            if (copyToClipboard)
                logToClipboard()

            val colDefaultText = getStyleColorVec4(Col.Text)
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
                textEx(i)
                if (popColor)
                    popStyleColor()
            }
            if (copyToClipboard)
                logFinish()
            if (scrollToBottom)
                setScrollHereY(1.0f)
            scrollToBottom = false
            popStyleVar()
            endChild()
            separator()

            var reclaimFocus = false
            if (inputText("Input", inputBuf, imgui.InputTextFlag.EnterReturnsTrue.i or imgui.InputTextFlag.CallbackCompletion.i or imgui.InputTextFlag.CallbackHistory.i, textEditCallbackStub, this)) {
                val slen = inputBuf.textStr(inputBuf)
                val s = String(inputBuf.copyOf(slen)).split(" ")[0]
                if (s.isNotEmpty())
                    execCommand(s)
                for (i in 0 until slen) {
                    inputBuf[i] = NUL
                }
                reclaimFocus = true
            }

            setItemDefaultFocus()
            if (reclaimFocus)
                setKeyboardFocusHere(-1)

            end()
        }

        fun execCommand(cmdLine: String) {
            addLog("# %s\n", cmdLine)

            historyPos = -1
            history.remove(cmdLine) //could be at any pos, we only want it to be last. so we remove the current instance
            history.add(cmdLine)

            when (cmdLine) {
                "CLEAR" -> clearLog()
                "HELP" -> {
                    addLog("Commands:")
                    commands.forEach { addLog("- %s", it) }
                }
                "HISTORY" -> {
                    val first = history.size - 10
                    for (i in (if (first > 0) first else 0) until history.size)
                        addLog("%3d: %s\n", i, history[i])
                }
                else -> addLog("Unknown command: '%s'\n", cmdLine)
            }

            // On commad input, we scroll to bottom even if AutoScroll==false
            scrollToBottom = true
        }

        val textEditCallbackStub = object : InputTextCallback {
            override fun invoke(p1: InputTextCallbackData): Int {
                return (p1.userData as ExampleAppConsole).inputTextCallback(p1)
            }
        }

        fun inputTextCallback(data: TextEditCallbackData): Int {
            when (data.eventFlag) {
                imgui.InputTextFlag.CallbackCompletion.i -> {
                    val wordEnd = data.cursorPos
                    var wordStart = wordEnd
                    while (wordStart > 0) {
                        val c = data.buf[wordStart]
                        if (c.isWhitespace() or (c == ';'))
                            break
                        wordStart--
                    }

                    val word = String(data.buf.copyOfRange(wordStart, wordEnd))
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
                                data.insertChars(data.cursorPos, candidates[0], matchLen)
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
            return 0
        }
    }
}