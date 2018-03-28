package imgui.imgui

import gli_.has
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.buttonBehavior
import imgui.ImGui.clearActiveId
import imgui.ImGui.clearDragDrop
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.end
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.keepAliveId
import imgui.ImGui.popId
import imgui.ImGui.pushId
import imgui.ImGui.setActiveId
import imgui.ImGui.setNextWindowSize
import imgui.internal.*
import kotlin.math.max
import kotlin.math.min
import imgui.WindowFlag as Wf
import imgui.internal.DrawListFlag as Dlf
import imgui.ConfigFlag as Cf

interface imgui_main {

    val io
        get() = gImGui?.io
                ?: throw Error("No current context. Did you call ImGui::Context() or ImGui::setCurrentContext()?")

    val style
        get() = gImGui?.style
                ?: throw Error("No current context. Did you call ImGui::Context() or ImGui::setCurrentContext()?")

    /** start a new ImGui frame, you can submit any command from this point until NewFrame()/Render().  */
    fun newFrame() {

        ptrIndices = 0

        /* (We pass an error message in the assert expression as a trick to get it visible to programmers who are not 
            using a debugger, as most assert handlers display their argument)         */
        assert(g.initialized)
        assert(io.deltaTime >= 0f) { "Need a positive deltaTime (zero is tolerated but will cause some timing issues)" }
        assert(io.displaySize greaterThanEqual 0) { "Invalid displaySize value" }
        assert(io.fonts.fonts.size > 0) { "Font Atlas not built. Did you call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8() ?" }
        assert(io.fonts.fonts[0].isLoaded) { "Font Atlas not built. Did you call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8() ?" }
        assert(style.curveTessellationTol > 0f) { "Invalid style setting" }
        assert(style.alpha in 0f..1f) { "Invalid style setting. Alpha cannot be negative (allows us to avoid a few clamps in color computations)" }
        assert(g.frameCount == 0 || g.frameCountEnded == g.frameCount) { "Forgot to call render() or endFrame() at the end of the previous frame?" }
        for (k in Key.values())
            assert(io.keyMap[k.i] >= -1 && io.keyMap[k.i] < io.keysDown.size) { "io.KeyMap[] contains an out of bound value (need to be 0..512, or -1 for unmapped key)" }

        /*  Do a simple check for required key mapping (we intentionally do NOT check all keys to not pressure user into
            setting up everything, but Space is required and was super recently added in 1.60 WIP)         */
        if (io.configFlags has Cf.NavEnableKeyboard)
            assert(io.keyMap[Key.Space] != -1) { "ImGuiKey_Space is not mapped, required for keyboard navigation." }

        // Load settings on first frame
        if (!g.settingsLoaded) {
            assert(g.settingsWindows.isEmpty())
            loadIniSettingsFromDisk(io.iniFilename)
            g.settingsLoaded = true
        }

        g.time += io.deltaTime
        g.frameCount += 1
        g.tooltipOverrideCount = 0
        g.windowsActiveCount = 0

        defaultFont.setCurrent()
        assert(g.font.isLoaded)
        g.drawListSharedData.clipRectFullscreen.put(0f, 0f, io.displaySize)
        g.drawListSharedData.curveTessellationTol = style.curveTessellationTol

        g.overlayDrawList.clear()
        g.overlayDrawList.pushTextureId(io.fonts.texId)
        g.overlayDrawList.pushClipRectFullScreen()
        g.overlayDrawList.flags = (if (style.antiAliasedLines) Dlf.AntiAliasedLines.i else 0) or if (style.antiAliasedFill) Dlf.AntiAliasedFill.i else 0

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it
        g.drawData.clear()

        // Clear reference to active widget if the widget isn't alive anymore
        if (g.hoveredIdPreviousFrame == 0) g.hoveredIdTimer = 0f
        g.hoveredIdPreviousFrame = g.hoveredId
        g.hoveredId = 0
        g.hoveredIdAllowOverlap = false
        if (!g.activeIdIsAlive && g.activeIdPreviousFrame == g.activeId && g.activeId != 0)
            clearActiveId()
        if (g.activeId != 0) g.activeIdTimer += io.deltaTime
        g.activeIdPreviousFrame = g.activeId
        g.activeIdIsAlive = false
        g.activeIdIsJustActivated = false
        if (g.scalarAsInputTextId != 0 && g.activeId != g.scalarAsInputTextId)
            g.scalarAsInputTextId = 0

        // Update keyboard input state
        for (i in 0 until io.keysDownDuration.size) io.keysDownDurationPrev[i] = io.keysDownDuration[i]
        for (i in 0 until io.keysDown.size)
            io.keysDownDuration[i] =
                    if (io.keysDown[i])
                        if (io.keysDownDuration[i] < 0f) 0f
                        else io.keysDownDuration[i] + io.deltaTime
                    else -1f

        // Elapse drag & drop payload
        if (g.dragDropActive && g.dragDropPayload.dataFrameCount + 1 < g.frameCount) {
            clearDragDrop()
            g.dragDropPayloadBufHeap.clear()
            val size = g.dragDropPayloadBufLocal.size
            g.dragDropPayloadBufLocal = ByteArray(size)
        }
        g.dragDropAcceptIdPrev = g.dragDropAcceptIdCurr
        g.dragDropAcceptIdCurr = 0
        g.dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE

        // Update gamepad/keyboard directional navigation
        navUpdate()

        /*  Update mouse input state
            If mouse just appeared or disappeared (usually denoted by -Float.MAX_VALUE component, but in reality we test
            for -256000.0f) we cancel out movement in MouseDelta         */
        if (isMousePosValid(io.mousePos) && isMousePosValid(io.mousePosPrev))
            io.mouseDelta = io.mousePos - io.mousePosPrev
        else
            io.mouseDelta put 0f
        if (io.mouseDelta.x != 0f || io.mouseDelta.y != 0f)
            g.navDisableMouseHover = false
        io.mousePosPrev put io.mousePos

        for (i in io.mouseDown.indices) {
            io.mouseClicked[i] = io.mouseDown[i] && io.mouseDownDuration[i] < 0f
            io.mouseReleased[i] = !io.mouseDown[i] && io.mouseDownDuration[i] >= 0f
            io.mouseDownDurationPrev[i] = io.mouseDownDuration[i]
            io.mouseDownDuration[i] = when (io.mouseDown[i]) {
                true -> if (io.mouseDownDuration[i] < 0f) 0f else io.mouseDownDuration[i] + io.deltaTime
                else -> -1f
            }
            io.mouseDoubleClicked[i] = false
            if (io.mouseClicked[i]) {
                if (g.time - io.mouseClickedTime[i] < io.mouseDoubleClickTime) {
                    if ((io.mousePos - io.mouseClickedPos[i]).lengthSqr < io.mouseDoubleClickMaxDist * io.mouseDoubleClickMaxDist)
                        io.mouseDoubleClicked[i] = true
                    io.mouseClickedTime[i] = -Float.MAX_VALUE   // so the third click isn't turned into a double-click
                } else
                    io.mouseClickedTime[i] = g.time
                io.mouseClickedPos[i] put io.mousePos
                io.mouseDragMaxDistanceSqr[i] = 0f
            } else if (io.mouseDown[i]) {
                val mouseDelta = io.mousePos - io.mouseClickedPos[i]
                io.mouseDragMaxDistanceAbs[i].x = max(io.mouseDragMaxDistanceAbs[i].x, if (mouseDelta.x < 0f) -mouseDelta.x else mouseDelta.x)
                io.mouseDragMaxDistanceAbs[i].y = max(io.mouseDragMaxDistanceAbs[i].y, if (mouseDelta.y < 0f) -mouseDelta.y else mouseDelta.y)
                io.mouseDragMaxDistanceSqr[i] = max(io.mouseDragMaxDistanceSqr[i], mouseDelta.lengthSqr)
            }
        }

        // Calculate frame-rate for the user, as a purely luxurious feature
        g.framerateSecPerFrameAccum += io.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
        g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = io.deltaTime
        g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
        io.framerate = 1.0f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size)

