package imgui.classes

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.callContextHooks
import imgui.ImGui.destroyPlatformWindows
import imgui.ImGui.dockContextInitialize
import imgui.ImGui.dockContextShutdown
import imgui.ImGui.saveIniSettingsToDisk
import imgui.api.g
import imgui.api.gImGui
import imgui.font.Font
import imgui.font.FontAtlas
import imgui.internal.classes.*
import imgui.internal.hash
import imgui.internal.sections.*
import imgui.static.*
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

/** Main Dear ImGui context
 *
 *  Dear ImGui context (opaque structure, unless including imgui_internal.h)
 *
 *  ~CreateContext */
class Context(sharedFontAtlas: FontAtlas? = null) {

    var initialized = false

    /** Io.Fonts-> is owned by the ImGuiContext and will be destructed along with it.   */
    var fontAtlasOwnedByContext = sharedFontAtlas == null

    var io = IO(sharedFontAtlas)

    var platformIO = PlatformIO()

    var style = Style()

    /** = g.IO.ConfigFlags at the time of NewFrame() */
    var configFlagsCurrFrame = ConfigFlag.None.i
    var configFlagsLastFrame = ConfigFlag.None.i

    lateinit var font: Font

    /** (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize(). Text height for current window. */
    var fontSize = 0f

    /** (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Base text height.    */
    var fontBaseSize = 0f

    var drawListSharedData = DrawListSharedData()

    var time = 0.0

    var frameCount = 0

    var frameCountEnded = -1

    var frameCountPlatformEnded = -1

    var frameCountRendered = -1

    /** Set by NewFrame(), cleared by EndFrame() */
    var withinFrameScope = false

    /** Set by NewFrame(), cleared by EndFrame() when the implicit debug window has been pushed */
    var withinFrameScopeWithImplicitWindow = false

    /** Set within EndChild() */
    var withinEndChild = false

    /** Will call test engine hooks: ImGuiTestEngineHook_ItemAdd(), ImGuiTestEngineHook_ItemInfo(), ImGuiTestEngineHook_Log() */
    var testEngineHookItems = false

    /** Will call test engine hooks: ImGuiTestEngineHook_IdInfo() from GetID() */
    var testEngineHookIdInfo: ID = 0

    /** Test engine user data */
    var testEngine: Any? = null


    // Windows state

    /** Windows, sorted in display order, back to front */
    val windows = ArrayList<Window>()

    /** Windows, sorted in focus order, back to front.
     * (FIXME: We could only store root windows here! Need to sort out the Docking equivalent which is RootWindowDockStop and is unfortunately a little more dynamic) */
    val windowsFocusOrder = ArrayList<Window>()

    val windowsTempSortBuffer = ArrayList<Window>()

    val currentWindowStack = Stack<Window>()

    /** Map window's ImGuiID to ImGuiWindow* */
    val windowsById = mutableMapOf<Int, Window>()

    /** Number of unique windows submitted by frame */
    var windowsActiveCount = 0

    /** Window being drawn into    */
    var currentWindow: Window? = null

    /** Window the mouse is hovering. Will typically catch mouse inputs. */
    var hoveredWindow: Window? = null

    /** == HoveredWindow ? HoveredWindow->RootWindow : NULL, merely a shortcut to avoid null test in some situation.   */
    var hoveredRootWindow: Window? = null

    /** Hovered window ignoring MovingWindow. Only set if MovingWindow is set. */
    var hoveredWindowUnderMovingWindow: Window? = null

    /** Hovered dock node. */
    var hoveredDockNode: DockNode? = null

    /** Track the window we clicked on (in order to preserve focus). The actually window that is moved is generally MovingWindow->RootWindow. */
    var movingWindow: Window? = null

    /** Track the window we started mouse-wheeling on. Until a timer elapse or mouse has moved, generally keep scrolling the same window even if during the course of scrolling the mouse ends up hovering a child window. */
    var wheelingWindow: Window? = null

    var wheelingWindowRefMousePos: Vec2 = Vec2()

    var wheelingWindowTimer = 0f


    // Item/widgets state and tracking information

    /** Hovered widget  */
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

    /** Active widget will want to read those nav move requests (e.g. can activate a button and move away from it) */
    var activeIdUsingNavDirMask = 0

    /** Active widget will want to read those nav inputs. */
    var activeIdUsingNavInputMask = 0

