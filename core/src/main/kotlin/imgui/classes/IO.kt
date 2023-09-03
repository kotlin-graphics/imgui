package imgui.classes

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.data
import imgui.ImGui.getData
import imgui.ImGui.isAlias
import imgui.ImGui.isGamepad
import imgui.ImGui.isNamedOrMod
import imgui.api.gImGui
import imgui.font.Font
import imgui.font.FontAtlas
import imgui.internal.floorSigned
import imgui.internal.sections.InputEvent
import imgui.internal.sections.InputSource
import imgui.internal.textCharFromUtf8
import imgui.statics.findLatestInputEvent
import uno.kotlin.NUL

//-----------------------------------------------------------------------------
// [SECTION] ImGuiIO
//-----------------------------------------------------------------------------
// Communicate most settings and inputs/outputs to Dear ImGui using this structure.
// Access via ::io. Read 'Programmer guide' section in .cpp file for general usage.
//-----------------------------------------------------------------------------


// [Internal] Storage used by IsKeyDown(), IsKeyPressed() etc functions.
// If prior to 1.87 you used io.KeysDownDuration[] (which was marked as internal), you should use GetKeyData(key)->DownDuration and *NOT* io.KeysData[key]->DownDuration.
class KeyData {
    /** True for if key is down */
    var down = false

    /** Duration the key has been down (<0.0f: not pressed, 0.0f: just pressed, >0.0f: time held) */
    var downDuration = 0f

    /** Last frame duration the key has been down */
    var downDurationPrev = 0f

    /** 0.0f..1.0f for gamepad values */
    var analogValue = 0f
}
class IO(sharedFontAtlas: FontAtlas? = null) {

    //------------------------------------------------------------------
    // Configuration
    //------------------------------------------------------------------

    /** See ConfigFlags enum. Set by user/application. Gamepad/keyboard navigation options, etc. */
    var configFlags: ConfigFlags = none

    /** Set ImGuiBackendFlags_ enum. Set by imgui_impl_xxx files or custom backend to communicate features supported by the backend. */
    var backendFlags: BackendFlags = none

    /** Main display size, in pixels (generally == GetMainViewport()->Size). May change every frame.   */
    var displaySize = Vec2i(-1)

    /** Time elapsed since last frame, in seconds. May change every frame.  */
    var deltaTime = 1f / 60f

    /** Minimum time between saving positions/sizes to .ini file, in seconds.   */
    var iniSavingRate = 5f

    /** Path to .ini file (important: default "imgui.ini" is relative to current working dir!). Set NULL to disable automatic .ini loading/saving or if you want to manually call LoadIniSettingsXXX() / SaveIniSettingsXXX() functions. */
    var iniFilename: String? = "imgui.ini"

    /** Path to .log file (default parameter to ImGui::LogToFile when no file is specified).    */
    var logFilename = "imgui_log.txt"

    /** Store your own data. */
    var userData: Any? = null

    /** Font atlas: load, rasterize and pack one or more fonts into a single texture.    */
    var fonts = sharedFontAtlas ?: FontAtlas()

    /** Global scale all fonts  */
    var fontGlobalScale = 1f

    /** Allow user scaling text of individual window with CTRL+Wheel.   */
    var fontAllowUserScaling = false

    /** Font to use on NewFrame(). Use NULL to useMouseDragThreshold s Fonts->Fonts[0].    */
    var fontDefault: Font? = null

    /** For retina display or other situations where window coordinates are different from framebuffer coordinates.
     *  This generally ends up in ImDrawData::FramebufferScale. */
    var displayFramebufferScale = Vec2(1f)

    //------------------------------------------------------------------
    // Miscellaneous options
    //------------------------------------------------------------------

    /** Request ImGui to draw a mouse cursor for you (if you are on a platform without a mouse cursor). */
    var mouseDrawCursor = false

