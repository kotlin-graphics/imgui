package imgui

import com.sun.jdi.VirtualMachine
import glm_.glm
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.internal.*
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

object Context {

    var initialized = false

    var style = Style()

    lateinit var font: Font
    /** (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize(). Text height for current window. */
    var fontSize = 0f
    /** (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Base text height.    */
    var fontBaseSize = 0f

    lateinit var drawListSharedData: DrawListSharedData

    var time = 0f

    var frameCount = 0

    var frameCountEnded = -1

    var frameCountRendered = -1

    val windows = ArrayList<Window>()

    val windowsSortBuffer = ArrayList<Window>()

    val currentWindowStack = Stack<Window>()

    val windowsById = mutableMapOf<Int, Window>()

    var windowsActiveCount = 0
    /** Being drawn into    */
    var currentWindow: Window? = null
    /** Will catch keyboard inputs  */
    var navWindow: Window? = null
    /** Will catch mouse inputs */
    var hoveredWindow: Window? = null
    /** Will catch mouse inputs (for focus/move only)   */
    var hoveredRootWindow: Window? = null
    /** Hovered widget  */
    var hoveredId = 0

    var hoveredIdAllowOverlap = false

    var hoveredIdPreviousFrame = 0

    var hoveredIdTimer = 0f
    /** Active widget   */
    var activeId = 0

    var activeIdPreviousFrame = 0

    var activeIdTimer = 0f
    /** Active widget has been seen this frame   */
    var activeIdIsAlive = false
    /** Set at the time of activation for one frame */
    var activeIdIsJustActivated = false
    /** Active widget allows another widget to steal active id (generally for overlapping widgets, but not always)   */
    var activeIdAllowOverlap = false
    /** Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior) */
    var activeIdClickOffset = Vec2(-1)

    var activeIdWindow: Window? = null
    /** Track the child window we clicked on to move a window.  */
    var movingWindow: Window? = null
    /** == MovedWindow->RootWindow->MoveId  */
    var movingdWindowMoveId = 0
    /** .ini Settings   */
    val settings = ArrayList<WindowSettings>()
    /** Save .ini Settings on disk when time reaches zero   */
    var settingsDirtyTimer = 0f
    /** Stack for PushStyleColor()/PopStyleColor()  */
    var colorModifiers = Stack<ColMod>()
    /** Stack for PushStyleVar()/PopStyleVar()  */
    val styleModifiers = Stack<StyleMod>()
    /** Stack for PushFont()/PopFont()  */
    val fontStack = Stack<Font>()
    /** Which popups are open (persistent)  */
    val openPopupStack = Stack<PopupRef>()
    /** Which level of BeginPopup() we are in (reset every frame)   */
    val currentPopupStack = Stack<PopupRef>()

    // Storage for SetNexWindow** and SetNextTreeNode*** functions
    var setNextWindowPosVal = Vec2()

    var setNextWindowPosPivot = Vec2()

    var setNextWindowSizeVal = Vec2()

    var setNextWindowContentSizeVal = Vec2()

    var setNextWindowCollapsedVal = false

    var setNextWindowPosCond = Cond.Null

    var setNextWindowSizeCond = Cond.Always

    var setNextWindowContentSizeCond = Cond.Null

    var setNextWindowCollapsedCond = Cond.Null
    /** Valid if 'SetNextWindowSizeConstraint' is true  */
    var setNextWindowSizeConstraintRect = Rect()

    var setNextWindowSizeConstraintCallback: SizeConstraintCallback? = null

    var setNextWindowSizeConstraintCallbackUserData: Any? = null

    var setNextWindowSizeConstraint = false

    var setNextWindowFocus = false

    var setNextTreeNodeOpenVal = false

    var setNextTreeNodeOpenCond = 0

    //------------------------------------------------------------------
    // Render
    //------------------------------------------------------------------

    /** Main ImDrawData instance to pass render information to the user */
    var renderDrawData = DrawData()

    val renderDrawLists = Array(3, { ArrayList<DrawList>() })

