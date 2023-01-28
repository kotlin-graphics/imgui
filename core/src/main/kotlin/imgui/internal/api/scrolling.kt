package imgui.internal.api

import glm_.has
import glm_.vec2.Vec2
import imgui.WindowFlag
import imgui.api.g
import imgui.has
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.isPowerOfTwo
import imgui.internal.sections.*

// Scrolling
internal interface scrolling {

    /** Use -1.0f on one axis to leave as-is */
    fun setNextWindowScroll(scroll: Vec2) {
        g.nextWindowData.flags = g.nextWindowData.flags or NextWindowDataFlag.HasScroll
        g.nextWindowData.scrollVal put scroll
    }

    // all in `Window` class
    //    IMGUI_API void          SetScrollX(ImGuiWindow* window, float scroll_x);
    //    IMGUI_API void          SetScrollY(ImGuiWindow* window, float scroll_y);
    //    IMGUI_API void          SetScrollFromPosX(ImGuiWindow* window, float local_x, float center_x_ratio);
    //    IMGUI_API void          SetScrollFromPosY(ImGuiWindow* window, float local_y, float center_y_ratio);

    // Early work-in-progress API (ScrollToItem() will become public)
    fun scrollToItem(flags: ScrollFlags = ScrollFlag.None.i) {
        val window = g.currentWindow!!
        scrollToRectEx(window, g.lastItemData.navRect, flags)
    }

    fun scrollToRect(window: Window, rect: Rect, flags: ScrollFlags = ScrollFlag.None.i): Vec2 = scrollToRectEx(window, rect, flags)

    fun scrollToRectEx(window: Window, itemRect: Rect, flags_: ScrollFlags = ScrollFlag.None.i): Vec2 {

        var flags = flags_

        val windowRect = Rect(window.innerRect.min - 1, window.innerRect.max + 1)
        //GetForegroundDrawList(window)->AddRect(window_rect.Min, window_rect.Max, IM_COL32_WHITE); // [DEBUG]

        // Check that only one behavior is selected per axis
        assert(flags hasnt ScrollFlag.MaskX_ || (flags and ScrollFlag.MaskX_).isPowerOfTwo)
        assert(flags hasnt ScrollFlag.MaskY_ || (flags and ScrollFlag.MaskY_).isPowerOfTwo)

        // Defaults
        var inFlags = flags
        if (flags hasnt ScrollFlag.MaskX_ && window.scrollbar.x)
            flags /= ScrollFlag.KeepVisibleEdgeX
        if (flags hasnt ScrollFlag.MaskY_)
            flags /= if (window.appearing) ScrollFlag.AlwaysCenterY else ScrollFlag.KeepVisibleEdgeY

        val fullyVisibleX = itemRect.min.x >= windowRect.min.x && itemRect.max.x <= windowRect.max.x
        val fullyVisibleY = itemRect.min.y >= windowRect.min.y && itemRect.max.y <= windowRect.max.y
        val canBeFullyVisibleX = (itemRect.width + g.style.itemSpacing.x * 2f) <= windowRect.width
        val canBeFullyVisibleY = (itemRect.height + g.style.itemSpacing.y * 2f) <= windowRect.height

        if (flags has ScrollFlag.KeepVisibleEdgeX && !fullyVisibleX) {
            if (itemRect.min.x < windowRect.min.x || !canBeFullyVisibleX)
                window.setScrollFromPosX(itemRect.min.x - g.style.itemSpacing.x - window.pos.x, 0f)
            else if (itemRect.max.x >= windowRect.max.x)
                window.setScrollFromPosX(itemRect.max.x + g.style.itemSpacing.x - window.pos.x, 1f)
        } else if ((flags has ScrollFlag.KeepVisibleCenterX && !fullyVisibleX) || flags has ScrollFlag.AlwaysCenterX) {
            val targetX = if (canBeFullyVisibleX) floor((itemRect.min.x + itemRect.max.x - window.innerRect.width) * 0.5f) else itemRect.min.x
            window.setScrollFromPosX(targetX - window.pos.x, 0f)
        }

        if (flags has ScrollFlag.KeepVisibleEdgeY && !fullyVisibleY) {
            if (itemRect.min.y < windowRect.min.y || !canBeFullyVisibleY)
                window.setScrollFromPosY(itemRect.min.y - g.style.itemSpacing.y - window.pos.y, 0f)
            else if (itemRect.max.y >= windowRect.max.y)
                window.setScrollFromPosY(itemRect.max.y + g.style.itemSpacing.y - window.pos.y, 1f)
        } else if ((flags has ScrollFlag.KeepVisibleCenterY && !fullyVisibleY) || flags has ScrollFlag.AlwaysCenterY) {
            val targetY = if (canBeFullyVisibleY) floor((itemRect.min.y + itemRect.max.y - window.innerRect.height) * 0.5f) else itemRect.min.y
            window.setScrollFromPosY(targetY - window.pos.y, 0f)
        }

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
    //    fun scrollToBringRectIntoView(ImGuiWindow* window, const ImRect& rect) { ScrollToRect(window, rect, ImGuiScrollFlags_KeepVisibleEdgeY); }
    //#endif
}