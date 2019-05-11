package imgui.imgui.widgets

import glm_.vec2.Vec2
import imgui.Col
import imgui.ID
import imgui.ImGui.buttonBehavior
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.markItemEdited
import imgui.ImGui.mouseCursor
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.style
import imgui.MouseCursor
import imgui.getValue
import imgui.imgui.g
import imgui.internal.*
import imgui.setValue
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import glm_.pow as _
import imgui.ColorEditFlag as Cef
import imgui.DragDropFlag as Ddf
import imgui.HoveredFlag as Hf
import imgui.InputTextFlag as Itf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.ColumnsFlag as Cf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.ItemFlag as If
import imgui.internal.LayoutType as Lt
import imgui.internal.SeparatorFlag as Sf

interface lowLevelLayoutHelpers {





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