    /** Active widget will want to read those key inputs. When we grow the ImGuiKey enum we'll need to either to order the enum to make useful keys come first, either redesign this into e.g. a small array. */
    var activeIdUsingKeyInputMask = 0L

    /** Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior) */
    var activeIdClickOffset = Vec2(-1)

    var activeIdWindow: Window? = null

    /** Activating with mouse or nav (gamepad/keyboard) */
    var activeIdSource = InputSource.None

    var activeIdMouseButton = 0

    var activeIdPreviousFrame: ID = 0

    var activeIdPreviousFrameIsAlive = false

    var activeIdPreviousFrameHasBeenEdited = false

    var activeIdPreviousFrameWindow: Window? = null

    /** Store the last non-zero ActiveId, useful for animation. */
    var lastActiveId: ID = 0

    /** Store the last non-zero ActiveId timer since the beginning of activation, useful for animation. */
    var lastActiveIdTimer = 0f


    // Next window/item data

    /** Storage for SetNextWindow** functions   */
    val nextWindowData = NextWindowData()

    /** Storage for SetNextItem** functions */
    var nextItemData = NextItemData()


    /** Stack for PushStyleColor()/PopStyleColor()  */
    var colorModifiers = Stack<ColorMod>()

    /** Stack for PushStyleVar()/PopStyleVar()  */
    val styleModifiers = Stack<StyleMod>()

    /** Stack for PushFont()/PopFont()  */
    val fontStack = Stack<Font>()

    /** Which popups are open (persistent)  */
    val openPopupStack = Stack<PopupData>()

    /** Which level of BeginPopup() we are in (reset every frame)   */
    val beginPopupStack = Stack<PopupData>()

    //------------------------------------------------------------------
    // Viewports
    //------------------------------------------------------------------

    /** Active viewports (always 1+, and generally 1 unless multi-viewports are enabled). Each viewports hold their copy of ImDrawData. */
    val viewports = ArrayList<ViewportP>()

    /**  == CurrentViewport->DpiScale */
    var currentDpiScale = 0f

    /** We track changes of viewport (happening in Begin) so we can call Platform_OnChangedViewport() */
    var currentViewport: ViewportP? = null
    var mouseViewport: ViewportP? = null

    /** Last known viewport that was hovered by mouse (even if we are not hovering any viewport any more) + honoring the _NoInputs flag. */
    var mouseLastHoveredViewport: ViewportP? = null

    /** Every time the front-most window changes, we stamp its viewport with an incrementing counter */
    var viewportFrontMostStampCount = 0


    //------------------------------------------------------------------
    // Gamepad/keyboard Navigation
    //------------------------------------------------------------------

    /** Focused window for navigation. Could be called 'FocusWindow'    */
    var navWindow: Window? = null

    /** Focused item for navigation */
    var navId: ID = 0

    /** Identify a selection scope (selection code often wants to "clear other items" when landing on an item of the selection set) */
    var navFocusScopeId = 0

    /** ~~ (g.activeId == 0) && NavInput.Activate.isPressed() ? navId : 0, also set when calling activateItem() */
    var navActivateId: ID = 0

    /** ~~ isNavInputDown(NavInput.Activate) ? navId : 0   */
    var navActivateDownId: ID = 0

    /** ~~ NavInput.Activate.isPressed() ? navId : 0    */
    var navActivatePressedId: ID = 0

    /** ~~ NavInput.Input.isPressed() ? navId : 0   */
    var navInputId: ID = 0

    /** Just tabbed to this id. */
    var navJustTabbedId: ID = 0

    /** Just navigated to this id (result of a successfully MoveRequest)    */
    var navJustMovedToId: ID = 0

    /** Just navigated to this focus scope id (result of a successfully MoveRequest). */
    var navJustMovedToFocusScopeId: ID = 0

    var navJustMovedToKeyMods: KeyModFlags = KeyMod.None.i

    /** Set by ActivateItem(), queued until next frame  */
    var navNextActivateId: ID = 0

    /** Keyboard or Gamepad mode? THIS WILL ONLY BE None or NavGamepad or NavKeyboard.  */
    var navInputSource = InputSource.None

    /** Rectangle used for scoring, in screen space. Based of window.NavRectRel[], modified for directional navigation scoring.  */
    var navScoringRect = Rect()

    /** Metrics for debugging   */
    var navScoringCount = 0

