package imgui.imgui

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginPopupEx
import imgui.ImGui.closePopup
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.isPopupOpen
import imgui.ImGui.openPopupEx
import imgui.ImGui.popStyleVar
import imgui.ImGui.setNextWindowPos
import imgui.Context as g
import imgui.WindowFlags as Wf

/** Popups  */
interface imgui_popups {


    /** call to mark popup as open (don't call every frame!). popups are closed when user click outside, or if
     *  CloseCurrentPopup() is called within a BeginPopup()/EndPopup() block. By default, Selectable()/MenuItem() are
     *  calling CloseCurrentPopup(). Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup
     *  needs to be at the same level).   */
    fun openPopup(strId: String) = openPopupEx(g.currentWindow!!.getId(strId), false)

    /** return true if the popup is open, and you can start outputting to it. only call EndPopup() if BeginPopup()
     *  returned true!   */
    fun beginPopup(strId: String): Boolean {
        if (g.openPopupStack.size <= g.currentPopupStack.size) {    // Early out for performance
            clearSetNextWindowData()    // We behave like Begin() and need to consume those values
            return false
        }
        return beginPopupEx(g.currentWindow!!.getId(strId), Wf.ShowBorders or Wf.AlwaysAutoResize)
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
//    IMGUI_API bool          BeginPopupContextItem(const char* str_id, int mouse_button = 1);                                        // helper to open and begin popup when clicked on last item. read comments in .cpp!
//    fun beginPopupContextWindow(strId = NULL, int mouse_button = 1, bool also_over_items = true);  // helper to open and begin popup when clicked on current window.
//    IMGUI_API bool          BeginPopupContextVoid(const char* str_id = NULL, int mouse_button = 1);                                 // helper to open and begin popup when clicked in void (no window).

    fun endPopup() {

        val window = currentWindow
        assert(window.flags has Wf.Popup)  // Mismatched BeginPopup()/EndPopup() calls
        assert(g.currentPopupStack.isNotEmpty())
        end()
        if (window.flags hasnt Wf.Modal)
            popStyleVar()
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