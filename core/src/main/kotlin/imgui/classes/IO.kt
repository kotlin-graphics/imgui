package imgui.classes

import glm_.b
import glm_.c
import glm_.i
import glm_.shl
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.*
import imgui.font.Font
import imgui.font.FontAtlas
import imgui.internal.textCharFromUtf8
import imgui.static.getClipboardTextFn_DefaultImpl
import imgui.static.imeSetInputScreenPosFn_Win32
import imgui.static.setClipboardTextFn_DefaultImpl
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform
import uno.glfw.HWND
import uno.kotlin.NUL

/** -----------------------------------------------------------------------------
 *  IO
 *  Communicate most settings and inputs/outputs to Dear ImGui using this structure.
 *  Access via ::io. Read 'Programmer guide' section in .cpp file for general usage.
 *  ----------------------------------------------------------------------------- */
class IO(sharedFontAtlas: FontAtlas? = null) {

    //------------------------------------------------------------------
    // Configuration (fill once)
    //------------------------------------------------------------------

    /** See ConfigFlags enum. Set by user/application. Gamepad/keyboard navigation options, etc. */
    var configFlags: ConfigFlags = ConfigFlag.None.i

    /** Set ImGuiBackendFlags_ enum. Set by imgui_impl_xxx files or custom backend to communicate features supported by the backend. */
    var backendFlags: BackendFlags = BackendFlag.None.i

    /** Main display size, in pixels.  This is for the default viewport.  */
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
    var keyRepeatDelay = 0.275f

    /** When holding a key/button, rate at which it repeats, in seconds.    */
    var keyRepeatRate = 0.05f

//    void*         UserData;                 // = NULL               // Store your own data for retrieval by callbacks.

    /** Font atlas: load, rasterize and pack one or more fonts into a single texture.    */
    val fonts = sharedFontAtlas ?: FontAtlas()

    /** Global scale all fonts  */
    var fontGlobalScale = 1f

    /** Allow user scaling text of individual window with CTRL+Wheel.   */
    var fontAllowUserScaling = false

    /** Font to use on NewFrame(). Use NULL to useMouseDragThreshold s Fonts->Fonts[0].    */
    var fontDefault: Font? = null

    /** For retina display or other situations where window coordinates are different from framebuffer coordinates.
     *  This generally ends up in ImDrawData::FramebufferScale. */
    var displayFramebufferScale = Vec2(1f)


    // Docking options (when ImGuiConfigFlags_DockingEnable is set)

    /** Simplified docking mode: disable window splitting, so docking is limited to merging multiple windows together into tab-bars. */
    var configDockingNoSplit = false

    /** Enable docking with holding Shift key (reduce visual noise, allows dropping in wider space) */
    var configDockingWithShift = false

    /** [BETA] [FIXME: This currently creates regression with auto-sizing and general overhead] Make every single floating window display within a docking node. */
    var configDockingAlwaysTabBar = false

    /** [BETA] Make window or viewport transparent when docking and only display docking boxes on the target viewport. Useful if rendering of multiple viewport cannot be synced. Best used with ConfigViewportsNoAutoMerge. */
    var configDockingTransparentPayload = false


    // Viewport options (when ImGuiConfigFlags_ViewportsEnable is set)

    /** Set to make all floating imgui windows always create their own viewport. Otherwise, they are merged into the main host viewports when overlapping it. May also set ImGuiViewportFlags_NoAutoMerge on individual viewport. */
    var configViewportsNoAutoMerge =     false
    /** Disable default OS task bar icon flag for secondary viewports. When a viewport doesn't want a task bar icon, ImGuiViewportFlags_NoTaskBarIcon will be set on it. */
    var configViewportsNoTaskBarIcon =   false
    /** [BETA] Disable default OS window decoration flag for secondary viewports. When a viewport doesn't want window decorations, ImGuiViewportFlags_NoDecoration will be set on it. Enabling decoration can create subsequent issues at OS levels (e.g. minimum window size). */
    var configViewportsNoDecoration =    true
    /** Disable default OS parenting to main viewport for secondary viewports. By default, viewports are marked with ParentViewportId = <main_viewport>, expecting the platform back-end to setup a parent/child relationship between the OS windows (some back-end may ignore this). Set to true if you want the default to be 0, then all viewports will be top-level OS windows. */
    var configViewportsNoDefaultParent = false

    //------------------------------------------------------------------
    // Miscellaneous options
    //------------------------------------------------------------------

    /** Request ImGui to draw a mouse cursor for you (if you are on a platform without a mouse cursor). */
    var mouseDrawCursor = false

    /** = defined(__APPLE__), OS X style: Text editing cursor movement using Alt instead of Ctrl, Shortcuts using
     *  Cmd/Super instead of Ctrl, Line/Text Start and End using Cmd + Arrows instead of Home/End, Double click selects
     *  by word instead of selecting whole text, Multi-selection in lists uses Cmd/Super instead of Ctrl
     *  (was called io.OptMacOSXBehaviors prior to 1.63) */
    var configMacOSXBehaviors = false

