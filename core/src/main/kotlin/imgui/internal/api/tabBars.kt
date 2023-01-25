package imgui.internal.api

import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeButton
import imgui.ImGui.isMouseClicked
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderTextEllipsis
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.classes.TabBar
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
                pathStroke(Col.Border.u32, 0, style.tabBorderSize)
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
            g.lastItemData = lastItemBackup

            // Close with middle mouse button
            if (flags hasnt TabItemFlag.NoCloseWithMiddleMouseButton && isMouseClicked(MouseButton.Middle))
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