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
import imgui.withBool
import imgui.withInt
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0

/**
 * Widgets: List Boxes
 * FIXME: To be consistent with all the newer API, ListBoxHeader/ListBoxFooter should in reality be called BeginListBox/EndListBox. Will rename them.
 */
interface widgetsListBoxes {

    fun listBox(label: String, currentItemPtr: IntArray, items: Array<String>, heightInItems: Int = -1): Boolean =
            withInt(currentItemPtr) { listBox(label, it, items, heightInItems) }

    fun listBox(label: String, currentItemPtr: KMutableProperty0<Int>, items: Array<String>, heightInItems: Int = -1): Boolean {

        var currentItem by currentItemPtr
        val itemsCount = items.size
        if (!listBoxHeader(label, itemsCount, heightInItems)) return false
        // Assume all items have even height (= 1 line of text). If you need items of different or variable height,
        // you can create a custom version of ListBox() in your code without using the clipper.
        var valueChanged = false
        // We know exactly our line height here so we pass it as a minor optimization, but generally you don't need to.
        val clipper = ListClipper()
        clipper.begin(itemsCount, textLineHeightWithSpacing)
        while (clipper.step())
            for (i in clipper.display)
                withBool { itemSelected ->
                    val itemText = items.getOrElse(i) { "*Unknown item*" }

                    pushID(i)
                    itemSelected.set(i == currentItem)
                    if (selectable(itemText, itemSelected)) {
                        currentItem = i
                        valueChanged = true
                    }
                    if (itemSelected()) setItemDefaultFocus()
                    popID()
                }
        listBoxFooter()
        if (valueChanged)
            markItemEdited(g.currentWindow!!.dc.lastItemId)

        return valueChanged
    }

    //-------------------------------------------------------------------------
    // FIXME: This is an old API. We should redesign some of it, rename ListBoxHeader->BeginListBox, ListBoxFooter->EndListBox
    // and promote using them over existing ListBox() functions, similarly to how we now use combo boxes.
    //-------------------------------------------------------------------------

    /** FIXME: In principle this function should be called BeginListBox(). We should rename it after re-evaluating if we
     *  want to keep the same signature.
     *  Tip: To have a list filling the entire window width, use `size.x = -FLT_MI` and pass an non-visible label e.g. "##empty"
     *  Tip: If your vertical size is calculated from an item count (e.g. 10 * item_height) consider adding a fractional part to facilitate seeing scrolling boundaries (e.g. 10.25 * item_height).*/
    fun listBoxHeader(label: String, sizeArg: Vec2 = Vec2()): Boolean {

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

    /** FIXME: In principle this function should be called BeginListBox(). We should rename it after re-evaluating if we want to keep the same signature. */
    fun listBoxHeader(label: String, itemsCount: Int, heightInItems: Int = -1): Boolean {
        // If height_in_items == -1, default height is maximum 7.
        val heightInItemsF = (if(heightInItems < 0) itemsCount min 7 else heightInItems) + 0.25f

        val size = Vec2(0f, textLineHeightWithSpacing * heightInItemsF + g.style.framePadding.y * 2f)
        return listBoxHeader(label, size)
    }

    /** FIXME: In principle this function should be called EndListBox(). We should rename it after re-evaluating if we want to keep the same signature.
     *  Terminate the scrolling region. Only call ListBoxFooter() if ListBoxHeader() returned true!  */
    fun listBoxFooter() {
        val window = g.currentWindow!!
        assert(window.flags has WindowFlag._ChildWindow) { "Mismatched ListBoxHeader/ListBoxFooter calls. Did you test the return value of ListBoxHeader()?" }

        endChildFrame()

        endGroup() // This is only required to be able to do IsItemXXX query on the whole ListBox including label
    }
}