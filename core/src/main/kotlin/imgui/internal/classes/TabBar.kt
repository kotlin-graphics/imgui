package imgui.internal.classes

import glm_.has
import glm_.hasnt
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.arrowButtonEx
import imgui.ImGui.beginCombo
import imgui.ImGui.buttonBehavior
import imgui.ImGui.dockContextQueueUndockWindow
import imgui.ImGui.endCombo
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.getIDWithSeed
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMouseReleased
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.keepAliveID
import imgui.ImGui.markIniSettingsDirty
import imgui.ImGui.popClipRect
import imgui.ImGui.popItemFlag
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushItemFlag
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.selectable
import imgui.ImGui.setActiveID
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setTooltip
import imgui.ImGui.shrinkWidths
import imgui.ImGui.style
import imgui.ImGui.tabItemBackground
import imgui.ImGui.tabItemCalcSize
import imgui.ImGui.tabItemLabelAndCloseButton
import imgui.api.g
import imgui.internal.floor
import imgui.internal.hash
import imgui.internal.linearSweep
import imgui.internal.sections.ButtonFlag
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.or
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

class TabBarSection {
    /** Number of tabs in this section. */
    var tabCount = 0

    /** Sum of width of tabs in this section (after shrinking down) */
    var width = 0f

    /** Horizontal spacing at the end of the section. */
    var spacing = 0f
}

/** Storage for a tab bar (sizeof() 92~96 bytes) */
class TabBar {
    val tabs = ArrayList<TabItem>()

    /** Zero for tab-bars used by docking */
    var id: ID = 0

    /** Selected tab/window */
    var selectedTabId: ID = 0
    var nextSelectedTabId: ID = 0

    /** Can occasionally be != SelectedTabId (e.g. when previewing contents for CTRL+TAB preview) */
    var visibleTabId: ID = 0
    var currFrameVisible = -1
    var prevFrameVisible = -1
    var barRect = Rect()

    /** Record the height of contents submitted below the tab bar */
    var lastTabContentHeight = 0f

    /** Actual width of all tabs (locked during layout) */
    var widthAllTabs = 0f

    /** Ideal width if all tabs were visible and not clipped */
    var widthAllTabsIdeal = 0f

    var scrollingAnim = 0f
    var scrollingTarget = 0f
    var scrollingTargetDistToVisibility = 0f
    var scrollingSpeed = 0f
    var scrollingRectMinX = 0f
    var scrollingRectMaxX = 0f
    var flags: TabBarFlags = TabBarFlag.None.i
    var reorderRequestTabId: ID = 0
    var reorderRequestDir = 0

    /** Number of tabs submitted this frame. */
    var tabsActiveCount = 0

    var wantLayout = false
    var visibleTabWasSubmitted = false

    /** Set to true when a new tab item or button has been added to the tab bar during last frame */
    var tabsAddedNew = false

    /** Index of last BeginTabItem() tab for use by EndTabItem()  */
    var lastTabItemIdx = -1

    /** style.FramePadding locked at the time of BeginTabBar() */
    var framePadding = Vec2()

    /** For non-docking tab bar we re-append names in a contiguous buffer. */
    val tabsNames = ArrayList<String>()

    val TabItem.order: Int
        get() = tabs.indexOf(this)

    val TabItem.name: String
        get() = window?.name ?: tabsNames[nameOffset]

    // classic overload (for external usage)
    infix fun getTabOrder(tab: TabItem): Int = tab.order
    infix fun getTabName(tab: TabItem): String = tab.name

    // Tab Bars

    /** ~ beginTabBarEx */
    fun beginEx(bb: Rect, flags__: TabBarFlags, dockNode: DockNode?): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        var flags_ = flags__
        if (flags_ hasnt TabBarFlag._DockNode) window.idStack += id

        // Add to stack
        g.currentTabBarStack += tabBarRef
        g.currentTabBar = this
        if (currFrameVisible == g.frameCount) {
            //IMGUI_DEBUG_LOG("BeginTabBarEx already called this frame\n", g.FrameCount);
//            assert(false)
            return true
        }

        // Ensure correct ordering when toggling ImGuiTabBarFlags_Reorderable flag, ensure tabs are ordered based on their submission order.
        if (flags_ has TabBarFlag.Reorderable != flags has TabBarFlag.Reorderable || (tabsAddedNew && flags hasnt TabBarFlag.Reorderable))
            if (tabs.size > 1)
                if (flags hasnt TabBarFlag._DockNode)
                    tabs.sortBy(TabItem::beginOrder)
        tabsAddedNew = false

        // Flags
        if (flags_ hasnt TabBarFlag.FittingPolicyMask_) flags_ = flags_ or TabBarFlag.FittingPolicyDefault_

        flags = flags_
        barRect = bb
        wantLayout = true // Layout will be done on the first call to ItemTab()
        prevFrameVisible = currFrameVisible
        currFrameVisible = g.frameCount
        framePadding put g.style.framePadding
        tabsActiveCount = 0

        // Layout
        // Set cursor pos in a way which only be used in the off-chance the user erroneously submits item before BeginTabItem(): items will overlap
        window.dc.cursorPos.put(barRect.min.x, barRect.max.y + style.itemSpacing.y)

