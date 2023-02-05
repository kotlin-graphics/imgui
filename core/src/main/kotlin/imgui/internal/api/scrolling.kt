package imgui.internal.api

import glm_.has
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.WindowFlag
import imgui.api.g
import imgui.has
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.isPowerOfTwo
import imgui.internal.sections.*
import imgui.static.calcNextScrollFromScrollTargetAndClamp

// Scrolling
internal interface scrolling {

    /** ~SetScrollX(ImGuiWindow* window, float new_scroll_x) */
    infix fun Window.setScrollX(scrollX: Float) {
        scrollTarget.x = scrollX
        scrollTargetCenterRatio.x = 0f
        scrollTargetEdgeSnapDist.x = 0.0f
    }

    /** ~SetScrollY(ImGuiWindow* window, float new_scroll_y) */
    infix fun Window.setScrollY(scrollY: Float) {
        scrollTarget.y = scrollY
        scrollTargetCenterRatio.y = 0f
        scrollTargetEdgeSnapDist.y = 0f
    }

    /** adjust scrolling amount to make given position visible. Generally GetCursorStartPos() + offset to compute a valid position.
     *
     *  Note that a local position will vary depending on initial scroll value,
     *  This is a little bit confusing so bear with us:
     *   - local_pos = (absolution_pos - window->Pos)
     *   - So local_x/local_y are 0.0f for a position at the upper-left corner of a window,
     *     and generally local_x/local_y are >(padding+decoration) && <(size-padding-decoration) when in the visible area.
     *   - They mostly exist because of legacy API.
     *  Following the rules above, when trying to work with scrolling code, consider that:
     *   - SetScrollFromPosY(0.0f) == SetScrollY(0.0f + scroll.y) == has no effect!
     *   - SetScrollFromPosY(-scroll.y) == SetScrollY(-scroll.y + scroll.y) == SetScrollY(0.0f) == reset scroll. Of course writing SetScrollY(0.0f) directly then makes more sense
     *  We store a target position so centering and clamping can occur on the next frame when we are guaranteed to have a known window size
     *
     *  ~SetScrollFromPosX(ImGuiWindow* window, float local_x, float center_x_ratio) */
    fun Window.setScrollFromPosX(localX: Float, centerXRatio: Float) {
        assert(centerXRatio in 0f..1f)
        scrollTarget.x = floor(localX + scroll.x) // Convert local position to scroll offset
        scrollTargetCenterRatio.x = centerXRatio
        scrollTargetEdgeSnapDist.x = 0f
    }

    /** adjust scrolling amount to make given position visible. Generally GetCursorStartPos() + offset to compute a valid position.   */
    fun Window.setScrollFromPosY(localY_: Float, centerYRatio: Float) {
        assert(centerYRatio in 0f..1f)
        val decorationUpHeight = titleBarHeight + menuBarHeight // FIXME: Would be nice to have a more standardized access to our scrollable/client rect;
        val localY = localY_ - decorationUpHeight
        scrollTarget.y = floor(localY + scroll.y) // Convert local position to scroll offset
        scrollTargetCenterRatio.y = centerYRatio
        scrollTargetEdgeSnapDist.y = 0f
    }

    // Early work-in-progress API (ScrollToItem() will become public)
    fun scrollToItem(flags: ScrollFlags = ScrollFlag.None.i) {
        val window = g.currentWindow!!
        scrollToRectEx(window, g.lastItemData.navRect, flags)
    }

    fun scrollToRect(window: Window, rect: Rect, flags: ScrollFlags = ScrollFlag.None.i): Vec2 = scrollToRectEx(window, rect, flags)