    /** Set to false to disable blinking cursor, for users who consider it distracting. (was called: io.OptCursorBlink prior to 1.63) */
    var configInputTextCursorBlink = true

    /** Enable resizing of windows from their edges and from the lower-left corner.
     *  This requires (io.backendFlags has BackendFlags.HasMouseCursors) because it needs mouse cursor feedback.
     *  (This used to be WindowFlag.ResizeFromAnySide flag) */
    var configWindowsResizeFromEdges = true

    /** [BETA] Set to true to only allow moving windows when clicked+dragged from the title bar. Windows without a title bar are not affected. */
    var configWindowsMoveFromTitleBarOnly = false

    /** [BETA] Compact window memory usage when unused. Set to -1.0f to disable. */
    var configWindowsMemoryCompactTimer = 60f

    //------------------------------------------------------------------
    // User Functions
    //------------------------------------------------------------------

    // Optional: Platform/Renderer backend name (informational only! will be displayed in About Window)
    // Optional: Platform/Renderer backend name (informational only! will be displayed in About Window) + User data for backend/wrappers to store their own stuff.
    var backendPlatformName: String? = null
    var backendRendererName: String? = null

    /** User data for platform backend */
    var backendPlatformUserData: Any? = null

    /** User data for renderer backend */
    var backendRendererUserData: Any? = null

    /** User data for non C++ programming language backend */
    var backendLanguageUserData: Any? = null

    // Optional: Access OS clipboard
    // (default to use native Win32 clipboard on Windows, otherwise uses a private clipboard. Override to access OS clipboard on other architectures)
    var getClipboardTextFn: ((userData: Any?) -> String?)? = getClipboardTextFn_DefaultImpl
    var setClipboardTextFn: ((userData: Any?, text: String) -> Unit)? = setClipboardTextFn_DefaultImpl
    var clipboardUserData: Any? = null

    //    // Optional: override memory allocations. MemFreeFn() may be called with a NULL pointer.
//    // (default to posix malloc/free)
//    void*       (*MemAllocFn)(size_t sz);
//    void        (*MemFreeFn)(void* ptr);
//
    // TODO remove
//    // Optional: Notify OS Input Method Editor of the screen position of your cursor for text input position (e.g. when using Japanese/Chinese IME in Windows)
//    // (default to use native imm32 api on Windows)
//    val imeSetInputScreenPosFn: ((x: Int, y: Int) -> Unit)? = imeSetInputScreenPosFn_Win32.takeIf { Platform.get() == Platform.WINDOWS }
//
//    /** (Windows) Set this to your HWND to get automatic IME cursor positioning.    */
//    var imeWindowHandle: HWND = HWND(MemoryUtil.NULL)

    //------------------------------------------------------------------
    // Input - Fill before calling NewFrame()
    //------------------------------------------------------------------

    /** Mouse position, in pixels. Set to ImVec2(-FLT_MAX, -FLT_MAX) if mouse is unavailable (on another screen, etc.) */
    var mousePos = Vec2(-Float.MAX_VALUE)

    /** Mouse buttons: 0=left, 1=right, 2=middle + extras (ImGuiMouseButton_COUNT == 5). Dear ImGui mostly uses left
     *  and right buttons. Others buttons allows us to track if the mouse is being used by your application + available
     *  to user as a convenience via IsMouse** API.   */
    val mouseDown = BooleanArray(5)

    /** Mouse wheel Vertical: 1 unit scrolls about 5 lines text. */
    var mouseWheel = 0f

    /** Mouse wheel Horizontal. Most users don't have a mouse with an horizontal wheel, may not be filled by all backends.   */
    var mouseWheelH = 0f

    /** (Optional) When using multiple viewports: viewport the OS mouse cursor is hovering _IGNORING_ viewports with the ImGuiViewportFlags_NoInputs flag, and _REGARDLESS_ of whether another viewport is focused. Set io.BackendFlags |= ImGuiBackendFlags_HasMouseHoveredViewport if you can provide this info. If you don't imgui will infer the value using the rectangles and last focused time of the viewports it knows about (ignoring other OS windows). */
    var mouseHoveredViewport: ID = 0

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

    /** Gamepad inputs. Cleared back to zero by EndFrame(). Keyboard keys will be auto-mapped and be written here by NewFrame().   */
    val navInputs = FloatArray(NavInput.COUNT)

    // Functions

    /** Queue new character input
     *
     *  Pass in translated ASCII characters for text input.
     * - with glfw you can get those from the callback set in glfwSetCharCallback()
     * - on Windows you can get those using ToAscii+keyboard state, or via the WM_CHAR message */
    fun addInputCharacter(c: Char) {
        if (c != NUL)
            inputQueueCharacters += when (c.i) {
                in 1..UNICODE_CODEPOINT_MAX -> c
                else -> UNICODE_CODEPOINT_INVALID.c
            }
    }

