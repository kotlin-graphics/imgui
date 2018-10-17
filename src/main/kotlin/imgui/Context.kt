package imgui

import com.sun.jdi.VirtualMachine
import glm_.glm
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.impl.windowsIme.HWND
import imgui.internal.*
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

/** Main imgui context */
class Context(sharedFontAtlas: FontAtlas? = null) {

    var initialized = false
    /** Set by NewFrame(), cleared by EndFrame()/Render() */
    var frameScopeActive = false
    /** Io.Fonts-> is owned by the ImGuiContext and will be destructed along with it.   */
    var fontAtlasOwnedByContext = sharedFontAtlas != null

    var io = IO(sharedFontAtlas)

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
    /** Windows, sorted in display order, back to front */
    val windows = ArrayList<Window>()
    /** Windows, sorted in focus order, back to front */
    val windowsFocusOrder = ArrayList<Window>()

    val windowsSortBuffer = ArrayList<Window>()

    val currentWindowStack = Stack<Window>()

    val windowsById = mutableMapOf<Int, Window>()

    var windowsActiveCount = 0
    /** Being drawn into    */
    var currentWindow: Window? = null
    /** Will catch mouse inputs */
    var hoveredWindow: Window? = null
    /** Will catch mouse inputs (for focus/move only)   */
    var hoveredRootWindow: Window? = null
    /** Hovered widget  */
    var hoveredId: ID = 0

    var hoveredIdAllowOverlap = false

    var hoveredIdPreviousFrame: ID = 0

    var hoveredIdTimer = 0f
    /** Active widget   */
    var activeId: ID = 0

    var activeIdPreviousFrame: ID = 0
    /** Active widget has been seen this frame (we can't use a bool as the ActiveId may change within the frame) */
    var activeIdIsAlive: ID = 0

    var activeIdTimer = 0f
    /** Set at the time of activation for one frame */
    var activeIdIsJustActivated = false
    /** Active widget allows another widget to steal active id (generally for overlapping widgets, but not always)   */
    var activeIdAllowOverlap = false
    /** Was the value associated to the widget edited over the course of the Active state. */
    var activeIdHasBeenEdited = false

    var activeIdPreviousFrameIsAlive = false

    var activeIdPreviousFrameHasBeenEdited = false
    /** Active widget allows using directional navigation (e.g. can activate a button and move away from it)    */
    var activeIdAllowNavDirFlags = 0
    /** Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior) */
    var activeIdClickOffset = Vec2(-1)

    var activeIdWindow: Window? = null

    var activeIdPreviousFrameWindow: Window? = null
    /** Activating with mouse or nav (gamepad/keyboard) */
    var activeIdSource = InputSource.None
    /** Store the last non-zero ActiveId, useful for animation. */
    var lastActiveId: ID = 0
    /** Store the last non-zero ActiveId timer since the beginning of activation, useful for animation. */
    var lastActiveIdTimer = 0f
    /** Track the window we clicked on (in order to preserve focus).
     *  The actually window that is moved is generally MovingWindow.rootWindow.  */
    var movingWindow: Window? = null
    /** Stack for PushStyleColor()/PopStyleColor()  */
    var colorModifiers = Stack<ColorMod>()
    /** Stack for PushStyleVar()/PopStyleVar()  */
    val styleModifiers = Stack<StyleMod>()
    /** Stack for PushFont()/PopFont()  */
    val fontStack = Stack<Font>()
    /** Which popups are open (persistent)  */
    val openPopupStack = Stack<PopupRef>()
    /** Which level of BeginPopup() we are in (reset every frame)   */
    val currentPopupStack = Stack<PopupRef>()

    /** Storage for SetNextWindow** functions   */
    val nextWindowData = NextWindowData()
    /** Storage for SetNextTreeNode** functions */
    var nextTreeNodeOpenVal = false

    var nextTreeNodeOpenCond = Cond.Null

    //------------------------------------------------------------------
    // Navigation data (for gamepad/keyboard)
    //------------------------------------------------------------------

    /** Focused window for navigation. Could be called 'FocusWindow'    */
    var navWindow: Window? = null
    /** Focused item for navigation */
    var navId: ID = 0
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
    /** Set by ActivateItem(), queued until next frame  */
    var navNextActivateId: ID = 0
    /** Keyboard or Gamepad mode? */
    var navInputSource = InputSource.None
    /** Rectangle used for scoring, in screen space. Based of window.dc.navRefRectRel[], modified for directional navigation scoring.  */
    var navScoringRectScreen = Rect()
    /** Metrics for debugging   */
    var navScoringCount = 0
    /** When selecting a window (holding Menu+FocusPrev/Next, or equivalent of CTRL-TAB) this window is temporarily displayed front-most.   */
    var navWindowingTarget: Window? = null
    /** Record of last valid NavWindowingTarget until DimBgRatio and NavWindowingHighlightAlpha becomes 0f */
    var navWindowingTargetAnim: Window? = null

