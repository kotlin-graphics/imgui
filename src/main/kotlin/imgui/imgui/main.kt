package imgui.imgui

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.buttonBehavior
import imgui.ImGui.clearActiveId
import imgui.ImGui.clearDragDrop
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.defaultFont
import imgui.ImGui.end
import imgui.ImGui.frontMostPopupModal
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isMouseDown
import imgui.ImGui.isMousePosValid
import imgui.ImGui.keepAliveId
import imgui.ImGui.popId
import imgui.ImGui.pushId
import imgui.ImGui.setCurrentFont
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setTooltip
import imgui.ImGui.updateHoveredWindowAndCaptureFlags
import imgui.ImGui.updateMouseMovingWindow
import imgui.internal.*
import kotlin.math.max
import kotlin.math.min
import imgui.ConfigFlag as Cf
import imgui.WindowFlag as Wf
import imgui.internal.DrawListFlag as Dlf

@Suppress("UNCHECKED_CAST")

interface imgui_main {

    /** access the IO structure (mouse/keyboard/gamepad inputs, time, various configuration options/flags) */
    val io
        get() = gImGui?.io
                ?: throw Error("No current context. Did you call ::Context() or Context::setCurrent()?")

    /** access the Style structure (colors, sizes). Always use PushStyleCol(), PushStyleVar() to modify style mid-frame. */
    val style
        get() = gImGui?.style
                ?: throw Error("No current context. Did you call ::Context() or Context::setCurrent()?")

