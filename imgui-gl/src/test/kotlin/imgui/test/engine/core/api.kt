package imgui.test.engine.core

import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.clipboardText
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragInt
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.frameHeight
import imgui.ImGui.getID
import imgui.ImGui.inputText
import imgui.ImGui.isItemHovered
import imgui.ImGui.plotLines
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxY
import imgui.ImGui.scrollY
import imgui.ImGui.separator
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowFocus
import imgui.ImGui.setScrollHereY
import imgui.ImGui.setTooltip
import imgui.ImGui.sliderInt
import imgui.ImGui.smallButton
import imgui.ImGui.splitterBehavior
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.classes.Context
import imgui.dsl.tabBar
import imgui.dsl.tabItem
import imgui.internal.Axis
import imgui.internal.ItemStatusFlags
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.test.IMGUI_HAS_DOCK
import imgui.test.engine.TestEngine
import imgui.test.engine.itemLocate
import kool.free
import kotlin.reflect.KMutableProperty0
import imgui.TabBarFlag as Tbf
import imgui.WindowFlag as Wf

//-------------------------------------------------------------------------
// ImGuiTestEngine API
//-------------------------------------------------------------------------

// Functions

// Create test context and attach to imgui context
fun createContext(imguiContext: Context): TestEngine {
    val engine = TestEngine().apply {
        uiContextVisible = imguiContext
//        engine->UiContextBlind = NULL
        uiContextTarget = uiContextVisible
//        engine->UiContextActive = NULL
    }

    // Setup hook
    if (hookingEngine == null)
        hookingEngine = engine

    // Add .ini handle for ImGuiWindow type
//    ImGuiSettingsHandler ini_handler
//    ini_handler.TypeName = "TestEngine"
//    ini_handler.TypeHash = ImHashStr("TestEngine")
//    ini_handler.ReadOpenFn = ImGuiTestEngine_SettingsReadOpen
//    ini_handler.ReadLineFn = ImGuiTestEngine_SettingsReadLine
//    ini_handler.WriteAllFn = ImGuiTestEngine_SettingsWriteAll
//    imgui_context->SettingsHandlers.push_back(ini_handler)

    return engine
}

infix fun TestEngine.shutdownContext(engine: TestEngine) {
    uiContextVisible = null
    uiContextBlind = null
    uiContextTarget = null
    uiContextActive = null

    userDataBuffer!!.free()
    userDataBuffer = null
//    userDataBufferSize = 0

    assert(callDepth == 0)
    clearTests()
    clearLocateTasks()

    // Release hook
    if (hookingEngine === this)
        hookingEngine = null
}

//ImGuiTestEngineIO&  ImGuiTestEngine_GetIO(ImGuiTestEngine* engine) [JVM] -> Class
fun TestEngine.abort() {
    abort = true
    testContext.abort = true
}

