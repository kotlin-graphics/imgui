package imgui.imgui

import imgui.WindowFlags
import imgui.closePopupToLevel
import imgui.has
import imgui.Context as g

/** Popups  */
interface imgui_popups {


    /** mark popup as open. popups are closed when user click outside, or activate a pressable item, or
     *  CloseCurrentPopup() is called within a BeginPopup()/EndPopup() block. popup identifiers are relative to the
     *  current ID-stack (so OpenPopup and BeginPopup needs to be at the same level).   */
    fun openPopup(strId:String) = open
//    IMGUI_API bool          BeginPopup(const char* str_id);                                     // return true if the popup is open, and you can start outputting to it. only call EndPopup() if BeginPopup() returned true!
//    IMGUI_API bool          BeginPopupModal(const char* name, bool* p_open = NULL, ImGuiWindowFlags extra_flags = 0);               // modal dialog (block interactions behind the modal window, can't close the modal window by clicking outside)
//    IMGUI_API bool          BeginPopupContextItem(const char* str_id, int mouse_button = 1);                                        // helper to open and begin popup when clicked on last item. read comments in .cpp!
//    IMGUI_API bool          BeginPopupContextWindow(bool also_over_items = true, const char* str_id = NULL, int mouse_button = 1);  // helper to open and begin popup when clicked on current window.
//    IMGUI_API bool          BeginPopupContextVoid(const char* str_id = NULL, int mouse_button = 1);                                 // helper to open and begin popup when clicked in void (no window).
//    IMGUI_API void          EndPopup();
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