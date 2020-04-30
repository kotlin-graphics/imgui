package imgui.static

import glm_.i
import imgui.*
import imgui.ImGui.dockBuilderRemoveNodeChildNodes
import imgui.ImGui.dockBuilderRemoveNodeDockedWindows
import imgui.ImGui.dockContextGenNodeID
import imgui.ImGui.dockNodeGetRootNode
import imgui.ImGui.findWindowByName
import imgui.ImGui.findWindowSettings
import imgui.ImGui.markIniSettingsDirty
import imgui.api.gImGui
import imgui.classes.Context
import imgui.internal.Axis
import imgui.internal.classes.*

// ImGuiDockContext

//-----------------------------------------------------------------------------
// Docking: ImGuiDockContext
//-----------------------------------------------------------------------------
// The lifetime model is different from the one of regular windows: we always create a ImGuiDockNode for each ImGuiDockNodeSettings,
// or we always hold the entire docking node tree. Nodes are frequently hidden, e.g. if the window(s) or child nodes they host are not active.
// At boot time only, we run a simple GC to remove nodes that have no references.
// Because dock node settings (which are small, contiguous structures) are always mirrored by their corresponding dock nodes (more complete structures),
// we can also very easily recreate the nodes from scratch given the settings data (this is what DockContextRebuild() does).
// This is convenient as docking reconfiguration can be implemented by mostly poking at the simpler settings data.
//-----------------------------------------------------------------------------
// - DockContextInitialize()
// - DockContextShutdown()
// - DockContextOnLoadSettings()
// - DockContextClearNodes()
// - DockContextRebuildNodes()
// - DockContextUpdateUndocking()
// - DockContextUpdateDocking()
// - DockContextFindNodeByID()
// - DockContextBindNodeToWindow()
// - DockContextGenNodeID()
// - DockContextAddNode()
// - DockContextRemoveNode()
// - ImGuiDockContextPruneNodeData
// - DockContextPruneUnusedSettingsNodes()
// - DockContextBuildNodesFromSettings()
// - DockContextBuildAddWindowsToNodes()
//-----------------------------------------------------------------------------

fun dockContextAddNode(ctx: Context, id_: ID): DockNode {

    var id = id_
    // Generate an ID for the new node (the exact ID value doesn't matter as long as it is not already used) and add the first window.
    if (id == 0)
        id = dockContextGenNodeID(ctx)
    else
        assert(dockContextFindNodeByID(ctx, id) == null)

    // We don't set node->LastFrameAlive on construction. Nodes are always created at all time to reflect .ini settings!
    IMGUI_DEBUG_LOG_DOCKING("DockContextAddNode 0x%08X".format(id))
    val node = DockNode(id)
    ctx.dockContext!!.nodes[node.id] = node
    return node
}

fun dockContextRemoveNode(ctx: Context, node: DockNode, mergeSiblingIntoParentNode: Boolean) {

    val g = ctx
    val dc = ctx.dockContext!!

    IMGUI_DEBUG_LOG_DOCKING("DockContextRemoveNode 0x%08X".format(node.id))
    assert(dockContextFindNodeByID(ctx, node.id) === node)
    assert(node.childNodes.all { it == null })
    assert(node.windows.isEmpty())

    node.hostWindow?.dockNodeAsHost = null

    val parentNode = node.parentNode
    val merge = mergeSiblingIntoParentNode && parentNode != null
    if (merge) {
        assert(parentNode!!.childNodes.all { it === node })
        val siblingNode = if (parentNode.childNodes[0] === node) parentNode.childNodes[1] else parentNode.childNodes[0]
        dockNodeTreeMerge(g, parentNode, siblingNode!!)
    } else {
        if (parentNode != null)
            for (n in parentNode.childNodes.indices)
                if (parentNode.childNodes[n] === node)
                    node.parentNode!!.childNodes[n] = null
        dc.nodes -= node.id // TODO check
    }
}