    /** = defined(__APPLE__), OS X style: Text editing cursor movement using Alt instead of Ctrl, Shortcuts using
     *  Cmd/Super instead of Ctrl, Line/Text Start and End using Cmd + Arrows instead of Home/End, Double click selects
     *  by word instead of selecting whole text, Multi-selection in lists uses Cmd/Super instead of Ctrl */
    var configMacOSXBehaviors = false

    /** Enable input queue trickling: some types of events submitted during the same frame (e.g. button down + up) will be spread over multiple frames, improving interactions with low framerates. */
    var configInputTrickleEventQueue = true

    /** Enable blinking cursor (optional as some users consider it to be distracting).. */
    var configInputTextCursorBlink = true

    /** [BETA] Pressing Enter will keep item active and select contents (single-line only). */
    var configInputTextEnterKeepActive = false

    /** [BETA] Enable turning DragXXX widgets into text input with a simple mouse click-release (without moving). Not desirable on devices without a keyboard. */
    var configDragClickToInputText = false

    /** Enable resizing of windows from their edges and from the lower-left corner.
     *  This requires (io.backendFlags has BackendFlags.HasMouseCursors) because it needs mouse cursor feedback.
     *  (This used to be WindowFlag.ResizeFromAnySide flag) */
    var configWindowsResizeFromEdges = true

    /** Enable allowing to move windows only when clicking on their title bar. Does not apply to windows without a title bar. */
    var configWindowsMoveFromTitleBarOnly = false

    /** Timer (in seconds) to free transient windows/tables memory buffers when unused. Set to -1.0f to disable. */
    var configMemoryCompactTimer = 60f


    //------------------------------------------------------------------
    // Inputs Behaviors
    // (other variables, ones which are expected to be tweaked within UI code, are exposed in ImGuiStyle)
    //------------------------------------------------------------------

    /** Time for a double-click, in seconds.    */
    var mouseDoubleClickTime = 0.3f

    /** Distance threshold to stay in to validate a double-click, in pixels.    */
    var mouseDoubleClickMaxDist = 6f

    /** Distance threshold before considering we are dragging.   */
    var mouseDragThreshold = 6f

    /** When holding a key/button, time before it starts repeating, in seconds (for buttons in Repeat mode, etc.).  */
    var keyRepeatDelay = 0.275f

    /** When holding a key/button, rate at which it repeats, in seconds.    */
    var keyRepeatRate = 0.05f


    //------------------------------------------------------------------
    // Debug options
    //------------------------------------------------------------------

    // Tools to test correct Begin/End and BeginChild/EndChild behaviors.
    // Presently Begin()/End() and BeginChild()/EndChild() needs to ALWAYS be called in tandem, regardless of return value of BeginXXX()
    // This is inconsistent with other BeginXXX functions and create confusion for many users.
    // We expect to update the API eventually. In the meanwhile we provide tools to facilitate checking user-code behavior.

    /** First-time calls to Begin()/BeginChild() will return false. NEEDS TO BE SET AT APPLICATION BOOT TIME if you don't want to miss windows. */
    var configDebugBeginReturnValueOnce = false

    /** Some calls to Begin()/BeginChild() will return false. Will cycle through window depths then repeat. Suggested use: add "io.ConfigDebugBeginReturnValue = io.KeyShift" in your main loop then occasionally press SHIFT. Windows should be flickering while running. */
    var configDebugBeginReturnValueLoop = false


    // Option to deactivate io.AddFocusEvent(false) handling. May facilitate interactions with a debugger when focus loss leads to clearing inputs data.
    // Backends may have other side-effects on focus loss, so this will reduce side-effects but not necessary remove all of them.
    // Consider using e.g. Win32's IsDebuggerPresent() as an additional filter (or see ImOsIsDebuggerPresent() in imgui_test_engine/imgui_te_utils.cpp for a Unix compatible version).

    /** Ignore io.AddFocusEvent(false), consequently not calling io.ClearInputKeys() in input processing. */
    var configDebugIgnoreFocusLoss = false

    // Options to audit .ini data

    // - tools to audit ini data
    var configDebugIniSettings = false         // = false          // Save .ini data with extra comments (particularly helpful for Docking, but makes saving slower)

