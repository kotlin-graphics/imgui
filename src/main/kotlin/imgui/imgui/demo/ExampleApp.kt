package imgui.imgui.demo

import gli_.has
import glm_.c
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui._begin
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginChild
import imgui.ImGui.beginGroup
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.colorEditVec4
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.image
import imgui.ImGui.inputFloat
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.itemsLineHeightWithSpacing
import imgui.ImGui.logButtons
import imgui.ImGui.logFinish
import imgui.ImGui.logToClipboard
import imgui.ImGui.menuItem
import imgui.ImGui.nextColumn
import imgui.ImGui.popFont
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.setScrollHere
import imgui.ImGui.setWindowFontScale
import imgui.ImGui.setWindowSize
import imgui.ImGui.showUserGuide
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderInt
import imgui.ImGui.sliderVec2
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.ImGui.time
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.version
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.functionalProgramming.button
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.mainMenuBar
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuBar
import imgui.functionalProgramming.menuItem
import imgui.functionalProgramming.popupContextWindow
import imgui.functionalProgramming.smallButton
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.window
import imgui.functionalProgramming.withId
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withTooltip
import imgui.functionalProgramming.withWindow
import imgui.imgui.imgui_demoDebugInfo.Companion.showHelpMarker
import imgui.imgui.imgui_demoDebugInfo.Companion.showExampleMenuFile
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlags as Cef
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

object ExampleApp {

    object show {
        var mainMenuBar = false
        var console = false
        var log = false
        var layout = false
        var propertyEditor = false
        var longText = false
        var autoResize = false
        var constrainedResize = false
        var fixedOverlay = false
        var manipulatingWindowTitle = false
        var customRendering = false
        var styleEditor = false

        var metrics = false
        var about = false
    }

    var noTitlebar = false
    var noBorder = true
    var noResize = false
    var noMove = false
    var noScrollbar = false
    var noCollapse = false
    var noMenu = false

    var filter = TextFilter()

    operator fun invoke(open: KMutableProperty0<Boolean>) {

        if (show.mainMenuBar) MainMenuBar()
        if (show.console) Console(show::console)
        if (show.log) Log(show::log)
        if (show.layout) Layout(show::layout)
        if (show.propertyEditor) PropertyEditor(show::propertyEditor)
        if (show.longText) LongText(show::longText)
        if (show.autoResize) AutoResize(show::autoResize)
        if (show.constrainedResize) ConstrainedResize(show::constrainedResize)
        if (show.fixedOverlay) FixedOverlay(show::fixedOverlay)
        if (show.manipulatingWindowTitle) ManipulatingWindowTitle(show::manipulatingWindowTitle)
        if (show.customRendering) CustomRendering(show::customRendering)
        if (show.metrics) ImGui.showMetricsWindow(show::metrics)
        if (show.styleEditor)
            withWindow("Style Editor", show::styleEditor) {
                StyleEditor()
            }

        if (show.about)
            withWindow("About ImGui", show::about, Wf.AlwaysAutoResize.i) {
                text("JVM ImGui, $version")
                separator()
                text("Original by Omar Cornut, ported by Giuseppe Barbieri and all github contributors.")
                text("ImGui is licensed under the MIT License, see LICENSE for more information.")
            }

        // Demonstrate the various window flags. Typically you would just use the default.
        var windowFlags = 0
        if (noTitlebar) windowFlags = windowFlags or Wf.NoTitleBar
        if (!noBorder) windowFlags = windowFlags or Wf.ShowBorders
        if (noResize) windowFlags = windowFlags or Wf.NoResize
        if (noMove) windowFlags = windowFlags or Wf.NoMove
        if (noScrollbar) windowFlags = windowFlags or Wf.NoScrollbar
        if (noCollapse) windowFlags = windowFlags or Wf.NoCollapse
        if (!noMenu) windowFlags = windowFlags or Wf.MenuBar
        setNextWindowSize(Vec2(550, 680), Cond.FirstUseEver)
        if (!_begin("ImGui Demo", open, windowFlags)) {
            end()   // Early out if the window is collapsed, as an optimization.
            return
        }

        //pushItemWidth(getWindowWidth() * 0.65f);    // 2/3 of the space for widget and 1/3 for labels
        pushItemWidth(-140) // Right align, keep 140 pixels for labels

        text("dear imgui says hello. ($version)")

        // Menu
        menuBar {
            menu("Menu") { showExampleMenuFile() }
            menu("Examples") {
                menuItem("Main menu bar", "", ExampleApp.show::mainMenuBar)
                menuItem("Console", "", ExampleApp.show::console)
                menuItem("Log", "", ExampleApp.show::log)
                menuItem("Simple layout", "", ExampleApp.show::layout)
                menuItem("Property editor", "", ExampleApp.show::propertyEditor)
                menuItem("Long text display", "", ExampleApp.show::longText)
                menuItem("Auto-resizing window", "", ExampleApp.show::autoResize)
                menuItem("Constrained-resizing window", "", ExampleApp.show::constrainedResize)
                menuItem("Simple overlay", "", ExampleApp.show::fixedOverlay)
                menuItem("Manipulating window title", "", ExampleApp.show::manipulatingWindowTitle)
                menuItem("Custom rendering", "", ExampleApp.show::customRendering)
            }
            menu("Help") {
                menuItem("Metrics", "", ExampleApp.show::metrics)
                menuItem("Style Editor", "", ExampleApp.show::styleEditor)
                menuItem("About ImGui", "", ExampleApp.show::about)
            }
        }

        spacing()
        collapsingHeader("Help") {
            textWrapped("This window is being created by the ShowTestWindow() function. Please refer to the code " +
                    "for programming reference.\n\nUser Guide:")
            showUserGuide()
        }

        collapsingHeader("Window options") {

            checkbox("No titlebar", ::noTitlebar); sameLine(150)
            checkbox("No border", ::noBorder); sameLine(300)
            checkbox("No resize", ::noResize)
            checkbox("No move", ::noMove); sameLine(150)
            checkbox("No scrollbar", ::noScrollbar); sameLine(300)
            checkbox("No collapse", ::noCollapse)
            checkbox("No menu", ::noMenu)

            treeNode("Style") { StyleEditor() }

            treeNode("Logging") {
                textWrapped("The logging API redirects all text output so you can easily capture the content of a " +
                        "window or a block. Tree nodes can be automatically expanded. You can also call LogText() to " +
                        "output directly to the log without a visual output.")
                logButtons()
            }
        }

        widgets()

        layout_()

        popupsAndModalWindows()

        columns_()

        collapsingHeader("Filtering TODO") {
//            ImGui::Text("Filter usage:\n"
//                    "  \"\"         display all lines\n"
//            "  \"xxx\"      display lines containing \"xxx\"\n"
//            "  \"xxx,yyy\"  display lines containing \"xxx\" or \"yyy\"\n"
//            "  \"-xxx\"     hide lines containing \"xxx\"");
//            filter.Draw();
//            const char* lines[] = { "aaa1.c", "bbb1.c", "ccc1.c", "aaa2.cpp", "bbb2.cpp", "ccc2.cpp", "abc.h", "hello, world" };
//            for (int i = 0; i < IM_ARRAYSIZE(lines); i++)
//            if (filter.PassFilter(lines[i]))
//                ImGui::BulletText("%s", lines[i]);
        }

        inputAndFocus()
    }
}