    var modalWindowDarkeningRatio = 0f
    /** Optional software render of mouse cursors, if io.MouseDrawCursor is set + a few debug overlays  */
    var overlayDrawList = DrawList(null).apply {
        _data = drawListSharedData
        _ownerName = "##Overlay" // Give it a name for debugging
    }

    var mouseCursor = MouseCursor.Arrow

    val mouseCursorData = Array(MouseCursor.Count.i, { MouseCursorData() })


    //------------------------------------------------------------------
    // Drag and Drop
    //------------------------------------------------------------------
    var dragDropActive = false

    var dragDropSourceFlags = 0

    var dragDropMouseButton = -1

    var dragDropPayload = Payload()

    var dragDropTargetRect = Rect()

    var dragDropTargetId = 0

    var dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE // TODO check
    /** Target item id (set at the time of accepting the payload) */
    var dragDropAcceptIdCurr = 0
    /** Target item id from previous frame (we need to store this to allow for overlapping drag and drop targets) */
    var dragDropAcceptIdPrev = 0
    /** Last time a target expressed a desire to accept the source */
    var dragDropAcceptFrameCount = -1
    /** We don't expose the ImVector<> directly */
    lateinit var dragDropPayloadBufHeap: ByteBuffer

    var dragDropPayloadBufLocal = ByteArray(8)

    //------------------------------------------------------------------
    // Widget state
    //------------------------------------------------------------------

    var inputTextState = TextEditState()

    var inputTextPasswordFont = Font()
    /** Temporary text input when CTRL+clicking on a slider, etc.   */
    var scalarAsInputTextId = 0
    /** Store user options for color edit widgets   */
    var colorEditOptions = ColorEditFlags._OptionsDefault.i

    val colorPickerRef = Vec4()
    /** Currently dragged value, always float, not rounded by end-user precision settings   */
    var dragCurrentValue = 0f

    var dragLastMouseDelta = Vec2()
    /** If speed == 0.0f, uses (max-min) * DragSpeedDefaultRatio    */
    var dragSpeedDefaultRatio = 1f / 100f

    var dragSpeedScaleSlow = 1f / 100

    var dragSpeedScaleFast = 10f
    /** Distance between mouse and center of grab box, normalized in parent space. Use storage? */
    var scrollbarClickDeltaToGrabCenter = Vec2()

    var tooltipOverrideCount = 0
    /** If no custom clipboard handler is defined   */
    var privateClipboard = ""
    /** Cursor position request to the OS Input Method Editor   */
    var osImePosRequest = Vec2(-1f)
    /** Last cursor position passed to the OS Input Method Editor   */
    var osImePosSet = Vec2(-1f)

    var imeInProgress = false
    var imeLastKey = 0


    //------------------------------------------------------------------
    // Logging
    //------------------------------------------------------------------

    var logEnabled = false
    /** If != NULL log to stdout/ file  */
    var logFile: File? = null
    /** Else log to clipboard. This is pointer so our GImGui static constructor doesn't call heap allocators.   */
    lateinit var logClipboard: StringBuilder

    var logStartDepth = 0

    var logAutoExpandMaxDepth = 2


    //------------------------------------------------------------------
    // Misc
    //------------------------------------------------------------------

    /** calculate estimate of framerate for user    */
    val framerateSecPerFrame = FloatArray(120)

    var framerateSecPerFrameIdx = 0

    var framerateSecPerFrameAccum = 0f
    /** explicit capture via CaptureInputs() sets those flags   */
    var wantCaptureMouseNextFrame = -1

    var wantCaptureKeyboardNextFrame = -1

    var wantTextInputNextFrame = -1

//    char                    TempBuffer[1024*3+1];               // temporary text buffer
}

/** This is where your app communicate with ImGui. Access via ImGui::GetIO().
 *  Read 'Programmer guide' section in .cpp file for general usage. */
object IO {

    //------------------------------------------------------------------
    // Settings (fill once)
    //------------------------------------------------------------------

