package imgui.api

import glm_.f
import glm_.hasnt
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.callContextHooks
import imgui.ImGui.clearActiveID
import imgui.ImGui.clearDragDrop
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.defaultFont
import imgui.ImGui.dockContextUpdateDocking
import imgui.ImGui.dockContextUpdateUndocking
import imgui.ImGui.end
import imgui.ImGui.focusTopMostWindowUnderOne
import imgui.ImGui.getBackgroundDrawList
import imgui.ImGui.getColorU32
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.io
import imgui.ImGui.isMouseDown
import imgui.ImGui.keepAliveID
import imgui.ImGui.mergedKeyModFlags
import imgui.ImGui.setCurrentFont
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setTooltip
import imgui.ImGui.style
import imgui.ImGui.topMostPopupModal
import imgui.ImGui.updateHoveredWindowAndCaptureFlags
import imgui.ImGui.updateMouseMovingWindowEndFrame
import imgui.ImGui.updateMouseMovingWindowNewFrame
import imgui.classes.ContextHookType
import imgui.classes.*
import imgui.font.FontAtlas
import imgui.has
import imgui.internal.DrawData
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.lengthSqr
import imgui.internal.sections.or
import imgui.static.*
import kool.lim
import imgui.WindowFlag as Wf
import imgui.internal.sections.DrawListFlag as Dlf

@Suppress("UNCHECKED_CAST")

/** Main */
interface main {

    /** access the IO structure (mouse/keyboard/gamepad inputs, time, various configuration options/flags) */
    val io: IO
        get() = gImGui?.io
                ?: throw Error("No current context. Did you call ::Context() or Context::setCurrent()?")

    /** access the Style structure (colors, sizes). Always use PushStyleCol(), PushStyleVar() to modify style mid-frame! */
    val style: Style
        get() = gImGui?.style
                ?: throw Error("No current context. Did you call ::Context() or Context::setCurrent()?")

    fun newFrame() {

        assert(gImGui != null) { "No current context. Did you call ImGui::CreateContext() and ImGui::SetCurrentContext()?" }

        callContextHooks(g, ContextHookType.NewFramePre)

        // Check and assert for various common IO and Configuration mistakes
        g.configFlagsLastFrame = g.configFlagsCurrFrame
        errorCheckNewFrameSanityChecks()
        g.configFlagsCurrFrame = g.io.configFlags

        // Load settings on first frame, save settings when modified (after a delay)
        updateSettings()

        g.time += io.deltaTime
        g.withinFrameScope = true
        g.frameCount += 1
        g.tooltipOverrideCount = 0
        g.windowsActiveCount = 0
        g.menusIdSubmittedThisFrame.clear()

        // Calculate frame-rate for the user, as a purely luxurious feature
        g.framerateSecPerFrameAccum += io.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
        g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = io.deltaTime
        g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
        io.framerate = if (g.framerateSecPerFrameAccum > 0f) 1f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size.f) else Float.MAX_VALUE

        updateViewportsNewFrame()

