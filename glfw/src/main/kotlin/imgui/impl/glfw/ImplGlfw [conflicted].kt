package imgui.impl.glfw

import glm_.b
import glm_.c
import glm_.f
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.mouseCursor
import imgui.Key
import imgui.api.g
import imgui.impl.*
import imgui.windowsIme.imeListener
import kool.cap
import kool.lim
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.glfw.*
import uno.glfw.GlfwWindow.CursorMode
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.collections.set

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

            // Setup back-end capabilities flags
            backendFlags = backendFlags or BackendFlag.HasMouseCursors   // We can honor GetMouseCursor() values (optional)
            backendFlags = backendFlags or BackendFlag.HasSetMousePos    // We can honor io.WantSetMousePos requests (optional, rarely used)
            backendPlatformName = "imgui_impl_glfw"

            // Keyboard mapping. Dear ImGui will use those indices to peek into the io.KeysDown[] array.
            keyMap[Key.Tab] = GLFW_KEY_TAB
            keyMap[Key.LeftArrow] = GLFW_KEY_LEFT
            keyMap[Key.RightArrow] = GLFW_KEY_RIGHT
            keyMap[Key.UpArrow] = GLFW_KEY_UP
            keyMap[Key.DownArrow] = GLFW_KEY_DOWN
            keyMap[Key.PageUp] = GLFW_KEY_PAGE_UP
            keyMap[Key.PageDown] = GLFW_KEY_PAGE_DOWN
            keyMap[Key.Home] = GLFW_KEY_HOME
            keyMap[Key.End] = GLFW_KEY_END
            keyMap[Key.Insert] = GLFW_KEY_INSERT
            keyMap[Key.Delete] = GLFW_KEY_DELETE
            keyMap[Key.Backspace] = GLFW_KEY_BACKSPACE
            keyMap[Key.Space] = GLFW_KEY_SPACE
            keyMap[Key.Enter] = GLFW_KEY_ENTER
            keyMap[Key.Escape] = GLFW_KEY_ESCAPE
            keyMap[Key.KeyPadEnter] = GLFW_KEY_KP_ENTER
            keyMap[Key.A] = GLFW_KEY_A
            keyMap[Key.C] = GLFW_KEY_C
            keyMap[Key.V] = GLFW_KEY_V
            keyMap[Key.X] = GLFW_KEY_X
            keyMap[Key.Y] = GLFW_KEY_Y
            keyMap[Key.Z] = GLFW_KEY_Z

            backendRendererName = null
            backendPlatformName = null
            backendLanguageUserData = null
            backendRendererUserData = null
            backendPlatformUserData = null
            setClipboardTextFn = { _, text ->  glfwSetClipboardString(clipboardUserData as Long, text) }
            getClipboardTextFn = { glfwGetClipboardString(clipboardUserData as Long) }
            clipboardUserData = window.handle.value

            if (Platform.get() == Platform.WINDOWS)
                imeWindowHandle = window.hwnd
        }

        // Create mouse cursors
        // (By design, on X11 cursors are user configurable and some cursors may be missing. When a cursor doesn't exist,
        // GLFW will emit an error which will often be printed by the app, so we temporarily disable error reporting.
        // Missing cursors will return NULL and our _UpdateMouseCursor() function will use the Arrow cursor instead.)
        val prevErrorCallback = glfwSetErrorCallback(null)
        mouseCursors[MouseCursor.Arrow.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.TextInput.i] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
        mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)  // FIXME: GLFW doesn't have this. [JVM] TODO