    val navWindowingList = ArrayList<Window>()

    var navWindowingTimer = 0f

    var navWindowingHighlightAlpha = 0f

    var navWindowingToggleLayer = false
    /** Layer we are navigating on. For now the system is hard-coded for 0 = main contents and 1 = menu/title bar,
     *  may expose layers later. */
    var navLayer = 0
    /** == NavWindow->DC.FocusIdxTabCounter at time of NavId processing */
    var navIdTabCounter = Int.MAX_VALUE
    /** Nav widget has been seen this frame ~~ NavRefRectRel is valid   */
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

    var navInitResultId: ID = 0

    var navInitResultRectRel = Rect()
    /** Set by manual scrolling, if we scroll to a point where NavId isn't visible we reset navigation from visible items   */
    var navMoveFromClampedRefRect = false
    /** Move request for this frame */
    var navMoveRequest = false

    var navMoveRequestFlags: NavMoveFlags = 0
    /** None / ForwardQueued / ForwardActive (this is used to navigate sibling parent menus from a child menu)  */
    var navMoveRequestForward = NavForward.None
    /** Direction of the move request (left/right/up/down), direction of the previous move request  */
    var navMoveDir = Dir.None
    /** Direction of the move request (left/right/up/down), direction of the previous move request  */
    var navMoveDirLast = Dir.None

    var navMoveClipDir = Dir.None
    /** Best move request candidate within NavWindow    */
    var navMoveResultLocal = NavMoveResult()
    /** Best move request candidate within NavWindow that are mostly visible (when using NavMoveFlags.AlsoScoreVisibleSet flag) */
    val navMoveResultLocalVisibleSet = NavMoveResult()
    /** Best move request candidate within NavWindow's flattened hierarchy (when using WindowFlags.NavFlattened flag)   */
    var navMoveResultOther = NavMoveResult()


    // ------------------------------------------------------------------
    // Render
    //------------------------------------------------------------------

    /** Main ImDrawData instance to pass render information to the user */
    var drawData = DrawData()

    val drawDataBuilder = DrawDataBuilder()
    /** 0.0..1.0 animation when fading in a dimming background (for modal window and CTRL+TAB list) */
    var dimBgRatio = 0f
    /** Optional software render of mouse cursors, if io.MouseDrawCursor is set + a few debug overlays  */
    var overlayDrawList = DrawList(null).apply {
        _data = drawListSharedData
        _ownerName = "##Overlay" // Give it a name for debugging
    }

    var mouseCursor = MouseCursor.Arrow


    //------------------------------------------------------------------
    // Drag and Drop
    //------------------------------------------------------------------
    var dragDropActive = false

    var dragDropWithinSourceOrTarget = false

    var dragDropSourceFlags: DragDropFlags = 0

    var dragDropSourceFrameCount = -1

    var dragDropMouseButton = -1

    var dragDropPayload = Payload()

    var dragDropTargetRect = Rect()

    var dragDropTargetId: ID = 0

    var dragDropAcceptFlags: DragDropFlags = 0
    /** Target item surface (we resolve overlapping targets by prioritizing the smaller surface) */
    var dragDropAcceptIdCurrRectSurface = 0f
    /** Target item id (set at the time of accepting the payload) */
    var dragDropAcceptIdCurr: ID = 0
    /** Target item id from previous frame (we need to store this to allow for overlapping drag and drop targets) */
    var dragDropAcceptIdPrev: ID = 0
    /** Last time a target expressed a desire to accept the source */
    var dragDropAcceptFrameCount = -1
    /** We don't expose the ImVector<> directly */
    var dragDropPayloadBufHeap = ByteBuffer.allocate(0)
    /** Local buffer for small payloads */
    var dragDropPayloadBufLocal = ByteBuffer.allocate(8)

    //------------------------------------------------------------------
    // Widget state
    //------------------------------------------------------------------

    var inputTextState = TextEditState()

    var inputTextPasswordFont = Font()
    /** Temporary text input when CTRL+clicking on a slider, etc.   */
    var scalarAsInputTextId: ID = 0
    /** Store user options for color edit widgets   */
    var colorEditOptions: ColorEditFlags = ColorEditFlag._OptionsDefault.i