        // Setup current font and draw list shared data
        // FIXME-VIEWPORT: the concept of a single ClipRectFullscreen is not ideal!
        io.fonts.locked = true
        setCurrentFont(defaultFont)
        assert(g.font.isLoaded)
        val virtualSpace = Rect(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (viewport in g.viewports)
            virtualSpace add viewport.mainRect
        g.drawListSharedData.clipRectFullscreen.put(virtualSpace.min.x, virtualSpace.min.y, virtualSpace.max.x, virtualSpace.max.y)
        g.drawListSharedData.curveTessellationTol = style.curveTessellationTol
        g.drawListSharedData.setCircleSegmentMaxError_(style.circleSegmentMaxError)
        var flags = Dlf.None.i
        if (style.antiAliasedLines)
            flags = flags or Dlf.AntiAliasedLines
        if (style.antiAliasedLinesUseTex && g.font.containerAtlas.flags hasnt FontAtlas.Flag.NoBakedLines.i)
            flags = flags or Dlf.AntiAliasedLinesUseTex
        if (style.antiAliasedFill)
            flags = flags or Dlf.AntiAliasedFill
        if (io.backendFlags has BackendFlag.RendererHasVtxOffset)
            flags = flags or Dlf.AllowVtxOffset
        g.drawListSharedData.initialFlags = flags

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it.
        for (viewport in g.viewports) {
            viewport.drawData = null
            viewport.drawDataP.clear()
        }

        // Drag and drop keep the source ID alive so even if the source disappear our state is consistent
        if (g.dragDropActive && g.dragDropPayload.sourceId == g.activeId)
            keepAliveID(g.dragDropPayload.sourceId)

        // Update HoveredId data
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
        g.hoveredIdDisabled = false

        // Update ActiveId data (clear reference to active widget if the widget isn't alive anymore)
        if (g.activeIdIsAlive != g.activeId && g.activeIdPreviousFrame == g.activeId && g.activeId != 0)
            clearActiveID()
        if (g.activeId != 0)
            g.activeIdTimer += io.deltaTime
        g.lastActiveIdTimer += io.deltaTime
        g.activeIdPreviousFrame = g.activeId
        g.activeIdPreviousFrameWindow = g.activeIdWindow
        g.activeIdPreviousFrameHasBeenEdited = g.activeIdHasBeenEditedBefore
        g.activeIdIsAlive = 0
        g.activeIdHasBeenEditedThisFrame = false
        g.activeIdPreviousFrameIsAlive = false
        g.activeIdIsJustActivated = false
        if (g.tempInputId != 0 && g.activeId != g.tempInputId)
            g.tempInputId = 0
        if (g.activeId == 0) {
            g.activeIdUsingNavInputMask = 0
            g.activeIdUsingNavDirMask = 0
            g.activeIdUsingKeyInputMask = 0
        }

        // Drag and drop
        g.dragDropAcceptIdPrev = g.dragDropAcceptIdCurr
        g.dragDropAcceptIdCurr = 0
        g.dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE
        g.dragDropWithinSource = false
        g.dragDropWithinTarget = false
        g.dragDropHoldJustPressedId = 0

        // Update keyboard input state
        // Synchronize io.KeyMods with individual modifiers io.KeyXXX bools
        io.keyMods = mergedKeyModFlags
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
        // Update gamepad/keyboard navigation
        navUpdate()

        // Update mouse input state
        updateMouseInputs()

        // Undocking
        // (needs to be before UpdateMouseMovingWindowNewFrame so the window is already offset and following the mouse on the detaching frame)
        dockContextUpdateUndocking(g)

        // Find hovered window
        // (needs to be before UpdateMouseMovingWindowNewFrame so we fill g.HoveredWindowUnderMovingWindow on the mouse release frame)
        updateHoveredWindowAndCaptureFlags()

        // Handle user moving window with mouse (at the beginning of the frame to avoid input lag or sheering)
        updateMouseMovingWindowNewFrame()

        // Background darkening/whitening
        g.dimBgRatio = when {
            topMostPopupModal != null || (g.navWindowingTarget != null && g.navWindowingHighlightAlpha > 0f) -> (g.dimBgRatio + io.deltaTime * 6f) min 1f
            else -> (g.dimBgRatio - io.deltaTime * 10f) max 0f
        }
        g.mouseCursor = MouseCursor.Arrow
        g.wantTextInputNextFrame = -1
        g.wantCaptureKeyboardNextFrame = -1
        g.wantCaptureMouseNextFrame = -1
        g.platformImePos put 1f // OS Input Method Editor showing on top-left of our window by default
        g.platformImePosViewport = null

        // Mouse wheel scrolling, scale
        updateMouseWheel()

        // Update legacy TAB focus
        updateTabFocus()

        // Mark all windows as not visible and compact unused memory.
        assert(g.windowsFocusOrder.size == g.windows.size)
        val memoryCompactStartTime = if (io.configWindowsMemoryCompactTimer >= 0f) g.time.f - io.configWindowsMemoryCompactTimer else Float.MAX_VALUE
        g.windows.forEach {
            it.wasActive = it.active
            it.beginCount = 0
            it.active = false
            it.writeAccessed = false

            // Garbage collect transient buffers of recently unused windows
            if (!it.wasActive && !it.memoryCompacted && it.lastTimeActive < memoryCompactStartTime)
                it.gcCompactTransientBuffers()
        }

        // Closing the focused window restore focus to the first active root window in descending z-order
        if (g.navWindow?.wasActive == false)
            focusTopMostWindowUnderOne()

        // No window should be open at the beginning of the frame.
        // But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an explicit clear.
        g.currentWindowStack.clear()
        g.beginPopupStack.clear()
        closePopupsOverWindow(g.navWindow, false)

        // Docking
        dockContextUpdateDocking(g)

        // [DEBUG] Item picker tool - start with DebugStartItemPicker() - useful to visually select an item and break into its call-stack.
        updateDebugToolItemPicker()

        // Create implicit/fallback window - which we will only render it if the user has added something to it.
        // We don't use "Debug" to avoid colliding with user trying to create a "Debug" window with custom flags.
        // This fallback is particularly important as it avoid ImGui:: calls from crashing.
        g.withinFrameScopeWithImplicitWindow = true
        setNextWindowSize(Vec2(400), Cond.FirstUseEver)
        begin("Debug##Default")
        assert(g.currentWindow!!.isFallbackWindow)

        callContextHooks(g, ContextHookType.NewFramePost)
    }

