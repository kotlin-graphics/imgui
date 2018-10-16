package imgui.imgui.widgets

import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.endChildFrame
import imgui.ImGui.endGroup
import imgui.ImGui.getId
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.popId
import imgui.ImGui.pushId
import imgui.ImGui.renderText
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.style
import imgui.ImGui.textLineHeightWithSpacing
import imgui.internal.Rect
import kotlin.reflect.KMutableProperty0
import imgui.ItemFlag as If
import imgui.SelectableFlag as Sf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf

interface listBoxes {

    fun listBox(label: String, currentItemPtr: KMutableProperty0<Int>, items: Array<String>, heightInItems: Int = -1): Boolean {

        var currentItem by currentItemPtr
        val itemsCount = items.size
        if (!listBoxHeader(label, itemsCount, heightInItems)) return false
        // Assume all items have even height (= 1 line of text). If you need items of different or variable sizes you can create a custom version of ListBox() in your code without using the clipper.
        var valueChanged = false
        // We know exactly our line height here so we pass it as a minor optimization, but generally you don't need to.
        val clipper = ListClipper(itemsCount, textLineHeightWithSpacing)
        while (clipper.step())
            for (i in clipper.display.start until clipper.display.last)
                withBool { itemSelected ->
                    itemSelected.set(i == currentItem)
                    val itemText = items.getOrElse(i) { "*Unknown item*" }
                    pushId(i)
                    if (selectable(itemText, itemSelected)) {
                        currentItem = i
                        valueChanged = true
                    }
                    if (itemSelected()) setItemDefaultFocus()
                    popId()
                }
        listBoxFooter()
        if (valueChanged)
            markItemEdited(g.currentWindow!!.dc.lastItemId)

        return valueChanged
    }

    /** Helper to calculate the size of a listbox and display a label on the right.
     *  Tip: To have a list filling the entire window width, PushItemWidth(-1) and pass an empty label "##empty"
     *  use if you want to reimplement ListBox() will custom data or interactions.
     *  If the function return true, you can output elements then call ListBoxFooter() afterwards. */
    fun listBoxHeader(label: String, sizeArg: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = getId(label)
        val labelSize = calcTextSize(label, true)

        // Size default to hold ~7 items. Fractional number of items helps seeing that we can scroll down/up without looking at scrollbar.
        val size = calcItemSize(sizeArg, calcItemWidth(), textLineHeightWithSpacing * 7.4f + style.itemSpacing.y)
        val frameSize = Vec2(size.x, glm.max(size.y, labelSize.y))
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        window.dc.lastItemRect put bb   // Forward storage for ListBoxFooter.. dodgy.

        beginGroup()
        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        beginChildFrame(id, frameBb.size)
        return true
    }

    /** use if you want to reimplement ListBox() will custom data or interactions. make sure to call ListBoxFooter()
     *  afterwards. */
    fun listBoxHeader(label: String, itemsCount: Int, heightInItems_: Int = -1): Boolean {
        /*  Size default to hold ~7 items. Fractional number of items helps seeing that we can scroll down/up without
            looking at scrollbar.
            We don't add +0.40f if items_count <= height_in_items. It is slightly dodgy, because it means a
            dynamic list of items will make the widget resize occasionally when it crosses that size.
            I am expecting that someone will come and complain about this behavior in a remote future, then we can
            advise on a better solution.    */
        val heightInItems = if (heightInItems_ < 0) glm.min(itemsCount, 7) else heightInItems_
        val heightInItemsF = heightInItems + if (heightInItems < itemsCount) 0.4f else 0f
        /*  We include ItemSpacing.y so that a list sized for the exact number of items doesn't make a scrollbar
            appears. We could also enforce that by passing a flag to BeginChild().         */
        val size = Vec2(0f, textLineHeightWithSpacing * heightInItemsF + style.itemSpacing.y)
        return listBoxHeader(label, size)
    }

    /** Terminate the scrolling region. Only call ListBoxFooter() if ListBoxHeader() returned true!  */
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

    companion object {
        // TODO move/delete
        private inline fun <R> withBool(block: (KMutableProperty0<Boolean>) -> R): R {
            Ref.bPtr++
            return block(Ref::bool).also { Ref.bPtr-- }
        }

        private inline fun <R> withInt(block: (KMutableProperty0<Int>) -> R): R {
            Ref.iPtr++
            return block(Ref::int).also { Ref.iPtr-- }
        }
    }
}