        // Handle user moving window with mouse (at the beginning of the frame to avoid input lag or sheering)
        updateMovingWindow()

        // Delay saving settings so we don't spam disk too much
        if (g.settingsDirtyTimer > 0.0f) {
            g.settingsDirtyTimer -= io.deltaTime
            if (g.settingsDirtyTimer <= 0.0f)
                saveIniSettingsToDisk(io.iniFilename)
        }

        /*  Find the window we are hovering
            - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
            - When moving a window we can skip the search, which also conveniently bypasses the fact that window.windowRectClipped
                is lagging as this point.
            - We also support the moved window toggling the NoInputs flag after moving has started in order to be able to detect
                windows below it, which is useful for e.g. docking mechanisms.  */
        g.hoveredWindow = if (g.movingWindow?.flags?.hasnt(Wf.NoInputs) == true) g.movingWindow else findHoveredWindow()
        g.hoveredRootWindow = g.hoveredWindow?.rootWindow

        val modalWindow = frontMostModalRootWindow
        if (modalWindow != null) {
            g.modalWindowDarkeningRatio = glm.min(g.modalWindowDarkeningRatio + io.deltaTime * 6f, 1f)
            g.hoveredRootWindow?.let {
                if (!it.isChildOf(modalWindow)) {
                    g.hoveredWindow = null
                    g.hoveredRootWindow = null
                }
            }
        } else g.modalWindowDarkeningRatio = 0f

