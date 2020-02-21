package engine.core

import IMGUI_DEBUG_TEST_ENGINE
import engine.CaptureArgs
import engine.context.TestActiveFunc
import engine.context.recoverFromUiContextErrors
import imgui.ID
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
                val footerSz = task.debugName.size - 2 - headerSz
                assert(headerSz > 0 && footerSz > 0)
                TODO()
//                formatString(task.debugName, "%.*s..%.*s", (int)header_sz, debug_id, (int)footer_sz, debug_id+debug_id_sz-footer_sz)
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
    val g = ctx!!.uiContext!!

    if (g.withinFrameScope)
        io.endFrameFunc!!(this, io.userData)

    io.newFrameFunc!!(this, io.userData)
    assert(g.io.deltaTime > 0f)

    if (!g.withinFrameScope)
        return

    if (ctx != null) {
        // Can only yield in the test func!
        assert(ctx.activeFunc == TestActiveFunc.TestFunc)

        ctx.test?.guiFunc?.let {
            // Call user GUI function
            if (ctx.runFlags hasnt TestRunFlag.NoGuiFunc) {
                val backupActiveFunc = ctx.activeFunc
                ctx.activeFunc = TestActiveFunc.GuiFunc
                it(ctx)
                ctx.activeFunc = backupActiveFunc
            }

            // Safety net
            //if (ctx->Test->Status == ImGuiTestStatus_Error)
            ctx.recoverFromUiContextErrors()
        }
    }
}

infix fun TestEngine.setDeltaTime(deltaTime: Float) {

    assert(deltaTime >= 0f)
    overrideDeltaTime = deltaTime
}

// ImGuiTestEngine_GetFrameCount -> val

val TestEngine.perfDeltaTime500Average
    get() = perfDeltaTime500.average

//const char*         ImGuiTestEngine_GetVerboseLevelName(ImGuiTestVerboseLevel v)

infix fun TestEngine.captureScreenshot(args: CaptureArgs): Boolean {

    val ct = captureTool.context
    if (ct.screenCaptureFunc == null) {
        assert(false)
        return false
    }

    // Graphics API must render a window so it can be captured
    val backupFast = io.configRunFast
    io.configRunFast = false

    while (ct.captureScreenshot(args))
        yield()

    io.configRunFast = backupFast
    return true
}