/** Demonstrate creating a fullscreen menu bar and populating it.   */
object MainMenuBar {
    operator fun invoke() = mainMenuBar {
        menu("File") { showExampleMenuFile() }
        menu("Edit") {
            menuItem("Undo", "CTRL+Z")
            menuItem("Redo", "CTRL+Y", false, false) // Disabled item
            separator()
            menuItem("Cut", "CTRL+X")
            menuItem("Copy", "CTRL+C")
            menuItem("Paste", "CTRL+V")
        }
    }
}

object Console {

    /** Demonstrating creating a simple console window, with scrolling, filtering, completion and history.
     *  For the console example, here we are using a more C++ like approach of declaring a class to hold the data and
     *  the functions.  */
    val console = ExampleAppConsole()

    operator fun invoke(open: KMutableProperty0<Boolean>) = console.draw("Example: Console", open)

    class ExampleAppConsole {
        //        char                  InputBuf[256];
//        ImVector<char*>       Items;
//        bool                  ScrollToBottom;
//        ImVector<char*>       History;
//        int                   HistoryPos;    // -1: new line, 0..History.Size-1 browsing history.
//        ImVector<const char*> Commands;
//
//        ExampleAppConsole()
//        {
//            ClearLog();
//            memset(InputBuf, 0, sizeof(InputBuf));
//            HistoryPos = -1;
//            Commands.push_back("HELP");
//            Commands.push_back("HISTORY");
//            Commands.push_back("CLEAR");
//            Commands.push_back("CLASSIFY");  // "classify" is here to provide an example of "C"+[tab] completing to "CL" and displaying matches.
//            AddLog("Welcome to ImGui!");
//        }
//        ~ExampleAppConsole()
//    {
//        ClearLog();
//        for (int i = 0; i < History.Size; i++)
//        free(History[i]);
//    }
//
//        // Portable helpers
//        static int   Stricmp(const char* str1, const char* str2)         { int d; while ((d = toupper(*str2) - toupper(*str1)) == 0 && *str1) { str1++; str2++; } return d; }
//        static int   Strnicmp(const char* str1, const char* str2, int n) { int d = 0; while (n > 0 && (d = toupper(*str2) - toupper(*str1)) == 0 && *str1) { str1++; str2++; n--; } return d; }
//        static char* Strdup(const char *str)                             { size_t len = strlen(str) + 1; void* buff = malloc(len); return (char*)memcpy(buff, (const void*)str, len); }
//
//        void    ClearLog()
//        {
//            for (int i = 0; i < Items.Size; i++)
//            free(Items[i]);
//            Items.clear();
//            ScrollToBottom = true;
//        }
//
//        void    AddLog(const char* fmt, ...) IM_PRINTFARGS(2)
//        {
//            char buf[1024];
//            va_list args;
//            va_start(args, fmt);
//            vsnprintf(buf, IM_ARRAYSIZE(buf), fmt, args);
//            buf[IM_ARRAYSIZE(buf)-1] = 0;
//            va_end(args);
//            Items.push_back(Strdup(buf));
//            ScrollToBottom = true;
//        }
//
        fun draw(title: String, open: KMutableProperty0<Boolean>) {

            setNextWindowSize(Vec2(520, 600), Cond.FirstUseEver)
            if (!_begin(title, open)) {
                end()
                return
            }

            textWrapped("This example is not yet implemented, you are welcome to contribute")
//            textWrapped("This example implements a console with basic coloring, completion and history. A more elaborate implementation may want to store entries along with extra data such as timestamp, emitter, etc.");
//            ImGui::TextWrapped("Enter 'HELP' for help, press TAB to use text completion.");
//
//            // TODO: display items starting from the bottom
//
//            if (ImGui::SmallButton("Add Dummy Text")) { AddLog("%d some text", Items.Size); AddLog("some more text"); AddLog("display very important message here!"); } ImGui::SameLine();
//            if (ImGui::SmallButton("Add Dummy Error")) { AddLog("[error] something went wrong"); } ImGui::SameLine();
//            if (ImGui::SmallButton("Clear")) { ClearLog(); } ImGui::SameLine();
//            bool copy_to_clipboard = ImGui::SmallButton("Copy"); ImGui::SameLine();
//            if (ImGui::SmallButton("Scroll to bottom")) ScrollToBottom = true;
//            //static float t = 0.0f; if (ImGui::GetTime() - t > 0.02f) { t = ImGui::GetTime(); AddLog("Spam %f", t); }
//
//            ImGui::Separator();
//
//            ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(0,0));
//            static ImGuiTextFilter filter;
//            filter.Draw("Filter (\"incl,-excl\") (\"error\")", 180);
//            ImGui::PopStyleVar();
//            ImGui::Separator();
//
//            ImGui::BeginChild("ScrollingRegion", ImVec2(0,-ImGui::GetItemsLineHeightWithSpacing()), false, ImGuiWindowFlags_HorizontalScrollbar);
//            if (ImGui::BeginPopupContextWindow())
//            {
//                if (ImGui::Selectable("Clear")) ClearLog();
//                ImGui::EndPopup();
//            }
//
//            // Display every line as a separate entry so we can change their color or add custom widgets. If you only want raw text you can use ImGui::TextUnformatted(log.begin(), log.end());
//            // NB- if you have thousands of entries this approach may be too inefficient and may require user-side clipping to only process visible items.
//            // You can seek and display only the lines that are visible using the ImGuiListClipper helper, if your elements are evenly spaced and you have cheap random access to the elements.
//            // To use the clipper we could replace the 'for (int i = 0; i < Items.Size; i++)' loop with:
//            //     ImGuiListClipper clipper(Items.Size);
//            //     while (clipper.Step())
//            //         for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
//            // However take note that you can not use this code as is if a filter is active because it breaks the 'cheap random-access' property. We would need random-access on the post-filtered list.
//            // A typical application wanting coarse clipping and filtering may want to pre-compute an array of indices that passed the filtering test, recomputing this array when user changes the filter,
//            // and appending newly elements as they are inserted. This is left as a task to the user until we can manage to improve this example code!
//            // If your items are of variable size you may want to implement code similar to what ImGuiListClipper does. Or split your data into fixed height items to allow random-seeking into your list.
//            ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(4,1)); // Tighten spacing
//            if (copy_to_clipboard)
//                ImGui::LogToClipboard();
//            for (int i = 0; i < Items.Size; i++)
//            {
//                const char* item = Items[i];
//                if (!filter.PassFilter(item))
//                    continue;
//                ImVec4 col = ImVec4(1.0f,1.0f,1.0f,1.0f); // A better implementation may store a type per-item. For the sample let's just parse the text.
//                if (strstr(item, "[error]")) col = ImColor(1.0f,0.4f,0.4f,1.0f);
//                else if (strncmp(item, "# ", 2) == 0) col = ImColor(1.0f,0.78f,0.58f,1.0f);
//                ImGui::PushStyleColor(ImGuiCol_Text, col);
//                ImGui::TextUnformatted(item);
//                ImGui::PopStyleColor();
//            }
//            if (copy_to_clipboard)
//                ImGui::LogFinish();
//            if (ScrollToBottom)
//                ImGui::SetScrollHere();
//            ScrollToBottom = false;
//            ImGui::PopStyleVar();
//            ImGui::EndChild();
//            ImGui::Separator();
//
//            // Command-line
//            if (ImGui::InputText("Input", InputBuf, IM_ARRAYSIZE(InputBuf), ImGuiInputTextFlags_EnterReturnsTrue|ImGuiInputTextFlags_CallbackCompletion|ImGuiInputTextFlags_CallbackHistory, &TextEditCallbackStub, (void*)this))
//            {
//                char* input_end = InputBuf+strlen(InputBuf);
//                while (input_end > InputBuf && input_end[-1] == ' ') { input_end--; } *input_end = 0;
//                if (InputBuf[0])
//                    ExecCommand(InputBuf);
//                strcpy(InputBuf, "");
//            }
//
//            // Demonstrate keeping auto focus on the input box
//            if (ImGui::IsItemHovered() || (ImGui::IsRootWindowOrAnyChildFocused() && !ImGui::IsAnyItemActive() && !ImGui::IsMouseClicked(0)))
//                ImGui::SetKeyboardFocusHere(-1); // Auto focus previous widget
//
//            ImGui::End();
        }
//
//        void    ExecCommand(const char* command_line)
//        {
//            AddLog("# %s\n", command_line);
//
//            // Insert into history. First find match and delete it so it can be pushed to the back. This isn't trying to be smart or optimal.
//            HistoryPos = -1;
//            for (int i = History.Size-1; i >= 0; i--)
//            if (Stricmp(History[i], command_line) == 0)
//            {
//                free(History[i]);
//                History.erase(History.begin() + i);
//                break;
//            }
//            History.push_back(Strdup(command_line));
//
//            // Process command
//            if (Stricmp(command_line, "CLEAR") == 0)
//            {
//                ClearLog();
//            }
//            else if (Stricmp(command_line, "HELP") == 0)
//            {
//                AddLog("Commands:");
//                for (int i = 0; i < Commands.Size; i++)
//                AddLog("- %s", Commands[i]);
//            }
//            else if (Stricmp(command_line, "HISTORY") == 0)
//            {
//                int first = History.Size - 10;
//                for (int i = first > 0 ? first : 0; i < History.Size; i++)
//                AddLog("%3d: %s\n", i, History[i]);
//            }
//            else
//            {
//                AddLog("Unknown command: '%s'\n", command_line);
//            }
//        }
//
//        static int TextEditCallbackStub(ImGuiTextEditCallbackData* data) // In C++11 you are better off using lambdas for this sort of forwarding callbacks
//        {
//            ExampleAppConsole* console = (ExampleAppConsole*)data->UserData;
//            return console->TextEditCallback(data);
//        }
//
//        int     TextEditCallback(ImGuiTextEditCallbackData* data)
//        {
//            //AddLog("cursor: %d, selection: %d-%d", data->CursorPos, data->SelectionStart, data->SelectionEnd);
//            switch (data->EventFlag)
//            {
//                case ImGuiInputTextFlags_CallbackCompletion:
//                {
//                    // Example of TEXT COMPLETION
//
//                    // Locate beginning of current word
//                    const char* word_end = data->Buf + data->CursorPos;
//                    const char* word_start = word_end;
//                    while (word_start > data->Buf)
//                    {
//                        const char c = word_start[-1];
//                        if (c == ' ' || c == '\t' || c == ',' || c == ';')
//                            break;
//                        word_start--;
//                    }
//
//                    // Build a list of candidates
//                    ImVector<const char*> candidates;
//                    for (int i = 0; i < Commands.Size; i++)
//                    if (Strnicmp(Commands[i], word_start, (int)(word_end-word_start)) == 0)
//                        candidates.push_back(Commands[i]);
//
//                    if (candidates.Size == 0)
//                    {
//                        // No match
//                        AddLog("No match for \"%.*s\"!\n", (int)(word_end-word_start), word_start);
//                    }
//                    else if (candidates.Size == 1)
//                        {
//                            // Single match. Delete the beginning of the word and replace it entirely so we've got nice casing
//                            data->DeleteChars((int)(word_start-data->Buf), (int)(word_end-word_start));
//                            data->InsertChars(data->CursorPos, candidates[0]);
//                            data->InsertChars(data->CursorPos, " ");
//                        }
//                    else
//                    {
//                        // Multiple matches. Complete as much as we can, so inputing "C" will complete to "CL" and display "CLEAR" and "CLASSIFY"
//                        int match_len = (int)(word_end - word_start);
//                        for (;;)
//                        {
//                            int c = 0;
//                            bool all_candidates_matches = true;
//                            for (int i = 0; i < candidates.Size && all_candidates_matches; i++)
//                            if (i == 0)
//                                c = toupper(candidates[i][match_len]);
//                            else if (c == 0 || c != toupper(candidates[i][match_len]))
//                                all_candidates_matches = false;
//                            if (!all_candidates_matches)
//                                break;
//                            match_len++;
//                        }
//
//                        if (match_len > 0)
//                            {
//                                data->DeleteChars((int)(word_start - data->Buf), (int)(word_end-word_start));
//                                data->InsertChars(data->CursorPos, candidates[0], candidates[0] + match_len);
//                            }
//
//                        // List matches
//                        AddLog("Possible matches:\n");
//                        for (int i = 0; i < candidates.Size; i++)
//                        AddLog("- %s\n", candidates[i]);
//                    }
//
//                    break;
//                }
//                case ImGuiInputTextFlags_CallbackHistory:
//                {
//                    // Example of HISTORY
//                    const int prev_history_pos = HistoryPos;
//                    if (data->EventKey == ImGuiKey_UpArrow)
//                    {
//                        if (HistoryPos == -1)
//                            HistoryPos = History.Size - 1;
//                        else if (HistoryPos > 0)
//                            HistoryPos--;
//                    }
//                    else if (data->EventKey == ImGuiKey_DownArrow)
//                    {
//                        if (HistoryPos != -1)
//                            if (++HistoryPos >= History.Size)
//                                HistoryPos = -1;
//                    }
//
//                    // A better implementation would preserve the data on the current input line along with cursor position.
//                    if (prev_history_pos != HistoryPos)
//                        {
//                            data->CursorPos = data->SelectionStart = data->SelectionEnd = data->BufTextLen = (int)snprintf(data->Buf, (size_t)data->BufSize, "%s", (HistoryPos >= 0) ? History[HistoryPos] : "");
//                            data->BufDirty = true;
//                        }
//                }
//            }
//            return 0;
//        }
    }
}

