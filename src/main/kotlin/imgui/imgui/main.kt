package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.clearActiveId
import imgui.ImGui.clearDragDrop
import imgui.ImGui.end
import imgui.ImGui.initialize
import imgui.ImGui.isMousePosValid
import imgui.ImGui.keepAliveId
import imgui.ImGui.setActiveId
import imgui.ImGui.setNextWindowSize
import imgui.internal.Window
import imgui.internal.focus
import imgui.internal.lengthSqr
import kotlin.math.max
import kotlin.math.min
import imgui.Context as g
import imgui.WindowFlags as Wf
import imgui.internal.DrawListFlags as Dlf

interface imgui_main {

    var style
        get() = g.style
        set(value) {
            g.style = value
        }

    /** Same value as passed to your RenderDrawListsFn() function. valid after Render() and
     *  until the next call to NewFrame()   */
    val drawData get() = g.renderDrawData.takeIf { it.valid }

    /** start a new ImGui frame, you can submit any command from this point until NewFrame()/Render().  */
    fun newFrame() {

        ptrIndices = 0

        /* (We pass an error message in the assert expression as a trick to get it visible to programmers who are not 
            using a debugger, as most assert handlers display their argument)         */
        assert(IO.deltaTime >= 0f, { "Need a positive deltaTime (zero is tolerated but will cause some timing issues)" })
        assert(IO.displaySize greaterThanEqual 0, { "Invalid displaySize value" })
        assert(IO.fonts.fonts.size > 0, { "Font Atlas not created. Did you call IO.fonts.getTexDataAsRGBA32 / getTexDataAsAlpha8 ?" })
        assert(IO.fonts.fonts[0].isLoaded, { "Font Atlas not created. Did you call IO.fonts.getTexDataAsRGBA32 / getTexDataAsAlpha8 ?" })
        assert(style.curveTessellationTol > 0f, { "Invalid style setting" })
        assert(style.alpha in 0f..1f, { "Invalid style setting. Alpha cannot be negative (allows us to avoid a few clamps in color computations)" })
        assert(g.frameCount == 0 || g.frameCountEnded == g.frameCount, { "Forgot to call render() or endFrame() at the end of the previous frame?" })

        // Initialize on first frame
        if (!g.initialized) initialize()

        g.time += IO.deltaTime
        g.frameCount += 1
        g.tooltipOverrideCount = 0
        g.windowsActiveCount = 0

        defaultFont.setCurrent()
        assert(g.font.isLoaded)
        g.drawListSharedData.clipRectFullscreen.put(0f, 0f, IO.displaySize)
        g.drawListSharedData.curveTessellationTol = style.curveTessellationTol

        g.overlayDrawList.clear()
        g.overlayDrawList.pushTextureId(IO.fonts.texId)
        g.overlayDrawList.pushClipRectFullScreen()
        g.overlayDrawList.flags = (if (style.antiAliasedLines) Dlf.AntiAliasedLines.i else 0) or if (style.antiAliasedFill) Dlf.AntiAliasedFill.i else 0

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it
        g.renderDrawData.valid = false
        g.renderDrawData.cmdLists.clear()
        g.renderDrawData.totalIdxCount = 0
        g.renderDrawData.totalVtxCount = 0
        g.renderDrawData.cmdListsCount = 0

        // Clear reference to active widget if the widget isn't alive anymore
        if (g.hoveredIdPreviousFrame == 0) g.hoveredIdTimer = 0f
        g.hoveredIdPreviousFrame = g.hoveredId
        g.hoveredId = 0
        g.hoveredIdAllowOverlap = false
        if (!g.activeIdIsAlive && g.activeIdPreviousFrame == g.activeId && g.activeId != 0)
            clearActiveId()
        if (g.activeId != 0) g.activeIdTimer += IO.deltaTime
        g.activeIdPreviousFrame = g.activeId
        g.activeIdIsAlive = false
        g.activeIdIsJustActivated = false
        if (g.scalarAsInputTextId != 0 && g.activeId != g.scalarAsInputTextId)
            g.scalarAsInputTextId = 0

        // Update keyboard input state
        for (i in 0 until IO.keysDownDuration.size) IO.keysDownDurationPrev[i] = IO.keysDownDuration[i]
        for (i in 0 until IO.keysDown.size)
            IO.keysDownDuration[i] =
                    if (IO.keysDown[i])
                        if (IO.keysDownDuration[i] < 0f) 0f
                        else IO.keysDownDuration[i] + IO.deltaTime
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

        /*  Update mouse inputs state
            If mouse just appeared or disappeared (usually denoted by -Float.MAX_VALUE component, but in reality we test
            for -256000.0f) we cancel out movement in MouseDelta         */
        if (isMousePosValid(IO.mousePos) && isMousePosValid(IO.mousePosPrev))
            IO.mouseDelta = IO.mousePos - IO.mousePosPrev
        else
            IO.mouseDelta put 0f
        IO.mousePosPrev put IO.mousePos

        for (i in IO.mouseDown.indices) {
            IO.mouseClicked[i] = IO.mouseDown[i] && IO.mouseDownDuration[i] < 0f
            IO.mouseReleased[i] = !IO.mouseDown[i] && IO.mouseDownDuration[i] >= 0f
            IO.mouseDownDurationPrev[i] = IO.mouseDownDuration[i]
            IO.mouseDownDuration[i] = when (IO.mouseDown[i]) {
                true -> if (IO.mouseDownDuration[i] < 0f) 0f else IO.mouseDownDuration[i] + IO.deltaTime
                else -> -1f
            }
            IO.mouseDoubleClicked[i] = false
            if (IO.mouseClicked[i]) {
                if (g.time - IO.mouseClickedTime[i] < IO.mouseDoubleClickTime) {
                    if ((IO.mousePos - IO.mouseClickedPos[i]).lengthSqr < IO.mouseDoubleClickMaxDist * IO.mouseDoubleClickMaxDist)
                        IO.mouseDoubleClicked[i] = true
                    IO.mouseClickedTime[i] = -Float.MAX_VALUE   // so the third click isn't turned into a double-click
                } else
                    IO.mouseClickedTime[i] = g.time
                IO.mouseClickedPos[i] put IO.mousePos
                IO.mouseDragMaxDistanceSqr[i] = 0f
            } else if (IO.mouseDown[i]) {
                val mouseDelta = IO.mousePos - IO.mouseClickedPos[i]
                IO.mouseDragMaxDistanceAbs[i].x = max(IO.mouseDragMaxDistanceAbs[i].x, if (mouseDelta.x < 0f) -mouseDelta.x else mouseDelta.x)
                IO.mouseDragMaxDistanceAbs[i].y = max(IO.mouseDragMaxDistanceAbs[i].y, if (mouseDelta.y < 0f) -mouseDelta.y else mouseDelta.y)
                IO.mouseDragMaxDistanceSqr[i] = max(IO.mouseDragMaxDistanceSqr[i], mouseDelta.lengthSqr)
            }
        }

        // Calculate frame-rate for the user, as a purely luxurious feature
        g.framerateSecPerFrameAccum += IO.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
        g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = IO.deltaTime
        g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
        IO.framerate = 1.0f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size)

