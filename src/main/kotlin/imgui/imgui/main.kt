package imgui.imgui

import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.clearActiveId
import imgui.ImGui.focusWindow
import imgui.ImGui.keepAliveId
import imgui.internal.lengthSqr
import imgui.Context as g

interface imgui_main {

    val style get() = g.style

    /** Same value as passed to your RenderDrawListsFn() function. valid after Render() and
     *  until the next call to NewFrame()   */
    val drawData get() = if (g.renderDrawData.valid) g.renderDrawData else null

    /** start a new ImGui frame, you can submit any command from this point until NewFrame()/Render().  */
    fun newFrame() {

        ptrIndices = 0

        // Check user data
        assert(IO.deltaTime >= 0f)  // Need a positive DeltaTime (zero is tolerated but will cause some timing issues)
        assert(IO.displaySize.x >= 0f && IO.displaySize.y >= 0f)
        assert(IO.fonts.fonts.size > 0) // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
        assert(IO.fonts.fonts[0].isLoaded)  // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
        assert(style.curveTessellationTol > 0f)  // Invalid style setting
        // Invalid style setting. Alpha cannot be negative (allows us to avoid a few clamps in color computations)
        assert(style.alpha in 0f..1f)

        if (!g.initialized) {
            // Initialize on first frame TODO
            g.logClipboard = StringBuilder()

            assert(g.settings.isEmpty())
            loadIniSettingsFromDisk(IO.iniFilename)
            g.initialized = true
        }

        defaultFont.setCurrent()
        assert(g.font.isLoaded)

        g.time += IO.deltaTime
        g.frameCount += 1
        g.tooltipOverrideCount = 0
        g.overlayDrawList.clear()
        g.overlayDrawList.pushTextureId(IO.fonts.texId)
        g.overlayDrawList.pushClipRectFullScreen()

        // Mark rendering data as invalid to prevent user who may have a handle on it to use it
        g.renderDrawData.valid = false
        g.renderDrawData.cmdLists.clear()
        g.renderDrawData.totalIdxCount = 0
        g.renderDrawData.totalVtxCount = 0
        g.renderDrawData.cmdListsCount = 0

        // Update inputs state
        if (IO.mousePos.x < 0 && IO.mousePos.y < 0)
            IO.mousePos put -9999.0f
        // if mouse just appeared or disappeared (negative coordinate) we cancel out movement in MouseDelta
        if ((IO.mousePos.x < 0 && IO.mousePos.y < 0) || (IO.mousePosPrev.x < 0 && IO.mousePosPrev.y < 0))
            IO.mouseDelta put 0f
        else
            IO.mouseDelta = IO.mousePos - IO.mousePosPrev
        IO.mousePosPrev put IO.mousePos

        for (i in IO.mouseDown.indices) {
            IO.mouseClicked[i] = IO.mouseDown[i] && IO.mouseDownDuration[i] < 0f
            IO.mouseReleased[i] = !IO.mouseDown[i] && IO.mouseDownDuration[i] >= 0f
            IO.mouseDownDurationPrev[i] = IO.mouseDownDuration[i]
            IO.mouseDownDuration[i] =
                    if (IO.mouseDown[i]) {
                        if (IO.mouseDownDuration[i] < 0f) 0f
                        else IO.mouseDownDuration[i] + IO.deltaTime
                    } else -1.0f
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
            } else if (IO.mouseDown[i])
                IO.mouseDragMaxDistanceSqr[i] = glm.max(IO.mouseDragMaxDistanceSqr[i], (IO.mousePos - IO.mouseClickedPos[i]).lengthSqr)
        }
        for (i in IO.keysDownDuration.indices)
            IO.keysDownDurationPrev[i] = IO.keysDownDuration[i]
        for (i in IO.keysDown.indices)
            IO.keysDownDuration[i] =
                    if (IO.keysDown[i])
                        if (IO.keysDownDuration[i] < 0f) 0f
                        else IO.keysDownDuration[i] + IO.deltaTime
                    else -1f

        // Calculate frame-rate for the user, as a purely luxurious feature
        g.framerateSecPerFrameAccum += IO.deltaTime - g.framerateSecPerFrame[g.framerateSecPerFrameIdx]
        g.framerateSecPerFrame[g.framerateSecPerFrameIdx] = IO.deltaTime
        g.framerateSecPerFrameIdx = (g.framerateSecPerFrameIdx + 1) % g.framerateSecPerFrame.size
        IO.framerate = 1.0f / (g.framerateSecPerFrameAccum / g.framerateSecPerFrame.size)

