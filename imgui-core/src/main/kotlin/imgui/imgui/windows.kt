package imgui.imgui

import gli_.has
import gli_.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.currentWindow
import imgui.ImGui.endColumns
import imgui.ImGui.findBestWindowPosForPopup
import imgui.ImGui.findWindowByName
import imgui.ImGui.frontMostPopupModal
import imgui.ImGui.getColorU32
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.navInitWindow
import imgui.ImGui.popClipRect
import imgui.ImGui.pushClipRect
import imgui.ImGui.style
import imgui.imgui.imgui_main.Companion.clampWindowRect
import imgui.imgui.imgui_main.Companion.renderWindowDecorations
import imgui.imgui.imgui_main.Companion.renderWindowTitleBarContents
import imgui.imgui.imgui_main.Companion.updateManualResize
import imgui.internal.*
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.DrawListFlag as Dlf
import imgui.internal.ItemFlag as If
import imgui.internal.LayoutType as Lt


/** - Begin() = push window to the stack and start appending to it. End() = pop window from the stack.
 *  - You may append multiple times to the same window during the same frame.
 *  - Passing 'bool* p_open != NULL' shows a window-closing widget in the upper-right corner of the window,
 *      which clicking will set the boolean to false when clicked.
 *  - Begin() return false to indicate the window is collapsed or fully clipped, so you may early out and omit submitting anything to the window.
 *    Always call a matching End() for each Begin() call, regardless of its return value
 *    [this is due to legacy reason and is inconsistent with most other functions such as BeginMenu/EndMenu, BeginPopup/EndPopup, etc.
 *    where the EndXXX call should only be called if the corresponding BeginXXX function returned true.]
 *
 *
 *    Windows   */
interface imgui_windows {

    fun begin_(name: String, pOpen: BooleanArray? = null, index: Int = 0, flags: WindowFlags = 0) = when (pOpen) {
        null -> begin(name, null, flags)
        else -> withBoolean(pOpen, 0) { begin(name, it, flags) }
    }

