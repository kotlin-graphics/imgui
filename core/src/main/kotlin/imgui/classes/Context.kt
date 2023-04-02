package imgui.classes

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.addSettingsHandler
import imgui.ImGui.callHooks
import imgui.ImGui.localizeRegisterEntries
import imgui.ImGui.saveIniSettingsToDisk
import imgui.ImGui.tableSettingsAddSettingsHandler
import imgui.api.g
import imgui.api.gImGui
import imgui.font.Font
import imgui.font.FontAtlas
import imgui.internal.DrawChannel
import imgui.internal.classes.*
import imgui.internal.hashStr
import imgui.internal.sections.*
import imgui.static.*
import java.io.File
import java.nio.ByteBuffer
import java.util.*

/** Main Dear ImGui context
 *
 *  Dear ImGui context (opaque structure, unless including imgui_internal.h) */
class Context(sharedFontAtlas: FontAtlas? = null) {

    var initialized = false

    /** Io.Fonts-> is owned by the ImGuiContext and will be destructed along with it.   */
    var fontAtlasOwnedByContext = sharedFontAtlas == null

    var io = IO(sharedFontAtlas)

    /** Input events which will be tricked/written into IO structure. */
    val inputEventsQueue = ArrayList<InputEvent>()

    /** Past input events processed in NewFrame(). This is to allow domain-specific application to access e.g mouse/pen trail. */
    val inputEventsTrail = ArrayList<InputEvent>()

    var style = Style()

    lateinit var font: Font

    /** (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize(). Text height for current window. */
    var fontSize = 0f

    /** (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Base text height.    */
    var fontBaseSize = 0f

    var drawListSharedData = DrawListSharedData()

    var time = 0.0

    var frameCount = 0

    var frameCountEnded = -1

    var frameCountRendered = -1

    /** Set by NewFrame(), cleared by EndFrame() */
    var withinFrameScope = false

    /** Set by NewFrame(), cleared by EndFrame() when the implicit debug window has been pushed */
    var withinFrameScopeWithImplicitWindow = false

    /** Set within EndChild() */
    var withinEndChild = false

    /** Request full GC */
    var gcCompactAll = false

    /** Will call test engine hooks: ImGuiTestEngineHook_ItemAdd(), ImGuiTestEngineHook_ItemInfo(), ImGuiTestEngineHook_Log() */
    var testEngineHookItems = false

    /** Test engine user data */
    var testEngine: Any? = null


    // Windows state

    /** Windows, sorted in display order, back to front */
    val windows = ArrayList<Window>()

    /** Root windows, sorted in focus order, back to front. */
    val windowsFocusOrder = ArrayList<Window>()

    val windowsTempSortBuffer = ArrayList<Window>()

    val currentWindowStack = Stack<WindowStackData>()

    /** Map window's ImGuiID to ImGuiWindow* */
    val windowsById = mutableMapOf<Int, Window>()

    /** Number of unique windows submitted by frame */
    var windowsActiveCount = 0

    /** Padding around resizable windows for which hovering on counts as hovering the window == ImMax(style.TouchExtraPadding, WINDOWS_HOVER_PADDING) */
    var windowsHoverPadding = Vec2()

    /** Window being drawn into    */
    var currentWindow: Window? = null

    /** Window the mouse is hovering. Will typically catch mouse inputs. */
    var hoveredWindow: Window? = null

    /** Hovered window ignoring MovingWindow. Only set if MovingWindow is set. */
    var hoveredWindowUnderMovingWindow: Window? = null

    /** Track the window we clicked on (in order to preserve focus). The actual window that is moved is generally MovingWindow->RootWindow. */
    var movingWindow: Window? = null

    /** Track the window we started mouse-wheeling on. Until a timer elapse or mouse has moved, generally keep scrolling the same window even if during the course of scrolling the mouse ends up hovering a child window. */
    var wheelingWindow: Window? = null

