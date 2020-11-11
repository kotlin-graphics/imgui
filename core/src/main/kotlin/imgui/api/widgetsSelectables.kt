package imgui.api

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.currentWindow
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.popColumnsBackground
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushColumnsBackground
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setNavID
import imgui.ImGui.style
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.NavHighlightFlag
import imgui.internal.sections.hasnt
import imgui.internal.sections.or
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0
import imgui.SelectableFlag as Sf
import imgui.WindowFlag as Wf
import imgui.internal.sections.ButtonFlag as Bf
import imgui.internal.sections.ItemFlag as If

/** Widgets: Selectables
 *  - A selectable highlights when hovered, and can display another color when selected.
 *  - Neighbors selectable extend their highlight bounds in order to leave no gap between them.
 *      This is so a series of selected Selectable appear contiguous.   */
interface widgetsSelectables {


    /** Tip: pass a non-visible label (e.g. "##hello") then you can use the space to draw other text or image.
     *  But you need to make sure the ID is unique, e.g. enclose calls in PushID/PopID or use ##unique_id.
     *  With this scheme, ImGuiSelectableFlags_SpanAllColumns and ImGuiSelectableFlags_AllowItemOverlap are also frequently used flags.
     *  FIXME: Selectable() with (size.x == 0.0f) and (SelectableTextAlign.x > 0.0f) followed by SameLine() is currently not supported.
     *
     *  "bool selected" carry the selection state (read-only). Selectable() is clicked is returns true so you can modify
     *  your selection state.
     *  size.x == 0f -> use remaining width
     *  size.x > 0f -> specify width
     *  size.y == 0f -> use label height
     *  size.y > 0f -> specify height   */
    fun selectable(label: String, selected_: Boolean = false, flags: SelectableFlags = 0, sizeArg: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val spanAllColumns = flags has Sf.SpanAllColumns
        if (spanAllColumns && window.dc.currentColumns != null)  // FIXME-OPT: Avoid if vertically clipped.
            pushColumnsBackground()

        // Submit label or explicit size to ItemSize(), whereas ItemAdd() will submit a larger/spanning rectangle.
        val id = window.getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)
        val size = Vec2(if (sizeArg.x != 0f) sizeArg.x else labelSize.x, if (sizeArg.y != 0f) sizeArg.y else labelSize.y)
        val pos = Vec2(window.dc.cursorPos)
        pos.y += window.dc.currLineTextBaseOffset
        itemSize(size, 0f)

        // Fill horizontal space
        // We don't support (size < 0.0f) in Selectable() because the ItemSpacing extension would make explicitely right-aligned sizes not visibly match other widgets.
        val minX = if(spanAllColumns) window.parentWorkRect.min.x else pos.x
        val maxX = if(spanAllColumns) window.parentWorkRect.max.x else window.workRect.max.x
        if (sizeArg.x == 0f || flags has Sf._SpanAvailWidth)
            size.x = max(labelSize.x, maxX - minX)

        // Text stays at the submission position, but bounding box may be extended on both sides
        val textMin = Vec2(pos)
        val textMax = Vec2(minX + size.x, pos.y + size.y)

        // Selectables are meant to be tightly packed together with no click-gap, so we extend their box to cover spacing between selectable.
        val bb = Rect(minX, pos.y, textMax.x, textMax.y)
        if (flags hasnt Sf._NoPadWithHalfSpacing) {
            val spacing = style.itemSpacing
            val spacingL = floor(spacing.x * 0.5f)
            val spacingU = floor(spacing.y * 0.5f)
            bb.min.x -= spacingL
            bb.min.y -= spacingU
            bb.max.x += spacing.x - spacingL
            bb.max.y += spacing.y - spacingU
        }
        //if (g.IO.KeyCtrl) { GetForegroundDrawList()->AddRect(bb.Min, bb.Max, IM_COL32(0, 255, 0, 255)); }