object Log {

    val log = ExampleAppLog()
    var lastTime = -1f
    val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
    val random = Random()
    val rand get() = abs(random.nextInt() / 100_000)

    /** Demonstrate creating a simple log window with basic filtering.  */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        // Demo: add random items (unless Ctrl is held)
        val time = g.time
        if (time - lastTime >= 0.2f && !IO.keyCtrl) {
            val s = randomWords[rand % randomWords.size]
            val t = "%.1f".format(style.locale, time)
            log.addLog("[$s] Hello, time is $t, rand() $rand\n")
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

        fun clear() = buf.setLength(0)

        fun draw(title: String, open: KMutableProperty0<Boolean>? = null) {

            setNextWindowSize(Vec2(500, 400), Cond.FirstUseEver)
            _begin(title, open)
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

            if (scrollToBottom) setScrollHere(1f)
            scrollToBottom = false
            endChild()
            end()
        }
    }
}

object Layout {

    var selectedChild = 0

    /** Demonstrate create a window with multiple child windows.    */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(500, 440), Cond.FirstUseEver)
        if (_begin("Example: Layout", open, Wf.MenuBar.i)) {
            menuBar {
                menu("File") {
                    if (menuItem("Close")) open.set(false)
                }
            }

            // left
            beginChild("left pane", Vec2(150, 0), true)
            repeat(100) {
                if (selectable("MyObject $it", selectedChild == it))
                    selectedChild = it
            }
            endChild()
            sameLine()

            // right
            beginGroup()
            beginChild("item view", Vec2(0, -itemsLineHeightWithSpacing)) // Leave room for 1 line below us
            text("MyObject: ${selectedChild}")
            separator()
            textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                    "incididunt ut labore et dolore magna aliqua. ")
            endChild()
            beginChild("buttons")
            if (button("Revert")) Unit
            sameLine()
            if (button("Save")) Unit
            endChild()
            endGroup()
        }
        end()
    }
}