    val wheelingWindowRefMousePos: Vec2 = Vec2()

    /** This may be set one frame before WheelingWindow is != NULL */
    var wheelingWindowStartFrame = -1

    var wheelingWindowReleaseTimer = 0f

    val wheelingWindowWheelRemainder = Vec2()
    val wheelingAxisAvg = Vec2()


    // Item/widgets state and tracking information

    /** Will call core hooks: DebugHookIdInfo() from GetID functions, used by Stack Tool [next HoveredId/ActiveId to not pull in an extra cache-line] */
    var debugHookIdInfo: ID = 0

    /** Hovered widget, filled during the frame  */
    var hoveredId: ID = 0

    var hoveredIdPreviousFrame: ID = 0

    var hoveredIdAllowOverlap = false

    /** At least one widget passed the rect test, but has been discarded by disabled flag or popup inhibit.
     *  May be true even if HoveredId == 0. */
    var hoveredIdDisabled = false

    /** Measure contiguous hovering time */
    var hoveredIdTimer = 0f

    /** Measure contiguous hovering time where the item has not been active */
    var hoveredIdNotActiveTimer = 0f

    /** Active widget   */
    var activeId: ID = 0

    /** Active widget has been seen this frame (we can't use a bool as the ActiveId may change within the frame) */
    var activeIdIsAlive: ID = 0

    var activeIdTimer = 0f

    /** Set at the time of activation for one frame */
    var activeIdIsJustActivated = false

    /** Active widget allows another widget to steal active id (generally for overlapping widgets, but not always)   */
    var activeIdAllowOverlap = false

    /** Disable losing active id if the active id window gets unfocused. */
    var activeIdNoClearOnFocusLoss = false

    /** Track whether the active id led to a press (this is to allow changing between PressOnClick and PressOnRelease without pressing twice). Used by range_select branch. */
    var activeIdHasBeenPressedBefore = false

    /** Was the value associated to the widget edited over the course of the Active state. */
    var activeIdHasBeenEditedBefore = false

    var activeIdHasBeenEditedThisFrame = false

    /** Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior) */
    var activeIdClickOffset = Vec2(-1)

    var activeIdWindow: Window? = null

    /** Activating with mouse or nav (gamepad/keyboard) */
    var activeIdSource = InputSource.None

    var activeIdMouseButton = MouseButton.None

    var activeIdPreviousFrame: ID = 0

    var activeIdPreviousFrameIsAlive = false

    var activeIdPreviousFrameHasBeenEdited = false

    var activeIdPreviousFrameWindow: Window? = null

    /** Store the last non-zero ActiveId, useful for animation. */
    var lastActiveId: ID = 0

    /** Store the last non-zero ActiveId timer since the beginning of activation, useful for animation. */
    var lastActiveIdTimer = 0f

    // [EXPERIMENTAL] Key/Input Ownership + Shortcut Routing system
    // - The idea is that instead of "eating" a given key, we can link to an owner.
    // - Input query can then read input by specifying ImGuiKeyOwner_Any (== 0), ImGuiKeyOwner_None (== -1) or a custom ID.
    // - Routing is requested ahead of time for a given chord (Key + Mods) and granted in NewFrame().
    val keysOwnerData = Array(Key.COUNT) { KeyOwnerData() }

    var keysRoutingTable = KeyRoutingTable()

    /** Active widget will want to read those nav move requests (e.g. can activate a button and move away from it) */
    var activeIdUsingNavDirMask = 0

    /** Active widget will want to read all keyboard keys inputs. (FIXME: This is a shortcut for not taking ownership of 100+ keys but perhaps best to not have the inconsistency) */
    var activeIdUsingAllKeyboardKeys = false


    // Next window/item data


    /** == g.FocusScopeStack.back() */
    var currentFocusScopeId: ID = 0

    /** == g.ItemFlagsStack.back() */
    var currentItemFlags: ItemFlags = emptyFlags()

