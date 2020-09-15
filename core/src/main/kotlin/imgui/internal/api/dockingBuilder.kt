package imgui.internal.api

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.createNewWindowSettings
import imgui.ImGui.dockNodeGetRootNode
import imgui.ImGui.dockSpace
import imgui.ImGui.findOrCreateWindowSettings
import imgui.ImGui.findWindowByID
import imgui.ImGui.findWindowByName
import imgui.ImGui.findWindowSettings
import imgui.ImGui.setWindowDock
import imgui.api.g
import imgui.internal.classes.DataAuthority
import imgui.internal.classes.DockNode
import imgui.internal.classes.DockRequest
import imgui.internal.classes.DockRequestType
import imgui.internal.hash
import imgui.internal.saturate
import imgui.static.*

// Docking - Builder function needs to be generally called before the node is used/submitted.
// - The DockBuilderXXX functions are designed to _eventually_ become a public API, but it is too early to expose it and guarantee stability.
// - Do not hold on ImGuiDockNode* pointers! They may be invalidated by any split/merge/remove operation and every frame.
// - To create a DockSpace() node, make sure to set the ImGuiDockNodeFlags_DockSpace flag when calling DockBuilderAddNode().
//   You can create dockspace nodes (attached to a window) _or_ floating nodes (carry its own window) with this API.
// - DockBuilderSplitNode() create 2 child nodes within 1 node. The initial node becomes a parent node.
// - If you intend to split the node immediately after creation using DockBuilderSplitNode(), make sure
//   to call DockBuilderSetNodeSize() beforehand. If you don't, the resulting split sizes may not be reliable.
// - Call DockBuilderFinish() after you are done.

//-----------------------------------------------------------------------------
// Docking: Builder Functions
//-----------------------------------------------------------------------------
// Very early end-user API to manipulate dock nodes.
// Only available in imgui_internal.h. Expect this API to change/break!
// It is expected that those functions are all called _before_ the dockspace node submission.
//-----------------------------------------------------------------------------
// - DockBuilderDockWindow()
// - DockBuilderGetNode()
// - DockBuilderSetNodePos()
// - DockBuilderSetNodeSize()
// - DockBuilderAddNode()
// - DockBuilderRemoveNode()
// - DockBuilderRemoveNodeChildNodes()
// - DockBuilderRemoveNodeDockedWindows()
// - DockBuilderSplitNode()
// - DockBuilderCopyNodeRec()
// - DockBuilderCopyNode()
// - DockBuilderCopyWindowSettings()
// - DockBuilderCopyDockSpace()
// - DockBuilderFinish()
//-----------------------------------------------------------------------------

interface dockingBuilder {

    fun dockBuilderDockWindow(windowName: String, nodeId: ID) {
        // We don't preserve relative order of multiple docked windows (by clearing DockOrder back to -1)
        val windowId = hash(windowName)
        val window = findWindowByID(windowId)
        if (window != null) {
            // Apply to created window
            setWindowDock(window, nodeId, Cond.Always)
            window.dockOrder = -1
        } else {
            // Apply to settings
            val settings = findWindowSettings(windowId) ?: createNewWindowSettings(windowName)
            settings.dockId = nodeId
            settings.dockOrder = -1
        }
    }

    fun dockBuilderGetNode(nodeId: ID): DockNode? = dockContextFindNodeByID(g, nodeId)

    fun dockBuilderGetCentralNode(nodeId: ID): DockNode? = dockBuilderGetNode(nodeId)?.let(::dockNodeGetRootNode)

    /** Make sure to use the ImGuiDockNodeFlags_DockSpace flag to create a dockspace node! Otherwise this will create a floating node!
     * - Floating node: you can then call DockBuilderSetNodePos()/DockBuilderSetNodeSize() to position and size the floating node.
     * - Dockspace node: calling DockBuilderSetNodePos() is unnecessary.
     * - If you intend to split a node immediately after creation using DockBuilderSplitNode(), make sure to call DockBuilderSetNodeSize() beforehand!
     *   For various reason, the splitting code currently needs a base size otherwise space may not be allocated as precisely as you would expect.
     * - Use (id == 0) to let the system allocate a node identifier. */
    fun dockBuilderAddNode(id: ID = 0, flags: DockNodeFlags = DockNodeFlag.None.i): ID {
        val ctx = g
        var node: DockNode? = null
        if (flags has DockNodeFlag._DockSpace) {
            dockSpace(id, Vec2(), (flags wo DockNodeFlag._DockSpace) or DockNodeFlag.KeepAliveOnly)
            node = dockContextFindNodeByID(ctx, id)
        } else {
            if (id != 0)
                node = dockContextFindNodeByID(ctx, id)
            if (node == null)
                node = dockContextAddNode(ctx, id)
            node.localFlags = flags
        }
        node!!.lastFrameAlive = ctx.frameCount   // Set this otherwise BeginDocked will undock during the same frame.
        return node.id
    }

