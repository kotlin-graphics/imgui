package imgui.api

import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginComboPopup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.endPopup
import imgui.ImGui.frameHeight
import imgui.ImGui.isPopupOpen
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.logSetNextTextDecoration
import imgui.ImGui.markItemEdited
import imgui.ImGui.openPopupEx
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.selectable
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.style
import imgui.classes.SizeCallbackData
import imgui.has
import imgui.hasnt
import imgui.internal.classes.Rect
import imgui.internal.hashStr
import imgui.internal.sections.*
import kool.getValue
import kool.setValue
import uno.kotlin.NUL
import kotlin.reflect.KMutableProperty0
import imgui.ComboFlag as Cf

// Widgets: Combo Box (Dropdown)
// - The BeginCombo()/EndCombo() api allows you to manage your contents and selection state however you want it, by creating e.g. Selectable() items.
// - The old Combo() api are helpers over BeginCombo()/EndCombo() which are kept available for convenience purpose. This is analogous to how ListBox are created.
interface widgetsComboBox {

    fun beginCombo(label: String, previewValue_: String?, flags_: ComboFlags = 0): Boolean {

        var previewValue = previewValue_
        var flags = flags_

        val window = currentWindow

        val backupNextWindowDataFlags = g.nextWindowData.flags
        g.nextWindowData.clearFlags() // We behave like Begin() and need to consume those values
        if (window.skipItems)
            return false

        val id = window.getID(label)
        assert(flags and (Cf.NoArrowButton or Cf.NoPreview) != Cf.NoArrowButton or Cf.NoPreview) { "Can't use both flags together" }

        val arrowSize = if (flags has Cf.NoArrowButton) 0f else frameHeight
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)
        val w = if (flags has Cf.NoPreview) arrowSize else calcItemWidth()
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(bb.min, bb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id, bb))
            return false

        // Open on click
        val (pressed, hovered, _) = buttonBehavior(bb, id)
        val popupId = hashStr("##ComboPopup", 0, id)
        var popupOpen = isPopupOpen(popupId, PopupFlag.None.i)
        if (pressed && !popupOpen) {
            openPopupEx(popupId, PopupFlag.None.i)
            popupOpen = true
        }

        // Render shape
        val frameCol = if (hovered) Col.FrameBgHovered else Col.FrameBg
        val valueX2 = bb.min.x max (bb.max.x - arrowSize)
        renderNavHighlight(bb, id)
        if (flags hasnt Cf.NoPreview)
            window.drawList.addRectFilled(bb.min, Vec2(valueX2, bb.max.y), frameCol.u32,
                                          style.frameRounding, if (flags has Cf.NoArrowButton) DrawFlag.RoundCornersAll.i else DrawFlag.RoundCornersLeft.i)
        if (flags hasnt Cf.NoArrowButton) {
            val bgCol = if (popupOpen || hovered) Col.ButtonHovered else Col.Button
            window.drawList.addRectFilled(Vec2(valueX2, bb.min.y), bb.max, bgCol.u32, style.frameRounding, if (w <= arrowSize) DrawFlag.RoundCornersAll.i else DrawFlag.RoundCornersRight.i)
            if (valueX2 + arrowSize - style.framePadding.x <= bb.max.x)
                window.drawList.renderArrow(Vec2(valueX2 + style.framePadding.y, bb.min.y + style.framePadding.y), Col.Text.u32, Dir.Down, 1f)
        }
        renderFrameBorder(bb.min, bb.max, style.frameRounding)

        // Custom preview
        if (flags has Cf._CustomPreview) {
            g.comboPreviewData.previewRect.put(bb.min.x, bb.min.y, valueX2, bb.max.y)
            assert(previewValue == null || previewValue[0] == NUL)
            previewValue = null
        }

        // Render preview and label
        if (previewValue != null && flags hasnt Cf.NoPreview) {
            if (g.logEnabled)
                logSetNextTextDecoration("{", "}")
            renderTextClipped(bb.min + style.framePadding, Vec2(valueX2, bb.max.y), previewValue)
        }
        if (labelSize.x > 0)
            renderText(Vec2(bb.max.x + style.itemInnerSpacing.x, bb.min.y + style.framePadding.y), label)

        if (!popupOpen)
            return false

        g.nextWindowData.flags = backupNextWindowDataFlags
        return beginComboPopup(popupId, bb, flags)
    }

    /** Only call EndCombo() if BeginCombo() returns true! */
    fun endCombo() = endPopup()

    /** Combo box helper allowing to pass an array of strings.  */
    fun combo(label: String, currentItem: KMutableProperty0<Int>, items: Array<String>, heightInItems: Int = -1): Boolean =
        combo(label, currentItem, items.toList(), heightInItems)

    /** Combo box helper allowing to pass all items in a single string literal holding multiple zero-terminated items "item1\0item2\0" */
    fun combo(label: String, currentItem: IntArray, itemsSeparatedByZeros: String, heightInItems: Int = -1): Boolean {
        _i = currentItem[0]
        val items = itemsSeparatedByZeros.split(NUL).filter { it.isNotEmpty() }
        // FIXME-OPT: Avoid computing this, or at least only when combo is open
        val res = combo(label, ::_i, items, heightInItems)
        currentItem[0] = _i
        return res
    }

    fun combo(label: String, currentItem: KMutableProperty0<Int>, itemsSeparatedByZeros: String, heightInItems: Int = -1): Boolean {
        val items = itemsSeparatedByZeros.split(NUL).filter { it.isNotEmpty() }
        // FIXME-OPT: Avoid computing this, or at least only when combo is open
        return combo(label, currentItem, items, heightInItems)
    }

    /** Combo box function. */
    fun combo(label: String, currentItem: IntArray, items: List<String>, popupMaxHeightInItem: Int = -1): Boolean {
        _i = currentItem[0]
        val res = combo(label, ::_i, items, popupMaxHeightInItem)
        currentItem[0] = _i
        return res
    }

    fun combo(label: String, currentItemPtr: KMutableProperty0<Int>, items: List<String>, popupMaxHeightInItem: Int = -1): Boolean {

        var currentItem by currentItemPtr
        val previewValue = items.getOrElse(currentItem) { "" }

        /*  The old Combo() API exposed "popup_max_height_in_items". The new more general BeginCombo() API doesn't,
            have/need it, but we emulate it here.         */
        if (popupMaxHeightInItem != -1 && g.nextWindowData.flags hasnt NextWindowDataFlag.HasSizeConstraint)
            setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE, calcMaxPopupHeightFromItemCount(popupMaxHeightInItem)))

        if (!beginCombo(label, previewValue, Cf.None.i)) return false

        // Display items
        // FIXME-OPT: Use clipper (but we need to disable it on the appearing frame to make sure our call to setItemDefaultFocus() is processed)
        var valueChanged = false
        for (i in items.indices) {
            pushID(i)
            val itemSelected = i == currentItem
            val itemText = items.getOrElse(i) { "*Unknown item*" }
            if (selectable(itemText, itemSelected)) {
                valueChanged = true
                currentItem = i
            }
            if (itemSelected) setItemDefaultFocus()
            popID()
        }
        endCombo()
        return valueChanged
    }

    /** Old API, prefer using BeginCombo() nowadays if you can. */
    fun combo(label: String, pCurrentItem: KMutableProperty0<Int>, itemsGetter: (Array<String>, Int, KMutableProperty0<String>) -> Boolean,
              items: Array<String>, popupMaxHeightInItems: Int = -1): Boolean {

        var currentItem by pCurrentItem
        // Call the getter to obtain the preview string which is a parameter to BeginCombo()
        var previewValue by ::_s
        if (currentItem >= 0 && currentItem < items.size)
            itemsGetter(items, currentItem, ::_s)

        // The old Combo() API exposed "popup_max_height_in_items". The new more general BeginCombo() API doesn't have/need it, but we emulate it here.
        if (popupMaxHeightInItems != -1 && g.nextWindowData.flags hasnt NextWindowDataFlag.HasSizeConstraint)
            setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE, calcMaxPopupHeightFromItemCount(popupMaxHeightInItems)))

        if (!beginCombo(label, previewValue, Cf.None.i))
            return false

        // Display items
        // FIXME-OPT: Use clipper (but we need to disable it on the appearing frame to make sure our call to SetItemDefaultFocus() is processed)
        var valueChanged = false
        for (i in items.indices) {
            pushID(i)
            val itemSelected = i == currentItem
            var itemText by ::_s
            if (!itemsGetter(items, i, ::_s))
                itemText = "*Unknown item*"
            if (selectable(itemText, itemSelected)) {
                valueChanged = true
                currentItem = i
            }
            if (itemSelected)
                setItemDefaultFocus()
            popID()
        }

        endCombo()

        if (valueChanged)
            markItemEdited(g.lastItemData.id)

        return valueChanged
    }

    companion object {
        fun calcMaxPopupHeightFromItemCount(itemsCount: Int) = when {
            itemsCount <= 0 -> Float.MAX_VALUE
            else -> (g.fontSize + style.itemSpacing.y) * itemsCount - style.itemSpacing.y + style.windowPadding.y * 2
        }

        fun sizeCallback(data: SizeCallbackData) {
            val totalWMinusArrow = data.userData as Float
            data.desiredSize.put(totalWMinusArrow, 200f)
        }
    }
}