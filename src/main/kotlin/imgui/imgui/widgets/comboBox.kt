package imgui.imgui.widgets

import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.endPopup
import imgui.ImGui.findBestWindowPosForPopupEx
import imgui.ImGui.findWindowByName
import imgui.ImGui.frameHeight
import imgui.ImGui.isPopupOpen
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.openPopupEx
import imgui.ImGui.popId
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushId
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderArrow
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.selectable
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.style
import imgui.internal.*
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.reflect.KMutableProperty0
import imgui.ComboFlag as Cf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf

/** Widgets: Combo Box
 *  - The new BeginCombo()/EndCombo() api allows you to manage your contents and selection state however you want it, by creating e.g. Selectable() items.
 *  - The old Combo() api are helpers over BeginCombo()/EndCombo() which are kept available for convenience purpose.    */
interface comboBox {

    fun beginCombo(label: String, previewValue: String?, flags_: ComboFlags = 0): Boolean {

        var flags = flags_

        // Always consume the SetNextWindowSizeConstraint() call in our early return paths
        val backupNextWindowSizeConstraint = g.nextWindowData.sizeConstraintCond
        g.nextWindowData.sizeConstraintCond = Cond.None

        val window = currentWindow
        if (window.skipItems) return false

        assert((flags and (Cf.NoArrowButton or Cf.NoPreview)) != (Cf.NoArrowButton or Cf.NoPreview)) { "Can't use both flags together" }

        val id = window.getId(label)

        val arrowSize = if (flags has Cf.NoArrowButton) 0f else frameHeight
        val labelSize = calcTextSize(label, true)
        val w = if (flags has Cf.NoPreview) arrowSize else calcItemWidth()
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id, frameBb)) return false

        val (pressed, hovered, held) = buttonBehavior(frameBb, id)
        var popupOpen = isPopupOpen(id)

        val valueBb = Rect(frameBb.min, frameBb.max - Vec2(arrowSize, 0f))
        val frameCol = if (hovered) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(frameBb, id)
        if (flags hasnt Cf.NoPreview)
            window.drawList.addRectFilled(frameBb.min, Vec2(frameBb.max.x - arrowSize, frameBb.max.y), frameCol.u32,
                    style.frameRounding, DrawCornerFlag.Left.i)
        if (flags hasnt Cf.NoArrowButton) {
            val col = if (popupOpen || hovered) Col.ButtonHovered else Col.Button
            val f = if (w <= arrowSize) DrawCornerFlag.All else DrawCornerFlag.Right
            window.drawList.addRectFilled(Vec2(frameBb.max.x - arrowSize, frameBb.min.y), frameBb.max, col.u32, style.frameRounding, f.i)
            renderArrow(Vec2(frameBb.max.x - arrowSize + style.framePadding.y, frameBb.min.y + style.framePadding.y), Dir.Down)
        }
        renderFrameBorder(frameBb.min, frameBb.max, style.frameRounding)
        if (previewValue != null && flags hasnt Cf.NoPreview)
            renderTextClipped(frameBb.min + style.framePadding, valueBb.max, previewValue)
        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        if ((pressed || g.navActivateId == id) && !popupOpen) {
            if (window.dc.navLayerCurrent == NavLayer.Main)
                window.navLastIds[0] = id
            openPopupEx(id)
            popupOpen = true
        }

        if (!popupOpen) return false

        if (backupNextWindowSizeConstraint != Cond.None) {
            g.nextWindowData.sizeConstraintCond = backupNextWindowSizeConstraint
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

        // Peak into expected window size so we can position it
        findWindowByName(name)?.let {
            if (it.wasActive) {
                val sizeExpected = it.calcExpectedSize()
                if (flags has Cf.PopupAlignLeft)
                    it.autoPosLastDirection = Dir.Left
                val rOuter = it.getAllowedExtentRect()
                val pos = findBestWindowPosForPopupEx(frameBb.bl, sizeExpected, it::autoPosLastDirection, rOuter, frameBb, PopupPositionPolicy.ComboBox)
                setNextWindowPos(pos)
            }
        }

        // Horizontally align ourselves with the framed text
        val windowFlags = Wf.AlwaysAutoResize or Wf.Popup or Wf.NoTitleBar or Wf.NoResize or Wf.NoSavedSettings
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
    fun combo(label: String, currentItem: KMutableProperty0<Int>, items: Array<String>, itemsCount: Int = items.size,
              heightInItems: Int = -1) = combo(label, currentItem, items.toList(), heightInItems)

    /** Combo box helper allowing to pass all items in a single string literal holding multiple zero-terminated items "item1\0item2\0" */
    fun combo(label: String, currentItem: IntArray, itemsSeparatedByZeros: String, heightInItems: Int = -1): Boolean {
        i = currentItem[0]
        val items = itemsSeparatedByZeros.split(NUL).filter { it.isNotEmpty() }
        // FIXME-OPT: Avoid computing this, or at least only when combo is open
        val res = combo(label, Companion::i, items, heightInItems)
        currentItem[0] = i
        return res
    }

    fun combo(label: String, currentItem: KMutableProperty0<Int>, itemsSeparatedByZeros: String, heightInItems: Int = -1): Boolean {
        val items = itemsSeparatedByZeros.split(NUL).filter { it.isNotEmpty() }
        // FIXME-OPT: Avoid computing this, or at least only when combo is open
        return combo(label, currentItem, items, heightInItems)
    }

    /** Combo box function. */
    fun combo(label: String, currentItem: IntArray, items: List<String>, popupMaxHeightInItem: Int = -1): Boolean {
        i = currentItem[0]
        val res = combo(label, Companion::i, items, popupMaxHeightInItem)
        currentItem[0] = i
        return res
    }

    fun combo(label: String, currentItemPtr: KMutableProperty0<Int>, items: List<String>, popupMaxHeightInItem: Int = -1): Boolean {

        var currentItem by currentItemPtr
        val previewValue = items.getOrElse(currentItem) { "" }

        /*  The old Combo() API exposed "popup_max_height_in_items". The new more general BeginCombo() API doesn't,
            have/need it, but we emulate it here.         */
        if (popupMaxHeightInItem != -1 && g.nextWindowData.sizeConstraintCond == Cond.None)
            setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE, calcMaxPopupHeightFromItemCount(popupMaxHeightInItem)))

        if (!beginCombo(label, previewValue, Cf.None.i)) return false

        // Display items
        // FIXME-OPT: Use clipper (but we need to disable it on the appearing frame to make sure our call to setItemDefaultFocus() is processed)
        var valueChanged = false
        for (i in 0 until items.size) {
            pushId(i)
            val itemSelected = i == currentItem
            val itemText = items.getOrElse(i) { "*Unknown item*" }
            if (selectable(itemText, itemSelected)) {
                valueChanged = true
                currentItem = i
            }
            if (itemSelected) setItemDefaultFocus()
            popId()
        }
        endCombo()
        return valueChanged
    }

    companion object {
        private var i = 0

        //-------------------------------------------------------------------------
        // [SECTION] Widgets: ComboBox
        //-------------------------------------------------------------------------
        // - BeginCombo()
        // - EndCombo()
        // - Combo()
        //-------------------------------------------------------------------------

        fun calcMaxPopupHeightFromItemCount(itemsCount: Int) = when {
                itemsCount <= 0 -> Float.MAX_VALUE
                else -> (g.fontSize + style.itemSpacing.y) * itemsCount - style.itemSpacing.y + style.windowPadding.y * 2
        }
    }
}