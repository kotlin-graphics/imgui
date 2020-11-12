package imgui.api

import gli_.has
import gli_.hasnt
import glm_.f
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.endColumns
import imgui.ImGui.findBestWindowPosForPopup
import imgui.ImGui.findWindowByName
import imgui.ImGui.focusWindow
import imgui.ImGui.getColorU32
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.logFinish
import imgui.ImGui.navInitWindow
import imgui.ImGui.popClipRect
import imgui.ImGui.pushClipRect
import imgui.ImGui.setLastItemData
import imgui.ImGui.style
import imgui.ImGui.topMostPopupModal
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import imgui.static.*
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf
import imgui.internal.sections.ItemFlag as If
import imgui.internal.sections.LayoutType as Lt


// Windows
// - Begin() = push window to the stack and start appending to it. End() = pop window from the stack.
// - Passing 'bool* p_open != NULL' shows a window-closing widget in the upper-right corner of the window,
//   which clicking will set the boolean to false when clicked.
// - You may append multiple times to the same window during the same frame by calling Begin()/End() pairs multiple times.
//   Some information such as 'flags' or 'p_open' will only be considered by the first call to Begin().
// - Begin() return false to indicate the window is collapsed or fully clipped, so you may early out and omit submitting
//   anything to the window. Always call a matching End() for each Begin() call, regardless of its return value!
//   [Important: due to legacy reason, this is inconsistent with most other functions such as BeginMenu/EndMenu,
//    BeginPopup/EndPopup, etc. where the EndXXX call should only be called if the corresponding BeginXXX function
//    returned true. Begin and BeginChild are the only odd ones out. Will be fixed in a future update.]
// - Note that the bottom of window stack always contains a window called "Debug".
interface windows {

    fun begin(name: String, pOpen: BooleanArray, flags: WindowFlags): Boolean = begin(name, pOpen, 0, flags)

