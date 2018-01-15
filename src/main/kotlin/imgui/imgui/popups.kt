package imgui.imgui

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginPopupEx
import imgui.ImGui.closePopup
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.isAnyItemHovered
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isPopupOpen
import imgui.ImGui.isWindowHovered
import imgui.ImGui.openPopupEx
import imgui.ImGui.popStyleVar
import imgui.ImGui.setNextWindowPos
import imgui.Context as g
import imgui.HoveredFlags as Hf
import imgui.WindowFlags as Wf

/** Popups  */
interface imgui_popups {


    /** call to mark popup as open (don't call every frame!). popups are closed when user click outside, or if
     *  CloseCurrentPopup() is called within a BeginPopup()/EndPopup() block. By default, Selectable()/MenuItem() are
     *  calling CloseCurrentPopup(). Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup
     *  needs to be at the same level).   */
    fun openPopup(strId: String) = openPopupEx(g.currentWindow!!.getId(strId), false)

    /** Helper to open popup when clicked on last item. return true when just opened.   */
    fun openPopupOnItemClick(strId: String = "", mouseButton: Int = 1) = with(g.currentWindow!!) {
        if (isMouseClicked(mouseButton) && isItemHovered(Hf.AllowWhenBlockedByPopup)) {
            // If user hasn't passed an ID, we can use the LastItemID. Using LastItemID as a Popup ID won't conflict!
            val id = if (strId.isNotEmpty()) getId(strId) else dc.lastItemId
            assert(id != 0) // However, you cannot pass a NULL str_id if the last item has no identifier (e.g. a Text() item)
            openPopupEx(id, true)
            true
        } else false
    }

    /** return true if the popup is open, and you can start outputting to it. only call EndPopup() if BeginPopup()
     *  returned true!   */
    fun beginPopup(strId: String): Boolean {
        if (g.openPopupStack.size <= g.currentPopupStack.size) {    // Early out for performance
            clearSetNextWindowData()    // We behave like Begin() and need to consume those values
            return false
        }
        return beginPopupEx(g.currentWindow!!.getId(strId), Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

    /** modal dialog (block interactions behind the modal window, can't close the modal window by clicking outside) */
    fun beginPopupModal(name: String, pOpen: BooleanArray? = null, extraFlags: Int = 0): Boolean {

        val window = g.currentWindow!!
        val id = window.getId(name)
        if (!isPopupOpen(id)) {
            clearSetNextWindowData() // We behave like Begin() and need to consume those values
            return false
        }
        // Center modal windows by default
        if (window.setWindowPosAllowFlags hasnt g.setNextWindowPosCond)
            setNextWindowPos(Vec2(IO.displaySize.x * 0.5f, IO.displaySize.y * 0.5f), Cond.Appearing, Vec2(0.5f))

        val flags = extraFlags or Wf.Popup or Wf.Modal or Wf.NoCollapse or Wf.NoSavedSettings
        val isOpen = begin(name, pOpen, flags)
        // NB: isOpen can be 'false' when the popup is completely clipped (e.g. zero size display)
        if (!isOpen || (pOpen != null && !pOpen.get(0))) {
            endPopup()
            if (isOpen) closePopup(id)
            return false
        }
        return isOpen
    }

    /** This is a helper to handle the simplest case of associating one named popup to one given widget.
     *  You may want to handle this on user side if you have specific needs (e.g. tweaking IsItemHovered() parameters).
     *  You can pass a NULL str_id to use the identifier of the last item.
     *  helper to open and begin popup when clicked on last item. if you can pass an empty strId only if the previous
     *  item had an id. If you want to use that on a non-interactive item such as text() you need to pass in an explicit
     *  id here. read comments in .cpp! */
    fun beginPopupContextItem(strId: String = "", mouseButton: Int = 1): Boolean {
        val window = currentWindow
        // If user hasn't passed an id, we can use the lastItemID. Using lastItemID as a Popup id won't conflict!
        val id = if(strId.isNotEmpty()) window.getId(strId) else window.dc.lastItemId
        assert(id != 0) // However, you cannot pass a NULL str_id if the last item has no identifier (e.g. a text() item)
        if (isMouseClicked(mouseButton))
            if (isItemHovered(Hf.AllowWhenBlockedByPopup))
                openPopupEx(id, true)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

    /** Helper to open and begin popup when clicked on current window.  */
    fun beginPopupContextWindow(strId: String = "", mouseButton: Int = 1, alsoOverItems: Boolean = true): Boolean {
        val strId = if (strId.isEmpty()) "window_context" else strId
        val id = currentWindow.getId(strId)
        if (isMouseClicked(mouseButton))
            if (isWindowHovered(Hf.AllowWhenBlockedByPopup))
                if (alsoOverItems || !isAnyItemHovered)
                    openPopupEx(id, true)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

//    IMGUI_API bool          BeginPopupContextVoid(const char* str_id = NULL, int mouse_button = 1); // helper to open and begin popup when clicked in void (where there are no imgui windows).

    fun endPopup() {
        assert(currentWindow.flags has Wf.Popup)  // Mismatched BeginPopup()/EndPopup() calls
        assert(g.currentPopupStack.isNotEmpty())
        end()
    }

    fun isPopupOpen(strId: String) = g.openPopupStack.size > g.currentPopupStack.size &&
            g.openPopupStack[g.currentPopupStack.size].popupId == g.currentWindow!!.getId(strId)

    /** close the popup we have begin-ed into. clicking on a MenuItem or Selectable automatically close
     *  the current popup.  */
    fun closeCurrentPopup() {

        var popupIdx = g.currentPopupStack.lastIndex
        if (popupIdx < 0 || popupIdx >= g.openPopupStack.size || g.currentPopupStack[popupIdx].popupId != g.openPopupStack[popupIdx].popupId)
            return
        while (popupIdx > 0 && g.openPopupStack[popupIdx].window != null && (g.openPopupStack[popupIdx].window!!.flags has Wf.ChildMenu))
            popupIdx--
        closePopupToLevel(popupIdx)
    }
}