    /** Remove node and all its child, undock all windows */
    fun dockBuilderRemoveNode(nodeId: ID) {
        val ctx = g
        val node = dockContextFindNodeByID(ctx, nodeId) ?: return
        dockBuilderRemoveNodeDockedWindows(nodeId, true)
        dockBuilderRemoveNodeChildNodes(nodeId)
        if (node.isCentralNode)
            node.parentNode?.run { localFlags = localFlags or DockNodeFlag._CentralNode }
        dockContextRemoveNode(ctx, node, true)
    }

    fun dockBuilderRemoveNodeDockedWindows(rootId: ID, clearPersistentDockingReferences: Boolean = true) {
        // Clear references in settings
        val ctx = g
        if (clearPersistentDockingReferences)
            for (setting in g.settingsWindows) {
                var wantRemoval = rootId == 0 || setting.dockId == rootId
                if (!wantRemoval && setting.dockId != 0)
                    dockContextFindNodeByID(ctx, setting.dockId)?.let {
                        if (dockNodeGetRootNode(it).id == rootId)
                            wantRemoval = true
                    }
                if (wantRemoval)
                    setting.dockId = 0
            }

        // Clear references in windows
        for (window in g.windows) {
            val dock = window.dockNode
            val host = window.dockNodeAsHost
            val wantRemoval = rootId == 0 || (dock != null && dockNodeGetRootNode(dock).id == rootId) || (host != null && host.id == rootId)
            if (wantRemoval) {
                val backupDockId = window.dockId
                dockContextProcessUndockWindow(ctx, window, clearPersistentDockingReferences)
                if (!clearPersistentDockingReferences)
                    assert(window.dockId == backupDockId)
            }
        }
    }

    /** Remove all split/hierarchy. All remaining docked windows will be re-docked to the root. */
    fun dockBuilderRemoveNodeChildNodes(rootId: ID) {

        val ctx = g
        val dc = ctx.dockContext!!

        val rootNode = if (rootId != 0) dockContextFindNodeByID(ctx, rootId) else null
        if (rootId != 0 && rootNode == null)
            return
        var hasCentralNode = false

        val backupRootNodeAuthorityForPos = rootNode?.authorityForPos ?: DataAuthority.Auto
        val backupRootNodeAuthorityForSize = rootNode?.authorityForSize ?: DataAuthority.Auto

        // Process active windows
        val nodesToRemove = ArrayList<DockNode>()
        for (node in dc.nodes.values) {
            val wantRemoval = rootId == 0 || (node.id != rootId && dockNodeGetRootNode(node).id == rootId)
            if (wantRemoval) {
                if (node.isCentralNode)
                    hasCentralNode = true
                if (rootId != 0)
                    dockContextQueueNotifyRemovedNode(ctx, node)
                rootNode?.let { dockNodeMoveWindows(it, node) }
                nodesToRemove += node
            }
        }

        // DockNodeMoveWindows->DockNodeAddWindow will normally set those when reaching two windows (which is only adequate during interactive merge)
        // Make sure we don't lose our current pos/size. (FIXME-DOCK: Consider tidying up that code in DockNodeAddWindow instead)
        rootNode?.apply {
            authorityForPos = backupRootNodeAuthorityForPos
            authorityForSize = backupRootNodeAuthorityForSize
        }

        // Apply to settings
        for (setting in ctx.settingsWindows) {
            val windowSettingsDockId: ID = setting.dockId
            if (windowSettingsDockId != 0)
                for (nodeToRemove in nodesToRemove)
                    if (nodeToRemove.id == windowSettingsDockId) {
                        setting.dockId = rootId
                        break
                    }
        }

        // Not really efficient, but easier to destroy a whole hierarchy considering DockContextRemoveNode is attempting to merge nodes
        if (nodesToRemove.size > 1)
            nodesToRemove.sortWith(dockNodeComparerDepthMostFirst)
        for (nodeToRemove in nodesToRemove)
            dockContextRemoveNode(ctx, nodeToRemove, false)

        if (rootId == 0) {
            dc.nodes.clear()
            dc.requests.clear()
        } else if (hasCentralNode)
            rootNode!!.run {
                localFlags = localFlags or DockNodeFlag._CentralNode
                centralNode = this
            }
    }

    fun dockBuilderSetNodePos(nodeId: ID, pos: Vec2) {
        val ctx = g
        val node = dockContextFindNodeByID(ctx, nodeId) ?: return
        node.pos put pos
        node.authorityForPos = DataAuthority.DockNode
    }

