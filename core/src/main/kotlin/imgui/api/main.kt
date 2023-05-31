package imgui.api

import glm_.f
import glm_.i
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.callHooks
import imgui.ImGui.clearActiveID
import imgui.ImGui.clearDragDrop
import imgui.ImGui.defaultFont
import imgui.ImGui.end
import imgui.ImGui.focusTopMostWindowUnderOne
import imgui.ImGui.gcCompactTransientBuffers
import imgui.ImGui.gcCompactTransientMiscBuffers
import imgui.ImGui.isDown
import imgui.ImGui.keepAliveID
import imgui.ImGui.mainViewport
import imgui.ImGui.renderMouseCursor
import imgui.ImGui.setCurrentFont
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setTooltip
import imgui.ImGui.topMostPopupModal
import imgui.ImGui.updateHoveredWindowAndCaptureFlags
import imgui.ImGui.updateInputEvents
import imgui.ImGui.updateMouseMovingWindowEndFrame
import imgui.ImGui.updateMouseMovingWindowNewFrame
import imgui.classes.Context
import imgui.classes.ContextHookType
import imgui.classes.IO
import imgui.classes.Style
import imgui.font.FontAtlas
import imgui.internal.DrawData
import imgui.internal.classes.Rect
import imgui.internal.sections.DrawListFlags
import imgui.internal.sections.IMGUI_DEBUG_LOG_ACTIVEID
import imgui.internal.sections.IMGUI_DEBUG_LOG_IO
import imgui.static.*
import imgui.statics.*
import org.lwjgl.system.Platform
import imgui.WindowFlag as Wf
import imgui.internal.sections.DrawListFlag as Dlf

/** Main */
interface main {

    /** Internal state access - if you want to share Dear ImGui state between modules (e.g. DLL) or allocate it yourself
     *  Note that we still point to some static data and members (such as GFontAtlas), so the state instance you end up using will point to the static data within its module */
    val currentContext: Context?
        get() = gImGuiNullable

    /** access the IO structure (mouse/keyboard/gamepad inputs, time, various configuration options/flags) */
    val io: IO
        get() = gImGui.io

    /** access the Style structure (colors, sizes). Always use PushStyleCol(), PushStyleVar() to modify style mid-frame! */
    val style: Style
        get() = gImGui.style

    fun newFrame() {
        // Remove pending delete hooks before frame start.
        // This deferred removal avoid issues of removal while iterating the hook vector
        for (n in g.hooks.indices.reversed())
            if (g.hooks[n].type == ContextHookType.PendingRemoval_)
                g.hooks.removeAt(n)

        g callHooks ContextHookType.NewFramePre

        // Check and assert for various common IO and Configuration mistakes
        errorCheckNewFrameSanityChecks()

        // Load settings on first frame, save settings when modified (after a delay)
        updateSettings()

        g.time += io.deltaTime
        g.withinFrameScope = true
        g.frameCount += 1
        if (g.debugLogFlags != none)
            println("      [%04d]".format(ImGui.frameCount))
        g.tooltipOverrideCount = 0
        g.windowsActiveCount = 0
        g.menusIdSubmittedThisFrame.clear()

        // Calculate frame-rate for the user, as a purely luxurious feature
        g.framerateSecPerFrameAccum += io.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
        g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = io.deltaTime
        g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
        g.framerateSecPerFrameCount = (g.framerateSecPerFrameCount + 1) min g.framerateSecPerFrame.size
        io.framerate = if (g.framerateSecPerFrameAccum > 0f) 1f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrameCount.f) else Float.MAX_VALUE

        // Process input queue (trickle as many events as possible), turn events into writes to IO structure
        g.inputEventsTrail.clear()
        updateInputEvents(g.io.configInputTrickleEventQueue)

        // Update viewports (after processing input queue, so io.MouseHoveredViewport is set)
        updateViewportsNewFrame()

