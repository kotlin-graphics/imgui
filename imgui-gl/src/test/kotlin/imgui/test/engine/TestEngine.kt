package imgui.test.engine

import imgui.classes.Context
import imgui.classes.TextFilter
import imgui.test.engine.context.TestContext
import imgui.test.engine.core.TestInputs
import imgui.test.engine.core.TestLocateTask
import java.nio.ByteBuffer

class TestEngine {

    lateinit var io: TestEngineIO
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
    val perfDeltaTime100 = MovingAverage<Double>().also { init(100) }
    val perfDeltaTime500 = MovingAverage<Double>().also { init(500) }
    val perfDeltaTime1000 = MovingAverage<Double>().also { init(1000) }
    val perfDeltaTime2000 = MovingAverage<Double>().also { init(2000) }


    // Tools
    val captureTool = CaptureTool()
    var toolSlowDown = false
    var toolSlowDownMs = 100

    // Functions
    fun destroy() = uiContextBlind?.destroy()
}