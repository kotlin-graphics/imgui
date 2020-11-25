package imgui.impl.glfw

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.mouseCursor
import imgui.Key
import imgui.classes.*
import imgui.impl.*
import imgui.windowsIme.COMPOSITIONFORM
import imgui.windowsIme.HIMC
import imgui.windowsIme.imeListener
import imgui.windowsIme.imm
import kool.BYTES
import kool.Stack
import kool.lim
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.system.Platform
import org.lwjgl.system.windows.User32
import org.lwjgl.system.windows.User32.*
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

    /** ~ImGui_ImplGlfw_Init */
    init {

        imgui.impl.window = window
        time = 0.0

        with(io) {

            // Setup backend capabilities flags
            backendFlags = backendFlags or BackendFlag.HasMouseCursors      // We can honor GetMouseCursor() values (optional)
            backendFlags = backendFlags or BackendFlag.HasSetMousePos       // We can honor io.WantSetMousePos requests (optional, rarely used)
            backendFlags = backendFlags or BackendFlag.PlatformHasViewports // We can create multi-viewports on the Platform side (optional)
            if (GLFW_HAS_MOUSE_PASSTHROUGH || (GLFW_HAS_WINDOW_HOVERED && Platform.get() == Platform.WINDOWS))
                backendFlags = backendFlags or BackendFlag.HasMouseHoveredViewport // We can set io.MouseHoveredViewport correctly (optional, not easy)
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

            setClipboardTextFn =
                    { _, text -> glfwSetClipboardString(clipboardUserData as Long, text) } // TODO uno -> clipboard
            getClipboardTextFn = { glfwGetClipboardString(clipboardUserData as Long) }
            clipboardUserData = window.handle.value

//            if (Platform.get() == Platform.WINDOWS)
//                imeWindowHandle = window.hwnd
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
        mouseCursors[MouseCursor.Hand.i] = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
        mouseCursors[MouseCursor.ResizeAll.i] = glfwCreateStandardCursor(if (GLFW_HAS_NEW_CURSORS) 0x00036009/*GLFW_RESIZE_ALL_CURSOR*/ else GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.ResizeNESW.i] = glfwCreateStandardCursor(if (GLFW_HAS_NEW_CURSORS) 0x00036008/*GLFW_RESIZE_NESW_CURSOR*/ else GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.ResizeNWSE.i] = glfwCreateStandardCursor(if (GLFW_HAS_NEW_CURSORS) 0x00036007/*GLFW_RESIZE_NWSE_CURSOR*/ else GLFW_ARROW_CURSOR)
        mouseCursors[MouseCursor.NotAllowed.i] = glfwCreateStandardCursor(if (GLFW_HAS_NEW_CURSORS) 0x0003600A/*GLFW_NOT_ALLOWED_CURSOR*/ else GLFW_ARROW_CURSOR)

        glfwSetErrorCallback(prevErrorCallback)

        // [JVM] Chain GLFW callbacks: our callbacks will be installed in parallel with any other already existing
        if (installCallbacks) {
            // native callbacks will be added at the GlfwWindow creation via default parameter
            window.mouseButtonCBs["imgui"] = mouseButtonCB
            window.scrollCBs["imgui"] = scrollCB
            window.keyCBs["imgui"] = keyCB
            window.charCBs["imgui"] = charCB
            imeListener.install(window)
        }

        // Update monitors the first time (note: monitor callback are broken in GLFW 3.2 and earlier, see github.com/glfw/glfw/issues/784)
        updateMonitors()
        glfwSetMonitorCallback(monitorCB)

        // Our mouse update function expect PlatformHandle to be filled for the main viewport
        val mainViewport = ImGui.mainViewport
        mainViewport.platformHandle = window.handle
        if (Platform.get() == Platform.WINDOWS)
            mainViewport.platformHandleRaw = GLFWNativeWin32.glfwGetWin32Window(window.handle.value)
        if (io.configFlags has ConfigFlag.ViewportsEnable)
            initPlatformInterface()

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
        io.mouseHoveredViewport = 0
        val platformIO = ImGui.platformIO
        for (viewport in platformIO.viewports) {
            val window = viewport.platformHandle as GlfwWindowHandle
            assert(window.value != NULL)
            val focused = glfwGetWindowAttrib(window.value, GLFW_FOCUSED) != 0
            if (focused) {
                if (io.wantSetMousePos)
                    glfwSetCursorPos(window.value, mousePosBackup.x - viewport.pos.x, mousePosBackup.y - viewport.pos.y)
                else
                    Stack {
                        val mouse = it.nmalloc(Double.BYTES, Double.BYTES * 2)
                        nglfwGetCursorPos(window.value, mouse, mouse + Double.BYTES)
                        val mouseX = memGetDouble(mouse)
                        val mouseY = memGetDouble(mouse + Double.BYTES)
                        if (io.configFlags has ConfigFlag.ViewportsEnable) {
                            // Multi-viewport mode: mouse position in OS absolute coordinates (io.MousePos is (0,0) when the mouse is on the upper-left of the primary monitor)
                            val wnd = it.nmalloc(Int.BYTES, Int.BYTES * 2)
                            nglfwGetWindowPos(window.value, wnd, wnd + Int.BYTES)
                            io.mousePos.put(mouseX + memGetInt(wnd), mouseY + memGetInt(wnd + Int.BYTES))
                        } else // Single viewport mode: mouse position in client window coordinates (io.MousePos is (0,0) when the mouse is on the upper-left corner of the app window)
                            io.mousePos.put(mouseX.f, mouseY.f)
                    }
                for (i in io.mouseDown.indices)
                    io.mouseDown[i] = io.mouseDown[i] || glfwGetMouseButton(window.value, i) != 0
            } else
                vrCursorPos?.let(io.mousePos::put) // [JVM] window is usually unfocused in vr

            // (Optional) When using multiple viewports: set io.MouseHoveredViewport to the viewport the OS mouse cursor is hovering.
            // Important: this information is not easy to provide and many high-level windowing library won't be able to provide it correctly, because
            // - This is _ignoring_ viewports with the ImGuiViewportFlags_NoInputs flag (pass-through windows).
            // - This is _regardless_ of whether another viewport is focused or being dragged from.
            // If ImGuiBackendFlags_HasMouseHoveredViewport is not set by the back-end, imgui will ignore this field and infer the information by relying on the
            // rectangles and last focused time of every viewports it knows about. It will be unaware of other windows that may be sitting between or over your windows.
            // [GLFW] FIXME: This is currently only correct on Win32. See what we do below with the WM_NCHITTEST, missing an equivalent for other systems.
            // See https://github.com/glfw/glfw/issues/1236 if you want to help in making this a GLFW feature.
            if (GLFW_HAS_MOUSE_PASSTHROUGH || (GLFW_HAS_WINDOW_HOVERED && Platform.get() == Platform.WINDOWS)) {
                val windowNoInput = viewport.flags has ViewportFlag.NoInputs
                if (GLFW_HAS_MOUSE_PASSTHROUGH)
                    glfwSetWindowAttrib(window.value, GLFW_MOUSE_PASSTHROUGH.i, windowNoInput.i)
                if (glfwGetWindowAttrib(window.value, GLFW_HOVERED) != 0 && !windowNoInput)
                    io.mouseHoveredViewport = viewport.id
            }
        }
    }

    private fun updateMouseCursor() {

        if (io.configFlags has ConfigFlag.NoMouseCursorChange || window.cursorMode == CursorMode.disabled)
            return

        val imguiCursor = mouseCursor
        val platformIO = ImGui.platformIO
        for (n in platformIO.viewports.indices) {
            val window = GlfwWindow(platformIO.viewports[n].platformHandle as GlfwWindowHandle)
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

    fun updateMonitors() {
        val platformIO = ImGui.platformIO
        val glfwMonitors = glfw.monitors
        platformIO.monitors.clear()
        for (glfwMonitor in glfwMonitors) {
            val monitor = PlatformMonitor()
            val pos = glfwMonitor.pos
            val vidMode = glfwMonitor.videoMode
            monitor.mainPos put pos
            monitor.workPos put pos
            monitor.mainSize put vidMode.size
            monitor.workSize put vidMode.size
            if (GLFW_HAS_MONITOR_WORK_AREA) {
                val workArea = glfwMonitor.workArea
                if (workArea.z > 0 && workArea.w > 0) {
                    monitor.workPos.put(workArea.x, workArea.y)
                    monitor.workSize.put(workArea.z, workArea.w)
                }
            }
            if (GLFW_HAS_PER_MONITOR_DPI)
            // Warning: the validity of monitor DPI information on Windows depends on the application DPI awareness settings, which generally needs to be set in the manifest or at runtime.
                monitor.dpiScale = glfwMonitor.contentScale.x
            platformIO.monitors += monitor
        }
        wantUpdateMonitors = false
    }

    fun newFrame() {

        assert(io.fonts.isBuilt) { "Font atlas not built! It is generally built by the renderer backend. Missing call to renderer _NewFrame() function? e.g. ImGui_ImplOpenGL3_NewFrame()." }

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

        fun initForOpenGL(window: GlfwWindow, installCallbacks: Boolean) =
                ImplGlfw(window, installCallbacks).also { clientApi = GlfwClientApi.OpenGL }

        fun initForVulkan(window: GlfwWindow, installCallbacks: Boolean) =
                ImplGlfw(window, installCallbacks).also { clientApi = GlfwClientApi.Vulkan }

        val mouseButtonCB: MouseButtonCB = { button: Int, action: Int, _: Int ->
            if (action == GLFW_PRESS && button in 0..2)
                mouseJustPressed[button] = true
        }

        val scrollCB: ScrollCB = { offset: Vec2d ->
            io.mouseWheelH += offset.x.f
            io.mouseWheel += offset.y.f
        }

        val keyCB: KeyCB = { key: Int, _: Int, action: Int, _: Int ->
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
                keySuper = when (Platform.get()) {
                    Platform.WINDOWS -> false
                    else -> keysDown[GLFW_KEY_LEFT_SUPER] || keysDown[GLFW_KEY_RIGHT_SUPER]
                }
            }
        }

        val charCB: CharCB = { c: Int -> if (!imeInProgress) io.addInputCharacter(c.c) }

        val monitorCB = GLFWMonitorCallback.create { _, _ -> wantUpdateMonitors = true }

//        val windowCloseCB: WindowCloseCallbackT = {
//            ImGui.findViewportByPlatformHandle(window!!)?.let { viewport ->
//                viewport.platformRequestClose = true
//            }
//        }

        fun initForOpengl(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
                ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.OpenGL)

        fun initForVulkan(window: GlfwWindow, installCallbacks: Boolean = true, vrTexSize: Vec2i? = null): ImplGlfw =
                ImplGlfw(window, installCallbacks, vrTexSize, GlfwClientApi.Vulkan)

        /** ~ImGui_ImplGlfw_InitPlatformInterface */
        fun initPlatformInterface() {
            // Register platform interface (will be coupled with a renderer interface)
            ImGui.platformIO.also {
                it.platform_CreateWindow = ::createWindow
                it.platform_DestroyWindow = ::destroyWindow
                it.platform_ShowWindow = ::showWindow
                it.platform_SetWindowPos = ::setWindowPos
                it.platform_GetWindowPos = ::getWindowPos
                it.platform_SetWindowSize = ::setWindowSize
                it.platform_GetWindowSize = ::getWindowSize
                it.platform_SetWindowFocus = ::setWindowFocus
                it.platform_GetWindowFocus = ::getWindowFocus
                it.platform_GetWindowMinimized = ::getWindowMinimized
                it.platform_SetWindowTitle = ::setWindowTitle
                it.platform_RenderWindow = ::renderWindow
                it.platform_SwapBuffers = ::swapBuffers
                if (GLFW_HAS_WINDOW_ALPHA)
                    it.platform_SetWindowAlpha = ::setWindowAlpha
//                if(GLFW_HAS_VULKAN)
//                    it.platform_CreateVkSurface = ImGui_ImplGlfw_CreateVkSurface
                if (HAS_WIN32_IME)
                    it.platform_SetImeInputPos = ::ImplWin32_SetImeInputPos
            }
            // Register main window handle (which is owned by the main application, not by us)
            // This is mostly for simplicity and consistency, so that our code (e.g. mouse handling etc.) can use same logic for main and secondary viewports.
            val mainViewport = ImGui.mainViewport
            val data = ViewportDataGlfw()
            data.window = window
            data.windowOwned = false
            mainViewport.platformUserData = data
            mainViewport.platformHandle = window!!.handle
        }

        fun createWindow(viewport: Viewport) {
            val data = ViewportDataGlfw()
            viewport.platformUserData = data

            // GLFW 3.2 unfortunately always set focus on glfwCreateWindow() if GLFW_VISIBLE is set, regardless of GLFW_FOCUSED
            // With GLFW 3.3, the hint GLFW_FOCUS_ON_SHOW fixes this problem
            glfw.windowHint {
                visible = false
                focused = false
                if (GLFW_HAS_FOCUS_ON_SHOW)
                    focusOnShow = false
                decorated = viewport.flags hasnt ViewportFlag.NoDecoration
                if (GLFW_HAS_WINDOW_TOPMOST)
                    floating = viewport.flags has ViewportFlag.TopMost
            }
            val shareWindow = window.takeIf { clientApi == GlfwClientApi.OpenGL }?.handle?.value ?: NULL
            val handle = glfwCreateWindow(viewport.size.x.i, viewport.size.y.i, "No Title Yet", NULL, shareWindow)
            data.window = GlfwWindow(GlfwWindowHandle(handle))
            data.windowOwned = true
            viewport.platformHandle = data.window!!.handle
            if (Platform.get() == Platform.WINDOWS)
                viewport.platformHandleRaw = GLFWNativeWin32.glfwGetWin32Window(data.window?.handle?.value ?: NULL)
            data.window!!.apply {

                installNativeCallbacks()
                installDefaultCallbacks()

                pos = Vec2i(viewport.pos)

                // Install GLFW callbacks for secondary viewports
                mouseButtonCBs["imgui"] = mouseButtonCB!!
                scrollCBs["imgui"] = scrollCB!!
                keyCBs["imgui"] = keyCB!!
//                charCBs["imgui"] = charCB!! TODO
//                windowCloseCallbacks["imgui"] = windowCloseCB
                glfwSetWindowCloseCallback(handle) { window ->
                    ImGui.findViewportByPlatformHandle(GlfwWindowHandle(window))?.let { viewport ->
                        viewport.platformRequestClose = true
                    }
                }
                glfwSetWindowPosCallback(handle) { window, _, _ ->
                    ImGui.findViewportByPlatformHandle(GlfwWindowHandle(window))?.let { viewport ->
                        viewport.platformRequestMove = true
                    }
                }
//                windowPosCallbacks["imgui"] = wdata->Window, ImGui_ImplGlfw_WindowPosCallback)
                glfwSetWindowSizeCallback(handle) { window, _, _ ->
                    var ignoreEvent = false
                    ImGui.findViewportByPlatformHandle(GlfwWindowHandle(window))?.let { viewport ->
                        (viewport.platformUserData as? ViewportDataGlfw)?.let { data ->
                            // GLFW may dispatch window size event after calling glfwSetWindowSize().
                            // However depending on the platform the callback may be invoked at different time: on Windows it
                            // appears to be called within the glfwSetWindowSize() call whereas on Linux it is queued and invoked
                            // during glfwPollEvents().
                            // Because the event doesn't always fire on glfwSetWindowSize() we use a frame counter tag to only
                            // ignore recent glfwSetWindowSize() calls.
                            ignoreEvent = ImGui.frameCount <= data.ignoreWindowSizeEventFrame + 1
                            data.ignoreWindowSizeEventFrame = -1
                        }
                        if (!ignoreEvent)
                            viewport.platformRequestResize = true
                    }
                }
                if (clientApi == GlfwClientApi.OpenGL) {
                    makeContextCurrent()
                    glfw.swapInterval = VSync.OFF
                }
            }
        }

        fun destroyWindow(viewport: Viewport) {
            (viewport.platformUserData as? ViewportDataGlfw)?.let { data ->
                if (data.windowOwned) {
                    if (!GLFW_HAS_MOUSE_PASSTHROUGH && GLFW_HAS_WINDOW_HOVERED && Platform.get() == Platform.WINDOWS) {
                        val hwnd = HWND(viewport.platformHandleRaw as Long)
                        TODO()
//                    ::RemovePropA(hwnd, "IMGUI_VIEWPORT");
                    }
                    data.window!!.destroy()
                }
                data.window = null
            }
            viewport.platformUserData = null
            viewport.platformHandle = null
        }

        val GetWindowLong by lazy { User32.getLibrary().getFunctionAddress("GetWindowLong") }
        val SetWindowLong by lazy { User32.getLibrary().getFunctionAddress("SetWindowLong") }
        fun getWindowLong(hWnd: HWND, nIndex: Int) = JNI.callPI(hWnd.L, nIndex, GetWindowLong)
        fun setWindowLong(hWnd: HWND, nIndex: Int, dwNewLong: Int) =
            JNI.callPI(hWnd.L, nIndex, dwNewLong, SetWindowLong)

        fun showWindow(viewport: Viewport) {

            val data = viewport.platformUserData as ViewportDataGlfw

            if (Platform.get() == Platform.WINDOWS) {
                // GLFW hack: Hide icon from task bar
                val hwnd = HWND(viewport.platformHandleRaw as Long)
                if (viewport.flags has ViewportFlag.NoTaskBarIcon) {
                    var exStyle = getWindowLong(hwnd, GWL_EXSTYLE)
                    exStyle = exStyle wo WS_EX_APPWINDOW
                    exStyle = exStyle or WS_EX_TOOLWINDOW
                    setWindowLong(hwnd, GWL_EXSTYLE, exStyle)
                }

                // GLFW hack: install hook for WM_NCHITTEST message handler
                if (!GLFW_HAS_MOUSE_PASSTHROUGH && GLFW_HAS_WINDOW_HOVERED && Platform.get() == Platform.WINDOWS) {
                    TODO()
//                    SetPropA(hwnd, "IMGUI_VIEWPORT", viewport)
//                    if (g_GlfwWndProc == NULL)
//                        g_GlfwWndProc = (WNDPROC)::GetWindowLongPtr(hwnd, GWLP_WNDPROC)
//                    ::SetWindowLongPtr(hwnd, GWLP_WNDPROC, (LONG_PTR) WndProcNoInputs)
                }

                if (!GLFW_HAS_FOCUS_ON_SHOW)
                // GLFW hack: GLFW 3.2 has a bug where glfwShowWindow() also activates/focus the window.
                // The fix was pushed to GLFW repository on 2018/01/09 and should be included in GLFW 3.3 via a GLFW_FOCUS_ON_SHOW window attribute.
                // See https://github.com/glfw/glfw/issues/1189
                // FIXME-VIEWPORT: Implement same work-around for Linux/OSX in the meanwhile.
                    if (viewport.flags has ViewportFlag.NoFocusOnAppearing) {
                        User32.ShowWindow(hwnd.L, SW_SHOWNA)
                        return
                    }
            }

            data.window!!.show()
        }

        fun getWindowPos(viewport: Viewport): Vec2 {
            val data = viewport.platformUserData as ViewportDataGlfw
            return Vec2(data.window!!.pos)
        }

        fun setWindowPos(viewport: Viewport, pos: Vec2) {
            val data = viewport.platformUserData as ViewportDataGlfw
            data.window!!.pos = Vec2i(pos)
        }

        fun getWindowSize(viewport: Viewport): Vec2 {
            val data = viewport.platformUserData as ViewportDataGlfw
            return Vec2(data.window!!.size)
        }

        fun setWindowSize(viewport: Viewport, size: Vec2) {
            val data = viewport.platformUserData as ViewportDataGlfw
            val wnd = data.window!!
            if (Platform.get() == Platform.MACOSX && !GLFW_HAS_OSX_WINDOW_POS_FIX) {
                // Native OS windows are positioned from the bottom-left corner on macOS, whereas on other platforms they are
                // positioned from the upper-left corner. GLFW makes an effort to convert macOS style coordinates, however it
                // doesn't handle it when changing size. We are manually moving the window in order for changes of size to be based
                // on the upper-left corner.
                val pos = wnd.pos
                val sz = wnd.size
                wnd.pos = Vec2i(pos.x, pos.y - sz.y + size.y)
            }
            data.ignoreWindowSizeEventFrame = ImGui.frameCount
            wnd.size = Vec2i(size)
        }

        fun setWindowTitle(viewport: Viewport, title: String) {
            val data = viewport.platformUserData as ViewportDataGlfw
            data.window!!.title = title
        }

        fun setWindowFocus(viewport: Viewport) {
            if (GLFW_HAS_FOCUS_WINDOW) {
                val data = viewport.platformUserData as ViewportDataGlfw
                data.window!!.focus()
            }
            // FIXME: What are the effect of not having this function? At the moment imgui doesn't actually call SetWindowFocus - we set that up ahead, will answer that question later.
        }

        fun getWindowFocus(viewport: Viewport): Boolean {
            val data = viewport.platformUserData as ViewportDataGlfw
            return data.window!!.isFocused
        }

        fun getWindowMinimized(viewport: Viewport): Boolean {
            val data = viewport.platformUserData as ViewportDataGlfw
            return data.window!!.isIconified
        }

        fun setWindowAlpha(viewport: Viewport, alpha: Float) {
            val data = viewport.platformUserData as ViewportDataGlfw
            data.window!!.opacity = alpha
        }

        fun renderWindow(viewport: Viewport, dummy: Any?) {
            val data = viewport.platformUserData as ViewportDataGlfw
            if (clientApi == GlfwClientApi.OpenGL)
                data.window!!.makeContextCurrent()
        }

        fun swapBuffers(viewport: Viewport, dummy: Any?) {
            val data = viewport.platformUserData as ViewportDataGlfw
            if (clientApi == GlfwClientApi.OpenGL) {
                data.window!!.makeContextCurrent()
                data.window!!.swapBuffers()
            }
        }

        //--------------------------------------------------------------------------------------------------------
        // IME (Input Method Editor) basic support for e.g. Asian language users
        //--------------------------------------------------------------------------------------------------------

        // We provide a Win32 implementation because this is such a common issue for IME users
        fun ImplWin32_SetImeInputPos(viewport: Viewport, pos: Vec2) {
            val hwnd = viewport.platformHandleRaw as HWND
            if (hwnd.L != NULL) {
                val himc = HIMC(imm.getContext(hwnd))
                if (himc.L != NULL)
                    Stack { stack ->
                        val cf = COMPOSITIONFORM(stack).apply {
                            dwStyle = imm.CFS_FORCE_POSITION
                            ptCurrentPos.x = (pos.x - viewport.pos.x).L
                            ptCurrentPos.y = (pos.y - viewport.pos.y).L
                        }
                        if (imm.setCompositionWindow(himc, cf) == 0)
                            System.err.println("imm.setCompositionWindow failed")
                        if (imm.releaseContext(hwnd, himc) == 0)
                            System.err.println("imm.releaseContext failed")
                    }
            }
        }
    }
}

//--------------------------------------------------------------------------------------------------------
// MULTI-VIEWPORT / PLATFORM INTERFACE SUPPORT
// This is an _advanced_ and _optional_ feature, allowing the back-end to create and handle multiple viewports simultaneously.
// If you are new to dear imgui or creating a new binding for dear imgui, it is recommended that you completely ignore this section first..
//--------------------------------------------------------------------------------------------------------

/** Helper structure we store in the void* RenderUserData field of each ImGuiViewport to easily retrieve our backend data. */
class ViewportDataGlfw {
    var window: GlfwWindow? = null
    var windowOwned = false
    var ignoreWindowPosEventFrame = -1
    var ignoreWindowSizeEventFrame = -1

    fun destroy() = assert(window == null)
}

var windowCloseCallback = GLFWWindowCloseCallback.create { window ->
    ImGui.findViewportByPlatformHandle(GlfwWindow.from(window))?.let { viewport ->
        viewport.platformRequestClose = true
    }
}

// GLFW may dispatch window pos/size events after calling glfwSetWindowPos()/glfwSetWindowSize().
// However: depending on the platform the callback may be invoked at different time:
// - on Windows it appears to be called within the glfwSetWindowPos()/glfwSetWindowSize() call
// - on Linux it is queued and invoked during glfwPollEvents()
// Because the event doesn't always fire on glfwSetWindowXXX() we use a frame counter tag to only
// ignore recent glfwSetWindowXXX() calls.
var windowPosCallback = GLFWWindowPosCallback.create { window, _, _ ->
    ImGui.findViewportByPlatformHandle(GlfwWindow.from(window))?.let { viewport ->
        var ignoreEvent = false
        (viewport.platformUserData as? ViewportDataGlfw)?.let { data ->
            ignoreEvent = ImGui.frameCount <= data.ignoreWindowPosEventFrame + 1
            //data->IgnoreWindowPosEventFrame = -1;
//            if (ignoreEvent)
//                return
        }
        if (!ignoreEvent)
            viewport.platformRequestMove = true
    }
}

var windowSizeCallback = GLFWScrollCallback.create { window, xoffset, yoffset ->
    ImGui.findViewportByPlatformHandle(GlfwWindow.from(window))?.let { viewport ->
        var ignoreEvent = false
        (viewport.platformUserData as? ViewportDataGlfw)?.let { data ->
            ignoreEvent = ImGui.frameCount <= data.ignoreWindowSizeEventFrame + 1
            //data->IgnoreWindowPosEventFrame = -1;
//            if (ignoreEvent)
//                return
        }
        if (!ignoreEvent)
            viewport.platformRequestResize = true
    }
}