    /** Ends the Dear ImGui frame. automatically called by ::render().
     *  If you don't need to render data (skipping rendering) you may call ::endFrame() without Render()... but you'll have wasted CPU already!
     *  If you don't need to render, better to not create any windows and not call ::newFrame() at all!  */
    fun endFrame() {

        assert(g.initialized)

        // Don't process endFrame() multiple times.
        if (g.frameCountEnded == g.frameCount) return
        assert(g.withinFrameScope) { "Forgot to call ImGui::newFrame()?" }

        callContextHooks(g, ContextHookType.EndFramePre)

        errorCheckEndFrameSanityChecks()

        // Notify OS when our Input Method Editor cursor has moved (e.g. CJK inputs using Microsoft IME)
        g.platformIO.platform_SetImeInputPos?.let { setIme ->
            if (g.platformImeLastPos.x == Float.MAX_VALUE || (g.platformImePos - g.platformImeLastPos).lengthSqr > 0.0001f)
                g.platformImePosViewport?.let { viewport ->
                    if (viewport.platformWindowCreated) {
                        setIme(viewport, g.platformImePos)
                        g.platformImeLastPos put g.platformImePos
                        g.platformImePosViewport = null
                    }
                }
        }

        // Hide implicit/fallback "Debug" window if it hasn't been used
        g.withinFrameScopeWithImplicitWindow = false
        g.currentWindow?.let {
            if (!it.writeAccessed) it.active = false
        }

        end()

        // Update navigation: CTRL+Tab, wrap-around requests
        navEndFrame()

        setCurrentViewport(null, null)

        // Drag and Drop: Elapse payload (if delivered, or if source stops being submitted)
        if (g.dragDropActive) {
            val isDelivered = g.dragDropPayload.delivery
            val isElapsed = g.dragDropPayload.dataFrameCount + 1 < g.frameCount &&
                    (g.dragDropSourceFlags has DragDropFlag.SourceAutoExpirePayload || !isMouseDown(g.dragDropMouseButton))
            if (isDelivered || isElapsed)
                clearDragDrop()
        }

        // Drag and Drop: Fallback for source tooltip. This is not ideal but better than nothing.
        if (g.dragDropActive && g.dragDropSourceFrameCount < g.frameCount && g.dragDropSourceFlags hasnt DragDropFlag.SourceNoPreviewTooltip) {
            g.dragDropWithinSource = true
            setTooltip("...")
            g.dragDropWithinSource = false
        }

        // End frame
        g.withinFrameScope = false
        g.frameCountEnded = g.frameCount

        // Initiate moving window + handle left-click and right-click focus
        updateMouseMovingWindowEndFrame()

        // Update user-facing viewport list (g.Viewports -> g.PlatformIO.Viewports after filtering out some)
        updateViewportsEndFrame()

        /*  Sort the window list so that all child windows are after their parent
            We cannot do that on FocusWindow() because children may not exist yet         */
        g.windowsTempSortBuffer.clear()
        g.windowsTempSortBuffer.ensureCapacity(g.windows.size)
        g.windows.filter { !it.active || it.flags hasnt Wf._ChildWindow }  // if a child is active its parent will add it
                .forEach { it addToSortBuffer g.windowsTempSortBuffer }
        assert(g.windows.size == g.windowsTempSortBuffer.size) { "This usually assert if there is a mismatch between the ImGuiWindowFlags_ChildWindow / ParentWindow values and DC.ChildWindows[] in parents, aka we've done something wrong." }
        g.windows.clear()
        g.windows += g.windowsTempSortBuffer
        io.metricsActiveWindows = g.windowsActiveCount

        // Unlock font atlas
        io.fonts.locked = false

        // Clear Input data for next frame
        io.mouseWheel = 0f
        io.mouseWheelH = 0f
        io.inputQueueCharacters.clear()
        io.navInputs.fill(0f)

        callContextHooks(g, ContextHookType.EndFramePost)
    }

