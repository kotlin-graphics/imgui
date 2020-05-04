package imgui.static

import glm_.asHexString
import glm_.i
import glm_.max
import imgui.*
import imgui.api.g
import imgui.classes.Context
import imgui.internal.Axis
import imgui.internal.classes.DockContext
import imgui.internal.classes.DockNode
import imgui.internal.classes.DockNodeSettings
import imgui.internal.classes.SettingsHandler

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

fun dockSettingsHandler_ReadOpen(ctx: Context, settingsHandler: SettingsHandler, name: String): Any? =
        if (name == "Data") 1 else null

fun dockSettingsHandler_ReadLine(ctx: Context, settingsHandler: SettingsHandler, entry: Any, line: String) {

    // Parsing, e.g.
    // " DockNode   ID=0x00000001 Pos=383,193 Size=201,322 Split=Y,0.506 "
    // "   DockNode ID=0x00000002 Parent=0x00000001 "
    // Important: this code expect currently fields in a fixed order.
    val node = DockNodeSettings()
    val chunks = line.trim().split(Regex("\\s+"))
    var r = 0
    when (chunks[r]) {
        "DockNode" -> r++
        "DockSpace" -> {
            r++
            node.flags = node.flags or DockNodeFlag._DockSpace
        }
        else -> return
    }
    var chunk = chunks.getOrElse(r++) { return }
    var done = false
    fun next() = if (r < chunks.size) chunk = chunks[++r] else done = true
    if (chunk.startsWith("ID=0x")) {
        node.id = chunk.substring(2 + 1 + 2).toInt(16)
        next()
    } else return
    if (!done && chunk.startsWith("Parent=0x")) {
        node.parentNodeId == chunk.substring(6 + 1 + 2).toInt(16)
        next()
        if (node.parentNodeId == 0) return
    }
    if (!done && chunk.startsWith("Window=0x")) {
        node.parentWindowId = chunk.substring(6 + 1 + 2).toInt(16)
        next()
        if (node.parentWindowId == 0) return
    }
    if (node.parentNodeId == 0) {
        if (chunk.startsWith("Pos=")) {
            node.pos put line.substring(3 + 1).split(',')
            next()
        } else return
        if (chunk.startsWith("Size=")) {
            node.size put line.substring(4 + 1).split(',')
            next()
        } else return
    } else
        if (!done && chunk.startsWith("SizeRef=")) {
            node.sizeRef put line.substring(7 + 1).split(',')
            next()
        }
    if (!done && chunk.startsWith("Split=")) {
        val c = chunk[6]
        if (c == 'X') node.splitAxis = Axis.X else if (c == 'Y') node.splitAxis = Axis.Y
        next()
    }
    if (!done && chunk.startsWith("NoResize=")) {
        if (chunk[9 + 1] != '0') node.flags = node.flags or DockNodeFlag.NoResize
        next()
    }
    if (!done && chunk.startsWith("CentralNode=")) {
        if (chunk[11 + 1] != '0') node.flags = node.flags or DockNodeFlag._CentralNode
        next()
    }
    if (!done && chunk.startsWith("NoTabBar=")) {
        if (chunk[8 + 1] != '0') node.flags = node.flags or DockNodeFlag._NoTabBar
        next()
    }
    if (!done && chunk.startsWith("HiddenTabBar=")) {
        if (chunk[12 + 1] != '0') node.flags = node.flags or DockNodeFlag._HiddenTabBar
        next()
    }
    if (!done && chunk.startsWith("NoWindowMenuButton=")) {
        if (chunk[18 + 1] != '0') node.flags = node.flags or DockNodeFlag._NoWindowMenuButton
        next()
    }
    if (!done && chunk.startsWith("NoCloseButton=")) {
        if (chunk[13 + 1] != '0') node.flags = node.flags or DockNodeFlag._NoCloseButton
        next()
    }
    if (!done && chunk.startsWith("Selected=0x")) {
        node.selectedWindowId == chunk.substring(8 + 1 + 2).toInt(16)
        next()
    }
    val dc = ctx.dockContext!!
    if (node.parentNodeId != 0)
        dockSettingsFindNodeSettings(ctx, node.parentNodeId)?.let { parentSettings ->
            node.depth = parentSettings.depth + 1
        }
    dc.settingsNodes += node
}

