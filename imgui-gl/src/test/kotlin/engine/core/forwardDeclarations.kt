package engine.core

import gli_.has
import gli_.hasnt
import imgui.test.engine.TestEngine
import imgui.wo

//-------------------------------------------------------------------------
// Forward Declarations
//-------------------------------------------------------------------------

//struct ImGuiTest;
//struct ImGuiTestContext;
//struct ImGuiTestEngine;
//struct ImGuiTestEngineIO;
//struct ImGuiTestItemInfo;
//struct ImGuiTestItemList;
//struct ImRect;

inline class TestFlags(val i: Int)         // Flags: See ImGuiTestFlags_
inline class TestCheckFlags(val i: Int)    // Flags: See ImGuiTestCheckFlags_
{
    infix fun hasnt(f: TestCheckFlag): Boolean = i hasnt f.i.i
}

inline class TestLogFlags(val i: Int){      // Flags: See ImGuiTestLogFlags_
    infix fun hasnt(f: TestLogFlag) = i hasnt f.i.i
}
inline class TestOpFlags(val i: Int)       // Flags: See ImGuiTestOpFlags_
{
    infix fun hasnt(f: TestOpFlag): Boolean = i hasnt f.i.i
    infix fun or(f: TestOpFlag): TestOpFlags = i or f.i.i
}

inline class TestRunFlags(val i: Int) {     // Flags: See ImGuiTestRunFlags_
    infix fun wo(f: TestRunFlag) = TestRunFlags(i wo f.i.i)
    infix fun or(f: TestRunFlag) = TestRunFlags(i or f.i.i)
    infix fun hasnt(f: TestRunFlag) = i hasnt f.i.i
    infix fun has(f: TestRunFlags): Boolean = i has f.i
    infix fun has(f: TestRunFlag): Boolean = i has f.i.i
}

//-------------------------------------------------------------------------
// [SECTION] FORWARD DECLARATIONS
//-------------------------------------------------------------------------

// Private functions
fun TestEngine.clearInput() {
    IM_ASSERT(engine->UiContextTarget != NULL);
    engine->Inputs.MouseButtonsValue = 0;
    engine->Inputs.KeyMods = ImGuiKeyModFlags_None;
    engine->Inputs.Queue.clear();

    ImGuiIO& simulated_io = engine->Inputs.SimulatedIO;
    simulated_io.KeyCtrl = simulated_io.KeyShift = simulated_io.KeyAlt = simulated_io.KeySuper = false;
    memset(simulated_io.MouseDown, 0, sizeof(simulated_io.MouseDown));
    memset(simulated_io.KeysDown, 0, sizeof(simulated_io.KeysDown));
    memset(simulated_io.NavInputs, 0, sizeof(simulated_io.NavInputs));
    simulated_io.ClearInputCharacters();
    ImGuiTestEngine_ApplyInputToImGuiContext(engine);
}
fun TestEngine.applyInputToImGuiContext();
fun TestEngine.processTestQueue();
fun TestEngine.clearTests();
fun TestEngine.clearLocateTasks();
fun TestEngine.preNewFrame(ImGuiContext* ctx);
fun TestEngine.postNewFrame(ImGuiContext* ctx);
fun TestEngine.runTest(ImGuiTestContext* ctx, void* user_data);

// Settings
//static void* ImGuiTestEngine_SettingsReadOpen(ImGuiContext*, ImGuiSettingsHandler*, const char* name);
//static void  ImGuiTestEngine_SettingsReadLine(ImGuiContext*, ImGuiSettingsHandler*, void* entry, const char* line);
//static void  ImGuiTestEngine_SettingsWriteAll(ImGuiContext* imgui_ctx, ImGuiSettingsHandler* handler, ImGuiTextBuffer* buf);
