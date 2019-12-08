package imgui.internal.classes

import glm_.max
import glm_.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.arrowButtonEx
import imgui.ImGui.beginCombo
import imgui.ImGui.buttonBehavior
import imgui.ImGui.endCombo
import imgui.ImGui.findRenderedTextEnd
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
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setTooltip
import imgui.ImGui.shrinkWidths
import imgui.ImGui.style
import imgui.ImGui.tabItemBackground
import imgui.ImGui.tabItemCalcSize
import imgui.ImGui.tabItemLabelAndCloseButton
import imgui.api.g
import imgui.internal.*
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

/** Storage for a tab bar (sizeof() 92~96 bytes) */
class TabBar {
    val tabs = ArrayList<TabItem>()
    /** Zero for tab-bars used by docking */
    var id: ID = 0
    /** Selected tab */
    var selectedTabId: ID = 0
    var nextSelectedTabId: ID = 0
    /** Can occasionally be != SelectedTabId (e.g. when previewing contents for CTRL+TAB preview) */
    var visibleTabId: ID = 0
    var currFrameVisible = -1
    var prevFrameVisible = -1
    var barRect = Rect()
    /** Record the height of contents submitted below the tab bar */
    var lastTabContentHeight = 0f
    /** Distance from BarRect.Min.x, locked during layout */
    var offsetMax = 0f
    /** Ideal offset if all tabs were visible and not clipped */
    var offsetMaxIdeal = 0f
    /** Distance from BarRect.Min.x, incremented with each BeginTabItem() call, not used if ImGuiTabBarFlags_Reorderable if set. */
    var offsetNextTab = 0f
    var scrollingAnim = 0f
    var scrollingTarget = 0f
    var scrollingTargetDistToVisibility = 0f
    var scrollingSpeed = 0f
    var flags: TabBarFlags = TabBarFlag.None.i
    var reorderRequestTabId: ID = 0
    var reorderRequestDir = 0
    var wantLayout = false
    var visibleTabWasSubmitted = false
    /** For BeginTabItem()/EndTabItem() */
    var lastTabItemIdx = -1
    /** style.FramePadding locked at the time of BeginTabBar() */
    var framePadding = Vec2()
    /** For non-docking tab bar we re-append names in a contiguous buffer. */
    val tabsNames = ArrayList<String>()

    val TabItem.order get() = tabs.indexOf(this)

    val TabItem.name: String
        get() {
            assert(nameOffset in tabsNames.indices)
            return tabsNames[nameOffset]
        }

    // Tab Bars

    /** ~ beginTabBarEx */
    fun beginEx(bb: Rect, flags__: TabBarFlags): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        var flags_ = flags__
        if (flags_ hasnt TabBarFlag._DockNode)
            window.idStack += id

        // Add to stack
        g.currentTabBarStack += tabBarRef
        g.currentTabBar = this
        if (currFrameVisible == g.frameCount) {
            //printf("[%05d] BeginTabBarEx already called this frame\n", g.FrameCount);
            assert(false)
            return true
        }

        // When toggling back from ordered to manually-reorderable, shuffle tabs to enforce the last visible order.
        // Otherwise, the most recently inserted tabs would move at the end of visible list which can be a little too confusing or magic for the user.
        if (flags_ has TabBarFlag.Reorderable && flags_ hasnt TabBarFlag.Reorderable && tabs.isNotEmpty() && prevFrameVisible != -1)
            tabs.sortBy(TabItem::offset)

        // Flags
        if (flags_ hasnt TabBarFlag.FittingPolicyMask_)
            flags_ = flags_ or TabBarFlag.FittingPolicyDefault_

        flags = flags_
        barRect = bb
        wantLayout = true // Layout will be done on the first call to ItemTab()
        prevFrameVisible = currFrameVisible
        currFrameVisible = g.frameCount

        // Layout
        itemSize(Vec2(offsetMaxIdeal, barRect.height), framePadding.y)
        window.dc.cursorPos.x = barRect.min.x