    /** Layer we are navigating on. For now the system is hard-coded for 0 = main contents and 1 = menu/title bar,
     *  may expose layers later. */
    var navLayer = NavLayer.Main

    /** == NavWindow->DC.FocusIdxTabCounter at time of NavId processing */
    var navIdTabCounter = Int.MAX_VALUE

    /** Nav widget has been seen this frame ~~ NavRectRel is valid   */
    var navIdIsAlive = false

    /** When set we will update mouse position if (io.ConfigFlag & ConfigFlag.NavMoveMouse) if set (NB: this not enabled by default) */
    var navMousePosDirty = false

    /** When user starts using mouse, we hide gamepad/keyboard highlight (NB: but they are still available, which is why
     *  NavDisableHighlight isn't always != NavDisableMouseHover)   */
    var navDisableHighlight = true

    /** When user starts using gamepad/keyboard, we hide mouse hovering highlight until mouse is touched again. */
    var navDisableMouseHover = false

    /** ~~ navMoveRequest || navInitRequest */
    var navAnyRequest = false

    /** Init request for appearing window to select first item  */
    var navInitRequest = false

    var navInitRequestFromMove = false

    /** Init request result (first item of the window, or one for which SetItemDefaultFocus() was called) */
    var navInitResultId: ID = 0

    /** Init request result rectangle (relative to parent window) */
    var navInitResultRectRel = Rect()

    /** Move request for this frame */
    var navMoveRequest = false

    var navMoveRequestFlags = NavMoveFlag.None.i

    /** None / ForwardQueued / ForwardActive (this is used to navigate sibling parent menus from a child menu)  */
    var navMoveRequestForward = NavForward.None

    var navMoveRequestKeyMods: KeyModFlags = KeyMod.None.i

    /** Direction of the move request (left/right/up/down), direction of the previous move request  */
    var navMoveDir = Dir.None

    /** Direction of the move request (left/right/up/down), direction of the previous move request  */
    var navMoveDirLast = Dir.None

    /** FIXME-NAV: Describe the purpose of this better. Might want to rename? */
    var navMoveClipDir = Dir.None

    /** Best move request candidate within NavWindow    */
    var navMoveResultLocal = NavMoveResult()

    /** Best move request candidate within NavWindow that are mostly visible (when using NavMoveFlags.AlsoScoreVisibleSet flag) */
    val navMoveResultLocalVisibleSet = NavMoveResult()

    /** Best move request candidate within NavWindow's flattened hierarchy (when using WindowFlags.NavFlattened flag)   */
    var navMoveResultOther = NavMoveResult()

    /** Window which requested trying nav wrap-around. */
    var navWrapRequestWindow: Window? = null

    /** Wrap-around operation flags. */
    var navWrapRequestFlags: NavMoveFlags = NavMoveFlag.None.i


    // Navigation: Windowing (CTRL+TAB for list, or Menu button + keys or directional pads to move/resize)

    /** Target window when doing CTRL+Tab (or Pad Menu + FocusPrev/Next), this window is temporarily displayed top-most! */
    var navWindowingTarget: Window? = null

    /** Record of last valid NavWindowingTarget until DimBgRatio and NavWindowingHighlightAlpha becomes 0.0f, so the fade-out can stay on it. */
    var navWindowingTargetAnim: Window? = null

    /** Internal window actually listing the CTRL+Tab contents */
    var navWindowingListWindow: Window? = null

    var navWindowingTimer = 0f

    var navWindowingHighlightAlpha = 0f

    var navWindowingToggleLayer = false


    // Legacy Focus/Tabbing system (older than Nav, active even if Nav is disabled, misnamed. FIXME-NAV: This needs a redesign!)

    var focusRequestCurrWindow: Window? = null

    var focusRequestNextWindow: Window? = null

    /** Any item being requested for focus, stored as an index (we on layout to be stable between the frame pressing TAB and the next frame, semi-ouch) */
    var focusRequestCurrCounterRegular = Int.MAX_VALUE

    /** Tab item being requested for focus, stored as an index */
    var focusRequestCurrCounterTabStop = Int.MAX_VALUE

    /** Stored for next frame */
    var focusRequestNextCounterRegular = Int.MAX_VALUE

    /** Stored for next frame */
    var focusRequestNextCounterTabStop = Int.MAX_VALUE

    var focusTabPressed = false


    // ------------------------------------------------------------------
    // Render
    //------------------------------------------------------------------

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

