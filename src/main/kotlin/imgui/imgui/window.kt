package imgui.imgui

import gli_.has
import gli_.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.F32_TO_INT8_SAT
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeButton
import imgui.ImGui.collapseButton
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endColumns
import imgui.ImGui.findBestWindowPosForPopup
import imgui.ImGui.findWindowByName
import imgui.ImGui.frontMostPopupModal
import imgui.ImGui.getColorU32
import imgui.ImGui.getColumnOffset
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.navInitWindow
import imgui.ImGui.popClipRect
import imgui.ImGui.pushClipRect
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderTextClipped
import imgui.ImGui.scrollbar
import imgui.ImGui.style
import imgui.imgui.imgui_main.Companion.resizeGripDef
import imgui.imgui.imgui_main.Companion.updateManualResize
import imgui.internal.*
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.ItemFlag as If
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.DrawListFlag as Dlf
import imgui.internal.LayoutType as Lt


/** - Begin() = push window to the stack and start appending to it. End() = pop window from the stack.
 *  - You may append multiple times to the same window during the same frame.
 *  - Passing 'bool* p_open != NULL' shows a window-closing widget in the upper-right corner of the window,
 *      which clicking will set the boolean to false when clicked.
 *  - Begin() return false to indicate the window is collapsed or fully clipped, so you may early out and omit submitting anything to the window.
 *    Always call a matching End() for each Begin() call, regardless of its return value
 *    [this is due to legacy reason and is inconsistent with most other functions such as BeginMenu/EndMenu, BeginPopup/EndPopup, etc.
 *    where the EndXXX call should only be called if the corresponding BeginXXX function returned true.]    */
interface imgui_window {

    // Windows

    /*  Push a new ImGui window to add widgets to:
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
            be set to false when the button is pressed. */
    // TODO mage begin pOpen default
    fun begin_(name: String, pOpen: KMutableProperty0<Boolean>?, flags: Int = 0) =
            if (pOpen != null) {
                var open by pOpen
                val bool = booleanArrayOf(open)
                val res = begin(name, bool, flags)
                open = bool[0]
                res
            } else
                begin(name, null, flags)

