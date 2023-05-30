package imgui.static

import glm_.max
import glm_.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.findTabByID
import imgui.ImGui.processReorder
import imgui.api.g
import imgui.internal.classes.*
import imgui.internal.floor
import imgui.internal.hashStr
import imgui.internal.linearSweep
import imgui.internal.sections.ButtonFlag
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** This is called only once a frame before by the first call to ItemTab()
 *  The reason we're not calling it in BeginTabBar() is to leave a chance to the user to call the SetTabItemClosed() functions.
 *  ~ TabBarLayout */
fun TabBar.layout() {

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
        val currTabSectionN = tabItemGetSectionIdx(tab)
        if (tabDstN > 0) {
            val prevTab = tabs[tabDstN - 1]
            val prevTabSectionN = tabItemGetSectionIdx(prevTab)
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
    sections[0].spacing = if (sections[0].tabCount > 0 && sections[1].tabCount + sections[2].tabCount > 0) g.style.itemInnerSpacing.x else 0f
    sections[1].spacing = if (sections[1].tabCount > 0 && sections[2].tabCount > 0) g.style.itemInnerSpacing.x else 0f

    // Setup next selected tab
    var scrollToTabID: ID = 0
    if (nextSelectedTabId != 0) {
        selectedTabId = nextSelectedTabId
        nextSelectedTabId = 0
        scrollToTabID = selectedTabId
    }

    // Process order change request (we could probably process it when requested but it's just saner to do it in a single spot).
    if (reorderRequestTabId != 0) {
        if (processReorder() && reorderRequestTabId == selectedTabId)
            scrollToTabID = reorderRequestTabId
        reorderRequestTabId = 0
    }

    // Tab List Popup
    val tabListPopupButton = flags has TabBarFlag.TabListPopupButton
    if (tabListPopupButton)
        tabListPopupButton()?.let { tabToSelect -> // NB: Will alter BarRect.Min.x!
            scrollToTabID = tabToSelect.id; selectedTabId = scrollToTabID
        }

    // Leading/Trailing tabs will be shrink only if central one aren't visible anymore, so layout the shrink data as: leading, trailing, central
    // (whereas our tabs are stored as: leading, central, trailing)
    val shrinkBufferIndexes = intArrayOf(0, sections[0].tabCount + sections[2].tabCount, sections[0].tabCount)
    // [JVM] each item will be set in the following for loop
    for (i in g.shrinkWidthBuffer.size until tabs.size) g.shrinkWidthBuffer += ShrinkWidthItem()
    for (i in tabs.size until g.shrinkWidthBuffer.size) g.shrinkWidthBuffer.removeLast()

    // Compute ideal tabs widths + store them into shrink buffer
    var mostRecentlySelectedTab: TabItem? = null
    var currSectionN = -1
    var foundSelectedTabID = false
    for (tabN in tabs.indices) {
        val tab = tabs[tabN]
        assert(tab.lastFrameVisible >= prevFrameVisible)

        if ((mostRecentlySelectedTab == null || mostRecentlySelectedTab.lastFrameSelected < tab.lastFrameSelected) && tab.flags hasnt TabItemFlag._Button)
            mostRecentlySelectedTab = tab
        if (tab.id == selectedTabId)
            foundSelectedTabID = true

        if (scrollToTabID == 0 && g.navJustMovedToId == tab.id)
            scrollToTabID = tab.id

        // Refresh tab width immediately, otherwise changes of style e.g. style.FramePadding.x would noticeably lag in the tab bar.
        // Additionally, when using TabBarAddTab() to manipulate tab bar order we occasionally insert new tabs that don't have a width yet,
        // and we cannot wait for the next BeginTabItem() call. We cannot compute this width within TabBarAddTab() because font size depends on the active window.
        val hasCloseButtonOrUnsavedMarker = tab.flags hasnt TabItemFlag._NoCloseButton || tab.flags has TabItemFlag.UnsavedDocument
        tab.contentWidth = if (tab.requestedWidth >= 0f) tab.requestedWidth else ImGui.tabItemCalcSize(tab.name, hasCloseButtonOrUnsavedMarker).x

        val sectionN = tabItemGetSectionIdx(tab)
        val section = sections[sectionN]
        section.width += tab.contentWidth + if (sectionN == currSectionN) g.style.itemInnerSpacing.x else 0f
        currSectionN = sectionN

        // Store data so we can build an array sorted by width if we need to shrink tabs down
        g.shrinkWidthBuffer[shrinkBufferIndexes[sectionN]++].apply {
            index = tabN
            width = tab.contentWidth; initialWidth = tab.contentWidth
        }

        tab.width = tab.contentWidth max 1f
    }

    // Compute total ideal width (used for e.g. auto-resizing a window)
    widthAllTabsIdeal = 0f
    for (section in sections)
        widthAllTabsIdeal += section.width + section.spacing

    // Horizontal scrolling buttons
    // (note that TabBarScrollButtons() will alter BarRect.Max.x)
    if ((widthAllTabsIdeal > barRect.width && tabs.size > 1) && flags hasnt TabBarFlag.NoTabListScrollingButtons && flags has TabBarFlag.FittingPolicyScroll)
        scrollingButtons()?.let { scrollAndSelectTab ->
            scrollToTabID = scrollAndSelectTab.id
            if (scrollAndSelectTab.flags hasnt TabItemFlag._Button)
                selectedTabId = scrollToTabID
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
    if (widthExcess >= 1f && (flags has TabBarFlag.FittingPolicyResizeDown || !centralSectionIsVisible)) {
        val shrinkDataCount = if (centralSectionIsVisible) sections[1].tabCount else sections[0].tabCount + sections[2].tabCount
        val shrinkDataOffset = if (centralSectionIsVisible) sections[0].tabCount + sections[2].tabCount else 0
        ImGui.shrinkWidths(g.shrinkWidthBuffer, shrinkDataOffset, shrinkDataCount, widthExcess)

        // Apply shrunk values into tabs and sections
        for (tabN in shrinkDataOffset until shrinkDataOffset + shrinkDataCount) {
            val tab = tabs[g.shrinkWidthBuffer[tabN].index]
            var shrinkedWidth = floor(g.shrinkWidthBuffer[tabN].width)
            if (shrinkedWidth < 0f)
                continue

            shrinkedWidth = 1f max shrinkedWidth
            val sectionN = tabItemGetSectionIdx(tab)
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
            tab.nameOffset = -1
            tabOffset += tab.width + if (tabN < section.tabCount - 1) g.style.itemInnerSpacing.x else 0f
        }
        widthAllTabs += (section.width + section.spacing) max 0f
        tabOffset += section.spacing
        sectionTabIndex += section.tabCount
    }

    // Clear name buffers
    tabsNames.clear()

    // If we have lost the selected tab, select the next most recently active one
    if (!foundSelectedTabID) selectedTabId = 0
    if (selectedTabId == 0 && nextSelectedTabId == 0)
        mostRecentlySelectedTab?.let {
            selectedTabId = it.id
            scrollToTabID = selectedTabId
        }

    // Lock in visible tab
    visibleTabId = selectedTabId
    visibleTabWasSubmitted = false

    // Update scrolling
    if (scrollToTabID != 0)
        scrollToTab(scrollToTabID, sections)
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
                ImGui.io.deltaTime * scrollingSpeed
        )
    } else scrollingSpeed = 0f

    scrollingRectMinX = barRect.min.x + sections[0].width + sections[0].spacing
    scrollingRectMaxX = barRect.max.x - sections[2].width - sections[1].spacing

    // Actual layout in host window (we don't do it in BeginTabBar() so as not to waste an extra frame)
    val window = g.currentWindow!!
    window.dc.cursorPos put barRect.min
    ImGui.itemSize(Vec2(widthAllTabs, barRect.height), framePadding.y)
    window.dc.idealMaxPos.x = window.dc.idealMaxPos.x max (barRect.min.x + widthAllTabsIdeal)
}

