package imgui.static

import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.dockNodeGetRootNode
import imgui.ImGui.getID
import imgui.ImGui.markIniSettingsDirty
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.splitterBehavior
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.DataAuthority
import imgui.internal.classes.DockNode
import imgui.internal.classes.IMGUI_DOCK_SPLITTER_SIZE
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.sections.Axis
import imgui.internal.sections.set
import imgui.internal.sections.get

// ImGuiDockNode tree manipulations

//-----------------------------------------------------------------------------
// Docking: ImGuiDockNode Tree manipulation functions
//-----------------------------------------------------------------------------
// - DockNodeTreeSplit()
// - DockNodeTreeMerge()
// - DockNodeTreeUpdatePosSize()
// - DockNodeTreeUpdateSplitterFindTouchingNode()
// - DockNodeTreeUpdateSplitter()
// - DockNodeTreeFindFallbackLeafNode()
// - DockNodeTreeFindNodeByPos()
//-----------------------------------------------------------------------------


fun dockNodeTreeSplit(ctx: Context, parentNode: DockNode, splitAxis: Axis, splitInheritorChildIdx: Int, splitRatio: Float, newNode: DockNode?) {

    assert(splitAxis != Axis.None)

    val child0 = newNode?.takeIf { splitInheritorChildIdx != 0 } ?: dockContextAddNode(ctx, 0)
    child0.parentNode = parentNode

    val child1 = newNode?.takeIf { splitInheritorChildIdx != 1 } ?: dockContextAddNode(ctx, 0)
    child1.parentNode = parentNode

    val childInheritor = if (splitInheritorChildIdx == 0) child0 else child1
    dockNodeMoveChildNodes(childInheritor, parentNode)
    parentNode.childNodes[0] = child0
    parentNode.childNodes[1] = child1
    parentNode.childNodes[splitInheritorChildIdx]!!.visibleWindow = parentNode.visibleWindow
    parentNode.splitAxis = splitAxis
    parentNode.visibleWindow = null
    parentNode.authorityForPos = DataAuthority.DockNode
    parentNode.authorityForSize = DataAuthority.DockNode

    var sizeAvail = parentNode.size[splitAxis] - IMGUI_DOCK_SPLITTER_SIZE
    sizeAvail = sizeAvail max (g.style.windowMinSize[splitAxis] * 2f)
    assert(sizeAvail > 0f) { "If you created a node manually with DockBuilderAddNode(), you need to also call DockBuilderSetNodeSize() before splitting." }
    child0.sizeRef put parentNode.size
    child1.sizeRef put parentNode.size
    child0.sizeRef[splitAxis] = floor(sizeAvail * splitRatio)
    child1.sizeRef[splitAxis] = floor(sizeAvail - child0.sizeRef[splitAxis])

    dockNodeMoveWindows(parentNode.childNodes[splitInheritorChildIdx], parentNode)
    dockNodeTreeUpdatePosSize(parentNode, parentNode.pos, parentNode.size)

    // Flags transfer (e.g. this is where we transfer the ImGuiDockNodeFlags_CentralNode property)
    child0.sharedFlags = parentNode.sharedFlags and DockNodeFlag._SharedFlagsInheritMask_
    child1.sharedFlags = parentNode.sharedFlags and DockNodeFlag._SharedFlagsInheritMask_
    childInheritor.localFlags = parentNode.localFlags or DockNodeFlag._LocalFlagsTransferMask_
    parentNode.localFlags = parentNode.localFlags wo DockNodeFlag._LocalFlagsTransferMask_
    if (childInheritor.isCentralNode)
        dockNodeGetRootNode(parentNode).centralNode = childInheritor
}