    /** Display size, in pixels. For clamping windows positions.    */
    var displaySize = Vec2i(-1)
    /** Time elapsed since last frame, in seconds.  */
    var deltaTime = 1f / 60f
    /** Maximum time between saving positions/sizes to .ini file, in seconds.   */
    var iniSavingRate = 5f
    /** Path to .ini file. NULL to disable .ini saving. */
    var iniFilename: String? = "imgui.ini"
    /** Path to .log file (default parameter to ImGui::LogToFile when no file is specified).    */
    var logFilename = "imgui_log.txt"
    /** Time for a double-click, in seconds.    */
    var mouseDoubleClickTime = 0.3f
    /** Distance threshold to stay in to validate a double-click, in pixels.    */
    var mouseDoubleClickMaxDist = 6f
    /** Distance threshold before considering we are dragging   */
    var mouseDragThreshold = 6f
    /** Map of indices into the KeysDown[512] entries array */
    var keyMap = IntArray(Key.COUNT.i, { -1 })
    /** When holding a key/button, time before it starts repeating, in seconds (for buttons in Repeat mode, etc.).  */
    var keyRepeatDelay = 0.25f
    /** When holding a key/button, rate at which it repeats, in seconds.    */
    var keyRepeatRate = 0.05f

//    void*         UserData;                 // = NULL               // Store your own data for retrieval by callbacks.

    /** Load and assemble one or more fonts into a single tightly packed texture. Output to Fonts array.    */
    val fonts = FontAtlas()
    /** Global scale all fonts  */
    var fontGlobalScale = 1f
    /** Allow user scaling text of individual window with CTRL+Wheel.   */
    var fontAllowUserScaling = false
    /** Font to use on NewFrame(). Use NULL to uses Fonts->Fonts[0].    */
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
    // Advanced/subtle behaviors
    //------------------------------------------------------------------

    /** = defined(__APPLE__), OS X style: Text editing cursor movement using Alt instead of Ctrl, Shortcuts using
     *  Cmd/Super instead of Ctrl, Line/Text Start and End using Cmd + Arrows instead of Home/End, Double click selects
     *  by word instead of selecting whole text, Multi-selection in lists uses Cmd/Super instead of Ctrl */
    var optMacOSXBehaviors = false  // JVM TODO
    /** Enable blinking cursor, for users who consider it annoying. */
    var optCursorBlink = true

    //------------------------------------------------------------------
    // User Functions
    //------------------------------------------------------------------

    /** Rendering function, will be called in Render().
     *  Alternatively you can keep this to NULL and call GetDrawData() after Render() to get the same pointer.
     *  See example applications if you are unsure of how to implement this.    */
    var renderDrawListsFn: ((DrawData) -> Unit)? = null

    // Optional: access OS clipboard
    // (default to use native Win32 clipboard on Windows, otherwise uses a private clipboard. Override to access OS clipboard on other architectures)
    var getClipboardTextFn: ((userData: Any) -> Unit)? = null
    var setClipboardTextFn: ((userData: Any, text: String) -> Unit)? = null
    lateinit var clipboardUserData: Any
    //
//    // Optional: override memory allocations. MemFreeFn() may be called with a NULL pointer.
//    // (default to posix malloc/free)
//    void*       (*MemAllocFn)(size_t sz);
//    void        (*MemFreeFn)(void* ptr);
//
    // Optional: notify OS Input Method Editor of the screen position of your cursor for text input position (e.g. when using Japanese/Chinese IME in Windows)
    // (default to use native imm32 api on Windows)
//    var imeSetInputScreenPosFn: ((x: Int, y: Int) -> Unit)? = null
    /** (Windows) Set this to your HWND to get automatic IME cursor positioning.    */
    var imeWindowHandle = 0L
//
//    //------------------------------------------------------------------
//    // Input - Fill before calling NewFrame()
//    //------------------------------------------------------------------