/** Dockable windows uses Name/ID in the global namespace. Non-dockable items use the ID stack.
 *  ~ TabBarCalcTabID   */
fun TabBar.calcTabID(label: String, dockedWindow: Window?): ID {
    assert(dockedWindow == null) { "master branch only" }
//    IM_UNUSED(docked_window);
    return when {
        flags has TabBarFlag._DockNode -> {
            val id = hashStr(label)
            ImGui.keepAliveID(id)
            id
        }

        else -> g.currentWindow!!.getID(label)
    }
}

fun tabBarCalcMaxTabWidth() = g.fontSize * 20f

/** ~TabBarScrollClamp */
fun TabBar.scrollClamp(scrolling: Float): Float = (scrolling min (widthAllTabs - barRect.width)) max 0f

/** ~TabBarScrollToTab
 *
 *  Note: we may scroll to tab that are not selected! e.g. using keyboard arrow keys */
fun TabBar.scrollToTab(tabId: ID, sections: Array<TabBarSection>) {

    val tab = findTabByID(tabId) ?: return
    if (tab.flags has TabItemFlag._SectionMask)
        return

    val margin = g.fontSize * 1f // When to scroll to make Tab N+1 visible always make a bit of N visible to suggest more scrolling area (since we don't have a scrollbar)
    val order = tab.order

    // Scrolling happens only in the central section (leading/trailing sections are not scrolling)
    // FIXME: This is all confusing.
    val scrollableWidth = barRect.width - sections[0].width - sections[2].width - sections[1].spacing

    // We make all tabs positions all relative Sections[0].Width to make code simpler
    val tabX1 = tab.offset - sections[0].width + if (order > sections[0].tabCount - 1) -margin else 0f
    val tabX2 = tab.offset - sections[0].width + tab.width + if (order + 1 < tabs.size - sections[2].tabCount) margin else 1f
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
fun TabBar.scrollingButtons(): TabItem? {

    val window = g.currentWindow!!

    val arrowButtonSize = Vec2(g.fontSize - 2f, g.fontSize + g.style.framePadding.y * 2f)
    val scrollingButtonsWidth = arrowButtonSize.x * 2f

    val backupCursorPos = Vec2(window.dc.cursorPos)
    //window->DrawList->AddRect(ImVec2(tab_bar->BarRect.Max.x - scrolling_buttons_width, tab_bar->BarRect.Min.y), ImVec2(tab_bar->BarRect.Max.x, tab_bar->BarRect.Max.y), IM_COL32(255,0,0,255));

    var selectDir = 0
    val arrowCol = Vec4(ImGui.style.colors[Col.Text])
    arrowCol.w *= 0.5f

    ImGui.pushStyleColor(Col.Text, arrowCol)
    ImGui.pushStyleColor(Col.Button, Vec4(0f))
    val backupRepeatDelay = ImGui.io.keyRepeatDelay
    val backupRepeatRate = ImGui.io.keyRepeatRate
    ImGui.io.keyRepeatDelay = 0.25f
    ImGui.io.keyRepeatRate = 0.2f
    val x = barRect.min.x max (barRect.max.x - scrollingButtonsWidth)
    window.dc.cursorPos.put(x, barRect.min.y)
    if (ImGui.arrowButtonEx("##<", Dir.Left, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat))
        selectDir = -1
    window.dc.cursorPos.put(x + arrowButtonSize.x, barRect.min.y)
    if (ImGui.arrowButtonEx("##>", Dir.Right, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat))
        selectDir = +1
    ImGui.popStyleColor(2)
    ImGui.io.keyRepeatRate = backupRepeatRate
    ImGui.io.keyRepeatDelay = backupRepeatDelay

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
                tabToScrollTo = if (targetOrder < 0 || targetOrder >= tabs.size) tabToScrollTo else null
            }
        }
    }
    window.dc.cursorPos put backupCursorPos
    barRect.max.x -= scrollingButtonsWidth + 1f

    return tabToScrollTo
}