    fun scrollToRectEx(window: Window, itemRect: Rect, flags_: ScrollFlags = ScrollFlag.None.i): Vec2 {

        var flags = flags_

        val scrollRect = Rect(window.innerRect.min - 1, window.innerRect.max + 1)
        //GetForegroundDrawList(window)->AddRect(item_rect.Min, item_rect.Max, IM_COL32(255,0,0,255), 0.0f, 0, 5.0f); // [DEBUG]
        //GetForegroundDrawList(window)->AddRect(scroll_rect.Min, scroll_rect.Max, IM_COL32_WHITE); // [DEBUG]

        // Check that only one behavior is selected per axis
        assert(flags hasnt ScrollFlag.MaskX_ || (flags and ScrollFlag.MaskX_).isPowerOfTwo)
        assert(flags hasnt ScrollFlag.MaskY_ || (flags and ScrollFlag.MaskY_).isPowerOfTwo)

        // Defaults
        var inFlags = flags
        if (flags hasnt ScrollFlag.MaskX_ && window.scrollbar.x)
            flags /= ScrollFlag.KeepVisibleEdgeX
        if (flags hasnt ScrollFlag.MaskY_)
            flags /= if (window.appearing) ScrollFlag.AlwaysCenterY else ScrollFlag.KeepVisibleEdgeY

        val fullyVisibleX = itemRect.min.x >= scrollRect.min.x && itemRect.max.x <= scrollRect.max.x
        val fullyVisibleY = itemRect.min.y >= scrollRect.min.y && itemRect.max.y <= scrollRect.max.y
        val canBeFullyVisibleX = (itemRect.width + g.style.itemSpacing.x * 2f) <= scrollRect.width || window.autoFitFrames.x > 0 || window.flags has WindowFlag.AlwaysAutoResize
        val canBeFullyVisibleY = (itemRect.height + g.style.itemSpacing.y * 2f) <= scrollRect.height || window.autoFitFrames.y > 0 || window.flags has WindowFlag.AlwaysAutoResize

        if (flags has ScrollFlag.KeepVisibleEdgeX && !fullyVisibleX)
            if (canBeFullyVisibleX)
                window.setScrollFromPosX(floor((itemRect.min.x + itemRect.max.y) * 0.5f) - window.pos.x, 0.5f)
            else
                window.setScrollFromPosX(itemRect.min.x - window.pos.x, 0f)
        else if ((flags has ScrollFlag.KeepVisibleCenterX && !fullyVisibleX) || flags has ScrollFlag.AlwaysCenterX) {
            val targetX = if (canBeFullyVisibleX) floor((itemRect.min.x + itemRect.max.x - window.innerRect.width) * 0.5f) else itemRect.min.x
            window.setScrollFromPosX(targetX - window.pos.x, 0f)
        }

        if (flags has ScrollFlag.KeepVisibleEdgeY && !fullyVisibleY) {
            if (itemRect.min.y < scrollRect.min.y || !canBeFullyVisibleY)
                window.setScrollFromPosY(itemRect.min.y - g.style.itemSpacing.y - window.pos.y, 0f)
            else if (itemRect.max.y >= scrollRect.max.y)
                window.setScrollFromPosY(itemRect.max.y + g.style.itemSpacing.y - window.pos.y, 1f)
        } else if ((flags has ScrollFlag.KeepVisibleCenterY && !fullyVisibleY) || flags has ScrollFlag.AlwaysCenterY)
            if (canBeFullyVisibleY)
                window.setScrollFromPosY(floor((itemRect.min.y + itemRect.max.y) * 0.5f) - window.pos.y, 0.5f)
            else
                window.setScrollFromPosY(itemRect.min.y - window.pos.y, 0f)

        val nextScroll = window.calcNextScrollFromScrollTargetAndClamp()
        val deltaScroll = nextScroll - window.scroll

        // Also scroll parent window to keep us into view if necessary
        if (flags hasnt ScrollFlag.NoScrollParent && window.flags has WindowFlag._ChildWindow) {
            // FIXME-SCROLL: May be an option?
            if (inFlags has (ScrollFlag.AlwaysCenterX or ScrollFlag.KeepVisibleCenterX))
                inFlags = (inFlags wo ScrollFlag.MaskX_) or ScrollFlag.KeepVisibleEdgeX
            if (inFlags has (ScrollFlag.AlwaysCenterY or ScrollFlag.KeepVisibleCenterY))
                inFlags = (inFlags wo ScrollFlag.MaskY_) or ScrollFlag.KeepVisibleEdgeY
            deltaScroll += scrollToRectEx(window.parentWindow!!, Rect(itemRect.min - deltaScroll, itemRect.max - deltaScroll), inFlags)
        }

        return deltaScroll
    }
    //#ifndef IMGUI_DISABLE_OBSOLETE_FUNCTIONS
    /** Scroll to keep newly navigated item fully into view */
    infix fun Window.scrollToBringRectIntoView(itemRect: Rect): Vec2 {
        val windowRect = Rect(innerRect.min - 1,
                              innerRect.max + 1) //GetOverlayDrawList(window)->AddRect(window->Pos + window_rect_rel.Min, window->Pos + window_rect_rel.Max, IM_COL32_WHITE); // [DEBUG]

        val deltaScroll = Vec2()
        if (itemRect !in windowRect) {
            if (scrollbar.x && itemRect.min.x < windowRect.min.x) setScrollFromPosX(itemRect.min.x - pos.x - ImGui.style.itemSpacing.x,
                                                                                    0f)
            else if (scrollbar.x && itemRect.max.x >= windowRect.max.x) setScrollFromPosX(itemRect.max.x - pos.x + ImGui.style.itemSpacing.x,
                                                                                          1f)
            if (itemRect.min.y < windowRect.min.y) setScrollFromPosY(itemRect.min.y - pos.y - ImGui.style.itemSpacing.y, 0f)
            else if (itemRect.max.y >= windowRect.max.y) setScrollFromPosY(itemRect.max.y - pos.y + ImGui.style.itemSpacing.y,
                                                                           1f)

            val nextScroll = calcNextScrollFromScrollTargetAndClamp()
            deltaScroll put (nextScroll - scroll)
        }

        // Also scroll parent window to keep us into view if necessary
        if (flags has WindowFlag._ChildWindow) deltaScroll += parentWindow!! scrollToBringRectIntoView Rect(itemRect.min - deltaScroll,
                                                                                                            itemRect.max - deltaScroll)

        return deltaScroll
    }
    //#endif
}