    val colorPickerRef = Vec4()

    var dragCurrentAccumDirty = false
    /** Accumulator for dragging modification. Always high-precision, not rounded by end-user precision settings */
    var dragCurrentAccum = 0f
    /** If speed == 0.0f, uses (max-min) * DragSpeedDefaultRatio    */
    var dragSpeedDefaultRatio = 1f / 100f

    /** Distance between mouse and center of grab box, normalized in parent space. Use storage? */
    var scrollbarClickDeltaToGrabCenter = Vec2()

    var tooltipOverrideCount = 0
    /** If no custom clipboard handler is defined   */
    var privateClipboard = ""
    /** Cursor position request to the OS Input Method Editor   */
    var platformImePos = Vec2(Float.MAX_VALUE)
    /** Last cursor position passed to the OS Input Method Editor   */
    var platformImeLastPos = Vec2(Float.MAX_VALUE)

    var imeInProgress = false
//    var imeLastKey = 0

    //------------------------------------------------------------------
    // Settings
    //------------------------------------------------------------------

    var settingsLoaded = false
    /** Save .ini Settings to memory when time reaches zero   */
    var settingsDirtyTimer = 0f
    /** In memory .ini Settings for Window  */
    var settingsIniData = ""
    /** ImGuiWindow .ini settings entries (parsed from the last loaded .ini file and maintained on saving) */
    val settingsWindows = ArrayList<WindowSettings>()

    //------------------------------------------------------------------
    // Logging
    //------------------------------------------------------------------

    var logEnabled = false
    /** If != NULL log to stdout/ file  */
    var logFile: File? = null
    /** Accumulation buffer when log to clipboard. This is pointer so our GImGui static constructor doesn't call heap allocators.   */
    var logClipboard = StringBuilder()

    var logStartDepth = 0

    var logAutoExpandMaxDepth = 2


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

//    char                    TempBuffer[1024*3+1];               // Temporary text buffer

    /* Context creation and access
        Each context create its own ImFontAtlas by default. You may instance one yourself and pass it to Context()
        to share a font atlas between imgui contexts.
        All those functions are not reliant on the current context.     */
    init {
        if (gImGui == null) setCurrent()

        // ~initialize(ctx)
        assert(!g.initialized && !g.settingsLoaded)

        g.initialized = true
    }

    /** This function is merely here to free heap allocations.     */
    fun shutdown() {

        /*  The fonts atlas can be used prior to calling NewFrame(), so we clear it even if g.Initialized is FALSE
            (which would happen if we never called NewFrame)         */
        io.fonts.clear()

        // Cleanup of other data are conditional on actually having initialized ImGui.
        if (!g.initialized) return

        // Save settings (unless we haven't attempted to load them: CreateContext/DestroyContext without a call to NewFrame shouldn't save an empty file)
        if (g.settingsLoaded)
            io.iniFilename?.let(::saveIniSettingsToDisk)

        // Clear everything else
        g.windows.forEach { it.clear() }
        g.windows.clear()
        g.windowsFocusOrder.clear()
        g.windowsSortBuffer.clear()
        g.currentWindow = null
        g.currentWindowStack.clear()
        g.windowsById.clear()
        g.navWindow = null
        g.hoveredWindow = null
        g.hoveredRootWindow = null
        g.activeIdWindow = null
        g.activeIdPreviousFrameWindow = null
        g.movingWindow = null
        g.settingsWindows.clear()
        g.colorModifiers.clear()
        g.styleModifiers.clear()
        g.fontStack.clear()
        g.openPopupStack.clear()
        g.currentPopupStack.clear()
        g.drawDataBuilder.clear()
        g.overlayDrawList.clearFreeMemory()
        g.privateClipboard = ""
        g.inputTextState.textW = charArrayOf()
        g.inputTextState.initialText = charArrayOf()
        g.inputTextState.tempBuffer = charArrayOf()

//        if (g.logFile != null && g.logFile != stdout) { TODO
//            fclose(g.LogFile)
//            g.LogFile = NULL
//        }
        g.logClipboard.setLength(0)

        g.initialized = false
    }
}

fun Context?.setCurrent() {
    gImGui = this
}

/** Destroy current context */
fun Context?.destroy() {
    val c = this ?: g
    c.shutdown()
    if (gImGui === c)
        c.setCurrent()
}

/** This is where your app communicate with Dear ImGui. Access via ImGui::GetIO().
 *  Read 'Programmer guide' section in .cpp file for general usage. */