    //------------------------------------------------------------------
    // Platform Functions
    // (the imgui_impl_xxxx backend files are setting those up for you)
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

    // Platform Functions
    // Note: Initialize() will setup default clipboard/ime handlers.
    // Optional: Access OS clipboard
    // (default to use native Win32 clipboard on Windows, otherwise uses a private clipboard. Override to access OS clipboard on other architectures)
    var getClipboardTextFn: ((userDataCtx: Any?) -> String?)? = null
    var setClipboardTextFn: ((userDataCtx: Any?, text: String) -> Unit)? = null
    var clipboardUserData: Any? = null

    // Optional: Notify OS Input Method Editor of the screen position of your cursor for text input position (e.g. when using Japanese/Chinese IME in Windows)
    // (default to use native imm32 api on Windows)
    var setPlatformImeDataFn: ((viewport: Viewport, data: PlatformImeData) -> Unit)? = null

    // [JVM] copy function for backup
    fun copy() = IO().also {
        it.configFlags = configFlags; it.backendFlags = backendFlags; it.displaySize = displaySize; it.deltaTime = deltaTime; it.iniSavingRate = iniSavingRate
        it.iniFilename = iniFilename; it.logFilename = logFilename; it.mouseDoubleClickTime = mouseDoubleClickTime; it.mouseDoubleClickMaxDist = mouseDoubleClickMaxDist
        it.mouseDragThreshold = mouseDragThreshold; it.keyRepeatDelay = keyRepeatDelay; it.keyRepeatRate = keyRepeatRate
        it.fonts = fonts; it.fontGlobalScale = fontGlobalScale; it.fontAllowUserScaling = fontAllowUserScaling; it.fontDefault = fontDefault
        it.displayFramebufferScale = displayFramebufferScale; it.mouseDrawCursor = mouseDrawCursor; it.configMacOSXBehaviors = configMacOSXBehaviors
        it.configInputTrickleEventQueue = configInputTrickleEventQueue; it.configInputTextCursorBlink = configInputTextCursorBlink
        it.configInputTextEnterKeepActive = configInputTextEnterKeepActive; it.configDragClickToInputText = configDragClickToInputText
        it.configWindowsResizeFromEdges = configWindowsResizeFromEdges; it.configWindowsMoveFromTitleBarOnly = configWindowsMoveFromTitleBarOnly
        it.configMemoryCompactTimer = configMemoryCompactTimer; it.backendPlatformName = backendPlatformName; it.backendPlatformUserData = backendPlatformUserData
        it.backendRendererUserData = backendRendererUserData; it.backendLanguageUserData = backendLanguageUserData; it.getClipboardTextFn = getClipboardTextFn
        it.setClipboardTextFn = setClipboardTextFn; it.clipboardUserData = clipboardUserData /*it.setPlatformImeDataFn = setPlatformImeDataFn*/
        it.ctx = ctx /*it.mousePos put mousePos; repeat(5) { i -> it.mouseDown[i] = mouseDown[i] }; it.mouseWheel = mouseWheelH
        it.mouseWheelH = mouseWheelH; it.keyCtrl = keyCtrl; it.keyShift = keyShift; it.keyAlt = keyAlt; it.keySuper = keySuper; it.mouseSource
        repeat(NavInput.COUNT) { i -> it.navInputs[i] = navInputs[i] }; it.keyMods = keyMods; repeat(Key.COUNT) {i -> it.keysData[i]}*/
    }

    //------------------------------------------------------------------
    // Input - Call before calling NewFrame()
    //------------------------------------------------------------------


    // Input Functions


    /** Queue a new key down/up event.
     *  - ImGuiKey key: Translated key (as in, generally ImGuiKey_A matches the key end-user would use to emit an 'A' character)
     *  - bool down:    Is the key down? use false to signify a key release. */
    fun addKeyEvent(key: Key, down: Boolean) {
        if (!appAcceptingEvents)
            return
        addKeyAnalogEvent(key, down, down.f)
    }