        // Clear reference to active widget if the widget isn't alive anymore
        g.hoveredIdPreviousFrame = g.hoveredId
        g.hoveredId = 0
        g.hoveredIdAllowOverlap = false
        if (!g.activeIdIsAlive && g.activeIdPreviousFrame == g.activeId && g.activeId != 0)
            clearActiveId()
        g.activeIdPreviousFrame = g.activeId
        g.activeIdIsAlive = false
        g.activeIdIsJustActivated = false

        // Handle user moving window (at the beginning of the frame to avoid input lag or sheering). Only valid for root windows.
        if (g.movedWindowMoveId != 0 && g.movedWindowMoveId == g.activeId) {
            keepAliveId(g.movedWindowMoveId)
            assert(g.movedWindow != null)
            assert(g.movedWindow!!.rootWindow.moveId == g.movedWindowMoveId)
            if (IO.mouseDown[0]) {
                if (g.movedWindow!!.flags hasnt WindowFlags.NoMove) {
                    g.movedWindow!!.posF plus_ IO.mouseDelta
                    if (g.movedWindow!!.flags hasnt WindowFlags.NoSavedSettings && (IO.mouseDelta.x != 0.0f || IO.mouseDelta.y != 0.0f))
                        markIniSettingsDirty()
                }
                focusWindow(g.movedWindow)
            } else {
                clearActiveId()
                g.movedWindow = null
                g.movedWindowMoveId = 0
            }
        } else {
            g.movedWindow = null
            g.movedWindowMoveId = 0
        }

        // Delay saving settings so we don't spam disk too much
        if (g.settingsDirtyTimer > 0.0f) {
            g.settingsDirtyTimer -= IO.deltaTime
            if (g.settingsDirtyTimer <= 0.0f)
                saveIniSettingsToDisk(IO.iniFilename)
        }

        // Find the window we are hovering. Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow
        g.hoveredWindow = g.movedWindow ?: findHoveredWindow(IO.mousePos, false)
        if (g.hoveredWindow != null && g.hoveredWindow!!.flags has WindowFlags.ChildWindow)
            g.hoveredRootWindow = g.hoveredWindow!!.rootWindow
        else
            g.hoveredRootWindow = g.movedWindow?.rootWindow ?: findHoveredWindow(IO.mousePos, true)

        val modalWindow = getFrontMostModalRootWindow()
        if (modalWindow != null) {
            g.modalWindowDarkeningRatio = glm.min(g.modalWindowDarkeningRatio + IO.deltaTime * 6f, 1f)
            var window = g.hoveredRootWindow
            while (window != null && window != modalWindow)
                window = window.parentWindow
            if (window == null) {
                g.hoveredWindow = null
                g.hoveredRootWindow = null
            }
        } else g.modalWindowDarkeningRatio = 0f

        /** Are we using inputs? Tell user so they can capture/discard the inputs away from the rest of their application.
        When clicking outside of a window we assume the click is owned by the application and won't request capture.
        We need to track click ownership.   */
        var mouseEarliestButtonDown = -1
        var mouseAnyDown = false
        for (i in IO.mouseDown.indices) {
            if (IO.mouseClicked[i])
                IO.mouseDownOwned[i] = g.hoveredWindow != null || g.openPopupStack.isNotEmpty()
            mouseAnyDown = mouseAnyDown || IO.mouseDown[i]
            if (IO.mouseDown[i])
                if (mouseEarliestButtonDown == -1 || IO.mouseClickedTime[mouseEarliestButtonDown] > IO.mouseClickedTime[i])
                    mouseEarliestButtonDown = i
        }
        val mouseAvailToImgui = mouseEarliestButtonDown == -1 || IO.mouseDownOwned[mouseEarliestButtonDown]
        if (g.captureMouseNextFrame != -1)
            IO.wantCaptureMouse = g.captureMouseNextFrame != 0
        else
            IO.wantCaptureMouse = (mouseAvailToImgui && (g.hoveredWindow != null || mouseAnyDown)) || g.activeId != 0 ||
                    g.openPopupStack.isNotEmpty()
        IO.wantCaptureKeyboard =
                if (g.captureKeyboardNextFrame != -1) g.captureKeyboardNextFrame != 0
                else g.activeId != 0
        IO.wantTextInput = g.activeId != 0 //&& g.inputTextState.id == g.activeId TODO
        g.mouseCursor = MouseCursor.Arrow
        g.captureKeyboardNextFrame = -1
        g.captureMouseNextFrame = -1
        g.osImePosRequest put 1f // OS Input Method Editor showing on top-left of our window by default

        // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
        if (!mouseAvailToImgui) {
            g.hoveredRootWindow = null
            g.hoveredWindow = null
        }