    /** ends the Dear ImGui frame, finalize the draw data. You can then get call GetDrawData(). */
    fun render() {

        assert(g.initialized)

        if (g.frameCountEnded != g.frameCount) endFrame()
        g.frameCountRendered = g.frameCount
        io.metricsRenderWindows = 0

        callContextHooks(g, ContextHookType.RenderPre)

        // Add background ImDrawList (for each active viewport)
        for (viewport in g.viewports) {
            viewport.drawDataBuilder.clear()
            if (viewport.drawLists[0] != null)
                getBackgroundDrawList(viewport) addTo viewport.drawDataBuilder.layers[0]
        }

        // Add ImDrawList to render
        val windowsToRenderTopMost = arrayOf(
                g.navWindowingTarget?.rootWindow?.takeIf { it.flags has Wf.NoBringToFrontOnFocus },
                g.navWindowingTarget?.let { g.navWindowingListWindow })
        g.windows.forEach {
            if (it.isActiveAndVisible && it.flags hasnt Wf._ChildWindow && it !== windowsToRenderTopMost[0] && it !== windowsToRenderTopMost[1])
                it.addToDrawData()
        }
        windowsToRenderTopMost.forEach {
            if (it?.isActiveAndVisible == true) // NavWindowingTarget is always temporarily displayed as the top-most window
                it.addToDrawData()
        }

        val mouseCursorOffset = Vec2()
        val mouseCursorSize = Vec2()
        val mouseCursorUv = Array(4) { Vec2() }
        if (io.mouseDrawCursor && g.mouseCursor != MouseCursor.None)
            io.fonts.getMouseCursorTexData(g.mouseCursor, mouseCursorOffset, mouseCursorSize, mouseCursorUv)

        // Setup ImDrawData structures for end-user
        io.metricsRenderVertices = 0
        io.metricsRenderIndices = 0
        for (viewport in g.viewports) {

            viewport.drawDataBuilder.flattenIntoSingleLayer()

            // Draw software mouse cursor if requested by io.MouseDrawCursor flag
            // (note we scale cursor by current viewport/monitor, however Windows 10 for its own hardware cursor seems to be using a different scale factor)
            if (mouseCursorSize.x > 0f && mouseCursorSize.y > 0f) { // TODO glm
                val scale = style.mouseCursorScale * viewport.dpiScale
                if (viewport.mainRect overlaps Rect(io.mousePos, io.mousePos + Vec2(mouseCursorSize.x + 2, mouseCursorSize.y + 2) * scale))
                    getForegroundDrawList(viewport).renderMouseCursor(io.mousePos, scale, g.mouseCursor, COL32_WHITE, COL32_BLACK, COL32(0, 0, 0, 48))
            }

            // Add foreground ImDrawList (for each active viewport)
            if (viewport.drawLists[1] != null)
                getForegroundDrawList(viewport) addTo viewport.drawDataBuilder.layers[0]

            setupViewportDrawData(viewport, viewport.drawDataBuilder.layers[0])
            io.metricsRenderVertices += viewport.drawData!!.totalVtxCount
            io.metricsRenderIndices += viewport.drawData!!.totalIdxCount
        }

        callContextHooks(g, ContextHookType.RenderPost)
    }

    /** Pass this to your backend rendering function! Valid after Render() and until the next call to NewFrame() */
    val drawData: DrawData?
        //        get() = when (Platform.get()) { TODO check
//            Platform.MACOSX -> g.drawData.clone()
//            else -> g.drawData
//        }.takeIf { it.valid }
        get() = g.viewports[0].drawDataP.takeIf { it.valid }