    /** Queue a new key down/up event for analog values (e.g. ImGuiKey_Gamepad_ values). Dead-zones should be handled by the backend. */
    // Queue a new key down/up event.
    // - ImGuiKey key:       Translated key (as in, generally ImGuiKey_A matches the key end-user would use to emit an 'A' character)
    // - bool down:          Is the key down? use false to signify a key release.
    // - float analog_value: 0.0f..1.0f
    // IMPORTANT: THIS FUNCTION AND OTHER "ADD" GRABS THE CONTEXT FROM OUR INSTANCE.
    // WE NEED TO ENSURE THAT ALL FUNCTION CALLS ARE FULLFILLING THIS, WHICH IS WHY GetKeyData() HAS AN EXPLICIT CONTEXT.
    fun addKeyAnalogEvent(key: Key, down: Boolean, analogValue: Float) {
        //if (e->Down) { IMGUI_DEBUG_LOG_IO("AddKeyEvent() Key='%s' %d, NativeKeycode = %d, NativeScancode = %d\n", ImGui::GetKeyName(e->Key), e->Down, e->NativeKeycode, e->NativeScancode); }
        val g = ctx!!
        if (key == Key.None || !appAcceptingEvents)
            return

        assert(key.isNamedOrMod) { "Backend needs to pass a valid ImGuiKey_ constant . 0..511 values are legacy native key codes which are not accepted by this API." }
        assert(!key.isAlias) { "Backend cannot submit ImGuiKey_MouseXXX values they are automatically inferred from AddMouseXXX() events ." }
        assert(key != Key.Mod_Shortcut) { "We could easily support the translation here but it seems saner to not accept it(TestEngine perform a translation itself)" }

        // Verify that backend isn't mixing up using new io.AddKeyEvent() api and old io.KeysDown[] + io.KeyMap[] data.
        if (key.isGamepad)
            backendUsingLegacyNavInputArray = false

        // Filter duplicate (in particular: key mods and gamepad analog values are commonly spammed)
        val latestEvent = findLatestInputEvent(g, key)
        val keyData = key.getData(g)
        val latestKeyDown = latestEvent?.down ?: keyData.down
        val latestKeyAnalog = latestEvent?.analogValue ?: keyData.analogValue
        if (latestKeyDown == down && latestKeyAnalog == analogValue)
            return

        // Add event
        g.inputEventsQueue += InputEvent.Key(key, down, analogValue, if (key.isGamepad) InputSource.Gamepad else InputSource.Keyboard, g.inputEventsNextEventId++)
    }

    /** Queue a mouse position update. Use -FLT_MAX,-FLT_MAX to signify no mouse (e.g. app not focused and not hovered) */
    fun addMousePosEvent(x: Float, y: Float) {
        val g = ctx!!
        if (!appAcceptingEvents)
            return

        // Apply same flooring as UpdateMouseInputs()
        val pos = Vec2(if (x > -Float.MAX_VALUE) floorSigned(x) else x, if (y > -Float.MAX_VALUE) floorSigned(y) else y)

        // Filter duplicate
        val latestEvent = findLatestInputEvent<InputEvent.MousePos>(g)
        val latestPos = latestEvent?.let { Vec2(it.posX, it.posY) } ?: g.io.mousePos
        if (latestPos.x == pos.x && latestPos.y == pos.y)
            return

        g.inputEventsQueue += InputEvent.MousePos(pos.x, pos.y, g.inputEventsNextMouseSource, g.inputEventsNextEventId++)
    }

    /** Queue a mouse button change */
    fun addMouseButtonEvent(mouseButton: MouseButton, down: Boolean) {
        val g = ctx!!
        if (!appAcceptingEvents)
            return

        // Filter duplicate
        val latestEvent = findLatestInputEvent(g, mouseButton)
        val latestButtonDown = latestEvent?.down ?: g.io.mouseDown[mouseButton.i]
        if (latestButtonDown == down)
            return

        g.inputEventsQueue += InputEvent.MouseButton(mouseButton, down, g.inputEventsNextMouseSource, g.inputEventsNextEventId++)
    }