        // Draw separator
        val col = if (flags has TabBarFlag._IsFocused) Col.TabActive else Col.TabUnfocusedActive
        val y = barRect.max.y - 1f
        if (dockNode != null) {
            val separatorMinX = dockNode.pos.x + window.windowBorderSize
            val separatorMaxX = dockNode.pos.x + dockNode.size.x - window.windowBorderSize
            window.drawList.addLine(Vec2(separatorMinX, y), Vec2(separatorMaxX, y), col.u32, 1f)
        } else {
            val separatorMinX = barRect.min.x - floor(window.windowPadding.x * 0.5f)
            val separatorMaxX = barRect.max.x + floor(window.windowPadding.x * 0.5f)
            window.drawList.addLine(Vec2(separatorMinX, y), Vec2(separatorMaxX, y), col.u32, 1f)
        }
        return true
    }

    // comfortable util
    val tabBarRef: PtrOrIndex?
        get() = when (this) {
            in g.tabBars -> PtrOrIndex(g.tabBars.getIndex(this))
            else -> PtrOrIndex(this)
        }

    /** ~TabBarFindTabByID */
    infix fun findTabByID(tabId: ID): TabItem? = when (tabId) {
        0 -> null
        else -> tabs.find { it.id == tabId }
    }

    /** FIXME: See references to #2304 in TODO.txt
     *  ~tabBarFindMostRecentlySelectedTabForActiveWindow */
    fun findMostRecentlySelectedTabForActiveWindow(): TabItem? {
        var mostRecentlySelectedTab: TabItem? = null
        tabs.forEach {
            if (mostRecentlySelectedTab == null || mostRecentlySelectedTab!!.lastFrameSelected < it.lastFrameSelected)
                if (it.window?.wasActive == true)
                    mostRecentlySelectedTab = it
        }
        return mostRecentlySelectedTab
    }

    /** The purpose of this call is to register tab in advance so we can control their order at the time they appear.
     *  Otherwise calling this is unnecessary as tabs are appending as needed by the BeginTabItem() function.
     *  ~TabBarAddTab */
    fun addTab(tabFlags: TabItemFlags, window: Window) {
        assert(findTabByID(window.id) == null)
        assert(g.currentTabBar !== this) { "Can't work while the tab bar is active as our tab doesn't have an X offset yet, in theory we could/should test something like (tab_bar->CurrFrameVisible < g.FrameCount) but we'd need to solve why triggers the commented early-out assert in BeginTabBarEx() (probably dock node going from implicit to explicit in same frame)" }

        val newTab = TabItem()
        newTab.id = window.id
        newTab.flags = tabFlags
        newTab.lastFrameVisible = currFrameVisible   // Required so BeginTabBar() doesn't ditch the tab
        if (newTab.lastFrameVisible == -1)
            newTab.lastFrameVisible = g.frameCount - 1
        newTab.window =
            window                                // Required so tab bar layout can compute the tab width before tab submission
        tabs += newTab
    }

    /** The *TabId fields be already set by the docking system _before_ the actual TabItem was created, so we clear them regardless.
     *  ~ tabBarRemoveTab     */
    infix fun removeTab(tabId: ID) {
        findTabByID(tabId)?.let(tabs::remove)
        if (visibleTabId == tabId) visibleTabId = 0
        if (selectedTabId == tabId) selectedTabId = 0
        if (nextSelectedTabId == tabId) nextSelectedTabId = 0
    }

    /** Called on manual closure attempt
     *  ~ tabBarCloseTab     */
    infix fun closeTab(tab: TabItem) {
        assert(tab.flags hasnt TabItemFlag._Button)
        if (tab.flags hasnt TabItemFlag.UnsavedDocument) { // This will remove a frame of lag for selecting another tab on closure.
            // However we don't run it in the case where the 'Unsaved' flag is set, so user gets a chance to fully undo the closure
            tab.wantClose = true
            if (visibleTabId == tab.id) {
                tab.lastFrameVisible = -1
                nextSelectedTabId = 0
                selectedTabId = 0
            }
        } else // Actually select before expecting closure attempt (on an UnsavedDocument tab user is expect to e.g. show a popup)
            if (visibleTabId != tab.id) nextSelectedTabId = tab.id
    }

    /** ~TabBarQueueReorder */
    fun queueReorder(tab: TabItem, dir: Int) {
        assert(dir == -1 || dir == +1)
        assert(reorderRequestTabId == 0)
        reorderRequestTabId = tab.id
        reorderRequestDir = dir
    }

    /** ~TabBarProcessReorder */
    fun processReorder(): Boolean {
        val tab1 = findTabByID(reorderRequestTabId)
        if (tab1 == null || tab1.flags has TabItemFlag.NoReorder) return false

        //IM_ASSERT(tab_bar->Flags & ImGuiTabBarFlags_Reorderable); // <- this may happen when using debug tools
        val tab2Order = tab1.order + reorderRequestDir
        if (tab2Order < 0 || tab2Order >= tabs.size) return false

        // Reordered TabItem must share the same position flags than target
        val tab2 = tabs[tab2Order]
        if (tab2.flags has TabItemFlag.NoReorder) return false
        if ((tab1.flags and (TabItemFlag.Leading or TabItemFlag.Trailing)) != (tab2.flags and (TabItemFlag.Leading or TabItemFlag.Trailing))) return false

        //        ImGuiTabItem* tab2 = &tab_bar->Tabs[tab2_order];
        //        ImGuiTabItem item_tmp = *tab1;
        //        *tab1 = *tab2;
        //        *tab2 = item_tmp;
        val itemTmp = tabs[reorderRequestTabId]
        tabs[reorderRequestTabId] = tabs[tab2Order]
        tabs[tab2Order] = itemTmp

        if (flags has TabBarFlag._SaveSettings) markIniSettingsDirty()
        return true
    }

    fun tabItemEx(
        label: String,
        pOpen_: KMutableProperty0<Boolean>?,
        flags_: TabItemFlags,
        dockedWindow: Window?
    ): Boolean {

        var pOpen = pOpen_
        var flags = flags_ // Layout whole tab bar if not already done
        if (wantLayout) layout()

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val id = calcTabID(label)

        // If the user called us with *p_open == false, we early out and don't render.
        // We make a call to ItemAdd() so that attempts to use a contextual popup menu with an implicit ID won't use an older ID.
        Hook.itemInfo?.invoke(g, id, label, window.dc.lastItemStatusFlags)
        if (pOpen?.get() == false) {
            pushItemFlag(ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus, true)
            itemAdd(Rect(), id)
            popItemFlag()
            return false
        }

        assert(pOpen == null || flags hasnt TabItemFlag._Button)
        assert((flags and (TabItemFlag.Leading or TabItemFlag.Trailing)) != (TabItemFlag.Leading or TabItemFlag.Trailing)) { "Can't use both Leading and Trailing" }

        // Store into ImGuiTabItemFlags_NoCloseButton, also honor ImGuiTabItemFlags_NoCloseButton passed by user (although not documented)
        if (flags has TabItemFlag._NoCloseButton) pOpen = null
        else if (pOpen == null) flags = flags or TabItemFlag._NoCloseButton

        // Calculate tab contents size
        val size = tabItemCalcSize(label, pOpen != null)

        // Acquire tab data
        var tabIsNew = false
        val tab = findTabByID(id) ?: TabItem().also {
            it.id = id
            it.width = size.x
            tabs += it
            tabIsNew = true
            tabsAddedNew = true
        }
        lastTabItemIdx = tabs.indexOf(tab)
        tab.contentWidth = size.x
        tab.beginOrder = tabsActiveCount++

        val tabBarAppearing = prevFrameVisible + 1 < g.frameCount
        val tabBarFocused = this.flags has TabBarFlag._IsFocused
        val tabAppearing = tab.lastFrameVisible + 1 < g.frameCount
        val isTabButton = flags has TabItemFlag._Button
        tab.lastFrameVisible = g.frameCount
        tab.flags = flags
        tab.window = dockedWindow

        // Append name with zero-terminator
        if (this.flags has TabBarFlag._DockNode) {
            assert(tab.window != null)
            tab.nameOffset = -1
        } else {
            assert(tab.window == null)
            tab.nameOffset = tabsNames.size
            tabsNames += label // Append name _with_ the zero-terminator.
        }

        // Update selected tab
        if (tabAppearing && this.flags has TabBarFlag.AutoSelectNewTabs && nextSelectedTabId == 0)
            if (!tabBarAppearing || selectedTabId == 0)
                if (!isTabButton)
                    nextSelectedTabId = id  // New tabs gets activated
        if (flags has TabItemFlag.SetSelected && selectedTabId != id) // SetSelected can only be passed on explicit tab bar
            if (!isTabButton)
                nextSelectedTabId = id

        // Lock visibility
        // (Note: tab_contents_visible != tab_selected... because CTRL+TAB operations may preview some tabs without selecting them!)
        var tabContentsVisible = visibleTabId == id
        if (tabContentsVisible)
            visibleTabWasSubmitted = true

        // On the very first frame of a tab bar we let first tab contents be visible to minimize appearing glitches
        if (!tabContentsVisible && selectedTabId == 0 && tabBarAppearing && dockedWindow == null)
            if (tabs.size == 1 && this.flags hasnt TabBarFlag.AutoSelectNewTabs)
                tabContentsVisible = true

        // Note that tab_is_new is not necessarily the same as tab_appearing! When a tab bar stops being submitted
        // and then gets submitted again, the tabs will have 'tab_appearing=true' but 'tab_is_new=false'.
        if (tabAppearing && (!tabBarAppearing || tabIsNew)) {
            pushItemFlag(ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus, true)
            itemAdd(Rect(), id)
            popItemFlag()
            return if (isTabButton) false else tabContentsVisible
        }

        if (selectedTabId == id) tab.lastFrameSelected = g.frameCount

        // Backup current layout position
        val backupMainCursorPos = Vec2(window.dc.cursorPos)

        // Layout
        val isCentralSection = tab.flags hasnt (TabItemFlag.Leading or TabItemFlag.Trailing)
        size.x = tab.width
        val x = if (isCentralSection) floor(tab.offset - scrollingAnim) else tab.offset
        window.dc.cursorPos = barRect.min + Vec2(x, 0f)
        val pos = Vec2(window.dc.cursorPos)
        val bb = Rect(pos, pos + size)

        // We don't have CPU clipping primitives to clip the CloseButton (until it becomes a texture), so need to add an extra draw call (temporary in the case of vertical animation)
        val wantClipRect = isCentralSection && (bb.min.x < scrollingRectMinX || bb.max.x > scrollingRectMaxX)
        if (wantClipRect)
            pushClipRect(Vec2(bb.min.x max scrollingRectMinX, bb.min.y - 1), Vec2(scrollingRectMaxX, bb.max.y), true)

        val backupCursorMaxPos = Vec2(window.dc.cursorMaxPos)
        itemSize(bb.size, style.framePadding.y)
        window.dc.cursorMaxPos = backupCursorMaxPos

        if (!itemAdd(bb, id)) {
            if (wantClipRect) popClipRect()
            window.dc.cursorPos = backupMainCursorPos
            return tabContentsVisible
        }

        // Click to Select a tab
        var buttonFlags = (if (isTabButton) ButtonFlag.PressedOnClickRelease else ButtonFlag.PressedOnClick) or ButtonFlag.AllowItemOverlap
        if (g.dragDropActive && !g.dragDropPayload.isDataType(IMGUI_PAYLOAD_TYPE_WINDOW)) buttonFlags = buttonFlags or ButtonFlag.PressedOnDragDropHold
        val (pressed, hovered_, held) = buttonBehavior(bb, id, buttonFlags)
        if (pressed && !isTabButton)
            nextSelectedTabId = id
        val hovered = hovered_ || g.hoveredId == id

        // Transfer active id window so the active id is not owned by the dock host (as StartMouseMovingWindow()
        // will only do it on the drag). This allows FocusWindow() to be more conservative in how it clears active id.
        if (held && dockedWindow != null && g.activeId == id && g.activeIdIsJustActivated)
            g.activeIdWindow = dockedWindow

        // Allow the close button to overlap unless we are dragging (in which case we don't want any overlapping tabs to be hovered)
        if (!held)
            setItemAllowOverlap()

        // Drag and drop a single floating window node moves it
        val node = dockedWindow?.dockNode
        val singleFloatingWindowNode = node?.isFloatingNode == true && node.windows.size == 1
        if (held && singleFloatingWindowNode && isMouseDragging(MouseButton.Left, 0f))
        // Move
            dockedWindow!!.startMouseMoving()
        else if(held && !tabAppearing && isMouseDragging(MouseButton.Left)) {
            // Drag and drop: re-order tabs
            var dragDistanceFromEdgeX = 0f
            if (!g.dragDropActive && (flags has TabBarFlag.Reorderable || dockedWindow != null)) {
                // While moving a tab it will jump on the other side of the mouse, so we also test for MouseDelta.x
                if (io.mouseDelta.x < 0f && io.mousePos.x < bb.min.x) {
                    dragDistanceFromEdgeX = bb.min.x - io.mousePos.x
                    if (flags has TabBarFlag.Reorderable)
                        queueReorder(tab, -1)
                } else if (io.mouseDelta.x > 0f && io.mousePos.x > bb.max.x) {
                    dragDistanceFromEdgeX = io.mousePos.x - bb.max.x
                    if (flags has TabBarFlag.Reorderable)
                        queueReorder(
                tab,
                +1
            )
                }
            }

            // Extract a Dockable window out of it's tab bar
            if (dockedWindow != null && dockedWindow.flags hasnt WindowFlag.NoMove) {
                // We use a variable threshold to distinguish dragging tabs within a tab bar and extracting them out of the tab bar
                var undockingTab = g.dragDropActive && g.dragDropPayload.sourceId == id

                if (!undockingTab) { //&& (!g.IO.ConfigDockingWithShift || g.IO.KeyShift)
                    val thresholdBase = g.fontSize
                    //float threshold_base = g.IO.ConfigDockingWithShift ? g.FontSize * 0.5f : g.FontSize;
                    val thresholdX = thresholdBase * 2.2f
                    val thresholdY = thresholdBase * 1.5f + clamp((abs(io.mouseDragMaxDistanceAbs[0].x) - thresholdBase * 2f) * 0.2f, 0f, thresholdBase * 4f)
                    //GetForegroundDrawList()->AddRect(ImVec2(bb.Min.x - threshold_x, bb.Min.y - threshold_y), ImVec2(bb.Max.x + threshold_x, bb.Max.y + threshold_y), IM_COL32_WHITE); // [DEBUG]

                    val distanceFromEdgeY = max(bb.min.y - io.mousePos.y, io.mousePos.y - bb.max.y)
                    if (distanceFromEdgeY >= thresholdY)
                        undockingTab = true
                    else if (dragDistanceFromEdgeX > thresholdX)
                        if ((reorderRequestDir < 0 && tab.order == 0) || (reorderRequestDir > 0 && tab.order == tabs.lastIndex))
                            undockingTab = true
                }

                if (undockingTab) {
                    // Undock
                    dockContextQueueUndockWindow(g, dockedWindow)
                    g.movingWindow = dockedWindow
                    setActiveID(dockedWindow.moveId, dockedWindow)
                    g.activeIdClickOffset.minusAssign(g.movingWindow!!.pos - bb.min)
                    g.activeIdNoClearOnFocusLoss = true
                }
            }
        }

        //        if (false)
        //            if (hovered && g.hoveredIdNotActiveTimer > 0.5f && bb.width < tab.widthContents)        {
        //                // Enlarge tab display when hovering
        //                bb.max.x = bb.min.x + lerp (bb.width, tab.widthContents, saturate((g.hoveredIdNotActiveTimer-0.4f) * 6f)).i.f
        //                displayDrawList = GetOverlayDrawList(window)
        //                TabItemBackground(display_draw_list, bb, flags, GetColorU32(ImGuiCol_TitleBgActive))
        //            }

        // Render tab shape
        val displayDrawList = window.drawList
        val tabCol = when {
            held || hovered -> Col.TabHovered
            else -> when {
                tabContentsVisible -> when {
                    tabBarFocused -> Col.TabActive
                    else -> Col.TabUnfocusedActive
                }
                else -> when {
                    tabBarFocused -> Col.Tab
                    else -> Col.TabUnfocused
                }
            }
        }
        tabItemBackground(displayDrawList, bb, flags, tabCol.u32)
        renderNavHighlight(bb, id)

        // Select with right mouse button. This is so the common idiom for context menu automatically highlight the current widget.
        val hoveredUnblocked = isItemHovered(HoveredFlag.AllowWhenBlockedByPopup)
        if (hoveredUnblocked && (isMouseClicked(MouseButton.Right) || isMouseReleased(MouseButton.Right))) if (!isTabButton) nextSelectedTabId =
            id

        if (this.flags has TabBarFlag.NoCloseWithMiddleMouseButton) flags =
            flags or TabItemFlag.NoCloseWithMiddleMouseButton

        // Render tab label, process close button
        val closeButtonId = if (pOpen?.get() == true) getIDWithSeed("#CLOSE", -1, id) else 0
        val justClosed = tabItemLabelAndCloseButton(
            displayDrawList,
            bb,
            flags,
            framePadding,
            label.toByteArray(),
            id,
            closeButtonId,
            tabContentsVisible
        )
        if (justClosed && pOpen != null) {
            pOpen.set(false)
            closeTab(tab)
        }

        // Restore main window position so user can draw there
        if (wantClipRect) popClipRect()
        window.dc.cursorPos = backupMainCursorPos

        // Tooltip (FIXME: Won't work over the close button because ItemOverlap systems messes up with HoveredIdTimer)
        // We test IsItemHovered() to discard e.g. when another item is active or drag and drop over the tab bar (which g.HoveredId ignores)
        if (g.hoveredId == id && !held && g.hoveredIdNotActiveTimer > 0.5f && isItemHovered()) if (this.flags hasnt TabBarFlag.NoTooltip && tab.flags hasnt TabItemFlag.NoTooltip) setTooltip(
            label.substring(0, findRenderedTextEnd(label))
        )

        assert(!isTabButton || !(selectedTabId == tab.id && isTabButton)) { "TabItemButton should not be selected" }
        return if (isTabButton) pressed else tabContentsVisible
    }

    /** This is called only once a frame before by the first call to ItemTab()
     *  The reason we're not calling it in BeginTabBar() is to leave a chance to the user to call the SetTabItemClosed() functions.
     *  ~ TabBarLayout */
    fun layout() {

        wantLayout = false

        // Garbage collect by compacting list
        // Detect if we need to sort out tab list (e.g. in rare case where a tab changed section)
        var tabDstN = 0
        var needSortBySection = false
        val sections = Array(3) { TabBarSection() } // Layout sections: Leading, Central, Trailing
        for (tabSrcN in tabs.indices) {
            val tab = tabs[tabSrcN]
            if (tab.lastFrameVisible < prevFrameVisible || tab.wantClose) {
                // Remove tab
                if (visibleTabId == tab.id) visibleTabId = 0
                if (selectedTabId == tab.id) selectedTabId = 0
                if (nextSelectedTabId == tab.id) nextSelectedTabId = 0
                continue
            }
            if (tabDstN != tabSrcN)
                tabs[tabDstN] = tabs[tabSrcN]

            // We will need sorting if tabs have changed section (e.g. moved from one of Leading/Central/Trailing to another)
            val currTabSectionN =
                if (tab.flags has TabItemFlag.Leading) 0 else if (tab.flags has TabItemFlag.Trailing) 2 else 1
            if (tabDstN > 0) {
                val prevTab = tabs[tabDstN - 1]
                val prevTabSectionN =
                    if (prevTab.flags has TabItemFlag.Leading) 0 else if (prevTab.flags has TabItemFlag.Trailing) 2 else 1
                if (currTabSectionN == 0 && prevTabSectionN != 0)
                    needSortBySection = true
                if (prevTabSectionN == 2 && currTabSectionN != 2)
                    needSortBySection = true
            }

            sections[currTabSectionN].tabCount++

            tabDstN++
        }
        if (tabs.size != tabDstN)
            for (i in tabDstN until tabs.size)
                tabs.pop()

        if (needSortBySection)
            tabs.sortWith(tabItemComparerBySection)

        // Calculate spacing between sections
        sections[0].spacing =
            if (sections[0].tabCount > 0 && sections[1].tabCount + sections[2].tabCount > 0) g.style.itemInnerSpacing.x else 0f
        sections[1].spacing =
            if (sections[1].tabCount > 0 && sections[2].tabCount > 0) g.style.itemInnerSpacing.x else 0f

        // Setup next selected tab
        var scrollTrackSelectedTabID: ID = 0
        if (nextSelectedTabId != 0) {
            selectedTabId = nextSelectedTabId
            nextSelectedTabId = 0
            scrollTrackSelectedTabID = selectedTabId
        }

        // Process order change request (we could probably process it when requested but it's just saner to do it in a single spot).
        if (reorderRequestTabId != 0) {
            if (processReorder() && reorderRequestTabId == selectedTabId)
                scrollTrackSelectedTabID = reorderRequestTabId
            reorderRequestTabId = 0
        }

        // Tab List Popup
        val tabListPopupButton = flags has TabBarFlag.TabListPopupButton
        if (tabListPopupButton) tabListPopupButton()?.let { tabToSelect -> // NB: Will alter BarRect.Min.x!
            selectedTabId = tabToSelect.id
            scrollTrackSelectedTabID = tabToSelect.id
        }

        // Leading/Trailing tabs will be shrink only if central one aren't visible anymore, so layout the shrink data as: leading, trailing, central
        // (whereas our tabs are stored as: leading, central, trailing)
        val shrinkBufferIndexes = intArrayOf(0, sections[0].tabCount + sections[2].tabCount, sections[0].tabCount)
        g.shrinkWidthBuffer.clear() // [JVM] it will automatically resized in the following for loop
        for (i in tabs.indices)
            g.shrinkWidthBuffer += ShrinkWidthItem()

        // Compute ideal tabs widths + store them into shrink buffer
        var mostRecentlySelectedTab: TabItem? = null
        var currSectionN = -1
        var foundSelectedTabID = false
        for (tabN in tabs.indices) {
            val tab = tabs[tabN]
            assert(tab.lastFrameVisible >= prevFrameVisible)

            if ((mostRecentlySelectedTab == null || mostRecentlySelectedTab.lastFrameSelected < tab.lastFrameSelected) && tab.flags hasnt TabItemFlag._Button)
                mostRecentlySelectedTab = tab
            if (tab.id == selectedTabId) foundSelectedTabID = true

            if (scrollTrackSelectedTabID == 0 && g.navJustMovedToId == tab.id)
                scrollTrackSelectedTabID = tab.id

            // Refresh tab width immediately, otherwise changes of style e.g. style.FramePadding.x would noticeably lag in the tab bar.
            // Additionally, when using TabBarAddTab() to manipulate tab bar order we occasionally insert new tabs that don't have a width yet,
            // and we cannot wait for the next BeginTabItem() call. We cannot compute this width within TabBarAddTab() because font size depends on the active window.
            val hasCloseButton = tab.window?.hasCloseButton ?: tab.flags hasnt TabItemFlag._NoCloseButton
            tab.contentWidth = tabItemCalcSize(tab.name, hasCloseButton).x

            val sectionN =
                if (tab.flags has TabItemFlag.Leading) 0 else if (tab.flags has TabItemFlag.Trailing) 2 else 1
            val section = sections[sectionN]
            section.width += tab.contentWidth + if (sectionN == currSectionN) g.style.itemInnerSpacing.x else 0f
            currSectionN = sectionN

            // Store data so we can build an array sorted by width if we need to shrink tabs down
            g.shrinkWidthBuffer[shrinkBufferIndexes[sectionN]++].apply {
                index = tabN
                width = tab.contentWidth
            }

            assert(tab.contentWidth > 0f)
            tab.width = tab.contentWidth
        }

        // Compute total ideal width (used for e.g. auto-resizing a window)
        widthAllTabsIdeal = 0f
        for (section in sections)
            widthAllTabsIdeal += section.width + section.spacing

        // Horizontal scrolling buttons
        // (note that TabBarScrollButtons() will alter BarRect.Max.x)
        if ((widthAllTabsIdeal > barRect.width && tabs.size > 1) && flags hasnt TabBarFlag.NoTabListScrollingButtons && flags has TabBarFlag.FittingPolicyScroll)
            scrollingButtons()?.let { scrollTrackSelectedTab ->
                scrollTrackSelectedTabID = scrollTrackSelectedTab.id
                if (scrollTrackSelectedTab.flags hasnt TabItemFlag._Button)
                    selectedTabId = scrollTrackSelectedTabID
            }

        // Shrink widths if full tabs don't fit in their allocated space
        val section0W = sections[0].width + sections[0].spacing
        val section1W = sections[1].width + sections[1].spacing
        val section2W = sections[2].width + sections[2].spacing
        val centralSectionIsVisible = section0W + section2W < barRect.width
        val widthExcess = when {
            centralSectionIsVisible -> (section1W - (barRect.width - section0W - section2W)) max 0f // Excess used to shrink central section
            else -> section0W + section2W - barRect.width // Excess used to shrink leading/trailing section
        }

        // With ImGuiTabBarFlags_FittingPolicyScroll policy, we will only shrink leading/trailing if the central section is not visible anymore
        if (widthExcess > 0f && (flags has TabBarFlag.FittingPolicyResizeDown || !centralSectionIsVisible)) {
            val shrinkDataCount =
                if (centralSectionIsVisible) sections[1].tabCount else sections[0].tabCount + sections[2].tabCount
            val shrinkDataOffset = if (centralSectionIsVisible) sections[0].tabCount + sections[2].tabCount else 0
            shrinkWidths(g.shrinkWidthBuffer, shrinkDataOffset, shrinkDataCount, widthExcess)

            // Apply shrunk values into tabs and sections
            for (tabN in shrinkDataOffset until shrinkDataOffset + shrinkDataCount) {
                val tab = tabs[g.shrinkWidthBuffer[tabN].index]
                val shrinkedWidth = floor(g.shrinkWidthBuffer[tabN].width)
                if (shrinkedWidth < 0f)
                    continue

                val sectionN =
                    if (tab.flags has TabItemFlag.Leading) 0 else if (tab.flags has TabItemFlag.Trailing) 2 else 1
                sections[sectionN].width -= tab.width - shrinkedWidth
                tab.width = shrinkedWidth
            }
        }

        // Layout all active tabs
        var sectionTabIndex = 0
        var tabOffset = 0f
        widthAllTabs = 0f
        for (sectionN in 0..2) {
            val section = sections[sectionN]
            if (sectionN == 2)
                tabOffset = min(max(0f, barRect.width - section.width), tabOffset)

            for (tabN in 0 until section.tabCount) {
                val tab = tabs[sectionTabIndex + tabN]
                tab.offset = tabOffset
                tabOffset += tab.width + if (tabN < section.tabCount - 1) g.style.itemInnerSpacing.x else 0f
            }
            widthAllTabs += (section.width + section.spacing) max 0f
            tabOffset += section.spacing
            sectionTabIndex += section.tabCount
        }

        // If we have lost the selected tab, select the next most recently active one
        if (!foundSelectedTabID) selectedTabId = 0
        if (selectedTabId == 0 && nextSelectedTabId == 0)
            mostRecentlySelectedTab?.let {
                selectedTabId = it.id
                scrollTrackSelectedTabID = selectedTabId
            }

        // Lock in visible tab
        visibleTabId = selectedTabId
        visibleTabWasSubmitted = false

        // CTRL+TAB can override visible tab temporarily
        if (g.navWindowingTarget?.dockNode?.tabBar === this) {
            visibleTabId = g.navWindowingTarget!!.id
            scrollTrackSelectedTabID = g.navWindowingTarget!!.id
        }

        // Update scrolling
        if (scrollTrackSelectedTabID != 0)
            findTabByID(scrollTrackSelectedTabID)?.let { scrollToTab(it, sections) }
        scrollingAnim = scrollClamp(scrollingAnim)
        scrollingTarget = scrollClamp(scrollingTarget)
        if (scrollingAnim != scrollingTarget) { // Scrolling speed adjust itself so we can always reach our target in 1/3 seconds.
            // Teleport if we are aiming far off the visible line
            scrollingSpeed = scrollingSpeed max (70f * g.fontSize)
            scrollingSpeed = scrollingSpeed max (abs(scrollingTarget - scrollingAnim) / 0.3f)
            val teleport = prevFrameVisible + 1 < g.frameCount || scrollingTargetDistToVisibility > 10f * g.fontSize
            scrollingAnim = if (teleport) scrollingTarget else linearSweep(
                scrollingAnim,
                scrollingTarget,
                io.deltaTime * scrollingSpeed
            )
        } else scrollingSpeed = 0f

        scrollingRectMinX = barRect.min.x + sections[0].width + sections[0].spacing
        scrollingRectMaxX = barRect.max.x - sections[2].width - sections[1].spacing

        // Clear name buffers
        if (flags hasnt TabBarFlag._DockNode) tabsNames.clear()

        // Actual layout in host window (we don't do it in BeginTabBar() so as not to waste an extra frame)
        val window = g.currentWindow!!
        window.dc.cursorPos put barRect.min
        itemSize(Vec2(widthAllTabsIdeal, barRect.height), framePadding.y)
    }

    /** Dockables uses Name/ID in the global namespace. Non-dockable items use the ID stack.
     *  ~ TabBarCalcTabID   */
    infix fun calcTabID(label: String): Int {
        return when {
            flags has TabBarFlag._DockNode -> {
                val id = hash(label)
                keepAliveID(id)
                id
            }
            else -> g.currentWindow!!.getID(label)
        }
    }

    /** ~TabBarScrollClamp */
    fun scrollClamp(scrolling: Float): Float = (scrolling min (widthAllTabs - barRect.width)) max 0f

    /** ~TabBarScrollToTab */
    fun scrollToTab(tab: TabItem, sections: Array<TabBarSection>) {
        if (tab.flags has (TabItemFlag.Leading or TabItemFlag.Trailing)) return

        val margin =
            g.fontSize * 1f // When to scroll to make Tab N+1 visible always make a bit of N visible to suggest more scrolling area (since we don't have a scrollbar)
        val order = tab.order

        // Scrolling happens only in the central section (leading/trailing sections are not scrolling)
        // FIXME: This is all confusing.
        val scrollableWidth = barRect.width - sections[0].width - sections[2].width - sections[1].spacing

        // We make all tabs positions all relative Sections[0].Width to make code simpler
        val tabX1 = tab.offset - sections[0].width + if (order > sections[0].tabCount - 1) -margin else 0f
        val tabX2 =
            tab.offset - sections[0].width + tab.width + if (order + 1 < tabs.size - sections[2].tabCount) margin else 1f
        scrollingTargetDistToVisibility = 0f
        if (scrollingTarget > tabX1 || (tabX2 - tabX1 >= scrollableWidth)) {
            // Scroll to the left
            scrollingTargetDistToVisibility = (scrollingAnim - tabX2) max 0f
            scrollingTarget = tabX1
        } else if (scrollingTarget < tabX2 - scrollableWidth) {
            // Scroll to the right
            scrollingTargetDistToVisibility = ((tabX1 - scrollableWidth) - scrollingAnim) max 0f
            scrollingTarget = tabX2 - scrollableWidth
        }
    }

    /** ~ TabBarScrollingButtons */
    fun scrollingButtons(): TabItem? {

        val window = g.currentWindow!!

        val arrowButtonSize = Vec2(g.fontSize - 2f, g.fontSize + g.style.framePadding.y * 2f)
        val scrollingButtonsWidth = arrowButtonSize.x * 2f

        val backupCursorPos = Vec2(window.dc.cursorPos)
        //window->DrawList->AddRect(ImVec2(tab_bar->BarRect.Max.x - scrolling_buttons_width, tab_bar->BarRect.Min.y), ImVec2(tab_bar->BarRect.Max.x, tab_bar->BarRect.Max.y), IM_COL32(255,0,0,255));

        var selectDir = 0
        val arrowCol = Vec4(style.colors[Col.Text])
        arrowCol.w *= 0.5f

        pushStyleColor(Col.Text, arrowCol)
        pushStyleColor(Col.Button, Vec4(0f))
        val backupRepeatDelay = io.keyRepeatDelay
        val backupRepeatRate = io.keyRepeatRate
        io.keyRepeatDelay = 0.25f
        io.keyRepeatRate = 0.2f
        val x = barRect.min.x max (barRect.max.x - scrollingButtonsWidth)
        window.dc.cursorPos.put(x, barRect.min.y)
        if (arrowButtonEx("##<", Dir.Left, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat)) selectDir =
            -1
        window.dc.cursorPos.put(x + arrowButtonSize.x, barRect.min.y)
        if (arrowButtonEx(
                "##>",
                Dir.Right,
                arrowButtonSize,
                ButtonFlag.PressedOnClick or ButtonFlag.Repeat
            )
        ) selectDir = +1
        popStyleColor(2)
        io.keyRepeatRate = backupRepeatRate
        io.keyRepeatDelay = backupRepeatDelay

        var tabToScrollTo: TabItem? = null

        if (selectDir != 0) findTabByID(selectedTabId)?.let { tabItem ->
            var selectedOrder = tabItem.order
            var targetOrder = selectedOrder + selectDir

            // Skip tab item buttons until another tab item is found or end is reached
            while (tabToScrollTo == null) { // If we are at the end of the list, still scroll to make our tab visible
                tabToScrollTo = tabs[if (targetOrder in tabs.indices) targetOrder else selectedOrder]

                // Cross through buttons
                // (even if first/last item is a button, return it so we can update the scroll)
                if (tabToScrollTo!!.flags has TabItemFlag._Button) {
                    targetOrder += selectDir
                    selectedOrder += selectDir
                    tabToScrollTo = if (targetOrder <= 0 || targetOrder >= tabs.size) tabToScrollTo else null
                }
            }
        }
        window.dc.cursorPos put backupCursorPos
        barRect.max.x -= scrollingButtonsWidth + 1f

        return tabToScrollTo
    }

    /** ~TabBarTabListPopupButton */
    fun tabListPopupButton(): TabItem? {

        val window = g.currentWindow!!

        // We use g.Style.FramePadding.y to match the square ArrowButton size
        val tabListPopupButtonWidth = g.fontSize + style.framePadding.y * 2f
        val backupCursorPos = Vec2(window.dc.cursorPos)
        window.dc.cursorPos.put(barRect.min.x - style.framePadding.y, barRect.min.y)
        barRect.min.x += tabListPopupButtonWidth

        val arrowCol = Vec4(style.colors[Col.Text])
        arrowCol.w *= 0.5f
        pushStyleColor(Col.Text, arrowCol)
        pushStyleColor(Col.Button, Vec4())
        val open = beginCombo("##v", null, ComboFlag.NoPreview or ComboFlag.HeightLargest)
        popStyleColor(2)

        var tabToSelect: TabItem? = null
        if (open) {
            for (tab in tabs) {
                if (tab.flags has TabItemFlag._Button) continue

                if (selectable(tab.name, selectedTabId == id)) tabToSelect = tab
            }
            endCombo()
        }

        window.dc.cursorPos = backupCursorPos
        return tabToSelect
    }


    companion object {
        fun calcMaxTabWidth() = g.fontSize * 20f

        val tabItemComparerBySection = Comparator<TabItem> { a, b ->
            val aSection = if (a.flags has TabItemFlag.Leading) 0 else if (a.flags has TabItemFlag.Trailing) 2 else 1
            val bSection = if (b.flags has TabItemFlag.Leading) 0 else if (b.flags has TabItemFlag.Trailing) 2 else 1
            if (aSection != bSection) aSection - bSection
            else a.indexDuringLayout - b.indexDuringLayout
        }
    }
}