fun dockNodeTreeMerge(ctx: Context, parentNode: DockNode, mergeLeadChild: DockNode) {
    // When called from DockContextProcessUndockNode() it is possible that one of the child is NULL.
    val child0 = parentNode.childNodes[0]
    val child1 = parentNode.childNodes[1]
    assert(child0 != null || child1 != null)
    assert(mergeLeadChild === child0 || mergeLeadChild === child1)
    if (child0?.windows?.isNotEmpty() == true || child1?.windows?.isNotEmpty() == true) {
        assert(parentNode.tabBar == null)
        assert(parentNode.windows.isEmpty())
    }
    IMGUI_DEBUG_LOG_DOCKING("DockNodeTreeMerge 0x%08X & 0x%08X back into parent 0x%08X".format(child0?.id
            ?: 0, child1?.id ?: 0, parentNode.id))

    val backupLastExplicitSize = Vec2(parentNode.sizeRef)
    dockNodeMoveChildNodes(parentNode, mergeLeadChild)
    if (child0 != null) {
        dockNodeMoveWindows(parentNode, child0) // Generally only 1 of the 2 child node will have windows
        dockSettingsRenameNodeReferences(child0.id, parentNode.id)
    }
    if (child1 != null) {
        dockNodeMoveWindows(parentNode, child1)
        dockSettingsRenameNodeReferences(child1.id, parentNode.id)
    }
    dockNodeApplyPosSizeToWindows(parentNode)
    parentNode.setAuthorities(DataAuthority.Auto)
    parentNode.visibleWindow = mergeLeadChild.visibleWindow
    parentNode.sizeRef put backupLastExplicitSize

    // Flags transfer
    parentNode.localFlags = parentNode.localFlags wo DockNodeFlag._LocalFlagsTransferMask_ // Preserve Dockspace flag
    parentNode.localFlags = parentNode.localFlags or ((child0?.localFlags
            ?: DockNodeFlag.None.i) and DockNodeFlag._LocalFlagsTransferMask_)
    parentNode.localFlags = parentNode.localFlags or ((child1?.localFlags
            ?: DockNodeFlag.None.i) and DockNodeFlag._LocalFlagsTransferMask_)

    if (child0 != null)
        ctx.dockContext.nodes -= child0.id
    if (child1 != null)
        ctx.dockContext.nodes -= child1.id
}

/** Update Pos/Size for a node hierarchy (don't affect child Windows yet)
 *  [JVM] safe Vec2 instance passing */
fun dockNodeTreeUpdatePosSize(node: DockNode, pos: Vec2, size: Vec2, onlyWriteToMarkedNodes: Boolean = false) {

    // During the regular dock node update we write to all nodes.
    // 'only_write_to_marked_nodes' is only set when turning a node visible mid-frame and we need its size right-away.
    val writeToNode = !onlyWriteToMarkedNodes || node.markedForPosSizeWrite
    if (writeToNode) {
        node.pos put pos
        node.size put size
    }

    if (node.isLeafNode)
        return

    val child0 = node.childNodes[0]!!
    val child1 = node.childNodes[1]!!
    val child0Pos = Vec2(pos)
    val child1Pos = Vec2(pos)
    val child0Size = Vec2(size)
    val child1Size = Vec2(size)
    if (child0.isVisible && child1.isVisible) {
        val spacing = IMGUI_DOCK_SPLITTER_SIZE
        val axis = node.splitAxis
        val sizeAvail = (size[axis] - spacing) max 0f

        // Size allocation policy
        // 1) The first 0..WindowMinSize[axis]*2 are allocated evenly to both windows.
        val sizeMinEach = floor(min(sizeAvail, g.style.windowMinSize[axis] * 2f) * 0.5f)

        // 2) Process locked absolute size (during a splitter resize we preserve the child of nodes not touching the splitter edge)
        assert(!child0.wantLockSizeOnce || !child1.wantLockSizeOnce)
        if (child0.wantLockSizeOnce) {
            child0.wantLockSizeOnce = false
            child0Size[axis] = child0.size[axis]
            child0.sizeRef[axis] = child0.size[axis]
            child1Size[axis] = sizeAvail - child0Size[axis]
            child1.sizeRef[axis] = sizeAvail - child0Size[axis]
            assert(child0.sizeRef[axis] > 0f && child1.sizeRef[axis] > 0f)

        } else if (child1.wantLockSizeOnce) {
            child1.wantLockSizeOnce = false
            child1Size[axis] = child1.size[axis]
            child1.sizeRef[axis] = child1.size[axis]
            child0Size[axis] = sizeAvail - child1Size[axis]
            child0.sizeRef[axis] = sizeAvail - child1Size[axis]
            assert(child0.sizeRef[axis] > 0f && child1.sizeRef[axis] > 0f)
        }

        // 3) If one window is the central node (~ use remaining space, should be made explicit!), use explicit size from the other, and remainder for the central node
        else if (child1.isCentralNode && child0.sizeRef[axis] != 0f) {
            child0Size[axis] = (sizeAvail - sizeMinEach) min child0.sizeRef[axis]
            child1Size[axis] = sizeAvail - child0Size[axis]
        } else if (child0.isCentralNode && child1.sizeRef[axis] != 0f) {
            child1Size[axis] = (sizeAvail - sizeMinEach) min child1.sizeRef[axis]
            child0Size[axis] = sizeAvail - child1Size[axis]
        } else {
            // 4) Otherwise distribute according to the relative ratio of each SizeRef value
            val splitRatio = child0.sizeRef[axis] / (child0.sizeRef[axis] + child1.sizeRef[axis])
            child0Size[axis] = sizeMinEach max floor(sizeAvail * splitRatio + 0.5f)
            child1Size[axis] = sizeAvail - child0Size[axis]
        }
        child1Pos[axis] += spacing + child0Size[axis]
    }
    if (child0.isVisible)
        dockNodeTreeUpdatePosSize(child0, child0Pos, child0Size)
    if (child1.isVisible)
        dockNodeTreeUpdatePosSize(child1, child1Pos, child1Size)
}

