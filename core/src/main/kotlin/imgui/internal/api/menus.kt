package imgui.internal.api

import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.beginDisabled
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderText
import imgui.ImGui.selectable
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.floor
import imgui.internal.sections.IMGUI_TEST_ENGINE_ITEM_INFO
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.LayoutType
import imgui.internal.sections.or

// Menus
internal interface menus {

    /** return true when activated. */
    fun menuItemEx(label: String, icon: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true): Boolean {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val pos = Vec2(window.dc.cursorPos)
        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)

        // We've been using the equivalent of ImGuiSelectableFlags_SetNavIdOnHover on all Selectable() since early Nav system days (commit 43ee5d73),
        // but I am unsure whether this should be kept at all. For now moved it to be an opt-in feature used by menus only.
        val pressed: Boolean
        pushID(label)
        if (!enabled)
            beginDisabled(true)
        val flags = SelectableFlag._SelectOnRelease or SelectableFlag._SetNavIdOnHover
        val offsets = window.dc.menuColumns
        if (window.dc.layoutType == LayoutType.Horizontal) {
            // Mimic the exact layout spacing of BeginMenu() to allow MenuItem() inside a menu bar, which is a little misleading but may be useful
            // Note that in this situation: we don't render the shortcut, we render a highlight instead of the selected tick mark.
            val w = labelSize.x
            window.dc.cursorPos.x += floor(style.itemSpacing.x * 0.5f)
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            pressed = selectable("", selected, flags, Vec2(w, 0f))
            popStyleVar()
            renderText(pos + Vec2(offsets.offsetLabel, 0f), label)
            window.dc.cursorPos.x += floor(style.itemSpacing.x * (-1f + 0.5f)) // -1 spacing to compensate the spacing added when Selectable() did a SameLine(). It would also work to call SameLine() ourselves after the PopStyleVar().
        } else {
            // Menu item inside a vertical menu
            // (In a typical menu window where all items are BeginMenu() or MenuItem() calls, extra_w will always be 0.0f.
            //  Only when they are other items sticking out we're going to add spacing, yet only register minimum width into the layout system.
            val iconW = if(icon.isNotEmpty()) calcTextSize(icon).x else 0f
            val shortcutW = if(shortcut.isNotEmpty()) calcTextSize(shortcut).x else 0f
            val checkmarkW = floor(g.fontSize * 1.2f)
            val minW = window.dc.menuColumns.declColumns(iconW, labelSize.x, shortcutW, checkmarkW) // Feedback for next frame
            val stretchW = 0f max (contentRegionAvail.x - minW)
            pressed = selectable("", false, flags or SelectableFlag._SpanAvailWidth, Vec2(minW, 0f))
            renderText(pos + Vec2(offsets.offsetLabel, 0f), label)
            if (iconW > 0f)
                renderText(pos + Vec2(offsets.offsetIcon, 0f), icon)
            if (shortcutW > 0f) {
                pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
                renderText(pos + Vec2(offsets.offsetShortcut + stretchW, 0f), shortcut, false)
                popStyleColor()
            }
            if (selected)
                window.drawList.renderCheckMark(pos + Vec2(offsets.offsetMark + stretchW + g.fontSize * 0.4f, g.fontSize * 0.134f * 0.5f),
                                                Col.Text.u32, g.fontSize * 0.866f)
        }
        IMGUI_TEST_ENGINE_ITEM_INFO(g.lastItemData.id, label, g.lastItemData.statusFlags or ItemStatusFlag.Checkable or if (selected) ItemStatusFlag.Checked else ItemStatusFlag.None)
        if (!enabled)
            popStyleColor()
        popID()

        return pressed
    }
}