    /**  Push a new Dear ImGui window to add widgets to:
    - A default window called "Debug" is automatically stacked at the beginning of every frame so you can use
    widgets without explicitly calling a Begin/End pair.
    - Begin/End can be called multiple times during the frame with the same window name to append content.
    - The window name is used as a unique identifier to preserve window information across frames (and save
    rudimentary information to the .ini file).
    You can use the "##" or "###" markers to use the same label with different id, or same id with different
    label. See documentation at the top of this file.
    - Return false when window is collapsed (so you can early out in your code) but you always need to call End()
    regardless. You always need to call ImGui::End() even if false is returned.
    - Passing 'bool* p_open' displays a Close button on the upper-right corner of the window, the pointed value will
    be set to false when the button is pressed.

    @return isOpen
     */
    fun begin(name: String, pOpen: KMutableProperty0<Boolean>? = null, flags_: WindowFlags = 0): Boolean {

        assert(name.isNotEmpty()) { "Window name required" }
        assert(g.frameScopeActive) { "Forgot to call ImGui::newFrame()" }
        assert(g.frameCountEnded != g.frameCount) { "Called ImGui::render() or ImGui::rndFrame() and haven't called ImGui::newFrame() again yet" }

        var flags = flags_

        // Find or create
        var windowJustCreated = false
        val window = findWindowByName(name) ?: run {
            // Any condition flag will do since we are creating a new window here.
            val sizeOnFirstUse = when {
                g.nextWindowData.flags has NextWindowDataFlag.HasSize -> Vec2(g.nextWindowData.sizeVal)
                else -> Vec2()
            }
            windowJustCreated = true
            createNewWindow(name, sizeOnFirstUse, flags)
        }

        // Automatically disable manual moving/resizing when NoInputs is set
        if ((flags and Wf.NoInputs) == Wf.NoInputs.i)
            flags = flags or Wf.NoMove or Wf.NoResize

        if (flags has Wf.NavFlattened)
            assert(flags has Wf.ChildWindow)

        val currentFrame = g.frameCount
        val firstBeginOfTheFrame = window.lastFrameActive != currentFrame

        // Update the Appearing flag
        // Not using !WasActive because the implicit "Debug" window would always toggle off->on
        var windowJustActivatedByUser = window.lastFrameActive < currentFrame - 1
        val windowJustAppearingAfterHiddenForResize = window.hiddenFramesCannotSkipItems > 0
        if (flags has Wf.Popup) {
            val popupRef = g.openPopupStack[g.beginPopupStack.size]
            // We recycle popups so treat window as activated if popup id changed
            windowJustActivatedByUser = windowJustActivatedByUser || window.popupId != popupRef.popupId
            windowJustActivatedByUser = windowJustActivatedByUser || window !== popupRef.window
        }
        window.appearing = windowJustActivatedByUser || windowJustAppearingAfterHiddenForResize
        if (window.appearing) window.setConditionAllowFlags(Cond.Appearing.i, true)

        // Update Flags, LastFrameActive, BeginOrderXXX fields
        if (firstBeginOfTheFrame) {
            window.flags = flags
            window.lastFrameActive = currentFrame
            window.beginOrderWithinParent = 0
            window.beginOrderWithinContext = g.windowsActiveCount++
        } else
            flags = window.flags

        // Parent window is latched only on the first call to Begin() of the frame, so further append-calls can be done from a different window stack
        val parentWindowInStack = g.currentWindowStack.lastOrNull()
        val parentWindow = when {
            firstBeginOfTheFrame -> parentWindowInStack.takeIf { flags has (Wf.ChildWindow or Wf.Popup) }
            else -> window.parentWindow
        }
        assert(parentWindow != null || flags hasnt Wf.ChildWindow)

        // Add to stack
        // We intentionally set g.CurrentWindow to NULL to prevent usage until when the viewport is set, then will call SetCurrentWindow()
        g.currentWindowStack += window
        g.currentWindow = null
        checkStacksSize(window, true)
        if (flags has Wf.Popup) {
            val popupRef = g.openPopupStack[g.beginPopupStack.size]
            popupRef.window = window
            g.beginPopupStack.push(popupRef)
            window.popupId = popupRef.popupId
        }

        if (windowJustAppearingAfterHiddenForResize && flags hasnt Wf.ChildWindow)
            window.navLastIds[0] = 0

        // Process SetNextWindow***() calls
        var windowPosSetByApi = false
        var windowSizeXsetByApi = false
        var windowSizeYsetByApi = false
        if (g.nextWindowData.flags has NextWindowDataFlag.HasPos) {
            windowPosSetByApi = window.setWindowPosAllowFlags has g.nextWindowData.posCond
            if (windowPosSetByApi && g.nextWindowData.posPivotVal.lengthSqr > 0.00001f) {
                /*  May be processed on the next frame if this is our first frame and we are measuring size
                    FIXME: Look into removing the branch so everything can go through this same code path for consistency.  */
                window.setWindowPosVal put g.nextWindowData.posVal
                window.setWindowPosPivot put g.nextWindowData.posPivotVal
                window.setWindowPosAllowFlags = window.setWindowPosAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()
            } else
                window.setPos(g.nextWindowData.posVal, g.nextWindowData.posCond)
        }
        if (g.nextWindowData.flags has NextWindowDataFlag.HasSize) {
            windowSizeXsetByApi = window.setWindowSizeAllowFlags has g.nextWindowData.sizeCond && g.nextWindowData.sizeVal.x > 0f
            windowSizeYsetByApi = window.setWindowSizeAllowFlags has g.nextWindowData.sizeCond && g.nextWindowData.sizeVal.y > 0f
            window.setSize(g.nextWindowData.sizeVal, g.nextWindowData.sizeCond)
        }
        if (g.nextWindowData.flags has NextWindowDataFlag.HasContentSize) {
            // Adjust passed "client size" to become a "window size"
            window.sizeContentsExplicit put g.nextWindowData.contentSizeVal
            if (window.sizeContentsExplicit.y != 0f)
                window.sizeContentsExplicit.y += window.titleBarHeight + window.menuBarHeight
        } else if (firstBeginOfTheFrame)
            window.sizeContentsExplicit put 0f
        if (g.nextWindowData.flags has NextWindowDataFlag.HasCollapsed)
            window.setCollapsed(g.nextWindowData.collapsedVal, g.nextWindowData.collapsedCond)
        if (g.nextWindowData.flags has NextWindowDataFlag.HasFocus)
            window.focus()
        if (window.appearing)
            window.setConditionAllowFlags(Cond.Appearing.i, false)

        // When reusing window again multiple times a frame, just append content (don't need to setup again)
        if (firstBeginOfTheFrame) {
            // Initialize
            val windowIsChildTooltip = flags has Wf.ChildWindow && flags has Wf.Tooltip // FIXME-WIP: Undocumented behavior of Child+Tooltip for pinned tooltip (#1345)
            window.updateParentAndRootLinks(flags, parentWindow)

            window.active = true
            window.hasCloseButton = pOpen != null
            window.clipRect.put(-Float.MAX_VALUE, -Float.MAX_VALUE, +Float.MAX_VALUE, +Float.MAX_VALUE)
            for (i in 1 until window.idStack.size) window.idStack.pop()  // resize 1

            // Update stored window name when it changes (which can _only_ happen with the "###" operator, so the ID would stay unchanged).
            // The title bar always display the 'name' parameter, so we only update the string storage if it needs to be visible to the end-user elsewhere.
            var windowTitleVisibleElsewhere = false
            if (g.navWindowingList.isNotEmpty() && window.flags hasnt Wf.NoNavFocus)   // Window titles visible when using CTRL+TAB
                windowTitleVisibleElsewhere = true
            if (windowTitleVisibleElsewhere && !windowJustCreated && name != window.name) {
//                val buf_len = (size_t)window->NameBufLen
//                window->Name = ImStrdupcpy(window->Name, &buf_len, name)
//                window->NameBufLen = (int)buf_len
                window.name = name
                window.nameBufLen = name.length
            }

            // UPDATE CONTENTS SIZE, UPDATE HIDDEN STATUS

            // Update contents size from last frame for auto-fitting (or use explicit size)
            window.sizeContents = window.calcSizeContents()
            if (window.hiddenFramesCanSkipItems > 0)
                window.hiddenFramesCanSkipItems--
            if (window.hiddenFramesCannotSkipItems > 0)
                window.hiddenFramesCannotSkipItems--

            // Hide new windows for one frame until they calculate their size
            if (windowJustCreated && (!windowSizeXsetByApi || !windowSizeYsetByApi))
                window.hiddenFramesCannotSkipItems = 1

            /*  Hide popup/tooltip window when re-opening while we measure size (because we recycle the windows)
                We reset Size/SizeContents for reappearing popups/tooltips early in this function,
                so further code won't be tempted to use the old size.             */
            if (windowJustActivatedByUser && flags has (Wf.Popup or Wf.Tooltip)) {
                window.hiddenFramesCannotSkipItems = 1
                if (flags has Wf.AlwaysAutoResize) {
                    if (!windowSizeXsetByApi) {
                        window.sizeFull.x = 0f
                        window.size.x = 0f
                    }
                    if (!windowSizeYsetByApi) {
                        window.size.y = 0f
                        window.sizeFull.y = 0f
                    }
                    window.sizeContents put 0f
                }
            }

            setCurrentWindow(window)

            // Lock border size and padding for the frame (so that altering them doesn't cause inconsistencies)
            window.windowBorderSize = when {
                flags has Wf.ChildWindow -> style.childBorderSize
                flags has (Wf.Popup or Wf.Tooltip) && flags hasnt Wf.Modal -> style.popupBorderSize
                else -> style.windowBorderSize
            }
            window.windowPadding put style.windowPadding
            if (flags has Wf.ChildWindow && !(flags has (Wf.AlwaysUseWindowPadding or Wf.Popup)) && window.windowBorderSize == 0f)
                window.windowPadding.put(0f, if (flags has Wf.MenuBar) style.windowPadding.y else 0f)
            window.dc.menuBarOffset.x = max(max(window.windowPadding.x, style.itemSpacing.x), g.nextWindowData.menuBarOffsetMinVal.x)
            window.dc.menuBarOffset.y = g.nextWindowData.menuBarOffsetMinVal.y

            /* Collapse window by double-clicking on title bar
            At this point we don't have a clipping rectangle setup yet, so we can use the title bar area for hit
            detection and drawing   */
            if (flags hasnt Wf.NoTitleBar && flags hasnt Wf.NoCollapse) {
                /*  We don't use a regular button+id to test for double-click on title bar (mostly due to legacy reason, could be fixed),
                    so verify that we don't have items over the title bar.                 */
                val titleBarRect = window.titleBarRect()
                if (g.hoveredWindow === window && g.hoveredId == 0 && g.hoveredIdPreviousFrame == 0 &&
                        isMouseHoveringRect(titleBarRect.min, titleBarRect.max) && io.mouseDoubleClicked[0])
                    window.wantCollapseToggle = true
                if (window.wantCollapseToggle) {
                    window.collapsed = !window.collapsed
                    window.markIniSettingsDirty()
                    window.focus()
                }
            } else window.collapsed = false
            window.wantCollapseToggle = false

            /* ---------- SIZE ---------- */

            // Calculate auto-fit size, handle automatic resize
            val sizeAutoFit = window.calcSizeAutoFit(window.sizeContents)
            val sizeFullModified = Vec2(Float.MAX_VALUE)
            if (flags has Wf.AlwaysAutoResize && !window.collapsed) {
                // Using SetNextWindowSize() overrides ImGuiWindowFlags_AlwaysAutoResize, so it can be used on tooltips/popups, etc.
                if (!windowSizeXsetByApi) {
                    sizeFullModified.x = sizeAutoFit.x
                    window.sizeFull.x = sizeAutoFit.x
                }
                if (!windowSizeYsetByApi) {
                    sizeFullModified.y = sizeAutoFit.y
                    window.sizeFull.y = sizeAutoFit.y
                }
            } else if (window.autoFitFrames.x > 0 || window.autoFitFrames.y > 0) {
                /*  Auto-fit may only grow window during the first few frames
                    We still process initial auto-fit on collapsed windows to get a window width,
                    but otherwise don't honor WindowFlag.AlwaysAutoResize when collapsed.                 */
                if (!windowSizeXsetByApi && window.autoFitFrames.x > 0) {
                    sizeFullModified.x = if (window.autoFitOnlyGrows) max(window.sizeFull.x, sizeAutoFit.x) else sizeAutoFit.x
                    window.sizeFull.x = sizeFullModified.x
                }
                if (!windowSizeYsetByApi && window.autoFitFrames.y > 0) {
                    sizeFullModified.y = if (window.autoFitOnlyGrows) max(window.sizeFull.y, sizeAutoFit.y) else sizeAutoFit.y
                    window.sizeFull.y = sizeFullModified.y
                }
                if (!window.collapsed) window.markIniSettingsDirty()
            }

            // Apply minimum/maximum window size constraints and final size
            window.sizeFull put window.calcSizeAfterConstraint(window.sizeFull)
            window.size put when (window.collapsed && flags hasnt Wf.ChildWindow) {
                true -> window.titleBarRect().size
                else -> window.sizeFull
            }

            /* ---------- SCROLLBAR STATUS ---------- */

            // Update scrollbar status (based on the Size that was effective during last frame or the auto-resized Size).
            if (!window.collapsed) {
                // When reading the current size we need to read it after size constraints have been applied
                val sizeXforScrollbars = if (sizeFullModified.x != Float.MAX_VALUE) window.sizeFull.x else window.sizeFullAtLastBegin.x
                val sizeYforScrollbars = if (sizeFullModified.y != Float.MAX_VALUE) window.sizeFull.y else window.sizeFullAtLastBegin.y
                window.scrollbar.y = flags has Wf.AlwaysVerticalScrollbar || (window.sizeContents.y > sizeYforScrollbars && flags hasnt Wf.NoScrollbar)
                window.scrollbar.x = flags has Wf.AlwaysHorizontalScrollbar || ((window.sizeContents.x > sizeXforScrollbars - if (window.scrollbar.y) style.scrollbarSize else 0f) && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar)
                if (window.scrollbar.x && !window.scrollbar.y)
                    window.scrollbar.y = window.sizeContents.y > sizeYforScrollbars - style.scrollbarSize && flags hasnt Wf.NoScrollbar
                window.scrollbarSizes.put(if (window.scrollbar.y) style.scrollbarSize else 0f, if (window.scrollbar.x) style.scrollbarSize else 0f)
            }

            /* ---------- POSITION ---------- */

            if (windowJustActivatedByUser) {
                // Popup first latch mouse position, will position itself when it appears next frame
                window.autoPosLastDirection = Dir.None
                if (flags has Wf.Popup && !windowPosSetByApi)
                    window.pos put g.beginPopupStack.last().openPopupPos
            }

            // Position child window
            if (flags has Wf.ChildWindow) {
                assert(parentWindow!!.active)
                window.beginOrderWithinParent = parentWindow.dc.childWindows.size
                parentWindow.dc.childWindows += window

                if (flags hasnt Wf.Popup && !windowPosSetByApi && !windowIsChildTooltip)
                    window.pos put parentWindow.dc.cursorPos
            }

            val windowPosWithPivot = window.setWindowPosVal.x != Float.MAX_VALUE && window.hiddenFramesCannotSkipItems == 0
            if (windowPosWithPivot)
            // Position given a pivot (e.g. for centering)
                window.setPos(glm.max(style.displaySafeAreaPadding, window.setWindowPosVal - window.sizeFull * window.setWindowPosPivot), Cond.None)
            else if (flags has Wf.ChildMenu)
                window.pos = findBestWindowPosForPopup(window)
            else if (flags has Wf.Popup && !windowPosSetByApi && windowJustAppearingAfterHiddenForResize)
                window.pos = findBestWindowPosForPopup(window)
            else if (flags has Wf.Tooltip && !windowPosSetByApi && !windowIsChildTooltip)
                window.pos = findBestWindowPosForPopup(window)

            // Clamp position so it stays visible
            // Ignore zero-sized display explicitly to avoid losing positions if a window manager reports zero-sized window when initializing or minimizing.
            val viewportRect = viewportRect
            if (!windowPosSetByApi && flags hasnt Wf.ChildWindow && window.autoFitFrames allLessThanEqual 0)
            // Ignore zero-sized display explicitly to avoid losing positions if a window manager reports zero-sized window when initializing or minimizing.
                if (io.displaySize allGreaterThan 0) {
                    val clampPadding = style.displayWindowPadding max style.displaySafeAreaPadding
                    clampWindowRect(window, viewportRect, clampPadding)
                }
            window.pos put floor(window.pos)

            // Lock window rounding for the frame (so that altering them doesn't cause inconsistencies)
            window.windowRounding = when {
                flags has Wf.ChildWindow -> style.childRounding
                else -> when {
                    flags has Wf.Popup && flags hasnt Wf.Modal -> style.popupRounding
                    else -> style.windowRounding
                }
            }

            // Apply scrolling
            window.scroll put calcNextScrollFromScrollTargetAndClamp(window, true)
            window.scrollTarget put Float.MAX_VALUE

            // Apply window focus (new and reactivated windows are moved to front)
            val wantFocus = when {
                !windowJustActivatedByUser || flags has Wf.NoFocusOnAppearing -> false
                else -> when {
                    flags has Wf.Popup -> true
                    flags hasnt (Wf.ChildWindow or Wf.Tooltip) -> true
                    else -> false
                }
            }

            // Handle manual resize: Resize Grips, Borders, Gamepad
            var borderHeld = -1
            val resizeGripCol = IntArray(4)
            val resizeGripCount = if (io.configWindowsResizeFromEdges) 2 else 1 // 4
            val resizeGripDrawSize = max(g.fontSize * 1.35f, window.windowRounding + 1f + g.fontSize * 0.2f).i.f
            if (!window.collapsed)
                borderHeld = updateManualResize(window, sizeAutoFit, borderHeld, resizeGripCount, resizeGripCol)
            window.resizeBorderHeld = borderHeld

            // Default item width. Make it proportional to window size if window manually resizes
            window.itemWidthDefault = when {
                window.size.x > 0f && flags hasnt Wf.Tooltip && flags hasnt Wf.AlwaysAutoResize -> window.size.x * 0.65f
                else -> g.fontSize * 16f
            }.i.f

            val titleBarRect = window.titleBarRect()
            window.apply {
                // Store a backup of SizeFull which we will use next frame to decide if we need scrollbars.
                sizeFullAtLastBegin put sizeFull

                // UPDATE RECTANGLES

                // Update various regions. Variables they depends on are set above in this function.
                // FIXME: ContentsRegionRect.Max is currently very misleading / partly faulty, but some BeginChild() patterns relies on it.
                // NB: WindowBorderSize is included in WindowPadding _and_ ScrollbarSizes so we need to cancel one out.
                contentsRegionRect.put(
                        pos.x - scroll.x + windowPadding.x,
                        pos.y - scroll.y + windowPadding.y + titleBarHeight + menuBarHeight,
                        pos.x - scroll.x - windowPadding.x + if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else (size.x - scrollbarSizes.x + min(scrollbarSizes.x, windowBorderSize)),
                        pos.y - scroll.y - windowPadding.y + if (sizeContentsExplicit.y != 0f) sizeContentsExplicit.y else (size.y - scrollbarSizes.y + min(scrollbarSizes.y, windowBorderSize)))

                // Save clipped aabb so we can access it in constant-time in FindHoveredWindow()
                outerRectClipped put rect()
                outerRectClipped clipWith clipRect

                // Inner rectangle
                // We set this up after processing the resize grip so that our clip rectangle doesn't lag by a frame
                // Note that if our window is collapsed we will end up with an inverted (~null) clipping rectangle which is the correct behavior.
                innerMainRect.put(
                        titleBarRect.min.x + windowBorderSize,
                        titleBarRect.max.y + menuBarHeight + if (flags has Wf.MenuBar || flags hasnt Wf.NoTitleBar) style.frameBorderSize else windowBorderSize,
                        pos.x + size.x - max(scrollbarSizes.x, windowBorderSize),
                        pos.y + size.y - max(scrollbarSizes.y, windowBorderSize))

                // Inner clipping rectangle will extend a little bit outside the work region.
                // This is to allow e.g. Selectable or CollapsingHeader or some separators to cover that space.
                // Force round operator last to ensure that e.g. (int)(max.x-min.x) in user's render code produce correct result.
                innerClipRect.put(
                        floor(0.5f + innerMainRect.min.x + max(0f, floor(windowPadding.x * 0.5f - windowBorderSize))),
                        floor(0.5f + innerMainRect.min.y),
                        floor(0.5f + innerMainRect.max.x - max(0f, floor(windowPadding.x * 0.5f - windowBorderSize))),
                        floor(0.5f + innerMainRect.max.y))
            }

            /* ---------- DRAWING ---------- */

            // Setup draw list and outer clipping rectangle
            window.drawList.clear()
            window.drawList.flags = (if (style.antiAliasedLines) Dlf.AntiAliasedLines.i else 0) or if (style.antiAliasedFill) Dlf.AntiAliasedFill.i else 0
            window.drawList.pushTextureId(g.font.containerAtlas.texId)
            if (flags has Wf.ChildWindow && flags hasnt Wf.Popup && !windowIsChildTooltip)
                pushClipRect(parentWindow!!.clipRect.min, parentWindow.clipRect.max, true)
            else
                pushClipRect(viewportRect.min, viewportRect.max, true)

            // Draw modal window background (darkens what is behind them, all viewports)
            val dimBgForModal = flags has Wf.Modal && window === frontMostPopupModal && window.hiddenFramesCannotSkipItems <= 0
            val dimBgForWindowList = g.navWindowingTargetAnim?.rootWindow === window
            if (dimBgForModal || dimBgForWindowList) {
                val dimBgCol = getColorU32(if (dimBgForModal) Col.ModalWindowDimBg else Col.NavWindowingDimBg, g.dimBgRatio)
                window.drawList.addRectFilled(viewportRect.min, viewportRect.max, dimBgCol)
            }

            // Draw navigation selection/windowing rectangle background
            if (dimBgForWindowList && window == g.navWindowingTargetAnim!!.rootWindow) {
                val bb = window.rect()
                bb expand g.fontSize
                if (viewportRect !in bb) // Avoid drawing if the window covers all the viewport anyway
                    window.drawList.addRectFilled(bb.min, bb.max, getColorU32(Col.NavWindowingHighlight, g.navWindowingHighlightAlpha * 0.25f), g.style.windowRounding)
            }

            val windowToHighlight = g.navWindowingTarget ?: g.navWindow
            val titleBarIsHighlight = wantFocus || (windowToHighlight?.let { window.rootWindowForTitleBarHighlight === it.rootWindowForTitleBarHighlight }
                    ?: false)
            renderWindowDecorations(window, titleBarRect, titleBarIsHighlight, resizeGripCount, resizeGripCol, resizeGripDrawSize)

            // Draw navigation selection/windowing rectangle border
            if (g.navWindowingTargetAnim === window) {
                var rounding = max(window.windowRounding, style.windowRounding)
                val bb = window.rect()
                bb expand g.fontSize
                if (viewportRect in bb) { // If a window fits the entire viewport, adjust its highlight inward
                    bb expand (-g.fontSize - 1f)
                    rounding = window.windowRounding
                }
                window.drawList.addRect(bb.min, bb.max, getColorU32(Col.NavWindowingHighlight, g.navWindowingHighlightAlpha), rounding, 0.inv(), 3f)
            }

            // Store a backup of SizeFull which we will use next frame to decide if we need scrollbars.
            window.sizeFullAtLastBegin put window.sizeFull

            with(window) {

                /*  Setup drawing context
                    (NB: That term "drawing context / DC" lost its meaning a long time ago. Initially was meant to hold
                    transient data only. Nowadays difference between window-> and window->DC-> is dubious.)
                 */
                dc.indent = 0f + windowPadding.x - scroll.x
                dc.groupOffset = 0f
                dc.columnsOffset = 0f
                dc.cursorStartPos.put(pos.x + dc.indent + dc.columnsOffset,
                        pos.y + titleBarHeight + menuBarHeight + windowPadding.y - scroll.y)
                dc.cursorPos put dc.cursorStartPos
                dc.cursorPosPrevLine put dc.cursorPos
                dc.cursorMaxPos put dc.cursorStartPos
                dc.prevLineSize put 0f
                dc.currLineSize put 0f
                dc.prevLineTextBaseOffset = 0f
                dc.currLineTextBaseOffset = 0f
                dc.navHideHighlightOneFrame = false
                dc.navHasScroll = window.scrollMaxY > 0f
                dc.navLayerActiveMask = window.dc.navLayerActiveMaskNext
                dc.navLayerActiveMaskNext = 0
                dc.menuBarAppending = false
                dc.childWindows.clear()
                dc.layoutType = Lt.Vertical
                dc.parentLayoutType = parentWindow?.dc?.layoutType ?: Lt.Vertical
                dc.focusCounterTab = -1
                dc.focusCounterAll = -1
                dc.itemFlags = parentWindow?.dc?.itemFlags ?: If.Default_.i
                dc.itemWidth = itemWidthDefault
                dc.textWrapPos = -1f // disabled
                dc.itemFlagsStack.clear()
                dc.itemWidthStack.clear()
                dc.textWrapPosStack.clear()
                dc.currentColumns = null
                dc.treeDepth = 0
                dc.treeStoreMayJumpToParentOnPop = 0
                dc.stateStorage = stateStorage
                dc.groupStack.clear()
                menuColumns.update(3, style.itemSpacing.x, windowJustActivatedByUser)

                if (flags has Wf.ChildWindow && dc.itemFlags != parentWindow!!.dc.itemFlags) {
                    dc.itemFlags = parentWindow.dc.itemFlags
                    dc.itemFlagsStack.add(dc.itemFlags)
                }

                if (autoFitFrames.x > 0) autoFitFrames.x--
                if (autoFitFrames.y > 0) autoFitFrames.y--
            }

            // Apply focus (we need to call FocusWindow() AFTER setting DC.CursorStartPos so our initial navigation reference rectangle can start around there)
            if (wantFocus) {
                window.focus()
                navInitWindow(window, false)
            }

            // Title bar
            if (flags hasnt Wf.NoTitleBar)
                renderWindowTitleBarContents(window, titleBarRect, name, pOpen)

            // Pressing CTRL+C while holding on a window copy its content to the clipboard
            // This works but 1. doesn't handle multiple Begin/End pairs, 2. recursing into another Begin/End pair - so we need to work that out and add better logging scope.
            // Maybe we can support CTRL+C on every element?
            /*
        if (g.ActiveId == move_id)
            if (g.io.KeyCtrl && IsKeyPressedMap(ImGuiKey_C))
                ImGui::LogToClipboard();
        */

            // We fill last item data based on Title Bar/Tab, in order for IsItemHovered() and IsItemActive() to be usable after Begin().
            // This is useful to allow creating context menus on title bar only, etc.
            window.dc.lastItemId = window.moveId
            window.dc.lastItemStatusFlags = if (isMouseHoveringRect(titleBarRect.min, titleBarRect.max)) ItemStatusFlag.HoveredRect.i else 0
            window.dc.lastItemRect = titleBarRect

            if (IMGUI_ENABLE_TEST_ENGINE && window.flags hasnt Wf.NoTitleBar)
                ImGuiTestEngineHook_ItemAdd(window.dc.lastItemRect, window.dc.lastItemId)
        } else   // Append
            window.setCurrent()

        pushClipRect(window.innerClipRect.min, window.innerClipRect.max, true)

        // Clear 'accessed' flag last thing
        if (firstBeginOfTheFrame) window.writeAccessed = false

        window.beginCount++
        g.nextWindowData.clearFlags()

        if (flags has Wf.ChildWindow) {

            // Child window can be out of sight and have "negative" clip windows.
            // Mark them as collapsed so commands are skipped earlier (we can't manually collapse them because they have no title bar).
            assert(flags has Wf.NoTitleBar)
            if (flags hasnt Wf.AlwaysAutoResize && window.autoFitFrames allLessThanEqual 0)
                if (window.outerRectClipped.min anyGreaterThanEqual window.outerRectClipped.max)
                    window.hiddenFramesCanSkipItems = 1

            // Completely hide along with parent or if parent is collapsed
            parentWindow?.let {
                if (it.collapsed || it.hidden)
                    window.hiddenFramesCanSkipItems = 1
            }
        }

        // Don't render if style alpha is 0.0 at the time of Begin(). This is arbitrary and inconsistent but has been there for a long while (may remove at some point)
        if (style.alpha <= 0f)
            window.hiddenFramesCanSkipItems = 1

        // Update the Hidden flag
        window.hidden = window.hiddenFramesCanSkipItems > 0 || window.hiddenFramesCannotSkipItems > 0

        // Return false if we don't intend to display anything to allow user to perform an early out optimization
        // Update the SkipItems flag, used to early out of all items functions (no layout required)
        val skipItems = window.run { (collapsed || !active || hidden) && autoFitFrames allLessThanEqual 0 && hiddenFramesCannotSkipItems <= 0 }
        window.skipItems = skipItems
        return !skipItems
    }

