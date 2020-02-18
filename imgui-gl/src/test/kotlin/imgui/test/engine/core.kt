package imgui.test.engine

import imgui.ID

//-------------------------------------------------------------------------
// Internal function
//-------------------------------------------------------------------------


// Request information about one item.
// Will push a request for the test engine to process.
// Will return NULL when results are not ready (or not available).
fun TestEngine.itemLocate(id: ID, debugId: String): TestItemInfo
//void                ImGuiTestEngine_PushInput(ImGuiTestEngine* engine, const ImGuiTestInput& input);
//void                ImGuiTestEngine_Yield(ImGuiTestEngine* engine);
//void                ImGuiTestEngine_SetDeltaTime(ImGuiTestEngine* engine, float delta_time);
//int                 ImGuiTestEngine_GetFrameCount(ImGuiTestEngine* engine);
//double              ImGuiTestEngine_GetPerfDeltaTime500Average(ImGuiTestEngine* engine);
//const char*         ImGuiTestEngine_GetVerboseLevelName(ImGuiTestVerboseLevel v);
//bool                ImGuiTestEngine_CaptureScreenshot(ImGuiTestEngine* engine, ImGuiCaptureArgs* args);