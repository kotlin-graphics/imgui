package imgui.api

import glm_.glm
import glm_.min
import glm_.vec2.Vec2
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.endChildFrame
import imgui.ImGui.endGroup
import imgui.ImGui.getID
import imgui.ImGui.isRectVisible
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.renderText
import imgui.ImGui.selectable
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.style
import imgui.ImGui.textLineHeightWithSpacing
import imgui.WindowFlag
import imgui.classes.ListClipper
import imgui.has
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.mutablePropertyAt
import imgui.mutableReference
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0

// Widgets: List Boxes
// - This is essentially a thin wrapper to using BeginChild/EndChild with some stylistic changes.
// - The BeginListBox()/EndListBox() api allows you to manage your contents and selection state however you want it, by creating e.g. Selectable() or any items.
// - The simplified/old ListBox() api are helpers over BeginListBox()/EndListBox() which are kept available for convenience purpose. This is analoguous to how Combos are created.
// - Choose frame width:   size.x > 0.0f: custom  /  size.x < 0.0f or -FLT_MIN: right-align   /  size.x = 0.0f (default): use current ItemWidth
// - Choose frame height:  size.y > 0.0f: custom  /  size.y < 0.0f or -FLT_MIN: bottom-align  /  size.y = 0.0f (default): arbitrary default height which can fit ~7 items
interface widgetsListBoxes {

    /** open a framed scrolling region */
    /** Tip: To have a list filling the entire window width, use `size.x = -FLT_MI` and pass an non-visible label e.g. "##empty"
     *  Tip: If your vertical size is calculated from an item count (e.g. 10 * item_height) consider adding a fractional part to facilitate seeing scrolling boundaries (e.g. 10.25 * item_height).*/
    fun beginListBox(label: String, sizeArg: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        // Size default to hold ~7.25 items.
        // Fractional number of items helps seeing that we can scroll down/up without looking at scrollbar.
        val size = floor(calcItemSize(sizeArg, calcItemWidth(), textLineHeightWithSpacing * 7.25f + style.framePadding.y * 2f))
        val frameSize = Vec2(size.x, glm.max(size.y, labelSize.y))
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        g.nextItemData.clearFlags()

        if (!isRectVisible(bb.min, bb.max)) {
            itemSize(bb.size, style.framePadding.y)
            itemAdd(bb, 0, frameBb)
            return false
        }

        // FIXME-OPT: We could omit the BeginGroup() if label_size.x but would need to omit the EndGroup() as well.
        beginGroup()
        if (labelSize.x > 0f) {
            val labelPos = Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y)
            renderText(labelPos, label)
            window.dc.cursorMaxPos maxAssign (labelPos + labelSize)
        }

        beginChildFrame(id, frameBb.size)
        return true
    }

    /** only call EndListBox() if BeginListBox() returned true! */
    fun endListBox() {
        val window = g.currentWindow!!
        assert(window.flags has WindowFlag._ChildWindow) { "Mismatched BeginListBox/EndListBox calls. Did you test the return value of BeginListBox()?" }

        endChildFrame()

        endGroup() // This is only required to be able to do IsItemXXX query on the whole ListBox including label
    }

    fun listBox(label: String, currentItemPtr: IntArray, items: Array<String>, heightInItems: Int = -1): Boolean =
            listBox(label, currentItemPtr mutablePropertyAt 0, items, heightInItems)

    /** This is merely a helper around BeginListBox(), EndListBox().
     *  Considering using those directly to submit custom data or store selection differently. */
    fun listBox(label: String, currentItemPtr: KMutableProperty0<Int>, items: Array<String>, heightInItems_: Int = -1): Boolean {

        var currentItem by currentItemPtr
        val itemsCount = items.size

        // Calculate size from "height_in_items"
        val heightInItems = if (heightInItems_ < 0) heightInItems_ else itemsCount min 7
        val heightInItemsF = heightInItems + 0.25f
        val size = Vec2(0f, floor(textLineHeightWithSpacing * heightInItemsF + g.style.framePadding.y * 2f))

        if (!beginListBox(label, size))
            return false
        // Assume all items have even height (= 1 line of text). If you need items of different or variable height,
        // you can create a custom version of ListBox() in your code without using the clipper.
        var valueChanged = false
        // We know exactly our line height here so we pass it as a minor optimization, but generally you don't need to.
        val clipper = ListClipper()
        clipper.begin(itemsCount, textLineHeightWithSpacing)
        while (clipper.step())
            for (i in clipper.display) {
                val itemText = items.getOrElse(i) { "*Unknown item*" }
                pushID(i)
                val itemSelectedRef = (i == currentItem).mutableReference
                val itemSelected by itemSelectedRef
                if (selectable(itemText, itemSelectedRef)) {
                    currentItem = i
                    valueChanged = true
                }
                if (itemSelected) setItemDefaultFocus()
                popID()
            }
        endListBox()

        if (valueChanged)
            markItemEdited(g.lastItemData.id)

        clipper.end()
        return valueChanged
    }

    //-------------------------------------------------------------------------
    // FIXME: This is an old API. We should redesign some of it, rename ListBoxHeader->BeginListBox, ListBoxFooter->EndListBox
    // and promote using them over existing ListBox() functions, similarly to how we now use combo boxes.
    //-------------------------------------------------------------------------

    /** Tip: To have a list filling the entire window width, use size.x = -FLT_MIN and pass an non-visible label e.g. "##empty"
     *  Tip: If your vertical size is calculated from an item count (e.g. 10 * item_height) consider adding a fractional part to facilitate seeing scrolling boundaries (e.g. 10.25 * item_height). */
    fun beginListBox(label: String, itemsCount: Int, heightInItems: Int = -1): Boolean {
        // If height_in_items == -1, default height is maximum 7.
        val heightInItemsF = (if(heightInItems < 0) itemsCount min 7 else heightInItems) + 0.25f

        val size = Vec2(0f, textLineHeightWithSpacing * heightInItemsF + g.style.framePadding.y * 2f)
        return beginListBox(label, size)
    }
}