package imgui.impl.glfw

import glm_.bool
import glm_.c
import glm_.f
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
import org.lwjgl.glfw.GLFWCharCallbackI
import org.lwjgl.glfw.GLFWCursorEnterCallbackI
import org.lwjgl.glfw.GLFWCursorPosCallbackI
import org.lwjgl.glfw.GLFWGamepadState
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFWMonitorCallbackI
import org.lwjgl.glfw.GLFWMouseButtonCallbackI
import org.lwjgl.glfw.GLFWScrollCallbackI
import org.lwjgl.glfw.GLFWWindowFocusCallbackI
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.glfw.*
import uno.glfw.GlfwWindow.CursorMode

enum class GlfwClientApi { Unknown, OpenGL, Vulkan }

// TODO chain previously installed callbacks
// TODO GLFW_HAS_NEW_CURSORS and similar

// GLFW callbacks install
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

    /** ~ImGui_ImplGlfw_Init */
    init {

        with(io) {
            assert(backendPlatformUserData == null) { "Already initialized a platform backend!" }
            //printf("GLFW_VERSION: %d.%d.%d (%d)", GLFW_VERSION_MAJOR, GLFW_VERSION_MINOR, GLFW_VERSION_REVISION, GLFW_VERSION_COMBINED);

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
            clipboardUserData = window.handle

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
//        #if (GLFW_VERSION_COMBINED >= 3300) // Eat errors (see #5785)
        glfw.error
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

        if (glfwGetInputMode(data.window.handle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE)
            return
        }

        val isAppFocused = data.window.focused
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

        if (io.configFlags has ConfigFlag.NoMouseCursorChange || window.cursorMode == CursorMode.Disabled)
            return

        val imguiCursor = mouseCursor
        if (imguiCursor == MouseCursor.None || io.mouseDrawCursor)
        // Hide OS mouse cursor if imgui is drawing it or if it wants no cursor
            window.cursorMode = CursorMode.Hidden
        else {
            // Show OS mouse cursor
            // FIXME-PLATFORM: Unfocused windows seems to fail changing the mouse cursor with GLFW 3.2, but 3.3 works here.
            window.cursor = GlfwCursor(data.mouseCursors[imguiCursor.i].takeIf { it != NULL }
                    ?: data.mouseCursors[MouseCursor.Arrow.i])
            window.cursorMode = CursorMode.Normal
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
        check(!data.installedCallbacks) { "Callbacks already installed!" }
        check(data.window == window)

        window.focusCB = windowFocusCB
        window.cursorEnterCB = cursorEnterCB
        window.cursorPosCB = cursorPosCB
        window.mouseButtonCB = mouseButtonCB
        window.scrollCB = scrollCB
        window.keyCB = keyCB
        window.charCB = charCB
        glfw.monitorCallback = monitorCB

        data.prevUserCBWindowFocus = glfwSetWindowFocusCallback(window.handle, null)
        data.prevUserCBCursorEnter = glfwSetCursorEnterCallback(window.handle, null)
        data.prevUserCBCursorPos = glfwSetCursorPosCallback(window.handle, null)
        data.prevUserCBMousebutton = glfwSetMouseButtonCallback(window.handle, null)
        data.prevUserCBScroll = glfwSetScrollCallback(window.handle, null)
        data.prevUserCBKey = glfwSetKeyCallback(window.handle, null)
        data.prevUserCBChar = glfwSetCharCallback(window.handle, null)
        data.prevUserCBMonitor = glfwSetMonitorCallback(null)
        data.installedCallbacks = true
    }

    fun restoreCallbacks(window: GlfwWindow) {
        check(data.installedCallbacks) { "Callbacks not installed!" }
        check(data.window == window)

        glfwSetWindowFocusCallback(window.handle, data.prevUserCBWindowFocus)
        glfwSetCursorEnterCallback(window.handle, data.prevUserCBCursorEnter)
        glfwSetCursorPosCallback(window.handle, data.prevUserCBCursorPos)
        glfwSetMouseButtonCallback(window.handle, data.prevUserCBMousebutton)
        glfwSetScrollCallback(window.handle, data.prevUserCBScroll)
        glfwSetKeyCallback(window.handle, data.prevUserCBKey)
        glfwSetCharCallback(window.handle, data.prevUserCBChar)
        glfwSetMonitorCallback(data.prevUserCBMonitor)
        data.installedCallbacks = false
        data.prevUserCBWindowFocus = null
        data.prevUserCBCursorEnter = null
        data.prevUserCBCursorPos = null
        data.prevUserCBMousebutton = null
        data.prevUserCBScroll = null
        data.prevUserCBKey = null
        data.prevUserCBChar = null
        data.prevUserCBMonitor = null
    }

    companion object {

        lateinit var instance: ImplGlfw

        fun init(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null) {
            instance = ImplGlfw(window, installCallbacks, vrTexSize)
        }

        fun newFrame() = instance.newFrame()
        fun shutdown() = instance.shutdown()

        val mouseButtonCB: MouseButtonCB = { wnd: GlfwWindow, button: Int, action: Int, mods: Int ->

            if (shouldChainCallback(wnd))
                data.prevUserCBMousebutton?.invoke(wnd.handle, button, action, mods)

            updateKeyModifiers(wnd)
            if (button >= 0 && button < MouseButton.COUNT)
                io.addMouseButtonEvent(MouseButton of button, action == GLFW_PRESS)
        }

        // X11 does not include current pressed/released modifier key in 'mods' flags submitted by GLFW
        // See https://github.com/ocornut/imgui/issues/6034 and https://github.com/glfw/glfw/issues/1630

        fun updateKeyModifiers(window: GlfwWindow) {
            val wnd = window.handle
            io.addKeyEvent(Key.Mod_Ctrl, (glfwGetKey(wnd, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) || (glfwGetKey(wnd, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS))
            io.addKeyEvent(Key.Mod_Shift, (glfwGetKey(wnd, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) || (glfwGetKey(wnd, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS))
            io.addKeyEvent(Key.Mod_Alt, (glfwGetKey(wnd, GLFW_KEY_LEFT_ALT) == GLFW_PRESS) || (glfwGetKey(wnd, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS))
            io.addKeyEvent(Key.Mod_Super, (glfwGetKey(wnd, GLFW_KEY_LEFT_SUPER) == GLFW_PRESS) || (glfwGetKey(wnd, GLFW_KEY_RIGHT_SUPER) == GLFW_PRESS))
        }

        // GFLW callbacks options:
        // - Set 'chain_for_all_windows=true' to enable chaining callbacks for all windows (including secondary viewports created by backends or by user)
        // Set to 'true' to enable chaining installed callbacks for all windows (including secondary viewports created by backends or by user.
        // This is 'false' by default meaning we only chain callbacks for the main viewport.
        // We cannot set this to 'true' by default because user callbacks code may be not testing the 'window' parameter of their callback.
        // If you set this to 'true' your user callback code will need to make sure you are testing the 'window' parameter.
        fun setCallbacksChainForAllWindows(chainForAllWindows: Boolean) {
            data.callbacksChainForAllWindows = chainForAllWindows
        }

        fun shouldChainCallback(window: GlfwWindow): Boolean = if (data.callbacksChainForAllWindows) true else window == data.window

        // GLFW callbacks (individual callbacks to call yourself if you didn't install callbacks)

        val scrollCB: ScrollCB = { wnd: GlfwWindow, offset: Vec2d ->
            if (shouldChainCallback(wnd))
                data.prevUserCBScroll?.invoke(wnd.handle, offset.x, offset.y)

            io.addMouseWheelEvent(offset.x.f, offset.y.f)
        }

        val keyCB: KeyCB = { wnd, keycode: uno.glfw.Key, scancode: Int, action: InputAction, mods: Int ->

            if (shouldChainCallback(wnd))
                data.prevUserCBKey?.invoke(wnd.handle, keycode.i, scancode, action.i, mods)

            if (action == InputAction.Press || action == InputAction.Release) {

                updateKeyModifiers(wnd)

                val imguiKey = keycode.imguiKey
                io.addKeyEvent(imguiKey, action == InputAction.Press)
                io.setKeyEventNativeData(imguiKey, keycode.i, scancode) // To support legacy indexing (<1.87 user code)
            }
        }

        val charCB: CharCB = { wnd, c: Int ->
            if (shouldChainCallback(wnd))
                data.prevUserCBChar?.invoke(wnd.handle, c)

            if (!imeInProgress) // [JVM]
                io.addInputCharacter(c.c)
        }

        val monitorCB: GlfwMonitorFun = { monitor: GlfwMonitor, connected: Boolean ->
            // Unused in 'master' branch but 'docking' branch will use this, so we declare it ahead of it so if you have to install callbacks you can install this one too.
        }

        // Since 1.84
        // Workaround: X11 seems to send spurious Leave/Enter events which would make us lose our position,
        // so we back it up and restore on Leave/Enter (see https://github.com/ocornut/imgui/issues/4984)
        val cursorEnterCB: CursorEnterCB = { wnd, entered ->
            if (shouldChainCallback(wnd))
                data.prevUserCBCursorEnter?.invoke(wnd.handle, entered)

            if (glfwGetInputMode(data.window.handle, GLFW_CURSOR) != GLFW_CURSOR_DISABLED)
                if (entered) {
                    data.mouseWindow = data.window
                    io.addMousePosEvent(data.lastValidMousePos.x, data.lastValidMousePos.y)
                } else if (!entered && data.mouseWindow == data.window) {
                    data.lastValidMousePos put io.mousePos
                    data.mouseWindow = null
                    io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE)
                }
        }

        // Since 1.84
        val windowFocusCB: WindowFocusCB = { wnd, focused ->
            if (shouldChainCallback(wnd))
                data.prevUserCBWindowFocus?.invoke(wnd.handle, focused)

            io.addFocusEvent(focused)
        }

        // Since 1.87
        val cursorPosCB: CursorPosCB = { wnd, pos ->
            if (shouldChainCallback(wnd))
                data.prevUserCBCursorPos?.invoke(wnd.handle, pos.x, pos.y)

            if (glfwGetInputMode(data.window.handle, GLFW_CURSOR) != GLFW_CURSOR_DISABLED) {

                io.addMousePosEvent(pos.x.f, pos.y.f)
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
            var callbacksChainForAllWindows = false

            // Chain GLFW callbacks: our callbacks will call the user's previously installed callbacks, if any.
            var prevUserCBWindowFocus: GLFWWindowFocusCallbackI? = null
            var prevUserCBCursorPos: GLFWCursorPosCallbackI? = null
            var prevUserCBCursorEnter: GLFWCursorEnterCallbackI? = null
            var prevUserCBMousebutton: GLFWMouseButtonCallbackI? = null
            var prevUserCBScroll: GLFWScrollCallbackI? = null
            var prevUserCBKey: GLFWKeyCallbackI? = null
            var prevUserCBChar: GLFWCharCallbackI? = null
            var prevUserCBMonitor: GLFWMonitorCallbackI? = null
        }

        val uno.glfw.Key.imguiKey: Key
            get() = when (this) {
                uno.glfw.Key.TAB -> Key.Tab
                uno.glfw.Key.LEFT -> Key.LeftArrow
                uno.glfw.Key.RIGHT -> Key.RightArrow
                uno.glfw.Key.UP -> Key.UpArrow
                uno.glfw.Key.DOWN -> Key.DownArrow
                uno.glfw.Key.PAGE_UP -> Key.PageUp
                uno.glfw.Key.PAGE_DOWN -> Key.PageDown
                uno.glfw.Key.HOME -> Key.Home
                uno.glfw.Key.END -> Key.End
                uno.glfw.Key.INSERT -> Key.Insert
                uno.glfw.Key.DELETE -> Key.Delete
                uno.glfw.Key.BACKSPACE -> Key.Backspace
                uno.glfw.Key.SPACE -> Key.Space
                uno.glfw.Key.ENTER -> Key.Enter
                uno.glfw.Key.ESCAPE -> Key.Escape
                uno.glfw.Key.APOSTROPHE -> Key.Apostrophe
                uno.glfw.Key.COMMA -> Key.Comma
                uno.glfw.Key.MINUS -> Key.Minus
                uno.glfw.Key.PERIOD -> Key.Period
                uno.glfw.Key.SLASH -> Key.Slash
                uno.glfw.Key.SEMICOLON -> Key.Semicolon
                uno.glfw.Key.EQUAL -> Key.Equal
                uno.glfw.Key.LEFT_BRACKET -> Key.LeftBracket
                uno.glfw.Key.BACKSLASH -> Key.Backslash
                uno.glfw.Key.RIGHT_BRACKET -> Key.RightBracket
                uno.glfw.Key.GRAVE_ACCENT -> Key.GraveAccent
                uno.glfw.Key.CAPS_LOCK -> Key.CapsLock
                uno.glfw.Key.SCROLL_LOCK -> Key.ScrollLock
                uno.glfw.Key.NUM_LOCK -> Key.NumLock
                uno.glfw.Key.PRINT_SCREEN -> Key.PrintScreen
                uno.glfw.Key.PAUSE -> Key.Pause
                uno.glfw.Key.KP_0 -> Key.Keypad0
                uno.glfw.Key.KP_1 -> Key.Keypad1
                uno.glfw.Key.KP_2 -> Key.Keypad2
                uno.glfw.Key.KP_3 -> Key.Keypad3
                uno.glfw.Key.KP_4 -> Key.Keypad4
                uno.glfw.Key.KP_5 -> Key.Keypad5
                uno.glfw.Key.KP_6 -> Key.Keypad6
                uno.glfw.Key.KP_7 -> Key.Keypad7
                uno.glfw.Key.KP_8 -> Key.Keypad8
                uno.glfw.Key.KP_9 -> Key.Keypad9
                uno.glfw.Key.KP_DECIMAL -> Key.KeypadDecimal
                uno.glfw.Key.KP_DIVIDE -> Key.KeypadDivide
                uno.glfw.Key.KP_MULTIPLY -> Key.KeypadMultiply
                uno.glfw.Key.KP_SUBTRACT -> Key.KeypadSubtract
                uno.glfw.Key.KP_ADD -> Key.KeypadAdd
                uno.glfw.Key.KP_ENTER -> Key.KeypadEnter
                uno.glfw.Key.KP_EQUAL -> Key.KeypadEqual
                uno.glfw.Key.LEFT_SHIFT -> Key.LeftShift
                uno.glfw.Key.LEFT_CONTROL -> Key.LeftCtrl
                uno.glfw.Key.LEFT_ALT -> Key.LeftAlt
                uno.glfw.Key.LEFT_SUPER -> Key.LeftSuper
                uno.glfw.Key.RIGHT_SHIFT -> Key.RightShift
                uno.glfw.Key.RIGHT_CONTROL -> Key.RightCtrl
                uno.glfw.Key.RIGHT_ALT -> Key.RightAlt
                uno.glfw.Key.RIGHT_SUPER -> Key.RightSuper
                uno.glfw.Key.MENU -> Key.Menu
                uno.glfw.Key.`0` -> Key.`0`
                uno.glfw.Key.`1` -> Key.`1`
                uno.glfw.Key.`2` -> Key.`2`
                uno.glfw.Key.`3` -> Key.`3`
                uno.glfw.Key.`4` -> Key.`4`
                uno.glfw.Key.`5` -> Key.`5`
                uno.glfw.Key.`6` -> Key.`6`
                uno.glfw.Key.`7` -> Key.`7`
                uno.glfw.Key.`8` -> Key.`8`
                uno.glfw.Key.`9` -> Key.`9`
                uno.glfw.Key.A -> Key.A
                uno.glfw.Key.B -> Key.B
                uno.glfw.Key.C -> Key.C
                uno.glfw.Key.D -> Key.D
                uno.glfw.Key.E -> Key.E
                uno.glfw.Key.F -> Key.F
                uno.glfw.Key.G -> Key.G
                uno.glfw.Key.H -> Key.H
                uno.glfw.Key.I -> Key.I
                uno.glfw.Key.J -> Key.J
                uno.glfw.Key.K -> Key.K
                uno.glfw.Key.L -> Key.L
                uno.glfw.Key.M -> Key.M
                uno.glfw.Key.N -> Key.N
                uno.glfw.Key.O -> Key.O
                uno.glfw.Key.P -> Key.P
                uno.glfw.Key.Q -> Key.Q
                uno.glfw.Key.R -> Key.R
                uno.glfw.Key.S -> Key.S
                uno.glfw.Key.T -> Key.T
                uno.glfw.Key.U -> Key.U
                uno.glfw.Key.V -> Key.V
                uno.glfw.Key.W -> Key.W
                uno.glfw.Key.X -> Key.X
                uno.glfw.Key.Y -> Key.Y
                uno.glfw.Key.Z -> Key.Z
                uno.glfw.Key.F1 -> Key.F1
                uno.glfw.Key.F2 -> Key.F2
                uno.glfw.Key.F3 -> Key.F3
                uno.glfw.Key.F4 -> Key.F4
                uno.glfw.Key.F5 -> Key.F5
                uno.glfw.Key.F6 -> Key.F6
                uno.glfw.Key.F7 -> Key.F7
                uno.glfw.Key.F8 -> Key.F8
                uno.glfw.Key.F9 -> Key.F9
                uno.glfw.Key.F10 -> Key.F10
                uno.glfw.Key.F11 -> Key.F11
                uno.glfw.Key.F12 -> Key.F12
                else -> Key.None
            }
    }
}