/** ~TabBarTabListPopupButton */
fun TabBar.tabListPopupButton(): TabItem? {

    val window = g.currentWindow!!

    // We use g.Style.FramePadding.y to match the square ArrowButton size
    val tabListPopupButtonWidth = g.fontSize + ImGui.style.framePadding.y
    val backupCursorPos = Vec2(window.dc.cursorPos)
    window.dc.cursorPos.put(barRect.min.x - ImGui.style.framePadding.y, barRect.min.y)
    barRect.min.x += tabListPopupButtonWidth

    val arrowCol = Vec4(ImGui.style.colors[Col.Text])
    arrowCol.w *= 0.5f
    ImGui.pushStyleColor(Col.Text, arrowCol)
    ImGui.pushStyleColor(Col.Button, Vec4())
    val open = ImGui.beginCombo("##v", null, ComboFlag.NoPreview or ComboFlag.HeightLargest)
    ImGui.popStyleColor(2)

    var tabToSelect: TabItem? = null
    if (open) {
        for (tab in tabs) {
            if (tab.flags has TabItemFlag._Button)
                continue

            if (ImGui.selectable(tab.name, selectedTabId == id))
                tabToSelect = tab
        }
        ImGui.endCombo()
    }

    window.dc.cursorPos = backupCursorPos
    return tabToSelect
}