package imgui.imgui.widgets

import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.Col
import imgui.ImGui.buttonBehavior
import imgui.ImGui.currentWindow
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.logRenderedText
import imgui.ImGui.logText
import imgui.ImGui.markItemEdited
import imgui.ImGui.mouseCursor
import imgui.ImGui.popClipRect
import imgui.ImGui.pushColumnClipRect
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.style
import imgui.internal.*
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import glm_.pow as _
import imgui.ColorEditFlag as Cef
import imgui.DragDropFlag as Ddf
import imgui.HoveredFlag as Hf
import imgui.InputTextFlag as Itf
import imgui.internal.ItemFlag as If
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.ColumnsFlag as Cf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.LayoutType as Lt
import imgui.internal.SeparatorFlag as Sf

interface lowLevelLayoutHelpers {

    /** add vertical spacing.    */
    fun spacing() {
        if (currentWindow.skipItems) return
        itemSize(Vec2())
    }

    /** add a dummy item of given size. unlike InvisibleButton(), Dummy() won't take the mouse click or be navigable into.  */
    fun dummy(size: Vec2) {

        val window = currentWindow
        if (window.skipItems) return

        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(bb)
        itemAdd(bb, 0)
    }

    /** undo a sameLine() or force a new line when in an horizontal-layout context.   */
    fun newLine() {
        val window = currentWindow
        if (window.skipItems) return

        val backupLayoutType = window.dc.layoutType
        window.dc.layoutType = Lt.Vertical
        // In the event that we are on a line with items that is smaller that FontSize high, we will preserve its height.
        itemSize(Vec2(0f, if (window.dc.currentLineSize.y > 0f) 0f else g.fontSize))
        window.dc.layoutType = backupLayoutType
    }

    /** Vertically align/lower upcoming text to framePadding.y so that it will aligns to upcoming widgets
     *  (call if you have text on a line before regular widgets)    */
    fun alignTextToFramePadding() {
        val window = currentWindow
        if (window.skipItems) return
        window.dc.currentLineSize.y = glm.max(window.dc.currentLineSize.y, g.fontSize + style.framePadding.y * 2)
        window.dc.currentLineTextBaseOffset = glm.max(window.dc.currentLineTextBaseOffset, style.framePadding.y)
    }

    /** Horizontal/vertical separating line
     *  Separator, generally horizontal. inside a menu bar or in horizontal layout mode, this becomes a vertical separator. */
    fun separator() {

        val window = currentWindow
        if (window.skipItems) return

        // Those flags should eventually be overridable by the user
        val flag: Sf = if (window.dc.layoutType == Lt.Horizontal) Sf.Vertical else Sf.Horizontal
        // useless on JVM with enums
        // assert((flags and (Sf.Horizontal or Sf.Vertical)).isPowerOfTwo)

        if (flag == Sf.Vertical) {
            verticalSeparator()
            return
        }
        // Horizontal Separator
        window.dc.columnsSet?.let { popClipRect() }

        var x1 = window.pos.x
        val x2 = window.pos.x + window.size.x
        if (window.dc.groupStack.isNotEmpty())
            x1 += window.dc.indent.i

        val bb = Rect(Vec2(x1, window.dc.cursorPos.y), Vec2(x2, window.dc.cursorPos.y + 1f))
        // NB: we don't provide our width so that it doesn't get feed back into AutoFit, we don't provide height to not alter layout.
        itemSize(Vec2())
        if (!itemAdd(bb, 0)) {
            window.dc.columnsSet?.let { pushColumnClipRect() }
            return
        }

        window.drawList.addLine(bb.min, Vec2(bb.max.x, bb.min.y), Col.Separator.u32)

        if (g.logEnabled)
            logRenderedText(bb.min, "--------------------------------")

        window.dc.columnsSet?.let {
            pushColumnClipRect()
            it.lineMinY = window.dc.cursorPos.y
        }
    }

    /** Vertical separator, for menu bars (use current line height). not exposed because it is misleading
     *  what it doesn't have an effect on regular layout.   */
    fun verticalSeparator() {
        val window = currentWindow
        if (window.skipItems) return

        val y1 = window.dc.cursorPos.y
        val y2 = window.dc.cursorPos.y + window.dc.currentLineSize.y
        val bb = Rect(Vec2(window.dc.cursorPos.x, y1), Vec2(window.dc.cursorPos.x + 1f, y2))
        itemSize(Vec2(bb.width, 0f))
        if (!itemAdd(bb, 0)) return

        window.drawList.addLine(Vec2(bb.min), Vec2(bb.min.x, bb.max.y), Col.Separator.u32)
        if (g.logEnabled) logText(" |")
    }

    /** Using 'hover_visibility_delay' allows us to hide the highlight and mouse cursor for a short time, which can be convenient to reduce visual noise. */
    fun splitterBehavior(bb: Rect, id: ID, axis: Axis, size1ptr: KMutableProperty0<Float>, size2ptr: KMutableProperty0<Float>,
                         minSize1: Float, minSize2: Float, hoverExtend: Float = 0f, hoverVisibilityDelay: Float): Boolean {

        var size1 by size1ptr
        var size2 by size2ptr
        val window = g.currentWindow!!

        val itemFlagsBackup = window.dc.itemFlags

        window.dc.itemFlags = window.dc.itemFlags or (If.NoNav or If.NoNavDefaultFocus)

        val itemAdd = itemAdd(bb, id)
        window.dc.itemFlags = itemFlagsBackup
        if (!itemAdd) return false

        val bbInteract = Rect(bb)
        bbInteract expand if (axis == Axis.Y) Vec2(0f, hoverExtend) else Vec2(hoverExtend, 0f)
        val (_, hovered, held) = buttonBehavior(bbInteract, id, Bf.FlattenChildren or Bf.AllowItemOverlap)
        if (g.activeId != id) setItemAllowOverlap()

        if (held || (g.hoveredId == id && g.hoveredIdPreviousFrame == id && g.hoveredIdTimer >= hoverVisibilityDelay))
            mouseCursor = if (axis == Axis.Y) MouseCursor.ResizeNS else MouseCursor.ResizeEW

        val bbRender = Rect(bb)
        if (held) {
            val mouseDelta2d = io.mousePos - g.activeIdClickOffset - bbInteract.min
            var mouseDelta = if (axis == Axis.Y) mouseDelta2d.y else mouseDelta2d.x

            // Minimum pane size
            val size1MaximumDelta = max(0f, size1 - minSize1)
            val size2MaximumDelta = max(0f, size2 - minSize2)
            if (mouseDelta < -size1MaximumDelta)
                mouseDelta = -size1MaximumDelta
            if (mouseDelta > size2MaximumDelta)
                mouseDelta = size2MaximumDelta


            // Apply resize
            if (mouseDelta != 0f) {
                if (mouseDelta < 0f)
                    assert(size1 + mouseDelta >= minSize1)
                else if (mouseDelta > 0f)
                    assert(size2 - mouseDelta >= minSize2)
                size1 = size1 + mouseDelta // cant += because of https://youtrack.jetbrains.com/issue/KT-14833
                size2 = size2 - mouseDelta
                bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)
                markItemEdited(id)
            }
            bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)

            markItemEdited(id)
        }

        // Render
        val col = when {
            held -> Col.SeparatorActive
            hovered && g.hoveredIdTimer >= hoverVisibilityDelay -> Col.SeparatorHovered
            else -> Col.Separator
        }
        window.drawList.addRectFilled(bbRender.min, bbRender.max, col.u32, style.frameRounding)

        return held
    }
}