    companion object {

        fun endFrameDrawDimmedBackgrounds() {

            // Draw modal whitening background on _other_ viewports than the one the modal is one
            val modalWindow = topMostPopupModal
            val dimBgForModal = modalWindow != null
            val dimBgForWindowList = g.navWindowingTargetAnim != null
            if (dimBgForModal || dimBgForWindowList)
                for (viewport in g.viewports) {
                    if (modalWindow != null && viewport === modalWindow.viewport)
                        continue
                    if (viewport === g.navWindowingListWindow?.viewport)
                        continue
                    if (viewport === g.navWindowingTargetAnim?.viewport)
                        continue
                    val drawList = getForegroundDrawList(viewport)
                    val dimBgCol = getColorU32(if (dimBgForModal) Col.ModalWindowDimBg else Col.NavWindowingDimBg, g.dimBgRatio)
                    drawList.addRectFilled(viewport.pos, viewport.pos + viewport.size, dimBgCol)
                }

            // Draw modal whitening background between CTRL-TAB list
            if (dimBgForWindowList && g.navWindowingTargetAnim!!.active) {
                // Choose a draw list that will be front-most across all our children
                // In the unlikely case that the window wasn't made active we can't rely on its drawlist and skip rendering all-together.
                val window = g.navWindowingTargetAnim!!
                val drawList = findFrontMostVisibleChildWindow(window.rootWindow!!).drawList
                drawList.pushClipRectFullScreen()

                // Docking: draw modal whitening background on other nodes of a same dock tree
                if (window.rootWindowDockStop!!.dockIsActive)
                    if (window.rootWindow !== window.rootWindowDockStop)
                        drawList.renderRectFilledWithHole(window.rootWindow!!.rect(), window.rootWindowDockStop!!.rect(), getColorU32(Col.NavWindowingDimBg, g.dimBgRatio), style.windowRounding)

                // Draw navigation selection/windowing rectangle border
                var rounding = window.windowRounding max style.windowRounding
                val bb = window.rect()
                bb expand g.fontSize
                if (window.viewport!!.mainRect in bb) { // If a window fits the entire viewport, adjust its highlight inward
                    bb expand (-g.fontSize - 1f)
                    rounding = window.windowRounding
                }
                drawList.addRect(bb.min, bb.max, getColorU32(Col.NavWindowingHighlight, g.navWindowingHighlightAlpha), rounding, -1, 3f)
                drawList.popClipRect()
            }
        }

        fun findFrontMostVisibleChildWindow(window: Window): Window {
            var n = window.dc.childWindows.lastIndex
            while (n >= 0)
                if (window.dc.childWindows[n].isActiveAndVisible)
                    return findFrontMostVisibleChildWindow(window.dc.childWindows[n])
                else n--
            return window
        }

        fun setupViewportDrawData(viewport: ViewportP, drawLists: ArrayList<DrawList>) {
            // When minimized, we report draw_data->DisplaySize as zero to be consistent with non-viewport mode,
            // and to allow applications/back-ends to easily skip rendering.
            // FIXME: Note that we however do NOT attempt to report "zero drawlist / vertices" into the ImDrawData structure.
            // This is because the work has been done already, and its wasted! We should fix that and add optimizations for
            // it earlier in the pipeline, rather than pretend to hide the data at the end of the pipeline.
            val isMinimized = viewport.flags has ViewportFlag.Minimized

            val drawData = viewport.drawDataP
            viewport.drawData = drawData // Make publicly accessible
            drawData.valid = true
            drawData.cmdLists.clear()
            if (drawLists.isNotEmpty())
                drawData.cmdLists += drawLists
            drawData.totalIdxCount = 0
            drawData.totalVtxCount = 0
            drawData.displayPos put viewport.pos
            if(isMinimized) drawData.displaySize put 0f
            else drawData.displaySize put viewport.size
            drawData.framebufferScale put io.displayFramebufferScale // FIXME-VIEWPORT: This may vary on a per-monitor/viewport basis?
            for (n in 0 until drawLists.size) {
                drawData.totalVtxCount += drawLists[n].vtxBuffer.lim
                drawData.totalIdxCount += drawLists[n].idxBuffer.lim
            }
        }
    }
}