    fun begin(name: String, pOpen: BooleanArray? = null, flags_: WindowFlags = 0): Boolean {

        assert(name.isNotEmpty()) { "Window name required" }
        assert(g.frameScopeActive) { "Forgot to call ImGui::newFrame()" }
        assert(g.frameCountEnded != g.frameCount) { "Called ImGui::render() or ImGui::rndFrame() and haven't called ImGui::newFrame() again yet" }

        var flags = flags_

        // Find or create
        var windowJustCreated = false
        val window = findWindowByName(name) ?: run {
            // Any condition flag will do since we are creating a new window here.
            val sizeOnFirstUse = if (g.nextWindowData.sizeCond != Cond.None) Vec2(g.nextWindowData.sizeVal) else Vec2()
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

        // Update Flags, LastFrameActive, BeginOrderXXX fields
        if (firstBeginOfTheFrame)
            window.flags = flags
        else
            flags = window.flags

        // Parent window is latched only on the first call to Begin() of the frame, so further append-calls can be done from a different window stack
        val parentWindowInStack = g.currentWindowStack.lastOrNull()
        val parentWindow = when {
            firstBeginOfTheFrame -> parentWindowInStack.takeIf { flags has (Wf.ChildWindow or Wf.Popup) }
            else -> window.parentWindow
        }
        assert(parentWindow != null || flags hasnt Wf.ChildWindow)
        window.hasCloseButton = pOpen != null

        // Update the Appearing flag
        // Not using !WasActive because the implicit "Debug" window would always toggle off->on
        var windowJustActivatedByUser = window.lastFrameActive < currentFrame - 1
        val windowJustAppearingAfterHiddenForResize = window.hiddenFramesForResize > 0
        if (flags has Wf.Popup) {
            val popupRef = g.openPopupStack[g.beginPopupStack.size]
            // We recycle popups so treat window as activated if popup id changed
            windowJustActivatedByUser = windowJustActivatedByUser || window.popupId != popupRef.popupId
            windowJustActivatedByUser = windowJustActivatedByUser || window !== popupRef.window
        }
        window.appearing = windowJustActivatedByUser || windowJustAppearingAfterHiddenForResize
        if (window.appearing) window.setConditionAllowFlags(Cond.Appearing.i, true)

        // Add to stack
        g.currentWindowStack += window
        window.setCurrent()
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
        if (g.nextWindowData.posCond != Cond.None) {
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
        if (g.nextWindowData.sizeCond != Cond.None) {
            windowSizeXsetByApi = window.setWindowSizeAllowFlags has g.nextWindowData.sizeCond && g.nextWindowData.sizeVal.x > 0f
            windowSizeYsetByApi = window.setWindowSizeAllowFlags has g.nextWindowData.sizeCond && g.nextWindowData.sizeVal.y > 0f
            window.setSize(g.nextWindowData.sizeVal, g.nextWindowData.sizeCond)
        }
        if (g.nextWindowData.contentSizeCond != Cond.None) {
            // Adjust passed "client size" to become a "window size"
            window.sizeContentsExplicit put g.nextWindowData.contentSizeVal
            if (window.sizeContentsExplicit.y != 0f)
                window.sizeContentsExplicit.y += window.titleBarHeight + window.menuBarHeight
        } else if (firstBeginOfTheFrame)
            window.sizeContentsExplicit put 0f
        if (g.nextWindowData.collapsedCond != Cond.None)
            window.setCollapsed(g.nextWindowData.collapsedVal, g.nextWindowData.collapsedCond)
        if (g.nextWindowData.focusCond != Cond.None)
            window.focus()
        if (window.appearing)
            window.setConditionAllowFlags(Cond.Appearing.i, false)

        // When reusing window again multiple times a frame, just append content (don't need to setup again)
        if (firstBeginOfTheFrame) {
            // Initialize
            val windowIsChildTooltip = flags has Wf.ChildWindow && flags has Wf.Tooltip // FIXME-WIP: Undocumented behavior of Child+Tooltip for pinned tooltip (#1345)
            window.updateParentAndRootLinks(flags, parentWindow)

            window.active = true
            window.beginOrderWithinParent = 0
            window.beginOrderWithinContext = g.windowsActiveCount++
            window.beginCount = 0
            window.clipRect.put(-Float.MAX_VALUE, -Float.MAX_VALUE, +Float.MAX_VALUE, +Float.MAX_VALUE)
            window.lastFrameActive = currentFrame
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
            if (window.hiddenFramesRegular > 0)
                window.hiddenFramesRegular--
            if (window.hiddenFramesForResize > 0)
                window.hiddenFramesForResize--

            // Hide new windows for one frame until they calculate their size
            if (windowJustCreated && (!windowSizeXsetByApi || !windowSizeYsetByApi))
                window.hiddenFramesForResize = 1

            /*  Hide popup/tooltip window when re-opening while we measure size (because we recycle the windows)
                We reset Size/SizeContents for reappearing popups/tooltips early in this function,
                so further code won't be tempted to use the old size.             */
            if (windowJustActivatedByUser && flags has (Wf.Popup or Wf.Tooltip)) {
                window.hiddenFramesForResize = 1
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

            val windowPosWithPivot = window.setWindowPosVal.x != Float.MAX_VALUE && window.hiddenFramesForResize == 0
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
            if (flags hasnt Wf.ChildWindow)
            /*  Ignore zero-sized display explicitly to avoid losing positions if a window manager reports zero-sized
        window when initializing or minimizing. */
                if (!windowPosSetByApi && window.autoFitFrames.x <= 0 && window.autoFitFrames.y <= 0 && io.displaySize allGreaterThan 0) {
                    val padding = glm.max(style.displayWindowPadding, style.displaySafeAreaPadding)
                    window.pos = glm.max(window.pos + window.size, padding) - window.size
                    window.pos.x = glm.min(window.pos.x, (io.displaySize.x - padding.x).f)
                    window.pos.y = glm.min(window.pos.y, (io.displaySize.y - padding.y).f)
                }
            window.pos put glm.floor(window.pos)

            // Lock window rounding for the frame (so that altering them doesn't cause inconsistencies)
            window.windowRounding = when {
                flags has Wf.ChildWindow -> style.childRounding
                else -> when {
                    flags has Wf.Popup && flags hasnt Wf.Modal -> style.popupRounding
                    else -> style.windowRounding
                }
            }

            // Prepare for item focus requests
            window.focusIdxAllRequestCurrent = when {
                window.focusIdxAllRequestNext == Int.MAX_VALUE || window.focusIdxAllCounter == -1 -> Int.MAX_VALUE
                else -> (window.focusIdxAllRequestNext + (window.focusIdxAllCounter + 1)) % (window.focusIdxAllCounter + 1)
            }
            window.focusIdxTabRequestCurrent = when {
                window.focusIdxTabRequestNext == Int.MAX_VALUE || window.focusIdxTabCounter == -1 -> Int.MAX_VALUE
                else -> (window.focusIdxTabRequestNext + (window.focusIdxTabCounter + 1)) % (window.focusIdxTabCounter + 1)
            }
            window.focusIdxTabCounter = -1
            window.focusIdxAllCounter = -1
            window.focusIdxTabRequestNext = Int.MAX_VALUE
            window.focusIdxAllRequestNext = Int.MAX_VALUE

            // Apply scrolling
            window.scroll put calcNextScrollFromScrollTargetAndClamp(window, true)
            window.scrollTarget put Float.MAX_VALUE

            // Apply window focus (new and reactivated windows are moved to front)
            val wantFocus = windowJustActivatedByUser && flags hasnt Wf.NoFocusOnAppearing &&
                    (flags hasnt (Wf.ChildWindow or Wf.Tooltip) || flags has Wf.Popup)

            // Handle manual resize: Resize Grips, Borders, Gamepad
            val borderHeld = -1
            val resizeGripCol = IntArray(4)
            val resizeGripCount = if (io.configWindowsResizeFromEdges) 2 else 1 // 4
            val gripDrawSize = max(g.fontSize * 1.35f, window.windowRounding + 1f + g.fontSize * 0.2f).i.f
            if (!window.collapsed)
                updateManualResize(window, sizeAutoFit, borderHeld, resizeGripCount, resizeGripCol)

            // Default item width. Make it proportional to window size if window manually resizes
            window.itemWidthDefault = when {
                window.size.x > 0f && flags hasnt Wf.Tooltip && flags hasnt Wf.AlwaysAutoResize -> window.size.x * 0.65f
                else -> g.fontSize * 16f
            }.i.f


            /* ---------- DRAWING ---------- */

            // Setup draw list and outer clipping rectangle
            window.drawList.clear()
            window.drawList.flags = (if (style.antiAliasedLines) Dlf.AntiAliasedLines.i else 0) or if (style.antiAliasedFill) Dlf.AntiAliasedFill.i else 0
            window.drawList.pushTextureId(g.font.containerAtlas.texId)
            val viewportRect = Rect(getViewportRect())
            if (flags has Wf.ChildWindow && flags hasnt Wf.Popup && !windowIsChildTooltip)
                pushClipRect(parentWindow!!.clipRect.min, parentWindow.clipRect.max, true)
            else
                pushClipRect(viewportRect.min, viewportRect.max, true)

            // Draw modal window background (darkens what is behind them, all viewports)
            val dimBgForModal = flags has Wf.Modal && window === frontMostPopupModal && window.hiddenFramesForResize <= 0
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

            // Draw window + handle manual resize
            val windowRounding = window.windowRounding
            val windowBorderSize = window.windowBorderSize
            val windowToHighlight = g.navWindowingTarget ?: g.navWindow
            val titleBarIsHighlight = wantFocus || (windowToHighlight?.let { window.rootWindowForTitleBarHighlight === it.rootWindowForTitleBarHighlight }
                    ?: false)
            val titleBarRect = window.titleBarRect()
            if (window.collapsed) {
                // Title bar only
                val backupBorderSize = style.frameBorderSize
                g.style.frameBorderSize = window.windowBorderSize
                val titleBarCol = (if (titleBarIsHighlight && !g.navDisableHighlight) Col.TitleBgActive else Col.TitleBgCollapsed)
                renderFrame(titleBarRect.min, titleBarRect.max, titleBarCol.u32, true, windowRounding)
                style.frameBorderSize = backupBorderSize
            } else {

                // Window background
                if (flags hasnt Wf.NoBackground) {
                    var bgCol = getWindowBgColorIdxFromFlags(flags).u32
                    if (g.nextWindowData.bgAlphaCond != Cond.None)
                        bgCol = (bgCol and COL32_A_MASK.inv()) or (F32_TO_INT8_SAT(g.nextWindowData.bgAlphaVal) shl COL32_A_SHIFT)
                    window.drawList.addRectFilled(window.pos + Vec2(0f, window.titleBarHeight), window.pos + window.size, bgCol, windowRounding,
                            if (flags has Wf.NoTitleBar) Dcf.All.i else Dcf.Bot.i)
                }
                g.nextWindowData.bgAlphaCond = Cond.None

                // Title bar
                if (flags hasnt Wf.NoTitleBar) {
                    val titleBarCol = if (window.collapsed) Col.TitleBgCollapsed else if (titleBarIsHighlight) Col.TitleBgActive else Col.TitleBg
                    window.drawList.addRectFilled(titleBarRect.min, titleBarRect.max, titleBarCol.u32, windowRounding, Dcf.Top.i)
                }

                // Menu bar
                if (flags has Wf.MenuBar) {
                    val menuBarRect = window.menuBarRect()
                    // Soft clipping, in particular child window don't have minimum size covering the menu bar so this is useful for them.
                    menuBarRect clipWith window.rect()
                    val rounding = if (flags has Wf.NoTitleBar) windowRounding else 0f
                    window.drawList.addRectFilled(menuBarRect.min, menuBarRect.max, Col.MenuBarBg.u32, rounding, Dcf.Top.i)
                    if (style.frameBorderSize > 0f && menuBarRect.max.y < window.pos.y + window.size.y)
                        window.drawList.addLine(menuBarRect.bl, menuBarRect.br, Col.Border.u32, style.frameBorderSize)
                }

                // Scrollbars
                if (window.scrollbar.x) scrollbar(Lt.Horizontal)
                if (window.scrollbar.y) scrollbar(Lt.Vertical)

                // Render resize grips (after their input handling so we don't have a frame of latency)
                if (flags hasnt Wf.NoResize)
                    for (resizeGripN in 0 until resizeGripCount) {
                        val grip = resizeGripDef[resizeGripN]
                        val corner = window.pos.lerp(window.pos + window.size, grip.cornerPos)
                        with(window.drawList) {
                            pathLineTo(corner + grip.innerDir * (if (resizeGripN has 1) Vec2(windowBorderSize, gripDrawSize) else Vec2(gripDrawSize, windowBorderSize)))
                            pathLineTo(corner + grip.innerDir * (if (resizeGripN has 1) Vec2(gripDrawSize, windowBorderSize) else Vec2(windowBorderSize, gripDrawSize)))
                            pathArcToFast(Vec2(corner.x + grip.innerDir.x * (windowRounding + windowBorderSize), corner.y + grip.innerDir.y * (windowRounding + windowBorderSize)), windowRounding, grip.angleMin12, grip.angleMax12)
                            pathFillConvex(resizeGripCol[resizeGripN])
                        }
                    }

                // Borders
                if (windowBorderSize > 0f && flags hasnt Wf.NoBackground)
                    window.drawList.addRect(Vec2(window.pos), window.size + window.pos, Col.Border.u32, windowRounding, Dcf.All.i, windowBorderSize)
                if (borderHeld != -1) {
                    val border = window.getResizeBorderRect(borderHeld, gripDrawSize, 0f)
                    window.drawList.addLine(border.min, border.max, Col.SeparatorActive.u32, max(1f, windowBorderSize))
                }
                if (style.frameBorderSize > 0 && flags hasnt Wf.NoTitleBar)
                    window.drawList.addLine(titleBarRect.bl + Vec2(style.windowBorderSize, -1),
                            titleBarRect.br + Vec2(style.windowBorderSize, -1), Col.Border.u32, style.frameBorderSize)
            }

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

                /*  Update ContentsRegionMax. Variables they depends on are set above in this function.
                    FIXME: window->ContentsRegionRect.Max is currently very misleading / partly faulty,
                    but some BeginChild() patterns relies on it.                 */
                contentsRegionRect.let {
                    it.min.put(pos.x - scroll.x + windowPadding.x,
                            pos.y - scroll.y + windowPadding.y + titleBarHeight + menuBarHeight)
                    val a = if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else size.x - scrollbarSizes.x
                    val b = if (sizeContentsExplicit.y != 0f) sizeContentsExplicit.y else size.y - scrollbarSizes.y
                    it.max.put(pos.x - scroll.x - windowPadding.x + a,
                            pos.y - scroll.y - windowPadding.y + b)
                }

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
                dc.currentLineSize put 0f
                dc.prevLineTextBaseOffset = 0f
                dc.currentLineTextBaseOffset = 0f
                dc.navHideHighlightOneFrame = false
                dc.navHasScroll = window.scrollMaxY > 0f
                dc.navLayerActiveMask = window.dc.navLayerActiveMaskNext
                dc.navLayerActiveMaskNext = 0
                dc.menuBarAppending = false
                dc.logLinePosY = dc.cursorPos.y - 9999f
                dc.childWindows.clear()
                dc.layoutType = Lt.Vertical
                dc.parentLayoutType = parentWindow?.dc?.layoutType ?: Lt.Vertical
                dc.itemFlags = parentWindow?.dc?.itemFlags ?: If.Default_.i
                dc.itemWidth = itemWidthDefault
                dc.textWrapPos = -1f // disabled
                dc.itemFlagsStack.clear()
                dc.itemWidthStack.clear()
                dc.textWrapPosStack.clear()
                dc.columnsSet = null
                dc.treeDepth = 0
                dc.treeDepthMayJumpToParentOnPop = 0
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
            if (flags hasnt Wf.NoTitleBar) {
                // Close & collapse button are on layer 1 (same as menus) and don't default focus
                val itemFlagsBackup = window.dc.itemFlags
                window.dc.itemFlags = window.dc.itemFlags or If.NoNavDefaultFocus
                window.dc.navLayerCurrent = NavLayer.Menu
                window.dc.navLayerCurrentMask = 1 shl NavLayer.Menu.i

                // Collapse button
                if (flags hasnt Wf.NoCollapse)
                    if (collapseButton(window.getId("#COLLAPSE"), window.pos + style.framePadding))
                        window.wantCollapseToggle = true // Defer collapsing to next frame as we are too far in the Begin() function

                // Close button
                if (pOpen != null) {
                    val pad = style.framePadding.y
                    val rad = g.fontSize * 0.5f
                    if (closeButton(window.getId("#CLOSE"), window.rect().tr + Vec2(-pad - rad, pad + rad), rad + 1))
                        pOpen[0] = false
                }

                window.dc.navLayerCurrent = NavLayer.Main
                window.dc.navLayerCurrentMask = 1 shl NavLayer.Main.i
                window.dc.itemFlags = itemFlagsBackup

                // Title bar text (with: horizontal alignment, avoiding collapse/close button, optional "unsaved document" marker)
                // FIXME: Refactor text alignment facilities along with RenderText helpers, this is too much code..
                val UNSAVED_DOCUMENT_MARKER = "*"
                val markerSizeX = if (flags has Wf.UnsavedDocument) calcTextSize(UNSAVED_DOCUMENT_MARKER, 0, false).x else 0f
                val textSize = calcTextSize(name, 0, true) + Vec2(markerSizeX, 0f)
                val textR = Rect(titleBarRect)
                val padLeft = when {
                    flags has Wf.NoCollapse -> style.framePadding.x
                    else -> style.framePadding.x + g.fontSize + style.itemInnerSpacing.x
                }
                var padRight = when (pOpen) {
                    null -> style.framePadding.x
                    else -> style.framePadding.x + g.fontSize + style.itemInnerSpacing.x
                }
                if (style.windowTitleAlign.x > 0f)
                    padRight = lerp(padRight, padLeft, style.windowTitleAlign.x)
                textR.min.x += padLeft
                textR.max.x -= padRight
                val clipRect = Rect(textR)
                // Match the size of CloseButton()
                clipRect.max.x = window.pos.x + window.size.x - (if (pOpen?.get(0) == true) titleBarRect.height - 3 else style.framePadding.x)
                renderTextClipped(textR.min, textR.max, name, 0, textSize, style.windowTitleAlign, clipRect)
                if (flags has Wf.UnsavedDocument) {
                    val markerPos = Vec2(max(textR.min.x, textR.min.x + (textR.width - textSize.x) * style.windowTitleAlign.x) + textSize.x, textR.min.y) + Vec2(2 - markerSizeX, 0f)
                    val off = Vec2(0f, (-g.fontSize * 0.25f).i.f)
                    renderTextClipped(markerPos + off, textR.max + off, UNSAVED_DOCUMENT_MARKER, 0, null, Vec2(0, style.windowTitleAlign.y), clipRect)
                }
            }

            // Save clipped aabb so we can access it in constant-time in FindHoveredWindow()
            window.outerRectClipped put window.rect()
            window.outerRectClipped clipWith window.clipRect

            // Pressing CTRL+C while holding on a window copy its content to the clipboard
            // This works but 1. doesn't handle multiple Begin/End pairs, 2. recursing into another Begin/End pair - so we need to work that out and add better logging scope.
            // Maybe we can support CTRL+C on every element?
            /*
        if (g.ActiveId == move_id)
            if (g.io.KeyCtrl && IsKeyPressedMap(ImGuiKey_C))
                ImGui::LogToClipboard();
        */
            /*  Inner rectangle
            We set this up after processing the resize grip so that our clip rectangle doesn't lag by a frame
            Note that if our window is collapsed we will end up with an inverted (~null) clipping rectangle which is the correct behavior.   */
            window.innerMainRect.min.x = titleBarRect.min.x + window.windowBorderSize
            window.innerMainRect.min.y = titleBarRect.max.y + window.menuBarHeight + if (flags has Wf.MenuBar || flags hasnt Wf.NoTitleBar) style.frameBorderSize else window.windowBorderSize
            window.innerMainRect.max.x = window.pos.x + window.size.x - window.scrollbarSizes.x - window.windowBorderSize
            window.innerMainRect.max.y = window.pos.y + window.size.y - window.scrollbarSizes.y - window.windowBorderSize
            //window->DrawList->AddRect(window->InnerRect.Min, window->InnerRect.Max, IM_COL32_WHITE);

            // Inner clipping rectangle
            // Force round operator last to ensure that e.g. (int)(max.x-min.x) in user's render code produce correct result.
            window.innerClipRect.min.x = floor(0.5f + window.innerMainRect.min.x + max(0f, floor(window.windowPadding.x * 0.5f - window.windowBorderSize)))
            window.innerClipRect.min.y = floor(0.5f + window.innerMainRect.min.y)
            window.innerClipRect.max.x = floor(0.5f + window.innerMainRect.max.x - max(0f, floor(window.windowPadding.x * 0.5f - window.windowBorderSize)))
            window.innerClipRect.max.y = floor(0.5f + window.innerMainRect.max.y)

            // After Begin() we fill the last item / hovered data based on title bar data. It is a standard behavior (to allow creation of context menus on title bar only, etc.).
            window.dc.lastItemId = window.moveId
            window.dc.lastItemStatusFlags = if (isMouseHoveringRect(titleBarRect.min, titleBarRect.max)) ItemStatusFlag.HoveredRect.i else 0
            window.dc.lastItemRect = titleBarRect
        }

        pushClipRect(window.innerClipRect.min, window.innerClipRect.max, true)

        // Clear 'accessed' flag last thing
        if (firstBeginOfTheFrame) window.writeAccessed = false

        window.beginCount++
        g.nextWindowData.clear()

        if (flags has Wf.ChildWindow) {

            // Child window can be out of sight and have "negative" clip windows.
            // Mark them as collapsed so commands are skipped earlier (we can't manually collapse them because they have no title bar).
            assert(flags has Wf.NoTitleBar)

            if (flags hasnt Wf.AlwaysAutoResize && window.autoFitFrames allLessThanEqual 0)
                if (window.outerRectClipped.min anyGreaterThanEqual window.outerRectClipped.max)
                    window.hiddenFramesRegular = 1

            // Completely hide along with parent or if parent is collapsed
            parentWindow?.let {
                if (it.collapsed || it.hidden)
                    window.hiddenFramesRegular = 1
            }
        }

        // Don't render if style alpha is 0.0 at the time of Begin(). This is arbitrary and inconsistent but has been there for a long while (may remove at some point)
        if (style.alpha <= 0f)
            window.hiddenFramesRegular = 1

        // Update the Hidden flag
        window.hidden = window.hiddenFramesRegular > 0 || window.hiddenFramesForResize > 0

        // Return false if we don't intend to display anything to allow user to perform an early out optimization
        window.apply {
            skipItems = (collapsed || !active || hidden) && autoFitFrames allLessThanEqual 0 && hiddenFramesForResize <= 0
        }
        return !window.skipItems
    }

    /** Always call even if Begin() return false (which indicates a collapsed window)! finish appending to current window,
     *  pop it off the window stack.    */
    fun end() {

        if (g.currentWindowStack.size <= 1 && g.frameScopePushedImplicitWindow) {
            assert(g.currentWindowStack.size > 1) { "Calling End() too many times!" }
            return // FIXME-ERRORHANDLING
        }
        assert(g.currentWindowStack.isNotEmpty())

        with(g.currentWindow!!) {

            if (dc.columnsSet != null) // close columns set if any is open
                endColumns()
            popClipRect()   // Inner window clip rectangle

            // Stop logging
//TODO            if (flags hasnt WindowFlag.ChildWindow)    // FIXME: add more options for scope of logging
//                logFinish()

            // Pop from window stack
            g.currentWindowStack.pop()
            if (flags has Wf.Popup)
                g.beginPopupStack.pop()
            checkStacksSize(this, false)
            setCurrentWindow(g.currentWindowStack.lastOrNull())
        }
    }

    // Child Windows

    /** - Use child windows to begin into a self-contained independent scrolling/clipping regions within a host window. Child windows can embed their own child.
     *  - For each independent axis of 'size': ==0.0f: use remaining host window size / >0.0f: fixed size
     *      / <0.0f: use remaining window size minus abs(size) / Each axis can use a different mode, e.g. ImVec2(0,400).
     *  - BeginChild() returns false to indicate the window is collapsed or fully clipped, so you may early out and omit submitting anything to the window.
     *    Always call a matching EndChild() for each BeginChild() call, regardless of its return value [this is due to legacy reason and
     *    is inconsistent with most other functions such as BeginMenu/EndMenu, BeginPopup/EndPopup, etc. where the EndXXX call
     *    should only be called if the corresponding BeginXXX function returned true.]  */
    fun beginChild(strId: String, size: Vec2 = Vec2(), border: Boolean = false, flags: WindowFlags = 0) =
            beginChildEx(strId, currentWindow.getId(strId), size, border, flags)

    /** begin a scrolling region.
     *  size == 0f: use remaining window size
     *  size < 0f: use remaining window size minus abs(size)
     *  size > 0f: fixed size. each axis can use a different mode, e.g. Vec2(0, 400).   */
    fun beginChild(id: ID, sizeArg: Vec2 = Vec2(), border: Boolean = false, flags: WindowFlags = 0): Boolean {
        assert(id != 0)
        return beginChildEx("", id, sizeArg, border, flags)
    }

    /** Always call even if BeginChild() return false (which indicates a collapsed or clipping child window)    */
    fun endChild() {

        val window = currentWindow

        assert(window.flags has Wf.ChildWindow) { "Mismatched BeginChild()/EndChild() callss" }
        if (window.beginCount > 1) end()
        else {
            /*  When using auto-filling child window, we don't provide full width/height to ItemSize so that it doesn't
                feed back into automatic size-fitting.             */
            val sz = Vec2(window.size)
            // Arbitrary minimum zero-ish child size of 4.0f causes less trouble than a 0.0f
            if (window.autoFitChildAxes has (1 shl Axis.X))
                sz.x = glm.max(4f, sz.x)
            if (window.autoFitChildAxes has (1 shl Axis.Y))
                sz.y = glm.max(4f, sz.y)
            end()

            val parentWindow = currentWindow
            val bb = Rect(parentWindow.dc.cursorPos, parentWindow.dc.cursorPos + sz)
            itemSize(sz)
            if ((window.dc.navLayerActiveMask != 0 || window.dc.navHasScroll) && window.flags hasnt Wf.NavFlattened) {
                itemAdd(bb, window.childId)
                renderNavHighlight(bb, window.childId)

                // When browsing a window that has no activable items (scroll only) we keep a highlight on the child
                if (window.dc.navLayerActiveMask == 0 && window === g.navWindow)
                    renderNavHighlight(Rect(bb.min - 2, bb.max + 2), g.navId, NavHighlightFlag.TypeThin.i)
            } else // Not navigable into
                itemAdd(bb, 0)
        }
    }

    // Windows Utilities
    // - "current window" = the window we are appending into while inside a Begin()/End() block. "next window" = next window we will Begin() into.


    val isWindowAppearing get() = currentWindowRead!!.appearing

    val isWindowCollapsed get() = currentWindowRead!!.collapsed

    /** is current window focused? or its root/child, depending on flags. see flags for options.    */
    fun isWindowFocused(flag: Ff) = isWindowFocused(flag.i)

    /** is current window focused? or its root/child, depending on flags. see flags for options.    */
    fun isWindowFocused(flags: FocusedFlags = Ff.None.i): Boolean {

        if (flags has Ff.AnyWindow)
            return g.navWindow != null

        val curr = g.currentWindow!!     // Not inside a Begin()/End()
        return when (flags and (Ff.RootWindow or Ff.ChildWindows)) {
            Ff.RootWindow or Ff.ChildWindows -> g.navWindow?.let { it.rootWindow === curr.rootWindow } ?: false
            Ff.RootWindow.i -> g.navWindow === curr.rootWindow
            Ff.ChildWindows.i -> g.navWindow?.isChildOf(curr) ?: false
            else -> g.navWindow === curr
        }
    }

    /** iis current window hovered (and typically: not blocked by a popup/modal)? see flag for options. */
    fun isWindowHovered(flag: Hf) = isWindowHovered(flag.i)

    /** Is current window hovered (and typically: not blocked by a popup/modal)? see flags for options.
     *  NB: If you are trying to check whether your mouse should be dispatched to imgui or to your app, you should use
     *  the 'io.wantCaptureMouse' boolean for that! Please read the FAQ!    */
    fun isWindowHovered(flags: HoveredFlags = Hf.None.i): Boolean {
        assert(flags hasnt Hf.AllowWhenOverlapped) { "Flags not supported by this function" }
        if (flags has Hf.AnyWindow) {
            if (g.hoveredWindow == null)
                return false
        } else when (flags and (Hf.RootWindow or Hf.ChildWindows)) {
            Hf.RootWindow or Hf.ChildWindows -> if (g.hoveredRootWindow !== g.currentWindow!!.rootWindow) return false
            Hf.RootWindow.i -> if (g.hoveredWindow != g.currentWindow!!.rootWindow) return false
            Hf.ChildWindows.i -> g.hoveredWindow.let { if (it == null || !it.isChildOf(g.currentWindow)) return false }
            else -> if (g.hoveredWindow !== g.currentWindow) return false
        }

        return when {
            !g.hoveredRootWindow!!.isContentHoverable(flags) -> false
            flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && !g.activeIdAllowOverlap && g.activeId != g.hoveredWindow!!.moveId -> false
            else -> true
        }
    }

    /** get rendering command-list if you want to append your own draw primitives   */
    val windowDrawList get() = currentWindow.drawList

    /** get current window position in screen space (useful if you want to do your own drawing via the DrawList api)    */
    val windowPos get() = g.currentWindow!!.pos

    /** get current window size */
    val windowSize get() = currentWindowRead!!.size

    val windowWidth get() = g.currentWindow!!.size.x

    val windowHeight get() = g.currentWindow!!.size.y

    /** current content boundaries (typically window boundaries including scrolling, or current column boundaries), in
     *  windows coordinates
     *  In window space (not screen space!) */
    val contentRegionMax: Vec2
        get() = currentWindowRead!!.run {
            val mx = contentsRegionRect.max - pos
            dc.columnsSet?.let { mx.x = getColumnOffset(it.current + 1) - windowPadding.x }
            mx
        }

    /** == GetContentRegionMax() - GetCursorPos()   */
    val contentRegionAvail
        get() = with(currentWindowRead!!) {
            contentRegionMax - (dc.cursorPos - pos)
        }

    val contentRegionAvailWidth get() = contentRegionAvail.x
    /** content boundaries min (roughly (0,0)-Scroll), in window coordinates    */
    val windowContentRegionMin: Vec2
        get() = currentWindowRead!!.run { contentsRegionRect.min - pos }
    /** content boundaries max (roughly (0,0)+Size-Scroll) where Size can be override with SetNextWindowContentSize(),
     * in window coordinates    */
    val windowContentRegionMax: Vec2
        get() = currentWindowRead!!.run { contentsRegionRect.max - pos }

    val windowContentRegionWidth: Float
        get() = currentWindowRead!!.contentsRegionRect.width


    /** set next window position. call before Begin()   */
    fun setNextWindowPos(pos: Vec2, cond: Cond = Cond.Always, pivot: Vec2 = Vec2()) {
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            posVal put pos
            posPivotVal put pivot
            posCond = cond
        }
    }

    /** set next window size. set axis to 0.0f to force an auto-fit on this axis. call before Begin()   */
    fun setNextWindowSize(size: Vec2, cond: Cond = Cond.Always) {
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            sizeVal put size
            sizeCond = cond
        }
    }

    /** set next window size limits. use -1,-1 on either X/Y axis to preserve the current size. Use callback to apply
     *  non-trivial programmatic constraints.   */
    fun setNextWindowSizeConstraints(sizeMin: Vec2, sizeMax: Vec2, customCallback: SizeCallback? = null, customCallbackUserData: Any? = null) {
        with(g.nextWindowData) {
            sizeConstraintCond = Cond.Always
            sizeConstraintRect.min put sizeMin
            sizeConstraintRect.max put sizeMax
            sizeCallback = customCallback
            sizeCallbackUserData = customCallbackUserData
        }
    }

    /** Set next window content size (~ enforce the range of scrollbars). not including window decorations (title bar, menu bar, etc.).
     *  set an axis to 0.0f to leave it automatic. call before Begin() */
    fun setNextWindowContentSize(size: Vec2) {
        // In Begin() we will add the size of window decorations (title bar, menu etc.) to that to form a SizeContents value.
        with(g.nextWindowData) {
            contentSizeVal put size
            contentSizeCond = Cond.Always
        }
    }

    /** Set next window collapsed state. call before Begin()    */
    fun setNextWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.Always) {
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            collapsedVal = collapsed
            collapsedCond = cond
        }
    }

    /** Set next window to be focused / front-most. call before Begin() */
    fun setNextWindowFocus() {
        // Using a Cond member for consistency (may transition all of them to single flag set for fast Clear() op)
        g.nextWindowData.focusCond = Cond.Always
    }

    /** Set next window background color alpha. helper to easily modify ImGuiCol_WindowBg/ChildBg/PopupBg.
     *  You may also use ImGuiWindowFlags_NoBackground. */
    fun setNextWindowBgAlpha(alpha: Float) {
        g.nextWindowData.bgAlphaVal = alpha
        // Using a Cond member for consistency (may transition all of them to single flag set for fast Clear() op)
        g.nextWindowData.bgAlphaCond = Cond.Always
    }

    /** (not recommended) set current window position - call within Begin()/End(). prefer using SetNextWindowPos(),
     *  as this may incur tearing and side-effects. */
    fun setWindowPos(pos: Vec2, cond: Cond = Cond.None) = currentWindowRead!!.setPos(pos, cond)

    /** (not recommended) set current window size - call within Begin()/End(). set to ImVec2(0,0) to force an auto-fit.
     *  prefer using SetNextWindowSize(), as this may incur tearing and minor side-effects. */
    fun setWindowSize(size: Vec2, cond: Cond = Cond.None) = g.currentWindow!!.setSize(size, cond)

    /** (not recommended) set current window collapsed state. prefer using SetNextWindowCollapsed().    */
    fun setWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.None) = g.currentWindow!!.setCollapsed(collapsed, cond)

    /** (not recommended) set current window to be focused / front-most. prefer using SetNextWindowFocus(). */
    fun setWindowFocus() = g.currentWindow.focus()

    /** Set named window position.  */
    fun setWindowPos(name: String, pos: Vec2, cond: Cond = Cond.None) = findWindowByName(name)?.setPos(pos, cond)

    /** Set named window size. set axis to 0.0f to force an auto-fit on this axis.  */
    fun setWindowSize(name: String, size: Vec2, cond: Cond = Cond.None) = findWindowByName(name)?.setSize(size, cond)

    /** Set named window collapsed state    */
    fun setWindowCollapsed(name: String, collapsed: Boolean, cond: Cond = Cond.None) = findWindowByName(name)?.setCollapsed(collapsed, cond)

    /** Set named window to be focused / front-most. use NULL to remove focus.  */
    fun setWindowFocus(name: String) = findWindowByName(name).focus()

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