    /** start a new ImGui frame, you can submit any command from this point until NewFrame()/Render().  */
    fun newFrame() {

        ptrIndices = 0

        assert(gImGui != null) { "No current context. Did you call ImGui::CreateContext() or ImGui::SetCurrentContext()?" }

        if (IMGUI_ENABLE_TEST_ENGINE)
            ImGuiTestEngineHook_PreNewFrame()

        /*  Check user data
            (We pass an error message in the assert expression as a trick to get it visible to programmers who are not using a debugger,
            as most assert handlers display their argument)         */
        assert(g.initialized)
        assert(io.deltaTime >= 0f) { "Need a positive DeltaTime (zero is tolerated but will cause some timing issues)" }
        assert(io.displaySize.x >= 0f && io.displaySize.y >= 0f) { "Invalid DisplaySize value" }
        assert(io.fonts.fonts.isNotEmpty()) { "Font Atlas not built. Did you call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8() ?" }
        assert(io.fonts.fonts[0].isLoaded) { "Font Atlas not built. Did you call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8() ?" }
        assert(style.curveTessellationTol > 0f) { "Invalid style setting" }
        assert(style.alpha in 0f..1f) { "Invalid style setting. Alpha cannot be negative (allows us to avoid a few clamps in color computations)" }
        assert(g.frameCount == 0 || g.frameCountEnded == g.frameCount) { "Forgot to call Render() or EndFrame() at the end of the previous frame?" }
        for (n in 0 until Key.COUNT)
            assert(io.keyMap[n] >= -1 && io.keyMap[n] < io.keysDown.size) { "io.KeyMap[] contains an out of bound value (need to be 0..512, or -1 for unmapped key)" }

        // Perform simple check: required key mapping (we intentionally do NOT check all keys to not pressure user into setting up everything, but Space is required and was only recently added in 1.60 WIP)
        if (io.configFlags has Cf.NavEnableKeyboard)
            assert(io.keyMap[Key.Space] != -1) { "ImGuiKey_Space is not mapped, required for keyboard navigation." }

        // Perform simple check: the beta io.configWindowsResizeFromEdges option requires back-end to honor mouse cursor changes and set the ImGuiBackendFlags_HasMouseCursors flag accordingly.
        if (io.configWindowsResizeFromEdges && io.backendFlags hasnt BackendFlag.HasMouseCursors)
            io.configWindowsResizeFromEdges = false

        // Load settings on first frame (if not explicitly loaded manually before)
        if (!g.settingsLoaded) {
            assert(g.settingsWindows.isEmpty())
            io.iniFilename?.let(::loadIniSettingsFromDisk)
            g.settingsLoaded = true
        }

        // Save settings (with a delay so we don't spam disk too much)
        if (g.settingsDirtyTimer > 0f) {
            g.settingsDirtyTimer -= io.deltaTime
            if (g.settingsDirtyTimer <= 0f) {
                val ini = io.iniFilename
                if (ini != null)
                    saveIniSettingsToDisk(ini)
                else
                    io.wantSaveIniSettings = true  // Let user know they can call SaveIniSettingsToMemory(). user will need to clear io.WantSaveIniSettings themselves.
                g.settingsDirtyTimer = 0f
            }
        }

        g.time += io.deltaTime
        g.frameScopeActive = true
        g.frameCount += 1
        g.tooltipOverrideCount = 0
        g.windowsActiveCount = 0

        // Setup current font and draw list
        io.fonts.locked = true
        setCurrentFont(defaultFont)
        assert(g.font.isLoaded)
        g.drawListSharedData.clipRectFullscreen = Vec4(0f, 0f, io.displaySize.x, io.displaySize.y)
        g.drawListSharedData.curveTessellationTol = style.curveTessellationTol

        g.overlayDrawList.clear()
        g.overlayDrawList.pushTextureId(io.fonts.texId)
        g.overlayDrawList.pushClipRectFullScreen()
        g.overlayDrawList.flags = (if (style.antiAliasedLines) Dlf.AntiAliasedLines.i else 0) or if (style.antiAliasedFill) Dlf.AntiAliasedFill.i else 0

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it
        g.drawData.clear()

        // Drag and drop keep the source ID alive so even if the source disappear our state is consistent
        if (g.dragDropActive && g.dragDropPayload.sourceId == g.activeId)
            keepAliveId(g.dragDropPayload.sourceId)

        // Clear reference to active widget if the widget isn't alive anymore
        if (g.hoveredIdPreviousFrame == 0)
            g.hoveredIdTimer = 0f
        if (g.hoveredIdPreviousFrame == 0 || (g.hoveredId != 0 && g.activeId == g.hoveredId))
            g.hoveredIdNotActiveTimer = 0f
        if (g.hoveredId != 0)
            g.hoveredIdTimer += io.deltaTime
        if (g.hoveredId != 0 && g.activeId != g.hoveredId)
            g.hoveredIdNotActiveTimer += io.deltaTime
        g.hoveredIdPreviousFrame = g.hoveredId
        g.hoveredId = 0
        g.hoveredIdAllowOverlap = false
        if (g.activeIdIsAlive != g.activeId && g.activeIdPreviousFrame == g.activeId && g.activeId != 0)
            clearActiveId()
        if (g.activeId != 0)
            g.activeIdTimer += io.deltaTime
        g.lastActiveIdTimer += io.deltaTime
        g.activeIdPreviousFrame = g.activeId
        g.activeIdPreviousFrameWindow = g.activeIdWindow
        g.activeIdPreviousFrameHasBeenEdited = g.activeIdHasBeenEdited
        g.activeIdIsAlive = 0
        g.activeIdPreviousFrameIsAlive = false
        g.activeIdIsJustActivated = false
        if (g.scalarAsInputTextId != 0 && g.activeId != g.scalarAsInputTextId)
            g.scalarAsInputTextId = 0

        // Drag and drop
        g.dragDropAcceptIdPrev = g.dragDropAcceptIdCurr
        g.dragDropAcceptIdCurr = 0
        g.dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE

        // Update keyboard input state
        for (i in io.keysDownDuration.indices)
            io.keysDownDurationPrev[i] = io.keysDownDuration[i]
        for (i in io.keysDown.indices)
            io.keysDownDuration[i] = when {
                io.keysDown[i] -> when {
                    io.keysDownDuration[i] < 0f -> 0f
                    else -> io.keysDownDuration[i] + io.deltaTime
                }
                else -> -1f
            }
        // Update gamepad/keyboard directional navigation
        navUpdate()

        // Update mouse input state
        updateMouseInputs()

        // Calculate frame-rate for the user, as a purely luxurious feature
        g.framerateSecPerFrameAccum += io.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
        g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = io.deltaTime
        g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
        io.framerate = when {
            g.framerateSecPerFrameAccum > 0f -> 1f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size)
            else -> Float.MAX_VALUE
        }

        // Handle user moving window with mouse (at the beginning of the frame to avoid input lag or sheering)
        updateMouseMovingWindow()
        updateHoveredWindowAndCaptureFlags()