object PropertyEditor {

    /** Demonstrate create a simple property editor.    */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(430, 450), Cond.FirstUseEver)
        if (!_begin("Example: Property editor", open)) {
            end()
            return
        }

        showHelpMarker("This example shows how you may implement a property editor using two columns.\n" +
                "All objects/fields data are dummies here.\n" +
                "Remember that in many simple cases, you can use ImGui::SameLine(xxx) to position\n" +
                "your cursor horizontally instead of using the Columns() API.")

        pushStyleVar(StyleVar.FramePadding, Vec2(2))
        columns(2)
        separator()


        // Iterate dummy objects with dummy members (all the same data)
        for (objI in 0..2)
            showDummyObject("Object", objI)

        columns(1)
        separator()
        popStyleVar()
        end()
    }

    fun showDummyObject(prefix: String, uid: Int) {
        //  Use object uid as identifier. Most commonly you could also use the object pointer as a base ID.
        pushId(uid)
        /*  Text and Tree nodes are less high than regular widgets, here we add vertical spacing to make the tree
            lines equal high.             */
        alignTextToFramePadding()
        val nodeOpen = treeNode("Object", "${prefix}_$uid")
        nextColumn()
        alignTextToFramePadding()
        text("my sailor is rich")
        nextColumn()
        if (nodeOpen) {
            for (i in 0..7) {
                pushId(i) // Use field index as identifier.
                if (i < 2)
                    showDummyObject("Child", 424242)
                else {
                    alignTextToFramePadding()
                    // Here we use a Selectable (instead of Text) to highlight on hover
                    //Text("Field_%d", i);
                    bullet()
                    selectable("Field_$i")
                    nextColumn()
                    pushItemWidth(-1)
                    if (i >= 5)
                        inputFloat("##value", dummyMembers, i, 1f)
                    else
                        dragFloat("##value", dummyMembers, i, 0.01f)
                    popItemWidth()
                    nextColumn()
                }
                popId()
            }
            treePop()
        }
        popId()
    }

    val dummyMembers = floatArrayOf(0f, 0f, 1f, 3.1416f, 100f, 999f, 0f, 0f, 0f)
}