//         mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(GLFW_RESIZE_ALL_CURSOR)
        mouseCursors[MouseCursor.ResizeNS.i] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR)
        mouseCursors[MouseCursor.ResizeEW.i] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR)
        mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR) // FIXME: GLFW doesn't have this.
        mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR) // FIXME: GLFW doesn't have this.
        // mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(GLFW_RESIZE_NESW_CURSOR)
        // mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(GLFW_RESIZE_NWSE_CURSOR)
        mouseCursors[MouseCursor.Hand.i] = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
        mouseCursors[MouseCursor.NotAllowed.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
//        mouseCursors[MouseCursor.NotAllowed.i] = glfwCreateStandardCursor(GLFW_NOT_ALLOWED_CURSOR)
        glfwSetErrorCallback(prevErrorCallback)

        // [JVM] Chain GLFW callbacks: our callbacks will be installed in parallel with any other already existing
        if (installCallbacks) {
            // native callbacks will be added at the GlfwWindow creation via default parameter
            window.mouseButtonCBs["imgui"] = mouseButtonCallback
            window.scrollCBs["imgui"] = scrollCallback
            window.keyCBs["imgui"] = keyCallback
            window.charCBs["imgui"] = charCallback
            imeListener.install(window)
        }

        imgui.impl.clientApi = clientApi
    }

    fun shutdown() {

        mouseCursors.forEach(::glfwDestroyCursor)
        mouseCursors.fill(NULL)

        clientApi = GlfwClientApi.Unknown
    }

    private fun updateMousePosAndButtons() {

        // Update buttons
        repeat(io.mouseDown.size) {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            io.mouseDown[it] = mouseJustPressed[it] || glfwGetMouseButton(window.handle.value, it) != 0
            mouseJustPressed[it] = false
        }

        // Update mouse position
        val mousePosBackup = Vec2d(io.mousePos)
        io.mousePos put -Float.MAX_VALUE
        if (window.isFocused)
            if (io.wantSetMousePos)
                window.cursorPos = mousePosBackup
            else
                io.mousePos put (vrCursorPos ?: window.cursorPos)
        else
            vrCursorPos?.let(io.mousePos::put) // window is usually unfocused in vr
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
            window.cursor = GlfwCursor(mouseCursors[imguiCursor.i].takeIf { it != NULL }
                    ?: mouseCursors[MouseCursor.Arrow.i])
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

        assert(io.fonts.isBuilt) { "Font atlas not built! It is generally built by the renderer back-end. Missing call to renderer _NewFrame() function? e.g. ImGui_ImplOpenGL3_NewFrame()." }

        // Setup display size (every frame to accommodate for window resizing)
        val size = window.size
        val displaySize = window.framebufferSize
        io.displaySize put (vrTexSize ?: window.size)
        if (size allGreaterThan 0)
            io.displayFramebufferScale put (displaySize / size)

        // Setup time step
        val currentTime = glfw.time
        io.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        time = currentTime

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
            if (action == GLFW_PRESS && button in 0..2)
                mouseJustPressed[button] = true
        }

        val scrollCallback: ScrollCB = { offset: Vec2d ->
            io.mouseWheelH += offset.x.f
            io.mouseWheel += offset.y.f
        }

        val keyCallback: KeyCB = { key: Int, _: Int, action: Int, _: Int ->
            with(io) {
                if (key in keysDown.indices)
                    if (action == GLFW_PRESS)
                        keysDown[key] = true
                    else if (action == GLFW_RELEASE)
                        keysDown[key] = false

                // Modifiers are not reliable across systems
                keyCtrl = keysDown[GLFW_KEY_LEFT_CONTROL] || keysDown[GLFW_KEY_RIGHT_CONTROL]
                keyShift = keysDown[GLFW_KEY_LEFT_SHIFT] || keysDown[GLFW_KEY_RIGHT_SHIFT]
                keyAlt = keysDown[GLFW_KEY_LEFT_ALT] || keysDown[GLFW_KEY_RIGHT_ALT]
                keySuper = when(Platform.get()) {
                    Platform.WINDOWS -> false
                    else -> keysDown[GLFW_KEY_LEFT_SUPER] || keysDown[GLFW_KEY_RIGHT_SUPER]
                }
            }
        }

        val charCallback: CharCB = { c: Int -> if (!imeInProgress) io.addInputCharacter(c.c) }

        fun initForOpengl(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
                ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.OpenGL)

        fun initForVulkan(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
                ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.Vulkan)
    }
}