        /*  Handle user moving window with mouse (at the beginning of the frame to avoid input lag or sheering).
            Only valid for root windows.         */
        if (g.movingdWindowMoveId != 0 && g.movingdWindowMoveId == g.activeId) {
            keepAliveId(g.movingdWindowMoveId)
            assert(g.movingWindow != null)
            assert(g.movingWindow!!.moveId == g.movingdWindowMoveId)
            if (IO.mouseDown[0]) {
                val pos = IO.mousePos - g.activeIdClickOffset
                if (g.movingWindow!!.rootWindow!!.posF != pos)
                    markIniSettingsDirty(g.movingWindow!!.rootWindow!!)
                g.movingWindow!!.rootWindow!!.posF put pos
                g.movingWindow.focus()
            } else {
                clearActiveId()
                g.movingWindow = null
                g.movingdWindowMoveId = 0
            }
        } else {
            g.movingWindow = null
            g.movingdWindowMoveId = 0
        }

        // Delay saving settings so we don't spam disk too much
        if (g.settingsDirtyTimer > 0.0f) {
            g.settingsDirtyTimer -= IO.deltaTime
            if (g.settingsDirtyTimer <= 0.0f)
                saveIniSettingsToDisk(IO.iniFilename)
        }

        /*  Find the window we are hovering
            - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
            - When moving a window we can skip the search, which also conveniently bypasses the fact that window.windowRectClipped
                is lagging as this point.
            - We also support the moved window toggling the NoInputs flag after moving has started in order to be able to detect
                windows below it, which is useful for e.g. docking mechanisms.  */
        g.hoveredWindow = if (g.movingWindow?.flags?.hasnt(Wf.NoInputs) == true) g.movingWindow else findHoveredWindow(IO.mousePos)
        g.hoveredRootWindow = g.hoveredWindow?.rootWindow

