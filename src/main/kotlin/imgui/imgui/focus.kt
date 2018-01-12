package imgui.imgui

import imgui.ImGui.isWindowAppearing
import imgui.ImGui.currentWindow
import imgui.ImGui.setScrollHere


interface imgui_focus {

    /** make last item the default focused item of a window (WIP navigation branch only). Please use instead of setScrollHere().
     *  FIXME-NAV: This function is a placeholder for the upcoming Navigation branch + Focusing features.
     *  In the current branch this function will only set the scrolling, in the navigation branch it will also set your navigation cursor.
     *  Prefer using "setItemDefaultFocus()" over "if (isWindowAppearing()) setScrollHere()" when applicable.*/
    fun setItemDefaultFocus() {
        if (isWindowAppearing) setScrollHere()
    }

    /** focus keyboard on the next widget. Use positive 'offset' to access sub components of a multiple component widget.
     *  Use -1 to access previous widget.   */
    fun setKeyboardFocusHere(offset: Int = 0) = with(currentWindow) {
        assert(offset >= -1)    // -1 is allowed but not below
        focusIdxAllRequestNext = focusIdxAllCounter + 1 + offset
        focusIdxTabRequestNext = Int.MAX_VALUE
    }
}