    /** UTF16 strings use surrogate pairs to encode codepoints >= 0x10000, so
     *  we should save the high surrogate. */
    fun addInputCharacterUTF16(c: Char) {

        if (c == NUL && inputQueueSurrogate == NUL)
            return

        val ci = c.i
        if ((ci and 0xFC00) == 0xD800) { // High surrogate, must save
            if (inputQueueSurrogate != NUL)
                inputQueueCharacters += UNICODE_CODEPOINT_INVALID.c
            inputQueueSurrogate = c
            return
        }

        var cp = c
        if (inputQueueSurrogate != NUL) {
            when {
                // Invalid low surrogate
                (ci and 0xFC00) != 0xDC00 -> inputQueueCharacters += UNICODE_CODEPOINT_INVALID.c
                // Codepoint will not fit in ImWchar (extra parenthesis around 0xFFFF somehow fixes -Wunreachable-code with Clang)
                UNICODE_CODEPOINT_MAX == 0xFFFF -> cp = UNICODE_CODEPOINT_INVALID.c
                else -> cp = (((inputQueueSurrogate - 0xD800) shl 10) + (c - 0xDC00).i + 0x10000).c
            }
            inputQueueSurrogate = NUL
        }
        inputQueueCharacters += cp
    }

    /** Queue new characters input from an UTF-8 string */
    fun addInputCharactersUTF8(utf8Chars: ByteArray) {
        var p = 0
        while (p < utf8Chars.size || utf8Chars[p] == 0.b) {
            val (c, bytes) = textCharFromUtf8(utf8Chars)
            p += bytes
            if (c != 0)
                inputQueueCharacters += c.c
        }
    }

    /** Clear the text input buffer manually */
    fun clearInputCharacters() = inputQueueCharacters.clear()


    //------------------------------------------------------------------
    // Output - Updated by NewFrame() or EndFrame()/Render()
    // (when reading from the io.WantCaptureMouse, io.WantCaptureKeyboard flags to dispatch your inputs, it is
    //  generally easier and more correct to use their state BEFORE calling NewFrame(). See FAQ for details!)
    //------------------------------------------------------------------

    /** Set when Dear ImGui will use mouse inputs, in this case do not dispatch them to your main game/application (either way, always pass on mouse inputs to imgui). (e.g. unclicked mouse is hovering over an imgui window, widget is active, mouse was clicked over an imgui window, etc.). */
    var wantCaptureMouse = false

    /** Set when Dear ImGui will use keyboard inputs, in this case do not dispatch them to your main game/application (either way, always pass keyboard inputs to imgui). (e.g. InputText active, or an imgui window is focused and navigation is enabled, etc.). */
    var wantCaptureKeyboard = false

    /** Mobile/console: when set, you may display an on-screen keyboard. This is set by Dear ImGui when it wants textual keyboard input to happen (e.g. when a InputText widget is active). */
    var wantTextInput = false

    /** MousePos has been altered, backend should reposition mouse on next frame. Rarely used! Set only when ImGuiConfigFlags_NavEnableSetMousePos flag is enabled. */
    var wantSetMousePos = false

    /** When manual .ini load/save is active (io.IniFilename == NULL), this will be set to notify your application that you can call SaveIniSettingsToMemory() and save yourself. Important: clear io.WantSaveIniSettings yourself after saving! */
    var wantSaveIniSettings = false

    /** Keyboard/Gamepad navigation is currently allowed (will handle ImGuiKey_NavXXX events) = a window is focused and it doesn't use the ImGuiWindowFlags_NoNavInputs flag.   */
    var navActive = false

    /** Keyboard/Gamepad navigation is visible and allowed (will handle ImGuiKey_NavXXX events). */
    var navVisible = false

    /** Application framerate estimate, in frame per second. Solely for convenience. Rolling average estimation based on io.DeltaTime over 120 frames. */
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

    /** Key mods flags (same as io.KeyCtrl/KeyShift/KeyAlt/KeySuper but merged into flags), updated by NewFrame() */
    var keyMods: KeyModFlags = KeyMod.None.i

    /** Previous mouse position (note that MouseDelta is not necessary == MousePos-MousePosPrev, in case either position is invalid)   */
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

    /** Track if button was clicked inside a dear imgui window. We don't request mouse capture from the application if click started outside ImGui bounds.   */
    var mouseDownOwned = BooleanArray(5)

    /** Track if button down was a double-click */
    var mouseDownWasDoubleClick = BooleanArray(5)

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

    /** Touch/Pen pressure (0.0f to 1.0f, should be >0.0f only when MouseDown[0] == true). Helper storage currently unused by Dear ImGui. */
    var penPressure = 0f

    /** For AddInputCharacterUTF16 */
    var inputQueueSurrogate = NUL

    /** Queue of _characters_ input (obtained by platform backend). Fill using AddInputCharacter() helper. */
    val inputQueueCharacters = ArrayList<Char>()
}