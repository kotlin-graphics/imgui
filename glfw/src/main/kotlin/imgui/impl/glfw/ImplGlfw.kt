package imgui.impl.glfw

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.mainViewport
import imgui.ImGui.mouseCursor
import imgui.Key
import imgui.MouseButton
import imgui.windowsIme.imeListener
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.glfw.*
import uno.glfw.GlfwWindow.CursorMode

enum class GlfwClientApi { Unknown, OpenGL, Vulkan }

// TODO chain previously installed callbacks

// GLFW callbacks (installer)
// - When calling Init with 'install_callbacks=true': ImGui_ImplGlfw_InstallCallbacks() is called. GLFW callbacks will be installed for you. They will chain-call user's previously installed callbacks, if any.
// - When calling Init with 'install_callbacks=false': GLFW callbacks won't be installed. You will need to call individual function yourself from your own GLFW callbacks.
class ImplGlfw @JvmOverloads constructor(
    /** Main window */
    val window: GlfwWindow, installCallbacks: Boolean = true,
    /** for vr environment */
    val vrTexSize: Vec2i? = null,
    clientApi: GlfwClientApi = GlfwClientApi.OpenGL) {

    /** for passing inputs in vr */
    var vrCursorPos: Vec2? = null

    init {

        with(io) {
            assert(io.backendPlatformUserData == NULL) { "Already initialized a platform backend!" }

            // Setup backend capabilities flags
            backendPlatformUserData = data
            backendPlatformName = "imgui_impl_glfw"
            backendFlags /= BackendFlag.HasMouseCursors   // We can honor GetMouseCursor() values (optional)
            backendFlags /= BackendFlag.HasSetMousePos    // We can honor io.WantSetMousePos requests (optional, rarely used)

            data.window = window
            //            Data.time = 0.0

            backendRendererName = null
            backendPlatformName = null
            backendLanguageUserData = null
            backendRendererUserData = null
            backendPlatformUserData = null
            setClipboardTextFn = { _, text -> glfwSetClipboardString(clipboardUserData as Long, text) }
            getClipboardTextFn = { glfwGetClipboardString(clipboardUserData as Long) }
            clipboardUserData = window.handle.value

            // Set platform dependent data in viewport
            if (Platform.get() == Platform.WINDOWS)
                mainViewport.platformHandleRaw = window.hwnd
        }

        // Create mouse cursors
        // (By design, on X11 cursors are user configurable and some cursors may be missing. When a cursor doesn't exist,
        // GLFW will emit an error which will often be printed by the app, so we temporarily disable error reporting.
        // Missing cursors will return NULL and our _UpdateMouseCursor() function will use the Arrow cursor instead.)
        val prevErrorCallback = glfwSetErrorCallback(null)
        data.mouseCursors[MouseCursor.Arrow.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        data.mouseCursors[MouseCursor.TextInput.i] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
        data.mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)  // FIXME: GLFW doesn't have this. [JVM] TODO
        data.mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(GLFW_RESIZE_ALL_CURSOR)
        data.mouseCursors[MouseCursor.ResizeNS.i] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR)
        data.mouseCursors[MouseCursor.ResizeEW.i] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR)
        data.mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR) // FIXME: GLFW doesn't have this.
        data.mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR) // FIXME: GLFW doesn't have this.
        data.mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(GLFW_RESIZE_NESW_CURSOR)
        data.mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(GLFW_RESIZE_NWSE_CURSOR)
        data.mouseCursors[MouseCursor.Hand.i] = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
        data.mouseCursors[MouseCursor.NotAllowed.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        data.mouseCursors[MouseCursor.NotAllowed.i] = glfwCreateStandardCursor(GLFW_NOT_ALLOWED_CURSOR)
        glfwSetErrorCallback(prevErrorCallback)

        // [JVM] Chain GLFW callbacks: our callbacks will be installed in parallel with any other already existing
        if (installCallbacks) {
            // TODO monitor callback
            imeListener.install(window)
            installCallbacks(window)
        }

        data.clientApi = clientApi
    }

    fun shutdown() {

        if (data.installedCallbacks)
            restoreCallbacks(window)

        data.mouseCursors.forEach(::glfwDestroyCursor)

        io.backendPlatformName = null
        io.backendPlatformUserData = null
    }

    private fun updateMouseData() {

        //        // Update mouse position
        //        val mousePosBackup = Vec2d(io.mousePos)
        //        io.mousePos put -Float.MAX_VALUE
        //        if (window.isFocused)
        //            if (io.wantSetMousePos)
        //                window.cursorPos = mousePosBackup
        //            else
        //                io.mousePos put (vrCursorPos ?: window.cursorPos)
        //        else
        //            vrCursorPos?.let(io.mousePos::put) // window is usually unfocused in vr

        if (glfwGetInputMode(data.window.handle.value, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE)
            return
        }

        val isAppFocused = data.window.isFocused
        if (isAppFocused) {
            // (Optional) Set OS mouse position from Dear ImGui if requested (rarely used, only when ImGuiConfigFlags_NavEnableSetMousePos is enabled by user)
            if (io.wantSetMousePos)
                data.window.cursorPos = Vec2d(io.mousePos)

            // (Optional) Fallback to provide mouse position when focused (ImGui_ImplGlfw_CursorPosCallback already provides this when hovered or captured)
            if (isAppFocused && data.mouseWindow == null) {
                val (mouseX, mouseY) = data.window.cursorPos
                io.addMousePosEvent(mouseX.f, mouseY.f)
                data.lastValidMousePos.put(mouseX, mouseY)
            }
        }
    }

    private fun updateMouseCursor() {

        if (io.configFlags has ConfigFlag.NoMouseCursorChange || window.cursorMode == CursorMode.disabled)
            return

        val imguiCursor = mouseCursor
        if (imguiCursor == MouseCursor.None || io.mouseDrawCursor)
        // Hide OS mouse cursor if imgui is drawing it or if it wants no cursor
            window.cursorMode = CursorMode.hidden
        else {
            // Show OS mouse cursor
            // FIXME-PLATFORM: Unfocused windows seems to fail changing the mouse cursor with GLFW 3.2, but 3.3 works here.
            window.cursor = GlfwCursor(data.mouseCursors[imguiCursor.i].takeIf { it != NULL }
                                           ?: data.mouseCursors[MouseCursor.Arrow.i])
            window.cursorMode = CursorMode.normal
        }
    }

    // Update gamepad inputs
    val Float.saturate
        get() = if (this < 0f) 0f else if (this > 1f) 1f else this

    fun updateGamepads() {

        if (io.configFlags hasnt ConfigFlag.NavEnableGamepad) // FIXME: Technically feeding gamepad shouldn't depend on this now that they are regular inputs.
            return

        io.backendFlags -= BackendFlag.HasGamepad
        //        #if GLFW_HAS_GAMEPAD_API
        GLFWGamepadState.calloc().use { gamepad ->
            if (!glfwGetGamepadState(GLFW_JOYSTICK_1, gamepad))
                return
            fun MAP_BUTTON(key: Key, button: Int, unused: Int) = io.addKeyEvent(key, gamepad.buttons(button).bool)
            fun MAP_ANALOG(key: Key, axis: Int, unused: Int, v0: Float, v1: Float) {
                var v = gamepad.axes(axis)
                v = (v - v0) / (v1 - v0)
                io.addKeyAnalogEvent(key, v > 0.1f, v.saturate)
            }

            io.backendFlags /= BackendFlag.HasGamepad
            MAP_BUTTON(Key.GamepadStart, GLFW_GAMEPAD_BUTTON_START, 7)
            MAP_BUTTON(Key.GamepadBack, GLFW_GAMEPAD_BUTTON_BACK, 6)
            MAP_BUTTON(Key.GamepadFaceLeft, GLFW_GAMEPAD_BUTTON_X, 2)     // Xbox X, PS Square
            MAP_BUTTON(Key.GamepadFaceRight, GLFW_GAMEPAD_BUTTON_B, 1)     // Xbox B, PS Circle
            MAP_BUTTON(Key.GamepadFaceUp, GLFW_GAMEPAD_BUTTON_Y, 3)     // Xbox Y, PS Triangle
            MAP_BUTTON(Key.GamepadFaceDown, GLFW_GAMEPAD_BUTTON_A, 0)     // Xbox A, PS Cross
            MAP_BUTTON(Key.GamepadDpadLeft, GLFW_GAMEPAD_BUTTON_DPAD_LEFT, 13)
            MAP_BUTTON(Key.GamepadDpadRight, GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, 11)
            MAP_BUTTON(Key.GamepadDpadUp, GLFW_GAMEPAD_BUTTON_DPAD_UP, 10)
            MAP_BUTTON(Key.GamepadDpadDown, GLFW_GAMEPAD_BUTTON_DPAD_DOWN, 12)
            MAP_BUTTON(Key.GamepadL1, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, 4)
            MAP_BUTTON(Key.GamepadR1, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER, 5)
            MAP_ANALOG(Key.GamepadL2, GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, 4, -0.75f, +1.0f)
            MAP_ANALOG(Key.GamepadR2, GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, 5, -0.75f, +1.0f)
            MAP_BUTTON(Key.GamepadL3, GLFW_GAMEPAD_BUTTON_LEFT_THUMB, 8)
            MAP_BUTTON(Key.GamepadR3, GLFW_GAMEPAD_BUTTON_RIGHT_THUMB, 9)
            MAP_ANALOG(Key.GamepadLStickLeft, GLFW_GAMEPAD_AXIS_LEFT_X, 0, -0.25f, -1.0f)
            MAP_ANALOG(Key.GamepadLStickRight, GLFW_GAMEPAD_AXIS_LEFT_X, 0, +0.25f, +1.0f)
            MAP_ANALOG(Key.GamepadLStickUp, GLFW_GAMEPAD_AXIS_LEFT_Y, 1, -0.25f, -1.0f)
            MAP_ANALOG(Key.GamepadLStickDown, GLFW_GAMEPAD_AXIS_LEFT_Y, 1, +0.25f, +1.0f)
            MAP_ANALOG(Key.GamepadRStickLeft, GLFW_GAMEPAD_AXIS_RIGHT_X, 2, -0.25f, -1.0f)
            MAP_ANALOG(Key.GamepadRStickRight, GLFW_GAMEPAD_AXIS_RIGHT_X, 2, +0.25f, +1.0f)
            MAP_ANALOG(Key.GamepadRStickUp, GLFW_GAMEPAD_AXIS_RIGHT_Y, 3, -0.25f, -1.0f)
            MAP_ANALOG(Key.GamepadRStickDown, GLFW_GAMEPAD_AXIS_RIGHT_Y, 3, +0.25f, +1.0f)
        }
    }

    fun newFrame() {

        assert(data != null) { "Did you call ImGui_ImplGlfw_InitForXXX()?" }

        // Setup display size (every frame to accommodate for window resizing)
        val size = window.size
        val displaySize = window.framebufferSize
        io.displaySize put (vrTexSize ?: window.size)
        if (size allGreaterThan 0)
            io.displayFramebufferScale put (displaySize / size)

        // Setup time step
        val currentTime = glfw.time
        io.deltaTime = if (data.time > 0) (currentTime - data.time).f else 1f / 60f
        data.time = currentTime

        updateMouseData()
        updateMouseCursor()

        // Update game controllers (if enabled and available)
        updateGamepads()
    }

    fun installCallbacks(window: GlfwWindow) {
        //        ImGui_ImplGlfw_Data* bd = ImGui_ImplGlfw_GetBackendData();
        //        IM_ASSERT(bd->InstalledCallbacks == false && "Callbacks already installed!");
        //        IM_ASSERT(bd->Window == window);
        //
        //        bd->PrevUserCallbackWindowFocus = glfwSetWindowFocusCallback(window, ImGui_ImplGlfw_WindowFocusCallback);
        //        bd->PrevUserCallbackCursorEnter = glfwSetCursorEnterCallback(window, ImGui_ImplGlfw_CursorEnterCallback);
        //        bd->PrevUserCallbackCursorPos = glfwSetCursorPosCallback(window, ImGui_ImplGlfw_CursorPosCallback);
        //        bd->PrevUserCallbackMousebutton = glfwSetMouseButtonCallback(window, ImGui_ImplGlfw_MouseButtonCallback);
        //        bd->PrevUserCallbackScroll = glfwSetScrollCallback(window, ImGui_ImplGlfw_ScrollCallback);
        //        bd->PrevUserCallbackKey = glfwSetKeyCallback(window, ImGui_ImplGlfw_KeyCallback);
        //        bd->PrevUserCallbackChar = glfwSetCharCallback(window, ImGui_ImplGlfw_CharCallback);
        //        bd->PrevUserCallbackMonitor = glfwSetMonitorCallback(ImGui_ImplGlfw_MonitorCallback);
        data.installedCallbacks = true
    }

    fun restoreCallbacks(window: GlfwWindow) {
        //        ImGui_ImplGlfw_Data* bd = ImGui_ImplGlfw_GetBackendData();
        //        IM_ASSERT(bd->InstalledCallbacks == true && "Callbacks not installed!");
        //        IM_ASSERT(bd->Window == window);
        //
        //        glfwSetWindowFocusCallback(window, bd->PrevUserCallbackWindowFocus);
        //        glfwSetCursorEnterCallback(window, bd->PrevUserCallbackCursorEnter);
        //        glfwSetCursorPosCallback(window, bd->PrevUserCallbackCursorPos);
        //        glfwSetMouseButtonCallback(window, bd->PrevUserCallbackMousebutton);
        //        glfwSetScrollCallback(window, bd->PrevUserCallbackScroll);
        //        glfwSetKeyCallback(window, bd->PrevUserCallbackKey);
        //        glfwSetCharCallback(window, bd->PrevUserCallbackChar);
        //        glfwSetMonitorCallback(bd->PrevUserCallbackMonitor);
        //        bd->InstalledCallbacks = false;
        //        bd->PrevUserCallbackWindowFocus = NULL;
        //        bd->PrevUserCallbackCursorEnter = NULL;
        //        bd->PrevUserCallbackCursorPos = NULL;
        //        bd->PrevUserCallbackMousebutton = NULL;
        //        bd->PrevUserCallbackScroll = NULL;
        //        bd->PrevUserCallbackKey = NULL;
        //        bd->PrevUserCallbackChar = NULL;
        //        bd->PrevUserCallbackMonitor = NULL;
    }

    companion object {

        lateinit var instance: ImplGlfw

        fun init(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null) {
            instance = ImplGlfw(window, installCallbacks, vrTexSize)
        }

        fun newFrame() = instance.newFrame()
        fun shutdown() = instance.shutdown()

        val mouseButtonCallback: MouseButtonCB = { button: Int, action: Int, mods: Int ->

            //            ImGui_ImplGlfw_Data* bd = ImGui_ImplGlfw_GetBackendData();
            //            if (bd->PrevUserCallbackMousebutton != NULL && window == bd->Window)
            //            bd->PrevUserCallbackMousebutton(window, button, action, mods);

            updateKeyModifiers(mods)
            if (button >= 0 && button < MouseButton.COUNT)
                io.addMouseButtonEvent(button, action == GLFW_PRESS)
        }

        fun keyToModifier(key: Int): Int = when (key) {
            GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> GLFW_MOD_CONTROL
            GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> GLFW_MOD_SHIFT
            GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> GLFW_MOD_ALT
            GLFW_KEY_LEFT_SUPER, GLFW_KEY_RIGHT_SUPER -> GLFW_MOD_SUPER
            else -> 0
        }

        fun updateKeyModifiers(mods: Int) {
            io.addKeyEvent(Key.Mod_Ctrl, mods has GLFW_MOD_CONTROL)
            io.addKeyEvent(Key.Mod_Shift, mods has GLFW_MOD_SHIFT)
            io.addKeyEvent(Key.Mod_Alt, mods has GLFW_MOD_ALT)
            io.addKeyEvent(Key.Mod_Super, mods has GLFW_MOD_SUPER)
        }

        val scrollCallback: ScrollCB = { offset: Vec2d ->
            io.addMouseWheelEvent(offset.x.f, offset.y.f)
        }

        val keyCallback: KeyCB = { keycode: Int, scancode: Int, action: Int, mods_: Int ->

            var mods = mods_

            //            if (bd->PrevUserCallbackKey != NULL && window == bd->Window)
            //            bd->PrevUserCallbackKey(window, keycode, scancode, action, mods);

            if (action == GLFW_PRESS || action == GLFW_RELEASE) {

                // Workaround: X11 does not include current pressed/released modifier key in 'mods' flags. https://github.com/glfw/glfw/issues/1630
                run {
                    val keycodeToMod = keyToModifier(keycode)
                    if (keycodeToMod != 0)
                        mods = if (action == GLFW_PRESS) mods or keycodeToMod else mods wo keycodeToMod
                }

                updateKeyModifiers(mods)

                val imguiKey = keycode.imguiKey
                io.addKeyEvent(imguiKey, action == GLFW_PRESS)
                io.setKeyEventNativeData(imguiKey, keycode, scancode) // To support legacy indexing (<1.87 user code)
            }
        }

        val charCallback: CharCB = { c: Int -> if (!imeInProgress) io.addInputCharacter(c.c) }

        // Workaround: X11 seems to send spurious Leave/Enter events which would make us lose our position,
        // so we back it up and restore on Leave/Enter (see https://github.com/ocornut/imgui/issues/4984)
        val cursorEnterCallback: CursorEnterCB = { entered ->
            //            if (bd->PrevUserCallbackCursorEnter != NULL && window == bd->Window)
            //            bd->PrevUserCallbackCursorEnter(window, entered);

            if (glfwGetInputMode(data.window.handle.value, GLFW_CURSOR) != GLFW_CURSOR_DISABLED)
                if (entered) {
                    data.mouseWindow = data.window
                    io.addMousePosEvent(data.lastValidMousePos.x, data.lastValidMousePos.y)
                } else if (!entered && data.mouseWindow === data.window) {
                    data.lastValidMousePos put io.mousePos
                    data.mouseWindow = null
                    io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE)
                }
        }

        val windowFocusCallback: WindowFocusCB = { focused ->
            //            if (bd->PrevUserCallbackWindowFocus != NULL && window == bd->Window)
            //            bd->PrevUserCallbackWindowFocus(window, focused);

            io.addFocusEvent(focused)
        }

        val cursorPosCallback: CursorPosCB = { pos ->
            //            ImGui_ImplGlfw_Data* bd = ImGui_ImplGlfw_GetBackendData();
            //            if (bd->PrevUserCallbackCursorPos != NULL && window == bd->Window)
            //            bd->PrevUserCallbackCursorPos(window, x, y);

            if (glfwGetInputMode(data.window.handle.value, GLFW_CURSOR) != GLFW_CURSOR_DISABLED) {

                io.addMousePosEvent(pos.x, pos.y)
                data.lastValidMousePos put pos
            }
        }

        fun initForOpengl(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
            ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.OpenGL)

        fun initForVulkan(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
            ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.Vulkan)

        fun initForOther(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
            ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.Unknown)

        object data {
            lateinit var window: GlfwWindow
            lateinit var clientApi: GlfwClientApi
            var time = 0.0
            var mouseWindow: GlfwWindow? = null
            val mouseCursors = LongArray/*<GlfwCursor>*/(MouseCursor.COUNT)
            val lastValidMousePos = Vec2()
            var installedCallbacks = false

            // Chain GLFW callbacks: our callbacks will call the user's previously installed callbacks, if any.
            //            GLFWmousebuttonfun      PrevUserCallbackMousebutton;
            //            GLFWscrollfun           PrevUserCallbackScroll;
            //            GLFWkeyfun              PrevUserCallbackKey;
            //            GLFWcharfun             PrevUserCallbackChar;
        }

        val Int.imguiKey: Key
            get() = when (this) {
                GLFW_KEY_TAB -> Key.Tab
                GLFW_KEY_LEFT -> Key.LeftArrow
                GLFW_KEY_RIGHT -> Key.RightArrow
                GLFW_KEY_UP -> Key.UpArrow
                GLFW_KEY_DOWN -> Key.DownArrow
                GLFW_KEY_PAGE_UP -> Key.PageUp
                GLFW_KEY_PAGE_DOWN -> Key.PageDown
                GLFW_KEY_HOME -> Key.Home
                GLFW_KEY_END -> Key.End
                GLFW_KEY_INSERT -> Key.Insert
                GLFW_KEY_DELETE -> Key.Delete
                GLFW_KEY_BACKSPACE -> Key.Backspace
                GLFW_KEY_SPACE -> Key.Space
                GLFW_KEY_ENTER -> Key.Enter
                GLFW_KEY_ESCAPE -> Key.Escape
                GLFW_KEY_APOSTROPHE -> Key.Apostrophe
                GLFW_KEY_COMMA -> Key.Comma
                GLFW_KEY_MINUS -> Key.Minus
                GLFW_KEY_PERIOD -> Key.Period
                GLFW_KEY_SLASH -> Key.Slash
                GLFW_KEY_SEMICOLON -> Key.Semicolon
                GLFW_KEY_EQUAL -> Key.Equal
                GLFW_KEY_LEFT_BRACKET -> Key.LeftBracket
                GLFW_KEY_BACKSLASH -> Key.Backslash
                GLFW_KEY_RIGHT_BRACKET -> Key.RightBracket
                GLFW_KEY_GRAVE_ACCENT -> Key.GraveAccent
                GLFW_KEY_CAPS_LOCK -> Key.CapsLock
                GLFW_KEY_SCROLL_LOCK -> Key.ScrollLock
                GLFW_KEY_NUM_LOCK -> Key.NumLock
                GLFW_KEY_PRINT_SCREEN -> Key.PrintScreen
                GLFW_KEY_PAUSE -> Key.Pause
                GLFW_KEY_KP_0 -> Key.Keypad0
                GLFW_KEY_KP_1 -> Key.Keypad1
                GLFW_KEY_KP_2 -> Key.Keypad2
                GLFW_KEY_KP_3 -> Key.Keypad3
                GLFW_KEY_KP_4 -> Key.Keypad4
                GLFW_KEY_KP_5 -> Key.Keypad5
                GLFW_KEY_KP_6 -> Key.Keypad6
                GLFW_KEY_KP_7 -> Key.Keypad7
                GLFW_KEY_KP_8 -> Key.Keypad8
                GLFW_KEY_KP_9 -> Key.Keypad9
                GLFW_KEY_KP_DECIMAL -> Key.KeypadDecimal
                GLFW_KEY_KP_DIVIDE -> Key.KeypadDivide
                GLFW_KEY_KP_MULTIPLY -> Key.KeypadMultiply
                GLFW_KEY_KP_SUBTRACT -> Key.KeypadSubtract
                GLFW_KEY_KP_ADD -> Key.KeypadAdd
                GLFW_KEY_KP_ENTER -> Key.KeypadEnter
                GLFW_KEY_KP_EQUAL -> Key.KeypadEqual
                GLFW_KEY_LEFT_SHIFT -> Key.LeftShift
                GLFW_KEY_LEFT_CONTROL -> Key.LeftCtrl
                GLFW_KEY_LEFT_ALT -> Key.LeftAlt
                GLFW_KEY_LEFT_SUPER -> Key.LeftSuper
                GLFW_KEY_RIGHT_SHIFT -> Key.RightShift
                GLFW_KEY_RIGHT_CONTROL -> Key.RightCtrl
                GLFW_KEY_RIGHT_ALT -> Key.RightAlt
                GLFW_KEY_RIGHT_SUPER -> Key.RightSuper
                GLFW_KEY_MENU -> Key.Menu
                GLFW_KEY_0 -> Key.`0`
                GLFW_KEY_1 -> Key.`1`
                GLFW_KEY_2 -> Key.`2`
                GLFW_KEY_3 -> Key.`3`
                GLFW_KEY_4 -> Key.`4`
                GLFW_KEY_5 -> Key.`5`
                GLFW_KEY_6 -> Key.`6`
                GLFW_KEY_7 -> Key.`7`
                GLFW_KEY_8 -> Key.`8`
                GLFW_KEY_9 -> Key.`9`
                GLFW_KEY_A -> Key.A
                GLFW_KEY_B -> Key.B
                GLFW_KEY_C -> Key.C
                GLFW_KEY_D -> Key.D
                GLFW_KEY_E -> Key.E
                GLFW_KEY_F -> Key.F
                GLFW_KEY_G -> Key.G
                GLFW_KEY_H -> Key.H
                GLFW_KEY_I -> Key.I
                GLFW_KEY_J -> Key.J
                GLFW_KEY_K -> Key.K
                GLFW_KEY_L -> Key.L
                GLFW_KEY_M -> Key.M
                GLFW_KEY_N -> Key.N
                GLFW_KEY_O -> Key.O
                GLFW_KEY_P -> Key.P
                GLFW_KEY_Q -> Key.Q
                GLFW_KEY_R -> Key.R
                GLFW_KEY_S -> Key.S
                GLFW_KEY_T -> Key.T
                GLFW_KEY_U -> Key.U
                GLFW_KEY_V -> Key.V
                GLFW_KEY_W -> Key.W
                GLFW_KEY_X -> Key.X
                GLFW_KEY_Y -> Key.Y
                GLFW_KEY_Z -> Key.Z
                GLFW_KEY_F1 -> Key.F1
                GLFW_KEY_F2 -> Key.F2
                GLFW_KEY_F3 -> Key.F3
                GLFW_KEY_F4 -> Key.F4
                GLFW_KEY_F5 -> Key.F5
                GLFW_KEY_F6 -> Key.F6
                GLFW_KEY_F7 -> Key.F7
                GLFW_KEY_F8 -> Key.F8
                GLFW_KEY_F9 -> Key.F9
                GLFW_KEY_F10 -> Key.F10
                GLFW_KEY_F11 -> Key.F11
                GLFW_KEY_F12 -> Key.F12
                else -> Key.None
            }
    }
}