package imgui.imgui.widgets

import glm_.f
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.contentRegionMax
import imgui.ImGui.currentWindow
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.popClipRect
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushColumnClipRect
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setNavId
import imgui.ImGui.style
import imgui.ImGui.windowContentRegionMax
import imgui.imgui.withBoolean
import imgui.internal.*
import kotlin.reflect.KMutableProperty0
import imgui.internal.ItemFlag as If
import imgui.SelectableFlag as Sf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf

/** Widgets: Selectables
 *  - A selectable highlights when hovered, and can display another color when selected.
 *  - Neighbors selectable extend their highlight bounds in order to leave no gap between them. */
interface selectableLists {


    /** Tip: pass a non-visible label (e.g. "##dummy") then you can use the space to draw other text or image.
     *  But you need to make sure the ID is unique, e.g. enclose calls in PushID/PopID or use ##unique_id.
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

        if (flags has Sf.SpanAllColumns && window.dc.currentColumns != null)  // FIXME-OPT: Avoid if vertically clipped.
            popClipRect()

        val id = window.getId(label)
        val labelSize = calcTextSize(label, true)
        val size = Vec2(if (sizeArg.x != 0f) sizeArg.x else labelSize.x, if (sizeArg.y != 0f) sizeArg.y else labelSize.y)
        val pos = Vec2(window.dc.cursorPos)
        pos.y += window.dc.currentLineTextBaseOffset
        val bbInner = Rect(pos, pos + size)
        itemSize(size)

        // Fill horizontal space.
        val windowPadding = Vec2(window.windowPadding)
        val maxX = if (flags has Sf.SpanAllColumns) windowContentRegionMax.x else contentRegionMax.x
        val wDraw = labelSize.x max (window.pos.x + maxX - windowPadding.x - pos.x)
        val sizeDraw = Vec2(
                if (sizeArg.x != 0f && flags hasnt Sf.DrawFillAvailWidth) sizeArg.x else wDraw,
                if (sizeArg.y != 0f) sizeArg.y else size.y)
        val bb = Rect(pos, pos + sizeDraw)
        if (sizeArg.x == 0f || flags has Sf.DrawFillAvailWidth)
            bb.max.x += windowPadding.x

        // Selectables are tightly packed together, we extend the box to cover spacing between selectable.
        val spacing = style.itemSpacing
        val spacingL = (spacing.x * 0.5f).i.f
        val spacingU = (spacing.y * 0.5f).i.f
        bb.min.x -= spacingL
        bb.min.y -= spacingU
        bb.max.x += spacing.x - spacingL
        bb.max.y += spacing.y - spacingU

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
            if (flags has Sf.SpanAllColumns && window.dc.currentColumns != null)
                pushColumnClipRect()
            return false
        }

        // We use NoHoldingActiveID on menus so user can click and _hold_ on a menu then drag to browse child entries
        var buttonFlags = 0
        if (flags has Sf.NoHoldingActiveID) buttonFlags = buttonFlags or Bf.NoHoldingActiveID
        if (flags has Sf.PressedOnClick) buttonFlags = buttonFlags or Bf.PressedOnClick
        if (flags has Sf.PressedOnRelease) buttonFlags = buttonFlags or Bf.PressedOnRelease
        if (flags has Sf.Disabled) buttonFlags = buttonFlags or Bf.Disabled
        if (flags has Sf.AllowDoubleClick) buttonFlags = buttonFlags or Bf.PressedOnClickRelease or Bf.PressedOnDoubleClick
        if (flags has Sf.AllowItemOverlap) buttonFlags = buttonFlags or Bf.AllowItemOverlap

        val selected = if (flags has Sf.Disabled) false else selected_

        val wasSelected = selected

        val (pressed, hovered, held) = buttonBehavior(bb, id, buttonFlags)
        /*  Hovering selectable with mouse updates navId accordingly so navigation can be resumed with gamepad/keyboard
            (this doesn't happen on most widgets)         */
        if (pressed || hovered)
            if (!g.navDisableMouseHover && g.navWindow === window && g.navLayer == window.dc.navLayerCurrent) {
                g.navDisableHighlight = true
                setNavId(id, window.dc.navLayerCurrent)
            }
        if (pressed)
            markItemEdited(id)

        if (flags has Sf.AllowItemOverlap)
            setItemAllowOverlap()

        // In this branch, Selectable() cannot toggle the selection so this will never trigger.
        if (selected != wasSelected)
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledSelection

        // Render
        if (hovered || selected) {
            val col = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
            renderFrame(bb.min, bb.max, col.u32, false, 0f)
            renderNavHighlight(bb, id, NavHighlightFlag.TypeThin or NavHighlightFlag.NoRounding)
        }

        if (flags has Sf.SpanAllColumns && window.dc.currentColumns != null) {
            pushColumnClipRect()
            bb.max.x -= contentRegionMax.x - maxX
        }

        if (flags has Sf.Disabled) pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
        renderTextClipped(bbInner.min, bbInner.max, label, 0, labelSize, style.selectableTextAlign, bb)
        if (flags has Sf.Disabled) popStyleColor()

        // Automatically close popups
        if (pressed && window.flags has Wf.Popup && flags hasnt Sf.DontClosePopups && window.dc.itemFlags hasnt If.SelectableDontClosePopup)
            closeCurrentPopup()

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags)
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
        if (selectable(label, selected, flags, size)) {
            selected = !selected
            return true
        }
        return false
    }
}