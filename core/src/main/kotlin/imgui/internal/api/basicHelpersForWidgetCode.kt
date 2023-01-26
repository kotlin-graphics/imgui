package imgui.internal.api

import glm_.*
import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.currentWindow
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.hoveredId
import imgui.ImGui.isActiveIdUsingKey
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.api.internal.Companion.shrinkWidthItemComparer
import imgui.internal.classes.Rect
import imgui.internal.classes.ShrinkWidthItem
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.sections.*
import imgui.static.navProcessItem
import java.util.*

//-----------------------------------------------------------------------------
// [SECTION] LAYOUT
//-----------------------------------------------------------------------------
// - ItemSize()
// - ItemAdd()
// - SameLine()
// - Indent()
// - Unindent()
// - BeginGroup()
// - EndGroup()
// Also see in imgui_widgets: tab bars, columns.
//-----------------------------------------------------------------------------

/** Basic Helpers for widget code */
internal interface basicHelpersForWidgetCode {

    /** Advance cursor given item size for layout.
     *  Register minimum needed size so it can extend the bounding box used for auto-fit calculation.
     *  See comments in ItemAdd() about how/why the size provided to ItemSize() vs ItemAdd() may often different. */
    fun itemSize(size: Vec2, textBaselineY: Float = -1f) {

        val window = currentWindow
        if (window.skipItems) return

        // We increase the height in this function to accommodate for baseline offset.
        // In theory we should be offsetting the starting position (window->DC.CursorPos), that will be the topic of a larger refactor,
        // but since ItemSize() is not yet an API that moves the cursor (to handle e.g. wrapping) enlarging the height has the same effect.
        val offsetToMatchBaselineY = if (textBaselineY >= 0f) 0f max (window.dc.currLineTextBaseOffset - textBaselineY) else 0f
        val lineHeight = window.dc.currLineSize.y max (size.y + offsetToMatchBaselineY)

        // Always align ourselves on pixel boundaries
        window.dc.apply {
            //if (io.keyAlt) window.drawList.addRect(window.dc.cursorPos, window.dc.cursorPos + Vec2(size.x, lineHeight), COL32(255,0,0,200)); // [DEBUG]
            cursorPosPrevLine.put(cursorPos.x + size.x, cursorPos.y)
            cursorPos.x = floor(window.pos.x + indent + columnsOffset)           // Next line
            cursorPos.y = floor(cursorPos.y + lineHeight + style.itemSpacing.y)  // Next line
            cursorMaxPos.x = cursorMaxPos.x max cursorPosPrevLine.x
            cursorMaxPos.y = cursorMaxPos.y max (cursorPos.y - style.itemSpacing.y)
            //if (io.keyAlt) window.drawList.addCircle(window.dc.cursorMaxPos, 3f, COL32(255,0,0,255), 4); // [DEBUG]

            prevLineSize.y = lineHeight
            currLineSize.y = 0f
            prevLineTextBaseOffset = currLineTextBaseOffset max textBaselineY
            currLineTextBaseOffset = 0f

            // Horizontal layout mode
            if (layoutType == LayoutType.Horizontal) sameLine()
        }
    }

    fun itemSize(bb: Rect, textBaselineY: Float = -1f) = itemSize(bb.size, textBaselineY)