class IO(sharedFontAtlas: FontAtlas?) {

    //------------------------------------------------------------------
    // Configuration (fill once)
    //------------------------------------------------------------------

    /** See ConfigFlags enum. Set by user/application. Gamepad/keyboard navigation options, etc. */
    var configFlags: ConfigFlags = 0
    /** Set ImGuiBackendFlags_ enum. Set by imgui_impl_xxx files or custom back-end to communicate features supported by the back-end. */
    var backendFlags: BackendFlags = 0
    /** Main display size, in pixels. For clamping windows positions.    */
    var displaySize = Vec2i(-1)
    /** Time elapsed since last frame, in seconds.  */
    var deltaTime = 1f / 60f
    /** Minimum time between saving positions/sizes to .ini file, in seconds.   */
    var iniSavingRate = 5f
    /** Path to .ini file. Set NULL to disable automatic .ini loading/saving, if e.g. you want to manually load/save from memory. */
    var iniFilename: String? = "imgui.ini"
    /** Path to .log file (default parameter to ImGui::LogToFile when no file is specified).    */
    var logFilename = "imgui_log.txt"
    /** Time for a double-click, in seconds.    */
    var mouseDoubleClickTime = 0.3f
    /** Distance threshold to stay in to validate a double-click, in pixels.    */
    var mouseDoubleClickMaxDist = 6f
    /** Distance threshold before considering we are dragging.   */
    var mouseDragThreshold = 6f
    /** Map of indices into the KeysDown[512] entries array which represent your "native" keyboard state.   */
    var keyMap = IntArray(Key.COUNT) { -1 }
    /** When holding a key/button, time before it starts repeating, in seconds (for buttons in Repeat mode, etc.).  */
    var keyRepeatDelay = 0.25f
    /** When holding a key/button, rate at which it repeats, in seconds.    */
    var keyRepeatRate = 0.05f

//    void*         UserData;                 // = NULL               // Store your own data for retrieval by callbacks.

    /** Load and assemble one or more fonts into a single tightly packed texture. Output to Fonts array.    */
    val fonts = sharedFontAtlas ?: FontAtlas()
    /** Global scale all fonts  */
    var fontGlobalScale = 1f
    /** Allow user scaling text of individual window with CTRL+Wheel.   */
    var fontAllowUserScaling = false
    /** Font to use on NewFrame(). Use NULL to useMouseDragThreshold s Fonts->Fonts[0].    */
    var fontDefault: Font? = null
    /** For retina display or other situations where window coordinates are different from framebuffer coordinates.
     *  User storage only, presently not used by ImGui. */
    var displayFramebufferScale = Vec2(1f)
    /** If you use DisplaySize as a virtual space larger than your screen, set DisplayVisibleMin/Max to the visible
     *  area.   */
    var displayVisibleMin = Vec2()
    /** If the values are the same, we defaults to Min=(0.0f) and Max=DisplaySize   */
    var displayVisibleMax = Vec2()

    //------------------------------------------------------------------
    // Miscellaneous options
    //------------------------------------------------------------------

    /** Request ImGui to draw a mouse cursor for you (if you are on a platform without a mouse cursor). */
    var mouseDrawCursor = false
    /** = defined(__APPLE__), OS X style: Text editing cursor movement using Alt instead of Ctrl, Shortcuts using
     *  Cmd/Super instead of Ctrl, Line/Text Start and End using Cmd + Arrows instead of Home/End, Double click selects
     *  by word instead of selecting whole text, Multi-selection in lists uses Cmd/Super instead of Ctrl
     *  (was called io.OptMacOSXBehaviors prior to 1.63) */
    var configMacOSXBehaviors = false  // JVM TODO
    /** Set to false to disable blinking cursor, for users who consider it distracting. (was called: io.OptCursorBlink prior to 1.63) */
    var configInputTextCursorBlink = true
    /** [BETA] Enable resizing of windows from their edges and from the lower-left corner.
     *  This requires (io.backendFlags has BackendFlags.HasMouseCursors) because it needs mouse cursor feedback.
     *  (This used to be the WindowFlag.ResizeFromAnySide flag) */
    var configResizeWindowsFromEdges = false

    //------------------------------------------------------------------
    // User Functions
    //------------------------------------------------------------------

    // Optional: access OS clipboard
    // (default to use native Win32 clipboard on Windows, otherwise uses a private clipboard. Override to access OS clipboard on other architectures)
    var getClipboardTextFn: ((userData: Any) -> Unit)? = null
    var setClipboardTextFn: ((userData: Any, text: String) -> Unit)? = null
    lateinit var clipboardUserData: Any

