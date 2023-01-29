package imgui.impl.glfw

import glm_.b
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
import kool.lim
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.glfw.*
import uno.glfw.GlfwWindow.CursorMode
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.collections.set

enum class GlfwClientApi { Unknown, OpenGL, Vulkan }

// TODO chain previously installed callbacks

// GLFW callbacks
// - When calling Init with 'install_callbacks=true': GLFW callbacks will be installed for you. They will call user's previously installed callbacks, if any.
// - When calling Init with 'install_callbacks=false': GLFW callbacks won't be installed. You will need to call those function yourself from your own GLFW callbacks.
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
            // native callbacks will be added at the GlfwWindow creation via default parameter
            window.mouseButtonCBs["imgui"] = mouseButtonCallback
            window.scrollCBs["imgui"] = scrollCallback
            window.keyCBs["imgui"] = keyCallback
            window.charCBs["imgui"] = charCallback
            window.cursorEnterCBs["imgui"] = cursorEnterCallback
            window.windowFocusCBs["imgui"] = cursorEnterCallback
            // TODO monitor callback
            imeListener.install(window)
            data.installedCallbacks = installCallbacks
        }

        data.clientApi = clientApi
    }

    fun shutdown() {

        if (data.installedCallbacks) {
            window.mouseButtonCBs -= "imgui"
            window.scrollCBs -= "imgui"
            window.keyCBs -= "imgui"
            window.charCBs -= "imgui"
        }

        data.mouseCursors.forEach(::glfwDestroyCursor)

        io.backendPlatformName = null
        io.backendPlatformUserData = null
    }

    private fun updateMousePosAndButtons() {

        val mousePosPrev = io.mousePos
        io.mousePos put -Float.MAX_VALUE

        // Update mouse buttons
        // (if a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release events that are shorter than 1 frame)
        repeat(io.mouseDown.size) {
            io.mouseDown[it] = data.mouseJustPressed[it] || glfwGetMouseButton(window.handle.value, it) != 0
            data.mouseJustPressed[it] = false
        }

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

        val focused = data.window.isFocused
        val mouseWindow = if (data.mouseWindow == data.window || focused) data.window else null

        // Set OS mouse position from Dear ImGui if requested (rarely used, only when ImGuiConfigFlags_NavEnableSetMousePos is enabled by user)
        if (io.wantSetMousePos && focused)
            data.window.cursorPos = Vec2d(mousePosPrev)

        // Set Dear ImGui mouse position from OS position
        if (mouseWindow != null)
            io.mousePos put mouseWindow.cursorPos
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

    fun updateGamepads() {

        io.navInputs.fill(0f)
        if (io.configFlags has ConfigFlag.NavEnableGamepad) {
            // Update gamepad inputs
            val buttons = Joystick._1.buttons ?: ByteBuffer.allocate(0)
            val buttonsCount = buttons.lim
            val axes = Joystick._1.axes ?: FloatBuffer.allocate(0)
            val axesCount = axes.lim

            fun mapButton(nav: NavInput, button: Int) {
                if (buttonsCount > button && buttons[button] == GLFW_PRESS.b)
                    io.navInputs[nav] = 1f
            }

            fun mapAnalog(nav: NavInput, axis: Int, v0: Float, v1: Float) {
                var v = if (axesCount > axis) axes[axis] else v0
                v = (v - v0) / (v1 - v0)
                if (v > 1f) v = 1f
                if (io.navInputs[nav] < v)
                    io.navInputs[nav] = v
            }

            mapButton(NavInput.Activate, 0)     // Cross / A
            mapButton(NavInput.Cancel, 1)     // Circle / B
            mapButton(NavInput.Menu, 2)     // Square / X
            mapButton(NavInput.Input, 3)     // Triangle / Y
            mapButton(NavInput.DpadLeft, 13)    // D-Pad Left
            mapButton(NavInput.DpadRight, 11)    // D-Pad Right
            mapButton(NavInput.DpadUp, 10)    // D-Pad Up
            mapButton(NavInput.DpadDown, 12)    // D-Pad Down
            mapButton(NavInput.FocusPrev, 4)     // L1 / LB
            mapButton(NavInput.FocusNext, 5)     // R1 / RB
            mapButton(NavInput.TweakSlow, 4)     // L1 / LB
            mapButton(NavInput.TweakFast, 5)     // R1 / RB
            mapAnalog(NavInput.LStickLeft, 0, -0.3f, -0.9f)
            mapAnalog(NavInput.LStickRight, 0, +0.3f, +0.9f)
            mapAnalog(NavInput.LStickUp, 1, +0.3f, +0.9f)
            mapAnalog(NavInput.LStickDown, 1, -0.3f, -0.9f)

            io.backendFlags = when {
                axesCount > 0 && buttonsCount > 0 -> io.backendFlags or BackendFlag.HasGamepad
                else -> io.backendFlags wo BackendFlag.HasGamepad
            }
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

        // Update key modifiers
        updateKeyModifiers()

        updateMousePosAndButtons()
        updateMouseCursor()

        // Update game controllers (if enabled and available)
        updateGamepads()
    }

    companion object {

        lateinit var instance: ImplGlfw

        fun init(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null) {
            instance = ImplGlfw(window, installCallbacks, vrTexSize)
        }

        fun newFrame() = instance.newFrame()
        fun shutdown() = instance.shutdown()

        val mouseButtonCallback: MouseButtonCB = { button: Int, action: Int, _: Int ->
            if (action == GLFW_PRESS && button in data.mouseJustPressed.indices)
                data.mouseJustPressed[button] = true
        }

        val scrollCallback: ScrollCB = { offset: Vec2d ->
            io.mouseWheelH += offset.x.f
            io.mouseWheel += offset.y.f
        }

        val keyCallback: KeyCB = { keycode: Int, scancode: Int, action: Int, _: Int ->

//            if (bd->PrevUserCallbackKey != NULL && window == bd->Window)
//            bd->PrevUserCallbackKey(window, keycode, scancode, action, mods);

            if (action == GLFW_PRESS || action == GLFW_RELEASE) {

                val imguiKey = keycode.imguiKey
                io.addKeyEvent(imguiKey, action == GLFW_PRESS)
                io.setKeyEventNativeData(imguiKey, keycode, scancode) // To support legacy indexing (<1.87 user code)
            }
        }

        val charCallback: CharCB = { c: Int -> if (!imeInProgress) io.addInputCharacter(c.c) }

        val cursorEnterCallback: CursorEnterCB = { entered ->
            //            if (bd->PrevUserCallbackCursorEnter != NULL && window == bd->Window)
            //            bd->PrevUserCallbackCursorEnter(window, entered);
            if (entered)
                data.mouseWindow = data.window
            if (!entered && data.mouseWindow === data.window)
                data.mouseWindow = null
        }

        val windowFocusCallback: WindowFocusCB = { focused ->
            //            if (bd->PrevUserCallbackWindowFocus != NULL && window == bd->Window)
            //            bd->PrevUserCallbackWindowFocus(window, focused);

            io.addFocusEvent(focused)
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
            val mouseJustPressed = BooleanArray(MouseButton.COUNT)
            val mouseCursors = LongArray/*<GlfwCursor>*/(MouseCursor.COUNT)
            var installedCallbacks = false

            // Chain GLFW callbacks: our callbacks will call the user's previously installed callbacks, if any.
            //            GLFWmousebuttonfun      PrevUserCallbackMousebutton;
            //            GLFWscrollfun           PrevUserCallbackScroll;
            //            GLFWkeyfun              PrevUserCallbackKey;
            //            GLFWcharfun             PrevUserCallbackChar;
        }

        fun updateKeyModifiers() {
            val wnd = data.window.handle.value
            io.keyShift = glfwGetKey(wnd, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(wnd, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS
            io.keyCtrl  = glfwGetKey(wnd, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(wnd, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS
            io.keyAlt   = glfwGetKey(wnd, GLFW_KEY_LEFT_ALT) == GLFW_PRESS || glfwGetKey(wnd, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS
            io.keySuper = glfwGetKey(wnd, GLFW_KEY_LEFT_SUPER) == GLFW_PRESS || glfwGetKey(wnd, GLFW_KEY_RIGHT_SUPER) == GLFW_PRESS
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
                GLFW_KEY_LEFT_CONTROL -> Key.LeftControl
                GLFW_KEY_LEFT_ALT -> Key.LeftAlt
                GLFW_KEY_LEFT_SUPER -> Key.LeftSuper
                GLFW_KEY_RIGHT_SHIFT -> Key.RightShift
                GLFW_KEY_RIGHT_CONTROL -> Key.RightControl
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