    /** Declare item bounding box for clipping and interaction.
     *  Note that the size can be different than the one provided to ItemSize(). Typically, widgets that spread over available surface
     *  declare their minimum size requirement to ItemSize() and provide a larger region to ItemAdd() which is used drawing/interaction. */
    fun itemAdd(bb: Rect, id: ID, navBbArg: Rect? = null, flags: ItemAddFlags = 0): Boolean {

        val window = g.currentWindow!!

        // Set item data
        // (DisplayRect is left untouched, made valid when ImGuiItemStatusFlags_HasDisplayRect is set)
        g.lastItemData.id = id
        g.lastItemData.rect put bb
        g.lastItemData.navRect put (navBbArg ?: bb)
        g.lastItemData.inFlags = g.currentItemFlags
        g.lastItemData.statusFlags = ItemStatusFlag.None.i

        // Directional navigation processing
        if (id != 0) {
            // Navigation processing runs prior to clipping early-out
            //  (a) So that NavInitRequest can be honored, for newly opened windows to select a default widget
            //  (b) So that we can scroll up/down past clipped items. This adds a small O(N) cost to regular navigation requests
            //      unfortunately, but it is still limited to one window. It may not scale very well for windows with ten of
            //      thousands of item, but at least NavMoveRequest is only set on user interaction, aka maximum once a frame.
            //      We could early out with "if (is_clipped && !g.NavInitRequest) return false;" but when we wouldn't be able
            //      to reach unclipped widgets. This would work if user had explicit scrolling control (e.g. mapped on a stick).
            // We intentionally don't check if g.NavWindow != NULL because g.NavAnyRequest should only be set when it is non null.
            // If we crash on a NULL g.NavWindow we need to fix the bug elsewhere.
            window.dc.navLayersActiveMaskNext = window.dc.navLayersActiveMaskNext or (1 shl window.dc.navLayerCurrent)
            if (g.navId == id || g.navAnyRequest)
                if (g.navWindow!!.rootWindowForNav === window.rootWindowForNav)
                    if (window == g.navWindow || (window.flags or g.navWindow!!.flags) has WindowFlag._NavFlattened)
                        navProcessItem()

            // [DEBUG] Item Picker tool, when enabling the "extended" version we perform the check in ItemAdd()
            if (IMGUI_DEBUG_TOOL_ITEM_PICKER_EX && id == g.debugItemPickerBreakId) {
                IM_DEBUG_BREAK()
                g.debugItemPickerBreakId = 0
            }
        }
        g.nextItemData.flags = NextItemDataFlag.None.i

        if (IMGUI_ENABLE_TEST_ENGINE && id != 0)
            IMGUI_TEST_ENGINE_ITEM_ADD(navBbArg ?: bb, id)

        // Clipping test
        if (isClippedEx(bb, id, false)) return false
        //if (g.io.KeyAlt) window->DrawList->AddRect(bb.Min, bb.Max, IM_COL32(255,255,0,120)); // [DEBUG]

        // Tab stop handling (previously was using internal ItemFocusable() api)
        // FIXME-NAV: We would now want to move this above the clipping test, but this would require being able to scroll and currently this would mean an extra frame. (#4079, #343)
        if (flags has ItemAddFlag.Focusable)
            itemFocusable(window, id)

        // We need to calculate this now to take account of the current clipping rectangle (as items like Selectable may change them)
        if (isMouseHoveringRect(bb))
            g.lastItemData.statusFlags /= ItemStatusFlag.HoveredRect
        return true
    }

    /** Internal facing ItemHoverable() used when submitting widgets. Differs slightly from IsItemHovered().    */
    fun itemHoverable(bb: Rect, id: ID): Boolean {
        val window = g.currentWindow!!
        return when {
            g.hoveredId != 0 && g.hoveredId != id && !g.hoveredIdAllowOverlap -> false
            g.hoveredWindow !== window -> false
            g.activeId != 0 && g.activeId != id && !g.activeIdAllowOverlap -> false
            !isMouseHoveringRect(bb) -> false
            g.navDisableMouseHover -> false
            !window.isContentHoverable(HoveredFlag.None) -> {
                g.hoveredIdDisabled = true
                false
            }
            else -> {
                // We exceptionally allow this function to be called with id==0 to allow using it for easy high-level
                // hover test in widgets code. We could also decide to split this function is two.
                if (id != 0)
                    hoveredId = id

                // When disabled we'll return false but still set HoveredId
                val itemFlags = if(g.lastItemData.id == id) g.lastItemData.inFlags else g.currentItemFlags
                if (itemFlags has ItemFlag.Disabled) {
                    // Release active id if turning disabled
                    if (g.activeId == id)
                        clearActiveID()
                    g.hoveredIdDisabled = true
                    false
                }
                else {
                    if (id != 0) {
                        // [DEBUG] Item Picker tool!
                        // We perform the check here because SetHoveredID() is not frequently called (1~ time a frame), making
                        // the cost of this tool near-zero. We can get slightly better call-stack and support picking non-hovered
                        // items if we perform the test in ItemAdd(), but that would incur a small runtime cost.
                        // #define IMGUI_DEBUG_TOOL_ITEM_PICKER_EX in imconfig.h if you want this check to also be performed in ItemAdd().
                        if (g.debugItemPickerActive && g.hoveredIdPreviousFrame == id)
                            foregroundDrawList.addRect(bb.min, bb.max, COL32(255, 255, 0, 255))
                        if (g.debugItemPickerBreakId == id)
                            IM_DEBUG_BREAK()
                    }
                    true
                }
            }
        }
    }