        /*  Update the WantCaptureMouse/WantCaptureKeyboard flags, so user can capture/discard the inputs away from
            the rest of their application.
            When clicking outside of a window we assume the click is owned by the application and won't request capture.
            We need to track click ownership.   */
        var mouseEarliestButtonDown = -1
        var mouseAnyDown = false
        for (i in io.mouseDown.indices) {
            if (io.mouseClicked[i])
                io.mouseDownOwned[i] = g.hoveredWindow != null || g.openPopupStack.isNotEmpty()
            mouseAnyDown = mouseAnyDown || io.mouseDown[i]
            if (io.mouseDown[i])
                if (mouseEarliestButtonDown == -1 || io.mouseClickedTime[i] < io.mouseClickedTime[mouseEarliestButtonDown])
                    mouseEarliestButtonDown = i
        }
        val mouseAvailToImgui = mouseEarliestButtonDown == -1 || io.mouseDownOwned[mouseEarliestButtonDown]
        io.wantCaptureMouse =
                if (g.wantCaptureMouseNextFrame != -1) g.wantCaptureMouseNextFrame != 0
                else (mouseAvailToImgui && (g.hoveredWindow != null || mouseAnyDown)) || g.openPopupStack.isNotEmpty()
        io.wantCaptureKeyboard =
                if (g.wantCaptureKeyboardNextFrame != -1) g.wantCaptureKeyboardNextFrame != 0
                else g.activeId != 0 || modalWindow != null
        if (io.navActive && io.configFlags has Cf.NavEnableKeyboard && io.configFlags hasnt Cf.NavNoCaptureKeyboard)
            io.wantCaptureKeyboard = true

        io.wantTextInput = g.wantTextInputNextFrame != -1 && g.wantTextInputNextFrame != 0
        g.mouseCursor = MouseCursor.Arrow
        g.wantCaptureKeyboardNextFrame = -1
        g.wantCaptureMouseNextFrame = -1
        g.osImePosRequest put 1f // OS Input Method Editor showing on top-left of our window by default

        // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
        // FIXME: For patterns of drag and drop across OS windows, we may need to rework/remove this test (first committed 311c0ca9 on 2015/02)
        val mouseDraggingExternPayload = g.dragDropActive && g.dragDropSourceFlags has DragDropFlag.SourceExtern
        if (!mouseAvailToImgui && !mouseDraggingExternPayload) {
            g.hoveredRootWindow = null
            g.hoveredWindow = null
        }