    //    // Optional: override memory allocations. MemFreeFn() may be called with a NULL pointer.
//    // (default to posix malloc/free)
//    void*       (*MemAllocFn)(size_t sz);
//    void        (*MemFreeFn)(void* ptr);
//
    // Optional: notify OS Input Method Editor of the screen position of your cursor for text input position (e.g. when using Japanese/Chinese IME in Windows)
    // (default to use native imm32 api on Windows)
    val imeSetInputScreenPosFn: ((x: Int, y: Int) -> Unit)? = imeSetInputScreenPosFn_Win32.takeIf { Platform.get() == Platform.WINDOWS }
    /** (Windows) Set this to your HWND to get automatic IME cursor positioning.    */
    var imeWindowHandle: HWND = NULL

    //------------------------------------------------------------------
    // Input - Fill before calling NewFrame()
    //------------------------------------------------------------------

    /** Mouse position, in pixels (set to -1,-1 if no mouse / on another screen, etc.)  */
    var mousePos = Vec2(-Float.MAX_VALUE)
    /** Mouse buttons: 0=left, 1=right, 2=middle + extras. ImGui itself mostly only uses left button (BeginPopupContext** are
    using right button). Others buttons allows us to track if the mouse is being used by your application +
    available to user as a convenience via IsMouse** API.   */
    val mouseDown = BooleanArray(5)
    /** Mouse wheel Vertical: 1 unit scrolls about 5 lines text. */
    var mouseWheel = 0f
    /** Mouse wheel Horizontal. Most users don't have a mouse with an horizontal wheel, may not be filled by all back-ends.   */
    var mouseWheelH = 0f
    /** Keyboard modifier pressed: Control  */
    var keyCtrl = false
    /** Keyboard modifier pressed: Shift    */
    var keyShift = false
    /** Keyboard modifier pressed: Alt  */
    var keyAlt = false
    /** Keyboard modifier pressed: Cmd/Super/Windows    */
    var keySuper = false
    /** Keyboard keys that are pressed (ideally left in the "native" order your engine has access to keyboard keys,
     *  so you can use your own defines/enums for keys).   */
    val keysDown = BooleanArray(512)
    /** List of characters input (translated by user from keypress + keyboard state). Fill using addInputCharacter()
     *  helper. */
    val inputCharacters = CharArray(16)
    /** Gamepad inputs (keyboard keys will be auto-mapped and be written here by ::newFrame,
     *  all values will be cleared back to zero in ImGui::EndFrame)   */
    val navInputs = FloatArray(NavInput.COUNT)

    // Functions

    /** Add new character into InputCharacters[]
     *  Pass in translated ASCII characters for text input.
     *  - with glfw you can get those from the callback set in glfwSetCharCallback()
     *  - on Windows you can get those using ToAscii+keyboard state, or via the WM_CHAR message */
    fun addInputCharacter(c: Char) {
        val n = inputCharacters.strlenW
        if (n + 1 < inputCharacters.size)
            inputCharacters[n] = c
    }
//    IMGUI_API void AddInputCharactersUTF8(const char* utf8_chars);      // Add new characters into InputCharacters[] from an UTF-8 string
//    inline void    ClearInputCharacters() { InputCharacters[0] = 0; }   // Clear the text input buffer manually


    //------------------------------------------------------------------
    // Output - Retrieve after calling NewFrame()
    //------------------------------------------------------------------