    /** Storage for DebugLocateItemOnHover() feature: this is read by ItemAdd() so we keep it in a hot/cached location */
    var debugLocateId: ID = 0

    /** Storage for SetNextItem** functions */
    var nextItemData = NextItemData()

    /** Storage for last submitted item (setup by ItemAdd) */
    val lastItemData = LastItemData()

    /** Storage for SetNextWindow** functions   */
    val nextWindowData = NextWindowData()


    /** Stack for PushStyleColor()/PopStyleColor() - inherited by Begin()  */
    var colorStack = Stack<ColorMod>()

    /** Stack for PushStyleVar()/PopStyleVar() - inherited by Begin()  */
    val styleVarStack = Stack<StyleMod>()

    /** Stack for PushFont()/PopFont() - inherited by Begin()  */
    val fontStack = Stack<Font>()

    /** Stack for PushFocusScope()/PopFocusScope() - inherited by BeginChild(), pushed into by Begin() */
    val focusScopeStack = Stack<ID>()

    /** Stack for PushItemFlag()/PopItemFlag() - inherited by Begin() */
    val itemFlagsStack = Stack<ItemFlags>()

    /** Stack for BeginGroup()/EndGroup() - not inherited by Begin() */
    val groupStack = Stack<GroupData>()

    /** Which popups are open (persistent)  */
    val openPopupStack = Stack<PopupData>()

    /** Which level of BeginPopup() we are in (reset every frame)   */
    val beginPopupStack = Stack<PopupData>()

    var beginMenuCount = 0

    //------------------------------------------------------------------
    // Viewports
    //------------------------------------------------------------------

    /** Active viewports (Size==1 in 'master' branch). Each viewports hold their copy of ImDrawData. */
    val viewports = ArrayList<ViewportP>()

    //------------------------------------------------------------------
    // Gamepad/keyboard Navigation
    //------------------------------------------------------------------

    /** Focused window for navigation. Could be called 'FocusedWindow'    */
    var navWindow: Window? = null

    /** Focused item for navigation */
    var navId: ID = 0

    /** Identify a selection scope (selection code often wants to "clear other items" when landing on an item of the selection set) */
    var navFocusScopeId = 0

    /** ~~ (g.ActiveId == 0) && (IsKeyPressed(ImGuiKey_Space) || IsKeyPressed(ImGuiKey_NavGamepadActivate)) ? NavId : 0, also set when calling ActivateItem() */
    var navActivateId: ID = 0

    /** ~~ IsKeyDown(ImGuiKey_Space) || IsKeyDown(ImGuiKey_NavGamepadActivate) ? NavId : 0  */
    var navActivateDownId: ID = 0

    /** ~~ IsKeyPressed(ImGuiKey_Space) || IsKeyPressed(ImGuiKey_NavGamepadActivate) ? NavId : 0 (no repeat)  */
    var navActivatePressedId: ID = 0

    /** ~~ IsKeyPressed(ImGuiKey_Enter) || IsKeyPressed(ImGuiKey_NavGamepadInput) ? NavId : 0; ImGuiActivateFlags_PreferInput will be set and NavActivateId will be 0.  */
    var navActivateInputId: ID = 0

    var navActivateFlags: ActivateFlags = emptyFlags()

    /** Just navigated to this id (result of a successfully MoveRequest)    */
    var navJustMovedToId: ID = 0

    /** Just navigated to this focus scope id (result of a successfully MoveRequest). */
    var navJustMovedToFocusScopeId: ID = 0

    var navJustMovedToKeyMods: KeyChord = Key.Mod_None

    /** Set by ActivateItem(), queued until next frame  */
    var navNextActivateId: ID = 0

    var navNextActivateFlags: ActivateFlags = emptyFlags()

    /** Keyboard or Gamepad mode? THIS WILL ONLY BE None or NavGamepad or NavKeyboard.  */
    var navInputSource = InputSource.None