fun dockSettingsHandler_WriteAll(ctx: Context, handler: SettingsHandler, buf: StringBuilder) {

    val g = ctx
    val dc = g.dockContext!!
    if (g.io.configFlags hasnt ConfigFlag.DockingEnable)
        return

    // Gather settings data
    // (unlike our windows settings, because nodes are always built we can do a full rewrite of the SettingsNode buffer)
    dc.settingsNodes.clear()
    dc.settingsNodes.ensureCapacity(dc.nodes.size)
    for (node in dc.nodes.values)
        if (node.isRootNode)
            dockSettingsHandler_DockNodeToSettings(dc, node, 0)

    val maxDepth = dc.settingsNodes.maxBy { it.depth }?.depth ?: 0

    // Write to text buffer
    buf += "[${handler.typeName}][Data]\n"
    for (nodeSettings in dc.settingsNodes) {
        val lineStartPos = buf.length
        buf += " ".repeat(nodeSettings.depth * 2) + if (nodeSettings.flags has DockNodeFlag._DockSpace) "DockSpace" else "DockNode " + " ".repeat((maxDepth - nodeSettings.depth) * 2)  // Text align nodes to facilitate looking at .ini file
        buf += " ID=0x${nodeSettings.id.asHexString}"
        if (nodeSettings.parentNodeId != 0) {
            buf += " Parent=0x${nodeSettings.parentNodeId.asHexString} SizeRef=${nodeSettings.sizeRef.x.i},${nodeSettings.sizeRef.y.i}"
        } else {
            if (nodeSettings.parentWindowId != 0)
                buf += " Window=0x${nodeSettings.parentWindowId.asHexString}"
            buf += " Pos=${nodeSettings.pos.x.i},${nodeSettings.pos.y.i} Size=${nodeSettings.size.x.i},${nodeSettings.size.y.i}"
        }
        if (nodeSettings.splitAxis != Axis.None)
            buf += " Split=${nodeSettings.splitAxis.name}"
        if (nodeSettings.flags has DockNodeFlag.NoResize)
            buf += " NoResize=1"
        if (nodeSettings.flags has DockNodeFlag._CentralNode)
            buf += " CentralNode=1"
        if (nodeSettings.flags has DockNodeFlag._NoTabBar)
            buf += " NoTabBar=1"
        if (nodeSettings.flags has DockNodeFlag._HiddenTabBar)
            buf += " HiddenTabBar=1"
        if (nodeSettings.flags has DockNodeFlag._NoWindowMenuButton)
            buf += " NoWindowMenuButton=1"
        if (nodeSettings.flags has DockNodeFlag._NoCloseButton)
            buf += " NoCloseButton=1"
        if (nodeSettings.selectedWindowId != 0)
            buf += " Selected=0x${nodeSettings.selectedWindowId.asHexString}"

        if(IMGUI_DEBUG_INI_SETTINGS)
            // [DEBUG] Include comments in the .ini file to ease debugging
            dockContextFindNodeByID(ctx, nodeSettings.id)?.let { node ->
                buf += " ".repeat(2 max ((lineStartPos + 92) - buf.length))     // Align everything
                if (node.isDockSpace)
                    node.hostWindow?.parentWindow?.let {
                        buf += " ; in '${it.name}'"
                    }
                // Iterate settings so we can give info about windows that didn't exist during the session.
                var containsWindow = 0
                for (settings in g.settingsWindows)
                    if (settings.dockId == nodeSettings.id) {
                    if (containsWindow++ == 0)
                        buf += " ; contains "
                    buf += "'${settings.name}' "
                }
            }
        buf += '\n'
    }
    buf += '\n'
}

fun dockSettingsHandler_DockNodeToSettings(dc: DockContext, node: DockNode, depth: Int) {
    val nodeSettings = DockNodeSettings()
    assert(depth < (1 shl (1 /*sizeof(nodeSettings.Depth)*/ shl 3)))
    nodeSettings.id = node.id
    nodeSettings.parentNodeId = node.parentNode?.id ?: 0
    nodeSettings.parentWindowId = if (node.isDockSpace) node.hostWindow?.parentWindow?.id ?: 0 else 0
    nodeSettings.selectedWindowId = node.selectedTabId
    nodeSettings.splitAxis = if (node.isSplitNode) node.splitAxis else Axis.None
    nodeSettings.depth = depth
    nodeSettings.flags = node.localFlags and DockNodeFlag._SavedFlagsMask_
    nodeSettings.pos put node.pos
    nodeSettings.size put node.size
    nodeSettings.sizeRef put node.sizeRef
    dc.settingsNodes += nodeSettings
    node.childNodes[0]?.let { dockSettingsHandler_DockNodeToSettings(dc, it, depth + 1) }
    node.childNodes[1]?.let { dockSettingsHandler_DockNodeToSettings(dc, it, depth + 1) }
}