object LongText {

    /** Demonstrate/test rendering huge amount of text, and the incidence of clipping.  */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(520, 600), Cond.FirstUseEver)
        if (!_begin("Example: Long text display, TODO", open)) {
            end()
            return
        }

//            static int test_type = 0;
//            static ImGuiTextBuffer log;
//            static int lines = 0;
//            ImGui::Text("Printing unusually long amount of text.");
//            ImGui::Combo("Test type", &test_type, "Single call to TextUnformatted()\0Multiple calls to Text(), clipped manually\0Multiple calls to Text(), not clipped\0");
//            ImGui::Text("Buffer contents: %d lines, %d bytes", lines, log.size());
//            if (ImGui::Button("Clear")) { log.clear(); lines = 0; }
//            ImGui::SameLine();
//            if (ImGui::Button("Add 1000 lines"))
//            {
//                for (int i = 0; i < 1000; i++)
//                log.append("%i The quick brown fox jumps over the lazy dog\n", lines+i);
//                lines += 1000;
//            }
//            ImGui::BeginChild("Log");
//            switch (test_type)
//            {
//                case 0:
//                // Single call to TextUnformatted() with a big buffer
//                ImGui::TextUnformatted(log.begin(), log.end());
//                break;
//                case 1:
//                {
//                    // Multiple calls to Text(), manually coarsely clipped - demonstrate how to use the ImGuiListClipper helper.
//                    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0,0));
//                    ImGuiListClipper clipper(lines);
//                    while (clipper.Step())
//                        for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
//                    ImGui::Text("%i The quick brown fox jumps over the lazy dog", i);
//                    ImGui::PopStyleVar();
//                    break;
//                }
//                case 2:
//                // Multiple calls to Text(), not clipped (slow)
//                ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0,0));
//                for (int i = 0; i < lines; i++)
//                ImGui::Text("%i The quick brown fox jumps over the lazy dog", i);
//                ImGui::PopStyleVar();
//                break;
//            }
//            ImGui::EndChild();
        end()
    }
}

object AutoResize {

    val lines = intArrayOf(10)

    /** Demonstrate creating a window which gets auto-resized according to its content. */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        if (!_begin("Example: Auto-resizing window", open, Wf.AlwaysAutoResize.i)) {
            end()
            return
        }

        text("Window will resize every-frame to the size of its content.\nNote that you probably don't want to " +
                "query the window size to\noutput your content because that would create a feedback loop.")
        sliderInt("Number of lines", lines, 1, 20)
        for (i in 0 until lines[0])
            text(" ".repeat(i * 4) + "This is line $i") // Pad with space to extend size horizontally
        end()
    }
}

object ConstrainedResize {

    var type = 0

    /** Demonstrate creating a window with custom resize constraints.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {
        when (type) {
            0 -> setNextWindowSizeConstraints(Vec2(-1, 0), Vec2(-1, Float.MAX_VALUE))      // Vertical only
            1 -> setNextWindowSizeConstraints(Vec2(0, -1), Vec2(Float.MAX_VALUE, -1))      // Horizontal only
            2 -> setNextWindowSizeConstraints(Vec2(100), Vec2(Float.MAX_VALUE)) // Width > 100, Height > 100
            3 -> setNextWindowSizeConstraints(Vec2(300, 0), Vec2(400, Float.MAX_VALUE))     // Width 300-400
            4 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints.square)          // Always Square
            5 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints.step, 100)// Fixed Step
        }
        withWindow("Example: Constrained Resize", open) {
            val desc = listOf("Resize vertical only", "Resize horizontal only", "Width > 100, Height > 100",
                    "Width 300-400", "Custom: Always Square", "Custom: Fixed Steps (100)")
            combo("Constraint", ::type, desc)
            button("200x200") { setWindowSize(Vec2(200)) }; sameLine()
            button("500x500") { setWindowSize(Vec2(500)) }; sameLine()
            button("800x200") { setWindowSize(Vec2(800, 200)) }
            for (i in 0 until 10) text("Hello, sailor! Making this line long enough for the example.")
        }
    }

    /** Helper functions to demonstrate programmatic constraints    */
    object CustomConstraints {
        val square: SizeConstraintCallback = { _: Any?, _: Vec2i, _: Vec2, desiredSize: Vec2 ->
            desiredSize put max(desiredSize.x, desiredSize.y)
        }
        val step: SizeConstraintCallback = { userData: Any?, _: Vec2i, _: Vec2, desiredSize: Vec2 ->
            val step = (userData as Int).f
            desiredSize.x = (desiredSize.x / step + 0.5f).i * step
            desiredSize.y = (desiredSize.y / step + 0.5f).i * step
        }

    }
}

object FixedOverlay {

    var corner = 0

