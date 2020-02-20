package engine.core

import glm_.vec2.Vec2
import imgui.ID
import imgui.Key
import imgui.NUL
import imgui.NavInput
import imgui.classes.IO
import imgui.test.engine.KeyModFlag
import imgui.test.engine.KeyModFlags
import imgui.test.engine.KeyState

//-------------------------------------------------------------------------
// Types
//-------------------------------------------------------------------------

inline class TestVerboseLevel(val i: Int) {
    operator fun compareTo(b: TestVerboseLevel): Int = i.compareTo(b.i)
    val name get() = when(this) {
        Silent -> "Silent"
        Error -> "Error"
        Warning -> "Warning"
        Info -> "Info"
        Debug -> "Debug"
        Trace -> "Trace"
        else -> error("")
    }
    companion object {
        val Silent = TestVerboseLevel(0)
        val Error = TestVerboseLevel(1)
        val Warning = TestVerboseLevel(2)
        val Info = TestVerboseLevel(3)
        val Debug = TestVerboseLevel(4)
        val Trace = TestVerboseLevel(5)
        val COUNT = TestVerboseLevel(6)
    }
}

inline class TestStatus(val i: Int) {
    companion object {
        val Unknown = TestStatus(-1)
        val Success = TestStatus(0)
        val Queued = TestStatus(1)
        val Running = TestStatus(2)
        val Error = TestStatus(3)
    }
}

inline class TestGroup(val i: Int) {
    companion object {
        val Unknown = TestGroup(-1)
        val Tests = TestGroup(0)
        val Perf = TestGroup(1)
        val COUNT = TestGroup(2)
    }
}

inline class TestFlag(val i: TestFlags) {
    companion object {
        val None = TestFlag(TestFlags(0))
        val NoWarmUp = TestFlag(TestFlags(1 shl 0))    // By default, we run the GUI func twice before starting the test code
        val NoAutoFinish = TestFlag(TestFlags(1 shl 1))// By default, tests with no test func end on Frame 0 (after the warm up). Setting this require test to call ctx->Finish().
    }
}

// Flags for IM_CHECK* macros.
inline class TestCheckFlag(val i: TestCheckFlags) {
    companion object {
        val None = TestCheckFlag(TestCheckFlags(0))
        val SilentSuccess = TestCheckFlag(TestCheckFlags(1 shl 0))
    }
}

// Flags for ImGuiTestContext::Log* functions.
inline class TestLogFlag(val i: TestLogFlags) {
    companion object {
        val None = TestLogFlag(TestLogFlags(0))
        val NoHeader = TestLogFlag(TestLogFlags(1 shl 0))  // Do not display frame count and depth padding
    }
}

// Generic flags for various ImGuiTestContext functions
inline class TestOpFlag(val i: TestOpFlags) {
    companion object {
        val None = TestOpFlag(TestOpFlags(0))
        val Verbose = TestOpFlag(TestOpFlags(1 shl 0))
        val NoCheckHoveredId = TestOpFlag(TestOpFlags(1 shl 1))
        val NoError = TestOpFlag(TestOpFlags(1 shl 2))   // Don't abort/error e.g. if the item cannot be found
        val NoFocusWindow = TestOpFlag(TestOpFlags(1 shl 3))
        val NoAutoUncollapse = TestOpFlag(TestOpFlags(1 shl 4))   // Disable automatically uncollapsing windows (useful when specifically testing Collapsing behaviors)
        val IsSecondAttempt = TestOpFlag(TestOpFlags(1 shl 5))
    }
}

inline class TestRunFlag(val i: TestRunFlags) {
    companion object {
        val None = TestRunFlag(TestRunFlags(0))
        val NoGuiFunc = TestRunFlag(TestRunFlags(1 shl 0))
        val NoTestFunc = TestRunFlag(TestRunFlags(1 shl 1))
        val NoSuccessMsg = TestRunFlag(TestRunFlags(1 shl 2))
        val NoStopOnError = TestRunFlag(TestRunFlags(1 shl 3))
        val NoBreakOnError = TestRunFlag(TestRunFlags(1 shl 4))
        val ManualRun = TestRunFlag(TestRunFlags(1 shl 5))
        val CommandLine = TestRunFlag(TestRunFlags(1 shl 6))
    }
}

enum class TestInputType { None, Key, Nav, Char }

class TestRef(var id: ID = 0, var path: String? = null)

class TestInput(
        val type: TestInputType,
        val key: Key = Key.Count,
        val keyMods: KeyModFlags = KeyModFlag.None.i,
        val navInput: NavInput = NavInput.Count,
        val char: Char = NUL,
        val state: KeyState = KeyState.Unknown) {
    companion object {
        fun fromKey(v: Key, state: KeyState, mods: KeyModFlags = KeyModFlag.None.i) = TestInput(TestInputType.Key, v, mods, state = state)
        fun fromNav(v: NavInput, state: KeyState) = TestInput(TestInputType.Nav, navInput = v, state = state)
        infix fun fromChar(v: Char) = TestInput(TestInputType.Char, char = v)
    }
}

class TestInputs {
    lateinit var simulatedIO: IO
    var applyingSimulatedIO = 0
    val mousePosValue = Vec2()             // Own non-rounded copy of MousePos in order facilitate simulating mouse movement very slow speed and high-framerate
    val hostLastMousePos = Vec2()
    var mouseButtonsValue = 0x00  // FIXME-TESTS: Use simulated_io.MouseDown[] ?
    var keyMods = KeyModFlags(0x00)            // FIXME-TESTS: Use simulated_io.KeyXXX ?
    val queue = ArrayList<TestInput>()
}