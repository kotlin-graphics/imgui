package imgui.api

import glm_.glm
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
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.style
import imgui.ImGui.textLineHeightWithSpacing
import imgui.classes.ListClipper
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
        // Assume all items have even height (= 1 line of text). If you need items of different or variable sizes you can create a custom version of ListBox() in your code without using the clipper.
        var valueChanged = false
        // We know exactly our line height here so we pass it as a minor optimization, but generally you don't need to.
        val clipper = ListClipper()
        clipper.begin(itemsCount, textLineHeightWithSpacing)
        while (clipper.step())
            for (i in clipper.displayStart until clipper.displayEnd)
                withBool { itemSelected ->
                    itemSelected.set(i == currentItem)
                    val itemText = items.getOrElse(i) { "*Unknown item*" }
                    pushID(i)
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
    // and promote using them over existing ListBox() functions, similarly to change with combo boxes.
    //-------------------------------------------------------------------------

    /** FIXME: In principle this function should be called BeginListBox(). We should rename it after re-evaluating if we
     *  want to keep the same signature.
     *  Helper to calculate the size of a listbox and display a label on the right.
     *  Tip: To have a list filling the entire window width, PushItemWidth(-1) and pass an non-visible label e.g. "##empty" */
    fun listBoxHeader(label: String, sizeArg: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        // Size default to hold ~7 items. Fractional number of items helps seeing that we can scroll down/up without looking at scrollbar.
        val size = calcItemSize(sizeArg, calcItemWidth(), textLineHeightWithSpacing * 7.4f + style.itemSpacing.y)
        val frameSize = Vec2(size.x, glm.max(size.y, labelSize.y))
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        window.dc.lastItemRect put bb   // Forward storage for ListBoxFooter.. dodgy.

        if (!isRectVisible(bb.min, bb.max)) {
            itemSize(bb.size, style.framePadding.y)
            itemAdd(bb, 0, frameBb)
            return false
        }

        beginGroup()
        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        beginChildFrame(id, frameBb.size)
        return true
    }

    /** FIXME: In principle this function should be called EndListBox(). We should rename it after re-evaluating if we want to keep the same signature. */
    fun listBoxHeader(label: String, itemsCount: Int, heightInItems_: Int = -1): Boolean {
        /*  Size default to hold ~7.25 items.
            We add +25% worth of item height to allow the user to see at a glance if there are more items up/down, without looking at the scrollbar.
            We don't add this extra bit if items_count <= height_in_items. It is slightly dodgy,
            because it means a dynamic list of items will make the widget resize occasionally when it crosses that size.     */
        val heightInItems = if (heightInItems_ < 0) glm.min(itemsCount, 7) else heightInItems_
        val heightInItemsF = heightInItems + if (heightInItems < itemsCount) 0.25f else 0f
        /*  We include ItemSpacing.y so that a list sized for the exact number of items doesn't make a scrollbar
            appears. We could also enforce that by passing a flag to BeginChild().         */
        val size = Vec2(0f, floor(textLineHeightWithSpacing * heightInItemsF + style.framePadding.y * 2f))
        return listBoxHeader(label, size)
    }

    /** FIXME: In principle this function should be called EndListBox(). We should rename it after re-evaluating if we want to keep the same signature.
     *  Terminate the scrolling region. Only call ListBoxFooter() if ListBoxHeader() returned true!  */
    fun listBoxFooter() {
        val parentWindow = currentWindow.parentWindow!!
        val bb = parentWindow.dc.lastItemRect // assign is safe, itemSize() won't modify bb

        endChildFrame()

        /*  Redeclare item size so that it includes the label (we have stored the full size in LastItemRect)
            We call SameLine() to restore DC.CurrentLine* data         */
        sameLine()
        parentWindow.dc.cursorPos put bb.min
        itemSize(bb, style.framePadding.y)
        endGroup()
    }
}