    /** When io.wantCaptureMouse is true, imgui will use the mouse inputs, do not dispatch them to your main game/application
     *  (in both cases, always pass on mouse inputs to imgui). (e.g. unclicked mouse is hovering over an imgui window,
     *  widget is active, mouse was clicked over an imgui window, etc.). */
    var wantCaptureMouse = false
    /** When io.wantCaptureKeyboard is true, imgui will use the keyboard inputs, do not dispatch them to your main game/application
     *  (in both cases, always pass keyboard inputs to imgui).
     *  (e.g. InputText active, or an imgui window is focused and navigation is enabled, etc.). */
    var wantCaptureKeyboard = false
    /** Mobile/console: when IO.wantTextInput is true, you may display an on-screen keyboard. This is set by ImGui when
     *  it wants textual keyboard input to happen (e.g. when a InputText widget is active). */
    var wantTextInput = false
    /** MousePos has been altered, back-end should reposition mouse on next frame.
     *  Set only when ConfigFlag.NavEnableSetMousePos flag is enabled in IO.configFlags.    */
    var wantSetMousePos = false
    /** When manual .ini load/save is active (io.IniFilename == NULL), this will be set to notify your application that
     *  you can call SaveIniSettingsToMemory() and save yourself. IMPORTANT: You need to clear io.WantSaveIniSettings yourself. */
    var wantSaveIniSettings = false
    /** Directional navigation is currently allowed (will handle KeyNavXXX events) = a window is focused and it doesn't
     *  use the WindowFlag.NoNavInputs flag.   */
    var navActive = false
    /** Directional navigation is visible and allowed (will handle KeyNavXXX events). */
    var navVisible = false
    /** Application framerate estimation, in frame per second. Solely for convenience. Rolling average estimation based
    on IO.DeltaTime over 120 frames */
    var framerate = 0f
    /** Number of active memory allocations */
    val metricsAllocs get() = Debug.instanceCounts
    /** Vertices output during last call to Render()    */
    var metricsRenderVertices = 0
    /** Indices output during last call to Render() = number of triangles * 3   */
    var metricsRenderIndices = 0
    /** Number of visible windows */
    var metricsRenderWindows = 0
    /** Number of active windows */
    var metricsActiveWindows = 0
    /** Number of active allocations, updated by MemAlloc/MemFree based on current context. May be off if you have multiple imgui contexts. */
    var metricsActiveAllocations = 0
    /** Mouse delta. Note that this is zero if either current or previous position are invalid (-FLOAT_MAX_VALUE), so a
    disappearing/reappearing mouse won't have a huge delta.   */
    var mouseDelta = Vec2()


    //------------------------------------------------------------------
    // [Private] ImGui will maintain those fields. Forward compatibility not guaranteed!
    //------------------------------------------------------------------

    /** Previous mouse position temporary storage (nb: not for public use, set to MousePos in NewFrame())   */
    var mousePosPrev = Vec2(-Float.MAX_VALUE)
    /** Position at time of clicking    */
    val mouseClickedPos = Array(5) { Vec2() }
    /** Time of last click (used to figure out double-click)    */
    val mouseClickedTime = DoubleArray(5)
    /** Mouse button went from !Down to Down    */
    val mouseClicked = BooleanArray(5)
    /** Has mouse button been double-clicked?    */
    val mouseDoubleClicked = BooleanArray(5)
    /** Mouse button went from Down to !Down    */
    val mouseReleased = BooleanArray(5)
    /** Track if button was clicked inside a window. We don't request mouse capture from the application if click
    started outside ImGui bounds.   */
    var mouseDownOwned = BooleanArray(5)
    /** Duration the mouse button has been down (0.0f == just clicked)  */
    val mouseDownDuration = FloatArray(5) { -1f }
    /** Previous time the mouse button has been down    */
    val mouseDownDurationPrev = FloatArray(5) { -1f }
    /** Maximum distance, absolute, on each axis, of how much mouse has traveled from the clicking point    */
    val mouseDragMaxDistanceAbs = Array(5) { Vec2() }
    /** Squared maximum distance of how much mouse has traveled from the clicking point */
    val mouseDragMaxDistanceSqr = FloatArray(5)
    /** Duration the keyboard key has been down (0.0f == just pressed)  */
    val keysDownDuration = FloatArray(512) { -1f }
    /** Previous duration the key has been down */
    val keysDownDurationPrev = FloatArray(512) { -1f }

    val navInputsDownDuration = FloatArray(NavInput.COUNT) { -1f }

    val navInputsDownDurationPrev = FloatArray(NavInput.COUNT)
}

// for IO.keyMap

operator fun IntArray.set(index: Key, value: Int) {
    this[index.i] = value
}

operator fun IntArray.get(index: Key) = get(index.i)

// for IO.navInputs

operator fun FloatArray.set(index: NavInput, value: Float) {
    this[index.i] = value
}

operator fun FloatArray.get(index: NavInput) = get(index.i)

/** You may modify the ImGui::GetStyle() main instance during initialization and before NewFrame().
 *  During the frame, use ImGui::PushStyleVar(StyleVar.XXXX)/PopStyleVar() to alter the main style values,
 *  and ImGui::PushStyleColor(Col.XXX)/PopStyleColor() for colors. */
class Style {