    fun dockBuilderSetNodeSize(nodeId: ID, size: Vec2) {
        val ctx = g
        val node = dockContextFindNodeByID(ctx, nodeId) ?: return
        assert(size.x > 0f && size.y > 0f)  // TODO glm
        node.size put size
        node.sizeRef put size
        node.authorityForSize = DataAuthority.DockNode
    }

    /** Create 2 child nodes in this parent node.
     *
     *  If 'out_id_at_dir' or 'out_id_at_opposite_dir' are non NULL, the function will write out the ID of the two new nodes created.
     * Return value is ID of the node at the specified direction, so same as (*out_id_at_dir) if that pointer is set.
     * FIXME-DOCK: We are not exposing nor using split_outer.
     *
     * @return [outIdAtDir, outIdAtOppositeDir] */
    fun dockBuilderSplitNode(id: ID, splitDir: Dir, sizeRatioForNodeAtDir: Float, outIds: IntArray?): ID {
        val ctx = g
        assert(splitDir != Dir.None)
        IMGUI_DEBUG_LOG_DOCKING("DockBuilderSplitNode node 0x%08X, split_dir $splitDir".format(id))

        val node = dockContextFindNodeByID(ctx, id) ?: return 0
//        if (node == null) {
//            assert(node != null)
//            return 0
//        }

        assert(!node.isSplitNode) { "Assert if already Split" }

        val req = DockRequest()
        req.type = DockRequestType.Split
        req.dockTargetWindow = null
        req.dockTargetNode = node
        req.dockPayload = null
        req.dockSplitDir = splitDir
        req.dockSplitRatio = saturate(if (splitDir == Dir.Left || splitDir == Dir.Up) sizeRatioForNodeAtDir else 1f - sizeRatioForNodeAtDir)
        req.dockSplitOuter = false
        dockContextProcessDock(ctx, req)

        val idAtDir = node.childNodes[if (splitDir == Dir.Left || splitDir == Dir.Up) 0 else 1]!!.id
        val idAtOppositeDir = node.childNodes[if (splitDir == Dir.Left || splitDir == Dir.Up) 1 else 0]!!.id
        outIds?.apply {
            set(0, idAtDir)
            set(1, idAtOppositeDir)
        }
        return idAtDir
    }

    /** FIXME: Will probably want to change this signature, in particular how the window remapping pairs are passed. */
    fun dockBuilderCopyDockSpace(srcDockspaceId: ID, dstDockspaceId: ID, inWindowRemapPairs: ArrayList<String>) {
        assert(srcDockspaceId != 0)
        assert(dstDockspaceId != 0)
//        assert(inWindowRemapPairs != NULL)
        assert(inWindowRemapPairs.size % 2 == 0)

        // Duplicate entire dock
        // FIXME: When overwriting dst_dockspace_id, windows that aren't part of our dockspace window class but that are docked in a same node will be split apart,
        // whereas we could attempt to at least keep them together in a new, same floating node.
        val nodeRemapPairs = ArrayList<ID>()
        dockBuilderCopyNode(srcDockspaceId, dstDockspaceId, nodeRemapPairs)

        // Attempt to transition all the upcoming windows associated to dst_dockspace_id into the newly created hierarchy of dock nodes
        // (The windows associated to src_dockspace_id are staying in place)
        val srcWindows = ArrayList<ID>()
        for (remapWindowN in inWindowRemapPairs.indices step 2) {
            val srcWindowName = inWindowRemapPairs[remapWindowN]
            val dstWindowName = inWindowRemapPairs[remapWindowN + 1]
            val srcWindowId = hash(srcWindowName)
            srcWindows += srcWindowId

            // Search in the remapping tables
            val srcDockId: ID = run {
                val srcWindow = findWindowByID(srcWindowId)
                when {
                    srcWindow != null -> srcWindow.dockId
                    else -> findWindowSettings(srcWindowId)?.dockId ?: 0
                }
            }
            var dstDockId: ID = 0
            for (dockRemapN in nodeRemapPairs.indices step 2)
                if (nodeRemapPairs[dockRemapN] == srcDockId) {
                    dstDockId = nodeRemapPairs[dockRemapN + 1]
                    //node_remap_pairs[dock_remap_n] = node_remap_pairs[dock_remap_n + 1] = 0; // Clear
                    break
                }

            if (dstDockId != 0) {
                // Docked windows gets redocked into the new node hierarchy.
                IMGUI_DEBUG_LOG_DOCKING("Remap live window '$srcWindowName' 0x%08X -> '$dstWindowName' 0x%08X".format(srcDockId, dstDockId))
                dockBuilderDockWindow(dstWindowName, dstDockId)
            } else {
                // Floating windows gets their settings transferred (regardless of whether the new window already exist or not)
                // When this is leading to a Copy and not a Move, we would get two overlapping floating windows. Could we possibly dock them together?
                IMGUI_DEBUG_LOG_DOCKING("Remap window settings '$srcWindowName' -> '$dstWindowName'")
                dockBuilderCopyWindowSettings(srcWindowName, dstWindowName)
            }
        }

        // Anything else in the source nodes of 'node_remap_pairs' are windows that were docked in src_dockspace_id but are not owned by it (unaffiliated windows, e.g. "ImGui Demo")
        // Find those windows and move to them to the cloned dock node. This may be optional?
        for (dockRemapN in nodeRemapPairs.indices step 2) {
            val srcDockId = nodeRemapPairs[dockRemapN]
            if (srcDockId != 0) {
                val dstDockId = nodeRemapPairs[dockRemapN + 1]
                val node = dockBuilderGetNode(srcDockId)!!
                for (window in node.windows) {
                    if (window.id in srcWindows)
                        continue

                    // Docked windows gets redocked into the new node hierarchy.
                    IMGUI_DEBUG_LOG_DOCKING("Remap window '${window.name}' %08X -> %08X".format(srcDockId, dstDockId))
                    dockBuilderDockWindow(window.name, dstDockId)
                }
            }
        }
    }