    /** Layer we are navigating on. For now the system is hard-coded for 0 = main contents and 1 = menu/title bar,
     *  may expose layers later. */
    var navLayer = NavLayer.Main

    /** Nav widget has been seen this frame ~~ NavRectRel is valid   */
    var navIdIsAlive = false

    /** When set we will update mouse position if (io.ConfigFlag & ConfigFlag.NavMoveMouse) if set (NB: this not enabled by default) */
    var navMousePosDirty = false

    /** When user starts using mouse, we hide gamepad/keyboard highlight (NB: but they are still available, which is why
     *  NavDisableHighlight isn't always != NavDisableMouseHover)   */
    var navDisableHighlight = true

    /** When user starts using gamepad/keyboard, we hide mouse hovering highlight until mouse is touched again. */
    var navDisableMouseHover = false

    //------------------------------------------------------------------
    // Navigation: Init & Move Requests
    //------------------------------------------------------------------

    /** ~~ navMoveRequest || navInitRequest this is to perform early out in ItemAdd() */
    var navAnyRequest = false

    /** Init request for appearing window to select first item  */
    var navInitRequest = false

    var navInitRequestFromMove = false

    /** Init request result (first item of the window, or one for which SetItemDefaultFocus() was called) */
    var navInitResultId: ID = 0

    /** Init request result rectangle (relative to parent window) */
    var navInitResultRectRel = Rect()

    /** Move request submitted, will process result on next NewFrame() */
    var navMoveSubmitted = false

    /** Move request submitted, still scoring incoming items */
    var navMoveScoringItems = false

    var navMoveForwardToNextFrame = false

    var navMoveFlags: NavMoveFlags = emptyFlags()

    var navMoveScrollFlags: ScrollFlags = emptyFlags()

    var navMoveKeyMods: KeyChord = Key.Mod_None

    /** Direction of the move request (left/right/up/down), direction of the previous move request  */
    var navMoveDir = Dir.None

    var navMoveDirForDebug = Dir.None

    /** FIXME-NAV: Describe the purpose of this better. Might want to rename? */
    var navMoveClipDir = Dir.None

    /** Rectangle used for scoring, in screen space. Based of window.NavRectRel[], modified for directional navigation scoring.  */
    val navScoringRect = Rect()

    /** Some nav operations (such as PageUp/PageDown) enforce a region which clipper will attempt to always keep submitted */
    val navScoringNoClipRect = Rect()

    /** Metrics for debugging   */
    var navScoringDebugCount = 0

    /** Generally -1 or +1, 0 when tabbing without a nav id */
    var navTabbingDir = 0

    /** >0 when counting items for tabbing */
    var navTabbingCounter = 0

    /** Best move request candidate within NavWindow    */
    var navMoveResultLocal = NavItemData()

    /** Best move request candidate within NavWindow that are mostly visible (when using NavMoveFlags.AlsoScoreVisibleSet flag) */
    val navMoveResultLocalVisible = NavItemData()

    /** Best move request candidate within NavWindow's flattened hierarchy (when using WindowFlags.NavFlattened flag)   */
    var navMoveResultOther = NavItemData()

    /** First tabbing request candidate within NavWindow and flattened hierarchy */
    val navTabbingResultFirst = NavItemData()


    //------------------------------------------------------------------
    // Navigation: Windowing (CTRL+TAB for list, or Menu button + keys or directional pads to move/resize)
    //------------------------------------------------------------------

    /** = ImGuiMod_Ctrl | ImGuiKey_Tab, for reconfiguration (see #4828) */
    var configNavWindowingKeyNext: KeyChord = Key.Mod_Ctrl or Key.Tab

    /** = ImGuiMod_Ctrl | ImGuiMod_Shift | ImGuiKey_Tab */
    var configNavWindowingKeyPrev: KeyChord = Key.Mod_Ctrl or Key.Mod_Shift or Key.Tab