    /** Always call even if Begin() return false (which indicates a collapsed window)! finish appending to current window,
     *  pop it off the window stack.    */
    fun end() {

        if (g.currentWindowStack.size <= 1 && g.frameScopePushedImplicitWindow) {
            assert(g.currentWindowStack.size > 1) { "Calling End() too many times!" }
            return // FIXME-ERRORHANDLING
        }
        assert(g.currentWindowStack.isNotEmpty())

        val window = g.currentWindow!!

        if (window.dc.currentColumns != null) // close columns set if any is open
            endColumns()
        popClipRect()   // Inner window clip rectangle

        // Stop logging
//TODO            if (flags hasnt WindowFlag.ChildWindow)    // FIXME: add more options for scope of logging
//                logFinish()

        // Pop from window stack
        g.currentWindowStack.pop()
        if (window.flags has Wf.Popup)
            g.beginPopupStack.pop()
        checkStacksSize(window, false)
        setCurrentWindow(g.currentWindowStack.lastOrNull())
    }


    /** per-window font scale. Adjust io.FontGlobalScale if you want to scale all windows   */
    fun setWindowFontScale(scale: Float) = with(currentWindow) {
        fontWindowScale = scale
        g.drawListSharedData.fontSize = calcFontSize()
        g.fontSize = g.drawListSharedData.fontSize
    }


    companion object {

        fun getWindowBgColorIdxFromFlags(flags: Int) = when {
            flags has (Wf.Tooltip or Wf.Popup) -> Col.PopupBg
            flags has Wf.ChildWindow -> Col.ChildBg
            else -> Col.WindowBg
        }
    }
}