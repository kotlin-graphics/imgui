package imgui.api

import glm_.f
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.clearActiveID
import imgui.ImGui.clearDragDrop
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.defaultFont
import imgui.ImGui.end
import imgui.ImGui.focusTopMostWindowUnderOne
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
import imgui.classes.IO
import imgui.classes.Style
import imgui.internal.*
import imgui.static.*
import org.lwjgl.system.Platform
import imgui.ConfigFlag as Cf
import imgui.WindowFlag as Wf
import imgui.internal.DrawListFlag as Dlf

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

        if (IMGUI_ENABLE_TEST_ENGINE)
            Hook.preNewFrame!!(g)

        // Check and assert for various common IO and Configuration mistakes
        errorCheckNewFrameSanityChecks()

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
        io.framerate = if(g.framerateSecPerFrameAccum > 0f) 1f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size.f) else Float.MAX_VALUE

        // Setup current font and draw list shared data
        io.fonts.locked = true
        setCurrentFont(defaultFont)
        assert(g.font.isLoaded)
        g.drawListSharedData.clipRectFullscreen = Vec4(0f, 0f, io.displaySize.x, io.displaySize.y)
        g.drawListSharedData.curveTessellationTol = style.curveTessellationTol
        g.drawListSharedData.setCircleSegmentMaxError_(style.circleSegmentMaxError)
        g.drawListSharedData.initialFlags = Dlf.None.i
        if (style.antiAliasedLines)
            g.drawListSharedData.initialFlags = g.drawListSharedData.initialFlags or Dlf.AntiAliasedLines
        if (style.antiAliasedFill)
            g.drawListSharedData.initialFlags = g.drawListSharedData.initialFlags or Dlf.AntiAliasedFill
        if (io.backendFlags has BackendFlag.RendererHasVtxOffset)
            g.drawListSharedData.initialFlags = g.drawListSharedData.initialFlags or Dlf.AllowVtxOffset

        g.backgroundDrawList.clear()
        g.backgroundDrawList.pushTextureId(io.fonts.texID)
        g.backgroundDrawList.pushClipRectFullScreen()

        g.foregroundDrawList.clear()
        g.foregroundDrawList.pushTextureId(io.fonts.texID)
        g.foregroundDrawList.pushClipRectFullScreen()

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it.
        g.drawData.clear()

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

        // [DEBUG] Item picker tool - start with DebugStartItemPicker() - useful to visually select an item and break into its call-stack.
        updateDebugToolItemPicker()

        // Create implicit/fallback window - which we will only render it if the user has added something to it.
        // We don't use "Debug" to avoid colliding with user trying to create a "Debug" window with custom flags.
        // This fallback is particularly important as it avoid ImGui:: calls from crashing.
        g.withinFrameScopeWithImplicitWindow = true
        setNextWindowSize(Vec2(400), Cond.FirstUseEver)
        begin("Debug##Default")
        assert(g.currentWindow!!.isFallbackWindow)

        if (IMGUI_ENABLE_TEST_ENGINE)
            Hook.postNewFrame!!(g)
    }

    /** Ends the Dear ImGui frame. automatically called by ::render().
     *  If you don't need to render data (skipping rendering) you may call ::endFrame() without Render()... but you'll have wasted CPU already!
     *  If you don't need to render, better to not create any windows and not call ::newFrame() at all!  */
    fun endFrame() {

        assert(g.initialized)

        // Don't process endFrame() multiple times.
        if (g.frameCountEnded == g.frameCount) return
        assert(g.withinFrameScope) { "Forgot to call ImGui::newFrame()?" }

        errorCheckEndFrameSanityChecks()

        // Notify OS when our Input Method Editor cursor has moved (e.g. CJK inputs using Microsoft IME)
        if (io.imeSetInputScreenPosFn != null && (g.platformImeLastPos.x == Float.MAX_VALUE || (g.platformImeLastPos - g.platformImePos).lengthSqr > 0.0001f)) {
            if (DEBUG)
                Unit // println("in (${g.platformImePos.x}, ${g.platformImePos.y}) (${g.platformImeLastPos.x}, ${g.platformImeLastPos.y})")
//            io.imeSetInputScreenPosFn!!(g.platformImePos.x.i, g.platformImePos.y.i)
            io.imeSetInputScreenPosFn!!(1000, 1000)
            g.platformImeLastPos put g.platformImePos
        }

        // Hide implicit/fallback "Debug" window if it hasn't been used
        g.withinFrameScopeWithImplicitWindow = false
        g.currentWindow?.let {
            if (!it.writeAccessed) it.active = false
        }

        end()

        // Show CTRL+TAB list window
        if (g.navWindowingTarget != null)
            navUpdateWindowingOverlay()

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

        /*  Sort the window list so that all child windows are after their parent
            We cannot do that on FocusWindow() because childs may not exist yet         */
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
    }

    /** ends the Dear ImGui frame, finalize the draw data. You can get call GetDrawData() to obtain it and run your rendering function (up to v1.60, this used to call io.RenderDrawListsFn(). Nowadays, we allow and prefer calling your render function yourself.)   */
    fun render() {

        assert(g.initialized)

        if (g.frameCountEnded != g.frameCount) endFrame()
        g.frameCountRendered = g.frameCount
        io.metricsRenderWindows = 0
        g.drawDataBuilder.clear()

        // Add background ImDrawList
        if (g.backgroundDrawList.vtxBuffer.hasRemaining())
            g.backgroundDrawList addTo g.drawDataBuilder.layers[0]

        // Add ImDrawList to render
        val windowsToRenderTopMost = arrayOf(
                g.navWindowingTarget?.rootWindow?.takeIf { it.flags has Wf.NoBringToFrontOnFocus },
                g.navWindowingTarget?.let { g.navWindowingList[0] })
        g.windows
                .filter { it.isActiveAndVisible && it.flags hasnt Wf._ChildWindow && it !== windowsToRenderTopMost[0] && it !== windowsToRenderTopMost[1] }
                .forEach { it.addToDrawData() }
        windowsToRenderTopMost
                .filterNotNull()
                .filter { it.isActiveAndVisible } // NavWindowingTarget is always temporarily displayed as the top-most window
                .forEach { it.addToDrawData() }
        g.drawDataBuilder.flattenIntoSingleLayer()

        // Draw software mouse cursor if requested
        if (io.mouseDrawCursor)
            g.foregroundDrawList.renderMouseCursor(Vec2(io.mousePos), style.mouseCursorScale, g.mouseCursor, COL32_WHITE, COL32_BLACK, COL32(0, 0, 0, 48))
        // Add foreground ImDrawList
        if (g.foregroundDrawList.vtxBuffer.hasRemaining())
            g.foregroundDrawList addTo g.drawDataBuilder.layers[0]

        // Setup ImDrawData structure for end-user
        g.drawData setup g.drawDataBuilder.layers[0]
        io.metricsRenderVertices = g.drawData.totalVtxCount
        io.metricsRenderIndices = g.drawData.totalIdxCount
    }

    /** Same value as passed to the old io.renderDrawListsFn function. Valid after ::render() and until the next call to
     *  ::newFrame()   */
    val drawData: DrawData?
        get() = when (Platform.get()) {
            Platform.MACOSX -> g.drawData.clone()
            else -> g.drawData
        }.takeIf { it.valid }
}