        // Draw separator
        val col = if (flags has TabBarFlag._IsFocused) Col.TabActive else Col.TabUnfocusedActive
        val y = barRect.max.y - 1f
        run {
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

    fun findTabByID(tabId: ID): TabItem? = when (tabId) {
        0 -> null
        else -> tabs.find { it.id == tabId }
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
    fun closeTab(tab: TabItem) {
        if (visibleTabId == tab.id && tab.flags hasnt TabItemFlag.UnsavedDocument) {
            // This will remove a frame of lag for selecting another tab on closure.
            // However we don't run it in the case where the 'Unsaved' flag is set, so user gets a chance to fully undo the closure
            tab.lastFrameVisible = -1
            nextSelectedTabId = 0
            selectedTabId = 0
        } else if (visibleTabId != tab.id && tab.flags has TabItemFlag.UnsavedDocument)
        // Actually select before expecting closure
            nextSelectedTabId = tab.id
    }

    /** ~ tabBarQueueChangeTabOrder */
    fun queueChangeTabOrder(tab: TabItem, dir: Int) {
        assert(dir == -1 || dir == +1)
        assert(reorderRequestTabId == 0)
        reorderRequestTabId = tab.id
        reorderRequestDir = dir
    }

    fun tabItemEx(label: String, pOpen: KMutableProperty0<Boolean>?, flags_: TabItemFlags): Boolean {

        var flags = flags_
        // Layout whole tab bar if not already done
        if (wantLayout)
            layout()

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val id = calcTabID(label)

        // If the user called us with *p_open == false, we early out and don't render. We make a dummy call to ItemAdd() so that attempts to use a contextual popup menu with an implicit ID won't use an older ID.
        if (pOpen?.get() == false) {
            pushItemFlag(ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus, true)
            itemAdd(Rect(), id)
            popItemFlag()
            return false
        }

        // Calculate tab contents size
        val size = tabItemCalcSize(label, pOpen != null)

        // Acquire tab data
        var tabIsNew = false
        val tab = findTabByID(id) ?: TabItem().also {
            it.id = id
            it.width = size.x
            tabs += it
            tabIsNew = true
        }
        lastTabItemIdx = tabs.indexOf(tab)
        tab.widthContents = size.x

        if (pOpen == null)
            flags = flags or TabItemFlag._NoCloseButton

        val tabBarAppearing = prevFrameVisible + 1 < g.frameCount
        val tabBarFocused = this.flags has TabBarFlag._IsFocused
        val tabAppearing = tab.lastFrameVisible + 1 < g.frameCount
        tab.lastFrameVisible = g.frameCount
        tab.flags = flags

        // Append name with zero-terminator
        tab.nameOffset = tabsNames.size
        tabsNames += label

        // If we are not reorderable, always reset offset based on submission order.
        // (We already handled layout and sizing using the previous known order, but sizing is not affected by order!)
        if (!tabAppearing && this.flags hasnt TabBarFlag.Reorderable) {
            tab.offset = offsetNextTab
            offsetNextTab += tab.width + style.itemInnerSpacing.x
        }

        // Update selected tab
        if (tabAppearing && this.flags has TabBarFlag.AutoSelectNewTabs && nextSelectedTabId == 0)
            if (!tabBarAppearing || selectedTabId == 0)
                nextSelectedTabId = id  // New tabs gets activated
        if (flags has TabItemFlag.SetSelected && selectedTabId != id) // SetSelected can only be passed on explicit tab bar
            nextSelectedTabId = id

        // Lock visibility
        var tabContentsVisible = visibleTabId == id
        if (tabContentsVisible)
            visibleTabWasSubmitted = true

        // On the very first frame of a tab bar we let first tab contents be visible to minimize appearing glitches
        if (!tabContentsVisible && selectedTabId == 0 && tabBarAppearing)
            if (tabs.size == 1 && this.flags hasnt TabBarFlag.AutoSelectNewTabs)
                tabContentsVisible = true

        if (tabAppearing && !tabBarAppearing || tabIsNew) {
            pushItemFlag(ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus, true)
            itemAdd(Rect(), id)
            popItemFlag()
            return tabContentsVisible
        }

        if (selectedTabId == id)
            tab.lastFrameSelected = g.frameCount

        // Backup current layout position
        val backupMainCursorPos = Vec2(window.dc.cursorPos)

        // Layout
        size.x = tab.width
        window.dc.cursorPos = barRect.min + Vec2(floor(tab.offset) - scrollingAnim, 0f)
        val pos = Vec2(window.dc.cursorPos)
        val bb = Rect(pos, pos + size)

        // We don't have CPU clipping primitives to clip the CloseButton (until it becomes a texture), so need to add an extra draw call (temporary in the case of vertical animation)
        val wantClipRect = bb.min.x < barRect.min.x || bb.max.x > barRect.max.x
        if (wantClipRect)
            pushClipRect(Vec2(bb.min.x max barRect.min.x, bb.min.y - 1), Vec2(barRect.max.x, bb.max.y), true)

        val backupCursorMaxPos = Vec2(window.dc.cursorMaxPos)
        itemSize(bb.size, style.framePadding.y)
        window.dc.cursorMaxPos = backupCursorMaxPos

        if (!itemAdd(bb, id)) {
            if (wantClipRect)
                popClipRect()
            window.dc.cursorPos = backupMainCursorPos
            return tabContentsVisible
        }

        // Click to Select a tab
        var buttonFlags = ButtonFlag.PressedOnClick or ButtonFlag.AllowItemOverlap
        if (g.dragDropActive)
            buttonFlags = buttonFlags or ButtonFlag.PressedOnDragDropHold
        val (pressed, hovered_, held) = buttonBehavior(bb, id, buttonFlags)
        if (pressed)
            nextSelectedTabId = id
        val hovered = hovered_ || g.hoveredId == id

        // Allow the close button to overlap unless we are dragging (in which case we don't want any overlapping tabs to be hovered)
        if (!held)
            setItemAllowOverlap()

        // Drag and drop: re-order tabs
        if (held && !tabAppearing && isMouseDragging(0))
            if (!g.dragDropActive && this.flags has TabBarFlag.Reorderable)
            // While moving a tab it will jump on the other side of the mouse, so we also test for MouseDelta.x
                if (io.mouseDelta.x < 0f && io.mousePos.x < bb.min.x) {
                    if (this.flags has TabBarFlag.Reorderable)
                        queueChangeTabOrder(tab, -1)
                } else if (io.mouseDelta.x > 0f && io.mousePos.x > bb.max.x)
                    if (this.flags has TabBarFlag.Reorderable)
                        queueChangeTabOrder(tab, +1)

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
        if (hoveredUnblocked && (isMouseClicked(1) || isMouseReleased(1)))
            nextSelectedTabId = id

        if (this.flags has TabBarFlag.NoCloseWithMiddleMouseButton)
            flags = flags or TabItemFlag.NoCloseWithMiddleMouseButton

        // Render tab label, process close button
        val closeButtonId = if (pOpen?.get() == true) window.getId(id + 1) else 0
        val justClosed = tabItemLabelAndCloseButton(displayDrawList, bb, flags, framePadding, label, id, closeButtonId)
        if (justClosed && pOpen != null) {
            pOpen.set(false)
            closeTab(tab)
        }

        // Restore main window position so user can draw there
        if (wantClipRect)
            popClipRect()
        window.dc.cursorPos = backupMainCursorPos

        // Tooltip (FIXME: Won't work over the close button because ItemOverlap systems messes up with HoveredIdTimer)
        // We test IsItemHovered() to discard e.g. when another item is active or drag and drop over the tab bar (which g.HoveredId ignores)
        if (g.hoveredId == id && !held && g.hoveredIdNotActiveTimer > 0.5f && isItemHovered())
            if (this.flags hasnt TabBarFlag.NoTooltip)
                setTooltip(label.substring(0, findRenderedTextEnd(label)))

        return tabContentsVisible
    }

    /** This is called only once a frame before by the first call to ItemTab()
     *  The reason we're not calling it in BeginTabBar() is to leave a chance to the user to call the SetTabItemClosed() functions.
     *  ~ TabBarLayout */
    fun layout() {

        wantLayout = false

        // Garbage collect
        var tabDstN = 0
        for (tabSrcN in tabs.indices) {
            val tab = tabs[tabSrcN]
            if (tab.lastFrameVisible < prevFrameVisible) {
                if (tab.id == selectedTabId)
                    selectedTabId = 0
                continue
            }
            if (tabDstN != tabSrcN)
                tabs[tabDstN] = tabs[tabSrcN]
            tabDstN++
        }
        if (tabs.size != tabDstN)
            for (i in tabDstN until tabs.size)
                tabs.remove(tabs.last())

        // Setup next selected tab
        var scrollTrackSelectedTabID: ID = 0
        if (nextSelectedTabId != 0) {
            selectedTabId = nextSelectedTabId
            nextSelectedTabId = 0
            scrollTrackSelectedTabID = selectedTabId
        }

        // Process order change request (we could probably process it when requested but it's just saner to do it in a single spot).
        if (reorderRequestTabId != 0) {
            findTabByID(reorderRequestTabId)?.let { tab1 ->
                //IM_ASSERT(tab_bar->Flags & ImGuiTabBarFlags_Reorderable); // <- this may happen when using debug tools
                val tab2_order = tab1.order + reorderRequestDir
                if (tab2_order in tabs.indices) {
                    val tab2 = tabs[tab2_order]
                    val itemTmp = tab1
                    tabs[tab1.order] = tab2 // *tab1 = *tab2
                    tabs[tab2_order] = itemTmp // *tab2 = itemTmp
                    if (tab2.id == selectedTabId)
                        scrollTrackSelectedTabID = tab2.id
                }
                if (flags has TabBarFlag._SaveSettings)
                    markIniSettingsDirty()
            }
            reorderRequestTabId = 0
        }

        // Tab List Popup
        val tabListPopupButton = flags has TabBarFlag.TabListPopupButton
        if (tabListPopupButton)
            tabListPopupButton()?.let { tabToSelect ->
                // NB: Will alter BarRect.Max.x!
                selectedTabId = tabToSelect.id
                scrollTrackSelectedTabID = tabToSelect.id
            }

        // Compute ideal widths
        g.shrinkWidthBuffer.clear() // it will automatically resized in the following for loop
        var widthTotalContents = 0f
        var mostRecentlySelectedTab: TabItem? = null
        var foundSelectedTabID = false
        for (tabN in tabs.indices) {
            val tab = tabs[tabN]
            assert(tab.lastFrameVisible >= prevFrameVisible)

            if (mostRecentlySelectedTab == null || mostRecentlySelectedTab.lastFrameSelected < tab.lastFrameSelected)
                mostRecentlySelectedTab = tab
            if (tab.id == selectedTabId)
                foundSelectedTabID = true

            // Refresh tab width immediately, otherwise changes of style e.g. style.FramePadding.x would noticeably lag in the tab bar.
            // Additionally, when using TabBarAddTab() to manipulate tab bar order we occasionally insert new tabs that don't have a width yet,
            // and we cannot wait for the next BeginTabItem() call. We cannot compute this width within TabBarAddTab() because font size depends on the active window.
            val hasCloseButton = tab.flags hasnt TabItemFlag._NoCloseButton
            tab.widthContents = tabItemCalcSize(tab.name, hasCloseButton).x

            widthTotalContents += (if (tabN > 0) style.itemInnerSpacing.x else 0f) + tab.widthContents

            // Store data so we can build an array sorted by width if we need to shrink tabs down
            g.shrinkWidthBuffer += ShrinkWidthItem(tabN, tab.widthContents)
        }

        // Compute width
        val initialOffsetX = 0f // g.Style.ItemInnerSpacing.x;
        val widthAvail = (barRect.width - initialOffsetX) max 0f
        val widthExcess = if (widthAvail < widthTotalContents) widthTotalContents - widthAvail else 0f
        if (widthExcess > 0f && flags has TabBarFlag.FittingPolicyResizeDown) {
            // If we don't have enough room, resize down the largest tabs first
            shrinkWidths(g.shrinkWidthBuffer, widthExcess)
            for (tabN in tabs.indices)
                tabs[g.shrinkWidthBuffer[tabN].index].width = floor(g.shrinkWidthBuffer[tabN].width)
        } else {
            val tabMaxWidth = calcMaxTabWidth()
            for (tab in tabs) {
                tab.width = tab.widthContents min tabMaxWidth
                assert(tab.width > 0f)
            }
        }

        // Layout all active tabs
        var offsetX = initialOffsetX
        var offsetXideal = offsetX
        offsetNextTab = offsetX // This is used by non-reorderable tab bar where the submission order is always honored.
        for (tab in tabs) {
            tab.offset = offsetX
            if (scrollTrackSelectedTabID == 0 && g.navJustMovedToId == tab.id)
                scrollTrackSelectedTabID = tab.id
            offsetX += tab.width + style.itemInnerSpacing.x
            offsetXideal += tab.widthContents + style.itemInnerSpacing.x
        }
        offsetMax = (offsetX - style.itemInnerSpacing.x) max 0f
        offsetMaxIdeal = (offsetXideal - style.itemInnerSpacing.x) max 0f

        // Horizontal scrolling buttons
        val scrollingButtons = offsetMax > barRect.width && tabs.size > 1 && flags hasnt TabBarFlag.NoTabListScrollingButtons && flags has TabBarFlag.FittingPolicyScroll
        if (scrollingButtons)
            scrollingButtons()?.let { tabToSelect ->
                // NB: Will alter BarRect.Max.x!
                selectedTabId = tabToSelect.id
                scrollTrackSelectedTabID = selectedTabId
            }

        // If we have lost the selected tab, select the next most recently active one
        if (!foundSelectedTabID)
            selectedTabId = 0
        if (selectedTabId == 0 && nextSelectedTabId == 0)
            mostRecentlySelectedTab?.let {
                selectedTabId = it.id
                scrollTrackSelectedTabID = selectedTabId
            }

        // Lock in visible tab
        visibleTabId = selectedTabId
        visibleTabWasSubmitted = false

        // Update scrolling
        if (scrollTrackSelectedTabID != 0)
            findTabByID(scrollTrackSelectedTabID)?.let(::scrollToTab)
        scrollingAnim = scrollClamp(scrollingAnim)
        scrollingTarget = scrollClamp(scrollingTarget)
        if (scrollingAnim != scrollingTarget) {
            // Scrolling speed adjust itself so we can always reach our target in 1/3 seconds.
            // Teleport if we are aiming far off the visible line
            scrollingSpeed = scrollingSpeed max (70f * g.fontSize)
            scrollingSpeed = scrollingSpeed max (abs(scrollingTarget - scrollingAnim) / 0.3f)
            val teleport = prevFrameVisible + 1 < g.frameCount || scrollingTargetDistToVisibility > 10f * g.fontSize
            scrollingAnim = if (teleport) scrollingTarget else linearSweep(scrollingAnim, scrollingTarget, io.deltaTime * scrollingSpeed)
        } else scrollingSpeed = 0f

        // Clear name buffers
        if (flags hasnt TabBarFlag._DockNode)
            tabsNames.clear()
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
            else -> g.currentWindow!!.getId(label)
        }
    }

    fun scrollClamp(scrolling_: Float): Float {
        val scrolling = scrolling_ min (offsetMax - barRect.width)
        return scrolling max 0f
    }

    fun scrollToTab(tab: TabItem) {

        val margin = g.fontSize * 1f // When to scroll to make Tab N+1 visible always make a bit of N visible to suggest more scrolling area (since we don't have a scrollbar)
        val order = tab.order
        val tabX1 = tab.offset + if (order > 0) -margin else 0f
        val tabX2 = tab.offset + tab.width + if (order + 1 < tabs.size) margin else 1f
        scrollingTargetDistToVisibility = 0f
        if (scrollingTarget > tabX1 || (tabX2 - tabX1 >= barRect.width)) {
            scrollingTargetDistToVisibility = (scrollingAnim - tabX2) max 0f
            scrollingTarget = tabX1
        } else if (scrollingTarget < tabX2 - barRect.width) {
            scrollingTargetDistToVisibility = ((tabX1 - barRect.width) - scrollingAnim) max 0f
            scrollingTarget = tabX2 - barRect.width
        }
    }

    /** ~ TabBarScrollingButtons */
    fun scrollingButtons(): TabItem? {

        val window = g.currentWindow!!

        val arrowButtonSize = Vec2(g.fontSize - 2f, g.fontSize + style.framePadding.y * 2f)
        val scrollingButtonsWidth = arrowButtonSize.x * 2f

        val backupCursorPos = Vec2(window.dc.cursorPos)
        //window->DrawList->AddRect(ImVec2(tab_bar->BarRect.Max.x - scrolling_buttons_width, tab_bar->BarRect.Min.y), ImVec2(tab_bar->BarRect.Max.x, tab_bar->BarRect.Max.y), IM_COL32(255,0,0,255));

        val availBarRect = Rect(barRect)
        val wantClipRect = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(scrollingButtonsWidth, 0f)) !in availBarRect
        if (wantClipRect)
            pushClipRect(barRect.min, barRect.max + Vec2(style.itemInnerSpacing.x, 0f), true)

        var tabToSelect: TabItem? = null

        var selectDir = 0
        val arrowCol = Vec4(style.colors[Col.Text])
        arrowCol.w *= 0.5f

        pushStyleColor(Col.Text, arrowCol)
        pushStyleColor(Col.Button, Vec4(0f))
        val backupRepeatDelay = io.keyRepeatDelay
        val backupRepeatRate = io.keyRepeatRate
        io.keyRepeatDelay = 0.25f
        io.keyRepeatRate = 0.20f
        window.dc.cursorPos.put(barRect.max.x - scrollingButtonsWidth, barRect.min.y)
        if (arrowButtonEx("##<", Dir.Left, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat))
            selectDir = -1
        window.dc.cursorPos.put(barRect.max.x - scrollingButtonsWidth + arrowButtonSize.x, barRect.min.y)
        if (arrowButtonEx("##>", Dir.Right, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat))
            selectDir = +1
        popStyleColor(2)
        io.keyRepeatRate = backupRepeatRate
        io.keyRepeatDelay = backupRepeatDelay

        if (wantClipRect)
            popClipRect()

        if (selectDir != 0)
            findTabByID(selectedTabId)?.let { tabItem ->
                val selectedOrder = tabItem.order
                val targetOrder = selectedOrder + selectDir
                tabToSelect = tabs[if (targetOrder in tabs.indices) targetOrder else selectedOrder] // If we are at the end of the list, still scroll to make our tab visible
            }
        window.dc.cursorPos put backupCursorPos
        barRect.max.x -= scrollingButtonsWidth + 1f

        return tabToSelect
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
        val open = beginCombo("##v", null, ComboFlag.NoPreview.i)
        popStyleColor(2)

        var tabToSelect: TabItem? = null
        if (open) {
            tabs.forEach { tab ->
                if (selectable(tab.name, selectedTabId == id))
                    tabToSelect = tab
            }
            endCombo()
        }

        window.dc.cursorPos = backupCursorPos
        return tabToSelect
    }


    companion object {
        fun calcMaxTabWidth() = g.fontSize * 20f
    }
}