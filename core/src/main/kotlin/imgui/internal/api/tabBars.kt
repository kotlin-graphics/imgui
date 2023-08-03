package imgui.internal.api

import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeButton
import imgui.ImGui.isClicked
import imgui.ImGui.isDragging
import imgui.ImGui.isReleased
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushOverrideID
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderBullet
import imgui.ImGui.renderTextEllipsis
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.classes.TabBar
import imgui.internal.classes.TabItem
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.sections.*
import imgui.static.calcTabID
import imgui.static.layout
import imgui.static.tabBarCalcMaxTabWidth
import imgui.static.tabBarRef
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0

internal interface tabBars {

    val currentTabBar: TabBar?
        get() = g.currentTabBar

    /** ~ beginTabBarEx */
    fun TabBar.beginEx(bb: Rect, flags_: TabBarFlags): Boolean {

        var flags = flags_

        val window = g.currentWindow!!
        if (window.skipItems)
            return false

        if (flags hasnt TabBarFlag._DockNode)
            pushOverrideID(id)

        // Add to stack
        g.currentTabBarStack += tabBarRef
        g.currentTabBar = this

        // Append with multiple BeginTabBar()/EndTabBar() pairs.
        backupCursorPos put window.dc.cursorPos
        if (currFrameVisible == g.frameCount) {
            window.dc.cursorPos.put(barRect.min.x, barRect.max.y + itemSpacingY)
            beginCount++
            return true
        }

        // Ensure correct ordering when toggling ImGuiTabBarFlags_Reorderable flag, ensure tabs are ordered based on their submission order.
        if ((flags and TabBarFlag.Reorderable) != (this.flags and TabBarFlag.Reorderable) || (tabsAddedNew && flags hasnt TabBarFlag.Reorderable))
            if (tabs.size > 1)
                tabs.sortBy(TabItem::beginOrder)
        tabsAddedNew = false

        // Flags
        if (flags hasnt TabBarFlag.FittingPolicyMask)
            flags /= TabBarFlag.FittingPolicyDefault

        this.flags = flags
        barRect put bb
        wantLayout = true // Layout will be done on the first call to ItemTab()
        prevFrameVisible = currFrameVisible
        currFrameVisible = g.frameCount
        prevTabsContentsHeight = currTabsContentsHeight
        currTabsContentsHeight = 0f
        itemSpacingY = g.style.itemSpacing.y
        framePadding put g.style.framePadding
        tabsActiveCount = 0
        lastTabItemIdx = -1
        beginCount = 1

        // Layout
        // Set cursor pos in a way which only be used in the off-chance the user erroneously submits item before BeginTabItem(): items will overlap
        window.dc.cursorPos.put(barRect.min.x, barRect.max.y + itemSpacingY)

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

    /** ~TabBarFindTabByID */
    infix fun TabBar.findTabByID(tabId: ID): TabItem? = if (tabId == 0) null else tabs.find { it.id == tabId }

    /** ~TabBarFindTabByOrder */
    infix fun TabBar.findTabByOrder(order: Int): TabItem? = tabs.getOrNull(order)

    /** ~TabBarGetCurrentTab */
    val TabBar.currentTab: TabItem?
        get() = tabs.getOrNull(lastTabItemIdx)

    // TabBarGetTabOrder
    // TabBarGetTabName
    // -> TabBar class

    infix fun TabBar.getTabName(tab: TabItem): String = tabsNames[tab.nameOffset]

    /** The *TabId fields be already set by the docking system _before_ the actual TabItem was created, so we clear them regardless.
     *  ~ tabBarRemoveTab     */
    infix fun TabBar.removeTab(tabId: ID) {
        findTabByID(tabId)?.let(tabs::remove)
        if (visibleTabId == tabId) visibleTabId = 0
        if (selectedTabId == tabId) selectedTabId = 0
        if (nextSelectedTabId == tabId) nextSelectedTabId = 0
    }

    /** Called on manual closure attempt
     *  ~ tabBarCloseTab     */
    infix fun TabBar.closeTab(tab: TabItem) {

        if (tab.flags has TabItemFlag._Button)
            return // A button appended with TabItemButton().

        if (tab.flags hasnt TabItemFlag.UnsavedDocument) { // This will remove a frame of lag for selecting another tab on closure.
            // However we don't run it in the case where the 'Unsaved' flag is set, so user gets a chance to fully undo the closure
            tab.wantClose = true
            if (visibleTabId == tab.id) {
                tab.lastFrameVisible = -1
                nextSelectedTabId = 0
                selectedTabId = 0
            }
        } else // Actually select before expecting closure attempt (on an UnsavedDocument tab user is expect to e.g. show a popup)
            if (visibleTabId != tab.id)
                queueFocus(tab)
    }

    /** ~TabBarQueueFocus */
    infix fun TabBar.queueFocus(tab: TabItem) {
        nextSelectedTabId = tab.id
    }

    /** ~TabBarQueueReorder */
    fun TabBar.queueReorder(tab: TabItem, offset: Int) {
        assert(offset != 0)
        assert(reorderRequestTabId == 0)
        reorderRequestTabId = tab.id
        reorderRequestOffset = offset
    }

    /** ~TabBarQueueReorderFromMousePos */
    fun TabBar.queueReorderFromMousePos(srcTab: TabItem, mousePos: Vec2) {

        assert(reorderRequestTabId == 0)
        if (flags hasnt TabBarFlag.Reorderable)
            return

        val isCentralSection = srcTab.flags hasnt TabItemFlag._SectionMask
        val barOffset = barRect.min.x - if (isCentralSection) scrollingTarget else 0f

        // Count number of contiguous tabs we are crossing over
        val dir = if (barOffset + srcTab.offset > mousePos.x) -1 else +1
        val srcIdx = tabs.indexOf(srcTab)
        var dstIdx = srcIdx
        var i = srcIdx
        while (i >= 0 && i < tabs.size) {
            // Reordered tabs must share the same section
            val dstTab = tabs[i]
            if (dstTab.flags has TabItemFlag.NoReorder) {
                i += dir
                break
            }
            if ((dstTab.flags and TabItemFlag._SectionMask) != (srcTab.flags and TabItemFlag._SectionMask)) {
                i += dir
                break
            }
            dstIdx = i

            // Include spacing after tab, so when mouse cursor is between tabs we would not continue checking further tabs that are not hovered.
            val x1 = barOffset + dstTab.offset - g.style.itemInnerSpacing.x
            val x2 = barOffset + dstTab.offset + dstTab.width + g.style.itemInnerSpacing.x
            //GetForegroundDrawList()->AddRect(ImVec2(x1, tab_bar->BarRect.Min.y), ImVec2(x2, tab_bar->BarRect.Max.y), IM_COL32(255, 0, 0, 255));
            if ((dir < 0 && mousePos.x > x1) || (dir > 0 && mousePos.x < x2)) {
                i += dir
                break
            }
            i += dir
        }

        if (dstIdx != srcIdx)
            queueReorder(srcTab, dstIdx - srcIdx)
    }

    /** ~TabBarProcessReorder */
    fun TabBar.processReorder(): Boolean {
        val tab1 = findTabByID(reorderRequestTabId)
        if (tab1 == null || tab1.flags has TabItemFlag.NoReorder)
            return false

        //IM_ASSERT(tab_bar->Flags & ImGuiTabBarFlags_Reorderable); // <- this may happen when using debug tools
        val tab2Order = tab1.order + reorderRequestOffset
        if (tab2Order < 0 || tab2Order >= tabs.size)
            return false

        // Reordered tabs must share the same section
        // (Note: TabBarQueueReorderFromMousePos() also has a similar test but since we allow direct calls to TabBarQueueReorder() we do it here too)
        val tab2 = tabs[tab2Order]
        if (tab2.flags has TabItemFlag.NoReorder)
            return false
        if ((tab1.flags and TabItemFlag._SectionMask) != (tab2.flags and TabItemFlag._SectionMask))
            return false

//        ImGuiTabItem item_tmp = * tab1;
//        ImGuiTabItem * src_tab = (tab_bar->ReorderRequestOffset > 0) ? tab1+1 : tab2;
//        ImGuiTabItem * dst_tab = (tab_bar->ReorderRequestOffset > 0) ? tab1 : tab2+1;
//        const int move_count = (tab_bar->ReorderRequestOffset > 0) ? tab_bar->ReorderRequestOffset :-tab_bar->ReorderRequestOffset;
//        memmove(dst_tab, src_tab, move_count * sizeof(ImGuiTabItem));
        tabs.remove(tab1)
        tabs.add(tab2Order, tab1)

        if (flags has TabBarFlag._SaveSettings)
            ImGui.markIniSettingsDirty()
        return true
    }

    fun TabBar.tabItemEx(label: String, pOpen_: KMutableProperty0<Boolean>?, flags_: TabItemFlags, dockedWindow: Window?): Boolean {

        var pOpen = pOpen_
        var flags = flags_ // Layout whole tab bar if not already done
        if (wantLayout) {
            val backupNextItemData = g.nextItemData.copy()
            layout()
            g.nextItemData = backupNextItemData
        }

        val window = g.currentWindow!!
        if (window.skipItems)
            return false

        val id = calcTabID(label, dockedWindow)

        // If the user called us with *p_open == false, we early out and don't render.
        // We make a call to ItemAdd() so that attempts to use a contextual popup menu with an implicit ID won't use an older ID.
        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags)
        if (pOpen?.get() == false) {
            ImGui.itemAdd(Rect(), id, null, ItemFlag.NoNav)
            return false
        }

        assert(pOpen == null || flags hasnt TabItemFlag._Button)
        assert((TabItemFlag.Leading or TabItemFlag.Trailing) !in flags) { "Can't use both Leading and Trailing" }

        // Store into ImGuiTabItemFlags_NoCloseButton, also honor ImGuiTabItemFlags_NoCloseButton passed by user (although not documented)
        if (flags has TabItemFlag._NoCloseButton)
            pOpen = null
        else if (pOpen == null)
            flags /= TabItemFlag._NoCloseButton

        // Acquire tab data
        var tabIsNew = false
        val tab = findTabByID(id) ?: TabItem().also {
            tabs += it
            it.id = id
            tabsAddedNew = true; tabIsNew = true
        }
        lastTabItemIdx = tabs.indexOf(tab)

        // Calculate tab contents size
        val size = tabItemCalcSize(label, pOpen != null || flags has TabItemFlag.UnsavedDocument)
        tab.requestedWidth = -1f
        if (g.nextItemData.flags has NextItemDataFlag.HasWidth) {
            size.x = g.nextItemData.width; tab.requestedWidth = g.nextItemData.width
        }
        if (tabIsNew)
            tab.width = 1f max size.x
        tab.contentWidth = size.x
        tab.beginOrder = tabsActiveCount++

        val tabBarAppearing = prevFrameVisible + 1 < g.frameCount
        val tabBarFocused = this.flags has TabBarFlag._IsFocused
        val tabAppearing = tab.lastFrameVisible + 1 < g.frameCount
        val tabJustUnsaved = flags has TabItemFlag.UnsavedDocument && tab.flags hasnt TabItemFlag.UnsavedDocument
        val isTabButton = flags has TabItemFlag._Button
        tab.lastFrameVisible = g.frameCount
        tab.flags = flags

        // Append name _WITH_ the zero-terminator
        if (dockedWindow != null)
            assert(dockedWindow == null) { "master branch only" }
        else {
            tab.nameOffset = tabsNames.size
            tabsNames += label
        }

        // Update selected tab
        if (!isTabButton) {
            if (tabAppearing && this.flags has TabBarFlag.AutoSelectNewTabs && nextSelectedTabId == 0)
                if (!tabBarAppearing || selectedTabId == 0)
                    queueFocus(tab)  // New tabs gets activated
            if (flags has TabItemFlag.SetSelected && selectedTabId != id) // _SetSelected can only be passed on explicit tab bar
                queueFocus(tab)
        }

        // Lock visibility
        // (Note: tab_contents_visible != tab_selected... because CTRL+TAB operations may preview some tabs without selecting them!)
        var tabContentsVisible = visibleTabId == id
        if (tabContentsVisible)
            visibleTabWasSubmitted = true

        // On the very first frame of a tab bar we let first tab contents be visible to minimize appearing glitches
        if (!tabContentsVisible && selectedTabId == 0 && tabBarAppearing)
            if (tabs.size == 1 && this.flags hasnt TabBarFlag.AutoSelectNewTabs)
                tabContentsVisible = true

        // Note that tab_is_new is not necessarily the same as tab_appearing! When a tab bar stops being submitted
        // and then gets submitted again, the tabs will have 'tab_appearing=true' but 'tab_is_new=false'.
        if (tabAppearing && (!tabBarAppearing || tabIsNew)) {
            ImGui.itemAdd(Rect(), id, null, ItemFlag.NoNav)
            if (isTabButton)
                return false
            return tabContentsVisible
        }

        if (selectedTabId == id)
            tab.lastFrameSelected = g.frameCount

        // Backup current layout position
        val backupMainCursorPos = Vec2(window.dc.cursorPos)

        // Layout
        val isCentralSection = tab.flags hasnt TabItemFlag._SectionMask
        size.x = tab.width
        val x = if (isCentralSection) floor(tab.offset - scrollingAnim) else tab.offset
        window.dc.cursorPos = barRect.min + Vec2(x, 0f)
        val pos = Vec2(window.dc.cursorPos)
        val bb = Rect(pos, pos + size)

        // We don't have CPU clipping primitives to clip the CloseButton (until it becomes a texture), so need to add an extra draw call (temporary in the case of vertical animation)
        val wantClipRect = isCentralSection && (bb.min.x < scrollingRectMinX || bb.max.x > scrollingRectMaxX)
        if (wantClipRect)
            ImGui.pushClipRect(Vec2(bb.min.x max scrollingRectMinX, bb.min.y - 1), Vec2(scrollingRectMaxX, bb.max.y), true)

        val backupCursorMaxPos = Vec2(window.dc.cursorMaxPos)
        ImGui.itemSize(bb.size, style.framePadding.y)
        window.dc.cursorMaxPos = backupCursorMaxPos

        if (!ImGui.itemAdd(bb, id)) {
            if (wantClipRect) ImGui.popClipRect()
            window.dc.cursorPos = backupMainCursorPos
            return tabContentsVisible
        }

        // Click to Select a tab
        var buttonFlags = (if (isTabButton) ButtonFlag.PressedOnClickRelease else ButtonFlag.PressedOnClick) or ButtonFlag.AllowOverlap
        if (g.dragDropActive)
            buttonFlags /= ButtonFlag.PressedOnDragDropHold
        val (pressed, hovered, held) = ImGui.buttonBehavior(bb, id, buttonFlags)
        if (pressed && !isTabButton)
            queueFocus(tab)

        // Allow the close button to overlap unless we are dragging (in which case we don't want any overlapping tabs to be hovered)
        if (g.activeId != id) // Because: we don't want to hover other items while dragging active)
            ImGui.setItemAllowOverlap()

        // Drag and drop: re-order tabs
        if (held && !tabAppearing && MouseButton.Left.isDragging())
        // While moving a tab it will jump on the other side of the mouse, so we also test for MouseDelta.x
            if (!g.dragDropActive && this.flags has TabBarFlag.Reorderable)
                if (ImGui.io.mouseDelta.x < 0f && ImGui.io.mousePos.x < bb.min.x) {
                    if (this.flags has TabBarFlag.Reorderable)
                        queueReorderFromMousePos(tab, g.io.mousePos)
                } else if (ImGui.io.mouseDelta.x > 0f && ImGui.io.mousePos.x > bb.max.x)
                    if (this.flags has TabBarFlag.Reorderable)
                        queueReorderFromMousePos(tab, g.io.mousePos)

        //        if (false)
        //            if (hovered && g.hoveredIdNotActiveTimer > TOOLTIP_DELAY && bb.width < tab.widthContents)        {
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
        ImGui.renderNavHighlight(bb, id)

        // Select with right mouse button. This is so the common idiom for context menu automatically highlight the current widget.
        val hoveredUnblocked = ImGui.isItemHovered(HoveredFlag.AllowWhenBlockedByPopup)
        if (hoveredUnblocked && (MouseButton.Right.isClicked || MouseButton.Right.isReleased) && !isTabButton)
            queueFocus(tab)

        if (this.flags has TabBarFlag.NoCloseWithMiddleMouseButton)
            flags /= TabItemFlag.NoCloseWithMiddleMouseButton

        // Render tab label, process close button
        val closeButtonId = if (pOpen?.get() == true) ImGui.getIDWithSeed("#CLOSE", -1, id) else 0
        val (justClosed, textClipped) = tabItemLabelAndCloseButton(displayDrawList, bb, if (tabJustUnsaved) flags wo TabItemFlag.UnsavedDocument else flags, framePadding, label.toByteArray(), id, closeButtonId, tabContentsVisible)
        if (justClosed && pOpen != null) {
            pOpen.set(false)
            closeTab(tab)
        }

        // Restore main window position so user can draw there
        if (wantClipRect) ImGui.popClipRect()
        window.dc.cursorPos = backupMainCursorPos

        // Tooltip
        // (Won't work over the close button because ItemOverlap systems messes up with HoveredIdTimer-> seems ok)
        // (We test IsItemHovered() to discard e.g. when another item is active or drag and drop over the tab bar, which g.HoveredId ignores)
        // FIXME: This is a mess.
        // FIXME: We may want disabled tab to still display the tooltip?
        if (textClipped && g.hoveredId == id && !held)
            if (this.flags hasnt TabBarFlag.NoTooltip && tab.flags hasnt TabItemFlag.NoTooltip)
                ImGui.setItemTooltip(label.substring(0, ImGui.findRenderedTextEnd(label)))

        assert(!isTabButton || !(selectedTabId == tab.id && isTabButton)) { "TabItemButton should not be selected" }
        return if (isTabButton) pressed else tabContentsVisible
    }

