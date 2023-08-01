package imgui.internal.api

import imgui.Dir
import imgui.ID
import imgui.ImGui.navMoveRequestResolveWithLastItem
import imgui.ImGui.navMoveRequestSubmit
import imgui.ImGui.setNavWindow
import imgui.api.g
import imgui.api.gImGui
import imgui.div
import imgui.internal.sections.IMGUI_DEBUG_LOG_FOCUS
import imgui.internal.sections.NavMoveFlag
import imgui.internal.sections.ScrollFlag
import imgui.none

// Focus/Activation
// This should be part of a larger set of API: FocusItem(offset = -1), FocusItemByID(id), ActivateItem(offset = -1), ActivateItemByID(id) etc. which are
// much harder to design and implement than expected. I have a couple of private branches on this matter but it's not simple. For now implementing the easy ones.
interface focusActivation {

    // Focus last item (no selection/activation).
    // Focus = move navigation cursor, set scrolling, set focus window.
    fun focusItem() {
        val g = gImGui
        val window = g.currentWindow!!
        IMGUI_DEBUG_LOG_FOCUS("FocusItem(0x%08x) in window \"${window.name}\"", g.lastItemData.id)
        if (g.dragDropActive || g.movingWindow != null) { // FIXME: Opt-in flags for this?
            IMGUI_DEBUG_LOG_FOCUS("FocusItem() ignored while DragDropActive!")
            return
        }

        val moveFlags = NavMoveFlag.Tabbing / NavMoveFlag.FocusApi / NavMoveFlag.NoSelect
        val scrollFlags = ScrollFlag.KeepVisibleEdgeX / if(window.appearing) ScrollFlag.AlwaysCenterY else ScrollFlag.KeepVisibleEdgeY
        setNavWindow(window)
        navMoveRequestSubmit(Dir.None, Dir.Up, moveFlags, scrollFlags)
        navMoveRequestResolveWithLastItem(g.navMoveResultLocal)
    }

    /** Remotely activate a button, checkbox, tree node etc. given its unique ID. activation is queued and processed
     *  on the next frame when the item is encountered again.
     *
     *  Activate an item by ID (button, checkbox, tree node etc.). Activation is queued and processed on the next frame when the item is encountered again. */
    fun activateItemByID(id: ID) {
        g.navNextActivateId = id
        g.navNextActivateFlags = none
    }
}