        val modalWindow = frontMostModalRootWindow
        if (modalWindow != null) {
            g.modalWindowDarkeningRatio = glm.min(g.modalWindowDarkeningRatio + IO.deltaTime * 6f, 1f)
            g.hoveredRootWindow?.let {
                if (!it.isChildOf(modalWindow)) {
                    g.hoveredWindow = null
                    g.hoveredRootWindow = null
                }
            }
        } else g.modalWindowDarkeningRatio = 0f

        /*  Update the WantCaptureMouse/WantCAptureKeyboard flags, so user can capture/discard the inputs away from
            the rest of their application.
            When clicking outside of a window we assume the click is owned by the application and won't request capture.
            We need to track click ownership.   */
        var mouseEarliestButtonDown = -1
        var mouseAnyDown = false
        for (i in IO.mouseDown.indices) {
            if (IO.mouseClicked[i])
                IO.mouseDownOwned[i] = g.hoveredWindow != null || g.openPopupStack.isNotEmpty()
            mouseAnyDown = mouseAnyDown || IO.mouseDown[i]
            if (IO.mouseDown[i])
                if (mouseEarliestButtonDown == -1 || IO.mouseClickedTime[i] < IO.mouseClickedTime[mouseEarliestButtonDown])
                    mouseEarliestButtonDown = i
        }
        val mouseAvailToImgui = mouseEarliestButtonDown == -1 || IO.mouseDownOwned[mouseEarliestButtonDown]
        IO.wantCaptureMouse =
                if (g.wantCaptureMouseNextFrame != -1) g.wantCaptureMouseNextFrame != 0
                else (mouseAvailToImgui && (g.hoveredWindow != null || mouseAnyDown)) || g.openPopupStack.isNotEmpty()
        IO.wantCaptureKeyboard = if (g.wantCaptureKeyboardNextFrame != -1) g.wantCaptureKeyboardNextFrame != 0 else g.activeId != 0
        IO.wantTextInput = g.wantTextInputNextFrame != -1 && g.wantTextInputNextFrame != 0
        g.mouseCursor = MouseCursor.Arrow
        g.wantCaptureKeyboardNextFrame = -1
        g.wantCaptureMouseNextFrame = -1
        g.osImePosRequest put 1f // OS Input Method Editor showing on top-left of our window by default

        // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
        // FIXME: For patterns of drag and drop across OS windows, we may need to rework/remove this test (first committed 311c0ca9 on 2015/02)
        val mouseDraggingExternPayload = g.dragDropActive && g.dragDropSourceFlags has DragDropFlags.SourceExtern
        if (!mouseAvailToImgui && !mouseDraggingExternPayload) {
            g.hoveredRootWindow = null
            g.hoveredWindow = null
        }

        // Scale & Scrolling
        if (g.hoveredWindow != null && IO.mouseWheel != 0f && !g.hoveredWindow!!.collapsed) {
            val window = g.hoveredWindow!!
            if (IO.keyCtrl && IO.fontAllowUserScaling) {
                // Zoom / Scale window
                val newFontScale = glm.clamp(window.fontWindowScale + IO.mouseWheel * 0.10f, 0.50f, 2.50f)
                val scale = newFontScale / window.fontWindowScale
                window.fontWindowScale = newFontScale

                val offset = window.size * (1.0f - scale) * (IO.mousePos - window.pos) / window.size
                window.pos plusAssign offset
                window.posF plusAssign offset
                window.size timesAssign scale
                window.sizeFull timesAssign scale
            } else if (!IO.keyCtrl) {
                // Mouse wheel Scrolling
                // If a child window has the ImGuiWindowFlags_NoScrollWithMouse flag, we give a chance to scroll its parent (unless either ImGuiWindowFlags_NoInputs or ImGuiWindowFlags_NoScrollbar are also set).
                var scrollWindow = window
                while (scrollWindow.flags has Wf.ChildWindow && scrollWindow.flags has Wf.NoScrollWithMouse &&
                        scrollWindow.flags hasnt Wf.NoScrollbar && scrollWindow.flags hasnt Wf.NoInputs && scrollWindow.parentWindow != null)
                    scrollWindow = scrollWindow.parentWindow!!

                if (scrollWindow.flags hasnt Wf.NoScrollWithMouse && scrollWindow.flags hasnt Wf.NoInputs) {
                    var scrollAmount = 5 * scrollWindow.calcFontSize()
                    scrollAmount = min(scrollAmount,
                            (scrollWindow.contentsRegionRect.height + scrollWindow.windowPadding.y * 2f) * 0.67f).i.f
                    scrollWindow.setScrollY(scrollWindow.scroll.y - IO.mouseWheel * scrollAmount)
                }
            }
        }