        val itemAdd = when {
            flags has Sf.Disabled -> {
                val backupItemFlags = window.dc.itemFlags
                window.dc.itemFlags = window.dc.itemFlags or If.Disabled or If.NoNavDefaultFocus
                itemAdd(bb, id).also {
                    window.dc.itemFlags = backupItemFlags
                }
            }
            else -> itemAdd(bb, id)
        }
        if (!itemAdd) {
            if (spanAllColumns && window.dc.currentColumns != null)
                pushColumnsBackground()
            return false
        }

        // We use NoHoldingActiveID on menus so user can click and _hold_ on a menu then drag to browse child entries
        var buttonFlags = 0
        if (flags has Sf._NoHoldingActiveId) buttonFlags = buttonFlags or Bf.NoHoldingActiveId
        if (flags has Sf._SelectOnClick) buttonFlags = buttonFlags or Bf.PressedOnClick
        if (flags has Sf._SelectOnRelease) buttonFlags = buttonFlags or Bf.PressedOnRelease
        if (flags has Sf.Disabled) buttonFlags = buttonFlags or Bf.Disabled
        if (flags has Sf.AllowDoubleClick) buttonFlags = buttonFlags or Bf.PressedOnClickRelease or Bf.PressedOnDoubleClick
        if (flags has Sf.AllowItemOverlap) buttonFlags = buttonFlags or Bf.AllowItemOverlap

        val selected = if (flags has Sf.Disabled) false else selected_

        val wasSelected = selected

        val (pressed, h, held) = buttonBehavior(bb, id, buttonFlags)
        var hovered = h

        // Update NavId when clicking or when Hovering (this doesn't happen on most widgets), so navigation can be resumed with gamepad/keyboard
        if (pressed || (hovered && flags has Sf._SetNavIdOnHover))
            if (!g.navDisableMouseHover && g.navWindow === window && g.navLayer == window.dc.navLayerCurrent) {
                g.navDisableHighlight = true
                setNavID(id, window.dc.navLayerCurrent, window.dc.navFocusScopeIdCurrent)
            }
        if (pressed)
            markItemEdited(id)

        if (flags has Sf.AllowItemOverlap)
            setItemAllowOverlap()

        // In this branch, Selectable() cannot toggle the selection so this will never trigger.
        if (selected != wasSelected)
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledSelection

        // Render
        if (held && flags has Sf._DrawHoveredWhenHeld)
            hovered = true
        if (hovered || selected) {
            val col = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
            renderFrame(bb.min, bb.max, col.u32, false, 0f)
            renderNavHighlight(bb, id, NavHighlightFlag.TypeThin or NavHighlightFlag.NoRounding)
        }

        if (spanAllColumns && window.dc.currentColumns != null)
            popColumnsBackground()

        if (flags has Sf.Disabled) pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
        renderTextClipped(textMin, textMax, label, labelSize, style.selectableTextAlign, bb)
        if (flags has Sf.Disabled) popStyleColor()

        // Automatically close popups
        if (pressed && window.flags has Wf._Popup && flags hasnt Sf.DontClosePopups && window.dc.itemFlags hasnt If.SelectableDontClosePopup)
            closeCurrentPopup()

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return pressed
    }

    /** "bool* p_selected" point to the selection state (read-write), as a convenient helper.   */
    fun selectable(label: String, selected: BooleanArray, ptr: Int, flags: SelectableFlags = 0, size: Vec2 = Vec2()) =
            withBoolean(selected, ptr) {
                selectable(label, it, flags, size)
            }

    /** "bool* p_selected" point to the selection state (read-write), as a convenient helper.   */
    fun selectable(label: String, selectedPtr: KMutableProperty0<Boolean>, flags: SelectableFlags = 0, size: Vec2 = Vec2()): Boolean {
        var selected by selectedPtr
        return if (selectable(label, selected, flags, size)) {
            selected = !selected
            true
        } else false
    }
}