    /** Target window when doing CTRL+Tab (or Pad Menu + FocusPrev/Next), this window is temporarily displayed top-most! */
    var navWindowingTarget: Window? = null

    /** Record of last valid NavWindowingTarget until DimBgRatio and NavWindowingHighlightAlpha becomes 0.0f, so the fade-out can stay on it. */
    var navWindowingTargetAnim: Window? = null

    /** Internal window actually listing the CTRL+Tab contents */
    var navWindowingListWindow: Window? = null

    var navWindowingTimer = 0f

    var navWindowingHighlightAlpha = 0f

    var navWindowingToggleLayer = false

    val navWindowingAccumDeltaPos = Vec2()
    val navWindowingAccumDeltaSize = Vec2()

    // ------------------------------------------------------------------
    // Render
    //------------------------------------------------------------------

    val drawDataBuilder = DrawDataBuilder()

    /** 0.0..1.0 animation when fading in a dimming background (for modal window and CTRL+TAB list) */
    var dimBgRatio = 0f

    var mouseCursor = MouseCursor.Arrow


    //------------------------------------------------------------------
    // Drag and Drop
    //------------------------------------------------------------------
    var dragDropActive = false

    /** Set when within a BeginDragDropXXX/EndDragDropXXX block for a drag source. */
    var dragDropWithinSource = false

    /** Set when within a BeginDragDropXXX/EndDragDropXXX block for a drag target. */
    var dragDropWithinTarget = false

    var dragDropSourceFlags: DragDropFlags = emptyFlags()

    var dragDropSourceFrameCount = -1

    var dragDropMouseButton = MouseButton.None // -1 at start

    var dragDropPayload = Payload()

    /** Store rectangle of current target candidate (we favor small targets when overlapping) */
    var dragDropTargetRect = Rect()

    var dragDropTargetId: ID = 0

    var dragDropAcceptFlags: DragDropFlags = emptyFlags()

    /** Target item surface (we resolve overlapping targets by prioritizing the smaller surface) */
    var dragDropAcceptIdCurrRectSurface = 0f

    /** Target item id (set at the time of accepting the payload) */
    var dragDropAcceptIdCurr: ID = 0

    /** Target item id from previous frame (we need to store this to allow for overlapping drag and drop targets) */
    var dragDropAcceptIdPrev: ID = 0

    /** Last time a target expressed a desire to accept the source */
    var dragDropAcceptFrameCount = -1

    /** Set when holding a payload just made ButtonBehavior() return a press. */
    var dragDropHoldJustPressedId: ID = 0

    /** We don't expose the ImVector<> directly, ImGuiPayload only holds pointer+size */
    var dragDropPayloadBufHeap = ByteBuffer.allocate(0)

    /** Local buffer for small payloads */
    var dragDropPayloadBufLocal = ByteBuffer.allocate(16)


    // Clipper
    var clipperTempDataStacked = 0

    val clipperTempData = ArrayList<ListClipperData>()


    // Tables

    var currentTable: Table? = null
    var tablesTempDataStacked = 0 // Temporary table data size (because we leave previous instances undestructed, we generally don't use TablesTempData.Size)
    val tablesTempData = ArrayList<TableTempData>() // Temporary table data (buffers reused/shared across instances, support nesting)
    val tables = Pool { Table() } // Persistent table data

    /** Last used timestamp of each tables (SOA, for efficient GC) */
    val tablesLastTimeActive = ArrayList<Float>()
    val drawChannelsTempMergeBuffer = ArrayList<DrawChannel>()


    // Tab bars

    var currentTabBar: TabBar? = null
    val tabBars = TabBarPool()
    val currentTabBarStack = Stack<PtrOrIndex>()
    val shrinkWidthBuffer = ArrayList<ShrinkWidthItem>()


    // Hover Delay system
    var hoverDelayId: ID = 0
    var hoverDelayIdPreviousFrame: ID = 0