fun dockContextQueueNotifyRemovedNode(ctx: Context, node: DockNode) {
    ctx.dockContext!!.requests.forEach {
        if (it.dockTargetNode === node)
            it.type = DockRequestType.None
    }
}

fun dockContextProcessDock(ctx: Context, req: DockRequest) {

    assert((req.type == DockRequestType.Dock && req.dockPayload != null) || (req.type == DockRequestType.Split && req.dockPayload == null))
    assert(req.dockTargetWindow != null || req.dockTargetNode != null)

    val g = ctx

    val payloadWindow = req.dockPayload     // Optional
    val targetWindow = req.dockTargetWindow
    var node = req.dockTargetNode
    val log = if (payloadWindow != null) "target '${targetWindow?.name}' dock window '${payloadWindow.name}'" else ""
    IMGUI_DEBUG_LOG_DOCKING("DockContextProcessDock node 0x%08X$log, split_dir ${req.dockSplitDir}".format(node?.id
            ?: 0))

    // Decide which Tab will be selected at the end of the operation
    var nextSelectedId: ID = 0
    var payloadNode: DockNode? = null
    if (payloadWindow != null) {
        payloadNode = payloadWindow.dockNodeAsHost
        payloadWindow.dockNodeAsHost = null // Important to clear this as the node will have its life as a child which might be merged/deleted later.
        if (payloadNode?.isLeafNode == true)
            nextSelectedId = payloadNode.tabBar!!.run { nextSelectedTabId.takeIf { it != 0 } ?: selectedTabId }
        if (payloadNode == null)
            nextSelectedId = payloadWindow.id
    }

    // FIXME-DOCK: When we are trying to dock an existing single-window node into a loose window, transfer Node ID as well
    // When processing an interactive split, usually LastFrameAlive will be < g.FrameCount. But DockBuilder operations can make it ==.
    node?.let { nd ->
        assert(nd.lastFrameAlive <= g.frameCount)
        targetWindow?.let {
            if (nd === targetWindow.dockNodeAsHost)
                assert(nd.windows.isNotEmpty() || nd.isSplitNode || nd.isCentralNode)
        }
    }
    // Create new node and add existing window to it
    if (node == null) {
        node = dockContextAddNode(ctx, 0)
        node.pos put targetWindow!!.pos
        node.size put targetWindow.size
        if (targetWindow.dockNodeAsHost == null) {
            dockNodeAddWindow(node, targetWindow, true)
            node.tabBar!!.tabs[0].flags = node.tabBar!!.tabs[0].flags wo TabItemFlag._Unsorted
            targetWindow.dockIsActive = true
        }
    }

    val splitDir = req.dockSplitDir
    if (splitDir != Dir.None) {
        // Split into one, one side will be our payload node unless we are dropping a loose window
        val splitAxis = if (splitDir == Dir.Left || splitDir == Dir.Right) Axis.X else Axis.Y
        val splitInheritorChildIdx = (splitDir == Dir.Left || splitDir == Dir.Up).i // Current contents will be moved to the opposite side
        val splitRatio = req.dockSplitRatio
        dockNodeTreeSplit(ctx, node, splitAxis, splitInheritorChildIdx, splitRatio, payloadNode)  // payload_node may be NULL here!
        val newNode = node.childNodes[splitInheritorChildIdx xor 1]!!
        newNode.hostWindow = node.hostWindow
        node = newNode
    }
    node.localFlags = node.localFlags wo DockNodeFlag._HiddenTabBar

    if (node !== payloadNode) {
        // Create tab bar before we call DockNodeMoveWindows (which would attempt to move the old tab-bar, which would lead us to payload tabs wrongly appearing before target tabs!)
        if (node.windows.isNotEmpty() && node.tabBar == null) {
            dockNodeAddTabBar(node)
            node.windows.forEach {
                node.tabBar!!.addTab(TabItemFlag.None.i, it)
            }
        }

        val plNode = payloadNode
        val plWindow = payloadWindow
        if (plNode != null) {
            // Transfer full payload node (with 1+ child windows or child nodes)
            if (plNode.isSplitNode) {
                if (node.windows.isNotEmpty()) {
                    // We can dock a split payload into a node that already has windows _only_ if our payload is a node tree with a single visible node.
                    // In this situation, we move the windows of the target node into the currently visible node of the payload.
                    // This allows us to preserve some of the underlying dock tree settings nicely.
                    assert(plNode.onlyNodeWithWindows != null) { "The docking should have been blocked by DockNodePreviewDockSetup() early on and never submitted." }
                    val visibleNode = plNode.onlyNodeWithWindows!!
                    visibleNode.tabBar?.let { assert(it.tabs.isNotEmpty()) }
                    dockNodeMoveWindows(node, visibleNode)
                    dockNodeMoveWindows(visibleNode, node)
                    dockSettingsRenameNodeReferences(node.id, visibleNode.id)
                }
                if (node.isCentralNode) {
                    // Central node property needs to be moved to a leaf node, pick the last focused one.
                    // FIXME-DOCK: If we had to transfer other flags here, what would the policy be?
                    val lastFocusedNode = dockContextFindNodeByID(ctx, plNode.lastFocusedNodeId)!! // ~assert(lastFocusedNode != null)
                    val lastFocusedRootNode = dockNodeGetRootNode(lastFocusedNode)
                    assert(lastFocusedRootNode === dockNodeGetRootNode(plNode))
                    lastFocusedNode.localFlags = lastFocusedNode.localFlags or DockNodeFlag._CentralNode
                    node.localFlags = node.localFlags wo DockNodeFlag._CentralNode
                    lastFocusedRootNode.centralNode = lastFocusedNode
                }

                assert(node.windows.isEmpty())
                dockNodeMoveChildNodes(node, plNode)
            } else {
                val payloadDockId = plNode.id
                dockNodeMoveWindows(node, plNode)
                dockSettingsRenameNodeReferences(payloadDockId, node.id)
            }
            dockContextRemoveNode(ctx, plNode, true)
        } else if (plWindow != null) {
            // Transfer single window
            val payloadDockId = plWindow.dockId
            node.visibleWindow = plWindow
            dockNodeAddWindow(node, plWindow, true)
            if (payloadDockId != 0)
                dockSettingsRenameNodeReferences(payloadDockId, node.id)
        }
    } else // When docking a floating single window node we want to reevaluate auto-hiding of the tab bar
        node.wantHiddenTabBarUpdate = true

    // Update selection immediately
    node.tabBar?.let { it.nextSelectedTabId = nextSelectedId }
    markIniSettingsDirty()
}