    /** Demonstrate creating a simple static window with no decoration + a context-menu to choose which corner
     *  of the screen to use */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        val DISTANCE = 10f
        val windowPos = Vec2(if (corner has 1) IO.displaySize.x - DISTANCE else DISTANCE,
                if (corner has 2) IO.displaySize.y - DISTANCE else DISTANCE)
        val windowPosPivot = Vec2(if (corner has 1) 1f else 0f, if (corner has 2) 1f else 0f)
        setNextWindowPos(windowPos, Cond.Always, windowPosPivot)
        pushStyleColor(Col.WindowBg, Vec4(0f, 0f, 0f, 0.3f))  // Transparent background
        window("Example: Fixed Overlay", open, Wf.NoTitleBar or Wf.NoResize or Wf.AlwaysAutoResize or Wf.NoMove or Wf.NoSavedSettings) {
            text("Simple overlay\nin the corner of the screen.\n(right-click to change position)")
            separator()
            text("Mouse Position: (%.1f,%.1f)".format(IO.mousePos.x, IO.mousePos.y))
            popupContextWindow {
                menuItem("Top-left", "", corner == 0) { corner = 0 }
                menuItem("Top-right", "", corner == 1) { corner = 1 }
                menuItem("Bottom-left", "", corner == 2) { corner = 2 }
                menuItem("Bottom-right", "", corner == 3) { corner = 3 }
            }
        }
        popStyleColor()
    }
}

object ManipulatingWindowTitle {

    /** Demonstrate using "##" and "###" in identifiers to manipulate ID generation.
     *  Read section "How can I have multiple widgets with the same label? Can I have widget without a label? (Yes).
     *  A primer on the purpose of labels/IDs." about ID.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {
        /*  By default, Windows are uniquely identified by their title.
            You can use the "##" and "###" markers to manipulate the display/ID.
            Using "##" to display same title but have unique identifier.    */
        setNextWindowPos(Vec2(100), Cond.FirstUseEver)
        withWindow("Same title as another window##1") {
            text("This is window 1.\nMy title is the same as window 2, but my identifier is unique.")
        }

        setNextWindowPos(Vec2(100, 200), Cond.FirstUseEver)
        withWindow("Same title as another window##2") {
            text("This is window 2.\nMy title is the same as window 1, but my identifier is unique.")
        }

        // Using "###" to display a changing title but keep a static identifier "AnimatedTitle"
        val title = "Animated title ${"|/-\\"[(time / 0.25f).i and 3]} ${glm_.detail.Random.int}###AnimatedTitle"
        setNextWindowPos(Vec2(100, 300), Cond.FirstUseEver)
        withWindow(title) { text("This window has a changing title.") }
    }
}

object CustomRendering {

    /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(350, 560), Cond.FirstUseEver)
        if (!_begin("Example: Custom rendering", open)) {
            end()
            return
        }

        text("TODO")

        /*  Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and
            benefit of overloaded operators, etc.
            Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your types and
            ImVec2/ImVec4.
            ImGui defines overloaded operators but they are internal to imgui.cpp and not exposed outside
            (to avoid messing with your types)
            In this example we are not using the maths operators!   */
//            ImDrawList* draw_list = ImGui::GetWindowDrawList();
//
//            // Primitives
//            ImGui::Text("Primitives");
//            static float sz = 36.0f;
//            static ImVec4 col = ImVec4(1.0f,1.0f,0.4f,1.0f);
//            ImGui::DragFloat("Size", &sz, 0.2f, 2.0f, 72.0f, "%.0f");
//            ImGui::ColorEdit3("Color", &col.x);
//            {
//                const ImVec2 p = ImGui::GetCursorScreenPos();
//                const ImU32 col32 = ImColor(col);
//                float x = p.x + 4.0f, y = p.y + 4.0f, spacing = 8.0f;
//                for (int n = 0; n < 2; n++)
//                {
//                    float thickness = (n == 0) ? 1.0f : 4.0f;
//                    draw_list->AddCircle(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 20, thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 0.0f, ~0, thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f, ~0, thickness); x += sz+spacing;
//                    draw_list->AddTriangle(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y   ), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x,    y+sz), col32, thickness); x += spacing;
//                    draw_list->AddBezierCurve(ImVec2(x, y), ImVec2(x+sz*1.3f,y+sz*0.3f), ImVec2(x+sz-sz*1.3f,y+sz-sz*0.3f), ImVec2(x+sz, y+sz), col32, thickness);
//                    x = p.x + 4;
//                    y += sz+spacing;
//                }
//                draw_list->AddCircleFilled(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f); x += sz+spacing;
//                draw_list->AddTriangleFilled(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32); x += sz+spacing;
//                draw_list->AddRectFilledMultiColor(ImVec2(x, y), ImVec2(x+sz, y+sz), ImColor(0,0,0), ImColor(255,0,0), ImColor(255,255,0), ImColor(0,255,0));
//                ImGui::Dummy(ImVec2((sz+spacing)*8, (sz+spacing)*3));
//            }
//            ImGui::Separator();
//            {
//                static ImVector<ImVec2> points;
//                static bool adding_line = false;
//                ImGui::Text("Canvas example");
//                if (ImGui::Button("Clear")) points.clear();
//                if (points.Size >= 2) { ImGui::SameLine(); if (ImGui::Button("Undo")) { points.pop_back(); points.pop_back(); } }
//                ImGui::Text("Left-click and drag to add lines,\nRight-click to undo");
//
//                // Here we are using InvisibleButton() as a convenience to 1) advance the cursor and 2) allows us to use IsItemHovered()
//                // However you can draw directly and poll mouse/keyboard by yourself. You can manipulate the cursor using GetCursorPos() and SetCursorPos().
//                // If you only use the ImDrawList API, you can notify the owner window of its extends by using SetCursorPos(max).
//                ImVec2 canvas_pos = ImGui::GetCursorScreenPos();            // ImDrawList API uses screen coordinates!
//                ImVec2 canvas_size = ImGui::GetContentRegionAvail();        // Resize canvas to what's available
//                if (canvas_size.x < 50.0f) canvas_size.x = 50.0f;
//                if (canvas_size.y < 50.0f) canvas_size.y = 50.0f;
//                draw_list->AddRectFilledMultiColor(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), ImColor(50,50,50), ImColor(50,50,60), ImColor(60,60,70), ImColor(50,50,60));
//                draw_list->AddRect(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), ImColor(255,255,255));
//
//                bool adding_preview = false;
//                ImGui::InvisibleButton("canvas", canvas_size);
//                ImVec2 mouse_pos_in_canvas = ImVec2(ImGui::GetIO().MousePos.x - canvas_pos.x, ImGui::GetIO().MousePos.y - canvas_pos.y);
//                if (adding_line)
//                {
//                    adding_preview = true;
//                    points.push_back(mouse_pos_in_canvas);
//                    if (!ImGui::GetIO().MouseDown[0])
//                        adding_line = adding_preview = false;
//                }
//                if (ImGui::IsItemHovered())
//                {
//                    if (!adding_line && ImGui::IsMouseClicked(0))
//                    {
//                        points.push_back(mouse_pos_in_canvas);
//                        adding_line = true;
//                    }
//                    if (ImGui::IsMouseClicked(1) && !points.empty())
//                    {
//                        adding_line = adding_preview = false;
//                        points.pop_back();
//                        points.pop_back();
//                    }
//                }
//                draw_list->PushClipRect(canvas_pos, ImVec2(canvas_pos.x+canvas_size.x, canvas_pos.y+canvas_size.y));      // clip lines within the canvas (if we resize it, etc.)
//                for (int i = 0; i < points.Size - 1; i += 2)
//                draw_list->AddLine(ImVec2(canvas_pos.x + points[i].x, canvas_pos.y + points[i].y), ImVec2(canvas_pos.x + points[i+1].x, canvas_pos.y + points[i+1].y), IM_COL32(255,255,0,255), 2.0f);
//                draw_list->PopClipRect();
//                if (adding_preview)
//                    points.pop_back();
//            }
        end()
    }
}