    var dragDropSourceFlags = DragDropFlag.None.i

    var dragDropSourceFrameCount = -1

    var dragDropMouseButton = MouseButton.None // -1 at start

    var dragDropPayload = Payload()

    /** Store rectangle of current target candidate (we favor small targets when overlapping) */
    var dragDropTargetRect = Rect()

    var dragDropTargetId: ID = 0

    var dragDropAcceptFlags = DragDropFlag.None.i

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


    // Tab bars
    var currentTabBar: TabBar? = null
    val tabBars = TabBarPool()
    val currentTabBarStack = Stack<PtrOrIndex>()
    val shrinkWidthBuffer = ArrayList<ShrinkWidthItem>()

    //------------------------------------------------------------------
    // Widget state
    //------------------------------------------------------------------

    var lastValidMousePos = Vec2()

    var inputTextState = InputTextState()

    var inputTextPasswordFont = Font()

    /** Temporary text input when CTRL+clicking on a slider, etc.   */
    var tempInputId: ID = 0

    /** Store user options for color edit widgets   */
    var colorEditOptions: ColorEditFlags = ColorEditFlag._OptionsDefault.i

    /** Backup of last Hue associated to LastColor[3], so we can restore Hue in lossy RGB<>HSV round trips */
    var colorEditLastHue = 0f

    /** Backup of last Saturation associated to LastColor[3], so we can restore Saturation in lossy RGB<>HSV round trips */
    var colorEditLastSat = 0f

    var colorEditLastColor = FloatArray(3) { Float.MAX_VALUE }

    /** Initial/reference color at the time of opening the color picker. */
    val colorPickerRef = Vec4()

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

    var tooltipOverrideCount = 0

    /** If no custom clipboard handler is defined   */
    var clipboardHandlerData = ""

    /** A list of menu IDs that were rendered at least once */
    val menusIdSubmittedThisFrame = ArrayList<ID>()

    //------------------------------------------------------------------
    // Platform support
    //------------------------------------------------------------------

    /** Cursor position request to the OS Input Method Editor   */
    var platformImePos = Vec2(Float.MAX_VALUE)

    /** Last cursor position passed to the OS Input Method Editor   */
    var platformImeLastPos = Vec2(Float.MAX_VALUE)

    var platformImePosViewport: ViewportP? = null

    /** '.' or *localeconv()->decimal_point */
    var platformLocaleDecimalPoint = '.'

    //------------------------------------------------------------------
    // Extensions
    // FIXME: We could provide an API to register one slot in an array held in ImGuiContext?
    //------------------------------------------------------------------
    val dockContext = DockContext()


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

    /** Hooks for extensions (e.g. test engine) */
    val hooks = ArrayList<ContextHook>()

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

    var logLinePosY = Float.MAX_VALUE

    var logLineFirstItem = false

    var logDepthRef = 0

    var logDepthToExpand = 2

    var logDepthToExpandDefault = 2

    // Debug Tools

    /** Item picker is active (started with DebugStartItemPicker()) */
    var debugItemPickerActive = false

    /** Will call IM_DEBUG_BREAK() when encountering this id */
    var debugItemPickerBreakId: ID = 0


    //------------------------------------------------------------------
    // Misc
    //------------------------------------------------------------------

    /** Calculate estimate of framerate for user over the last 2 seconds.    */
    val framerateSecPerFrame = FloatArray(120)

    var framerateSecPerFrameIdx = 0

    var framerateSecPerFrameAccum = 0f

    /** Explicit capture via CaptureKeyboardFromApp()/CaptureMouseFromApp() sets those flags   */
    var wantCaptureMouseNextFrame = -1

    var wantCaptureKeyboardNextFrame = -1

    var wantTextInputNextFrame = -1

    /** Temporary text buffer */
    val tempBuffer = ByteArray(1024 * 3)

    /*  Context creation and access
        Each context create its own ImFontAtlas by default. You may instance one yourself and pass it to Context()
        to share a font atlas between imgui contexts.
        None of those functions is reliant on the current context.
        ~CreateContext */
    init {
        if (gImGui == null)
            setCurrent()

        initialize()
    }

