package app

import engine.core.TestEngine
import engine.core.TestVerboseLevel
import glm_.vec4.Vec4

//#if defined(IMGUI_TESTS_BACKEND_WIN32_DX11) || defined(IMGUI_TESTS_BACKEND_SDL_GL3)
val DEFAULT_OPT_GUI = true
//#else
//static const bool DEFAULT_OPT_GUI = false
//#endif

object TestApp {

    var quit = false
    var testEngine: TestEngine? = null
    var lastTime = 0L
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)

    // Command-line options
    var optGUI = DEFAULT_OPT_GUI
    var optFast = true
    var optVerboseLevel = TestVerboseLevel.COUNT // Set in main.cpp
    var optVerboseLevelOnError = TestVerboseLevel.COUNT // Set in main.cpp
    var optNoThrottle = false
    var optPauseOnExit = true
    var optStressAmount = 5
//    char*                   OptFileOpener = NULL
    val testsToRun = ArrayList<String>()
}