        // Background darkening/whitening
        g.dimBgRatio = when {
            frontMostPopupModal != null || (g.navWindowingTarget != null && g.navWindowingHighlightAlpha > 0f) -> (g.dimBgRatio + io.deltaTime * 6f) min 1f
            else -> (g.dimBgRatio - io.deltaTime * 10f) max 0f
        }
        g.mouseCursor = MouseCursor.Arrow
        g.wantTextInputNextFrame = -1
        g.wantCaptureKeyboardNextFrame = -1
        g.wantCaptureMouseNextFrame = -1
        g.platformImePos put 1f // OS Input Method Editor showing on top-left of our window by default

        // Mouse wheel scrolling, scale
        updateMouseWheel()

        // Pressing TAB activate widget focus
        if (g.activeId == 0)
            g.navWindow?.let {
                if (it.active && it.flags hasnt Wf.NoNavInputs && !io.keyCtrl && Key.Tab.isPressed(false))
                    if (g.navId != 0 && g.navIdTabCounter != Int.MAX_VALUE)
                        it.focusIdxTabRequestNext = g.navIdTabCounter + 1 + if (io.keyShift) -1 else 1
                    else
                        it.focusIdxTabRequestNext = if (io.keyShift) -1 else 0
            }
        g.navIdTabCounter = Int.MAX_VALUE

        // Mark all windows as not visible
        assert(g.windowsFocusOrder.size == g.windows.size)
        g.windows.forEach {
            it.wasActive = it.active
            it.active = false
            it.writeAccessed = false
        }

        // Closing the focused window restore focus to the first active root window in descending z-order
        if (g.navWindow?.wasActive == false)
            focusPreviousWindowIgnoringOne(null)

        // No window should be open at the beginning of the frame.
        // But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an explicit clear.
        g.currentWindowStack.clear()
        g.beginPopupStack.clear()
        closePopupsOverWindow(g.navWindow)

        /*  Create implicit/fallback window - which we will only render it if the user has added something to it.
            We don't use "Debug" to avoid colliding with user trying to create a "Debug" window with custom flags.
            This fallback is particularly important as it avoid ImGui:: calls from crashing.
         */
        setNextWindowSize(Vec2(400), Cond.FirstUseEver)
        begin("Debug##Default")