    /** Mouse position, in pixels (set to -1,-1 if no mouse / on another screen, etc.)  */
    var mousePos = Vec2(-Float.MAX_VALUE)
    /** Mouse buttons: left, right, middle + extras. ImGui itself mostly only uses left button (BeginPopupContext** are
    using right button). Others buttons allows us to track if the mouse is being used by your application +
    available to user as a convenience via IsMouse** API.   */
    val mouseDown = BooleanArray(5)
    /** Mouse wheel: 1 unit scrolls about 5 lines text. */
    var mouseWheel = 0f
    /** Request ImGui to draw a mouse cursor for you (if you are on a platform without a mouse cursor). */
    var mouseDrawCursor = false
    /** Keyboard modifier pressed: Control  */
    var keyCtrl = false
    /** Keyboard modifier pressed: Shift    */
    var keyShift = false
    /** Keyboard modifier pressed: Alt  */
    var keyAlt = false
    /** Keyboard modifier pressed: Cmd/Super/Windows    */
    var keySuper = false
    /** Keyboard keys that are pressed (in whatever storage order you naturally have access to keyboard data)   */
    val keysDown = BooleanArray(512)
    /** List of characters input (translated by user from keypress + keyboard state). Fill using addInputCharacter()
     *  helper. */
    val inputCharacters = CharArray(16)

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

    /** When IO.wantCaptureMouse is true, do not dispatch mouse input data to your main application. This is set by ImGui
     *  when it wants to use your mouse (e.g. unclicked mouse is hovering a window, or a widget is active).     */
    var wantCaptureMouse = false
    /** When IO.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application. This is set
     *  by ImGui when it wants to use your keyboard inputs. */
    var wantCaptureKeyboard = false
    /** Mobile/console: when IO.wantTextInput is true, you may display an on-screen keyboard. This is set by ImGui when
     *  it wants textual keyboard input to happen (e.g. when a InputText widget is active). */
    var wantTextInput = false
    /** [BETA-NAV] MousePos has been altered, back-end should reposition mouse on next frame. Set only when
     * 'navMovesMouse = true'.    */
    var wantMoveMouse = false
    /** Application framerate estimation, in frame per second. Solely for convenience. Rolling average estimation based
    on IO.DeltaTime over 120 frames */
    var framerate = 0f
    /** Number of active memory allocations */
    val metricsAllocs get() = Debug.instanceCounts
    /** Vertices output during last call to Render()    */
    var metricsRenderVertices = 0
    /** Indices output during last call to Render() = number of triangles * 3   */
    var metricsRenderIndices = 0
    /** Number of visible root windows (exclude child windows)  */
    var metricsActiveWindows = 0
    /** Mouse delta. Note that this is zero if either current or previous position are invalid (-FLOAT_MAX_VALUE), so a
    disappearing/reappearing mouse won't have a huge delta.   */
    var mouseDelta = Vec2()


    //------------------------------------------------------------------
    // [Private] ImGui will maintain those fields. Forward compatibility not guaranteed!
    //------------------------------------------------------------------

    /** Previous mouse position temporary storage (nb: not for public use, set to MousePos in NewFrame())   */
    var mousePosPrev = Vec2(-Float.MAX_VALUE)
    /** Position at time of clicking    */
    val mouseClickedPos = Array(5, { Vec2() })
    /** Time of last click (used to figure out double-click)    */
    val mouseClickedTime = FloatArray(5)
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
    val mouseDownDuration = FloatArray(5, { -1f })
    /** Previous time the mouse button has been down    */
    val mouseDownDurationPrev = FloatArray(5, { -1f })
    /** Maximum distance, absolute, on each axis, of how much mouse has traveled from the clicking point    */
    val mouseDragMaxDistanceAbs = Array(5, { Vec2() })
    /** Squared maximum distance of how much mouse has traveled from the clicking point */
    val mouseDragMaxDistanceSqr = FloatArray(5)
    /** Duration the keyboard key has been down (0.0f == just pressed)  */
    val keysDownDuration = FloatArray(512, { -1f })
    /** Previous duration the key has been down */
    val keysDownDurationPrev = FloatArray(512, { -1f })

//    var imeSetInputScreenPosFn_DefaultImpl = { x: Int, y: Int -> TODO
//        // Notify OS Input Method Editor of text input position
//        val hwnd = imeWindowHandle
//        val a = WindowProc
//        if (hwnd != 0L)
//            if (HIMC himc = ImmGetContext (hwnd)) {
//                COMPOSITIONFORM cf;
//                cf.ptCurrentPos.x = x;
//                cf.ptCurrentPos.y = y;
//                cf.dwStyle = CFS_FORCE_POSITION;
//                ImmSetCompositionWindow(himc, & cf);
//            }
//    }
}