    /** Queue a mouse wheel update. wheel_y<0: scroll down, wheel_y>0: scroll up, wheel_x<0: scroll right, wheel_x>0: scroll left.
     *
     *  Queue a mouse wheel event (some mouse/API may only have a Y component) */
    fun addMouseWheelEvent(wheelX: Float, wheelY: Float) {
        val g = ctx!!

        // Filter duplicate (unlike most events, wheel values are relative and easy to filter)
        if (!appAcceptingEvents || (wheelX == 0f && wheelY == 0f))
            return

        g.inputEventsQueue += InputEvent.MouseWheel(wheelX, wheelY, g.inputEventsNextMouseSource, g.inputEventsNextEventId++)
    }

    // Queue a mouse source change (Mouse/TouchScreen/Pen)
    // This is not a real event, the data is latched in order to be stored in actual Mouse events.
    // This is so that duplicate events (e.g. Windows sending extraneous WM_MOUSEMOVE) gets filtered and are not leading to actual source changes.
    fun addMouseSourceEvent(source: MouseSource) {
//        IM_ASSERT(Ctx != NULL);
        val g = ctx!!
        g.inputEventsNextMouseSource = source
    }

    /** Queue a gain/loss of focus for the application (generally based on OS/platform focus of your window) */
    fun addFocusEvent(focused: Boolean) {
        val g = ctx!!

        // Filter duplicate
        val latestEvent = findLatestInputEvent<InputEvent.AppFocused>(g)
        val latestFocused = latestEvent?.focused ?: !g.io.appFocusLost
        if (latestFocused == focused || (configDebugIgnoreFocusLoss && !focused))
            return

        g.inputEventsQueue += InputEvent.AppFocused(focused, g.inputEventsNextEventId++)
    }

    /** Queue a new character input
     *
     *  Pass in translated ASCII characters for text input.
     * - with glfw you can get those from the callback set in glfwSetCharCallback()
     * - on Windows you can get those using ToAscii+keyboard state, or via the WM_CHAR message */
    fun addInputCharacter(c: Char) {
        val g = ctx!!
        if (c == NUL || !appAcceptingEvents)
            return

        g.inputEventsQueue += InputEvent.Text(c, g.inputEventsNextEventId++)
    }

    /** Queue a new character input from a UTF-16 character, it can be a surrogate
     *
     *  UTF16 strings use surrogate pairs to encode codepoints >= 0x10000, so we should save the high surrogate. */
    fun addInputCharacterUTF16(c: Char) {

        if ((c == NUL && inputQueueSurrogate == NUL) || !appAcceptingEvents)
            return

        val ci = c.i
        if ((ci and 0xFC00) == 0xD800) { // High surrogate, must save
            if (inputQueueSurrogate != NUL)
                addInputCharacter(UNICODE_CODEPOINT_INVALID.c)
            inputQueueSurrogate = c
            return
        }

        var cp = c
        if (inputQueueSurrogate != NUL) {
            when {
                // Invalid low surrogate
                (ci and 0xFC00) != 0xDC00 -> addInputCharacter(UNICODE_CODEPOINT_INVALID.c)
                // Codepoint will not fit in ImWchar (extra parenthesis around 0xFFFF somehow fixes -Wunreachable-code with Clang)
                UNICODE_CODEPOINT_MAX == 0xFFFF -> cp = UNICODE_CODEPOINT_INVALID.c // Codepoint will not fit in ImWchar
                else -> cp = (((inputQueueSurrogate - 0xD800) shl 10) + (c - 0xDC00).i + 0x10000).c
            }
            inputQueueSurrogate = NUL
        }
        addInputCharacter(cp)
    }

    /** Queue a new characters input from a UTF-8 string */
    fun addInputCharactersUTF8(utf8Chars: ByteArray) {
        if (!appAcceptingEvents)
            return
        var p = 0
        while (p < utf8Chars.size && utf8Chars[p] != 0.b) {
            val (c, bytes) = textCharFromUtf8(utf8Chars)
            p += bytes
            addInputCharacter(c.c)
        }
    }