fun dockNodeTreeUpdateSplitterFindTouchingNode(node: DockNode, axis: Axis, side: Int, touchingNodes: ArrayList<DockNode>) {
    if (node.isLeafNode) {
        touchingNodes += node
        return
    }
    val c0 = node.childNodes[0]!!
    val c1 = node.childNodes[1]!!
    if (c0.isVisible)
        if (node.splitAxis != axis || side == 0 || !c1.isVisible)
            dockNodeTreeUpdateSplitterFindTouchingNode(c0, axis, side, touchingNodes)
    if (c1.isVisible)
        if (node.splitAxis != axis || side == 1 || !c0.isVisible)
            dockNodeTreeUpdateSplitterFindTouchingNode(c1, axis, side, touchingNodes)
}

fun dockNodeTreeUpdateSplitter(node: DockNode) {

    if (node.isLeafNode)
        return

    val child0 = node.childNodes[0]!!
    val child1 = node.childNodes[1]!!
    if (child0.isVisible && child1.isVisible) {
        // Bounding box of the splitter cover the space between both nodes (w = Spacing, h = Size[xy^1] for when splitting horizontally)
        val axis = node.splitAxis
        assert(axis != Axis.None)
        val bb = Rect(child0.pos, child1.pos)
        bb.min[axis] += child0.size[axis]
        bb.max[axis xor 1] += child1.size[axis xor 1]
        //if (g.IO.KeyCtrl) GetForegroundDrawList(g.CurrentWindow->Viewport)->AddRect(bb.Min, bb.Max, IM_COL32(255,0,255,255));

        if ((child0.mergedFlags or child1.mergedFlags) has DockNodeFlag.NoResize) {
            val window = g.currentWindow!!
            window.drawList.addRectFilled(bb.min, bb.max, Col.Separator.u32, g.style.frameRounding)
        } else {
            //bb.Min[axis] += 1; // Display a little inward so highlight doesn't connect with nearby tabs on the neighbor node.
            //bb.Max[axis] -= 1;
            pushID(node.id)

            // Gather list of nodes that are touching the splitter line. Find resizing limits based on those nodes.
            val touchingNodes = Array(2) { ArrayList<DockNode>() }
            val minSize = g.style.windowMinSize[axis]
            val resizeLimits = floatArrayOf(
                    child0.pos[axis] + minSize,
                    child1.pos[axis] + child1.size[axis] - minSize)

            val splitterId = getID("##Splitter")
            if (g.activeId == splitterId) {
                // Only process when splitter is active
                dockNodeTreeUpdateSplitterFindTouchingNode(child0, axis, 1, touchingNodes[0])
                dockNodeTreeUpdateSplitterFindTouchingNode(child1, axis, 0, touchingNodes[1])
                for (touchingNode in touchingNodes[0])
                    resizeLimits[0] = resizeLimits[0] max (touchingNode.rect().min[axis] + minSize)
                for (touchingNode in touchingNodes[1])
                    resizeLimits[1] = resizeLimits[1] min (touchingNode.rect().max[axis] - minSize)

                /*
                // [DEBUG] Render limits
                ImDrawList* draw_list = node->HostWindow ? GetForegroundDrawList(node->HostWindow) : GetForegroundDrawList((ImGuiViewportP*)GetMainViewport());
                for (int n = 0; n < 2; n++)
                    if (axis == ImGuiAxis_X)
                        draw_list->AddLine(ImVec2(resize_limits[n], node->ChildNodes[n]->Pos.y), ImVec2(resize_limits[n], node->ChildNodes[n]->Pos.y + node->ChildNodes[n]->Size.y), IM_COL32(255, 0, 255, 255), 3.0f);
                    else
                        draw_list->AddLine(ImVec2(node->ChildNodes[n]->Pos.x, resize_limits[n]), ImVec2(node->ChildNodes[n]->Pos.x + node->ChildNodes[n]->Size.x, resize_limits[n]), IM_COL32(255, 0, 255, 255), 3.0f);
                */
            }

            // Use a short delay before highlighting the splitter (and changing the mouse cursor) in order for regular mouse movement to not highlight many splitters
            var curSize0 = child0.size[axis]
            var curSize1 = child1.size[axis]
            val minSize0 = resizeLimits[0] - child0.pos[axis]
            val minSize1 = child1.pos[axis] + child1.size[axis] - resizeLimits[1]
            _f = curSize0
            _f1 = curSize1
            if (splitterBehavior(bb, getID("##Splitter"), axis, ::_f, ::_f1, minSize0, minSize1, WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS, WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER)) {
                curSize0 = _f
                curSize1 = _f1
                if (touchingNodes[0].size > 0 && touchingNodes[1].size > 0) {
                    child0.size[axis] = curSize0
                    child0.sizeRef[axis] = curSize0
                    child1.pos[axis] -= curSize1 - child1.size[axis]
                    child1.size[axis] = curSize1
                    child1.sizeRef[axis] = curSize1

                    // Lock the size of every node that is a sibling of the node we are touching
                    // This might be less desirable if we can merge sibling of a same axis into the same parental level.
                    for (sideN in 0..1) {
                        var touchingNodeN = 0
                        while (touchingNodeN < touchingNodes[sideN].size) {
                            var touchingNode = touchingNodes[sideN][touchingNodeN++]
                            //ImDrawList* draw_list = node->HostWindow ? GetForegroundDrawList(node->HostWindow) : GetForegroundDrawList((ImGuiViewportP*)GetMainViewport());
                            //draw_list->AddRect(touching_node->Pos, touching_node->Pos + touching_node->Size, IM_COL32(255, 128, 0, 255));
                            val parent = touchingNode.parentNode!!
                            while (parent !== node) {
                                if (parent.splitAxis == axis) {
                                    // Mark other node so its size will be preserved during the upcoming call to DockNodeTreeUpdatePosSize().
                                    val nodeToPreserve = parent.childNodes[sideN]!!
                                    nodeToPreserve.wantLockSizeOnce = true
                                    //draw_list->AddRect(touching_node->Pos, touching_node->Rect().Max, IM_COL32(255, 0, 0, 255));
                                    //draw_list->AddRectFilled(node_to_preserve->Pos, node_to_preserve->Rect().Max, IM_COL32(0, 255, 0, 100));
                                }
                                touchingNode = parent
                            }
                        }
                    }

                    dockNodeTreeUpdatePosSize(child0, child0.pos, child0.size)
                    dockNodeTreeUpdatePosSize(child1, child1.pos, child1.size)
                    markIniSettingsDirty()
                }
            }
            popID()
        }
    }

    if (child0.isVisible)
        dockNodeTreeUpdateSplitter(child0)
    if (child1.isVisible)
        dockNodeTreeUpdateSplitter(child1)
}