    fun itemFocusable(window: Window, id: ID) {

        assert(id != 0 && id == g.lastItemData.id)

        // Increment counters
        // FIXME: ImGuiItemFlags_Disabled should disable more.
        val isTabStop = g.lastItemData.inFlags hasnt (ItemFlag.NoTabStop or ItemFlag.Disabled)
        window.dc.focusCounterRegular++
        if (isTabStop) {
            window.dc.focusCounterTabStop++
            if (g.navId == id)
                g.navIdTabCounter = window.dc.focusCounterTabStop
        }

        // Process TAB/Shift-TAB to tab *OUT* of the currently focused item.
        // (Note that we can always TAB out of a widget that doesn't allow tabbing in)
        if (g.activeId == id && g.tabFocusPressed && !isActiveIdUsingKey(Key.Tab) && g.tabFocusRequestNextWindow == null) {
            g.tabFocusRequestNextWindow = window
            g.tabFocusRequestNextCounterTabStop = window.dc.focusCounterTabStop + if (g.io.keyShift) if (isTabStop) -1 else 0 else +1 // Modulo on index will be applied at the end of frame once we've got the total counter of items.
        }

        // Handle focus requests
        if (g.tabFocusRequestCurrWindow === window) {
            if (window.dc.focusCounterRegular == g.tabFocusRequestCurrCounterRegular) {
                g.lastItemData.statusFlags = g.lastItemData.statusFlags or ItemStatusFlag.FocusedByCode
                return
            }
            if (isTabStop && window.dc.focusCounterTabStop == g.tabFocusRequestCurrCounterTabStop) {
                g.navJustTabbedId = id
                g.lastItemData.statusFlags = g.lastItemData.statusFlags or ItemStatusFlag.FocusedByTabbing
                return
            }

            // If another item is about to be focused, we clear our own active id
            if (g.activeId == id)
                clearActiveID()
        }
    }

    fun isClippedEx(bb: Rect, id: ID, clipEvenWhenLogged: Boolean): Boolean {
        val window = g.currentWindow!!
        if (!(bb overlaps window.clipRect))
            if (id == 0 || (id != g.activeId && id != g.navId))
                if (clipEvenWhenLogged || !g.logEnabled)
                    return true
        return false
    }

    /** [Internal] Calculate full item size given user provided 'size' parameter and default width/height. Default width is often == CalcItemWidth().
     *  Those two functions CalcItemWidth vs CalcItemSize are awkwardly named because they are not fully symmetrical.
     *  Note that only CalcItemWidth() is publicly exposed.
     *  The 4.0f here may be changed to match CalcItemWidth() and/or BeginChild() (right now we have a mismatch which is harmless but undesirable) */
    fun calcItemSize(size_: Vec2, defaultW: Float, defaultH: Float): Vec2 {
        val window = g.currentWindow!!

        val regionMax = if (size_ anyLessThan 0f) contentRegionMaxAbs else Vec2()

        val size = Vec2(size_)
        if (size.x == 0f)
            size.x = defaultW
        else if (size.x < 0f)
            size.x = 4f max (regionMax.x - window.dc.cursorPos.x + size.x)

        if (size.y == 0f)
            size.y = defaultH
        else if (size.y < 0f)
            size.y = 4f max (regionMax.y - window.dc.cursorPos.y + size.y)

        return size
    }

