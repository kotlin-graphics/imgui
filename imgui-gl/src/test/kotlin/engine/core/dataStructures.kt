package engine.core

import engine.CaptureTool
import engine.MovingAverageDouble
import engine.context.TestContext
import imgui.ID
import imgui.classes.Context
import imgui.classes.TextFilter
import java.nio.ByteBuffer

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

class TestRunTask(var test: Test? = null, var runFlags: TestRunFlags = TestRunFlag.None.i)

// [Internal] Test Engine Context
class TestEngine {

    val io = TestEngineIO()
    var uiContextVisible: Context? = null        // imgui context for visible/interactive needs
    var uiContextBlind: Context? = null          // FIXME
    var uiContextTarget: Context? = null         // imgui context for testing == io.ConfigRunBlind ? UiBlindContext : UiVisibleContext when running tests, otherwise NULL.
    var uiContextActive: Context? = null         // imgui context for testing == UiContextTarget or NULL

    var frameCount = 0
    var overrideDeltaTime = -1f      // Inject custom delta time into imgui context to simulate clock passing faster than wall clock time.
    val testsAll = ArrayList<Test>()
    val testsQueue = ArrayList<TestRunTask>()
    var testContext: TestContext? = null
    var callDepth = 0
    val locateTasks = ArrayList<TestLocateTask>()
    val gatherTask = TestGatherTask()
    var userDataBuffer: ByteBuffer? = null
//    size_t                      UserDataBufferSize = 0

    // Inputs
    var inputs = TestInputs()

    // UI support
    var abort = false
    var uiFocus = false
    var uiSelectAndScrollToTest: Test? = null
    var uiSelectedTest: Test? = null
    var uiTestFilter = TextFilter()
    var uiLogHeight = 150f

    // Performance Monitor
    var perfRefDeltaTime = 0.0
    val perfDeltaTime100 = MovingAverageDouble(100)
    val perfDeltaTime500 = MovingAverageDouble(500)
    val perfDeltaTime1000 = MovingAverageDouble(1000)
    val perfDeltaTime2000 = MovingAverageDouble(2000)


    // Tools
    val captureTool = CaptureTool()
    var toolSlowDown = false
    var toolSlowDownMs = 100

    // Functions
    fun destroy() = uiContextBlind?.destroy()
}