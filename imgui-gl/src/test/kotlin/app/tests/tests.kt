// dear imgui
// (tests)

package app.tests

import engine.core.TestEngine
import engine.core.Test
import engine.core.TestGroup
import engine.pathFindFilename

//-------------------------------------------------------------------------
// NOTES (also see TODO in imgui_te_core.cpp)
//-------------------------------------------------------------------------
// - Tests can't reliably once ImGuiCond_Once or ImGuiCond_FirstUseEver
// - GuiFunc can't run code that yields. There is an assert for that.
//-------------------------------------------------------------------------

fun TestEngine.registerTests(){
    // Tests
    registerTests_Window(this)
//    RegisterTests_Layout(e)
//    RegisterTests_Widgets(e)
//    RegisterTests_Nav(e)
//    RegisterTests_Columns(e)
//    RegisterTests_Table(e)
//    RegisterTests_Docking(e)
//    RegisterTests_Misc(e)
//
//    // Captures
//    RegisterTests_Capture(e)
//
//    // Performance Benchmarks
//    RegisterTests_Perf(e)
}