fun dockContextProcessUndockWindow(ctx: Context, window: Window, clearPersistentDockingRef: Boolean = true) {
    val dockNode = window.dockNode
    if (dockNode != null)
        dockNodeRemoveWindow(dockNode, window, if (clearPersistentDockingRef) 0 else window.dockId)
    else
        window.dockId = 0
    window.collapsed = false
    window.dockIsActive = false
    window.dockTabIsVisible = false
    markIniSettingsDirty()
}

fun dockContextProcessUndockNode(ctx: Context, node_: DockNode) {
    var node = node_
    assert(node.isLeafNode)
    assert(node.windows.size >= 1)

    if (node.isRootNode || node.isCentralNode) {
        // In the case of a root node or central node, the node will have to stay in place. Create a new node to receive the payload.
        val newNode = dockContextAddNode(ctx, 0)
        dockNodeMoveWindows(newNode, node)
        dockSettingsRenameNodeReferences(node.id, newNode.id)
        newNode.windows.forEach { it.updateParentAndRootLinks(it.flags, null) }
        node = newNode
    } else {
        val parent = node.parentNode!!
        // Otherwise extract our node and merging our sibling back into the parent node.
        assert(parent.childNodes.all { it === node })
        val indexInParent = (parent.childNodes[0] !== node).i
        parent.childNodes[indexInParent] = null
        dockNodeTreeMerge(ctx, parent, parent.childNodes[indexInParent xor 1]!!)
        parent.authorityForViewport = DataAuthority.Window // The node that stays in place keeps the viewport, so our newly dragged out node will create a new viewport
        node.parentNode = null
    }
    node.authorityForPos = DataAuthority.Window
    node.authorityForSize = DataAuthority.Window
    node.wantMouseMove = true
    markIniSettingsDirty()
}