fun dockNodeTreeFindNodeByPos(node: DockNode, pos: Vec2): DockNode? {
    if (!node.isVisible)
        return null

    val dockSpacing = g.style.itemInnerSpacing.x
    val r = Rect(node.pos, node.pos + node.size)
    r expand dockSpacing * 0.5f
    val inside = pos in r
    if (!inside)
        return null

    if (node.isLeafNode)
        return node
    dockNodeTreeFindNodeByPos(node.childNodes[0]!!, pos)?.let { return it }
    dockNodeTreeFindNodeByPos(node.childNodes[1]!!, pos)?.let { return it }

    // There is an edge case when docking into a dockspace which only has inactive nodes (because none of the windows are active)
    // In this case we need to fallback into any leaf mode, possibly the central node.
    if (node.isDockSpace && node.isRootNode) {
        node.centralNode?.let {
            if (node.isLeafNode) // FIXME-20181220: We should not have to test for IsLeafNode() here but we have another bug to fix first.
                return it
        }
                ?: return dockNodeTreeFindFallbackLeafNode(node)
    }

    return null
}

fun dockNodeTreeFindFallbackLeafNode(node: DockNode): DockNode? = when {
    node.isLeafNode -> node
    else -> dockNodeTreeFindFallbackLeafNode(node.childNodes[0]!!)
            ?: dockNodeTreeFindFallbackLeafNode(node.childNodes[1]!!)
}