fun TestEngine.showTestWindow(pOpen: KMutableProperty0<Boolean>) {

    if (uiFocus) {
        setNextWindowFocus()
        uiFocus = false
    }
    begin("Dear ImGui Test Engine", pOpen)// , ImGuiWindowFlags_MenuBar);

//    #if 0
//    if (0 && ImGui::BeginMenuBar())
//    {
//        if (ImGui::BeginMenu("Options"))
//        {
//            const bool busy = ImGuiTestEngine_IsRunningTests(engine)
//            ImGui::MenuItem("Run fast", NULL, &engine->IO.ConfigRunFast, !busy)
//            ImGui::MenuItem("Run in blind context", NULL, &engine->IO.ConfigRunBlind)
//            ImGui::MenuItem("Debug break on error", NULL, &engine->IO.ConfigBreakOnError)
//            ImGui::EndMenu()
//        }
//        ImGui::EndMenuBar()
//    }
//    #endif

    fun helpTooltip(desc: String) {
        if (isItemHovered()) setTooltip(desc)
    }

    // Options
    pushStyleVar(StyleVar.FramePadding, Vec2())
    checkbox("Fast", io::configRunFast); helpTooltip("Run tests as fast as possible (no vsync, no delay, teleport mouse, etc.).")
    sameLine()
    checkbox("Blind", io::configRunBlind); helpTooltip("<UNSUPPORTED>\nRun tests in a blind ui context.")
    sameLine()
    checkbox("Stop", io::configStopOnError); helpTooltip("Stop running tests when hitting an error.")
    sameLine()
    checkbox("DbgBrk", io::configBreakOnError); helpTooltip("Break in debugger when hitting an error.")
    sameLine()
    checkbox("KeepGUI", io::configKeepGuiFunc); helpTooltip("Keep GUI function running after test function is finished.")
    sameLine()
    checkbox("Refocus", io::configTakeFocusBackAfterTests); helpTooltip("Set focus back to Test window after running tests.")
    sameLine()
    setNextItemWidth(60f)
    if (dragInt("Verbose", io::configVerboseLevel, 0.1f, 0, TestVerboseLevel.COUNT - 1, io.configVerboseLevel.name))
        io.configVerboseLevelOnError = io.configVerboseLevel
    sameLine()
    setNextItemWidth(30f)
    dragInt("Perf Stress Amount", io::perfStressAmount, 0.1f, 1, 20); helpTooltip("Increase workload of performance tests (higher means longer run).")
    popStyleVar()
    separator()

    // FIXME-DOGFOODING: It would be about time that we made it easier to create splitters, current low-level splitter behavior is not easy to use properly.
    // FIXME-SCROLL: When resizing either we'd like to keep scroll focus on something (e.g. last clicked item for list, bottom for log)
    // See https://github.com/ocornut/imgui/issues/319
    val availY = contentRegionAvail.y - style.itemSpacing.y
    val minSize0 = frameHeight * 1.2f
    val minSize1 = frameHeight * 1
    var logHeight = uiLogHeight
    var listHeight = (availY - uiLogHeight) max minSize0
    run {
        val window = currentWindow
        val y = cursorScreenPos.y + listHeight
        val splitterBb = Rect(window.workRect.min.x, y - 1, window.workRect.max.x, y + 1)
        _f = listHeight
        _f1 = logHeight
        splitterBehavior(splitterBb, getID("splitter"), Axis.Y, ::_f, ::_f1, minSize0, minSize1, 3f)
        uiLogHeight = logHeight
        //DebugDrawItemRect();
    }

    // TESTS
    beginChild("List", Vec2(0, listHeight), false, Wf.NoScrollbar.i)
    tabBar("##Tests", Tbf.NoTooltip.i) {
        tabItem("TESTS") { showTestGroup(TestGroup.Tests) }
        tabItem("PERFS") { showTestGroup(TestGroup.Perf) }
    }
    endChild()
    uiSelectAndScrollToTest = null

    // LOG
    beginChild("Log", Vec2(0f, logHeight))
    tabBar("##tools") {
        tabItem("LOG") {
            text(uiSelectedTest.let { "Log for ${uiSelectedTest.category}: ${uiSelectedTest.name}" } ?: "N/A")
            if (smallButton("Clear"))
                uiSelectedTest.testLog.clear()
            sameLine()
            if (smallButton("Copy to clipboard"))
                uiSelectedTest.let { clipboardText = it.testLog.buffer }
            separator()

            // Quick status
            val uiContext = uiContextActive ?: uiContextVisible
            val itemHoveredId = uiContext!!.hoveredIdPreviousFrame
            val itemActiveId = uiContext.activeId
            val itemHoveredInfo = if (itemHoveredId != 0) itemLocate(itemHoveredId, "") else null
            val itemActiveInfo = if (itemActiveId != 0) itemLocate(itemActiveId, "") else null
            text("Hovered: 0x%08X (\"${itemHoveredInfo?.debugLabel ?: ""}\") @ (%.1f,%.1f)", itemHoveredId, uiContext.io.mousePos.x, uiContext.io.mousePos.y)
            text("Active:  0x%08X (\"${itemActiveInfo?.debugLabel else ""}\")", itemActiveId)

            separator()
            beginChild("Log")
            uiSelectedTest.let {
                drawTestLog(it, true)
                if (scrollY >= scrollMaxY)
                    setScrollHereY()
            }
            endChild()
        }

        // Tools
        tabItem("MISC TOOLS") {
            text("%.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
            separator()

            text("Tools:")
            checkbox("Capture Tool", captureTool::visible)
            checkbox("Slow down whole app", ::toolSlowDown)
            sameLine()
            setNextItemWidth(70f)
            sliderInt("##ms", ::toolSlowDownMs, 0, 400, "%d ms")

            separator()
            text("Configuration:")
            checkboxFlags("io.ConfigFlags: NavEnableKeyboard", io::configFlags, ConfigFlag.NavEnableKeyboard.i)
            checkboxFlags("io.ConfigFlags: NavEnableGamepad", io::configFlags, ConfigFlag.NavEnableGamepad.i)
            if (IMGUI_HAS_DOCK)
                checkbox("io.ConfigDockingAlwaysTabBar", io::configDockingAlwaysTabBar)
        }

        // FIXME-TESTS: Need to be visualizing the samples/spikes.
        tabItem("PERFS TOOLS") {
            val dt1 = 1.0 / ImGui.io.framerate
            val fpsNow = 1.0 / dt1
            val dt100 = perfDeltaTime100.average
            val dt1000 = perfDeltaTime1000.average
            val dt2000 = perfDeltaTime2000.average

            //if (engine->PerfRefDeltaTime <= 0.0 && engine->PerfRefDeltaTime.IsFull())
            //    engine->PerfRefDeltaTime = dt_2000;

            checkbox("Unthrolled", io::configNoThrottle)
            sameLine()
            if (button("Pick ref dt"))
                perfRefDeltaTime = dt2000

            val filterCallback: InputTextCallback = { data -> data.eventChar == ',' || data.eventChar == ';' }
            inputText("Branch/Annotation", io.perfAnnotation, InputTextFlag.CallbackCharFilter.i, filterCallback)

            val dtRef = perfRefDeltaTime
            text("[ref dt]    %6.3f ms", perfRefDeltaTime * 1000)
            text("[last 0001] %6.3f ms (%.1f FPS) ++ %6.3f ms", dt1 * 1000.0, 1.0 / dt1, (dt1 - dtRef) * 1000)
            text("[last 0100] %6.3f ms (%.1f FPS) ++ %6.3f ms ~ converging in %.1f secs", dt100 * 1000.0, 1.0 / dt100, (dt1 - dtRef) * 1000, 100.0 / fpsNow)
            text("[last 1000] %6.3f ms (%.1f FPS) ++ %6.3f ms ~ converging in %.1f secs", dt1000 * 1000.0, 1.0 / dt1000, (dt1 - dtRef) * 1000, 1000.0 / fpsNow)
            text("[last 2000] %6.3f ms (%.1f FPS) ++ %6.3f ms ~ converging in %.1f secs", dt2000 * 1000.0, 1.0 / dt2000, (dt1 - dtRef) * 1000, 2000.0 / fpsNow)

            //PlotLines("Last 100", &engine->PerfDeltaTime100.Samples.Data, engine->PerfDeltaTime100.Samples.Size, engine->PerfDeltaTime100.Idx, NULL, 0.0f, dt_1000 * 1.10f, ImVec2(0.0f, GetFontSize()));
            val plotSize = Vec2(0f, frameHeight * 3)
            val ma = perfDeltaTime500
            plotLines("Last 500",
                    { n -> val ma = (ImMovingAverage<double> *) data; return (float)(ma->Samples[n] * 1000); },
                    ma, ma->Samples.Size, 0*ma->Idx, NULL, 0.0f, (float)(ImMax(dt_100, dt_1000) * 1000.0 * 1.2f), plot_size)
        }
    }
    endChild()

    end()

    // Capture Tool
    captureTool.context.screenCaptureFunc = io.screenCaptureFunc
    if (captureTool.Visible)
        captureTool.showCaptureToolWindow(captureTool.visible)
}

fun drawTestLog(e: TestEngine, test: Test, isInteractive: Boolean) {
    val errorCol = COL32(255, 150, 150, 255)
    val warningCol = COL32(240, 240, 150, 255)
    val unimportantCol = COL32(190, 190, 190, 255)

    // FIXME-OPT: Split TestLog by lines so we can clip it easily.
    val log = test.testLog

    val text = test.testLog.buffer.begin()
    const char * text_end = test->TestLog.Buffer.end()
    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(6.0f, 2.0f))
    ImVector<ImGuiTestLogLineInfo>& line_info_vector = test->Status == ImGuiTestStatus_Error ? log->LineInfoError : log->LineInfo
    ImGuiListClipper clipper
            clipper.Begin(line_info_vector.Size)
    while (clipper.Step()) {
        for (int line_no = clipper.DisplayStart; line_no < clipper.DisplayEnd; line_no++)
        {
            ImGuiTestLogLineInfo& line_info = line_info_vector[line_no]
            const char * line_start = text +line_info.LineOffset
            const char * line_end = strchr (line_start, '\n')
            if (line_end == NULL)
                line_end = text_end

            switch(line_info.Level)
            {
                case ImGuiTestVerboseLevel_Error :
                ImGui::PushStyleColor(ImGuiCol_Text, error_col)
                break
                case ImGuiTestVerboseLevel_Warning :
                ImGui::PushStyleColor(ImGuiCol_Text, warning_col)
                break
                case ImGuiTestVerboseLevel_Debug :
                case ImGuiTestVerboseLevel_Trace :
                ImGui::PushStyleColor(ImGuiCol_Text, unimportant_col)
                break
                default:
                ImGui::PushStyleColor(ImGuiCol_Text, IM_COL32_WHITE)
                break
            }
            ImGui::TextUnformatted(line_start, line_end)
            ImGui::PopStyleColor()

            ImGui::PushID(line_no)
            if (ImGui::BeginPopupContextItem("Context", 1)) {
                if (!ParseLineAndDrawFileOpenItem(e, test, line_start, line_end))
                    ImGui::MenuItem("No options", NULL, false, false)
                ImGui::EndPopup()
            }
            ImGui::PopID()
        }
    }
    ImGui::PopStyleVar()
}