/** Pre C++0x doesn't allow us to use a function-local type (without linkage) as template parameter, so we moved this here. */
class DockContextPruneNodeData {
    var countWindows = 0
    var countChildWindows = 0
    var countChildNodes = 0
    var rootId: ID = 0
}

/** Garbage collect unused nodes (run once at init time) */
fun dockContextPruneUnusedSettingsNodes(ctx: Context) {
    val g = ctx
    val dc = ctx.dockContext!!
    assert(g.windows.isEmpty())

    val pool = HashMap<ID, DockContextPruneNodeData>(dc.settingsNodes.size)

    // Count child nodes and compute RootID
    dc.settingsNodes.forEach {
        val parentData = if (it.parentNodeId != 0) pool[it.parentNodeId] else null
        pool.getOrPut(it.id) { DockContextPruneNodeData() }.rootId = parentData?.rootId ?: it.id
        if (it.parentNodeId != 0)
            pool.getOrPut(it.parentNodeId) { DockContextPruneNodeData() }.countChildNodes++
    }

    // Count reference to dock ids from dockspaces
    // We track the 'auto-DockNode <- manual-Window <- manual-DockSpace' in order to avoid 'auto-DockNode' being ditched by DockContextPruneUnusedSettingsNodes()
    dc.settingsNodes.forEach {
        if (it.parentWindowId != 0)
            findWindowSettings(it.parentWindowId)?.let { windowSettings ->
                if (windowSettings.dockId != 0)
                    pool[windowSettings.dockId]?.run { countChildNodes++ }
            }
    }

    // Count reference to dock ids from window settings
    // We guard against the possibility of an invalid .ini file (RootID may point to a missing node)
    for (setting in g.settingsWindows) {
        val dockId = setting.dockId
        if (dockId != 0)
            pool[dockId]?.let { data ->
                data.countWindows++
                val dataRoot = data.takeIf { it.rootId == dockId } ?: pool[data.rootId]
                dataRoot?.run { countChildWindows++ }
            }
    }

    // Prune
    for (setting in dc.settingsNodes) {
        val data = pool[setting.id]!!
        if (data.countWindows > 1)
            continue
        val dataRoot = data.takeIf { it.rootId == setting.id } ?: pool[data.rootId]!!

        var remove = false
        remove = remove || (data.countWindows == 1 && setting.parentNodeId == 0 && data.countChildNodes == 0 && setting.flags hasnt DockNodeFlag._CentralNode)  // Floating root node with only 1 window
        remove = remove || (data.countWindows == 0 && setting.parentNodeId == 0 && data.countChildNodes == 0) // Leaf nodes with 0 window
        remove = remove || dataRoot.countChildWindows == 0
        if (remove) {
            IMGUI_DEBUG_LOG_DOCKING("DockContextPruneUnusedSettingsNodes: Prune 0x%08X".format(setting.id))
            dockSettingsRemoveNodeReferences(arrayOf(setting.id))
            setting.id = 0
        }
    }
}

fun dockContextFindNodeByID(ctx: Context, id: ID): DockNode? = ctx.dockContext!!.nodes[id]

