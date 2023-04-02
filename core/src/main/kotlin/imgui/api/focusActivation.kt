package imgui.api

import imgui.Dir
import imgui.ImGui.currentWindow
import imgui.ImGui.isItemVisible
import imgui.ImGui.navMoveRequestResolveWithLastItem
import imgui.ImGui.navMoveRequestSubmit
import imgui.ImGui.rectAbsToRel
import imgui.ImGui.scrollToRectEx
import imgui.ImGui.setNavWindow
import imgui.internal.sections.IMGUI_DEBUG_LOG_ACTIVEID
import imgui.internal.sections.NavMoveFlag
import imgui.internal.sections.ScrollFlag
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
        if (g.navWindow !== window.rootWindowForNav || (!g.navInitRequest && g.navInitResultId == 0) || g.navLayer != window.dc.navLayerCurrent)
            return

        g.navInitRequest = false
        g.navInitResultId = g.lastItemData.id
        g.navInitResultRectRel = window rectAbsToRel g.lastItemData.rect
        navUpdateAnyRequestFlag()

        // Scroll could be done in NavInitRequestApplyResult() via an opt-in flag (we however don't want regular init requests to scroll)
        if (!isItemVisible)
            scrollToRectEx(window, g.lastItemData.rect)
    }

    /** focus keyboard on the next widget. Use positive 'offset' to access sub components of a multiple component widget.
     *  Use -1 to access previous widget.
     *
     *  Note: this will likely be called ActivateItem() once we rework our Focus/Activation system! */
    fun setKeyboardFocusHere(offset: Int = 0) = with(currentWindow) {
        val window = g.currentWindow!!
        assert(offset >= -1) { "-1 is allowed but not below" }
        IMGUI_DEBUG_LOG_ACTIVEID("SetKeyboardFocusHere($offset) in window \"${window.name}\"")

        // It makes sense in the vast majority of cases to never interrupt a drag and drop.
        // When we refactor this function into ActivateItem() we may want to make this an option.
        // MovingWindow is protected from most user inputs using SetActiveIdUsingNavAndKeys(), but
        // is also automatically dropped in the event g.ActiveId is stolen.
        if (g.dragDropActive || g.movingWindow != null) {
            IMGUI_DEBUG_LOG_ACTIVEID("SetKeyboardFocusHere() ignored while DragDropActive!")
            return
        }

        setNavWindow(window)

        val scrollFlags = if (window.appearing) ScrollFlag.KeepVisibleEdgeX or ScrollFlag.AlwaysCenterY else ScrollFlag.KeepVisibleEdgeX or ScrollFlag.KeepVisibleEdgeY
        navMoveRequestSubmit(Dir.None, if (offset < 0) Dir.Up else Dir.Down, NavMoveFlag.Tabbing or NavMoveFlag.FocusApi, scrollFlags) // FIXME-NAV: Once we refactor tabbing, add LegacyApi flag to not activate non-inputable.
        if (offset == -1)
            navMoveRequestResolveWithLastItem(g.navMoveResultLocal)
        else {
            g.navTabbingDir = 1
            g.navTabbingCounter = offset + 1
        }
    }
}