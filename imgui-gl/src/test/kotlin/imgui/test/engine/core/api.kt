package imgui.test.engine.core

import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.checkbox
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragInt
import imgui.ImGui.endChild
import imgui.ImGui.frameHeight
import imgui.ImGui.getID
import imgui.ImGui.isItemHovered
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowFocus
import imgui.ImGui.setTooltip
import imgui.ImGui.smallButton
import imgui.ImGui.splitterBehavior
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.classes.Context
import imgui.dsl.tabBar
import imgui.dsl.tabItem
import imgui.internal.Axis
import imgui.internal.classes.Rect
import imgui.test.engine.TestEngine
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

    if (uiFocus)    {
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

    fun helpTooltip(desc: String)    {
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
        val window = currentWindow!!
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
    tabBar("##Tests", Tbf.NoTooltip.i)    {
        tabItem("TESTS") { showTestGroup(TestGroup.Tests) }
        tabItem("PERFS") { showTestGroup(TestGroup.Perf) }
    }
    endChild()
    uiSelectAndScrollToTest = null

    // LOG
    beginChild("Log", Vec2(0f, logHeight))
    tabBar("##tools") {
        tabItem("LOG") {
            text(uiSelectedTest?.let { "Log for ${uiSelectedTest.category}: ${uiSelectedTest.name}" } ?: "N/A")
            if (smallButton("Clear"))
                if (engine->UiSelectedTest)
            engine->UiSelectedTest->TestLog.Clear()
            SameLine()
            if (SmallButton("Copy to clipboard"))
                if (engine->UiSelectedTest)
            SetClipboardText(engine->UiSelectedTest->TestLog.Buffer.c_str())
            Separator()

            // Quick status
            ImGuiContext* ui_context = engine->UiContextActive ? engine->UiContextActive : engine->UiContextVisible
            ImGuiID item_hovered_id = ui_context->HoveredIdPreviousFrame
            ImGuiID item_active_id = ui_context->ActiveId
            ImGuiTestItemInfo* item_hovered_info = item_hovered_id ? ImGuiTestEngine_ItemLocate(engine, item_hovered_id, "") : NULL
            ImGuiTestItemInfo* item_active_info = item_active_id ? ImGuiTestEngine_ItemLocate(engine, item_active_id, "") : NULL
            Text("Hovered: 0x%08X (\"%s\") @ (%.1f,%.1f)", item_hovered_id, item_hovered_info ? item_hovered_info->DebugLabel : "", ui_context->IO.MousePos.x, ui_context->IO.MousePos.y)
            Text("Active:  0x%08X (\"%s\")", item_active_id, item_active_info ? item_active_info->DebugLabel : "")

            Separator()
            BeginChild("Log")
            if (engine->UiSelectedTest)
            {
                DrawTestLog(engine, engine->UiSelectedTest, true)
                if (GetScrollY() >= GetScrollMaxY())
                    SetScrollHereY()
            }
            EndChild()
        }

        // Tools
        if (BeginTabItem("MISC TOOLS"))
        {
            ImGuiIO& io = GetIO()
            Text("%.3f ms/frame (%.1f FPS)", 1000.0f / io.Framerate, io.Framerate)
            Separator()

            Text("Tools:")
            Checkbox("Capture Tool", &engine->CaptureTool.Visible)
            Checkbox("Slow down whole app", &engine->ToolSlowDown)
            SameLine()
            SetNextItemWidth(70)
            SliderInt("##ms", &engine->ToolSlowDownMs, 0, 400, "%d ms")

            Separator()
            Text("Configuration:")
            CheckboxFlags("io.ConfigFlags: NavEnableKeyboard", (unsigned int *)&io.ConfigFlags, ImGuiConfigFlags_NavEnableKeyboard)
            CheckboxFlags("io.ConfigFlags: NavEnableGamepad", (unsigned int *)&io.ConfigFlags, ImGuiConfigFlags_NavEnableGamepad)
            #ifdef IMGUI_HAS_DOCK
                Checkbox("io.ConfigDockingAlwaysTabBar", &io.ConfigDockingAlwaysTabBar)
            #endif
            EndTabItem()
        }

        // FIXME-TESTS: Need to be visualizing the samples/spikes.
        if (BeginTabItem("PERFS TOOLS"))
        {
            double dt_1 = 1.0 / GetIO().Framerate
            double fps_now = 1.0 / dt_1
            double dt_100 = engine->PerfDeltaTime100.GetAverage()
            double dt_1000 = engine->PerfDeltaTime1000.GetAverage()
            double dt_2000 = engine->PerfDeltaTime2000.GetAverage()

            //if (engine->PerfRefDeltaTime <= 0.0 && engine->PerfRefDeltaTime.IsFull())
            //    engine->PerfRefDeltaTime = dt_2000;

            Checkbox("Unthrolled", &engine->IO.ConfigNoThrottle)
            SameLine()
            if (Button("Pick ref dt"))
                engine->PerfRefDeltaTime = dt_2000

            const ImGuiInputTextCallback filter_callback = [](ImGuiInputTextCallbackData* data) { return (data->EventChar == ',' || data->EventChar == ';') ? 1 : 0; }
            InputText("Branch/Annotation", engine->IO.PerfAnnotation, IM_ARRAYSIZE(engine->IO.PerfAnnotation), ImGuiInputTextFlags_CallbackCharFilter, filter_callback, NULL)

            double dt_ref = engine->PerfRefDeltaTime
            Text("[ref dt]    %6.3f ms", engine->PerfRefDeltaTime * 1000)
            Text("[last 0001] %6.3f ms (%.1f FPS) ++ %6.3f ms",                           dt_1    * 1000.0, 1.0 / dt_1,    (dt_1 - dt_ref) * 1000)
            Text("[last 0100] %6.3f ms (%.1f FPS) ++ %6.3f ms ~ converging in %.1f secs", dt_100  * 1000.0, 1.0 / dt_100,  (dt_1 - dt_ref) * 1000, 100.0  / fps_now)
            Text("[last 1000] %6.3f ms (%.1f FPS) ++ %6.3f ms ~ converging in %.1f secs", dt_1000 * 1000.0, 1.0 / dt_1000, (dt_1 - dt_ref) * 1000, 1000.0 / fps_now)
            Text("[last 2000] %6.3f ms (%.1f FPS) ++ %6.3f ms ~ converging in %.1f secs", dt_2000 * 1000.0, 1.0 / dt_2000, (dt_1 - dt_ref) * 1000, 2000.0 / fps_now)

            //PlotLines("Last 100", &engine->PerfDeltaTime100.Samples.Data, engine->PerfDeltaTime100.Samples.Size, engine->PerfDeltaTime100.Idx, NULL, 0.0f, dt_1000 * 1.10f, ImVec2(0.0f, GetFontSize()));
            ImVec2 plot_size(0.0f, GetFrameHeight() * 3)
            ImMovingAverage<double>* ma = &engine->PerfDeltaTime500
            PlotLines("Last 500",
                    [](void* data, int n) { ImMovingAverage<double>* ma = (ImMovingAverage<double>*)data; return (float)(ma->Samples[n] * 1000); },
                    ma, ma->Samples.Size, 0*ma->Idx, NULL, 0.0f, (float)(ImMax(dt_100, dt_1000) * 1000.0 * 1.2f), plot_size)

            EndTabItem()
        }
    }
    EndChild()

    End()

    // Capture Tool
    ImGuiCaptureTool& capture_tool = engine->CaptureTool
    capture_tool.Context.ScreenCaptureFunc = engine->IO.ScreenCaptureFunc
    if (capture_tool.Visible)
        capture_tool.ShowCaptureToolWindow(&capture_tool.Visible)
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
struct ImGuiTestEngineIO
{
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
struct ImGuiTestItemInfo
{
    int                         RefCount : 8               // User can increment this if they want to hold on the result pointer across frames, otherwise the task will be GC-ed.
    int                         NavLayer : 1               // Nav layer of the item
    int                         Depth : 16                 // Depth from requested parent id. 0 == ID is immediate child of requested parent id.
    int                         TimestampMain = -1         // Timestamp of main result (all fields)
    int                         TimestampStatus = -1       // Timestamp of StatusFlags
    ImGuiID                     ID = 0                     // Item ID
    ImGuiID                     ParentID = 0               // Item Parent ID (value at top of the ID stack)
    ImGuiWindow*                Window = NULL              // Item Window
    ImRect                      RectFull = ImRect()        // Item Rectangle
    ImRect                      RectClipped = ImRect()     // Item Rectangle (clipped with window->ClipRect at time of item submission)
    ImGuiItemStatusFlags        StatusFlags = 0            // Item Status flags (fully updated for some items only, compare TimestampStatus to FrameCount)
    char                        DebugLabel[32] = {}        // Shortened label for debugging purpose

    ImGuiTestItemInfo()
    {
        RefCount = 0
        NavLayer = 0
        Depth = 0
    }
}

// Result of an ItemGather query
struct ImGuiTestItemList
{
    ImPool<ImGuiTestItemInfo>   Pool
    int&                        Size           // FIXME: THIS IS REF/POINTER to Pool.Buf.Size! This codebase is totally embracing evil C++!

    void                        Clear()                 { Pool.Clear(); }
    void                        Reserve(int capacity)   { Pool.Reserve(capacity); }
    //int                       GetSize() const         { return Pool.GetSize(); }
    const ImGuiTestItemInfo*    operator[] (size_t n)   { return Pool.GetByIndex((int)n); }
    const ImGuiTestItemInfo*    GetByIndex(int n)       { return Pool.GetByIndex(n); }
    const ImGuiTestItemInfo*    GetByID(ImGuiID id)     { return Pool.GetByKey(id); }

    ImGuiTestItemList() : Size(Pool.Buf.Size) {} // FIXME: THIS IS REF/POINTER to Pool.Buf.Size!
}

// Gather items in given parent scope.
struct ImGuiTestGatherTask
{
    ImGuiID                 ParentID = 0
    int                     Depth = 0
    ImGuiTestItemList*      OutList = NULL
    ImGuiTestItemInfo*      LastItemInfo = NULL
}

// Helper to output a string showing the Path, ID or Debug Label based on what is available (some items only have ID as we couldn't find/store a Path)
struct ImGuiTestRefDesc
{
    char Buf[80]

    const char* c_str()     { return Buf; }
    ImGuiTestRefDesc(const ImGuiTestRef& ref, const ImGuiTestItemInfo* item = NULL)
    {
        if (ref.Path)
            ImFormatString(Buf, IM_ARRAYSIZE(Buf), "'%s' > %08X", ref.Path, ref.ID)
        else
            ImFormatString(Buf, IM_ARRAYSIZE(Buf), "%08X > '%s'", ref.ID, item ? item->DebugLabel : "NULL")
    }
}