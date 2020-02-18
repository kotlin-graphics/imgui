package imgui.test.app

object testApp {

    var quit = false
    ImGuiTestEngine*        TestEngine = NULL
    ImU64                   LastTime = 0
    ImVec4                  ClearColor = ImVec4(0.45f, 0.55f, 0.60f, 1.00f)

    // Command-line options
    bool                    OptGUI = DEFAULT_OPT_GUI
    bool                    OptFast = true
    ImGuiTestVerboseLevel   OptVerboseLevel = ImGuiTestVerboseLevel_COUNT // Set in main.cpp
    ImGuiTestVerboseLevel   OptVerboseLevelOnError = ImGuiTestVerboseLevel_COUNT // Set in main.cpp
    bool                    OptNoThrottle = false
    bool                    OptPauseOnExit = true
    int                     OptStressAmount = 5
    char*                   OptFileOpener = NULL
    ImVector<char*>         TestsToRun
}