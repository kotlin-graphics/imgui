package imgui.imgui

import glm_.f
import glm_.i
import imgui.ImGui


/** Windows Scrolling */
interface imgui_windowScrolling {

    /** Scrolling amount [0..GetScrollMaxX()]   */
    var scrollX: Float
        get() = g.currentWindow!!.scroll.x
        set(value) = with(ImGui.currentWindow) { scrollTarget.x = value; scrollTargetCenterRatio.x = 0f }

    /** scrolling amount [0..GetScrollMaxY()]   */
    var scrollY: Float
        get() = g.currentWindow!!.scroll.y
        set(value) = with(ImGui.currentWindow) {
            // title bar height canceled out when using ScrollTargetRelY
            scrollTarget.y = value + titleBarHeight + menuBarHeight
            scrollTargetCenterRatio.y = 0f
        }

    /** get maximum scrolling amount ~~ ContentSize.X - WindowSize.X    */
    val scrollMaxX: Float
        get() = ImGui.currentWindowRead!!.scrollMaxX

    /** get maximum scrolling amount ~~ ContentSize.Y - WindowSize.Y    */
    val scrollMaxY: Float
        get() = ImGui.currentWindowRead!!.scrollMaxY

    /** adjust scrolling amount to make current cursor position visible.
     *  centerYRatio = 0.0: top, 0.5: center, 1.0: bottom.
     *   When using to make a "default/current item" visible, consider using setItemDefaultFocus() instead.*/
    fun setScrollHereY(centerYRatio: Float = 0.5f) = with(ImGui.currentWindow) {
        var targetY = dc.cursorPosPrevLine.y - pos.y  // Top of last item, in window space
        // Precisely aim above, in the middle or below the last line.
        targetY += (dc.prevLineSize.y * centerYRatio) + ImGui.style.itemSpacing.y * (centerYRatio - 0.5f) * 2f
        setScrollFromPosY(targetY, centerYRatio)
    }

    /** adjust scrolling amount to make given position visible. Generally GetCursorStartPos() + offset to compute a valid position.   */
    fun setScrollFromPosY(localY: Float, centerYRatio: Float = 0.5f) = with(ImGui.currentWindow) {
        /*  We store a target position so centering can occur on the next frame when we are guaranteed to have a known
            window size         */
        assert(centerYRatio in 0f..1f)
        scrollTarget.y = (localY + scroll.y).i.f
        scrollTargetCenterRatio.y = centerYRatio
    }
}