fun dockContextBindNodeToWindow(ctx: Context, window: Window): DockNode? {

    val g = ctx
    var node = dockContextFindNodeByID(ctx, window.dockId)
    assert(window.dockNode == null)

    // We should not be docking into a split node (SetWindowDock should avoid this)
    if (node?.isSplitNode == true) {
        dockContextProcessUndockWindow(ctx, window)
        return null
    }

    // Create node
    if (node == null)
        node = dockContextAddNode(ctx, window.dockId).apply {
            setAuthorities(DataAuthority.Window)
            lastFrameAlive = g.frameCount
        }

    // If the node just turned visible and is part of a hierarchy, it doesn't have a Size assigned by DockNodeTreeUpdatePosSize() yet,
    // so we're forcing a Pos/Size update from the first ancestor that is already visible (often it will be the root node).
    // If we don't do this, the window will be assigned a zero-size on its first frame, which won't ideally warm up the layout.
    // This is a little wonky because we don't normally update the Pos/Size of visible node mid-frame.
    if (!node.isVisible) {
        var ancestorNode: DockNode = node
        while (!ancestorNode.isVisible) {
            ancestorNode.isVisible = true
            ancestorNode.markedForPosSizeWrite = true
            ancestorNode.parentNode?.let {
                ancestorNode = it
            }
        }
        assert(ancestorNode.size.x > 0f && ancestorNode.size.y > 0f) // TODO glm
        dockNodeTreeUpdatePosSize(ancestorNode, ancestorNode.pos, ancestorNode.size, true)
    }

    // Add window to node
    dockNodeAddWindow(node, window, true)
    assert(node === window.dockNode)
    return node
}

/** Use root_id==0 to clear all */
fun dockContextClearNodes(ctx: Context, rootId: ID, clearPersistentDockingReferences: Boolean) {
    assert(ctx === gImGui)
    dockBuilderRemoveNodeDockedWindows(rootId, clearPersistentDockingReferences)
    dockBuilderRemoveNodeChildNodes(rootId)
}

fun dockContextBuildNodesFromSettings(ctx: Context, nodeSettings: ArrayList<DockNodeSettings>) {
    // Build nodes
    for (setting in nodeSettings) {
        if (setting.id == 0)
            continue
        val node = dockContextAddNode(ctx, setting.id).apply {
            parentNode = if (setting.parentNodeId != 0) dockContextFindNodeByID(ctx, setting.parentNodeId) else null
            pos put setting.pos
            size put setting.size
            sizeRef put setting.sizeRef
            setAuthorities(DataAuthority.DockNode)
            parentNode?.let { parent ->
                if (parent.childNodes[0] == null)
                    parent.childNodes[0] = this
                else if (parent.childNodes[1] == null)
                    parent.childNodes[1] = this
            }
            selectedTabId = setting.selectedWindowId
            splitAxis = setting.splitAxis
            localFlags = localFlags or (setting.flags and DockNodeFlag._SavedFlagsMask_)
        }
        // Bind host window immediately if it already exist (in case of a rebuild)
        // This is useful as the RootWindowForTitleBarHighlight links necessary to highlight the currently focused node requires node->HostWindow to be set.
        val rootNode = dockNodeGetRootNode(node)
        node.hostWindow = findWindowByName(dockNodeGetHostWindowTitle(rootNode))
    }
}

/** Use root_id==0 to add all */
fun dockContextBuildAddWindowsToNodes(ctx: Context, rootId: ID){
    // Rebind all windows to nodes (they can also lazily rebind but we'll have a visible glitch during the first frame)
    val g = ctx
    for (window in g.windows) {
        if (window.dockId == 0 || window.lastFrameActive < g.frameCount - 1)
            continue
        if (window.dockNode != null)
            continue

        val node = dockContextFindNodeByID(ctx, window.dockId)!! //~IM_ASSERT(node != NULL)   // This should have been called after DockContextBuildNodesFromSettings()
        if (rootId == 0 || dockNodeGetRootNode(node).id == rootId)
        dockNodeAddWindow(node, window, true)
    }
}