    fun begin(name: String, pOpen: BooleanArray?, index: Int, flags: WindowFlags): Boolean = when (pOpen) {
        null -> begin(name, null, flags)
        else -> withBoolean(pOpen, index) { begin(name, it, flags) }
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
        assert(g.withinFrameScope) { "Forgot to call ImGui::newFrame()" }
        assert(g.frameCountEnded != g.frameCount) { "Called ImGui::render() or ImGui::rndFrame() and haven't called ImGui::newFrame() again yet" }

        var flags = flags_

        // Find or create
        var windowJustCreated = false
        val window = findWindowByName(name) ?: createNewWindow(name, flags).also { windowJustCreated = true }

        // Automatically disable manual moving/resizing when NoInputs is set
        if ((flags and Wf.NoInputs) == Wf.NoInputs.i) flags = flags or Wf.NoMove or Wf.NoResize

        if (flags has Wf._NavFlattened) assert(flags has Wf._ChildWindow)

        val currentFrame = g.frameCount
        val firstBeginOfTheFrame = window.lastFrameActive != currentFrame
        window.isFallbackWindow = g.currentWindowStack.isEmpty() && g.withinFrameScopeWithImplicitWindow

        // Update the Appearing flag
        // Not using !WasActive because the implicit "Debug" window would always toggle off->on
        var windowJustActivatedByUser = window.lastFrameActive < currentFrame - 1
        val windowJustAppearingAfterHiddenForResize = window.hiddenFramesCannotSkipItems > 0
        if (flags has Wf._Popup) {
            val popupRef =
                g.openPopupStack[g.beginPopupStack.size] // We recycle popups so treat window as activated if popup id changed
            windowJustActivatedByUser = windowJustActivatedByUser || window.popupId != popupRef.popupId
            windowJustActivatedByUser = windowJustActivatedByUser || window !== popupRef.window
        }
        window.appearing = windowJustActivatedByUser || windowJustAppearingAfterHiddenForResize
        if (window.appearing) window.setConditionAllowFlags(Cond.Appearing.i, true)

        // Update Flags, LastFrameActive, BeginOrderXXX fields
        if (firstBeginOfTheFrame) {
            window.flags = flags
            window.lastFrameActive = currentFrame
            window.lastTimeActive = g.time.f
            window.beginOrderWithinParent = 0
            window.beginOrderWithinContext = g.windowsActiveCount++
        } else flags = window.flags

        // Parent window is latched only on the first call to Begin() of the frame, so further append-calls can be done from a different window stack
        val parentWindowInStack = g.currentWindowStack.lastOrNull()
        val parentWindow = when {
            firstBeginOfTheFrame -> parentWindowInStack.takeIf { flags has (Wf._ChildWindow or Wf._Popup) }
            else -> window.parentWindow
        }
        assert(parentWindow != null || flags hasnt Wf._ChildWindow)

        // We allow window memory to be compacted so recreate the base stack when needed.
        if (window.idStack.isEmpty()) window.idStack += window.id

        // Add to stack
        // We intentionally set g.CurrentWindow to NULL to prevent usage until when the viewport is set, then will call SetCurrentWindow()
        g.currentWindowStack += window
        g.currentWindow = null
        errorCheckBeginEndCompareStacksSize(window, true)
        if (flags has Wf._Popup) {
            val popupRef = g.openPopupStack[g.beginPopupStack.size]
            popupRef.window = window
            g.beginPopupStack += popupRef
            window.popupId = popupRef.popupId
        }

        if (windowJustAppearingAfterHiddenForResize && flags hasnt Wf._ChildWindow) window.navLastIds[0] = 0

        // Update ->RootWindow and others pointers (before any possible call to FocusWindow)
        if (firstBeginOfTheFrame) window.updateParentAndRootLinks(flags, parentWindow)

        // Process SetNextWindow***() calls
        // (FIXME: Consider splitting the HasXXX flags into X/Y components
        var windowPosSetByApi = false
        var windowSizeXsetByApi = false
        var windowSizeYsetByApi = false
        if (g.nextWindowData.flags has NextWindowDataFlag.HasPos) {
            windowPosSetByApi = window.setWindowPosAllowFlags has g.nextWindowData.posCond
            if (windowPosSetByApi && g.nextWindowData.posPivotVal.lengthSqr > 0.00001f) {/*  May be processed on the next frame if this is our first frame and we are measuring size
                    FIXME: Look into removing the branch so everything can go through this same code path for consistency.  */
                window.setWindowPosVal put g.nextWindowData.posVal
                window.setWindowPosPivot put g.nextWindowData.posPivotVal
                window.setWindowPosAllowFlags =
                    window.setWindowPosAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()
            } else window.setPos(g.nextWindowData.posVal, g.nextWindowData.posCond)
        }
        if (g.nextWindowData.flags has NextWindowDataFlag.HasSize) {
            windowSizeXsetByApi =
                window.setWindowSizeAllowFlags has g.nextWindowData.sizeCond && g.nextWindowData.sizeVal.x > 0f
            windowSizeYsetByApi =
                window.setWindowSizeAllowFlags has g.nextWindowData.sizeCond && g.nextWindowData.sizeVal.y > 0f
            window.setSize(g.nextWindowData.sizeVal, g.nextWindowData.sizeCond)
        }
        if (g.nextWindowData.flags has NextWindowDataFlag.HasScroll) {
            if (g.nextWindowData.scrollVal.x >= 0f) {
                window.scrollTarget.x = g.nextWindowData.scrollVal.x
                window.scrollTargetCenterRatio.x = 0f
            }
            if (g.nextWindowData.scrollVal.y >= 0f) {
                window.scrollTarget.y = g.nextWindowData.scrollVal.y
                window.scrollTargetCenterRatio.y = 0f
            }
        }
        if (g.nextWindowData.flags has NextWindowDataFlag.HasContentSize) window.contentSizeExplicit put g.nextWindowData.contentSizeVal
        else if (firstBeginOfTheFrame) window.contentSizeExplicit put 0f
        if (g.nextWindowData.flags has NextWindowDataFlag.HasCollapsed) window.setCollapsed(g.nextWindowData.collapsedVal,
            g.nextWindowData.collapsedCond)
        if (g.nextWindowData.flags has NextWindowDataFlag.HasFocus) focusWindow(window)
        if (window.appearing) window.setConditionAllowFlags(Cond.Appearing.i, false)

        // When reusing window again multiple times a frame, just append content (don't need to setup again)
        if (firstBeginOfTheFrame) { // Initialize
            val windowIsChildTooltip =
                flags has Wf._ChildWindow && flags has Wf._Tooltip // FIXME-WIP: Undocumented behavior of Child+Tooltip for pinned tooltip (#1345)

            window.active = true
            window.hasCloseButton = pOpen != null
            window.clipRect.put(-Float.MAX_VALUE, -Float.MAX_VALUE, +Float.MAX_VALUE, +Float.MAX_VALUE)
            for (i in 1 until window.idStack.size) window.idStack.pop()  // resize 1
            window.drawList._resetForNewFrame()

            // Restore buffer capacity when woken from a compacted state, to avoid
            if (window.memoryCompacted) window.gcAwakeTransientBuffers()

            // Update stored window name when it changes (which can _only_ happen with the "###" operator, so the ID would stay unchanged).
            // The title bar always display the 'name' parameter, so we only update the string storage if it needs to be visible to the end-user elsewhere.
            var windowTitleVisibleElsewhere = false
            if (g.navWindowingListWindow != null && window.flags hasnt Wf.NoNavFocus)   // Window titles visible when using CTRL+TAB
                windowTitleVisibleElsewhere = true
            if (windowTitleVisibleElsewhere && !windowJustCreated && name != window.name) { //                val buf_len = (size_t)window->NameBufLen
                //                window->Name = ImStrdupcpy(window->Name, &buf_len, name)
                //                window->NameBufLen = (int)buf_len
                window.name = name
                window.nameBufLen = name.toByteArray().size
            }

            // UPDATE CONTENTS SIZE, UPDATE HIDDEN STATUS

            // Update contents size from last frame for auto-fitting (or use explicit size)
            window.contentSize = window.calcContentSize()
            if (window.hiddenFramesCanSkipItems > 0) window.hiddenFramesCanSkipItems--
            if (window.hiddenFramesCannotSkipItems > 0) window.hiddenFramesCannotSkipItems--

            // Hide new windows for one frame until they calculate their size
            if (windowJustCreated && (!windowSizeXsetByApi || !windowSizeYsetByApi)) window.hiddenFramesCannotSkipItems =
                1

            /*  Hide popup/tooltip window when re-opening while we measure size (because we recycle the windows)
                We reset Size/SizeContents for reappearing popups/tooltips early in this function,
                so further code won't be tempted to use the old size.             */
            if (windowJustActivatedByUser && flags has (Wf._Popup or Wf._Tooltip)) {
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
                    window.contentSize put 0f
                }
            }

            // SELECT VIEWPORT
            // FIXME-VIEWPORT: In the docking/viewport branch, this is the point where we select the current viewport (which may affect the style)
            setCurrentWindow(window)

            // LOCK BORDER SIZE AND PADDING FOR THE FRAME (so that altering them doesn't cause inconsistencies)

            window.windowBorderSize = when {
                flags has Wf._ChildWindow -> style.childBorderSize
                flags has (Wf._Popup or Wf._Tooltip) && flags hasnt Wf._Modal -> style.popupBorderSize
                else -> style.windowBorderSize
            }
            window.windowPadding put style.windowPadding
            if (flags has Wf._ChildWindow && !(flags has (Wf.AlwaysUseWindowPadding or Wf._Popup)) && window.windowBorderSize == 0f) window.windowPadding.put(
                0f,
                if (flags has Wf.MenuBar) style.windowPadding.y else 0f)

            // Lock menu offset so size calculation can use it as menu-bar windows need a minimum size.
            window.dc.menuBarOffset.x =
                (window.windowPadding.x max style.itemSpacing.x) max g.nextWindowData.menuBarOffsetMinVal.x
            window.dc.menuBarOffset.y = g.nextWindowData.menuBarOffsetMinVal.y

            // Collapse window by double-clicking on title bar
            // At this point we don't have a clipping rectangle setup yet, so we can use the title bar area
            // for hit detection and drawing
            if (flags hasnt Wf.NoTitleBar && flags hasnt Wf.NoCollapse) {/*  We don't use a regular button+id to test for double-click on title bar (mostly due to legacy reason, could be fixed),
                    so verify that we don't have items over the title bar.                 */
                val titleBarRect = window.titleBarRect()
                if (g.hoveredWindow === window && g.hoveredId == 0 && g.hoveredIdPreviousFrame == 0 && isMouseHoveringRect(
                        titleBarRect) && io.mouseDoubleClicked[0]
                ) window.wantCollapseToggle = true
                if (window.wantCollapseToggle) {
                    window.collapsed = !window.collapsed
                    window.markIniSettingsDirty()
                    focusWindow(window)
                }
            } else window.collapsed = false
            window.wantCollapseToggle = false

            /* ---------- SIZE ---------- */

            // Calculate auto-fit size, handle automatic resize
            val sizeAutoFit = window.calcAutoFitSize(window.contentSize)
            var useCurrentSizeForScrollbarX = windowJustCreated
            var useCurrentSizeForScrollbarY = windowJustCreated
            if (flags has Wf.AlwaysAutoResize && !window.collapsed) { // Using SetNextWindowSize() overrides ImGuiWindowFlags_AlwaysAutoResize, so it can be used on tooltips/popups, etc.
                if (!windowSizeXsetByApi) {
                    window.sizeFull.x = sizeAutoFit.x
                    useCurrentSizeForScrollbarX = true
                }
                if (!windowSizeYsetByApi) {
                    window.sizeFull.y = sizeAutoFit.y
                    useCurrentSizeForScrollbarY = true
                }
            } else if (window.autoFitFrames.x > 0 || window.autoFitFrames.y > 0) {/*  Auto-fit may only grow window during the first few frames
                    We still process initial auto-fit on collapsed windows to get a window width,
                    but otherwise don't honor WindowFlag.AlwaysAutoResize when collapsed.                 */
                if (!windowSizeXsetByApi && window.autoFitFrames.x > 0) {
                    window.sizeFull.x =
                        if (window.autoFitOnlyGrows) max(window.sizeFull.x, sizeAutoFit.x) else sizeAutoFit.x
                    useCurrentSizeForScrollbarX = true
                }
                if (!windowSizeYsetByApi && window.autoFitFrames.y > 0) {
                    window.sizeFull.y =
                        if (window.autoFitOnlyGrows) max(window.sizeFull.y, sizeAutoFit.y) else sizeAutoFit.y
                    useCurrentSizeForScrollbarY = true
                }
                if (!window.collapsed) window.markIniSettingsDirty()
            }

            // Apply minimum/maximum window size constraints and final size
            window.sizeFull = window.calcSizeAfterConstraint(window.sizeFull)
            window.size put when (window.collapsed && flags hasnt Wf._ChildWindow) {
                true -> window.titleBarRect().size
                else -> window.sizeFull
            }

            // Decoration size
            val decorationUpHeight = window.titleBarHeight + window.menuBarHeight

            /* ---------- POSITION ---------- */

            // Popup latch its initial position, will position itself when it appears next frame
            if (windowJustActivatedByUser) {
                window.autoPosLastDirection = Dir.None
                if (flags has Wf._Popup && flags hasnt Wf._Modal && !windowPosSetByApi) // FIXME: BeginPopup() could use SetNextWindowPos()
                    window.pos put g.beginPopupStack.last().openPopupPos
            }

            // Position child window
            if (flags has Wf._ChildWindow) {
                assert(parentWindow!!.active)
                window.beginOrderWithinParent = parentWindow.dc.childWindows.size
                parentWindow.dc.childWindows += window

                if (flags hasnt Wf._Popup && !windowPosSetByApi && !windowIsChildTooltip) window.pos put parentWindow.dc.cursorPos
            }

            val windowPosWithPivot =
                window.setWindowPosVal.x != Float.MAX_VALUE && window.hiddenFramesCannotSkipItems == 0
            if (windowPosWithPivot) // Position given a pivot (e.g. for centering)
                window.setPos(window.setWindowPosVal - window.size * window.setWindowPosPivot, Cond.None)
            else if (flags has Wf._ChildMenu) window.pos = findBestWindowPosForPopup(window)
            else if (flags has Wf._Popup && !windowPosSetByApi && windowJustAppearingAfterHiddenForResize) window.pos =
                findBestWindowPosForPopup(window)
            else if (flags has Wf._Tooltip && !windowPosSetByApi && !windowIsChildTooltip) window.pos =
                findBestWindowPosForPopup(window)

            // Calculate the range of allowed position for that window (to be movable and visible past safe area padding)
            // When clamping to stay visible, we will enforce that window->Pos stays inside of visibility_rect.
            val viewportRect = viewportRect
            val visibilityPadding = style.displayWindowPadding max style.displaySafeAreaPadding
            val visibilityRect = Rect(viewportRect.min + visibilityPadding, viewportRect.max - visibilityPadding)

            // Clamp position/size so window stays visible within its viewport or monitor
            // Ignore zero-sized display explicitly to avoid losing positions if a window manager reports zero-sized window when initializing or minimizing.
            if (!windowPosSetByApi && flags hasnt Wf._ChildWindow && window.autoFitFrames allLessThanEqual 0) if (viewportRect.width > 0f && viewportRect.height > 0f) window clampRect visibilityRect
            window.pos put floor(window.pos)

            // Lock window rounding for the frame (so that altering them doesn't cause inconsistencies)
            // Large values tend to lead to variety of artifacts and are not recommended.
            window.windowRounding = when {
                flags has Wf._ChildWindow -> style.childRounding
                else -> when {
                    flags has Wf._Popup && flags hasnt Wf._Modal -> style.popupRounding
                    else -> style.windowRounding
                }
            }

            // For windows with title bar or menu bar, we clamp to FrameHeight(FontSize + FramePadding.y * 2.0f) to completely hide artifacts.
            //if ((window->Flags & ImGuiWindowFlags_MenuBar) || !(window->Flags & ImGuiWindowFlags_NoTitleBar))
            //    window->WindowRounding = ImMin(window->WindowRounding, g.FontSize + style.FramePadding.y * 2.0f);

            // Apply window focus (new and reactivated windows are moved to front)
            val wantFocus = when {
                !windowJustActivatedByUser || flags has Wf.NoFocusOnAppearing -> false
                else -> when {
                    flags has Wf._Popup -> true
                    flags hasnt (Wf._ChildWindow or Wf._Tooltip) -> true
                    else -> false
                }
            }

            // Handle manual resize: Resize Grips, Borders, Gamepad
            var borderHeld = -1
            val resizeGripCol = IntArray(4)
            val resizeGripCount =
                if (io.configWindowsResizeFromEdges) 2 else 1 // Allow resize from lower-left if we have the mouse cursor feedback for it.
            val resizeGripDrawSize = floor(max(g.fontSize * 1.35f, window.windowRounding + 1f + g.fontSize * 0.2f))
            if (!window.collapsed) {
                val (borderHeld_, ret) = updateWindowManualResize(window,
                    sizeAutoFit,
                    borderHeld,
                    resizeGripCount,
                    resizeGripCol,
                    visibilityRect)
                if (ret) {
                    useCurrentSizeForScrollbarX = true
                    useCurrentSizeForScrollbarY = true
                }
                borderHeld = borderHeld_
            }
            window.resizeBorderHeld = borderHeld

            // SCROLLBAR VISIBILITY

            // Update scrollbar visibility (based on the Size that was effective during last frame or the auto-resized Size).
            if (!window.collapsed) { // When reading the current size we need to read it after size constraints have been applied.
                // When we use InnerRect here we are intentionally reading last frame size, same for ScrollbarSizes values before we set them again.
                val availSizeFromCurrentFrame = Vec2(window.sizeFull.x, window.sizeFull.y - decorationUpHeight)
                val availSizeFromLastFrame = window.innerRect.size + window.scrollbarSizes
                val neededSizeFromLastFrame =
                    if (windowJustCreated) Vec2() else window.contentSize + window.windowPadding * 2f
                val sizeXforScrollbars =
                    if (useCurrentSizeForScrollbarX) availSizeFromCurrentFrame.x else availSizeFromLastFrame.x
                val sizeYforScrollbars =
                    if (useCurrentSizeForScrollbarY) availSizeFromCurrentFrame.y else availSizeFromLastFrame.y //bool scrollbar_y_from_last_frame = window->ScrollbarY; // FIXME: May want to use that in the ScrollbarX expression? How many pros vs cons?
                window.scrollbar.y =
                    flags has Wf.AlwaysVerticalScrollbar || (neededSizeFromLastFrame.y > sizeYforScrollbars && flags hasnt Wf.NoScrollbar)
                window.scrollbar.x =
                    flags has Wf.AlwaysHorizontalScrollbar || ((neededSizeFromLastFrame.x > sizeXforScrollbars - if (window.scrollbar.y) style.scrollbarSize else 0f) && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar)
                if (window.scrollbar.x && !window.scrollbar.y) window.scrollbar.y =
                    neededSizeFromLastFrame.y > sizeYforScrollbars && flags hasnt Wf.NoScrollbar
                window.scrollbarSizes.put(if (window.scrollbar.y) style.scrollbarSize else 0f,
                    if (window.scrollbar.x) style.scrollbarSize else 0f)
            }

            val titleBarRect: Rect
            val hostRect: Rect
            window.apply { // UPDATE RECTANGLES (1- THOSE NOT AFFECTED BY SCROLLING)
                // Update various regions. Variables they depends on should be set above in this function.
                // We set this up after processing the resize grip so that our rectangles doesn't lag by a frame.

                // Outer rectangle
                // Not affected by window border size. Used by:
                // - FindHoveredWindow() (w/ extra padding when border resize is enabled)
                // - Begin() initial clipping rect for drawing window background and borders.
                // - Begin() clipping whole child
                hostRect = when {
                    flags has Wf._ChildWindow && flags hasnt Wf._Popup && !windowIsChildTooltip -> parentWindow!!.clipRect
                    else -> viewportRect
                }
                val outerRect = rect()
                titleBarRect = titleBarRect()
                outerRectClipped = outerRect
                outerRectClipped clipWith hostRect

                // Inner rectangle
                // Not affected by window border size. Used by:
                // - InnerClipRect
                // - NavScrollToBringItemIntoView()
                // - NavUpdatePageUpPageDown()
                // - Scrollbar()
                innerRect.put(minX = pos.x,
                    minY = pos.y + decorationUpHeight,
                    maxX = pos.x + size.x - scrollbarSizes.x,
                    maxY = pos.y + size.y - scrollbarSizes.y)

                // Inner clipping rectangle.
                // Will extend a little bit outside the normal work region.
                // This is to allow e.g. Selectable or CollapsingHeader or some separators to cover that space.
                // Force round operator last to ensure that e.g. (int)(max.x-min.x) in user's render code produce correct result.
                // Note that if our window is collapsed we will end up with an inverted (~null) clipping rectangle which is the correct behavior.
                // Affected by window/frame border size. Used by:
                // - Begin() initial clip rect
                val topBorderSize =
                    if (flags has Wf.MenuBar || flags hasnt Wf.NoTitleBar) style.frameBorderSize else window.windowBorderSize
                innerClipRect.put(minX = floor(0.5f + innerRect.min.x + max(floor(windowPadding.x * 0.5f),
                    windowBorderSize)),
                    minY = floor(0.5f + innerRect.min.y + topBorderSize),
                    maxX = floor(0.5f + innerRect.max.x - max(floor(windowPadding.x * 0.5f), windowBorderSize)),
                    maxY = floor(0.5f + innerRect.max.y - windowBorderSize))
                innerClipRect clipWithFull hostRect
            }

            // Default item width. Make it proportional to window size if window manually resizes
            window.itemWidthDefault = when {
                window.size.x > 0f && flags hasnt Wf._Tooltip && flags hasnt Wf.AlwaysAutoResize -> floor(window.size.x * 0.65f)
                else -> floor(g.fontSize * 16f)
            }

            // SCROLLING

            // Lock down maximum scrolling
            // The value of ScrollMax are ahead from ScrollbarX/ScrollbarY which is intentionally using InnerRect from previous rect in order to accommodate
            // for right/bottom aligned items without creating a scrollbar.
            window.scrollMax.x = max(0f, window.contentSize.x + window.windowPadding.x * 2f - window.innerRect.width)
            window.scrollMax.y = max(0f, window.contentSize.y + window.windowPadding.y * 2f - window.innerRect.height)

            // Apply scrolling
            window.scroll = window.calcNextScrollFromScrollTargetAndClamp()
            window.scrollTarget put Float.MAX_VALUE

            /* ---------- DRAWING ---------- */

            // Setup draw list and outer clipping rectangle
            assert(window.drawList.cmdBuffer.size == 1 && window.drawList.cmdBuffer[0].elemCount == 0)
            window.drawList.pushTextureId(g.font.containerAtlas.texID)
            pushClipRect(hostRect.min, hostRect.max, false)

            // Draw modal window background (darkens what is behind them, all viewports)
            val dimBgForModal =
                flags has Wf._Modal && window === topMostPopupModal && window.hiddenFramesCannotSkipItems <= 0
            val dimBgForWindowList = g.navWindowingTargetAnim?.rootWindow === window
            if (dimBgForModal || dimBgForWindowList) {
                val dimBgCol =
                    getColorU32(if (dimBgForModal) Col.ModalWindowDimBg else Col.NavWindowingDimBg, g.dimBgRatio)
                window.drawList.addRectFilled(viewportRect.min, viewportRect.max, dimBgCol)
            }

            // Draw navigation selection/windowing rectangle background
            if (dimBgForWindowList && window == g.navWindowingTargetAnim!!.rootWindow) {
                val bb = window.rect()
                bb expand g.fontSize
                if (viewportRect !in bb) // Avoid drawing if the window covers all the viewport anyway
                    window.drawList.addRectFilled(bb.min,
                        bb.max,
                        getColorU32(Col.NavWindowingHighlight, g.navWindowingHighlightAlpha * 0.25f),
                        g.style.windowRounding)
            }

            // Since 1.71, child window can render their decoration (bg color, border, scrollbars, etc.) within their parent to save a draw call.
            // When using overlapping child windows, this will break the assumption that child z-order is mapped to submission order.
            // We disable this when the parent window has zero vertices, which is a common pattern leading to laying out multiple overlapping child.
            // We also disabled this when we have dimming overlay behind this specific one child.
            // FIXME: More code may rely on explicit sorting of overlapping child window and would need to disable this somehow. Please get in contact if you are affected.
            run {
                var renderDecorationsInParent = false
                if (flags has Wf._ChildWindow && flags hasnt Wf._Popup && !windowIsChildTooltip) if (window.drawList.cmdBuffer.last().elemCount == 0 && parentWindow!!.drawList.vtxBuffer.rem > 0) renderDecorationsInParent =
                    true
                if (renderDecorationsInParent) window.drawList = parentWindow!!.drawList

                // Handle title bar, scrollbar, resize grips and resize borders
                val windowToHighlight = g.navWindowingTarget ?: g.navWindow
                val titleBarIsHighlight =
                    wantFocus || (windowToHighlight?.let { window.rootWindowForTitleBarHighlight === it.rootWindowForTitleBarHighlight }
                        ?: false)
                window.renderDecorations(titleBarRect,
                    titleBarIsHighlight,
                    resizeGripCount,
                    resizeGripCol,
                    resizeGripDrawSize)

                if (renderDecorationsInParent) window.drawList = window.drawListInst
            } // Draw navigation selection/windowing rectangle border
            if (g.navWindowingTargetAnim === window) {
                var rounding = max(window.windowRounding, style.windowRounding)
                val bb = window.rect()
                bb expand g.fontSize
                if (viewportRect in bb) { // If a window fits the entire viewport, adjust its highlight inward
                    bb expand (-g.fontSize - 1f)
                    rounding = window.windowRounding
                }
                window.drawList.addRect(bb.min,
                    bb.max,
                    getColorU32(Col.NavWindowingHighlight, g.navWindowingHighlightAlpha),
                    rounding,
                    0.inv(),
                    3f)
            }

            with(window) {

                // UPDATE RECTANGLES (2- THOSE AFFECTED BY SCROLLING)

                // Work rectangle.
                // Affected by window padding and border size. Used by:
                // - Columns() for right-most edge
                // - TreeNode(), CollapsingHeader() for right-most edge
                // - BeginTabBar() for right-most edge
                val allowScrollbarX = flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar
                val allowScrollbarY = flags hasnt Wf.NoScrollbar
                val workRectSizeX =
                    if (contentSizeExplicit.x != 0f) contentSizeExplicit.x else max(if (allowScrollbarX) window.contentSize.x else 0f,
                        size.x - windowPadding.x * 2f - scrollbarSizes.x)
                val workRectSizeY =
                    if (contentSizeExplicit.y != 0f) contentSizeExplicit.y else max(if (allowScrollbarY) window.contentSize.y else 0f,
                        size.y - windowPadding.y * 2f - decorationUpHeight - scrollbarSizes.y)
                workRect.min.put(floor(innerRect.min.x - scroll.x + max(windowPadding.x, windowBorderSize)),
                    floor(innerRect.min.y - scroll.y + max(windowPadding.y, windowBorderSize)))
                workRect.max.put(workRect.min.x + workRectSizeX, workRect.min.y + workRectSizeY)
                parentWorkRect put workRect

                // [LEGACY] Content Region
                // FIXME-OBSOLETE: window->ContentRegionRect.Max is currently very misleading / partly faulty, but some BeginChild() patterns relies on it.
                // NB: WindowBorderSize is included in WindowPadding _and_ ScrollbarSizes so we need to cancel one out when we have both.
                // Used by:
                // - Mouse wheel scrolling + many other things
                contentRegionRect.min.put( // need to split min max, because max relies on min
                    x = pos.x - scroll.x + windowPadding.x, y = pos.y - scroll.y + windowPadding.y + decorationUpHeight)
                contentRegionRect.max.put(x = contentRegionRect.min.x + when (contentSizeExplicit.x) {
                    0f -> size.x - windowPadding.x * 2f - scrollbarSizes.x
                    else -> contentSizeExplicit.x
                }, y = contentRegionRect.min.y + when (contentSizeExplicit.y) {
                    0f -> size.y - windowPadding.y * 2f - decorationUpHeight - scrollbarSizes.y
                    else -> contentSizeExplicit.y
                })

                // Setup drawing context
                // (NB: That term "drawing context / DC" lost its meaning a long time ago. Initially was meant to hold transient data only. Nowadays difference between window-> and window->DC-> is dubious.)
                dc.indent = 0f + windowPadding.x - scroll.x
                dc.groupOffset = 0f
                dc.columnsOffset = 0f
                dc.cursorStartPos.put(pos.x + dc.indent + dc.columnsOffset,
                    pos.y + decorationUpHeight + windowPadding.y - scroll.y)
                dc.cursorPos put dc.cursorStartPos
                dc.cursorPosPrevLine put dc.cursorPos
                dc.cursorMaxPos put dc.cursorStartPos
                dc.prevLineSize put 0f
                dc.currLineSize put 0f
                dc.prevLineTextBaseOffset = 0f
                dc.currLineTextBaseOffset = 0f

                dc.navLayerCurrent = NavLayer.Main
                dc.navLayerActiveMask = dc.navLayerActiveMaskNext
                dc.navLayerActiveMaskNext = 0x00
                dc.navFocusScopeIdCurrent =
                    if (flags has Wf._ChildWindow) parentWindow!!.dc.navFocusScopeIdCurrent else 0
                dc.navHideHighlightOneFrame = false
                dc.navHasScroll = scrollMax.y > 0f

                dc.menuBarAppending = false
                dc.menuColumns.update(3, style.itemSpacing.x, windowJustActivatedByUser)
                dc.treeDepth = 0
                dc.treeJumpToParentOnPopMask = 0x00
                dc.childWindows.clear()
                dc.stateStorage = stateStorage
                dc.currentColumns = null
                dc.layoutType = Lt.Vertical
                dc.parentLayoutType = parentWindow?.dc?.layoutType ?: Lt.Vertical
                dc.focusCounterTabStop = -1
                dc.focusCounterRegular = -1

                dc.itemWidth = itemWidthDefault
                dc.textWrapPos = -1f // disabled
                dc.itemFlagsStack.clear()
                dc.itemWidthStack.clear()
                dc.textWrapPosStack.clear()
                dc.groupStack.clear()

                dc.itemFlags = parentWindow?.dc?.itemFlags ?: If.Default_.i
                if (parentWindow != null) dc.itemFlagsStack += dc.itemFlags

                if (autoFitFrames.x > 0) autoFitFrames.x--
                if (autoFitFrames.y > 0) autoFitFrames.y--
            }

            // Apply focus (we need to call FocusWindow() AFTER setting DC.CursorStartPos so our initial navigation reference rectangle can start around there)
            if (wantFocus) {
                focusWindow(window)
                navInitWindow(window, false)
            }

            // Title bar
            if (flags hasnt Wf.NoTitleBar) window.renderTitleBarContents(titleBarRect, name, pOpen)

            // Clear hit test shape every frame
            window.hitTestHoleSize put 0

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
            val flag = if (isMouseHoveringRect(titleBarRect, false)) ItemStatusFlag.HoveredRect else ItemStatusFlag.None
            setLastItemData(window, window.moveId, flag.i, titleBarRect)

            if (IMGUI_ENABLE_TEST_ENGINE && window.flags hasnt Wf.NoTitleBar) Hook.itemAdd!!(g,
                window.dc.lastItemRect,
                window.dc.lastItemId)
        } else   // Append
            setCurrentWindow(window)