    fun dockBuilderCopyNodeRec(srcNode: DockNode, dstNodeIdIfKnown: ID, outNodeRemapPairs: ArrayList<ID>): DockNode {
        val ctx = g
        val dstNode = dockContextAddNode(ctx, dstNodeIdIfKnown)
        dstNode.sharedFlags = srcNode.sharedFlags
        dstNode.localFlags = srcNode.localFlags
        dstNode.pos put srcNode.pos
        dstNode.size put srcNode.size
        dstNode.sizeRef put srcNode.sizeRef
        dstNode.splitAxis = srcNode.splitAxis

        outNodeRemapPairs += srcNode.id
        outNodeRemapPairs += dstNode.id

        for (childN in srcNode.childNodes.indices)
            srcNode.childNodes[childN]?.let {
                dstNode.childNodes[childN] = dockBuilderCopyNodeRec(it, 0, outNodeRemapPairs).apply {
                    parentNode = dstNode
                }
        }

        IMGUI_DEBUG_LOG_DOCKING("Fork node %08X -> %08X (${if(dstNode.isSplitNode) 2 else 0} childs)".format(srcNode.id, dstNode.id))
        return dstNode
    }

    fun dockBuilderCopyNode(srcNodeId: ID, dstNodeId: ID, outNodeRemapPairs: ArrayList<ID>) {
        val ctx = g
        assert(srcNodeId != 0)
        assert(dstNodeId != 0)
//        assert(outNodeRemapPairs != NULL)

        val srcNode = dockContextFindNodeByID(ctx, srcNodeId)!! // ~IM_ASSERT(srcNode != NULL)

        outNodeRemapPairs.clear()
        dockBuilderRemoveNode(dstNodeId)
        dockBuilderCopyNodeRec(srcNode, dstNodeId, outNodeRemapPairs)

        assert(outNodeRemapPairs.size % 2 == 0)
    }

    fun dockBuilderCopyWindowSettings(srcName: String, dstName: String)    {
        val srcWindow = findWindowByName(srcName) ?: return
        val dstWindow = findWindowByName(dstName)
        if (dstWindow != null) {
                dstWindow.pos put srcWindow.pos
                dstWindow.size put srcWindow.size
                dstWindow.sizeFull put srcWindow.sizeFull
                dstWindow.collapsed = srcWindow.collapsed
            }
        else {
            val dstSettings = findOrCreateWindowSettings(dstName)
            val windowPos = srcWindow.pos
            if (srcWindow.viewportId != 0 && srcWindow.viewportId != IMGUI_VIEWPORT_DEFAULT_ID) {
                dstSettings.viewportPos put windowPos
                dstSettings.viewportId = srcWindow.viewportId
                dstSettings.pos put 0
            }
            else
                dstSettings.pos put windowPos
            dstSettings.size put srcWindow.sizeFull
            dstSettings.collapsed = srcWindow.collapsed
        }
    }

    fun dockBuilderFinish(rootId: ID) {
        val ctx = g
        //DockContextRebuild(ctx);
        dockContextBuildAddWindowsToNodes(ctx, rootId)
    }

    companion object {
        object dockNodeComparerDepthMostFirst : Comparator<DockNode> {
            override fun compare(a: DockNode, b: DockNode): Int = dockNodeGetDepth(b) - dockNodeGetDepth(a)
        }
    }
}