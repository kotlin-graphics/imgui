package imgui.api

import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorPos
import imgui.ImGui.endChild
import imgui.ImGui.endPopup
import imgui.ImGui.findBestWindowPosForPopupEx
import imgui.ImGui.findWindowByName
import imgui.ImGui.frameHeight
import imgui.ImGui.inputTextEx
import imgui.ImGui.isPopupOpen
import imgui.ImGui.isWindowAppearing
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.openPopupEx
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.selectable
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.setScrollHereY
import imgui.ImGui.style
import imgui.ImGui.windowWidth
import imgui.classes.SizeCallbackData
import imgui.has
import imgui.hasnt
import imgui.internal.classes.Rect
import imgui.internal.isPowerOfTwo
import imgui.internal.sections.*
import kool.getValue
import kool.setValue
import uno.kotlin.NUL
import kotlin.reflect.KMutableProperty0
import imgui.ComboFlag as Cf
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf

// Widgets: Combo Box
// - The BeginCombo()/EndCombo() api allows you to manage your contents and selection state however you want it, by creating e.g. Selectable() items.
// - The old Combo() api are helpers over BeginCombo()/EndCombo() which are kept available for convenience purpose. This is analogous to how ListBox are created.
interface widgetsComboBox {

    fun beginCombo(label: String, previewValue: String?, flags_: ComboFlags = 0): Boolean {

        var flags = flags_

        // Always consume the SetNextWindowSizeConstraint() call in our early return paths
        val hasWindowSizeConstraint = g.nextWindowData.flags has NextWindowDataFlag.HasSizeConstraint
        g.nextWindowData.flags = g.nextWindowData.flags wo NextWindowDataFlag.HasSizeConstraint

        val window = currentWindow
        if (window.skipItems) return false

        assert((flags and (Cf.NoArrowButton or Cf.NoPreview)) != (Cf.NoArrowButton or Cf.NoPreview)) { "Can't use both flags together" }

        val id = window.getID(label)

        val arrowSize = if (flags has Cf.NoArrowButton) 0f else frameHeight
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)
        val expectedW = calcItemWidth()
        val w = if (flags has Cf.NoPreview) arrowSize else expectedW
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id, frameBb)) return false

        val (pressed, hovered, _) = buttonBehavior(frameBb, id)
        var popupOpen = isPopupOpen(id)

        val frameCol = if (hovered) Col.FrameBgHovered else Col.FrameBg
        val valueX2 = frameBb.min.x max (frameBb.max.x - arrowSize)
        renderNavHighlight(frameBb, id)
        if (flags hasnt Cf.NoPreview)
            window.drawList.addRectFilled(frameBb.min, Vec2(valueX2, frameBb.max.y), frameCol.u32,
                    style.frameRounding, if (flags has Cf.NoArrowButton) 0 else DrawFlag.NoRoundCornerR.i)
        if (flags hasnt Cf.NoArrowButton) {
            val bgCol = if (popupOpen || hovered) Col.ButtonHovered else Col.Button
            window.drawList.addRectFilled(Vec2(valueX2, frameBb.min.y), frameBb.max, bgCol.u32, style.frameRounding, if (w <= arrowSize) 0 else DrawFlag.NoRoundCornerL.i)
            if (valueX2 + arrowSize - style.framePadding.x <= frameBb.max.x)
                window.drawList.renderArrow(Vec2(valueX2 + style.framePadding.y, frameBb.min.y + style.framePadding.y), Col.Text.u32, Dir.Down, 1f)
        }
        renderFrameBorder(frameBb.min, frameBb.max, style.frameRounding)
        if (previewValue != null && flags hasnt Cf.NoPreview)
            renderTextClipped(frameBb.min + style.framePadding, Vec2(valueX2, frameBb.max.y), previewValue)
        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        if ((pressed || g.navActivateId == id) && !popupOpen) {
            if (window.dc.navLayerCurrent == NavLayer.Main)
                window.navLastIds[0] = id
            openPopupEx(id)
            popupOpen = true
        }

        if (!popupOpen) return false

        if (hasWindowSizeConstraint) {
            g.nextWindowData.flags = g.nextWindowData.flags or NextWindowDataFlag.HasSizeConstraint
            g.nextWindowData.sizeConstraintRect.min.x = g.nextWindowData.sizeConstraintRect.min.x max w
        } else {
            if (flags hasnt Cf.HeightMask_)
                flags = flags or Cf.HeightRegular
            assert((flags and Cf.HeightMask_).isPowerOfTwo) { "Only one" }
            val popupMaxHeightInItems = when {
                flags has Cf.HeightRegular -> 8
                flags has Cf.HeightSmall -> 4
                flags has Cf.HeightLarge -> 20
                else -> -1
            }
            setNextWindowSizeConstraints(Vec2(w, 0f), Vec2(Float.MAX_VALUE, calcMaxPopupHeightFromItemCount(popupMaxHeightInItems)))
        }

        val name = "##Combo_%02d".format(g.beginPopupStack.size) // Recycle windows based on depth

        // Position the window given a custom constraint (peak into expected window size so we can position it)
        //    // This might be easier to express with an hypothetical SetNextWindowPosConstraints() function.
        findWindowByName(name)?.let {
            if (it.wasActive) {
                // Always override 'AutoPosLastDirection' to not leave a chance for a past value to affect us.
                val sizeExpected = it.calcNextAutoFitSize()
                it.autoPosLastDirection = when {
                    flags has Cf.PopupAlignLeft -> Dir.Left // "Below, Toward Left"
                    else -> Dir.Down // "Below, Toward Right (default)"
                }
                val rOuter = it.allowedExtentRect
                val pos = findBestWindowPosForPopupEx(frameBb.bl, sizeExpected, it::autoPosLastDirection, rOuter, frameBb, PopupPositionPolicy.ComboBox)
                setNextWindowPos(pos)
            }
        }

        // We don't use BeginPopupEx() solely because we have a custom name string, which we could make an argument to BeginPopupEx()
        val windowFlags: WindowFlags = Wf.AlwaysAutoResize or Wf._Popup or Wf.NoTitleBar or Wf.NoResize or Wf.NoSavedSettings or Wf.NoMove

        // Horizontally align ourselves with the framed text
        pushStyleVar(StyleVar.WindowPadding, Vec2(style.framePadding.x, style.windowPadding.y))
        val ret = begin(name, null, windowFlags)
        popStyleVar()
        if (!ret) {
            endPopup()
            assert(false) { "This should never happen as we tested for IsPopupOpen() above" }
            return false
        }

        return true
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
            markItemEdited(g.currentWindow!!.dc.lastItemId)

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