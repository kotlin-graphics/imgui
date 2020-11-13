package imgui.api

import glm_.max
import glm_.vec2.Vec2
import imgui.ImGui.currentWindow
import imgui.ImGui.style
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.or
import imgui.lerp

/** Windows Scrolling */
interface windowScrolling {

    /** Use -1.0f on one axis to leave as-is */
    fun setNextWindowScroll(scroll: Vec2) {
        g.nextWindowData.flags = g.nextWindowData.flags or NextWindowDataFlag.HasScroll
        g.nextWindowData.scrollVal put scroll
    }

    /** Scrolling amount [0..GetScrollMaxX()] */
    var scrollX: Float
        /** ~GetScrollX */
        get() = currentWindow.scroll.x
        /**  ~SetScrollX */
        set(value) = currentWindow.setScrollX(value)

    /** scrolling amount [0..GetScrollMaxY()] */
    var scrollY: Float
        /** ~GetScrollY */
        get() = currentWindow.scroll.y
        /**  ~SetScrollY */
        set(value) = currentWindow.setScrollY(value)

    /** get maximum scrolling amount ~~ ContentSize.x - WindowSize.x
     *  ~GetScrollMaxX */
    val scrollMaxX: Float
        get() = currentWindow.scrollMax.x

    /** get maximum scrolling amount ~~ ContentSize.y - WindowSize.y
     *  ~GetScrollMaxY */
    val scrollMaxY: Float
        get() = currentWindow.scrollMax.y

    /** center_x_ratio: 0.0f left of last item, 0.5f horizontal center of last item, 1.0f right of last item.
     *
     *  adjust scrolling amount to make current cursor position visible. center_x_ratio=0.0: left, 0.5: center, 1.0: right. When using to make a "default/current item" visible, consider using SetItemDefaultFocus() instead. */
    fun setScrollHereX(centerXRatio: Float) {
        val window = g.currentWindow!!
        val spacingX = style.itemSpacing.x
        val targetPosX =
            lerp(window.dc.lastItemRect.min.x - spacingX, window.dc.lastItemRect.max.x + spacingX, centerXRatio)
        window.setScrollFromPosX(targetPosX - window.pos.x, centerXRatio) // Convert from absolute to local pos

        // Tweak: snap on edges when aiming at an item very close to the edge
        window.scrollTargetEdgeSnapDist.x = 0f max (window.windowPadding.x - spacingX)
    }

    /** adjust scrolling amount to make current cursor position visible.
     *  centerYRatio = 0.0: top, 0.5: center, 1.0: bottom.
     *   When using to make a "default/current item" visible, consider using setItemDefaultFocus() instead.*/
    fun setScrollHereY(centerYRatio: Float = 0.5f) {
        val window = g.currentWindow!!
        val spacingY = style.itemSpacing.y
        val targetPosY = lerp(window.dc.cursorPosPrevLine.y - spacingY,
            window.dc.cursorPosPrevLine.y + window.dc.prevLineSize.y + spacingY,
            centerYRatio)
        window.setScrollFromPosY(targetPosY - window.pos.y, centerYRatio) // Convert from absolute to local pos

        // Tweak: snap on edges when aiming at an item very close to the edge
        window.scrollTargetEdgeSnapDist.y = 0f max (window.windowPadding.y - spacingY)
    }

    fun setScrollFromPosX(localX: Float, centerXratio: Float) =
        g.currentWindow!!.setScrollFromPosX(localX, centerXratio)

    fun setScrollFromPosY(localY: Float, centerYratio: Float) =
        g.currentWindow!!.setScrollFromPosY(localY, centerYratio)
}