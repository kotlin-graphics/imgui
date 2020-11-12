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
        get() = g.currentWindow!!.scroll.x
        /**  ~SetScrollX */
        set(value) = with(currentWindow) { scrollTarget.x = value; scrollTargetCenterRatio.x = 0f }

    /** scrolling amount [0..GetScrollMaxY()] */
    var scrollY: Float
        /** ~GetScrollY */
        get() = g.currentWindow!!.scroll.y
        /**  ~SetScrollY */
        set(value) = with(currentWindow) {
            scrollTarget.y = value
            scrollTargetCenterRatio.y = 0f
        }

    /** get maximum scrolling amount ~~ ContentSize.x - WindowSize.x
     *  ~GetScrollMaxX */
    val scrollMaxX: Float
        get() = currentWindow.scrollMax.x

    /** get maximum scrolling amount ~~ ContentSize.y - WindowSize.y
     *  ~GetScrollMaxY */
    val scrollMaxY: Float
        get() = currentWindow.scrollMax.y

    /** Tweak: snap on edges when aiming at an item very close to the edge,
     *  So the difference between WindowPadding and ItemSpacing will be in the visible area after scrolling.
     *  When we refactor the scrolling API this may be configurable with a flag?
     *  Note that the effect for this won't be visible on X axis with default Style settings as WindowPadding.x == ItemSpacing.x by default. */
    fun calcScrollSnap(target: Float, snapMin: Float, snapMax: Float, snapThreshold: Float, centerRatio: Float): Float = when {
        target <= snapMin + snapThreshold -> lerp(snapMin, target, centerRatio)
        target >= snapMax - snapThreshold -> lerp(target, snapMax, centerRatio)
        else -> target
    }

    /** center_x_ratio: 0.0f left of last item, 0.5f horizontal center of last item, 1.0f right of last item.
     *
     *  adjust scrolling amount to make current cursor position visible. center_x_ratio=0.0: left, 0.5: center, 1.0: right. When using to make a "default/current item" visible, consider using SetItemDefaultFocus() instead. */
    fun setScrollHereX(centerXRatio: Float) {
        val window = g.currentWindow!!
        val spacingX = style.itemSpacing.x
        var targetX = lerp(window.dc.lastItemRect.min.x - spacingX, window.dc.lastItemRect.max.x + spacingX, centerXRatio)

        // Tweak: snap on edges when aiming at an item very close to the edge
        val snapXThreshold = 0f max (window.windowPadding.x - spacingX)
        val snapXMin = window.dc.cursorStartPos.x - window.windowPadding.x
        val snapXMax = window.dc.cursorStartPos.x + window.contentSize.x + window.windowPadding.x
        targetX = calcScrollSnap(targetX, snapXMin, snapXMax, snapXThreshold, centerXRatio)

        window.setScrollFromPosX(targetX - window.pos.x, centerXRatio)
    }

    /** adjust scrolling amount to make current cursor position visible.
     *  centerYRatio = 0.0: top, 0.5: center, 1.0: bottom.
     *   When using to make a "default/current item" visible, consider using setItemDefaultFocus() instead.*/
    fun setScrollHereY(centerYRatio: Float = 0.5f) {
        val window = g.currentWindow!!
        val spacingY = style.itemSpacing.y
        var targetY = lerp(window.dc.cursorPosPrevLine.y - spacingY, window.dc.cursorPosPrevLine.y + window.dc.prevLineSize.y + spacingY, centerYRatio)

        // Tweak: snap on edges when aiming at an item very close to the edge
        val snapYThreshold = 0f max (window.windowPadding.y - spacingY)
        val snapYMin = window.dc.cursorStartPos.y - window.windowPadding.y
        val snapYMax = window.dc.cursorStartPos.y + window.contentSize.y + window.windowPadding.y
        targetY = calcScrollSnap(targetY, snapYMin, snapYMax, snapYThreshold, centerYRatio)

        window.setScrollFromPosY(targetY - window.pos.y, centerYRatio)
    }

    fun setScrollFromPosX(localX: Float, centerXratio: Float) = g.currentWindow!!.setScrollFromPosX(localX, centerXratio)

    fun setScrollFromPosY(localY: Float, centerYratio: Float) = g.currentWindow!!.setScrollFromPosY(localY, centerYratio)
}