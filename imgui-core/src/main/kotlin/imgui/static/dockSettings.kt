package imgui.static

import imgui.ID
import imgui.IMGUI_DEBUG_LOG_DOCKING
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.DockNodeSettings

// Settings

//-----------------------------------------------------------------------------
// Docking: Settings
//-----------------------------------------------------------------------------
// - DockSettingsRenameNodeReferences()
// - DockSettingsRemoveNodeReferences()
// - DockSettingsFindNodeSettings()
// - DockSettingsHandler_ReadOpen()
// - DockSettingsHandler_ReadLine()
// - DockSettingsHandler_DockNodeToSettings()
// - DockSettingsHandler_WriteAll()
//-----------------------------------------------------------------------------


fun dockSettingsRenameNodeReferences(oldNodeId: ID, newNodeId: ID) {

    IMGUI_DEBUG_LOG_DOCKING("DockSettingsRenameNodeReferences: from 0x%08X -> to 0x%08X".format(oldNodeId, newNodeId))
    for (window in g.windows)
        if (window.dockId == oldNodeId && window.dockNode == null)
            window.dockId = newNodeId

    //// FIXME-OPT: We could remove this loop by storing the index in the map
    for (setting in g.settingsWindows)
        if (setting.dockId == oldNodeId)
            setting.dockId = newNodeId
}

/** Remove references stored in ImGuiWindowSettings to the given ImGuiDockNodeSettings */
fun dockSettingsRemoveNodeReferences(nodeIds: Array<ID>) {

    var found = 0
    //// FIXME-OPT: We could remove this loop by storing the index in the map
    for (setting in g.settingsWindows)
        for (nodeID in nodeIds)
            if (setting.dockId == nodeID) {
                setting.dockId = 0
                setting.dockOrder = -1
                if (++found < nodeIds.size)
                    break
                return
            }
}

fun dockSettingsFindNodeSettings(ctx: Context, id: ID): DockNodeSettings? =
    // FIXME-OPT
    ctx.dockContext!!.settingsNodes.find { it.id == id }

//static void*            DockSettingsHandler_ReadOpen(ImGuiContext*, ImGuiSettingsHandler*, const char* name) TODO
//static void             DockSettingsHandler_ReadLine(ImGuiContext*, ImGuiSettingsHandler*, void* entry, const char* line)
//static void             DockSettingsHandler_WriteAll(ImGuiContext* imgui_ctx, ImGuiSettingsHandler* handler, ImGuiTextBuffer* buf)