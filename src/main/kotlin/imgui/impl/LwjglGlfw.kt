package imgui.impl

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
import imgui.impl.windowsIme.imeListener
import kool.cap
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.glfw.*
import uno.glfw.GlfwWindow.CursorStatus
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.collections.set


class LwjglGlfw(val window: GlfwWindow, installCallbacks: Boolean = true, val clientApi: GlfwClientApi = GlfwClientApi.OpenGL, val vrTexSize: Vec2i? = null) {
    companion object {
        lateinit var instance: LwjglGlfw

        fun init(window: GlfwWindow, installCallbacks: Boolean = true, clientApi_: GlfwClientApi = GlfwClientApi.OpenGL, vrTexSize: Vec2i? = null) {
            instance = LwjglGlfw(window, installCallbacks, clientApi_, vrTexSize)
        }

        fun newFrame() = instance.newFrame()
        fun shutdown() = instance.shutdown()
    }

    val impl: LwjglRendererI
    var time = 0.0
    val mouseCursors = LongArray(MouseCursor.COUNT)

    var vrCursorPos: Vec2? = null

    enum class GlfwClientApi { OpenGL, OpenGL3, OpenGL2, Vulkan }

    val mouseButtonCallback: MouseButtonCallbackT = { button: Int, action: Int, _: Int ->
        if (action == GLFW_PRESS && button in 0..2)
            mouseJustPressed[button] = true
    }

    val scrollCallback: ScrollCallbackT = { offset: Vec2d ->
        io.mouseWheelH += offset.x.f
        io.mouseWheel += offset.y.f
    }

    val keyCallback: KeyCallbackT = { key: Int, _: Int, action: Int, _: Int ->
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
            keySuper = keysDown[GLFW_KEY_LEFT_SUPER] || keysDown[GLFW_KEY_RIGHT_SUPER]
        }
    }

    val charCallback: CharCallbackT = { c: Int -> if (!g.imeInProgress && c in 1..65535) io.addInputCharacter(c.c) }

    init {

        with(io) {

            // Setup back-end capabilities flags
            backendFlags = backendFlags or BackendFlag.HasMouseCursors   // We can honor GetMouseCursor() values (optional)
            backendFlags = backendFlags or BackendFlag.HasSetMousePos    // We can honor io.WantSetMousePos requests (optional, rarely used)

            // Keyboard mapping. ImGui will use those indices to peek into the io.KeysDown[] array.
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
            setClipboardTextFn = { glfwSetClipboardString(clipboardUserData, it) }
            getClipboardTextFn = { glfwGetClipboardString(clipboardUserData) ?: "" }
            clipboardUserData = window.handle.L

            if (Platform.get() == Platform.WINDOWS)
                imeWindowHandle = window.hwnd
        }

        mouseCursors[MouseCursor.Arrow.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.TextInput.i] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
        mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)  // FIXME: GLFW doesn't have this.
        mouseCursors[MouseCursor.ResizeNS.i] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR)
        mouseCursors[MouseCursor.ResizeEW.i] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR)
        mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR) // FIXME: GLFW doesn't have this.
        mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR) // FIXME: GLFW doesn't have this.
        mouseCursors[MouseCursor.Hand.i] = glfwCreateStandardCursor(GLFW_HAND_CURSOR)

        if (installCallbacks) {
            // native callbacks will be added at the GlfwWindow creation via default parameter
            window.mouseButtonCallbacks["imgui"] = mouseButtonCallback
            window.scrollCallbacks["imgui"] = scrollCallback
            window.keyCallbacks["imgui"] = keyCallback
            window.charCallbacks["imgui"] = charCallback // TODO check if used (jogl doesnt have)
            imeListener.install(window)
        }

        impl = when (clientApi) {
            GlfwClientApi.OpenGL3 -> ImplGL3()
            GlfwClientApi.OpenGL2 -> ImplGL2()
            GlfwClientApi.OpenGL -> {
                val glcaps = GL.getCapabilities()
                when {
                    glcaps.OpenGL32 -> ImplGL3()
                    (Platform.get() == Platform.WINDOWS) and glcaps.OpenGL30 -> ImplGL3()
                    glcaps.OpenGL20 -> ImplGL2()
                    else -> throw RuntimeException("JVM ImGui Requires OpenGL 2.0, you do not support that!")
                }
            }
            GlfwClientApi.Vulkan -> TODO("Vulkan support is not yet implemented!") //ImplVk.init()
        }
    }

    fun newFrame() {

        if (fontTexture[0] == 0)
            impl.createDeviceObjects()

        assert(io.fonts.isBuilt) { "Font atlas not built! It is generally built by the renderer back-end. Missing call to renderer _NewFrame() function? e.g. ImGui_ImplOpenGL3_NewFrame()." }

        // Setup display size (every frame to accommodate for window resizing)
        io.displaySize put (vrTexSize ?: window.size)
        io.displayFramebufferScale.x = if (window.size.x > 0) window.framebufferSize.x / window.size.x.f else 0f
        io.displayFramebufferScale.y = if (window.size.y > 0) window.framebufferSize.y / window.size.y.f else 0f

        // Setup time step
        val currentTime = glfw.time
        io.deltaTime = if (time > 0) (currentTime - time).f else 1f / 60f
        time = currentTime

        updateMousePosAndButtons()
        updateMouseCursor()

        // Gamepad navigation mapping
        updateGamepads()
    }

    fun updateGamepads() {

        io.navInputs.fill(0f)
        if (io.configFlags has ConfigFlag.NavEnableGamepad) {
            // Update gamepad inputs
            val buttons = window.joystick1Buttons ?: ByteBuffer.wrap(byteArrayOf())
            val buttonsCount = buttons.cap
            val axes = window.joystick1Axes ?: FloatBuffer.wrap(floatArrayOf())
            val axesCount = axes.cap

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

    fun renderDrawData(drawData: DrawData) = impl.renderDrawData(drawData)

    private fun updateMousePosAndButtons() {

        if (io.configFlags has ConfigFlag.NoMouseUpdate)
            return

        repeat(io.mouseDown.size) {
            /*  If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release
                events that are shorter than 1 frame.   */
            io.mouseDown[it] = mouseJustPressed[it] || window.isPressed(MouseButton.of(it))
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

        if (io.configFlags has ConfigFlag.NoMouseCursorChange || window.cursorStatus == CursorStatus.Disabled)
            return

        val imguiCursor = mouseCursor
        if (imguiCursor == MouseCursor.None || io.mouseDrawCursor)
        // Hide OS mouse cursor if imgui is drawing it or if it wants no cursor
            window.cursorStatus = CursorStatus.Hidden
        else {
            // Show OS mouse cursor
            // FIXME-PLATFORM: Unfocused windows seems to fail changing the mouse cursor with GLFW 3.2, but 3.3 works here.
            window.cursor = GlfwCursor(mouseCursors[imguiCursor.i].takeIf { it != NULL }
                    ?: mouseCursors[MouseCursor.Arrow.i])
            window.cursorStatus = CursorStatus.Normal
        }
    }

    fun shutdown() {

        // Destroy GLFW mouse cursors
        mouseCursors.forEach(::glfwDestroyCursor)
        mouseCursors.fill(NULL)

        impl.destroyDeviceObjects()
    }
}