    fun tabItemCalcSize(window: Window): Vec2 {
        check(false) { "This function exists to facilitate merge with 'docking' branch." }
        return Vec2()
    }

    fun tabItemCalcSize(label: String, hasCloseButtonOrUnsavedMarker: Boolean): Vec2 {

        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)
        val size = Vec2(labelSize.x + style.framePadding.x, labelSize.y + style.framePadding.y * 2f)
        size.x += style.framePadding.x + when {
            hasCloseButtonOrUnsavedMarker -> style.itemInnerSpacing.x + g.fontSize // We use Y intentionally to fit the close button circle.
            else -> 1f
        }
        return Vec2(size.x min tabBarCalcMaxTabWidth(), size.y)
    }

    fun tabItemBackground(drawList: DrawList, bb: Rect, flags: TabItemFlags, col: Int) {

        // While rendering tabs, we trim 1 pixel off the top of our bounding box so they can fit within a regular frame height while looking "detached" from it.
        val width = bb.width
        assert(width > 0f)
        val rounding = max(0f, min(if (flags has TabItemFlag._Button) g.style.frameRounding else style.tabRounding, width * 0.5f - 1f))
        val y1 = bb.min.y + 1f
        val y2 = bb.max.y - 1f
        drawList.apply {
            pathLineTo(Vec2(bb.min.x, y2))
            pathArcToFast(Vec2(bb.min.x + rounding, y1 + rounding), rounding, 6, 9)
            pathArcToFast(Vec2(bb.max.x - rounding, y1 + rounding), rounding, 9, 12)
            pathLineTo(Vec2(bb.max.x, y2))
            pathFillConvex(col)
            if (style.tabBorderSize > 0f) {
                pathLineTo(Vec2(bb.min.x + 0.5f, y2))
                pathArcToFast(Vec2(bb.min.x + rounding + 0.5f, y1 + rounding + 0.5f), rounding, 6, 9)
                pathArcToFast(Vec2(bb.max.x - rounding - 0.5f, y1 + rounding + 0.5f), rounding, 9, 12)
                pathLineTo(Vec2(bb.max.x - 0.5f, y2))
                pathStroke(Col.Border.u32, thickness = style.tabBorderSize)
            }
        }
    }

    /** Render text label (with custom clipping) + Unsaved Document marker + Close Button logic
     *  We tend to lock style.FramePadding for a given tab-bar, hence the 'frame_padding' parameter.
     *  [JVM] @return [justClosed: Boolean, textClipped: Boolean] */
    fun tabItemLabelAndCloseButton(drawList: DrawList, bb: Rect, flags: TabItemFlags, framePadding: Vec2,
                                   label: ByteArray, tabId: ID, closeButtonId: ID, isContentsVisible: Boolean): Vec2bool {

        val labelSize = calcTextSize(label, 0, hideTextAfterDoubleHash = true)

        var justClosed = false
        var textClipped = false

        if (bb.width <= 1f)
            return Vec2bool(justClosed, textClipped)

        // In Style V2 we'll have full override of all colors per state (e.g. focused, selected)
        // But right now if you want to alter text color of tabs this is what you need to do.
        //        #if 0
        //        const float backup_alpha = g.Style.Alpha;
        //        if (!is_contents_visible)
        //            g.Style.Alpha *= 0.7f;
        //        #endif

        // Render text label (with clipping + alpha gradient) + unsaved marker
        val textPixelClipBb = Rect(bb.min.x + framePadding.x, bb.min.y + framePadding.y, bb.max.x - framePadding.x, bb.max.y)
        val textEllipsisClipBb = Rect(textPixelClipBb)

        // Return clipped state ignoring the close button
        textClipped = (textEllipsisClipBb.min.x + labelSize.x) > textPixelClipBb.max.x
        //draw_list->AddCircle(text_ellipsis_clip_bb.Min, 3.0f, *out_text_clipped ? IM_COL32(255, 0, 0, 255) : IM_COL32(0, 255, 0, 255));

        val buttonSz = g.fontSize
        val buttonPos = Vec2(bb.min.x max (bb.max.x - framePadding.x * 2f - buttonSz), bb.min.y)

        // Close Button & Unsaved Marker
        // We are relying on a subtle and confusing distinction between 'hovered' and 'g.HoveredId' which happens because we are using ImGuiButtonFlags_AllowOverlapMode + SetItemAllowOverlap()
        //  'hovered' will be true when hovering the Tab but NOT when hovering the close button
        //  'g.HoveredId==id' will be true when hovering the Tab including when hovering the close button
        //  'g.ActiveId==close_button_id' will be true when we are holding on the close button, in which case both hovered booleans are false
        var closeButtonPressed = false
        var closeButtonVisible = false
        if (closeButtonId != 0)
            if (isContentsVisible || bb.width >= buttonSz max style.tabMinWidthForCloseButton)
                if (g.hoveredId == tabId || g.hoveredId == closeButtonId || g.activeId == tabId || g.activeId == closeButtonId)
                    closeButtonVisible = true
        val unsavedMarkerVisible = flags hasnt TabItemFlag.UnsavedDocument && buttonPos.x + buttonSz <= bb.max.x

        if (closeButtonVisible) {
            val lastItemBackup = g.lastItemData
            pushStyleVar(StyleVar.FramePadding, framePadding)
            if (closeButton(closeButtonId, buttonPos))
                closeButtonPressed = true
            popStyleVar()
            g.lastItemData put lastItemBackup

            // Close with middle mouse button
            if (flags hasnt TabItemFlag.NoCloseWithMiddleMouseButton && MouseButton.Middle.isClicked)
                closeButtonPressed = true
        } else if (unsavedMarkerVisible) {
            val bulletBb = Rect(buttonPos, buttonPos + buttonSz + g.style.framePadding * 2f)
            drawList.renderBullet(bulletBb.center, Col.Text.u32) // ~RenderBullet(bullet_bb.GetCenter());
        }

        // This is all rather complicated
        // (the main idea is that because the close button only appears on hover, we don't want it to alter the ellipsis position)
        // FIXME: if FramePadding is noticeably large, ellipsis_max_x will be wrong here (e.g. #3497), maybe for consistency that parameter of RenderTextEllipsis() shouldn't exist..
        var ellipsisMaxX = if (closeButtonVisible) textPixelClipBb.max.x else bb.max.x - 1f
        if (closeButtonVisible || unsavedMarkerVisible) {
            textPixelClipBb.max.x -= if (closeButtonVisible) buttonSz else buttonSz * 0.8f
            textEllipsisClipBb.max.x -= if (unsavedMarkerVisible) buttonSz * 0.8f else 0f
            ellipsisMaxX = textPixelClipBb.max.x
        }
        renderTextEllipsis(drawList, textEllipsisClipBb.min, textEllipsisClipBb.max, textPixelClipBb.max.x,
                ellipsisMaxX, label, textSizeIfKnown = labelSize)

        //        #if 0
        //        if (!is_contents_visible)
        //            g.Style.Alpha = backup_alpha;
        //        #endif

        justClosed = closeButtonPressed
        return Vec2bool(justClosed, textClipped)
    }
}