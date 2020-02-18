package imgui.test.engine.core

import imgui.ID
import imgui.test.engine.TestEngine

//-------------------------------------------------------------------------
// [SECTION] DATA STRUCTURES
//-------------------------------------------------------------------------

var hookingEngine: TestEngine? = null

// [Internal] Locate item position/window/state given ID.
class TestLocateTask(
        var id: ID = 0,
        var frameCount: Int = -1        // Timestamp of request
) {
    var debugName = ByteArray(64)  // char[64]
    val result = TestItemInfo()
}

struct ImGuiTestRunTask
{
    ImGuiTest * Test = NULL
    ImGuiTestRunFlags RunFlags = ImGuiTestRunFlags_None
}

// [Internal] Test Engine Context
struct ImGuiTestEngine
{
    ImGuiTestEngineIO IO
            ImGuiContext * UiContextVisible = NULL        // imgui context for visible/interactive needs
    ImGuiContext * UiContextBlind = NULL          // FIXME
    ImGuiContext * UiContextTarget = NULL         // imgui context for testing == io.ConfigRunBlind ? UiBlindContext : UiVisibleContext when running tests, otherwise NULL.
    ImGuiContext * UiContextActive = NULL         // imgui context for testing == UiContextTarget or NULL

    int FrameCount = 0
    float OverrideDeltaTime = - 1.0f      // Inject custom delta time into imgui context to simulate clock passing faster than wall clock time.
    ImVector < ImGuiTest * > TestsAll
            ImVector<ImGuiTestRunTask> TestsQueue
            ImGuiTestContext * TestContext = NULL
    int CallDepth = 0
    ImVector < ImGuiTestLocateTask * > LocateTasks
            ImGuiTestGatherTask GatherTask
            void * UserDataBuffer = NULL
    size_t UserDataBufferSize = 0

    // Inputs
    ImGuiTestInputs Inputs

            // UI support
            bool Abort = false
    bool UiFocus = false
    ImGuiTest * UiSelectAndScrollToTest = NULL
    ImGuiTest * UiSelectedTest = NULL
    ImGuiTextFilter UiTestFilter
            float UiLogHeight = 150.0f

    // Performance Monitor
    double PerfRefDeltaTime
            ImMovingAverage<double> PerfDeltaTime100
            ImMovingAverage<double> PerfDeltaTime500
            ImMovingAverage<double> PerfDeltaTime1000
            ImMovingAverage<double> PerfDeltaTime2000

            // Tools
            ImGuiCaptureTool CaptureTool
            bool ToolSlowDown = false
    int ToolSlowDownMs = 100

    // Functions
    ImGuiTestEngine()
    {
        PerfRefDeltaTime = 0.0f
        PerfDeltaTime100.Init(100)
        PerfDeltaTime500.Init(500)
        PerfDeltaTime1000.Init(1000)
        PerfDeltaTime2000.Init(2000)
    }
    ~ImGuiTestEngine()
{
    if (UiContextBlind != NULL)
        ImGui::DestroyContext(UiContextBlind)
}
}