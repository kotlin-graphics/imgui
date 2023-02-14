package imgui.internal.sections

import imgui.ID
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.Rect

//-----------------------------------------------------------------------------
// [SECTION] Test Engine specific hooks (imgui_test_engine)
//-----------------------------------------------------------------------------

typealias TestEngineHook_ItemAdd = (ctx: Context, bb: Rect, id: ID) -> Unit
typealias TestEngineHook_ItemInfo = (ctx: Context, id: ID, label: String?, flags: ItemStatusFlags) -> Unit
typealias TestEngineHook_Log = (ctx: Context, fmt: String, args: Array<out Any>) -> Unit
typealias TestEngine_FindItemDebugLabel = (ctx: Context, id: ID) -> String?

lateinit var testEngineHook_ItemAdd: TestEngineHook_ItemAdd
lateinit var testEngineHook_ItemInfo: TestEngineHook_ItemInfo

lateinit var testEngineHook_Log: TestEngineHook_Log
lateinit var testEngine_FindItemDebugLabel: TestEngine_FindItemDebugLabel

/**  Register item bounding box */
fun IMGUI_TEST_ENGINE_ITEM_ADD(bb: Rect, id: ID) {
    if (g.testEngineHookItems)
        testEngineHook_ItemAdd(g, bb, id)
}

/** Register item label and status flags (optional)} */
fun IMGUI_TEST_ENGINE_ITEM_INFO(id: ID, label: String, flags: ItemFlags) {
    if (g.testEngineHookItems)
        testEngineHook_ItemInfo(g, id, label, flags)
}

/** Custom log entry from user land into test log */
fun IMGUI_TEST_ENGINE_LOG(fmt: String, vararg args: Any) {
    if (g.testEngineHookItems)
        testEngineHook_Log(g, fmt, args)
}