ImGuiTest*          ImGuiTestEngine_RegisterTest(ImGuiTestEngine* engine, const char* category, const char* name, const char* src_file = NULL, int src_line = 0)
void                ImGuiTestEngine_QueueTests(ImGuiTestEngine* engine, ImGuiTestGroup group, const char* filter = NULL, ImGuiTestRunFlags run_flags = 0)
void                ImGuiTestEngine_QueueTest(ImGuiTestEngine* engine, ImGuiTest* test, ImGuiTestRunFlags run_flags)
bool                ImGuiTestEngine_IsRunningTests(ImGuiTestEngine* engine)
bool                ImGuiTestEngine_IsRunningTest(ImGuiTestEngine* engine, ImGuiTest* test)
void                ImGuiTestEngine_CalcSourceLineEnds(ImGuiTestEngine* engine)
void                ImGuiTestEngine_PrintResultSummary(ImGuiTestEngine* engine)
void                ImGuiTestEngine_GetResult(ImGuiTestEngine* engine, int& count_tested, int& success_count)

// Function pointers for IO structure
typedef bool (*ImGuiTestEngineNewFrameFunc)(ImGuiTestEngine*, void* user_data)
typedef bool (*ImGuiTestEngineEndFrameFunc)(ImGuiTestEngine*, void* user_data)
typedef void (*ImGuiTestEngineSrcFileOpenFunc)(const char* filename, int line, void* user_data)
typedef bool (*ImGuiTestEngineScreenCaptureFunc)(int x, int y, int w, int h, unsigned int* pixels, void* user_data)