    /** Currently used IsItemHovered(), generally inferred from g.HoveredIdTimer but kept uncleared until clear timer elapse. */
    var hoverDelayTimer = 0f

    /** Currently used IsItemHovered(): grace time before g.TooltipHoverTimer gets cleared. */
    var hoverDelayClearTimer = 0f

    //------------------------------------------------------------------
    // Widget state
    //------------------------------------------------------------------

    val mouseLastValidPos = Vec2()

    var inputTextState = InputTextState(this)

    var inputTextPasswordFont = Font()

    /** Temporary text input when CTRL+clicking on a slider, etc.   */
    var tempInputId: ID = 0

    /** Store user options for color edit widgets   */
    var colorEditOptions: ColorEditFlags = ColorEditFlag.DefaultOptions

    /** Backup of last Hue associated to LastColor, so we can restore Hue in lossy RGB<>HSV round trips */
    var colorEditLastHue = 0f

    /** Backup of last Saturation associated to LastColor, so we can restore Saturation in lossy RGB<>HSV round trips */
    var colorEditLastSat = 0f

    var colorEditLastColor = 0

    /** Initial/reference color at the time of opening the color picker. */
    val colorPickerRef = Vec4()

    val comboPreviewData = ComboPreviewData()

    var sliderGrabClickOffset = 0f

    /** Accumulated slider delta when using navigation controls. */
    var sliderCurrentAccum = 0f

    /** Has the accumulated slider delta changed since last time we tried to apply it? */
    var sliderCurrentAccumDirty = false

    var dragCurrentAccumDirty = false

    /** Accumulator for dragging modification. Always high-precision, not rounded by end-user precision settings */
    var dragCurrentAccum = 0f

    /** If speed == 0.0f, uses (max-min) * DragSpeedDefaultRatio    */
    var dragSpeedDefaultRatio = 1f / 100f

    /** Distance between mouse and center of grab box, normalized in parent space. Use storage? */
    var scrollbarClickDeltaToGrabCenter = 0f

    /** Backup for style.Alpha for BeginDisabled() */
    var disabledAlphaBackup = 0f

    var disabledStackSize = 0

    var tooltipOverrideCount = 0

    /** If no custom clipboard handler is defined   */
    var clipboardHandlerData = ""

    /** A list of menu IDs that were rendered at least once */
    val menusIdSubmittedThisFrame = ArrayList<ID>()


    // Platform support

    /** Data updated by current frame */
    var platformImeData = PlatformImeData()

    /** Previous frame data (when changing we will call io.SetPlatformImeDataFn */
    var platformImeDataPrev = PlatformImeData(inputPos = Vec2(-1f))

    /** '.' or *localeconv()->decimal_point */
    var platformLocaleDecimalPoint = '.'


    //------------------------------------------------------------------
    // Settings
    //------------------------------------------------------------------

    var settingsLoaded = false

    /** Save .ini Settings to memory when time reaches zero   */
    var settingsDirtyTimer = 0f

    /** In memory .ini Settings for Window  */
    var settingsIniData = ""

    /** List of .ini settings handlers */
    val settingsHandlers = ArrayList<SettingsHandler>()

    /** ImGuiWindow .ini settings entries (parsed from the last loaded .ini file and maintained on saving) */
    val settingsWindows = ArrayList<WindowSettings>()

    /** ImGuiTable .ini settings entries */
    val settingsTables = ArrayList<TableSettings>()

    /** Hooks for extensions (e.g. test engine) */
    val hooks = ArrayList<ContextHook>()

    /** Next available HookId */
    var hookIdNext: ID = 0

    //------------------------------------------------------------------
    // Localization
    //------------------------------------------------------------------

    val localizationTable = mutableMapOf<LocKey, String>()

    //------------------------------------------------------------------
    // Capture/Logging
    //------------------------------------------------------------------

    /** Currently capturing */
    var logEnabled = false

    /** Capture target */
    var logType = LogType.None