        // Setup current font and draw list shared data
        io.fonts.locked = true
        setCurrentFont(defaultFont)
        assert(g.font.isLoaded)
        val virtualSpace = Rect(Float.MAX_VALUE, -Float.MAX_VALUE)
        for (v in g.viewports)
            virtualSpace add v.mainRect
        g.drawListSharedData.clipRectFullscreen = virtualSpace.toVec4()
        g.drawListSharedData.curveTessellationTol = style.curveTessellationTol
        g.drawListSharedData.setCircleTessellationMaxError_(style.circleTessellationMaxError)
        var flags: DrawListFlags = none
        if (style.antiAliasedLines)
            flags /= Dlf.AntiAliasedLines
        if (style.antiAliasedLinesUseTex && g.font.containerAtlas.flags hasnt FontAtlas.Flag.NoBakedLines)
            flags /= Dlf.AntiAliasedLinesUseTex
        if (style.antiAliasedFill)
            flags /= Dlf.AntiAliasedFill
        if (io.backendFlags has BackendFlag.RendererHasVtxOffset)
            flags /= Dlf.AllowVtxOffset
        g.drawListSharedData.initialFlags = flags

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it.
        for (v in g.viewports)
            v.drawDataP.clear()

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

        // Clear ActiveID if the item is not alive anymore.
        // In 1.87, the common most call to KeepAliveID() was moved from GetID() to ItemAdd().
        // As a result, custom widget using ButtonBehavior() _without_ ItemAdd() need to call KeepAliveID() themselves.
        if (g.activeId != 0 && g.activeIdIsAlive != g.activeId && g.activeIdPreviousFrame == g.activeId) {
            IMGUI_DEBUG_LOG_ACTIVEID("NewFrame(): ClearActiveID() because it isn't marked alive anymore!")
            clearActiveID()
        }