        // Scale & Scrolling
        if (g.hoveredWindow != null && IO.mouseWheel != 0f && !g.hoveredWindow!!.collapsed) {
            val window = g.hoveredWindow!!
            if (IO.keyCtrl) {
                if (IO.fontAllowUserScaling) {
                    // Zoom / Scale window
                    val newFontScale = glm.clamp(window.fontWindowScale + IO.mouseWheel * 0.10f, 0.50f, 2.50f)
                    val scale = newFontScale / window.fontWindowScale
                    window.fontWindowScale = newFontScale

                    val offset = window.size * (1.0f - scale) * (IO.mousePos - window.pos) / window.size
                    window.pos plus_ offset
                    window.posF plus_ offset
                    window.size times_ scale
                    window.sizeFull times_ scale
                }
            } else if (window.flags hasnt WindowFlags.NoScrollWithMouse) {
                // Scroll
                val scrollLines = if (window.flags has WindowFlags.ComboBox) 3 else 5
                window.setScrollY(window.scroll.y - IO.mouseWheel * window.calcFontSize() * scrollLines)
            }
        }

        /*  Pressing TAB activate widget focus
            NB: Don't discard FocusedWindow if it isn't active, so that a window that go on/off programatically won't lose
            its keyboard focus.     */
        if (g.activeId == 0 && g.focusedWindow != null && g.focusedWindow!!.active && Key.Tab.isPressed(false))
            g.focusedWindow!!.focusIdxTabRequestNext = 0

        // Mark all windows as not visible
        var i = 0
        while (i != g.windows.size) {
            val window = g.windows[i]
            window.wasActive = window.active
            window.active = false
            window.accessed = false
            i++
        }

        // Closing the focused window restore focus to the first active root window in descending z-order
        if (g.focusedWindow != null && !g.focusedWindow!!.wasActive)
            for (i in g.windows.size - 1 downTo 0)
                if (g.windows[i].wasActive && g.windows[i].flags hasnt WindowFlags.ChildWindow) {
                    focusWindow(g.windows[i])
                    break
                }

        /*  No window should be open at the beginning of the frame.
            But in order to allow the user to call NewFrame() multiple times without calling Render(), we are doing an
            explicit clear. */
        g.currentWindowStack.clear()
        g.currentPopupStack.clear()
        closeInactivePopups()

        // Create implicit window - we will only render it if the user has added something to it.
        ImGui.setNextWindowSize(Vec2(400, 400), Cond.FirstUseEver)
        ImGui.begin("Debug")
    }


    /** ends the ImGui frame, finalize rendering data, then call your io.RenderDrawListsFn() function if set.   */
    fun render() {

        assert(g.initialized)   // Forgot to call ImGui::NewFrame()

        if (g.frameCountEnded != g.frameCount)
            ImGui.endFrame()
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
            g.windows.forEach {
                if (it.active && it.hiddenFrames <= 0 && it.flags hasnt WindowFlags.ChildWindow) {
                    // FIXME: Generalize this with a proper layering system so e.g. user can draw in specific layers, below text, ..
                    IO.metricsActiveWindows++
                    it addTo when {
                        it.flags has WindowFlags.Popup -> g.renderDrawLists[1]
                        it.flags has WindowFlags.Tooltip -> g.renderDrawLists[2]
                        else -> g.renderDrawLists[0]
                    }
                }
            }

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

    /** NB: behavior of ImGui after Shutdown() is not tested/guaranteed at the moment. This function is merely here to
    free heap allocations, nothing to do here on JVM     */
    fun shutdown() {

        /*  The fonts atlas can be used prior to calling NewFrame(), so we clear it even if g.Initialized is FALSE
            (which would happen if we never called NewFrame)         */
//        if (IO.fonts) // Testing for NULL to allow user to NULLify in case of running Shutdown() on multiple contexts. Bit hacky.
        IO.fonts.clear()

        // Cleanup of other data are conditional on actually having used ImGui.
        if (!g.initialized) return

        saveIniSettingsToDisk(IO.iniFilename)

        for (window in g.windows) window.clear()
        g.windows.clear()
        g.windowsSortBuffer.clear()
        g.currentWindow = null
        g.currentWindowStack.clear()
        g.focusedWindow = null
        g.hoveredWindow = null
        g.hoveredRootWindow = null
        g.activeIdWindow = null
        g.movedWindow = null
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

//    IMGUI_API void          ShowUserGuide();                            // help block
//    IMGUI_API void          ShowStyleEditor(ImGuiStyle* ref = NULL);    // style editor block. you can pass in a reference ImGuiStyle structure to compare to, revert to and save to (else it uses the default style)
//    IMGUI_API void          ShowTestWindow(bool* p_open = NULL);        // test window demonstrating ImGui features
//    IMGUI_API void          ShowMetricsWindow(bool* p_open = NULL);     // metrics window for debugging ImGui (browse draw commands, individual vertices, window list, etc.)
}