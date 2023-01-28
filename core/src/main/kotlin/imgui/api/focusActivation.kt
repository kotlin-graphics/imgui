package imgui.api

import imgui.Dir
import imgui.ImGui.currentWindow
import imgui.ImGui.isItemVisible
import imgui.ImGui.navMoveRequestResolveWithLastItem
import imgui.ImGui.navMoveRequestSubmit
import imgui.ImGui.setScrollHereY
import imgui.internal.classes.Rect
import imgui.internal.sections.NavMoveFlag
import imgui.internal.sections.ScrollFlag
import imgui.internal.sections.or
import imgui.static.navUpdateAnyRequestFlag


/** Focus, Activation
 *  - Prefer using "SetItemDefaultFocus()" over "if (IsWindowAppearing()) SetScrollHereY()" when applicable to signify "this is the default item" */
interface focusActivation {

    // (Prefer using "SetItemDefaultFocus()" over "if (IsWindowAppearing()) SetScrollHereY()" when applicable to signify "this is the default item")

    /** make last item the default focused item of a window. */
    fun setItemDefaultFocus() {
        val window = g.currentWindow!!
        if (!window.appearing)
            return
        if (g.navWindow === window.rootWindowForNav && (g.navInitRequest || g.navInitResultId != 0) && g.navLayer == window.dc.navLayerCurrent) {
            g.navInitRequest = false
            g.navInitResultId = g.lastItemData.id
            g.navInitResultRectRel = Rect(g.lastItemData.rect.min - window.pos, g.lastItemData.rect.max - window.pos)
            navUpdateAnyRequestFlag()
            if (!isItemVisible) setScrollHereY()
        }
    }

    /** focus keyboard on the next widget. Use positive 'offset' to access sub components of a multiple component widget.
     *  Use -1 to access previous widget.   */
    fun setKeyboardFocusHere(offset: Int = 0) = with(currentWindow) {
        assert(offset >= -1) { "-1 is allowed but not below" }
        val window = g.currentWindow!!
        assert(offset >= -1) { "-1 is allowed but not below" }
        g.navWindow = window
        val scrollFlags = if(window.appearing) ScrollFlag.KeepVisibleEdgeX or ScrollFlag.AlwaysCenterY else ScrollFlag.KeepVisibleEdgeX or ScrollFlag.KeepVisibleEdgeY
        navMoveRequestSubmit(Dir.None, Dir.None, NavMoveFlag.Tabbing.i, scrollFlags) // FIXME-NAV: Once we refactor tabbing, add LegacyApi flag to not activate non-inputable.
        if (offset == -1)
            navMoveRequestResolveWithLastItem()
        else
            g.navTabbingInputableRemaining = offset + 1
    }
}