    /** If != NULL log to stdout/ file  */
    var logFile: File? = null

    /** Accumulation buffer when log to clipboard. This is pointer so our GImGui static constructor doesn't call heap allocators.   */
    var logBuffer = StringBuilder()

    var logNextPrefix = ""

    var logNextSuffix = ""

    var logLinePosY = Float.MAX_VALUE

    var logLineFirstItem = false

    var logDepthRef = 0

    var logDepthToExpand = 2

    var logDepthToExpandDefault = 2

    // Debug Tools

    var debugLogFlags: DebugLogFlags = DebugLogFlag.OutputToTTY or DebugLogFlag.EventMask wo DebugLogFlag.EventClipper
    val debugLogBuf = StringBuilder()

    val debugLogIndex = TextIndex()

    /** For DebugLocateItemOnHover(). This is used together with DebugLocateId which is in a hot/cached spot above. */
    var debugLocateFrames = 0

    /** Item picker is active (started with DebugStartItemPicker()) */
    var debugItemPickerActive = false

    var debugItemPickerMouseButton = MouseButton.Left

    /** Will call IM_DEBUG_BREAK() when encountering this ID */
    var debugItemPickerBreakId: ID = 0

    var debugMetricsConfig = MetricsConfig()

    val debugStackTool = StackTool()

    //------------------------------------------------------------------
    // Misc
    //------------------------------------------------------------------

    /** Calculate estimate of framerate for user over the last 60 frames..    */
    val framerateSecPerFrame = FloatArray(60)

    var framerateSecPerFrameIdx = 0

    var framerateSecPerFrameCount = 0

    var framerateSecPerFrameAccum = 0f

    /** Explicit capture override via SetNextFrameWantCaptureMouse()/SetNextFrameWantCaptureKeyboard(). Default to -1. */
    var wantCaptureMouseNextFrame = -1

    /** Explicit capture override via SetNextFrameWantCaptureMouse()/SetNextFrameWantCaptureKeyboard(). Default to -1. */
    var wantCaptureKeyboardNextFrame = -1

    var wantTextInputNextFrame = -1

    /** Temporary text buffer */
    var tempBuffer = ByteArray(1024 * 3)

    /*  Context creation and access
        Each context create its own ImFontAtlas by default. You may instance one yourself and pass it to Context()
        to share a font atlas between imgui contexts.
        None of those functions is reliant on the current context.
        ~CreateContext */
    init {
        val prevCtx = ImGui.currentContext
        setCurrent()
        initialize()
        prevCtx?.setCurrent() // Restore previous context if any, else keep new one.
    }

    // Init

    fun initialize() {
        assert(!initialized && !g.settingsLoaded)

        // Add .ini handle for ImGuiWindow and ImGuiTable types
        val iniHandler = SettingsHandler().apply {
            typeName = "Window"
            typeHash = hashStr("Window")
            clearAllFn = ::windowSettingsHandler_ClearAll
            readOpenFn = ::windowSettingsHandler_ReadOpen
            readLineFn = ::windowSettingsHandler_ReadLine
            applyAllFn = ::windowSettingsHandler_ApplyAll
            writeAllFn = ::windowSettingsHandler_WriteAll
        }
        addSettingsHandler(iniHandler)
        tableSettingsAddSettingsHandler()

        // Setup default localization table
        localizeRegisterEntries(gLocalizationEntriesEnUS)

        // Create default viewport
        val viewport = ViewportP()
        g.viewports += viewport
        // [JVM] useless
        // g.TempBuffer.resize(1024 * 3 + 1, 0);

        //        #ifdef IMGUI_HAS_DOCK
        //        #endif // #ifdef IMGUI_HAS_DOCK

        initialized = true
    }