        // Mouse wheel scrolling, scale
        if (g.hoveredWindow != null && !g.hoveredWindow!!.collapsed && (io.mouseWheel != 0f || io.mouseWheelH != 0f)) {
            /*  If a child window has the WindowFlag.NoScrollWithMouse flag, we give a chance to scroll its parent
                (unless either WindowFlag.NoInputs or WindowFlag.NoScrollbar are also set).             */
            val window = g.hoveredWindow!!
            var scrollWindow = window
            while (scrollWindow.flags has Wf.ChildWindow && scrollWindow.flags has Wf.NoScrollWithMouse &&
                    scrollWindow.flags hasnt Wf.NoScrollbar && scrollWindow.flags hasnt Wf.NoInputs && scrollWindow.parentWindow != null)
                scrollWindow.parentWindow?.let { scrollWindow = it }
            val scrollAllowed = scrollWindow.flags hasnt Wf.NoScrollWithMouse && scrollWindow.flags hasnt Wf.NoInputs

            if (io.mouseWheel != 0f)
                if (io.keyCtrl && io.fontAllowUserScaling) {
                    // Zoom / Scale window
                    val newFontScale = glm.clamp(window.fontWindowScale + io.mouseWheel * 0.1f, 0.5f, 2.5f)
                    val scale = newFontScale / window.fontWindowScale
                    window.fontWindowScale = newFontScale

                    val offset = window.size * (1f - scale) * (io.mousePos - window.pos) / window.size
                    with(window) {
                        pos plusAssign offset
                        posF plusAssign offset
                        size timesAssign scale
                        sizeFull timesAssign scale
                    }
                } else if (!io.keyCtrl && scrollAllowed) {
                    // Mouse wheel vertical scrolling
                    var scrollAmount = 5 * scrollWindow.calcFontSize()
                    scrollAmount = min(scrollAmount,
                            (scrollWindow.contentsRegionRect.height + scrollWindow.windowPadding.y * 2f) * 0.67f).i.f
                    scrollWindow.setScrollY(scrollWindow.scroll.y - io.mouseWheel * scrollAmount)
                }
            if (io.mouseWheelH != 0f && scrollAllowed) {
                // Mouse wheel horizontal scrolling (for hardware that supports it)
                val scrollAmount = scrollWindow.calcFontSize()
                if (!io.keyCtrl && window.flags hasnt Wf.NoScrollWithMouse)
                    window.setScrollX(window.scroll.x - io.mouseWheelH * scrollAmount)
            }
        }

        // Pressing TAB activate widget focus
        g.navWindow?.let {
            if (g.activeId == 0 && it.active && it.flags hasnt Wf.NoNavInputs && !io.keyCtrl && Key.Tab.isPressed(false)) {
                if (g.navId != 0 && g.navIdTabCounter != Int.MAX_VALUE)
                    it.focusIdxTabRequestNext = g.navIdTabCounter + 1 + if (io.keyShift) -1 else 1
                else it.focusIdxTabRequestNext = if (io.keyShift) -1 else 0
            }
        }
        g.navIdTabCounter = Int.MAX_VALUE

        // Mark all windows as not visible
        var i = 0
        while (i != g.windows.size) {
            val window = g.windows[i]
            window.wasActive = window.active
            window.active = false
            window.writeAccessed = false
            i++
        }

        // Closing the focused window restore focus to the first active root window in descending z-order
        if (g.navWindow != null && !g.navWindow!!.wasActive)
            focusFrontMostActiveWindow(null)

        /*  No window should be open at the beginning of the frame.
            But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an
            explicit clear. */
        g.currentWindowStack.clear()
        g.currentPopupStack.clear()
        closePopupsOverWindow(g.navWindow)