object StyleEditor {

    var outputDest = 0
    var outputOnlyModified = false
    var alphaFlags = 0
    val filter = TextFilter()
    var windowScale = 1f

    operator fun invoke(ref: Style? = null) {

        /*  You can pass in a reference ImGuiStyle structure to compare to, revert to and save to
            (else it compares to the default style)         */
        val defaultStyle = Style()  // Default style
        button("Revert Style") {
            g.style = ref ?: defaultStyle
        }

        ref?.let {
            sameLine()
            button("Save Style") {
                TODO()//*ref = style
            }
        }

        pushItemWidth(windowWidth * 0.55f)

        treeNode("Rendering") {
            checkbox("Anti-aliased lines", style::antiAliasedLines)
            checkbox("Anti-aliased shapes", style::antiAliasedShapes)
            pushItemWidth(100)
            dragFloat("Curve Tessellation Tolerance", style::curveTessellationTol, 0.02f, 0.1f, Float.MAX_VALUE, "", 2f)
            if (style.curveTessellationTol < 0f) style.curveTessellationTol = 0.1f
            /*  Not exposing zero here so user doesn't "lose" the UI (zero alpha clips all widgets).
                But application code could have a toggle to switch between zero and non-zero.             */
            dragFloat("Global Alpha", style::alpha, 0.005f, 0.2f, 1f, "%.2f")
            popItemWidth()
        }

        treeNode("Settings") {
            sliderVec2("WindowPadding", style.windowPadding, 0f, 20f, "%.0f")
            sliderFloat("WindowRounding", style::windowRounding, 0f, 16f, "%.0f")
            sliderFloat("ChildWindowRounding", style::childWindowRounding, 0f, 16f, "%.0f")
            sliderVec2("FramePadding", style.framePadding, 0f, 20f, "%.0f")
            sliderFloat("FrameRounding", style::frameRounding, 0f, 16f, "%.0f")
            sliderVec2("ItemSpacing", style.itemSpacing, 0f, 20f, "%.0f")
            sliderVec2("ItemInnerSpacing", style.itemInnerSpacing, 0f, 20f, "%.0f")
            sliderVec2("TouchExtraPadding", style.touchExtraPadding, 0f, 10f, "%.0f")
            sliderFloat("IndentSpacing", style::indentSpacing, 0f, 30f, "%.0f")
            sliderFloat("ScrollbarSize", style::scrollbarSize, 1f, 20f, "%.0f")
            sliderFloat("ScrollbarRounding", style::scrollbarRounding, 0.0f, 16.0f, "%.0f")
            sliderFloat("GrabMinSize", style::grabMinSize, 1f, 20f, "%.0f")
            sliderFloat("GrabRounding", style::grabRounding, 0f, 16f, "%.0f")
            text("Alignment")
            sliderVec2("WindowTitleAlign", style.windowTitleAlign, 0f, 1f, "%.2f")
            sliderVec2("ButtonTextAlign", style.buttonTextAlign, 0f, 1f, "%.2f")
            sameLine()
            showHelpMarker("Alignment applies when a button is larger than its text content.")
        }

        treeNode("Colors") {

            button("Copy Colors") {
                if (outputDest == 0)
                    logToClipboard()
                else
                    TODO() //logToTTY()
                //ImGui::LogText("ImVec4* colors = ImGui::GetStyle().Colors;" IM_NEWLINE); TODO
                for (i in Col.values()) {
                    val col = style.colors[i]
                    val name = i.name
                    if (!outputOnlyModified || col != (ref?.colors?.get(i) ?: defaultStyle.colors[i]))
                        TODO()//logText("colors[ImGuiCol_%s]%*s= ImVec4(%.2ff, %.2ff, %.2ff, %.2ff);" IM_NEWLINE, name, 23 - (int)strlen(name), "", col.x, col.y, col.z, col.w);
                }
                logFinish()
            }
            sameLine()
            withItemWidth(120f) { combo("##output_type", ::outputDest, "To Clipboard\u0000To TTY\u0000") }
            sameLine()
            checkbox("Only Modified Fields", ::outputOnlyModified)

            text("Tip: Left-click on colored square to open color picker,\nRight-click to open edit options menu.")

            radioButton("Opaque", ::alphaFlags, 0); sameLine()
            radioButton("Alpha", ::alphaFlags, Cef.AlphaPreview.i); sameLine()
            radioButton("Both", ::alphaFlags, Cef.AlphaPreviewHalf.i)

            beginChild("#colors", Vec2(0, 300), true, Wf.AlwaysVerticalScrollbar.i)
            pushItemWidth(-160)
            for (i in 0 until Col.COUNT.i) {
                val name = Col.values()[i].name
                if (!filter.passFilter(name)) // TODO fix bug
                    continue
                withId(i) {
                    colorEditVec4(name, style.colors[i], Cef.AlphaBar or alphaFlags)
                    if (style.colors[i] != (ref?.colors?.get(i) ?: defaultStyle.colors[i])) {
                        sameLine()
                        button("Revert") { style.colors[i] put (ref?.colors?.get(i) ?: defaultStyle.colors[i]) }
                        ref?.let {
                            sameLine()
                            button("Save") { it.colors[i] = style.colors[i] }
                        }
                    }
                }
            }
            popItemWidth()
            endChild()
        }

        val fontsOpened = treeNode("Fonts", "Fonts (${IO.fonts.fonts.size})")
        sameLine(); showHelpMarker("Tip: Load fonts with IO.fonts.addFontFromFileTTF()\nbefore calling IO.fonts.getTex* functions.")
        if (fontsOpened) {
            val atlas = IO.fonts
            treeNode("Atlas texture", "Atlas texture (${atlas.texSize.x}x${atlas.texSize.y} pixels)") {
                image(atlas.texId, Vec2(atlas.texSize), Vec2(), Vec2(1), Vec4.fromColor(255, 255, 255, 255),
                        Vec4.fromColor(255, 255, 255, 128))
            }
            pushItemWidth(100)
            for (i in 0 until atlas.fonts.size) {

                val font = atlas.fonts[i]
                val name = font.configData.getOrNull(0)?.name ?: ""
                val fontDetailsOpened = bulletText("Font $i: '$name', %.2f px, ${font.glyphs.size} glyphs", font.fontSize)
                sameLine(); smallButton("Set as default") { IO.fontDefault = font }
                if (fontsOpened) {
                    pushFont(font)
                    text("The quick brown fox jumps over the lazy dog")
                    popFont()
                    val scale = floatArrayOf(font.scale)
                    // Scale only this font
                    dragFloat("Font scale", scale, 0.005f, 0.3f, 2f, "%.1f")
                    font.scale = scale[0]
                    sameLine()
                    showHelpMarker("""
                        |Note than the default embedded font is NOT meant to be scaled.
                        |
                        |Font are currently rendered into bitmaps at a given size at the time of building the atlas. You may oversample them to get some flexibility with scaling. You can also render at multiple sizes and select which one to use at runtime.
                        |
                        |(Glimmer of hope: the atlas system should hopefully be rewritten in the future to make scaling more natural and automatic.)""".trimMargin())
                    text("Ascent: ${font.ascent}, Descent: ${font.descent}, Height: ${font.ascent - font.descent}")
                    text("Fallback character: '${font.fallbackChar}' (${font.fallbackChar.i})")
                    val side = sqrt(font.metricsTotalSurface.f).i
                    text("Texture surface: ${font.metricsTotalSurface} pixels (approx) ~ ${side}x$side")
                    for (cfgI in font.configData.indices) {
                        val cfg = font.configData[cfgI]
                        bulletText("Input $cfgI: '${cfg.name}', Oversample: ${cfg.oversample}, PixelSnapH: ${cfg.pixelSnapH}")
                    }
                    treeNode("Glyphs", "Glyphs (${font.glyphs.size})") {
                        // Display all glyphs of the fonts in separate pages of 256 characters
                        // Forcefully/dodgily make FindGlyph() return NULL on fallback, which isn't the default behavior.
                        val glyphFallback = font.fallbackGlyph
                        font.fallbackGlyph = null
                        for (base in 0 until 0x10000 step 256) {
                            val count = (0 until 256).sumBy { if (font.findGlyph((base + it).c) != null) 1 else 0 }
                            val s = if (count > 1) "glyphs" else "glyph"
                            if (count > 0 && treeNode(base, "U+%04X..U+%04X ($count $s)", base, base + 255)) {
                                val cellSpacing = style.itemSpacing.y
                                val cellSize = Vec2(font.fontSize)
                                val basePos = Vec2(cursorScreenPos)
                                val drawList = windowDrawList
                                for (n in 0 until 256) {
                                    val cellP1 = Vec2(basePos.x + (n % 16) * (cellSize.x + cellSpacing),
                                            basePos.y + (n / 16) * (cellSize.y + cellSpacing))
                                    val cellP2 = Vec2(cellP1.x + cellSize.x, cellP1.y + cellSize.y)
                                    val glyph = font.findGlyph((base + n).c)
                                    drawList.addRect(cellP1, cellP2, COL32(255, 255, 255, if (glyph != null) 100 else 50))
                                    /*  We use ImFont::RenderChar as a shortcut because we don't have UTF-8 conversion
                                        functions available to generate a string.                                     */
                                    font.renderChar(drawList, cellSize.x, cellP1, Col.Text.u32, (base + n).c)
                                    if (glyph != null && isMouseHoveringRect(cellP1, cellP2))
                                        withTooltip {
                                            text("Codepoint: U+%04X", base + n)
                                            separator()
                                            text("AdvanceX+1: %.1f", glyph.advanceX)
                                            text("Pos: (%.2f,%.2f)->(%.2f,%.2f)", glyph.x0, glyph.y0, glyph.x1, glyph.y1)
                                            text("UV: (%.3f,%.3f)->(%.3f,%.3f)", glyph.u0, glyph.v0, glyph.u1, glyph.v1)
                                        }
                                }
                                dummy(Vec2((cellSize.x + cellSpacing) * 16, (cellSize.y + cellSpacing) * 16))
                                treePop()
                            }
                        }
                        font.fallbackGlyph = glyphFallback
                    }
                }
            }
            val pF = floatArrayOf(windowScale)
            dragFloat("this window scale", pF, 0.005f, 0.3f, 2f, "%.1f")    // scale only this window
            windowScale = pF[0]
            pF[0] = IO.fontGlobalScale
            dragFloat("global scale", pF, 0.005f, 0.3f, 2f, "%.1f") // scale everything
            IO.fontGlobalScale = pF[0]
            popItemWidth()
            setWindowFontScale(windowScale)
        }
        popItemWidth()
    }
}