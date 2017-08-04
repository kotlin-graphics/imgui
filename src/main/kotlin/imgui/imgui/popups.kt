package imgui.imgui

import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.openPopupEx
import imgui.ImGui.popStyleVar
import imgui.Context as g

/** Popups  */
interface imgui_popups {


    /** mark popup as open. popups are closed when user click outside, or activate a pressable item, or
     *  CloseCurrentPopup() is called within a BeginPopup()/EndPopup() block. popup identifiers are relative to the
     *  current ID-stack (so OpenPopup and BeginPopup needs to be at the same level).   */
    fun openPopup(strId: String) = openPopupEx(strId, false)
//    IMGUI_API bool          BeginPopup(const char* str_id);                                     // return true if the popup is open, and you can start outputting to it. only call EndPopup() if BeginPopup() returned true!

    /** modal dialog (block interactions behind the modal window, can't close the modal window by clicking outside) */
    fun beginPopupModal(name: String, pOpen: BooleanArray? = null, extraFlags: Int = 0): Boolean {

        val window = g.currentWindow!!
        val id = window.getId(name)
        if (!isPopupOpen(id)) {
            clearSetNextWindowData() // We behave like Begin() and need to consume those values
            return false
        }

        val flags = extraFlags or WindowFlags.Popup or WindowFlags.Modal or WindowFlags.NoCollapse or WindowFlags.NoSavedSettings
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
//    IMGUI_API bool          BeginPopupContextWindow(bool also_over_items = true, const char* str_id = NULL, int mouse_button = 1);  // helper to open and begin popup when clicked on current window.
//    IMGUI_API bool          BeginPopupContextVoid(const char* str_id = NULL, int mouse_button = 1);                                 // helper to open and begin popup when clicked in void (no window).

    fun endPopup() {

        val window = currentWindow
        assert(window.flags has WindowFlags.Popup)  // Mismatched BeginPopup()/EndPopup() calls
        assert(g.currentPopupStack.isNotEmpty())
        end()
        if (window.flags hasnt WindowFlags.Modal)
            popStyleVar()
    }

    /** close the popup we have begin-ed into. clicking on a MenuItem or Selectable automatically close
     *  the current popup.  */
    fun closeCurrentPopup() {

        var popupIdx = g.currentPopupStack.lastIndex
        if (popupIdx < 0 || popupIdx > g.openPopupStack.size || g.currentPopupStack[popupIdx].popupId != g.openPopupStack[popupIdx].popupId)
            return
        while (popupIdx > 0 && g.openPopupStack[popupIdx].window != null && (g.openPopupStack[popupIdx].window!!.flags has WindowFlags.ChildMenu))
            popupIdx--
        closePopupToLevel(popupIdx)
    }
}