        if (IMGUI_ENABLE_TEST_ENGINE)
            ImGuiTestEngineHook_PostNewFrame()
    }

    /** Ends the ImGui frame. automatically called by ::render(), you likely don't need to call that yourself directly.
     *  If you don't need to render data (skipping rendering) you may call ::endFrame() but you'll have wasted CPU already!
     *  If you don't need to render, better to not create any imgui windows and not call ::newFrame() at all!  */
    fun endFrame() {

        assert(g.initialized)
        if (g.frameCountEnded == g.frameCount) return   // Don't process endFrame() multiple times.
        assert(g.frameScopeActive) { "Forgot to call ImGui::newFrame()" }

        // Notify OS when our Input Method Editor cursor has moved (e.g. CJK inputs using Microsoft IME)
        if (io.imeSetInputScreenPosFn != null && (g.platformImeLastPos - g.platformImePos).lengthSqr > 0.0001f) {
            println("in (${g.platformImePos.x}, ${g.platformImePos.y}) (${g.platformImeLastPos.x}, ${g.platformImeLastPos.y})")
//            io.imeSetInputScreenPosFn!!(g.platformImePos.x.i, g.platformImePos.y.i)
            io.imeSetInputScreenPosFn!!(1000, 1000)
            g.platformImeLastPos put g.platformImePos
        }

        // Hide implicit "Debug" window if it hasn't been used
        assert(g.currentWindowStack.size == 1) { "Mismatched Begin()/End() calls, did you forget to call end on g.currentWindow.name?" }
        g.currentWindow?.let {
            if (!it.writeAccessed) it.active = false
        }

        end()

        // Show CTRL+TAB list
        if (g.navWindowingTarget != null)
            navUpdateWindowingList()

        // Drag and Drop: Elapse payload (if delivered, or if source stops being submitted)
        if (g.dragDropActive) {
            val isDelivered = g.dragDropPayload.delivery
            val isElapsed = g.dragDropPayload.dataFrameCount + 1 < g.frameCount && (g.dragDropSourceFlags has DragDropFlag.SourceAutoExpirePayload || !isMouseDown(g.dragDropMouseButton))
            if (isDelivered || isElapsed)
                clearDragDrop()
        }

        // Drag and Drop: Fallback for source tooltip. This is not ideal but better than nothing.
        if (g.dragDropActive && g.dragDropSourceFrameCount < g.frameCount) {
            g.dragDropWithinSourceOrTarget = true
            setTooltip("...")
            g.dragDropWithinSourceOrTarget = false
        }

        // Initiate moving window
        if (g.activeId == 0 && g.hoveredId == 0)
            if (g.navWindow == null || !g.navWindow!!.appearing) { // Unless we just made a window/popup appear
                // Click to focus window and start moving (after we're done with all our widgets)
                if (io.mouseClicked[0])
                    g.hoveredRootWindow?.let { hoveredRoot ->
                        g.hoveredWindow!!.startMouseMoving()
                        if (io.configWindowsMoveFromTitleBarOnly && hoveredRoot.flags hasnt Wf.NoTitleBar)
                        if (io.mouseClickedPos[0] !in hoveredRoot.titleBarRect())
                                g.movingWindow = null
                    } ?: if (g.navWindow != null && frontMostPopupModal == null)
                        null.focus()   // Clicking on void disable focus

                /*  With right mouse button we close popups without changing focus
                (The left mouse button path calls FocusWindow which will lead NewFrame::closePopupsOverWindow to trigger) */
                if (io.mouseClicked[1]) {
                    /*  Find the top-most window between HoveredWindow and the front most Modal Window.
                        This is where we can trim the popup stack.  */
                    val modal = frontMostPopupModal
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
        g.windowsSortBuffer.ensureCapacity(g.windows.size)
        g.windows.filter { !it.active || it.flags hasnt Wf.ChildWindow }  // if a child is active its parent will add it
                .forEach { it addToSortBuffer g.windowsSortBuffer }
        assert(g.windows.size == g.windowsSortBuffer.size) { "we done something wrong" }
        g.windows.clear()
        g.windows += g.windowsSortBuffer
        io.metricsActiveWindows = g.windowsActiveCount

        // Unlock font atlas
        io.fonts.locked = false

        // Clear Input data for next frame
        io.mouseWheel = 0f
        io.mouseWheelH = 0f
        io.inputCharacters.fill(NUL)
        io.navInputs.fill(0f)

        g.frameScopeActive = false
        g.frameCountEnded = g.frameCount
    }


    /** Ends the ImGui frame, finalize the draw data. (Obsolete: optionally call io.renderDrawListsFn if set.
     *  Nowadays, prefer calling your render function yourself.)   */
    fun render() {

        assert(g.initialized)

        if (g.frameCountEnded != g.frameCount) endFrame()
        g.frameCountRendered = g.frameCount

        // Gather DrawList to render (for each active window)
        io.metricsRenderWindows = 0
        io.metricsRenderIndices = 0
        io.metricsRenderVertices = 0
        g.drawDataBuilder.clear()
        val windowsToRenderFrontMost = arrayOf(
                g.navWindowingTarget?.rootWindow?.takeIf { it.flags has Wf.NoBringToFrontOnFocus },
                g.navWindowingList.getOrNull(0).takeIf { g.navWindowingTarget != null })
        g.windows
                .filter { it.isActiveAndVisible && it.flags hasnt Wf.ChildWindow && it !== windowsToRenderFrontMost[0] && it !== windowsToRenderFrontMost[1] }
                .forEach { it.addToDrawDataSelectLayer() }
        windowsToRenderFrontMost
                .filterNotNull()
                .filter { it.isActiveAndVisible } // NavWindowingTarget is always temporarily displayed as the front-most window
                .forEach { it.addToDrawDataSelectLayer() }
        g.drawDataBuilder.flattenIntoSingleLayer()

        // Draw software mouse cursor if requested
        val offset = Vec2()
        val size = Vec2()
        val uv = Array(4) { Vec2() }
        if (io.mouseDrawCursor && io.fonts.getMouseCursorTexData(g.mouseCursor, offset, size, uv)) {
            val pos = io.mousePos - offset
            val texId = io.fonts.texId
            val sc = style.mouseCursorScale
            g.overlayDrawList.apply {
                pushTextureId(texId)
                addImage(texId, pos + Vec2(1, 0) * sc, pos + Vec2(1, 0) * sc + size * sc, uv[2], uv[3], COL32(0, 0, 0, 48))        // Shadow
                addImage(texId, pos + Vec2(2, 0) * sc, pos + Vec2(2, 0) * sc + size * sc, uv[2], uv[3], COL32(0, 0, 0, 48))        // Shadow
                addImage(texId, pos, pos + size * sc, uv[2], uv[3], COL32(0, 0, 0, 255))       // Black border
                addImage(texId, pos, pos + size * sc, uv[0], uv[1], COL32(255, 255, 255, 255)) // White fill
                popTextureId()
            }
        }
        if (g.overlayDrawList.vtxBuffer.isNotEmpty())
            g.overlayDrawList addTo g.drawDataBuilder.layers[0]

        // Setup ImDrawData structure for end-user
        g.drawData setup g.drawDataBuilder.layers[0]
        io.metricsRenderVertices = g.drawData.totalVtxCount
        io.metricsRenderIndices = g.drawData.totalIdxCount
    }

    /** Same value as passed to the old io.renderDrawListsFn function. Valid after ::render() and until the next call to
     *  ::newFrame()   */
    val drawData get() = g.drawData.takeIf { it.valid }

    companion object {

        // Misc
        fun updateMouseInputs() {

            with(io) {

                // Round mouse position to avoid spreading non-rounded position (e.g. UpdateManualResize doesn't support them well)
                if (isMousePosValid(mousePos)) {
                    g.lastValidMousePos = glm.floor(mousePos)
                    mousePos = Vec2(g.lastValidMousePos)
                }

                // If mouse just appeared or disappeared (usually denoted by -FLT_MAX component) we cancel out movement in MouseDelta
                if (isMousePosValid(mousePos) && isMousePosValid(mousePosPrev))
                    mouseDelta = mousePos - mousePosPrev
                else
                    mouseDelta put 0f
                if (mouseDelta.x != 0f || mouseDelta.y != 0f)
                    g.navDisableMouseHover = false

                mousePosPrev put mousePos
                for (i in mouseDown.indices) {
                    mouseClicked[i] = mouseDown[i] && mouseDownDuration[i] < 0f
                    mouseReleased[i] = !mouseDown[i] && mouseDownDuration[i] >= 0f
                    mouseDownDurationPrev[i] = mouseDownDuration[i]
                    mouseDownDuration[i] = when {
                        mouseDown[i] -> when {
                            mouseDownDuration[i] < 0f -> 0f
                            else -> mouseDownDuration[i] + deltaTime
                        }
                        else -> -1f
                    }
                    mouseDoubleClicked[i] = false
                    if (mouseClicked[i]) {
                        if (g.time - mouseClickedTime[i] < mouseDoubleClickTime) { // TODO check if ok or (g.time - mouseClickedTime[i]).f
                            val deltaFromClickPos = when {
                                isMousePosValid(io.mousePos) -> io.mousePos - io.mouseClickedPos[i]
                                else -> Vec2()
                            }
                            if (deltaFromClickPos.lengthSqr < io.mouseDoubleClickMaxDist * io.mouseDoubleClickMaxDist)
                                mouseDoubleClicked[i] = true
                            mouseClickedTime[i] = -Double.MAX_VALUE    // so the third click isn't turned into a double-click
                        } else
                            mouseClickedTime[i] = g.time
                        mouseClickedPos[i] put mousePos
                        mouseDragMaxDistanceAbs[i] put 0f
                        mouseDragMaxDistanceSqr[i] = 0f
                    } else if (mouseDown[i]) {
                        // Maintain the maximum distance we reaching from the initial click position, which is used with dragging threshold
                        val deltaFromClickPos = when {
                            isMousePosValid(io.mousePos) -> io.mousePos - io.mouseClickedPos[i]
                            else -> Vec2()
                        }
                        io.mouseDragMaxDistanceSqr[i] = io.mouseDragMaxDistanceSqr[i] max deltaFromClickPos.lengthSqr
                        io.mouseDragMaxDistanceAbs[i].x = io.mouseDragMaxDistanceAbs[i].x max when {
                            deltaFromClickPos.x < 0f -> -deltaFromClickPos.x
                            else -> deltaFromClickPos.x
                        }
                        io.mouseDragMaxDistanceAbs[i].y = io.mouseDragMaxDistanceAbs[i].y max when {
                            deltaFromClickPos.y < 0f -> -deltaFromClickPos.y
                            else -> deltaFromClickPos.y
                        }
                        val mouseDelta = mousePos - mouseClickedPos[i]
                        mouseDragMaxDistanceAbs[i].x = mouseDragMaxDistanceAbs[i].x max if (mouseDelta.x < 0f) -mouseDelta.x else mouseDelta.x
                        mouseDragMaxDistanceAbs[i].y = mouseDragMaxDistanceAbs[i].y max if (mouseDelta.y < 0f) -mouseDelta.y else mouseDelta.y
                        mouseDragMaxDistanceSqr[i] = mouseDragMaxDistanceSqr[i] max mouseDelta.lengthSqr
                    }
                    // Clicking any mouse button reactivate mouse hovering which may have been deactivated by gamepad/keyboard navigation
                    if (mouseClicked[i])
                        g.navDisableMouseHover = false
                }
            }
        }

        fun updateMouseWheel() {
            val window = g.hoveredWindow
            if (window == null || window.collapsed) return
            if (io.mouseWheel == 0f && io.mouseWheelH == 0f) return

            // If a child window has the Wf.NoScrollWithMouse flag, we give a chance to scroll its parent (unless either Wf.NoInputs or Wf.NoScrollbar are also set).
            var scrollWindow: Window = window
            while (scrollWindow.flags has Wf.ChildWindow && scrollWindow.flags has Wf.NoScrollWithMouse &&
                    scrollWindow.flags hasnt Wf.NoScrollbar && scrollWindow.flags hasnt Wf.NoMouseInputs && scrollWindow.parentWindow != null)
                scrollWindow = scrollWindow.parentWindow!!
            val scrollAllowed = scrollWindow.flags hasnt Wf.NoScrollWithMouse && scrollWindow.flags hasnt Wf.NoMouseInputs

            if (io.mouseWheel != 0f)
                if (io.keyCtrl && io.fontAllowUserScaling) {
                    // Zoom / Scale window
                    val newFontScale = glm.clamp(window.fontWindowScale + io.mouseWheel * 0.1f, 0.5f, 2.5f)
                    val scale = newFontScale / window.fontWindowScale
                    window.fontWindowScale = newFontScale

                    val offset = window.size * (1f - scale) * (io.mousePos - window.pos) / window.size
                    window.pos plusAssign offset
                    window.size timesAssign scale
                    window.sizeFull timesAssign scale
                } else if (!io.keyCtrl && scrollAllowed) {
                    // Mouse wheel vertical scrolling
                    var scrollAmount = 5 * scrollWindow.calcFontSize()
                    scrollAmount = min(scrollAmount, (scrollWindow.contentsRegionRect.height + scrollWindow.windowPadding.y * 2f) * 0.67f).i.f
                    scrollWindow.setScrollY(scrollWindow.scroll.y - io.mouseWheel * scrollAmount)
                }
            if (io.mouseWheelH != 0f && scrollAllowed && !io.keyCtrl) {
                // Mouse wheel horizontal scrolling (for hardware that supports it)
                val scrollAmount = scrollWindow.calcFontSize()
                scrollWindow.setScrollX(scrollWindow.scroll.x - io.mouseWheelH * scrollAmount)
            }
        }

        /** Handle resize for: Resize Grips, Borders, Gamepad
         * @return borderHelf   */
        fun updateManualResize(window: Window, sizeAutoFit: Vec2, borderHeld_: Int, resizeGripCount: Int, resizeGripCol: IntArray) {

            var borderHeld = borderHeld_

            val flags = window.flags
            if (flags has Wf.NoResize || flags has Wf.AlwaysAutoResize || window.autoFitFrames anyGreaterThan 0)
                return
            if (window.wasActive == false) // Early out to avoid running this code for e.g. an hidden implicit/fallback Debug window.
                return

            val resizeBorderCount = if (io.configWindowsResizeFromEdges) 4 else 0
            val gripDrawSize = max(g.fontSize * 1.35f, window.windowRounding + 1f + g.fontSize * 0.2f).i.f
            val gripHoverInnerSize = (gripDrawSize * 0.75f).i.f
            val gripHoverOuterSize = if (io.configWindowsResizeFromEdges) WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS else 0f

            val posTarget = Vec2(Float.MAX_VALUE)
            val sizeTarget = Vec2(Float.MAX_VALUE)

            // Manual resize grips
            pushId("#RESIZE")
            for (resizeGripN in 0 until resizeGripCount) {

                val grip = resizeGripDef[resizeGripN]
                val corner = window.pos.lerp(window.pos + window.size, grip.cornerPos)

                // Using the FlattenChilds button flag we make the resize button accessible even if we are hovering over a child window
                val resizeRect = Rect(corner - grip.innerDir * gripHoverOuterSize, corner + grip.innerDir * gripHoverInnerSize)
                if (resizeRect.min.x > resizeRect.max.x) swap(resizeRect.min::x, resizeRect.max::x)
                if (resizeRect.min.y > resizeRect.max.y) swap(resizeRect.min::y, resizeRect.max::y)

                val f = ButtonFlag.FlattenChildren or ButtonFlag.NoNavFocus
                val (_, hovered, held) = buttonBehavior(resizeRect, window.getId(resizeGripN), f)
                //GetOverlayDrawList(window)->AddRect(resize_rect.Min, resize_rect.Max, IM_COL32(255, 255, 0, 255));
                if (hovered || held)
                    g.mouseCursor = if (resizeGripN has 1) MouseCursor.ResizeNESW else MouseCursor.ResizeNWSE

                if (held && g.io.mouseDoubleClicked[0] && resizeGripN == 0) {
                    // Manual auto-fit when double-clicking
                    sizeTarget put window.calcSizeAfterConstraint(sizeAutoFit)
                    clearActiveId()
                } else if (held) {
                    // Resize from any of the four corners
                    // We don't use an incremental MouseDelta but rather compute an absolute target size based on mouse position
                    // Corner of the window corresponding to our corner grip
                    val cornerTarget = g.io.mousePos - g.activeIdClickOffset + (grip.innerDir * gripHoverOuterSize).lerp(grip.innerDir * -gripHoverInnerSize, grip.cornerPos)
                    window.calcResizePosSizeFromAnyCorner(cornerTarget, grip.cornerPos, posTarget, sizeTarget)
                }
                if (resizeGripN == 0 || held || hovered)
                    resizeGripCol[resizeGripN] = (if (held) Col.ResizeGripActive else if (hovered) Col.ResizeGripHovered else Col.ResizeGrip).u32
            }
            for (borderN in 0 until resizeBorderCount) {
                val borderRect = window.getResizeBorderRect(borderN, gripHoverInnerSize, WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS)
                val (_, hovered, held) = buttonBehavior(borderRect, window.getId((borderN + 4)), ButtonFlag.FlattenChildren)
                //GetOverlayDrawList(window)->AddRect(border_rect.Min, border_rect.Max, IM_COL32(255, 255, 0, 255));
                if ((hovered && g.hoveredIdTimer > WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER) || held) {
                    g.mouseCursor = if (borderN has 1) MouseCursor.ResizeEW else MouseCursor.ResizeNS
                    if (held) borderHeld = borderN
                }
                if (held) {
                    val borderTarget = Vec2(window.pos)
                    val borderPosN = when (borderN) {
                        0 -> {
                            borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                            Vec2(0, 0)
                        }
                        1 -> {
                            borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                            Vec2(1, 0)
                        }
                        2 -> {
                            borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                            Vec2(0, 1)
                        }
                        3 -> {
                            borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                            Vec2(0, 0)
                        }
                        else -> Vec2(0, 0)
                    }
                    window.calcResizePosSizeFromAnyCorner(borderTarget, borderPosN, posTarget, sizeTarget)
                }
            }
            popId()

            // Navigation resize (keyboard/gamepad)
            if (g.navWindowingTarget?.rootWindow === window) {
                val navResizeDelta = Vec2()
                if (g.navInputSource == InputSource.NavKeyboard && g.io.keyShift)
                    navResizeDelta put getNavInputAmount2d(NavDirSourceFlag.Keyboard.i, InputReadMode.Down)
                if (g.navInputSource == InputSource.NavGamepad)
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
                window.markIniSettingsDirty()
            }
            if (posTarget.x != Float.MAX_VALUE) {
                window.pos = glm.floor(posTarget)
                window.markIniSettingsDirty()
            }

            window.size put window.sizeFull
        }

        class ResizeGripDef(val cornerPos: Vec2, val innerDir: Vec2, val angleMin12: Int, val angleMax12: Int)

        val resizeGripDef = arrayOf(
                ResizeGripDef(Vec2(1, 1), Vec2(-1, -1), 0, 3),  // Lower right
                ResizeGripDef(Vec2(0, 1), Vec2(+1, -1), 3, 6),  // Lower left
                ResizeGripDef(Vec2(0, 0), Vec2(+1, +1), 6, 9),  // Upper left
                ResizeGripDef(Vec2(1, 0), Vec2(-1, +1), 9, 12)) // Upper right
    }
}