    /**  Global alpha applies to everything in ImGui.    */
    var alpha = 1f
    /** Padding within a window. */
    var windowPadding = Vec2(8)
    /** Radius of window corners rounding. Set to 0.0f to have rectangular windows.  */
    var windowRounding = 7f
    /** Thickness of border around windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly).  */
    var windowBorderSize = 1f
    /** Minimum window size. This is a global setting. If you want to constraint individual windows, use SetNextWindowSizeConstraints(). */
    var windowMinSize = Vec2i(32)
    /** Alignment for title bar text    */
    var windowTitleAlign = Vec2(0f, 0.5f)
    /** Radius of child window corners rounding. Set to 0.0f to have rectangular child windows.  */
    var childRounding = 0f
    /** Thickness of border around child windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly). */
    var childBorderSize = 1f
    /** Radius of popup window corners rounding. (Note that tooltip windows use WindowRounding) */
    var popupRounding = 0f
    /** Thickness of border around popup/tooltip windows. Generally set to 0f or 1f.
     *  (Other values are not well tested and more CPU/GPU costly). */
    var popupBorderSize = 1f
    /** Padding within a framed rectangle (used by most widgets).    */
    var framePadding = Vec2(4, 3)
    /** Radius of frame corners rounding. Set to 0.0f to have rectangular frames (used by most widgets).    */
    var frameRounding = 0f
    /** Thickness of border around frames. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly).    */
    var frameBorderSize = 0f
    /** Horizontal and vertical spacing between widgets/lines.   */
    var itemSpacing = Vec2(8, 4)
    /** Horizontal and vertical spacing between within elements of a composed widget (e.g. a slider and its label).  */
    var itemInnerSpacing = Vec2(4)
    /** Expand reactive bounding box for touch-based system where touch position is not accurate enough. Unfortunately
     *  we don't sort widgets so priority on overlap will always be given to the first widget. So don't grow this too much!   */
    var touchExtraPadding = Vec2()
    /** Horizontal spacing when e.g. entering a tree node. Generally == (FontSize + FramePadding.x*2).  */
    var indentSpacing = 21f
    /** Minimum horizontal spacing between two columns.  */
    var columnsMinSpacing = 6f
    /** Width of the vertical scrollbar, Height of the horizontal scrollbar. */
    var scrollbarSize = 16f
    /** Radius of grab corners rounding for scrollbar.   */
    var scrollbarRounding = 9f
    /** Minimum width/height of a grab box for slider/scrollbar */
    var grabMinSize = 10f
    /** Radius of grabs corners rounding. Set to 0.0f to have rectangular slider grabs. */
    var grabRounding = 0f
    /** Alignment of button text when button is larger than text.   */
    var buttonTextAlign = Vec2(0.5f)
    /** Window position are clamped to be visible within the display area by at least this amount.
     *  Only applies to regular windows.    */
    var displayWindowPadding = Vec2(20)
    /** If you cannot see the edges of your screen (e.g. on a TV) increase the safe area padding. Apply to popups/tooltips
     *  as well regular windows.  NB: Prefer configuring your TV sets correctly!   */
    var displaySafeAreaPadding = Vec2(3)
    /** Scale software rendered mouse cursor (when io.MouseDrawCursor is enabled). May be removed later.    */
    var mouseCursorScale = 1f
    /** Enable anti-aliasing on lines/borders. Disable if you are really short on CPU/GPU.  */
    var antiAliasedLines = true
    /**  Enable anti-aliasing on filled shapes (rounded rectangles, circles, etc.)  */
    var antiAliasedFill = true
    /** Tessellation tolerance when using pathBezierCurveTo() without a specific number of segments.
     *  Decrease for highly tessellated curves (higher quality, more polygons), increase to reduce quality. */
    var curveTessellationTol = 1.25f

    val colors = ArrayList<Vec4>()

    /** JVM IMGUI   */
    var locale: Locale = Locale.US
//    var locale: Locale = Locale("no", "NO")
//    val locale = Locale.getDefault()

    init {
        ImGui.styleColorsClassic(this)
    }

    constructor()

    constructor(style: Style) {
        alpha = style.alpha
        windowPadding put style.windowPadding
        windowRounding = style.windowRounding
        windowBorderSize = style.windowBorderSize
        windowMinSize put style.windowMinSize
        windowTitleAlign put style.windowTitleAlign
        childRounding = style.childRounding
        childBorderSize = style.childBorderSize
        popupRounding = style.popupRounding
        popupBorderSize = style.popupBorderSize
        framePadding put style.framePadding
        frameRounding = style.frameRounding
        frameBorderSize = style.frameBorderSize
        itemSpacing put style.itemSpacing
        itemInnerSpacing put style.itemInnerSpacing
        touchExtraPadding put style.touchExtraPadding
        indentSpacing = style.indentSpacing
        columnsMinSpacing = style.columnsMinSpacing
        scrollbarSize = style.scrollbarSize
        scrollbarRounding = style.scrollbarRounding
        grabMinSize = style.grabMinSize
        grabRounding = style.grabRounding
        buttonTextAlign put style.buttonTextAlign
        displayWindowPadding put style.displayWindowPadding
        displaySafeAreaPadding put style.displaySafeAreaPadding
        antiAliasedLines = style.antiAliasedLines
        antiAliasedFill = style.antiAliasedFill
        curveTessellationTol = style.curveTessellationTol
        style.colors.forEach { colors.add(Vec4(it)) }
//        locale = style.locale
    }