    /** [Optional] Specify index for legacy <1.87 IsKeyXXX() functions with native indices + specify native keycode, scancode. */
    fun setKeyEventNativeData(key: Key, native_keycode: Int, native_scancode: Int, native_legacy_index: Int = -1) {

    }

    /** Set master flag for accepting key/mouse/text events (default to true). Useful if you have native dialog boxes that are interrupting your application loop/refresh, and you want to disable events being queued while your app is frozen. */
    fun setAppAcceptingEvents_(acceptingEvents: Boolean) {
        appAcceptingEvents = acceptingEvents
    }

    // Clear all incoming events.
    fun clearEventsQueue() {
//        assert(ctx != null)
        val g = ctx!!
        g.inputEventsQueue.clear()
    }

    /** [Internal] Release all keys
     *
     *  Clear current keyboard/mouse/gamepad state + current frame text input buffer. Equivalent to releasing all keys/buttons. */
    fun clearInputKeys() {
        for (keyData in keysData) {
            keyData.down = false
            keyData.downDuration = -1f
            keyData.downDurationPrev = -1f
        }
        keyCtrl = false; keyShift = false; keyAlt = false; keySuper = false
        keyMods = Key.Mod_None
        mousePos put -Float.MAX_VALUE
        for (n in mouseDown.indices) {
            mouseDown[n] = false
            mouseDownDuration[n] = -1f; mouseDownDurationPrev[n] = -1f
        }
        mouseWheel = 0f; mouseWheelH = 0f
        inputQueueCharacters.clear() // Behavior of old ClearInputCharacters().
    }


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

    /** Estimate of application framerate (rolling average over 60 frames, based on io.DeltaTime), in frame per second. Solely for convenience. Slow applications may not want to use a moving average or may want to reset underlying buffers occasionally. */
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
    // [Internal] Dear ImGui will maintain those fields. Forward compatibility not guaranteed!
    //------------------------------------------------------------------

    /** Parent UI context (needs to be set explicitly by parent). */
    var ctx: Context? = null

    // Main Input State
    // (this block used to be written by backend, since 1.87 it is best to NOT write to those directly, call the AddXXX functions above instead)
    // (reading from those variables is fair game, as they are extremely unlikely to be moving anywhere)

    /** Mouse position, in pixels. Set to ImVec2(-FLT_MAX, -FLT_MAX) if mouse is unavailable (on another screen, etc.) */
    val mousePos = Vec2(-Float.MAX_VALUE)

    /** Mouse buttons: 0=left, 1=right, 2=middle + extras (ImGuiMouseButton_COUNT == 5). Dear ImGui mostly uses left
     *  and right buttons. Other buttons allow us to track if the mouse is being used by your application + available
     *  to user as a convenience via IsMouse** API.   */
    val mouseDown = BooleanArray(5)

    /** Mouse wheel Vertical: 1 unit scrolls about 5 lines text. >0 scrolls Up, <0 scrolls Down. Hold SHIFT to turn vertical scroll into horizontal scroll. */
    var mouseWheel = 0f

    /** Mouse wheel Horizontal. >0 scrolls Left, <0 scrolls Right. Most users don't have a mouse with a horizontal wheel, may not be filled by all backends.  */
    var mouseWheelH = 0f

    /** Mouse actual input peripheral (Mouse/TouchScreen/Pen). */
    var mouseSource: MouseSource = MouseSource.Mouse

    /** Keyboard modifier down: Control  */
    var keyCtrl = false

    /** Keyboard modifier down: Shift    */
    var keyShift = false

    /** Keyboard modifier down: Alt  */
    var keyAlt = false

    /** Keyboard modifier down: Cmd/Super/Windows    */
    var keySuper = false

    /** Gamepad inputs. Cleared back to zero by EndFrame(). Keyboard keys will be auto-mapped and be written here by NewFrame().   */
    val navInputs = FloatArray(NavInput.COUNT)


    // Other state maintained from data above + IO function calls

