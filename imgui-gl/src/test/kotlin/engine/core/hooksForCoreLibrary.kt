package engine.core

import engine.context.logEx
import glm_.func.common.min
import imgui.ID
import imgui.classes.Context
import imgui.internal.ItemStatusFlag
import imgui.internal.ItemStatusFlags
import imgui.internal.NavLayer
import imgui.internal.classes.Rect

//-------------------------------------------------------------------------
// Hooks for Core Library
//-------------------------------------------------------------------------

fun hookPrenewframe(ctx: Context) {
    hookingEngine?.preNewFrame(ctx)
}

fun hookPostnewframe(ctx: Context) {
    hookingEngine?.postNewFrame(ctx)
}

fun hookItemAdd(ctx: Context, bb: Rect, id: ID) {

    val engine = hookingEngine
    if (engine == null || engine.uiContextActive !== ctx)
        return

    assert(id != 0)
    val g = ctx
    val window = g.currentWindow!!

    // FIXME-OPT: Early out if there are no active Locate/Gather tasks.

    // Locate Tasks
    engine.findLocateTask(id)?.let { task ->
        task.result.also {
            it.timestampMain = g.frameCount
            it.id = id
            it.parentID = window.idStack.last()
            it.window = window
            it.rectFull put bb
            it.rectClipped put bb
            it.rectClipped clipWithFull window.clipRect      // This two step clipping is important, we want RectClipped to stays within RectFull
            it.rectClipped clipWithFull it.rectFull
            it.navLayer = window.dc.navLayerCurrent
            it.depth = 0
            it.statusFlags = when (window.dc.lastItemId) {
                id -> window.dc.lastItemStatusFlags
                else -> ItemStatusFlag.None.i
            }
        }
    }

    // Gather Task (only 1 can be active)
    if (engine.gatherTask.parentID != 0 && window.dc.navLayerCurrent == NavLayer.Main) { // FIXME: Layer filter?
        val gatherParentId = engine.gatherTask.parentID
        var depth = -1
        if (gatherParentId == window.idStack.last())
            depth = 0
        else {
            val maxDepth = window.idStack.size min engine.gatherTask.depth
            for (nDepth in 1 until maxDepth)
                if (window.idStack[window.idStack.lastIndex - nDepth] == gatherParentId) {
                    depth = nDepth
                    break
                }
        }
        if (depth != -1)
            engine.gatherTask.lastItemInfo = engine.gatherTask.outList!!.getOrAddByKey(id).also {
                it.timestampMain = engine.frameCount
                it.id = id
                it.parentID = window.idStack.last()
                it.window = window
                it.rectFull put bb
                it.rectClipped put bb
                it.rectClipped clipWithFull window.clipRect      // This two step clipping is important, we want RectClipped to stays within RectFull
                it.rectClipped clipWithFull it.rectFull
                it.navLayer = window.dc.navLayerCurrent
                it.depth = depth
            }
    }
}

// label is optional
fun hookItemInfo(ctx: Context, id: ID, label: String, flags: ItemStatusFlags) {

    val engine = hookingEngine
    if (engine == null || engine.uiContextActive !== ctx)
        return

    assert(id != 0)
    val g = ctx
    val window = g.currentWindow!!
    assert(window.dc.lastItemId == id || window.dc.lastItemId == 0)

    // Update Locate Task status flags
    engine.findLocateTask(id)?.let { task ->
        task.result.also {
            it.timestampStatus = g.frameCount
            it.statusFlags = flags
            if (label.isNotEmpty())
                it.debugLabel = label
        }
    }

    // Update Gather Task status flags
    engine.gatherTask.lastItemInfo?.let {
        if (it.id == id) {
            it.timestampStatus = g.frameCount
            it.statusFlags = flags
            if (label.isNotEmpty())
                it.debugLabel = label
        }
    }
}

// Forward core/user-land text to test log
fun hookLog(ctx: Context, fmt: String) {
    val engine = hookingEngine
    if (engine == null || engine.uiContextActive !== ctx)
    return

    engine.testContext!!.logEx(TestVerboseLevel.Debug, TestLogFlag.None.i, fmt)
}

//fun hookAssertfunc(expr: String, const char* file, const char* function, int line)
//{
//    if (ImGuiTestEngine* engine = GImGuiHookingEngine)
//    {
//        if (ImGuiTestContext* ctx = engine->TestContext)
//        {
//            ctx->LogError("Assert: '%s'", expr);
//            ctx->LogWarning("In %s:%d, function %s()", file, line, function);
//            if (ImGuiTest* test = ctx->Test)
//            ctx->LogWarning("While running test: %s %s", test->Category, test->Name);
//        }
//    }
//
//    // Consider using github.com/scottt/debugbreak
//    #ifdef _MSC_VER
//        __debugbreak();
//    #else
//    IM_ASSERT(0);
//    #endif
//}