        // Create implicit window - we will only render it if the user has added something to it.
        setNextWindowSize(Vec2(400), Cond.FirstUseEver)
        begin("Debug##Default")
    }


    /** Ends the ImGui frame, finalize the draw data. (Obsolete: optionally call io.renderDrawListsFn if set.
     *  Nowadays, prefer calling your render function yourself.)   */
    fun render() {

        assert(g.initialized)   // Forgot to call ImGui::NewFrame()

        if (g.frameCountEnded != g.frameCount) endFrame()
        g.frameCountRendered = g.frameCount

        /*  Skip render altogether if alpha is 0.0
            Note that vertex buffers have been created and are wasted, so it is best practice that you don't create
            windows in the first place, or consistently respond to Begin() returning false. */
        if (style.alpha > 0f) {
            // Gather windows to render
            io.metricsActiveWindows = 0
            io.metricsRenderIndices = 0
            io.metricsRenderVertices = 0
            g.drawDataBuilder.clear()
            val windowToRenderFrontMost = g.navWindowingTarget.takeIf {
                it?.flags?.hasnt(Wf.NoBringToFrontOnFocus) ?: false
            }
            g.windows.filter { it.active && it.hiddenFrames <= 0 && it.flags hasnt Wf.ChildWindow && it !== windowToRenderFrontMost }
                    .map(Window::addToDrawDataSelectLayer)
            windowToRenderFrontMost?.let {
                if (it.active && it.hiddenFrames <= 0) // NavWindowingTarget is always temporarily displayed as the front-most window
                    it.addToDrawDataSelectLayer()
            }
            g.drawDataBuilder.flattenIntoSingleLayer()

            // Draw software mouse cursor if requested
            val offset = Vec2()
            val size = Vec2()
            val uv = Array(4) { Vec2() }
            if (io.mouseDrawCursor && io.fonts.getMouseCursorTexData(g.mouseCursor, offset, size, uv)) {
                val pos = io.mousePos - offset
                val texId = io.fonts.texId
                val sc = style.mouseCursorScale
                g.overlayDrawList.pushTextureId(texId)
                g.overlayDrawList.addImage(texId, pos + Vec2(1, 0) * sc, pos + Vec2(1, 0) * sc + size * sc, uv[2], uv[3], COL32(0, 0, 0, 48))        // Shadow
                g.overlayDrawList.addImage(texId, pos + Vec2(2, 0) * sc, pos + Vec2(2, 0) * sc + size * sc, uv[2], uv[3], COL32(0, 0, 0, 48))        // Shadow
                g.overlayDrawList.addImage(texId, pos, pos + size * sc, uv[2], uv[3], COL32(0, 0, 0, 255))       // Black border
                g.overlayDrawList.addImage(texId, pos, pos + size * sc, uv[0], uv[1], COL32(255, 255, 255, 255)) // White fill
                g.overlayDrawList.popTextureId()
            }
            if (g.overlayDrawList.vtxBuffer.isNotEmpty())
                g.overlayDrawList addTo g.drawDataBuilder.layers[0]

            // Setup ImDrawData structure for end-user
            setupDrawData(g.drawDataBuilder.layers[0], g.drawData)
            io.metricsRenderVertices = g.drawData.totalVtxCount
            io.metricsRenderIndices = g.drawData.totalIdxCount
        }
    }

    /** Same value as passed to the old io.renderDrawListsFn function. Valid after ::render() and until the next call to
     *  ::newFrame()   */
    val drawData get() = g.drawData.takeIf { it.valid }

    /** Ends the ImGui frame. Automatically called by Render()! you most likely don't need to ever call that yourself
     *  directly. If you don't need to render you can call EndFrame() but you'll have wasted CPU already. If you don't
     *  need to render, don't create any windows instead!
     *
     *  This is normally called by Render(). You may want to call it directly if you want to avoid calling Render() but
     *  the gain will be very minimal.  */
    fun endFrame() {

        assert(g.initialized)                       // Forgot to call newFrame()
        if (g.frameCountEnded == g.frameCount) return   // Don't process endFrame() multiple times.

        // Notify OS when our Input Method Editor cursor has moved (e.g. CJK inputs using Microsoft IME)
        if (/*io.imeSetInputScreenPosFn &&*/ (g.osImePosRequest - g.osImePosSet).lengthSqr > 0.0001f) {
//            (LwjglGL3.windowProc!! as WindowProc).in
//            g.io.ImeSetInputScreenPosFn((int) g . OsImePosRequest . x, (int) g . OsImePosRequest . y)
            g.osImePosSet put g.osImePosRequest
        }

        // Hide implicit "Debug" window if it hasn't been used
// TODO        assert(g.currentWindowStack.size == 1)    // Mismatched Begin()/End() calls
        g.currentWindow?.let {
            if (!it.writeAccessed) it.active = false
        }

        end()

        if (g.activeId == 0 && g.hoveredId == 0)
            if (g.navWindow == null || !g.navWindow!!.appearing) { // Unless we just made a window/popup appear
                // Click to focus window and start moving (after we're done with all our widgets)
                if (io.mouseClicked[0])
                    if (g.hoveredRootWindow != null) {
                        /*  Set ActiveId even if the _NoMove flag is set, without it dragging away from a window
                            with _NoMove would activate hover on other windows.                         */
                        g.hoveredWindow.focus()
                        setActiveId(g.hoveredWindow!!.moveId, g.hoveredWindow)
                        g.navDisableHighlight = true
                        g.activeIdClickOffset = io.mousePos - g.hoveredRootWindow!!.pos
                        if (g.hoveredWindow!!.flags hasnt Wf.NoMove && g.hoveredRootWindow!!.flags hasnt Wf.NoMove)
                            g.movingWindow = g.hoveredWindow
                    } else if (g.navWindow != null && frontMostModalRootWindow == null)
                        null.focus()   // Clicking on void disable focus

                /*  With right mouse button we close popups without changing focus
                (The left mouse button path calls FocusWindow which will lead NewFrame::closePopupsOverWindow to trigger) */
                if (io.mouseClicked[1]) {
                    /*  Find the top-most window between HoveredWindow and the front most Modal Window.
                        This is where we can trim the popup stack.  */
                    val modal = frontMostModalRootWindow
                    var hoveredWindowAboveModal = false
                    if (modal == null)
                        hoveredWindowAboveModal = true
                    var i = g.windows.lastIndex
                    while (i >= 0 && !hoveredWindowAboveModal) {
                        val window = g.windows[i]
                        if (window === modal) break
                        if (window === g.hoveredWindow) hoveredWindowAboveModal = true
                        i--
                    }
                    closePopupsOverWindow(if (hoveredWindowAboveModal) g.hoveredWindow else modal)
                }
            }

        /*  Sort the window list so that all child windows are after their parent
            We cannot do that on FocusWindow() because childs may not exist yet         */
        g.windowsSortBuffer.clear()
        g.windows.forEach {
            if (!it.active || it.flags hasnt Wf.ChildWindow)  // if a child is active its parent will add it
                it.addToSortedBuffer()
        }
        assert(g.windows.size == g.windowsSortBuffer.size)  // we done something wrong
        g.windows.clear()
        g.windows.addAll(g.windowsSortBuffer)

        // Clear Input data for next frame
        io.mouseWheel = 0f
        io.mouseWheelH = 0f
        io.inputCharacters.fill(NUL)
        io.navInputs.fill(0f)

        g.frameCountEnded = g.frameCount
    }

    companion object {

        fun updateMovingWindow() {

            val mov = g.movingWindow
            if (mov != null && mov.moveId == g.activeId && g.activeIdSource == InputSource.Mouse) {
                /*  We actually want to move the root window. g.movingWindow === window we clicked on
                    (could be a child window).
                    We track it to preserve Focus and so that activeIdWindow === movingWindow and
                    activeId == movingWindow.moveId for consistency.    */
                keepAliveId(g.activeId)
                assert(mov.rootWindow != null)
                val movingWindow = mov.rootWindow!!
                if (io.mouseDown[0]) {
                    val pos = io.mousePos - g.activeIdClickOffset
                    if (movingWindow.posF != pos) {
                        markIniSettingsDirty(movingWindow)
                        movingWindow.posF put pos
                    }
                    mov.focus()
                } else {
                    clearActiveId()
                    g.movingWindow = null
                }
            } else {
                /*  When clicking/dragging from a window that has the _NoMove flag, we still set the ActiveId in order
                    to prevent hovering others.                 */
                if (g.activeIdWindow?.moveId == g.activeId) {
                    keepAliveId(g.activeId)
                    if (!io.mouseDown[0])
                        clearActiveId()
                }
                g.movingWindow = null
            }
        }

        /** Handle resize for: Resize Grips, Borders, Gamepad
         * @return borderHelf   */
        fun updateManualResize(window: Window, sizeAutoFit: Vec2, borderHeld: Int, resizeGripCount: Int, resizeGripCol: IntArray): Int {

            var borderHeld = borderHeld

            val flags = window.flags
            if (flags has Wf.NoResize || flags has Wf.AlwaysAutoResize || window.autoFitFrames.x > 0 || window.autoFitFrames.y > 0)
                return borderHeld

            val resizeBorderCount = if (flags has Wf.ResizeFromAnySide) 4 else 0
            val gripDrawSize = max(g.fontSize * 1.35f, window.windowRounding + 1f + g.fontSize * 0.2f).i.f
            val gripHoverSize = (gripDrawSize * 0.75f).i.f

            val posTarget = Vec2(Float.MAX_VALUE)
            val sizeTarget = Vec2(Float.MAX_VALUE)

            // Manual resize grips
            pushId("#RESIZE")
            for (resizeGripN in 0 until resizeGripCount) {
                val grip = resizeGripDef[resizeGripN]
                val corner = window.pos.lerp(window.pos + window.size, grip.cornerPos)

                // Using the FlattenChilds button flag we make the resize button accessible even if we are hovering over a child window
                val resizeRect = Rect(corner, corner + grip.innerDir * gripHoverSize)
                resizeRect.fixInverted()

                val flags = ButtonFlag.FlattenChildren or ButtonFlag.NoNavFocus
                val (_, hovered, held) = buttonBehavior(resizeRect, window.getId(resizeGripN), flags)
                if (hovered || held)
                    g.mouseCursor = if (resizeGripN has 1) MouseCursor.ResizeNESW else MouseCursor.ResizeNWSE

                if (g.hoveredWindow === window && held && g.io.mouseDoubleClicked[0] && resizeGripN == 0) {
                    // Manual auto-fit when double-clicking
                    sizeTarget put window.calcSizeAfterConstraint(sizeAutoFit)
                    clearActiveId()
                } else if (held) {
                    // Resize from any of the four corners
                    // We don't use an incremental MouseDelta but rather compute an absolute target size based on mouse position
                    val cornerTarget = g.io.mousePos - g.activeIdClickOffset + resizeRect.size * grip.cornerPos // Corner of the window corresponding to our corner grip
                    window.calcResizePosSizeFromAnyCorner(cornerTarget, grip.cornerPos, posTarget, sizeTarget)
                }
                if (resizeGripN == 0 || held || hovered)
                    resizeGripCol[resizeGripN] = (if (held) Col.ResizeGripActive else if (hovered) Col.ResizeGripHovered else Col.ResizeGrip).u32
            }
            for (borderN in 0 until resizeBorderCount) {
                val BORDER_SIZE = 5f          // FIXME: Only works _inside_ window because of HoveredWindow check.
                val BORDER_APPEAR_TIMER = 0.05f // Reduce visual noise
                val borderRect = window.getBorderRect(borderN, gripHoverSize, BORDER_SIZE)
                val (_, hovered, held) = buttonBehavior(borderRect, window.getId((borderN + 4)), ButtonFlag.FlattenChildren)
                if ((hovered && g.hoveredIdTimer > BORDER_APPEAR_TIMER) || held) {
                    g.mouseCursor = if (borderN has 1) MouseCursor.ResizeEW else MouseCursor.ResizeNS
                    if (held) borderHeld = borderN
                }
                if (held) {
                    val borderTarget = Vec2(window.pos)
                    val borderPosN = when (borderN) {
                        0 -> {
                            borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y
                            Vec2(0, 0)
                        }
                        1 -> {
                            borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + BORDER_SIZE
                            Vec2(1, 0)
                        }
                        2 -> {
                            borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + BORDER_SIZE
                            Vec2(0, 1)
                        }
                        3 -> {
                            borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x
                            Vec2(0, 0)
                        }
                        else -> Vec2(0, 0)
                    }
                    window.calcResizePosSizeFromAnyCorner(borderTarget, borderPosN, posTarget, sizeTarget)
                }
            }
            popId()

            // Navigation/gamepad resize
            if (g.navWindowingTarget === window) {
                val navResizeDelta = Vec2()
                if (g.navWindowingInputSource == InputSource.NavKeyboard && g.io.keyShift)
                    navResizeDelta put getNavInputAmount2d(NavDirSourceFlag.Keyboard.i, InputReadMode.Down)
                if (g.navWindowingInputSource == InputSource.NavGamepad)
                    navResizeDelta put getNavInputAmount2d(NavDirSourceFlag.PadDPad.i, InputReadMode.Down)
                if (navResizeDelta.x != 0f || navResizeDelta.y != 0f) {
                    val NAV_RESIZE_SPEED = 600f
                    navResizeDelta *= glm.floor(NAV_RESIZE_SPEED * g.io.deltaTime * min(g.io.displayFramebufferScale.x, g.io.displayFramebufferScale.y))
                    g.navWindowingToggleLayer = false
                    g.navDisableMouseHover = true
                    resizeGripCol[0] = Col.ResizeGripActive.u32
                    // FIXME-NAV: Should store and accumulate into a separate size buffer to handle sizing constraints properly, right now a constraint will make us stuck.
                    sizeTarget put window.calcSizeAfterConstraint(window.sizeFull + navResizeDelta)
                }
            }

            // Apply back modified position/size to window
            if (sizeTarget.x != Float.MAX_VALUE) {
                window.sizeFull put sizeTarget
                markIniSettingsDirty(window)
            }
            if (posTarget.x != Float.MAX_VALUE) {
                window.posF = glm.floor(posTarget)
                window.pos put window.posF
                markIniSettingsDirty(window)
            }

            window.size put window.sizeFull

            return borderHeld
        }

        class ResizeGripDef(val cornerPos: Vec2, val innerDir: Vec2, val angleMin12: Int, val angleMax12: Int)

        val resizeGripDef = arrayOf(
                ResizeGripDef(Vec2(1, 1), Vec2(-1, -1), 0, 3),  // Lower right
                ResizeGripDef(Vec2(0, 1), Vec2(+1, -1), 3, 6),  // Lower left
                ResizeGripDef(Vec2(0, 0), Vec2(+1, +1), 6, 9),  // Upper left
                ResizeGripDef(Vec2(1, 0), Vec2(-1, +1), 9, 12)) // Upper right

        fun focusFrontMostActiveWindow(ignoreWindow: Window?) {
            for (i in g.windows.lastIndex downTo 0)
                if (g.windows[i] !== ignoreWindow && g.windows[i].wasActive && g.windows[i].flags hasnt Wf.ChildWindow) {
                    val focusWindow = navRestoreLastChildNavWindow(g.windows[i])
                    focusWindow.focus()
                    return
                }
        }

        fun setupDrawData(drawLists: ArrayList<DrawList>, outDrawData: DrawData) = with(outDrawData) {
            valid = true
            cmdLists.clear()
            if (drawLists.isNotEmpty())
                cmdLists += drawLists
            cmdListsCount = drawLists.size
            outDrawData.totalIdxCount = 0
            totalVtxCount = 0
            for (n in 0 until drawLists.size) {
                totalVtxCount += drawLists[n].vtxBuffer.size
                totalIdxCount += drawLists[n].idxBuffer.size
            }
        }
    }
}