operator fun IntArray.set(index: Key, value: Int) {
    this[index.i] = value
}


class Style {

    /**  Global alpha applies to everything in ImGui    */
    var alpha = 1f
    /** Padding within a window */
    var windowPadding = Vec2(8)
    /** Radius of window corners rounding. Set to 0.0f to have rectangular windows  */
    var windowRounding = 7f
    /** Thickness of border around windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly)   */
    var windowBorderSize = 0f
    /** Minimum window size */
    var windowMinSize = Vec2i(32)
    /** Alignment for title bar text    */
    var windowTitleAlign = Vec2(0f, 0.5f)
    /** Radius of child window corners rounding. Set to 0.0f to have rectangular child windows.  */
    var childRounding = 0f
    /** Thickness of border around child windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly) */
    var childBorderSize = 1f
    /** Radius of popup window corners rounding.    */
    var popupRounding = 0f
    /** Thickness of border around popup windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly) */
    var popupBorderSize = 1f
    /** Padding within a framed rectangle (used by most widgets)    */
    var framePadding = Vec2(4, 3)
    /** Radius of frame corners rounding. Set to 0.0f to have rectangular frames (used by most widgets).    */
    var frameRounding = 0f
    /** Thickness of border around frames. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly)    */
    var frameBorderSize = 0f
    /** Horizontal and vertical spacing between widgets/lines   */
    var itemSpacing = Vec2(8, 4)
    /** Horizontal and vertical spacing between within elements of a composed widget (e.g. a slider and its label)  */
    var itemInnerSpacing = Vec2(4)
    /** Expand reactive bounding box for touch-based system where touch position is not accurate enough. Unfortunately
     *  we don't sort widgets so priority on overlap will always be given to the first widget. So don't grow this too
     *  much!   */
    var touchExtraPadding = Vec2()
    /** Horizontal spacing when e.g. entering a tree node. Generally == (FontSize + FramePadding.x*2).  */
    var indentSpacing = 21f
    /** Minimum horizontal spacing between two columns  */
    var columnsMinSpacing = 6f
    /** Width of the vertical scrollbar, Height of the horizontal scrollbar */
    var scrollbarSize = 16f
    /** Radius of grab corners rounding for scrollbar   */
    var scrollbarRounding = 9f
    /** Minimum width/height of a grab box for slider/scrollbar */
    var grabMinSize = 10f
    /** Radius of grabs corners rounding. Set to 0.0f to have rectangular slider grabs. */
    var grabRounding = 0f
    /** Alignment of button text when button is larger than text.   */
    var buttonTextAlign = Vec2(0.5f)
    /** Window positions are clamped to be visible within the display area by at least this amount. Only covers regular
     *  windows.    */
    var displayWindowPadding = Vec2(22)
    /** If you cannot see the edge of your screen (e.g. on a TV) increase the safe area padding. Covers popups/tooltips
     *  as well regular windows.    */
    var displaySafeAreaPadding = Vec2(4)
    /** Enable anti-aliasing on lines/borders. Disable if you are really short on CPU/GPU.  */
    var antiAliasedLines = true
    /**  Enable anti-aliasing on filled shapes (rounded rectangles, circles, etc.)  */
    var antiAliasedFill = true
    /** Tessellation tolerance when using pathBezierCurveTo() without a specific number of segments.
     *  Decrease for highly tessellated curves (higher quality, more polygons), increase to reduce quality. */
    var curveTessellationTol = 1.25f

    val colors = ArrayList<Vec4>()

    /** JVM IMGUI   */
    val locale = Locale.US

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
operator fun MutableMap<Int, Float>.set(key: Int, value: ColorEditFlags) = set(key, glm.intBitsToFloat(value.i))
//operator fun MutableMap<Int, Float>.getInt(key: Int) = 0 TODO float