// IO structure
class TestEngineIO {
    ImGuiTestEngineEndFrameFunc     EndFrameFunc = NULL
    ImGuiTestEngineNewFrameFunc     NewFrameFunc = NULL
    ImGuiTestEngineSrcFileOpenFunc  SrcFileOpenFunc = NULL     // (Optional) To open source files
    ImGuiTestEngineScreenCaptureFunc ScreenCaptureFunc = NULL  // (Optional) To capture graphics output
    void*                           UserData = NULL

    // Inputs: Options
    bool                        ConfigRunWithGui = false       // Run without graphics output (e.g. command-line)
    bool                        ConfigRunFast = true           // Run tests as fast as possible (teleport mouse, skip delays, etc.)
    bool                        ConfigRunBlind = false         // Run tests in a blind ImGuiContext separated from the visible context
    bool                        ConfigStopOnError = false      // Stop queued tests on test error
    bool                        ConfigBreakOnError = false     // Break debugger on test error
    bool                        ConfigKeepGuiFunc = false      // Keep test GUI running at the end of the test
    ImGuiTestVerboseLevel       ConfigVerboseLevel = ImGuiTestVerboseLevel_Warning
    ImGuiTestVerboseLevel       ConfigVerboseLevelOnError = ImGuiTestVerboseLevel_Info
    bool                        ConfigLogToTTY = false
    bool                        ConfigTakeFocusBackAfterTests = true
    bool                        ConfigNoThrottle = false       // Disable vsync for performance measurement
    float                       MouseSpeed = 800.0f            // Mouse speed (pixel/second) when not running in fast mode
    float                       MouseWobble = 0.25f            // How much wobble to apply to the mouse (pixels per pixel of move distance) when not running in fast mode
    float                       ScrollSpeed = 1600.0f          // Scroll speed (pixel/second) when not running in fast mode
    float                       TypingSpeed = 30.0f            // Char input speed (characters/second) when not running in fast mode
    int                         PerfStressAmount = 1           // Integer to scale the amount of items submitted in test
    char                        PerfAnnotation[32] = ""        // e.g. fill in branch name

