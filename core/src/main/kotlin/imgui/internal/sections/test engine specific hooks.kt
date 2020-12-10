package imgui.internal.sections

import imgui.DataType
import imgui.ID
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.Rect

//-----------------------------------------------------------------------------
// [SECTION] Test Engine specific hooks (imgui_test_engine)
//-----------------------------------------------------------------------------

typealias TestEngineHook_ItemAdd = (ctx: Context, bb: Rect, id: ID) -> Unit
typealias TestEngineHook_ItemInfo = (ctx: Context, id: ID, label: String, flags: ItemStatusFlags) -> Unit
//typealias TestEngineHook_IdInfo = (ctx: Context, dataType: DataType, id: ID, dataId: Any) -> Unit
typealias TestEngineHook_IdInfo = (ctx: Context, dataType: DataType, id: ID, dataId: Any, dataIdEnd: Any?) -> Unit
typealias TestEngineHook_Log = (ctx: Context, fmt: String) -> Unit

lateinit var testEngineHook_ItemAdd: TestEngineHook_ItemAdd
lateinit var testEngineHook_ItemInfo: TestEngineHook_ItemInfo

//typealias TestEngineHook_IdInfo = (ctx: Context, dataType: DataType, id: ID, dataId: Any) -> Unit
lateinit var testEngineHook_IdInfo: TestEngineHook_IdInfo
lateinit var testEngineHook_Log: TestEngineHook_Log

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
        testEngineHook_Log(g, fmt.format(*args))
}

//fun IMGUI_TEST_ENGINE_ID_INFO(_ID,_TYPE,_DATA)          if (g.TestEngineHookIdInfo == id) ImGuiTestEngineHook_IdInfo(&g, _TYPE, _ID, (const void*)(_DATA));

fun IMGUI_TEST_ENGINE_ID_INFO(id: ID, type: DataType, data: Any, data2: Any? = null) {
    if (g.testEngineHookIdInfo == id)
        testEngineHook_IdInfo(g, type, id, data, data2)
}