        // Update ActiveId data (clear reference to active widget if the widget isn't alive anymore)
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
            g.activeIdUsingNavDirMask = 0
            g.activeIdUsingAllKeyboardKeys = false
        }

        // Update hover delay for IsItemHovered() with delays and tooltips
        g.hoverDelayIdPreviousFrame = g.hoverDelayId
        if (g.hoverDelayId != 0) {
            //if (g.IO.MouseDelta.x == 0.0f && g.IO.MouseDelta.y == 0.0f) // Need design/flags
            g.hoverDelayTimer += g.io.deltaTime
            g.hoverDelayClearTimer = 0f
            g.hoverDelayId = 0
        } else if (g.hoverDelayTimer > 0f) {
            // This gives a little bit of leeway before clearing the hover timer, allowing mouse to cross gaps
            g.hoverDelayClearTimer += g.io.deltaTime
            if (g.hoverDelayClearTimer >= 0.2f max (g.io.deltaTime * 2f)) { // ~6 frames at 30 Hz + allow for low framerate
                g.hoverDelayTimer = 0f; g.hoverDelayClearTimer = 0f // May want a decaying timer, in which case need to clamp at max first, based on max of caller last requested timer.
            }
        }

        // Drag and drop
        g.dragDropAcceptIdPrev = g.dragDropAcceptIdCurr
        g.dragDropAcceptIdCurr = 0
        g.dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE
        g.dragDropWithinSource = false
        g.dragDropWithinTarget = false
        g.dragDropHoldJustPressedId = 0

        // Close popups on focus lost (currently wip/opt-in)
        //if (g.IO.AppFocusLost)
        //    ClosePopupsExceptModals();

        // Update keyboard input state
        updateKeyboardInputs()

        //IM_ASSERT(g.IO.KeyCtrl == IsKeyDown(ImGuiKey_LeftCtrl) || IsKeyDown(ImGuiKey_RightCtrl));
        //IM_ASSERT(g.IO.KeyShift == IsKeyDown(ImGuiKey_LeftShift) || IsKeyDown(ImGuiKey_RightShift));
        //IM_ASSERT(g.IO.KeyAlt == IsKeyDown(ImGuiKey_LeftAlt) || IsKeyDown(ImGuiKey_RightAlt));
        //IM_ASSERT(g.IO.KeySuper == IsKeyDown(ImGuiKey_LeftSuper) || IsKeyDown(ImGuiKey_RightSuper));

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

        // Platform IME data: reset for the frame
        g.platformImeDataPrev = g.platformImeData // OS Input Method Editor showing on top-left of our window by default
        g.platformImeData.wantVisible = false

        // Mouse wheel scrolling, scale
        updateMouseWheel()

        // Mark all windows as not visible and compact unused memory.
        assert(g.windowsFocusOrder.size <= g.windows.size)
        val memoryCompactStartTime = if (g.gcCompactAll || io.configMemoryCompactTimer < 0f) Float.MAX_VALUE else g.time.f - io.configMemoryCompactTimer
        g.windows.forEach {
            it.wasActive = it.active
            it.active = false
            it.writeAccessed = false
            it.beginCountPreviousFrame = it.beginCount
            it.beginCount = 0

            // Garbage collect transient buffers of recently unused windows
            if (!it.wasActive && !it.memoryCompacted && it.lastTimeActive < memoryCompactStartTime)
                it.gcCompactTransientBuffers()

            // Garbage collect transient buffers of recently unused tables
            for (i in 0 until g.tablesLastTimeActive.size)
                if (g.tablesLastTimeActive[i] >= 0f && g.tablesLastTimeActive[i] < memoryCompactStartTime)
                    g.tables.getByIndex(i).gcCompactTransientBuffers()
        }
        if (g.gcCompactAll)
            gcCompactTransientMiscBuffers()
        g.gcCompactAll = false

        // Closing the focused window restore focus to the first active root window in descending z-order
        if (g.navWindow?.wasActive == false)
            focusTopMostWindowUnderOne()

        // No window should be open at the beginning of the frame.
        // But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an explicit clear.
        g.currentWindowStack.clear()
        g.beginPopupStack.clear()
        g.itemFlagsStack.clear()
        g.itemFlagsStack += none
        g.groupStack.clear()

        // // [DEBUG] Update debug features
        updateDebugToolItemPicker()
        updateDebugToolStackQueries()
        if (g.debugLocateFrames > 0 && --g.debugLocateFrames == 0)
            g.debugLocateId = 0

        // Create implicit/fallback window - which we will only render it if the user has added something to it.
        // We don't use "Debug" to avoid colliding with user trying to create a "Debug" window with custom flags.
        // This fallback is particularly important as it prevents ImGui:: calls from crashing.
        g.withinFrameScopeWithImplicitWindow = true
        setNextWindowSize(Vec2(400), Cond.FirstUseEver)
        begin("Debug##Default")
        assert(g.currentWindow!!.isFallbackWindow)

        g callHooks ContextHookType.NewFramePost
    }

    /** Ends the Dear ImGui frame. automatically called by ::render().
     *  If you don't need to render data (skipping rendering) you may call ::endFrame() without Render()... but you'll have wasted CPU already!
     *  If you don't need to render, better to not create any windows and not call ::newFrame() at all!  */
    fun endFrame() {

        assert(g.initialized)

        // Don't process endFrame() multiple times.
        if (g.frameCountEnded == g.frameCount) return
        assert(g.withinFrameScope) { "Forgot to call ImGui::newFrame()?" }

        g callHooks ContextHookType.EndFramePre

        errorCheckEndFrameSanityChecks()

        // Notify Platform/OS when our Input Method Editor cursor has moved (e.g. CJK inputs using Microsoft IME)
        val imeData = g.platformImeData
        if (io.setPlatformImeDataFn != null && imeData !== g.platformImeDataPrev) {
            //            if (DEBUG)
            // println("in (${g.platformImePos.x}, ${g.platformImePos.y}) (${g.platformImeLastPos.x}, ${g.platformImeLastPos.y})")
            IMGUI_DEBUG_LOG_IO("Calling io.SetPlatformImeDataFn(): WantVisible: ${imeData.wantVisible.i}, InputPos (%.2f,%.2f)", imeData.inputPos.x, imeData.inputPos.y)
            g.io.setPlatformImeDataFn!!(mainViewport, g.platformImeData)
        }

        // Hide implicit/fallback "Debug" window if it hasn't been used
        g.withinFrameScopeWithImplicitWindow = false
        g.currentWindow?.let {
            if (!it.writeAccessed) it.active = false
        }

        end()

        // Update navigation: CTRL+Tab, wrap-around requests
        navEndFrame()

        // Drag and Drop: Elapse payload (if delivered, or if source stops being submitted)
        if (g.dragDropActive) {
            val isDelivered = g.dragDropPayload.delivery
            val isElapsed = g.dragDropPayload.dataFrameCount + 1 < g.frameCount &&
                    (g.dragDropSourceFlags has DragDropFlag.SourceAutoExpirePayload || !g.dragDropMouseButton.isDown)
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

        // Sort the window list so that all child windows are after their parent
        // We cannot do that on FocusWindow() because children may not exist yet
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
        io.appFocusLost = false
        io.mouseWheel = 0f; io.mouseWheelH = 0f
        io.inputQueueCharacters.clear()

        g callHooks ContextHookType.EndFramePost
    }

    /** ends the Dear ImGui frame, finalize the draw data. You can then get call GetDrawData().
     *
     *  Prepare the data for rendering so you can call GetDrawData()
     *  (As with anything within the ImGui:: namspace this doesn't touch your GPU or graphics API at all:
     *  it is the role of the ImGui_ImplXXXX_RenderDrawData() function provided by the renderer backend) */
    fun render() {

        assert(g.initialized)

        if (g.frameCountEnded != g.frameCount)
            endFrame()
        val firstRenderOfFrame = g.frameCountRendered != g.frameCount
        g.frameCountRendered = g.frameCount
        io.metricsRenderWindows = 0

        g callHooks ContextHookType.RenderPre

        // Add background ImDrawList (for each active viewport)
        for (viewport in g.viewports) {
            viewport.drawDataBuilder.clear()
            if (viewport.drawLists[0] != null)
                viewport.backgroundDrawList addTo viewport.drawDataBuilder.layers[0]
        }

        // Draw modal/window whitening backgrounds
        if (firstRenderOfFrame)
            renderDimmedBackgrounds()

        // Add ImDrawList to render
        val windowsToRenderTopMost = arrayOf(
            g.navWindowingTarget?.rootWindow?.takeIf { it.flags has Wf.NoBringToFrontOnFocus },
            g.navWindowingTarget?.let { g.navWindowingListWindow })
        g.windows
                .filter { it.isActiveAndVisible && it.flags hasnt Wf._ChildWindow && it !== windowsToRenderTopMost[0] && it !== windowsToRenderTopMost[1] }
                .forEach { it.addRootToDrawData() }
        windowsToRenderTopMost
                .filterNotNull()
                .filter { it.isActiveAndVisible } // NavWindowingTarget is always temporarily displayed as the top-most window
                .forEach { it.addRootToDrawData() }

        // Draw software mouse cursor if requested by io.MouseDrawCursor flag
        if (g.io.mouseDrawCursor && firstRenderOfFrame && g.mouseCursor != MouseCursor.None)
            renderMouseCursor(g.io.mousePos, g.style.mouseCursorScale, g.mouseCursor, COL32_WHITE, COL32_BLACK, COL32(0, 0, 0, 48))

        // Setup ImDrawData structures for end-user
        io.metricsRenderVertices = 0
        io.metricsRenderIndices = 0
        for (viewport in g.viewports) {
            viewport.drawDataBuilder.flattenIntoSingleLayer()

            // Add foreground ImDrawList (for each active viewport)
            if (viewport.drawLists[1] != null)
                viewport.foregroundDrawList addTo viewport.drawDataBuilder.layers[0]

            viewport.setupDrawData(viewport.drawDataBuilder.layers[0])
            val drawData = viewport.drawDataP
            io.metricsRenderVertices += drawData.totalVtxCount
            io.metricsRenderIndices += drawData.totalIdxCount
        }

        g callHooks ContextHookType.RenderPost
    }

    /** Pass this to your backend rendering function! Valid after Render() and until the next call to NewFrame() */
    val drawData: DrawData?
        get() {
            val viewport = g.viewports[0]
            return when (Platform.get()) {
                Platform.MACOSX -> viewport.drawDataP.clone()
                else -> viewport.drawDataP
            }.takeIf { it.valid }
        }
}