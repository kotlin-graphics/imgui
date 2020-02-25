package engine.context

import engine.CaptureArgs
import engine.core.TestEngine
import engine.core.*
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.isItemActivated
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemDeactivated
import imgui.ImGui.isItemDeactivatedAfterEdit
import imgui.ImGui.isItemEdited
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isItemVisible
import imgui.classes.Context
import imgui.internal.*

//-------------------------------------------------------------------------
// ImGuiTestContext
// This is the interface that most tests will interact with.
//-------------------------------------------------------------------------

// Note: keep in sync with GetActionName()
enum class TestAction {
    Unknown, Click, DoubleClick, Check, Uncheck, Open, Close, Input, NavActivate;

    companion object {
        val COUNT = values().size
    }
}

// Helper struct to store various query-able state of an item.
// This facilitate interactions between GuiFunc <> TestFunc, since those state are frequently used.
class TestGenericStatus {
    var ret = 0
    var hovered = 0
    var active = 0
    var focused = 0
    var clicked = 0
    var visible = 0
    var edited = 0
    var activated = 0
    var deactivated = 0
    var deactivatedAfterEdit = 0

    fun clear() {
        ret = 0
        hovered = 0
        active = 0
        focused = 0
        clicked = 0
        visible = 0
        edited = 0
        activated = 0
        deactivated = 0
        deactivatedAfterEdit = 0
    }

    fun querySet(retVal: Boolean = false) {
        clear(); queryInc(retVal); }

    fun queryInc(retVal: Boolean = false) {
        ret += retVal.i
        hovered += isItemHovered().i
        active += isItemActive.i
        focused += isItemFocused.i
        clicked += isItemClicked().i
        visible += isItemVisible.i
        edited += isItemEdited.i
        activated += isItemActivated.i
        deactivated += isItemDeactivated.i
        deactivatedAfterEdit += isItemDeactivatedAfterEdit.i
    }
}

enum class TestActiveFunc { None, GuiFunc, TestFunc }

// Generic structure with varied data. This is useful for tests to quickly share data between the GUI functions and the Test function.
// This is however totally optional. Using SetUserDataType() it is possible to store custom data on the stack and read from it as UserData.
class TestGenericVars {
//    var byte1: Byte = 0
    var int1 = 0
    var int2 = 0
    val intArray = IntArray(10)
    var float1 = 0f
    var float2 = 0f
    val floatArray = FloatArray(10)
    var bool1 = false
    var bool2 = false
    val boolArray = BooleanArray(10)
    val vec2 = Vec2()
    val vec4 = Vec4()
    val vec4Array = Array(10) { Vec4() }
    var id: ID = 0
    var idArray = IntArray(10)
    val str1 = ByteArray(256)
    val str2 = ByteArray(256)
    var strLarge = ByteArray(0)

    //    void * Ptr1
//    void * Ptr2
//    void * PtrArray[10]
    var dockId: ID = 0
    var status = TestGenericStatus()

    fun clear() {
        int1 = 0
        int2 = 0
        intArray.fill(0)
        float1 = 0f
        float2 = 0f
        floatArray.fill(0f)
        bool1 = false
        bool2 = false
        boolArray.fill(false)
        vec2 put 0f
        vec4 put 0f
        vec4Array.forEach { it put 0f }
        id = 0
        idArray.fill(0)
        str1.fill(0)
        str2.fill(0)
        strLarge = ByteArray(0)
    }

    fun clearInts() {
        int1 = 0
        int2 = 0
        intArray.fill(0)
    }

    fun clearBools() {
        bool1 = false
        bool2 = false
        boolArray.fill(false)
    }
}

class TestContext {
    var engine: TestEngine? = null
    var test: Test? = null
    var engineIO: TestEngineIO? = null
    var uiContext: Context? = null
    var inputs: TestInputs? = null
    var gatherTask: TestGatherTask? = null
    var runFlags = TestRunFlag.None.i
    var activeFunc = TestActiveFunc.None  // None/GuiFunc/TestFunc
    internal var userData: Any? = null
    var frameCount = 0                         // Test frame count (restarts from zero every time)
    var firstFrameCount = 0                    // First frame where Test is running. After warm-up. This is generally -2 or 0 depending on whether we have warm up enabled
    var runningTime = 0.0                     // Amount of wall clock time the Test has been running. Used by safety watchdog.
    var actionDepth = 0
    var abort = false
    var hasDock = false                        // #ifdef IMGUI_HAS_DOCK
    var captureArgs = CaptureArgs()

    // Commonly user exposed state for the ctx-> functions
    var genericVars = TestGenericVars()
    var refStr = ByteArray(256)                    // Reference window/path for ID construction
    var refID: ID = 0
    var inputMode = InputSource.Mouse
    var opFlags = TestOpFlag.None.i
    val clipboard = ArrayList<Byte>()

    // Performance
    var perfRefDt = -1.0
    var perfStressAmount = 0                   // Convenience copy of engine->IO.PerfStressAmount

    // -> corresponding .kt file

    // Main control

    // Logging

    // Yield, Timing

    // Windows
    // FIXME-TESTS: Refactor this horrible mess... perhaps all functions should have a ImGuiTestRef defaulting to empty?

    // ID

    // Misc

    // Mouse inputs

    // Keyboard inputs

    // Navigation inputs

    // Scrolling

    // Low-level queries

    // Item/Widgets manipulation

    // Menus

    // Docking
//    #ifdef IMGUI_HAS_DOCK
//    void DockWindowInto (const char * window_src, const char* window_dst, ImGuiDir split_dir = ImGuiDir_None)
//    void DockMultiClear (const char * window_name, ...)
//    void DockMultiSet (ImGuiID dock_id, const char* window_name, ...)
//    ImGuiID DockMultiSetupBasic (ImGuiID dock_id, const char* window_name, ...)
//    bool DockIdIsUndockedOrStandalone (ImGuiID dock_id)
//    void UndockNode (ImGuiID dock_id)
//    #endif

    // Performances

}

// Helper to increment/decrement the function depth (so our log entry can be padded accordingly)
//#define IM_TOKENPASTE(x, y)     x ## y
//#define IM_TOKENPASTE2(x, y)    IM_TOKENPASTE(x, y)
//#define IMGUI_TEST_CONTEXT_REGISTER_DEPTH(_THIS)        ImGuiTestContextDepthScope IM_TOKENPASTE2(depth_register, __LINE__)(_THIS)

inline fun <R>TestContext.REGISTER_DEPTH(block: () -> R): R {
    actionDepth++
    val res = block()
    actionDepth--
    return res
}

//struct ImGuiTestContextDepthScope
//{
//    ImGuiTestContext * TestContext
//    ImGuiTestContextDepthScope(ImGuiTestContext * ctx) { TestContext = ctx; TestContext->ActionDepth++; }
//    ~ImGuiTestContextDepthScope { TestContext -> ActionDepth--; }
//}