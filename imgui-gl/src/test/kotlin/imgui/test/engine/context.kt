package imgui.test.engine

import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.endTabBar
import imgui.ImGui.findWindowByID
import imgui.ImGui.focusWindow
import imgui.ImGui.isItemActivated
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemDeactivated
import imgui.ImGui.isItemDeactivatedAfterEdit
import imgui.ImGui.isItemEdited
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isItemVisible
import imgui.ImGui.popID
import imgui.ImGui.treePop
import imgui.classes.Context
import imgui.internal.InputSource
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.lengthSqr
import imgui.test.IMGUI_HAS_TABLE
import imgui.test.engine.core.*
import io.kotlintest.shouldBe

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
    val strLarge = ArrayList<Int>()

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
        strLarge.clear()
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
    private var userData: Any? = null
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
    var opFlags_ = TestOpFlag.None.i
    val clipboard = ArrayList<Byte>()

    // Performance
    var perfRefDt = -1.0
    var perfStressAmount = 0                   // Convenience copy of engine->IO.PerfStressAmount

    // Main control
    fun finish() {
        if (runFlags has TestRunFlag.NoTestFunc.i)
            return
        val test = this.test!!
        if (test.status == TestStatus.Running)
            test.status = TestStatus.Success
    }

    val isError
        get() = test!!.status == TestStatus.Error || abort
    val isFirstFrame
        get() = frameCount == firstFrameCount

    fun setGuiFuncEnabled(v: Boolean) {
        runFlags = when {
            v -> runFlags wo TestRunFlag.NoGuiFunc
            else -> runFlags or TestRunFlag.NoGuiFunc
        }
    }

    // FIXME-ERRORHANDLING: Can't recover from inside BeginTabItem/EndTabItem yet.
    // FIXME-ERRORHANDLING: Can't recover from interleaved BeginTabBar/Begin
    // FIXME-ERRORHANDLING: Once this function is amazingly sturdy, we should make it a ImGui:: function.. See #1651
    // FIXME-ERRORHANDLING: This is flawed as we are not necessarily End/Popping things in the right order.
    fun recoverFromUiContextErrors() {

        val g = uiContext!!
        val test = test!!

        // If we are _already_ in a test error state, recovering is normal so we'll hide the log.
        val verbose = test.status != TestStatus.Error

        while (g.currentWindowStack.size > 1) {
            if (IMGUI_HAS_TABLE) {
//                val table = g.currentTable
//                if (table && (table->OuterWindow == g.CurrentWindow || table->InnerWindow == g.CurrentWindow))
//                {
//                    if (verbose) LogWarning("Recovered from missing EndTable() call.")
//                    ImGui::EndTable()
//                }
            }

            while (g.currentTabBar != null) {
                if (verbose) logWarning("Recovered from missing EndTabBar() call.")
                endTabBar()
            }

            val win = g.currentWindow!!

            while (win.dc.treeDepth > 0) {
                if (verbose) logWarning("Recovered from missing TreePop() call.")
                treePop()
            }

            while (win.dc.groupStack.size > win.dc.stackSizesBackup[1]) {
                if (verbose) logWarning("Recovered from missing EndGroup() call.")
                endGroup()
            }

            while (win.idStack.size > win.dc.stackSizesBackup[0]) {
                if (verbose) logWarning("Recovered from missing PopID() call.")
                popID()
            }

            if (win.flags has WindowFlag._ChildWindow) {
                if (verbose) logWarning("Recovered from missing EndChild() call.")
                endChild()
            } else {
                if (verbose) logWarning("Recovered from missing End() call.")
                end()
            }
        }
    }

    fun <T> getUserData(): T {
        assert(userData != null)
        return userData as T
    } // FIXME: Assert to compare sizes

    // Logging
    fun logEx(level_: TestVerboseLevel, flags: TestLogFlags, fmt: String, vararg args: Any) {

        var level = level_
        assert(level > TestVerboseLevel.Silent)

        if (level == TestVerboseLevel.Debug && actionDepth > 1)
            level = TestVerboseLevel.Trace

        // Log all messages that we may want to print in future.
        if (engineIO!!.configVerboseLevelOnError < level)
            return

        val log = test!!.testLog
        val prevSize = log.buffer.size()

        //const char verbose_level_char = ImGuiTestEngine_GetVerboseLevelName(level)[0];
        //if (flags & ImGuiTestLogFlags_NoHeader)
        //    log->Buffer.appendf("[%c] ", verbose_level_char);
        //else
        //    log->Buffer.appendf("[%c] [%04d] ", verbose_level_char, ctx->FrameCount);
        if (flags hasnt TestLogFlag.NoHeader)
            log.buffer.appendf("[%04d] ", frameCount)

        if (level >= TestVerboseLevel.Debug)
            log.buffer.appendf("-- %*s", max(0, (actionDepth - 1) * 2), "")
        log.buffer.appendfv(fmt, args)
        log.buffer.append("\n")

        log.updateLineOffsets(engineIO, level, log.buffer.begin() + prevSize)
        logToTTY(level, log.buffer.c_str() + prevSize)
    }

    fun logToTTY(level: TestVerboseLevel, message: String) {

        assert(level > VerboseLevel.Silent && level < TestVerboseLevel.COUNT)

        if (!engineIO.configLogToTTY)
            return

        val test = test!!
        val log = test.testLog

        if (test.status == TestStatus.Error) {
            // Current test failed.
            if (!log.cachedLinesPrintedToTTY) {
                // Print current message and all previous logged messages.
                log.cachedLinesPrintedToTTY = true
                for (line in log.lineInfo) {
                    char * line_beg = log->Buffer.Buf.Data+line_info.LineOffset
                    char * line_end = strchr(line_beg, '\n')
                    char line_end_bkp = * (line_end + 1)
                    *(line_end + 1) = 0                            // Terminate line temporarily to avoid extra copying.
                    LogToTTY(line_info.Level, line_beg)
                    *(line_end + 1) = line_end_bkp                 // Restore new line after printing.
                }
                return                                             // This process included current line as well.
            }
            // Otherwise print only current message. If we are executing here log level already is within range of
            // ConfigVerboseLevelOnError setting.
        } else if (engineIO.configVerboseLevel < level)
        // Skip printing messages of lower level than configured.
            return

        val color = when (level) {
            TestVerboseLevel.Warning -> osConsoleTextColor_BrightYellow
            TestVerboseLevel.Error -> osConsoleTextColor_BrightRed
            else -> osConsoleTextColor_White
        }
        osConsoleSetTextColor(osConsoleStream_StandardOutput, color)
        println(message)
        osConsoleSetTextColor(ImOsConsoleStream_StandardOutput, ImOsConsoleTextColor_White)
    }

    fun logDebug(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Debug, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Debug or ImGuiTestVerboseLevel_Trace depending on context depth
    fun logInfo(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Info, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Info
    fun logWarning(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Warning, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Warning
    fun logError(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Error, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Error
    fun logDebugInfo() {
        val itemHoveredId = uiContext!!.hoveredIdPreviousFrame
        val itemActiveId = uiContext!!.activeId
        val itemHoveredInfo = if (itemHoveredId != 0) engine!!.itemLocate(itemHoveredId, "") else null
        val itemActiveInfo = if (itemActiveId != 0) engine!!.itemLocate(itemActiveId, "") else null
        val hovered = itemHoveredInfo?.debugLabel ?: ""
        val active = itemActiveInfo?.debugLabel ?: ""
        logDebug("Hovered: 0x%08X (\"$hovered\"), Active:  0x%08X(\"$active\")", itemHoveredId, itemActiveId)
    }


    // Yield, Timing

    fun yield() = engine!!.yield()
    fun yieldFrames(count_: Int) {
        var count = count_
        while (count > 0) {
            engine!!.yield()
            count--
        }
    }

    fun yieldUntil(frameCount: Int) {
        while (frameCount < frameCount)
            engine!!.yield()
    }

    fun sleep(time_: Float) {
        var time = time_
        if (isError)
            return

        if (engineIO!!.configRunFast)
        //ImGuiTestEngine_AddExtraTime(Engine, time); // We could add time, for now we have no use for it...
            engine!!.yield()
        else
            while (time > 0f && !abort) {
                engine!!.yield()
                time -= uiContext!!.io.deltaTime
            }
    }

    // Sleep for a given clock time from the point of view of the imgui context, without affecting wall clock time of the running application.
    fun sleepNoSkip(time_: Float, frameTimeStep: Float) {
        var time = time_
        if (isError)
            return

        while (time > 0f && !abort) {
            engine!!.setDeltaTime(frameTimeStep)
            engine!!.yield()
            time -= uiContext!!.io.deltaTime
        }
    }

    fun sleepShort() = sleep(0.3f)


    // Windows
    // FIXME-TESTS: Refactor this horrible mess... perhaps all functions should have a ImGuiTestRef defaulting to empty?


    // FIXME-TESTS: May be to focus window when docked? Otherwise locate request won't even see an item?
    fun windowRef (ref: TestRef) {

//        IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
        logDebug("WindowRef '${ref.path ?: "NULL"}' %08X", ref.id)

        ref.path?.let {
//            size_t len = strlen(ref.Path)
//            IM_ASSERT(len < IM_ARRAYSIZE(RefStr) - 1)

            it.toByteArray(refStr)
            refID = hashDecoratedPath(ref.path, 0)
        } ?: run {
            refStr[0] = 0
            refID = ref.id
        }

        // Automatically uncollapse by default
        if (opFlags_ hasnt TestOpFlag.NoAutoUncollapse)
            getWindowByRef("").let { windowAutoUncollapse(it) }
    }
    fun windowClose (ref: TestRef) {
        if (isError)
            return

        IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
        logDebug("WindowClose")
        itemClick(getID("#CLOSE", ref))
    }
    fun windowCollapse (window: Window?, collapsed: Boolean) {
        if (isError)
            return
        if (window == null)
            return

        IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
        logDebug("WindowSetCollapsed $collapsed")
        //ImGuiWindow* window = GetWindowByRef(ref);
        //if (window == NULL)
        //{
        //    IM_ERRORF_NOHDR("Unable to find Ref window: %s / %08X", RefStr, RefID);
        //    return;
        //}

        if (window.collapsed != collapsed)        {
            var opFlags = opFlags_
            val backupOpFlags = opFlags
            opFlags = opFlags or TestOpFlag.NoAutoUncollapse
            itemClick(getID("#COLLAPSE", window.id))
            opFlags = backupOpFlags
            yield()
            CHECK(window.collapsed == collapsed)
        }
    }

    fun windowAutoUncollapse (window: Window) {
        if (window.collapsed)        {
            IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
            logDebug("Uncollapse window '${window.name}'")
            windowCollapse(window, false)
            window.collapsed shouldBe false
        }
    }

    // FIXME-TESTS: Ideally we would aim toward a clickable spot in the window.
    fun windowFocus (ref: TestRef) {

        IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
        val desc = TestRefDesc(ref, null)
        logDebug("FocusWindow('$desc')")

        val windowId = getID(ref)
        val window = findWindowByID(windowId)
        CHECK_SILENT(window != null)
        window?.let {
            focusWindow(it)
            yield()
        }
    }

    fun windowMove (ref: TestRef, inputPos: Vec2, pivot: Vec2 = Vec2()) {

        if (isError)
            return

        val window = getWindowByRef(ref)
        CHECK_SILENT(window != null)
        window!!

        IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
        logDebug("WindowMove ${window.name} (%.1f,%.1f) ", inputPos.x, inputPos.y)
        val targetPos = floor(inputPos - pivot * window.size)
        if ((targetPos - window.pos).lengthSqr < 0.001f)
        return

        windowBringToFront(window)
        windowCollapse(window, false)

        val h = ImGui.frameHeight

        // FIXME-TESTS: Need to find a -visible- click point
        MouseMoveToPos(window->Pos + ImVec2(h * 2.0f, h * 0.5f))
        //IM_CHECK_SILENT(UiContext->HoveredWindow == window);  // FIXME-TESTS:
        MouseDown(0)

        // Disable docking
        #if IMGUI_HAS_DOCK
        if (UiContext->IO.ConfigDockingWithShift)
        KeyUpMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift)
        else
        KeyDownMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift)
        #endif

        ImVec2 delta = target_pos - window->Pos
        MouseMoveToPos(Inputs->MousePosValue + delta)
        Yield()

        MouseUp()
        #if IMGUI_HAS_DOCK
        KeyUpMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift)
        #endif
    }
    void WindowResize (ImGuiTestRef ref, ImVec2 sz)
    void WindowMoveToMakePosVisible (ImGuiWindow * window, ImVec2 pos)
    bool WindowBringToFront (ImGuiWindow * window, ImGuiTestOpFlags flags = ImGuiTestOpFlags_None)
    void PopupClose ()
    fun getWindowByRef(ref: TestRef): Window? {
        val windowId = getID(ref)
        return findWindowByID(windowId)
    }
    ImGuiTestRef GetFocusWindowRef ()

    // ID
    ImGuiID GetID (ImGuiTestRef ref)
    ImGuiID GetID (ImGuiTestRef ref, ImGuiTestRef seed_ref)
    ImGuiID GetIDByInt (int n)
    ImGuiID GetIDByInt (int n, ImGuiTestRef seed_ref)
    ImGuiID GetIDByPtr (void * p)
    ImGuiID GetIDByPtr (void * p, ImGuiTestRef seed_ref)

    // Misc
    ImVec2 GetMainViewportPos ()
    bool CaptureAddWindow (ImGuiTestRef ref)
    bool CaptureScreenshot ()

    // Mouse inputs
    void MouseMove (ImGuiTestRef ref, ImGuiTestOpFlags flags = ImGuiTestOpFlags_None)
    void MouseMoveToPos (ImVec2 pos)
    void MouseMoveToPosInsideWindow (ImVec2 * pos, ImGuiWindow* window)
    void MouseClick (int button = 0)
    void MouseDoubleClick (int button = 0)
    void MouseDown (int button = 0)
    void MouseUp (int button = 0)
    void MouseLiftDragThreshold (int button = 0)

    // Keyboard inputs
    void KeyDownMap (ImGuiKey key, int mod_flags = 0)
    void KeyUpMap (ImGuiKey key, int mod_flags = 0)
    void KeyPressMap (ImGuiKey key, int mod_flags = 0, int count = 1)
    void KeyChars (const char * chars)
    void KeyCharsAppend (const char * chars)
    void KeyCharsAppendEnter (const char * chars)
    void KeyCharsReplace (const char * chars)
    void KeyCharsReplaceEnter (const char * chars)

    // Navigation inputs
    void SetInputMode (ImGuiInputSource input_mode)
    void NavKeyPress (ImGuiNavInput input)
    void NavMoveTo (ImGuiTestRef ref)
    void NavActivate ()  // Activate current selected item. Same as pressing [space].
    void NavInput ()     // Press ImGuiNavInput_Input (e.g. Triangle) to turn a widget into a text input

    // Scrolling
    void ScrollToY (ImGuiTestRef ref, float scroll_ratio_y = 0.5f)
    void ScrollVerifyScrollMax (ImGuiWindow * window)

    // Low-level queries
    ImGuiTestItemInfo * ItemLocate(ImGuiTestRef ref, ImGuiTestOpFlags flags = ImGuiTestOpFlags_None)
    void GatherItems (ImGuiTestItemList * out_list, ImGuiTestRef parent, int depth = -1)

    // Item/Widgets manipulation
    void ItemAction (ImGuiTestAction action, ImGuiTestRef ref, void* action_arg = NULL)
    void ItemClick (ImGuiTestRef ref, int button = 0)
    { ItemAction(ImGuiTestAction_Click, ref, (void *)(intptr_t) button); }
    void ItemDoubleClick (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_DoubleClick, ref); }
    void ItemCheck (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_Check, ref); }
    void ItemUncheck (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_Uncheck, ref); }
    void ItemOpen (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_Open, ref); }
    void ItemClose (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_Close, ref); }
    void ItemInput (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_Input, ref); }
    void ItemNavActivate (ImGuiTestRef ref)
    { ItemAction(ImGuiTestAction_NavActivate, ref); }

    void ItemActionAll (ImGuiTestAction action, ImGuiTestRef ref_parent, int depth = -1, int passes = -1)
    void ItemOpenAll (ImGuiTestRef ref_parent, int depth = -1, int passes = -1)
    { ItemActionAll(ImGuiTestAction_Open, ref_parent, depth, passes); }
    void ItemCloseAll (ImGuiTestRef ref_parent, int depth = -1, int passes = -1)
    { ItemActionAll(ImGuiTestAction_Close, ref_parent, depth, passes); }

    void ItemHold (ImGuiTestRef ref, float time)
    void ItemHoldForFrames (ImGuiTestRef ref, int frames)
    void ItemDragAndDrop (ImGuiTestRef ref_src, ImGuiTestRef ref_dst)
    void ItemVerifyCheckedIfAlive (ImGuiTestRef ref, bool checked)

    // Menus
    void MenuAction (ImGuiTestAction action, ImGuiTestRef ref)
    void MenuActionAll (ImGuiTestAction action, ImGuiTestRef ref_parent)
    void MenuClick (ImGuiTestRef ref)
    { MenuAction(ImGuiTestAction_Click, ref); }
    void MenuCheck (ImGuiTestRef ref)
    { MenuAction(ImGuiTestAction_Check, ref); }
    void MenuUncheck (ImGuiTestRef ref)
    { MenuAction(ImGuiTestAction_Uncheck, ref); }
    void MenuCheckAll (ImGuiTestRef ref_parent)
    { MenuActionAll(ImGuiTestAction_Check, ref_parent); }
    void MenuUncheckAll (ImGuiTestRef ref_parent)
    { MenuActionAll(ImGuiTestAction_Uncheck, ref_parent); }

    // Docking
    #ifdef IMGUI_HAS_DOCK
    void DockWindowInto (const char * window_src, const char* window_dst, ImGuiDir split_dir = ImGuiDir_None)
    void DockMultiClear (const char * window_name, ...)
    void DockMultiSet (ImGuiID dock_id, const char* window_name, ...)
    ImGuiID DockMultiSetupBasic (ImGuiID dock_id, const char* window_name, ...)
    bool DockIdIsUndockedOrStandalone (ImGuiID dock_id)
    void UndockNode (ImGuiID dock_id)
    #endif

    // Performances
    void PerfCalcRef ()
    void PerfCapture ()
}

// Helper to increment/decrement the function depth (so our log entry can be padded accordingly)
#define IM_TOKENPASTE(x, y)     x ## y
#define IM_TOKENPASTE2(x, y)    IM_TOKENPASTE(x, y)
#define IMGUI_TEST_CONTEXT_REGISTER_DEPTH(_THIS)        ImGuiTestContextDepthScope IM_TOKENPASTE2(depth_register, __LINE__)(_THIS)

struct ImGuiTestContextDepthScope
{
    ImGuiTestContext * TestContext
    ImGuiTestContextDepthScope(ImGuiTestContext * ctx) { TestContext = ctx; TestContext->ActionDepth++; }
    ~ImGuiTestContextDepthScope { TestContext -> ActionDepth--; }
}