package imgui.internal.api

import glm_.func.common.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeButton
import imgui.ImGui.isMouseClicked
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderTextClippedEx
import imgui.ImGui.renderTextEllipsis
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.classes.TabBar
import imgui.internal.classes.lastItemDataBackup
import imgui.internal.floor
import kotlin.math.max
import kotlin.math.min

internal interface tabBars {

    // the rest of the function is inside the TabBar class

    fun tabItemCalcSize(label: String, hasCloseButton: Boolean): Vec2 {

        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)
        val size = Vec2(labelSize.x + style.framePadding.x, labelSize.y + style.framePadding.y * 2f)
        size.x += style.framePadding.x + when {
            hasCloseButton -> style.itemInnerSpacing.x + g.fontSize // We use Y intentionally to fit the close button circle.
            else -> 1f
        }
        return Vec2(size.x min TabBar.calcMaxTabWidth(), size.y)
    }

    fun tabItemBackground(drawList: DrawList, bb: Rect, flags: TabItemFlags, col: Int) {

        // While rendering tabs, we trim 1 pixel off the top of our bounding box so they can fit within a regular frame height while looking "detached" from it.
        val width = bb.width
        assert(width > 0f)
        val rounding = max(0f, min(if(flags has TabItemFlag._Button) g.style.frameRounding else style.tabRounding, width * 0.5f - 1f))
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
                pathStroke(Col.Border.u32, false, style.tabBorderSize)
            }
        }
    }

    /** Render text label (with custom clipping) + Unsaved Document marker + Close Button logic
     *  We tend to lock style.FramePadding for a given tab-bar, hence the 'frame_padding' parameter.    */
    fun tabItemLabelAndCloseButton(drawList: DrawList, bb: Rect, flags: TabItemFlags, framePadding: Vec2,
                                   label: ByteArray, tabId: ID, closeButtonId: ID, isContentsVisible: Boolean): Boolean {

        val labelSize = calcTextSize(label, 0, hideTextAfterDoubleHash =  true)
        if (bb.width <= 1f)
            return false

        // In Style V2 we'll have full override of all colors per state (e.g. focused, selected)
        // But right now if you want to alter text color of tabs this is what you need to do.
//        #if 0
//        const float backup_alpha = g.Style.Alpha;
//        if (!is_contents_visible)
//            g.Style.Alpha *= 0.7f;
//        #endif

        // In Style V2 we'll have full override of all colors per state (e.g. focused, selected)
        // But right now if you want to alter text color of tabs this is what you need to do.
//        val backupAlpha = style.alpha
//        if(!isContentsVisible)
//            style.alpha *= 0.7f

        // Render text label (with clipping + alpha gradient) + unsaved marker
        val TAB_UNSAVED_MARKER = "*".toByteArray()
        val textPixelClipBb = Rect(bb.min.x + framePadding.x, bb.min.y + framePadding.y, bb.max.x - framePadding.x, bb.max.y)
        if (flags has TabItemFlag.UnsavedDocument) {
            textPixelClipBb.max.x -= calcTextSize(TAB_UNSAVED_MARKER, 0, -1, false).x
            val unsavedMarkerPos = Vec2(min(bb.min.x + framePadding.x + labelSize.x + 2, textPixelClipBb.max.x), bb.min.y + framePadding.y + floor(-g.fontSize * 0.25f))
            renderTextClippedEx(drawList, unsavedMarkerPos, bb.max - framePadding, TAB_UNSAVED_MARKER, 0, null)
        }
        val textEllipsisClipBb = Rect(textPixelClipBb)

        // Close Button
        // We are relying on a subtle and confusing distinction between 'hovered' and 'g.HoveredId' which happens because we are using ImGuiButtonFlags_AllowOverlapMode + SetItemAllowOverlap()
        //  'hovered' will be true when hovering the Tab but NOT when hovering the close button
        //  'g.HoveredId==id' will be true when hovering the Tab including when hovering the close button
        //  'g.ActiveId==close_button_id' will be true when we are holding on the close button, in which case both hovered booleans are false
        var closeButtonPressed = false
        var closeButtonVisible = false
        if (closeButtonId != 0)
            if (isContentsVisible || bb.width >= style.tabMinWidthForCloseButton)
                if (g.hoveredId == tabId || g.hoveredId == closeButtonId || g.activeId == tabId || g.activeId == closeButtonId)
                    closeButtonVisible = true
        if (closeButtonVisible) {
            val closeButtonSz = g.fontSize
            pushStyleVar(StyleVar.FramePadding, framePadding)
            lastItemDataBackup {
                if (closeButton(closeButtonId, Vec2(bb.max.x - framePadding.x * 2f - closeButtonSz, bb.min.y)))
                    closeButtonPressed = true
            }
            popStyleVar()

            // Close with middle mouse button
            if (flags hasnt TabItemFlag.NoCloseWithMiddleMouseButton && isMouseClicked(MouseButton.Middle))
                closeButtonPressed = true

            textPixelClipBb.max.x -= closeButtonSz
        }

        // FIXME: if FramePadding is noticeably large, ellipsis_max_x will be wrong here (e.g. #3497), maybe for consistency that parameter of RenderTextEllipsis() shouldn't exist..
        val ellipsisMaxX = if (closeButtonVisible) textPixelClipBb.max.x else bb.max.x - 1f
        renderTextEllipsis(drawList, textEllipsisClipBb.min, textEllipsisClipBb.max, textPixelClipBb.max.x,
                ellipsisMaxX, label, textSizeIfKnown = labelSize)

//        if(!isContentsVisible)
//            style.alpha = backupAlpha

        return closeButtonPressed
    }
}