    fun calcWrapWidthForPos(pos: Vec2, wrapPosX_: Float): Float {

        if (wrapPosX_ < 0f) return 0f

        var wrapPosX = wrapPosX_
        val window = g.currentWindow!!
        if (wrapPosX == 0f) {
            // We could decide to setup a default wrapping max point for auto-resizing windows,
            // or have auto-wrap (with unspecified wrapping pos) behave as a ContentSize extending function?
            //if (window->Hidden && (window->Flags & ImGuiWindowFlags_AlwaysAutoResize))
            //    wrap_pos_x = ImMax(window->WorkRect.Min.x + g.FontSize * 10.0f, window->WorkRect.Max.x);
            //else
            wrapPosX = window.workRect.max.x
        } else if (wrapPosX > 0f)
            wrapPosX += window.pos.x - window.scroll.x // wrap_pos_x is provided is window local space

        return glm.max(wrapPosX - pos.x, 1f)
    }

    fun pushMultiItemsWidths(components: Int, wFull: Float) {
        val window = currentWindow
        val wItemOne = 1f max floor((wFull - (style.itemInnerSpacing.x) * (components - 1)) / components.f)
        val wItemLast = 1f max floor(wFull - (wItemOne + style.itemInnerSpacing.x) * (components - 1))
        window.dc.itemWidthStack.push(window.dc.itemWidth) // Backup current width
        window.dc.itemWidthStack.push(wItemLast)
        for (i in 0 until components - 2)
            window.dc.itemWidthStack.push(wItemOne)
        window.dc.itemWidth = if (components == 1) wItemLast else wItemOne
        g.nextItemData.flags = g.nextItemData.flags wo NextItemDataFlag.HasWidth
    }

    /** Was the last item selection toggled? (after Selectable(), TreeNode() etc. We only returns toggle _event_ in order to handle clipping correctly) */
    val isItemToggledSelection: Boolean
        get() = g.lastItemData.statusFlags has ItemStatusFlag.ToggledSelection

    /** [Internal] Absolute coordinate. Saner. This is not exposed until we finishing refactoring work rect features.
     *  ~GetContentRegionMaxAbs */
    val contentRegionMaxAbs: Vec2
        get() {
            val window = g.currentWindow!!
            val mx = window.contentRegionRect.max
            if (window.dc.currentColumns != null || g.currentTable != null)
                mx.x = window.workRect.max.x
            return mx
        }

    /** Shrink excess width from a set of item, by removing width from the larger items first.
     *  Set items Width to -1.0f to disable shrinking this item. */
    fun shrinkWidths(items: ArrayList<ShrinkWidthItem>, ptr: Int, count: Int, widthExcess_: Float) {
        var widthExcess = widthExcess_
        if (count == 1) {
            if (items[ptr].width >= 0f)
                items[ptr].width = (items[ptr].width - widthExcess) max 1f
            return
        }
        items.subList(ptr, ptr + count).sortWith(shrinkWidthItemComparer)
        var countSameWidth = 1
        while (widthExcess > 0f && countSameWidth < count) {
            while (countSameWidth < count && items[ptr].width <= items[countSameWidth].width)
                countSameWidth++
            val maxWidthToRemovePerItem = when {
                countSameWidth < count && items[ptr + countSameWidth].width >= 0f -> items[ptr].width - items[ptr + countSameWidth].width
                else -> items[ptr].width - 1f
            }
            if (maxWidthToRemovePerItem <= 0f)
                break
            val widthToRemovePerItem = (widthExcess / countSameWidth) min maxWidthToRemovePerItem
            for (itemN in 0 until countSameWidth)
                items[ptr + itemN].width -= widthToRemovePerItem
            widthExcess -= widthToRemovePerItem * countSameWidth
        }

        // Round width and redistribute remainder left-to-right (could make it an option of the function?)
        // Ensure that e.g. the right-most tab of a shrunk tab-bar always reaches exactly at the same distance from the right-most edge of the tab bar separator.
        widthExcess = 0f
        repeat(count) {
            val widthRounded = floor(items[it].width)
            widthExcess += items[it].width - widthRounded
            items[it].width = widthRounded
        }
        if (widthExcess > 0f)
            for (n in 0 until count)
                if (items[n].index < (widthExcess + 0.01f).i)
                    items[n].width += 1f
    }
}