        pushClipRect(window.innerClipRect.min, window.innerClipRect.max, true)

        // Clear 'accessed' flag last thing (After PushClipRect which will set the flag. We want the flag to stay false when the default "Debug" window is unused)
        if (firstBeginOfTheFrame) window.writeAccessed = false

        window.beginCount++
        g.nextWindowData.clearFlags()

        // Update visibility
        if (firstBeginOfTheFrame) {
            if (flags has Wf._ChildWindow) { // Child window can be out of sight and have "negative" clip windows.
                // Mark them as collapsed so commands are skipped earlier (we can't manually collapse them because they have no title bar).
                assert(flags has Wf.NoTitleBar)
                if (flags hasnt Wf.AlwaysAutoResize && window.autoFitFrames allLessThanEqual 0) if (window.outerRectClipped.min.x >= window.outerRectClipped.max.x || window.outerRectClipped.min.y >= window.outerRectClipped.max.y) // TODO anyGreaterThanEqual bugged
                    window.hiddenFramesCanSkipItems = 1

                // Hide along with parent or if parent is collapsed
                parentWindow?.let {
                    if (it.collapsed || it.hiddenFramesCanSkipItems > 0) window.hiddenFramesCanSkipItems = 1
                    if (it.collapsed || it.hiddenFramesCannotSkipItems > 0) window.hiddenFramesCannotSkipItems = 1
                }
            }

            // Don't render if style alpha is 0.0 at the time of Begin(). This is arbitrary and inconsistent but has been there for a long while (may remove at some point)
            if (style.alpha <= 0f) window.hiddenFramesCanSkipItems = 1

            // Update the Hidden flag
            window.hidden = window.hiddenFramesCanSkipItems > 0 || window.hiddenFramesCannotSkipItems > 0

            // Update the SkipItems flag, used to early out of all items functions (no layout required)
            window.skipItems = window.run {
                (collapsed || !active || hidden) && autoFitFrames allLessThanEqual 0 && hiddenFramesCannotSkipItems <= 0
            }
        }

        return !window.skipItems
    }

    /** Always call even if Begin() return false (which indicates a collapsed window)! finish appending to current window,
     *  pop it off the window stack.    */
    fun end() {

        val window = g.currentWindow!!

        // Error checking: verify that user hasn't called End() too many times!
        if (g.currentWindowStack.size <= 1 && g.withinFrameScopeWithImplicitWindow) {
            assert(g.currentWindowStack.size > 1) { "Calling End() too many times!" }
            return
        }
        assert(g.currentWindowStack.isNotEmpty())

        // Error checking: verify that user doesn't directly call End() on a child window.
        if (window.flags has Wf._ChildWindow) assert(g.withinEndChild) { "Must call EndChild() and not End()!" }

        // Close anything that is open
        if (window.dc.currentColumns != null) endColumns()
        popClipRect()   // Inner window clip rectangle

        // Stop logging
        if (window.flags hasnt Wf._ChildWindow)    // FIXME: add more options for scope of logging
            logFinish()

        // Pop from window stack
        g.currentWindowStack.pop()
        if (window.flags has Wf._Popup) g.beginPopupStack.pop()
        errorCheckBeginEndCompareStacksSize(window, false)
        setCurrentWindow(g.currentWindowStack.lastOrNull())
    }
}