    /** This function is merely here to free heap allocations.
     *  ~Shutdown(ImGuiContext* context)    */
    fun shutdown() {

        // The fonts atlas can be used prior to calling NewFrame(), so we clear it even if g.Initialized is FALSE (which would happen if we never called NewFrame)
        if (fontAtlasOwnedByContext)
            io.fonts.locked = false
        io.fonts.clear()
        drawListSharedData.tempBuffer.clear()

        // Cleanup of other data are conditional on actually having initialized Dear ImGui.
        if (!initialized)
            return

        // Save settings (unless we haven't attempted to load them: CreateContext/DestroyContext without a call to NewFrame shouldn't save an empty file)
        if (settingsLoaded)
            io.iniFilename?.let(::saveIniSettingsToDisk)

        // Notify hooked test engine, if any
        g callHooks ContextHookType.Shutdown

        // Clear everything else
        windows.forEach { it.destroy() }
        windows.clear()
        windowsFocusOrder.clear()
        windowsTempSortBuffer.clear()
        currentWindow = null
        currentWindowStack.clear()
        windowsById.clear()
        navWindow = null
        hoveredWindow = null
        hoveredWindowUnderMovingWindow = null
        activeIdWindow = null
        activeIdPreviousFrameWindow = null
        movingWindow = null

        keysRoutingTable.clear()

        settingsWindows.clear()
        colorStack.clear()
        styleVarStack.clear()
        fontStack.clear()
        openPopupStack.clear()
        beginPopupStack.clear()
        drawDataBuilder.clear()

        viewports.clear()

        tabBars.clear()
        currentTabBarStack.clear()
        shrinkWidthBuffer.clear()

        clipperTempData.clear()

        tables.clear()
        tablesTempData.clear()
        drawChannelsTempMergeBuffer.clear() // TODO check if this needs proper deallocation

        clipboardHandlerData = ""
        menusIdSubmittedThisFrame.clear()
        inputTextState.textW = CharArray(0)
        inputTextState.initialTextA = ByteArray(0)
        inputTextState.textA = ByteArray(0)

        if (logFile != null) {
            logFile = null
        }
        logBuffer.setLength(0)
        debugLogBuf.clear()

        initialized = false
    }

    /** ~SetCurrentContext */
    fun setCurrent() {
        gImGui = this
    }

    /** Destroy current context
     *  ~DestroyContext */
    fun destroy() {
        val prevCtx = ImGui.currentContext
        //        if (ctx/this == NULL) //-V1051
        //            ctx = GImGui;
        setCurrent()
        shutdown()
        if (prevCtx !== this)
            prevCtx?.setCurrent()
    }

    companion object {
        // IMPORTANT: ###xxx suffixes must be same in ALL languages
        val gLocalizationEntriesEnUS = listOf(
            LocEntry(LocKey.TableSizeOne, "Size column to fit###SizeOne"),
            LocEntry(LocKey.TableSizeAllFit, "Size all columns to fit###SizeAll"),
            LocEntry(LocKey.TableSizeAllDefault, "Size all columns to default###SizeAll"),
            LocEntry(LocKey.TableResetOrder, "Reset order###ResetOrder"),
            LocEntry(LocKey.WindowingMainMenuBar, "(Main menu bar)"),
            LocEntry(LocKey.WindowingPopup, "(Popup)"),
            LocEntry(LocKey.WindowingUntitled, "(Untitled)"))
    }
}


//-----------------------------------------------------------------------------
// [SECTION] Generic context hooks
//-----------------------------------------------------------------------------

typealias ContextHookCallback = (ctx: Context, hook: ContextHook) -> Unit

enum class ContextHookType { NewFramePre, NewFramePost, EndFramePre, EndFramePost, RenderPre, RenderPost, Shutdown, PendingRemoval_ }

/** Hook for extensions like ImGuiTestEngine */
class ContextHook(
    // A unique ID assigned by AddContextHook()
    var hookId: ID = 0,
    var type: ContextHookType = ContextHookType.NewFramePre,
    var owner: ID = 0,
    var callback: ContextHookCallback? = null,
    var userData: Any? = null)