    /** Key mods flags (any of ImGuiMod_Ctrl/ImGuiMod_Shift/ImGuiMod_Alt/ImGuiMod_Super flags, same as io.KeyCtrl/KeyShift/KeyAlt/KeySuper but merged into flags. DOES NOT CONTAINS ImGuiMod_Shortcut which is pretranslated). Read-only, updated by NewFrame() */
    var keyMods: KeyChord = none

    /** Key state for all known keys. Use IsKeyXXX() functions to access this. */
    val keysData = Array(Key.COUNT) { KeyData().apply { downDuration = -1f; downDurationPrev = -1f } }

    /** Alternative to WantCaptureMouse: (WantCaptureMouse == true && WantCaptureMouseUnlessPopupClose == false) when a click over void is expected to close a popup. */
    var wantCaptureMouseUnlessPopupClose = false

    /** Previous mouse position (note that MouseDelta is not necessary == MousePos-MousePosPrev, in case either position is invalid)   */
    var mousePosPrev = Vec2(-Float.MAX_VALUE)

    /** Position at time of clicking    */
    val mouseClickedPos = Array(5) { Vec2() }

    /** Time of last click (used to figure out double-click)    */
    val mouseClickedTime = DoubleArray(5)

    /** Mouse button went from !Down to Down (same as MouseClickedCount[x] != 0)    */
    val mouseClicked = BooleanArray(5)

    /** Has mouse button been double-clicked? (same as MouseClickedCount[x] == 2) */
    val mouseDoubleClicked = BooleanArray(5)

    /** == 0 (not clicked), == 1 (same as MouseClicked[]), == 2 (double-clicked), == 3 (triple-clicked) etc. when going from !Down to Down */
    val mouseClickedCount = IntArray(5)

    /** Count successive number of clicks. Stays valid after mouse release. Reset after another click is done. */
    val mouseClickedLastCount = IntArray(5)

    /** Mouse button went from Down to !Down    */
    val mouseReleased = BooleanArray(5)

    /** Track if button was clicked inside a dear imgui window or over void blocked by a popup. We don't request mouse capture from the application if click started outside ImGui bounds. */
    var mouseDownOwned = BooleanArray(5)

    /** Track if button was clicked inside a dear imgui window. */
    val mouseDownOwnedUnlessPopupClose = BooleanArray(5)

    /** On a non-Mac system, holding SHIFT requests WheelY to perform the equivalent of a WheelX event. On a Mac system this is already enforced by the system. */
    var mouseWheelRequestAxisSwap = false

    /** Duration the mouse button has been down (0.0f == just clicked)  */
    val mouseDownDuration = FloatArray(5) { -1f }

    /** Previous time the mouse button has been down    */
    val mouseDownDurationPrev = FloatArray(5) { -1f }

    /** Squared maximum distance of how much mouse has traveled from the clicking point (used for moving thresholds) */
    val mouseDragMaxDistanceSqr = FloatArray(5)

    val navInputsDownDuration = FloatArray(NavInput.COUNT) { -1f }

    val navInputsDownDurationPrev = FloatArray(NavInput.COUNT)

    /** Touch/Pen pressure (0.0f to 1.0f, should be >0.0f only when MouseDown[0] == true). Helper storage currently unused by Dear ImGui. */
    var penPressure = 0f

    /** Only modify via AddFocusEvent() */
    var appFocusLost = false

    /** Only modify via SetAppAcceptingEvents() */
    var appAcceptingEvents = true

    /** -1: unknown, 0: using AddKeyEvent(), 1: using legacy io.KeysDown[] */
    var backendUsingLegacyKeyArrays = -1

    /** 0: using AddKeyAnalogEvent(), 1: writing to legacy io.NavInputs[] directly */
    var backendUsingLegacyNavInputArray = true // assume using legacy array until proven wrong

    /** For AddInputCharacterUTF16() */
    var inputQueueSurrogate = NUL

    /** Queue of _characters_ input (obtained by platform backend). Fill using AddInputCharacter() helper. */
    val inputQueueCharacters = ArrayList<Char>()
}

