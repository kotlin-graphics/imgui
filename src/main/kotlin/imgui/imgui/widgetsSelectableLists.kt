package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.contentRegionMax
import imgui.ImGui.currentWindow
import imgui.ImGui.getColorU32
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.popClipRect
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderFrame
import imgui.ImGui.renderTextClipped
import imgui.ImGui.windowContentRegionMax
import imgui.internal.ButtonFlags
import imgui.internal.Rect
import imgui.internal.or

/** Widgets: Selectable / Lists */
interface imgui_widgetsSelectableLists {


    /** size.x==0.0: use remaining width, size.x>0.0: specify width. size.y==0.0: use label height, size.y>0.0:
     *  specify height
     *  Tip: pass an empty label (e.g. "##dummy") then you can use the space to draw other text or image.
     *  But you need to make sure the ID is unique, e.g. enclose calls in PushID/PopID. */
    fun selectable(label: String, selected: Boolean = false, flags: Int = 0, sizeArg: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        if (flags has SelectableFlags.SpanAllColumns && window.dc.columnsCount > 1)
            popClipRect()

        val id = window.getId(label)
        val labelSize = calcTextSize(label, true)
        val size = Vec2(if (sizeArg.x != 0f) sizeArg.x else labelSize.x, if (sizeArg.y != 0f) sizeArg.y else labelSize.y)
        val pos = Vec2(window.dc.cursorPos)
        pos.y += window.dc.currentLineTextBaseOffset
        val bb = Rect(pos, pos + size)
        itemSize(bb)

        // Fill horizontal space.
        val windowPadding = Vec2(window.windowPadding)
        val maxX = if (flags has SelectableFlags.SpanAllColumns) windowContentRegionMax.x else contentRegionMax.x
        val wDraw = glm.max(labelSize.x, window.pos.x + maxX - windowPadding.x - window.dc.cursorPos.x)
        val sizeDraw = Vec2(
                if (sizeArg.x != 0f && flags hasnt SelectableFlags.DrawFillAvailWidth) sizeArg.x else wDraw,
                if (sizeArg.y != 0f) sizeArg.y else size.y)
        val bbWithSpacing = Rect(pos, pos + sizeDraw)
        if (sizeArg.x == 0f || flags has SelectableFlags.DrawFillAvailWidth)
            bbWithSpacing.max.x += windowPadding.x

        // Selectables are tightly packed together, we extend the box to cover spacing between selectable.
        val spacingL = (Style.itemSpacing.x * 0.5f).i.f
        val spacingU = (Style.itemSpacing.y * 0.5f).i.f
        val spacingR = Style.itemSpacing.x - spacingL
        val spacingD = Style.itemSpacing.y - spacingU
        bbWithSpacing.min.x -= spacingL
        bbWithSpacing.min.y -= spacingU
        bbWithSpacing.max.x += spacingR
        bbWithSpacing.max.y += spacingD
        if (!itemAdd(bbWithSpacing, id)) {
            if (flags has SelectableFlags.SpanAllColumns && window.dc.columnsCount > 1)
                pushColumnClipRect()
            return false
        }

        var buttonFlags = 0
        if (flags has SelectableFlags.Menu) buttonFlags = buttonFlags or ButtonFlags.PressedOnClick
        if (flags has SelectableFlags.MenuItem) buttonFlags = buttonFlags or ButtonFlags.PressedOnClick or ButtonFlags.PressedOnRelease
        if (flags has SelectableFlags.Disabled) buttonFlags = buttonFlags or ButtonFlags.Disabled
        if (flags has SelectableFlags.AllowDoubleClick) buttonFlags = buttonFlags or ButtonFlags.PressedOnClickRelease or ButtonFlags.PressedOnDoubleClick
        val (pressed, hovered, held) = buttonBehavior(bbWithSpacing, id, buttonFlags)
        var selected = selected
        if (flags has SelectableFlags.Disabled)
            selected = false

        // Render
        if (hovered || selected) {
            val col = getColorU32(if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header)
            renderFrame(bbWithSpacing.min, bbWithSpacing.max, col, false, 0f)
        }

        if (flags has SelectableFlags.SpanAllColumns && window.dc.columnsCount > 1) {
            pushColumnClipRect()
            bbWithSpacing.max.x -= contentRegionMax.x - maxX
        }

        if (flags has SelectableFlags.Disabled) pushStyleColor(Col.Text, Style.colors[Col.TextDisabled])
        renderTextClipped(bb.min, bbWithSpacing.max, label, 0, labelSize, Vec2())
        if (flags has SelectableFlags.Disabled) popStyleColor()

        // Automatically close popups
        if (pressed && flags hasnt SelectableFlags.DontClosePopups && window.flags has WindowFlags.Popup)
            closeCurrentPopup()
        return pressed
    }
//    IMGUI_API bool          Selectable(const char* label, bool* p_selected, ImGuiSelectableFlags flags = 0, const ImVec2& size = ImVec2(0,0));
//    IMGUI_API bool          ListBox(const char* label, int* current_item, const char* const* items, int items_count, int height_in_items = -1);
//    IMGUI_API bool          ListBox(const char* label, int* current_item, bool (*items_getter)(void* data, int idx, const char** out_text), void* data, int items_count, int height_in_items = -1);
//    IMGUI_API bool          ListBoxHeader(const char* label, const ImVec2& size = ImVec2(0,0)); // use if you want to reimplement ListBox() will custom data or interactions. make sure to call ListBoxFooter() afterwards.
//    IMGUI_API bool          ListBoxHeader(const char* label, int items_count, int height_in_items = -1); // "
//    IMGUI_API void          ListBoxFooter();                                                    // terminate the scrolling region
}