        // Pressing TAB activate widget focus
        if (g.activeId == 0 && g.navWindow != null && g.navWindow!!.active && Key.Tab.isPressed(false))
            g.navWindow!!.focusIdxTabRequestNext = 0

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
        if (g.navWindow != null && !g.navWindow!!.wasActive) focusPreviousWindow()

        /*  No window should be open at the beginning of the frame.
            But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an
            explicit clear. */
        g.currentWindowStack.clear()
        g.currentPopupStack.clear()
        closeInactivePopups(g.navWindow)

        // Create implicit window - we will only render it if the user has added something to it.
        setNextWindowSize(Vec2(400), Cond.FirstUseEver)
        begin("Debug##Default")
    }


    /** ends the ImGui frame, finalize rendering data, then call your io.RenderDrawListsFn() function if set.   */
    fun render() {

        assert(g.initialized)   // Forgot to call ImGui::NewFrame()

        if (g.frameCountEnded != g.frameCount) endFrame()
        g.frameCountRendered = g.frameCount

        /*  Skip render altogether if alpha is 0.0
            Note that vertex buffers have been created and are wasted, so it is best practice that you don't create
            windows in the first place, or consistently respond to Begin() returning false. */
        if (style.alpha > 0f) {
            // Gather windows to render
            IO.metricsActiveWindows = 0
            IO.metricsRenderIndices = 0
            IO.metricsRenderVertices = 0
            g.renderDrawLists.forEach { it.clear() }
            g.windows.filter { it.active && it.hiddenFrames <= 0 && it.flags hasnt Wf.ChildWindow }
                    .map(Window::addToRenderListSelectLayer)

            // Flatten layers
            for (i in 1 until g.renderDrawLists.size)
                g.renderDrawLists[0].addAll(g.renderDrawLists[i])

            // Draw software mouse cursor if requested
            if (IO.mouseDrawCursor) {
                val cursorData = g.mouseCursorData[g.mouseCursor.i]
                val pos = IO.mousePos - cursorData.hotOffset
                val size = cursorData.size
                val texId = IO.fonts.texId
                g.overlayDrawList.pushTextureId(texId)
                g.overlayDrawList.addImage(texId, pos + Vec2(1, 0), pos + Vec2(1, 0) + size,
                        cursorData.texUvMin[1], cursorData.texUvMax[1], COL32(0, 0, 0, 48))        // Shadow
                g.overlayDrawList.addImage(texId, pos + Vec2(2, 0), pos + Vec2(2, 0) + size,
                        cursorData.texUvMin[1], cursorData.texUvMax[1], COL32(0, 0, 0, 48))        // Shadow
                g.overlayDrawList.addImage(texId, pos, pos + size, cursorData.texUvMin[1], cursorData.texUvMax[1],
                        COL32(0, 0, 0, 255))       // Black border
                g.overlayDrawList.addImage(texId, pos, pos + size, cursorData.texUvMin[0], cursorData.texUvMax[0],
                        COL32(255, 255, 255, 255)) // White fill
                g.overlayDrawList.popTextureId()
            }
            if (g.overlayDrawList.vtxBuffer.isNotEmpty())
                g.overlayDrawList addTo g.renderDrawLists[0]

            // Setup draw data
            g.renderDrawData.valid = true
            g.renderDrawData.cmdLists.clear()
            g.renderDrawData.cmdLists.addAll(g.renderDrawLists[0])
            g.renderDrawData.cmdListsCount = g.renderDrawLists[0].size
            g.renderDrawData.totalVtxCount = IO.metricsRenderVertices
            g.renderDrawData.totalIdxCount = IO.metricsRenderIndices

            // Render. If user hasn't set a callback then they may retrieve the draw data via GetDrawData()
            if (g.renderDrawData.cmdListsCount > 0 && IO.renderDrawListsFn != null)
                IO.renderDrawListsFn!!(g.renderDrawData)
        }
    }

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
        if (/*IO.imeSetInputScreenPosFn &&*/ (g.osImePosRequest - g.osImePosSet).lengthSqr > 0.0001f) {
//            (LwjglGL3.windowProc!! as WindowProc).in
//            g.IO.ImeSetInputScreenPosFn((int) g . OsImePosRequest . x, (int) g . OsImePosRequest . y)
            g.osImePosSet put g.osImePosRequest
        }

        // Hide implicit "Debug" window if it hasn't been used
        assert(g.currentWindowStack.size == 1)    // Mismatched Begin()/End() calls
        g.currentWindow?.let {
            if (!it.writeAccessed) it.active = false
        }

        end()

        if (g.activeId == 0 && g.hoveredId == 0)
            if (g.navWindow == null || !g.navWindow!!.appearing) { // Unless we just made a window/popup appear
                // Click to focus window and start moving (after we're done with all our widgets)
                if (IO.mouseClicked[0])
                    if (g.hoveredRootWindow != null) {
                        g.hoveredWindow.focus()
                        if (g.hoveredWindow!!.flags hasnt Wf.NoMove && g.hoveredRootWindow!!.flags hasnt Wf.NoMove) {
                            g.movingWindow = g.hoveredWindow
                            g.movingdWindowMoveId = g.hoveredWindow!!.moveId
                            setActiveId(g.movingdWindowMoveId, g.hoveredRootWindow)
                            g.activeIdClickOffset = IO.mousePos - g.movingWindow!!.rootWindow!!.pos
                        }
                    } else if (g.navWindow != null && frontMostModalRootWindow == null)
                        null.focus()   // Clicking on void disable focus

                /*  With right mouse button we close popups without changing focus
                (The left mouse button path calls FocusWindow which will lead NewFrame->CloseInactivePopups to trigger) */
                if (IO.mouseClicked[1]) {
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
                    closeInactivePopups(if (hoveredWindowAboveModal) g.hoveredWindow else modal)
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
        IO.mouseWheel = 0f
        IO.inputCharacters.fill(NUL)

        g.frameCountEnded = g.frameCount
    }

    /** This function is merely here to free heap allocations.     */
    fun shutdown() {

        /*  The fonts atlas can be used prior to calling NewFrame(), so we clear it even if g.Initialized is FALSE
            (which would happen if we never called NewFrame)         */
//        if (IO.fonts) // Testing for NULL to allow user to NULLify in case of running Shutdown() on multiple contexts. Bit hacky.
        IO.fonts.clear()

        // Cleanup of other data are conditional on actually having initialize ImGui.
        if (!g.initialized) return

        saveIniSettingsToDisk(IO.iniFilename)

        for (window in g.windows) window.clear()
        g.windows.clear()
        g.windowsSortBuffer.clear()
        g.currentWindow = null
        g.currentWindowStack.clear()
        g.windowsById.clear()
        g.navWindow = null
        g.hoveredWindow = null
        g.hoveredRootWindow = null
        g.activeIdWindow = null
        g.movingWindow = null
        g.settings.clear()
        g.colorModifiers.clear()
        g.styleModifiers.clear()
        g.fontStack.clear()
        g.openPopupStack.clear()
        g.currentPopupStack.clear()
        g.setNextWindowSizeConstraintCallback = null
        g.setNextWindowSizeConstraintCallbackUserData = null
        g.renderDrawLists.forEach { it.clear() }
        g.overlayDrawList.clearFreeMemory()
        g.privateClipboard = ""
        g.inputTextState.text = charArrayOf()
        g.inputTextState.initialText = charArrayOf()
        g.inputTextState.tempTextBuffer = charArrayOf()

//        if (g.logFile != null && g.logFile != stdout) { TODO
//            fclose(g.LogFile)
//            g.LogFile = NULL
//        }
        g.logClipboard.setLength(0)

        g.initialized = false
    }

    companion object {
        fun focusPreviousWindow() {
            for (i in g.windows.lastIndex downTo 0)
                if (g.windows[i].wasActive && g.windows[i].flags hasnt Wf.ChildWindow) {
                    g.windows[i].focus()
                    return
                }
        }
    }
}