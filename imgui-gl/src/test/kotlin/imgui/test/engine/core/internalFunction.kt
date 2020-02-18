package imgui.test.engine.core

import imgui.ID
import imgui.internal.formatString
import imgui.test.IMGUI_DEBUG_TEST_ENGINE
import imgui.test.engine.TestEngine
import imgui.toByteArray


//-------------------------------------------------------------------------
// Internal function
//-------------------------------------------------------------------------


// Request information about one item.
// Will push a request for the test engine to process.
// Will return NULL when results are not ready (or not available).
fun TestEngine.itemLocate(id: ID, debugId: String?): TestItemInfo? {

    assert(id != 0)

    findLocateTask(id)?.let { task ->
        if (task.result.timestampMain + 2 >= frameCount) {
            task.frameCount = frameCount // Renew task
            return task.result
        }
        return null
    }

    // Create task
    val task = TestLocateTask(id, frameCount)
    if (IMGUI_DEBUG_TEST_ENGINE)
        debugId?.let {
            val debugIdSz = debugId.length
            if (debugIdSz < task.debugName.size)
                debugId.toByteArray(task.debugName)
            else {
                val headerSz = task.debugName.size * 0.3f
                val footerSz = task.debugName.size-2-headerSz
                assert(headerSz > 0 && footerSz > 0)
                formatString(task.debugName, "%.*s..%.*s", (int)header_sz, debug_id, (int)footer_sz, debug_id+debug_id_sz-footer_sz)
            }
        }
    locateTasks += task

    return null
}

// FIXME-OPT
infix fun TestEngine.findLocateTask(id: ID): TestLocateTask? = locateTasks.find { it.id == id }

infix fun TestEngine.pushInput(input: TestInput) {
    inputs.queue += input
}
// Yield control back from the TestFunc to the main update + GuiFunc, for one frame.
fun TestEngine.yield() {
    val ctx = testContext
    val g = ctx.uiContext

    if (g.withinFrameScope)
        io.endFrameFunc(engine, engine->IO.UserData)

    io.newFrameFunc(engine, engine->IO.UserData)
    assert(g.IO.DeltaTime > 0.0f)

    if (!g.WithinFrameScope)
        return

    if (ctx != null)
        // Can only yield in the test func!
        assert(ctx.activeFunc == TestActiveFunc_TestFunc)

    ctx?.test?.guiFunc?.let {
        // Call user GUI function
        if (!(ctx.runFlags & ImGuiTestRunFlags_NoGuiFunc))
        {
            ImGuiTestActiveFunc backup_active_func = ctx->ActiveFunc
            ctx->ActiveFunc = ImGuiTestActiveFunc_GuiFunc
            engine->TestContext->Test->GuiFunc(engine->TestContext)
            ctx->ActiveFunc = backup_active_func
        }

        // Safety net
        //if (ctx->Test->Status == ImGuiTestStatus_Error)
        ctx->RecoverFromUiContextErrors()
    }
}
void                ImGuiTestEngine_SetDeltaTime(ImGuiTestEngine* engine, float delta_time)
int                 ImGuiTestEngine_GetFrameCount(ImGuiTestEngine* engine)
double              ImGuiTestEngine_GetPerfDeltaTime500Average(ImGuiTestEngine* engine)
const char*         ImGuiTestEngine_GetVerboseLevelName(ImGuiTestVerboseLevel v)
bool                ImGuiTestEngine_CaptureScreenshot(ImGuiTestEngine* engine, ImGuiCaptureArgs* args)