    fun initialize() {
        assert(!initialized && !g.settingsLoaded)

        // Add .ini handle for ImGuiWindow type
        settingsHandlers += SettingsHandler().apply {
            typeName = "Window"
            typeHash = hash("Window")
            clearAllFn = ::windowSettingsHandler_ClearAll
            readOpenFn = ::windowSettingsHandler_ReadOpen
            readLineFn = ::windowSettingsHandler_ReadLine
            applyAllFn = ::windowSettingsHandler_ApplyAll
            writeAllFn = ::windowSettingsHandler_WriteAll
        }
//        #ifdef IMGUI_HAS_TABLE
//                // Add .ini handle for ImGuiTable type
//                {
//                    ImGuiSettingsHandler ini_handler;
//                    ini_handler.TypeName = "Table";
//                    ini_handler.TypeHash = ImHashStr("Table");
//                    ini_handler.ReadOpenFn = TableSettingsHandler_ReadOpen;
//                    ini_handler.ReadLineFn = TableSettingsHandler_ReadLine;
//                    ini_handler.WriteAllFn = TableSettingsHandler_WriteAll;
//                    g.SettingsHandlers.push_back(ini_handler);
//                }
//        #endif // #ifdef IMGUI_HAS_TABLE

        // Create default viewport
        val viewport = ViewportP()
        viewport.id = IMGUI_VIEWPORT_DEFAULT_ID
        viewport.idx = 0
        viewport.platformWindowCreated = true
        g.viewports += viewport
        g.platformIO.mainViewport = g.viewports[0] // Make it accessible in public-facing GetPlatformIO() immediately (before the first call to EndFrame)
        g.platformIO.viewports += g.viewports[0]

        // Extensions
        dockContextInitialize(g)

        initialized = true
    }

    fun setCurrent() {
        gImGui = this
    }

    /** Destroy current context
     *  ~DestroyContext */
    fun destroy() {
        shutdown()
        if (gImGui === this)
            gImGui = null // SetCurrentContext(NULL);
    }

    /** This function is merely here to free heap allocations.
     *  ~Shutdown(ImGuiContext* context)    */
    fun shutdown() {

        val g = this
        /*  The fonts atlas can be used prior to calling NewFrame(), so we clear it even if g.Initialized is FALSE
            (which would happen if we never called NewFrame)         */
        if (fontAtlasOwnedByContext)
            io.fonts.locked = false
        io.fonts.clear()

        // Cleanup of other data are conditional on actually having initialized Dear ImGui.
        if (!initialized) return

        // Save settings (unless we haven't attempted to load them: CreateContext/DestroyContext without a call to NewFrame shouldn't save an empty file)
        if (settingsLoaded)
            io.iniFilename?.let {
                val backupContext = gImGui!!
                this.setCurrent()
                saveIniSettingsToDisk(it)
                backupContext.setCurrent()
            }

        // Destroy platform windows
        val backupContext = gImGui
        this.setCurrent()
        destroyPlatformWindows()
        backupContext!!.setCurrent()

        // Shutdown extensions
        dockContextShutdown(g)

        // Notify hooked test engine, if any
        callContextHooks(this, ContextHookType.Shutdown)

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
        hoveredRootWindow = null
        hoveredWindowUnderMovingWindow = null
        activeIdWindow = null
        activeIdPreviousFrameWindow = null
        movingWindow = null
        settingsWindows.clear()
        colorModifiers.clear()
        styleModifiers.clear()
        fontStack.clear()
        openPopupStack.clear()
        beginPopupStack.clear()

        currentViewport = null
        mouseViewport = null
        mouseLastHoveredViewport = null
        viewports.forEach(ViewportP::destroy)
        viewports.clear()

        tabBars.clear()
        currentTabBarStack.clear()
        shrinkWidthBuffer.clear()

        clipboardHandlerData = ""
        menusIdSubmittedThisFrame.clear()
        inputTextState.clearFreeMemory()

        settingsWindows.clear()
        settingsHandlers.clear()

        if (logFile != null) {
            logFile = null
        }
        logBuffer.setLength(0)

        initialized = false
    }
}


//-----------------------------------------------------------------------------
// [SECTION] Generic context hooks
//-----------------------------------------------------------------------------

typealias ContextHookCallback = (ctx: Context, hook: ContextHook) -> Unit

enum class ContextHookType { NewFramePre, NewFramePost, EndFramePre, EndFramePost, RenderPre, RenderPost, Shutdown }

/** Hook for extensions like ImGuiTestEngine */
class ContextHook {
    var type = ContextHookType.NewFramePre
    var owner: ID = 0
    var callback: ContextHookCallback? = null
    var userData: Any? = null
}