    // Outputs: State
    bool                        RunningTests = false
}

// Result of an ItemLocate query
class TestItemInfo {
    var refCount = 0               // User can increment this if they want to hold on the result pointer across frames, otherwise the task will be GC-ed.
    var navLayer = 0              // Nav layer of the item
    var depth = 0              // Depth from requested parent id. 0 == ID is immediate child of requested parent id.
    var timestampMain = -1         // Timestamp of main result (all fields)
    var timestampStatus = -1       // Timestamp of StatusFlags
    var id: ID = 0                     // Item ID
    var parentID: ID = 0               // Item Parent ID (value at top of the ID stack)
    var window: Window? = null              // Item Window
    var rectFull = Rect()        // Item Rectangle
    var rectClipped = Rect()     // Item Rectangle (clipped with window->ClipRect at time of item submission)
    var statusFlags: ItemStatusFlags = 0            // Item Status flags (fully updated for some items only, compare TimestampStatus to FrameCount)
    var debugLabel/*[32]*/ = ""         // Shortened label for debugging purpose
}

// Result of an ItemGather query
class TestItemList {
    ImPool<ImGuiTestItemInfo>   Pool
    int&                        Size           // FIXME: THIS IS REF/POINTER to Pool.Buf.Size! This codebase is totally embracing evil C++!

    void                        Clear()
    { Pool.Clear(); }
    void                        Reserve(int capacity)
    { Pool.Reserve(capacity); }
    //int                       GetSize() const         { return Pool.GetSize(); }
    const ImGuiTestItemInfo*    operator [] (size_t n)
    { return Pool.GetByIndex((int) n); }
    const ImGuiTestItemInfo*    GetByIndex(int n)
    { return Pool.GetByIndex(n); }
    const ImGuiTestItemInfo*    GetByID(ImGuiID id)
    { return Pool.GetByKey(id); }

    ImGuiTestItemList() : Size(Pool.Buf.Size)
    {} // FIXME: THIS IS REF/POINTER to Pool.Buf.Size!
}

// Gather items in given parent scope.
class TestGatherTask {
    ImGuiID                 ParentID = 0
    int                     Depth = 0
    ImGuiTestItemList*      OutList = NULL
    ImGuiTestItemInfo*      LastItemInfo = NULL
}

// Helper to output a string showing the Path, ID or Debug Label based on what is available (some items only have ID as we couldn't find/store a Path)
class TestRefDesc {
    char Buf[80]

    const char* c_str()
    { return Buf; }
    ImGuiTestRefDesc(const ImGuiTestRef& ref, const ImGuiTestItemInfo* item = NULL)
    {
        if (ref.Path)
            ImFormatString(Buf, IM_ARRAYSIZE(Buf), "'%s' > %08X", ref.Path, ref.ID)
        else
            ImFormatString(Buf, IM_ARRAYSIZE(Buf), "%08X > '%s'", ref.ID, item ? item->DebugLabel : "NULL")
    }

    override fun toString(): String {
        return super.toString()
    }
}