    /** To scale your entire UI (e.g. if you want your app to use High DPI or generally be DPI aware) you may use this
     *  helper function. Scaling the fonts is done separately and is up to you.
     *  Tips: if you need to change your scale multiple times, prefer calling this on a freshly initialized Style
     *  structure rather than scaling multiple times (because floating point multiplications are lossy).    */
    fun scaleAllSizes(scaleFactor: Float) {
        windowPadding = glm.floor(windowPadding * scaleFactor)
        windowRounding = glm.floor(windowRounding * scaleFactor)
        windowMinSize.put(glm.floor(windowMinSize.x * scaleFactor), glm.floor(windowMinSize.y * scaleFactor))
        childRounding = glm.floor(childRounding * scaleFactor)
        popupRounding = glm.floor(popupRounding * scaleFactor)
        framePadding = glm.floor(framePadding * scaleFactor)
        frameRounding = glm.floor(frameRounding * scaleFactor)
        itemSpacing = glm.floor(itemSpacing * scaleFactor)
        itemInnerSpacing = glm.floor(itemInnerSpacing * scaleFactor)
        touchExtraPadding = glm.floor(touchExtraPadding * scaleFactor)
        indentSpacing = glm.floor(indentSpacing * scaleFactor)
        columnsMinSpacing = glm.floor(columnsMinSpacing * scaleFactor)
        scrollbarSize = glm.floor(scrollbarSize * scaleFactor)
        scrollbarRounding = glm.floor(scrollbarRounding * scaleFactor)
        grabMinSize = glm.floor(grabMinSize * scaleFactor)
        grabRounding = glm.floor(grabRounding * scaleFactor)
        displayWindowPadding = glm.floor(displayWindowPadding * scaleFactor)
        displaySafeAreaPadding = glm.floor(displaySafeAreaPadding * scaleFactor)
        mouseCursorScale = glm.floor(mouseCursorScale * scaleFactor)
    }
}

object Debug {

    var vm: VirtualMachine? = null
    /** Instance count update interval in seconds   */
    var updateInterval = 5
    private var lastUpdate = System.nanoTime()

    init {
        try {
//            var ac: AttachingConnector? = null
//            for (x in Bootstrap.virtualMachineManager().attachingConnectors()) {
//                if (x.javaClass.name.toLowerCase().indexOf("socket") != -1) {
//                    ac = x
//                    break
//                }
//            }
//            if (ac == null) {
//                throw Error("No socket attaching connector found")
//            }
//            val connectArgs = HashMap<String, Argument>(ac.defaultArguments())
//            connectArgs["hostname"]!!.setValue("127.0.0.1")
//            connectArgs["port"]!!.setValue(Integer.toString(3001))
//            connectArgs["timeout"]!!.setValue("3000")
//            vm = ac.attach(connectArgs)
        } catch (error: Exception) {
            System.err.println("Couldn't retrieve the number of allocations, $error")
        }
    }

    val instanceCounts
        get() = when {
            vm != null -> {
                val now = System.nanoTime()
                if ((now - lastUpdate) > updateInterval * 1e9) {
                    cachedInstanceCounts = countInstances()
                    lastUpdate = now
                }
                cachedInstanceCounts
            }
            else -> -1
        }

    private fun countInstances() = vm?.instanceCounts(vm?.allClasses())?.sum() ?: -1

    private var cachedInstanceCounts = countInstances()
}

/** for style.colors    */
operator fun ArrayList<Vec4>.get(idx: Col) = this[idx.i]

operator fun ArrayList<Vec4>.set(idx: Col, vec: Vec4) = this[idx.i] put vec

operator fun MutableMap<Int, Float>.set(key: Int, value: Int) = set(key, glm.intBitsToFloat(value))
operator fun MutableMap<Int, Float>.set(key: Int, value: ColorEditFlag) = set(key, glm.intBitsToFloat(value.i))
//operator fun MutableMap<Int, Float>.getInt(key: Int) = 0 TODO float