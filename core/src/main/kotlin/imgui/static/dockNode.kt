package imgui.static

import gli_.hasnt
import glm_.i
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.wo
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginDockableDragDropTarget
import imgui.ImGui.beginPopup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.closeButton
import imgui.ImGui.collapseButton
import imgui.ImGui.dockNodeGetRootNode
import imgui.ImGui.dragDropPayload
import imgui.ImGui.end
import imgui.ImGui.endPopup
import imgui.ImGui.endTabBar
import imgui.ImGui.focusWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isPopupOpen
import imgui.ImGui.menuItem
import imgui.ImGui.navInitWindow
import imgui.ImGui.openPopup
import imgui.ImGui.popID
import imgui.ImGui.popItemFlag
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushItemFlag
import imgui.ImGui.pushOverrideID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setNextWindowClass
import imgui.ImGui.setNextWindowCollapsed
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowViewport
import imgui.ImGui.startMouseMovingWindowOrNode
import imgui.ImGui.style
import imgui.ImGui.tabItemBackground
import imgui.ImGui.tabItemCalcSize
import imgui.ImGui.tabItemLabelAndCloseButton
import imgui.ImGui.text
import imgui.api.g
import imgui.hasnt
import imgui.internal.*
import imgui.internal.classes.*
import imgui.internal.sections.*
import imgui.WindowFlag as Wf

// ImGuiDockNode

//-----------------------------------------------------------------------------
// Docking: ImGuiDockNode
//-----------------------------------------------------------------------------
// - DockNodeGetTabOrder()
// - DockNodeAddWindow()
// - DockNodeRemoveWindow()
// - DockNodeMoveChildNodes()
// - DockNodeMoveWindows()
// - DockNodeApplyPosSizeToWindows()
// - DockNodeHideHostWindow()
// - ImGuiDockNodeFindInfoResults
// - DockNodeFindInfo()
// - DockNodeFindWindowByID()
// - DockNodeUpdateVisibleFlagAndInactiveChilds()
// - DockNodeUpdateVisibleFlag()
// - DockNodeStartMouseMovingWindow()
// - DockNodeUpdate()
// - DockNodeUpdateWindowMenu()
// - DockNodeUpdateTabBar()
// - DockNodeAddTabBar()
// - DockNodeRemoveTabBar()
// - DockNodeIsDropAllowedOne()
// - DockNodeIsDropAllowed()
// - DockNodeCalcTabBarLayout()
// - DockNodeCalcSplitRects()
// - DockNodeCalcDropRectsAndTestMousePos()
// - DockNodePreviewDockSetup()
// - DockNodePreviewDockRender()
//-----------------------------------------------------------------------------


fun dockNodeAddWindow(node: DockNode, window: Window, addToTabBar: Boolean) {
    window.dockNode?.let {
        // Can overwrite an existing window->DockNode (e.g. pointing to a disabled DockSpace node)
        assert(it.id != node.id)
        dockNodeRemoveWindow(it, window, 0)
    }
    assert(window.dockNode == null || window.dockNodeAsHost == null)
    IMGUI_DEBUG_LOG_DOCKING("DockNodeAddWindow node 0x%08X window '${window.name}'".format(node.id))

    node.windows += window
    node.wantHiddenTabBarUpdate = true
    window.dockNode = node
    window.dockId = node.id
    window.dockIsActive = node.windows.size > 1
    window.dockTabWantClose = false

    // If more than 2 windows appeared on the same frame, we'll create a new hosting DockNode from the point of the second window submission.
    // Then we need to hide the first window (after its been output) otherwise it would be visible as a standalone window for one frame.
    if (node.hostWindow == null && node.windows.size == 2 && !node.windows[0].wasActive) {
        node.windows[0].hidden = true
        node.windows[0].hiddenFramesCanSkipItems = 1
    }

    // When reactivating a node with one or two loose window, the window pos/size/viewport are authoritative over the node storage.
    // In particular it is important we init the viewport from the first window so we don't create two viewports and drop one.
    if (node.hostWindow == null && node.isFloatingNode) {
        if (node.authorityForPos == DataAuthority.Auto)
            node.authorityForPos = DataAuthority.Window
        if (node.authorityForSize == DataAuthority.Auto)
            node.authorityForSize = DataAuthority.Window
        if (node.authorityForViewport == DataAuthority.Auto)
            node.authorityForViewport = DataAuthority.Window
    }

    // Add to tab bar if requested
    if (addToTabBar) {
        if (node.tabBar == null) {
            dockNodeAddTabBar(node)
            val tabBar = node.tabBar!!
            tabBar.selectedTabId = node.selectedTabId
            tabBar.nextSelectedTabId = node.selectedTabId

            // Add existing windows
            for (n in 0 until node.windows.lastIndex)
                tabBar.addTab(TabItemFlag.None.i, node.windows[n])
        }
        node.tabBar!!.addTab(TabItemFlag._Unsorted.i, window)
    }

    dockNodeUpdateVisibleFlag(node)

    // Update this without waiting for the next time we Begin() in the window, so our host window will have the proper title bar color on its first frame.
    node.hostWindow?.let {
        window.updateParentAndRootLinks(window.flags or Wf._ChildWindow, it)
    }
}

fun dockNodeMoveWindows(dstNode: DockNode?, srcNode: DockNode?) {
    // Insert tabs in the same orders as currently ordered (node->Windows isn't ordered)
    srcNode!!
    dstNode!!
    assert(/*srcNode != null && dstNode != null &&*/ dstNode !== srcNode)
    val srcTabBar = srcNode.tabBar
    if (srcTabBar != null)
        assert(srcNode.windows.size == srcNode.tabBar!!.tabs.size)

    // If the dst_node is empty we can just move the entire tab bar (to preserve selection, scrolling, etc.)
    val moveTabBar = srcTabBar != null && dstNode.tabBar == null
    if (moveTabBar) {
        dstNode.tabBar = srcNode.tabBar
        srcNode.tabBar = null
    }

    for (n in srcNode.windows.indices) {
        val window = srcTabBar?.tabs?.get(n)?.window ?: srcNode.windows[n]
        window.dockNode = null
        window.dockIsActive = false
        dockNodeAddWindow(dstNode, window, !moveTabBar)
    }
    srcNode.windows.clear()

    if (!moveTabBar && srcNode.tabBar != null) {
        if (dstNode.tabBar != null)
            dstNode.tabBar!!.selectedTabId = srcNode.tabBar!!.selectedTabId
        dockNodeRemoveTabBar(srcNode)
    }
}

fun dockNodeMoveChildNodes(dstNode: DockNode, srcNode: DockNode) {
    assert(dstNode.windows.isEmpty())
    dstNode.childNodes[0] = srcNode.childNodes[0]
    dstNode.childNodes[1] = srcNode.childNodes[1]
    dstNode.childNodes[0]?.let { it.parentNode = dstNode }
    dstNode.childNodes[1]?.let { it.parentNode = dstNode }
    dstNode.splitAxis = srcNode.splitAxis
    dstNode.sizeRef put srcNode.sizeRef
    srcNode.childNodes[0] = null
    srcNode.childNodes[1] = null
}

fun dockNodeFindInfo(node: DockNode, results: DockNodeFindInfoResults) {
    if (node.windows.isNotEmpty()) {
        if (results.firstNodeWithWindows == null)
            results.firstNodeWithWindows = node
        results.countNodesWithWindows++
    }
    if (node.isCentralNode) {
        assert(results.centralNode == null) { "Should be only one" }
        assert(node.isLeafNode) { "If you get this assert: please submit .ini file + repro of actions leading to this." }
        results.centralNode = node
    }
    if (results.countNodesWithWindows > 1 && results.centralNode != null)
        return
    node.childNodes[0]?.let { dockNodeFindInfo(it, results) }
    node.childNodes[1]?.let { dockNodeFindInfo(it, results) }
}

fun dockNodeFindWindowByID(node: DockNode, id: ID): Window? {
    assert(id != 0)
    return node.windows.find { it.id == id }
}

fun dockNodeApplyPosSizeToWindows(node: DockNode) {
    node.windows.forEach {
        it.setPos(node.pos, Cond.Always) // We don't assign directly to Pos because it can break the calculation of SizeContents on next frame
        it.setSize(node.size, Cond.Always)
    }
}

fun dockNodeRemoveWindow(node: DockNode, window: Window, saveDockId: ID) {
    assert(window.dockNode === node)
    //IM_ASSERT(window->RootWindow == node->HostWindow);
    //IM_ASSERT(window->LastFrameActive < g.FrameCount);    // We may call this from Begin()
    assert(saveDockId == 0 || saveDockId == node.id)
    IMGUI_DEBUG_LOG_DOCKING("DockNodeRemoveWindow node 0x%08X window '${window.name}'".format(node.id))

    window.dockNode = null
    window.dockIsActive = false
    window.dockTabWantClose = false
    window.dockId = saveDockId
    window.updateParentAndRootLinks(window.flags wo Wf._ChildWindow, null) // Update immediately

    // Remove window
    var erased = false
    for (n in node.windows.indices)
        if (node.windows[n] === window) {
            node.windows.removeAt(n)
            erased = true
            break
        }
    assert(erased)
    if (node.visibleWindow === window)
        node.visibleWindow = null

    // Remove tab and possibly tab bar
    node.wantHiddenTabBarUpdate = true
    node.tabBar?.let {
        it removeTab window.id
        val tabCountThresholdForTabBar = if (node.isCentralNode) 1 else 2
        if (node.windows.size < tabCountThresholdForTabBar)
            dockNodeRemoveTabBar(node)
    }

    if (node.windows.isEmpty() && !node.isCentralNode && !node.isDockSpace && window.dockId != node.id) {
        // Automatic dock node delete themselves if they are not holding at least one tab
        dockContextRemoveNode(g, node, true)
        return
    }

    if (node.windows.size == 1 && !node.isCentralNode)
        node.hostWindow?.let {
            val remainingWindow = node.windows[0]
            if (it.viewportOwned && node.isRootNode) {
                val vp = it.viewport!!
                // Transfer viewport back to the remaining loose window
                assert(vp.window === node.hostWindow)
                vp.window = remainingWindow
                vp.id = remainingWindow.id
            }
            remainingWindow.collapsed = it.collapsed
        }

    // Update visibility immediately is required so the DockNodeUpdateRemoveInactiveChilds() processing can reflect changes up the tree
    dockNodeUpdateVisibleFlag(node)
}

fun dockNodeHideHostWindow(node: DockNode) {
    node.hostWindow?.let {
        if (it.dockNodeAsHost === node)
            it.dockNodeAsHost = null
        node.hostWindow = null
    }

    if (node.windows.size == 1) {
        node.visibleWindow = node.windows[0]
        node.windows[0].dockIsActive = false
    }

    if (node.tabBar != null)
        dockNodeRemoveTabBar(node)
}

fun dockNodeUpdate(node: DockNode) {
    assert(node.lastFrameActive != g.frameCount)
    node.lastFrameAlive = g.frameCount
    node.markedForPosSizeWrite = false

    node.centralNode = null
    node.onlyNodeWithWindows = null
    if (node.isRootNode)
        dockNodeUpdateForRootNode(node)

    // Remove tab bar if not needed
    if (node.tabBar != null && node.isNoTabBar)
        dockNodeRemoveTabBar(node)

    // Early out for hidden root dock nodes (when all DockId references are in inactive windows, or there is only 1 floating window holding on the DockId)
    var wantToHideHostWindow = false
    if (node.windows.size <= 1 && node.isFloatingNode && node.isLeafNode)
        if (!g.io.configDockingAlwaysTabBar && (node.windows.isEmpty() || !node.windows[0].windowClass.dockingAlwaysTabBar))
            wantToHideHostWindow = true
    if (wantToHideHostWindow) {
        if (node.windows.size == 1) {
            // Floating window pos/size is authoritative
            val singleWindow = node.windows[0]
            node.pos put singleWindow.pos
            node.size put singleWindow.sizeFull
            node.setAuthorities(DataAuthority.Window)

            // Transfer focus immediately so when we revert to a regular window it is immediately selected
            node.hostWindow?.let {
                if (g.navWindow === it)
                    focusWindow(singleWindow)
                singleWindow.viewport = it.viewport
                singleWindow.viewportId = it.viewportId
                if (it.viewportOwned) {
                    singleWindow.viewport!!.window = singleWindow
                    singleWindow.viewportOwned = true
                }
            }
        }

        dockNodeHideHostWindow(node)
        node.state = DockNodeState.HostWindowHiddenBecauseSingleWindow
        node.wantCloseAll = false
        node.wantCloseTabId = 0
        node.hasCloseButton = false
        node.hasWindowMenuButton = false
        node.enableCloseButton = false
        node.lastFrameActive = g.frameCount

        if (node.wantMouseMove && node.windows.size == 1)
            dockNodeStartMouseMovingWindow(node, node.windows[0])
        return
    }

    // In some circumstance we will defer creating the host window (so everything will be kept hidden),
    // while the expected visible window is resizing itself.
    // This is important for first-time (no ini settings restored) single window when io.ConfigDockingAlwaysTabBar is enabled,
    // otherwise the node ends up using the minimum window size. Effectively those windows will take an extra frame to show up:
    //   N+0: Begin(): window created (with no known size), node is created
    //   N+1: DockNodeUpdate(): node skip creating host window / Begin(): window size applied, not visible
    //   N+2: DockNodeUpdate(): node can create host window / Begin(): window becomes visible
    // We could remove this frame if we could reliably calculate the expected window size during node update, before the Begin() code.
    // It would require a generalization of CalcWindowExpectedSize(), probably extracting code away from Begin().
    // In reality it isn't very important as user quickly ends up with size data in .ini file.
    if (node.isVisible && node.hostWindow == null && node.isFloatingNode && node.isLeafNode) {
        assert(node.windows.isNotEmpty())
        var refWindow: Window? = null
        if (node.selectedTabId != 0) // Note that we prune single-window-node settings on .ini loading, so this is generally 0 for them!
            refWindow = dockNodeFindWindowByID(node, node.selectedTabId)
        if (refWindow == null)
            refWindow = node.windows[0]
        if (refWindow.autoFitFrames.x > 0 || refWindow.autoFitFrames.y > 0) { // TODO glm
            node.state = DockNodeState.HostWindowHiddenBecauseWindowsAreResizing
            return
        }
    }

    val nodeFlags = node.mergedFlags

    // Bind or create host window
    var hostWindow: Window? = null
    var beginnedIntoHostWindow = false
    if (node.isDockSpace) {
        // [Explicit root dockspace node]
        assert(node.hostWindow != null)
        node.enableCloseButton = false
        node.hasCloseButton = nodeFlags hasnt DockNodeFlag._NoCloseButton
        node.hasWindowMenuButton = nodeFlags hasnt DockNodeFlag._NoWindowMenuButton
        hostWindow = node.hostWindow
    } else {
        // [Automatic root or child nodes]
        node.enableCloseButton = false
        node.hasCloseButton = node.windows.isNotEmpty() && nodeFlags hasnt DockNodeFlag._NoCloseButton
        node.hasWindowMenuButton = node.windows.isNotEmpty() && nodeFlags hasnt DockNodeFlag._NoWindowMenuButton
        for (window in node.windows) {
            // FIXME-DOCK: Setting DockIsActive here means that for single active window in a leaf node, DockIsActive will be cleared until the next Begin() call.
            window.dockIsActive = node.windows.size > 1
            node.enableCloseButton = node.enableCloseButton || window.hasCloseButton
        }

        val parent = node.parentNode
        if (node.isRootNode && node.isVisible) {

            val refWindow = node.windows.getOrNull(0)

            // Sync Pos
            if (node.authorityForPos == DataAuthority.Window && refWindow != null)
                setNextWindowPos(refWindow.pos)
            else if (node.authorityForPos == DataAuthority.DockNode)
                setNextWindowPos(node.pos)

            // Sync Size
            if (node.authorityForSize == DataAuthority.Window && refWindow != null)
                setNextWindowSize(refWindow.sizeFull)
            else if (node.authorityForSize == DataAuthority.DockNode)
                setNextWindowSize(node.size)

            // Sync Collapsed
            if (node.authorityForSize == DataAuthority.Window && refWindow != null)
                setNextWindowCollapsed(refWindow.collapsed)

            // Sync Viewport
            if (node.authorityForViewport == DataAuthority.Window && refWindow != null)
                setNextWindowViewport(refWindow.viewportId)

            setNextWindowClass(node.windowClass)

            // Begin into the host window
            val windowLabel = dockNodeGetHostWindowTitle(node)
            val windowFlags = Wf.NoScrollbar or Wf.NoScrollWithMouse or Wf._DockNodeHost or
                    Wf.NoFocusOnAppearing or Wf.NoSavedSettings or Wf.NoNavFocus or Wf.NoCollapse or Wf.NoTitleBar

            pushStyleVar(StyleVar.WindowPadding, Vec2())
            begin(windowLabel, null, windowFlags)
            popStyleVar()
            beginnedIntoHostWindow = true

            hostWindow = g.currentWindow!!
            node.hostWindow = hostWindow
            hostWindow.dockNodeAsHost = node
            hostWindow.dc.cursorPos = hostWindow.pos
            node.pos put hostWindow.pos
            node.size put hostWindow.size

            // We set ImGuiWindowFlags_NoFocusOnAppearing because we don't want the host window to take full focus (e.g. steal NavWindow)
            // But we still it bring it to the front of display. There's no way to choose this precise behavior via window flags.
            // One simple case to ponder if: window A has a toggle to create windows B/C/D. Dock B/C/D together, clear the toggle and enable it again.
            // When reappearing B/C/D will request focus and be moved to the top of the display pile, but they are not linked to the dock host window
            // during the frame they appear. The dock host window would keep its old display order, and the sorting in EndFrame would move B/C/D back
            // after the dock host window, losing their top-most status.
            if (node.hostWindow!!.appearing)
                node.hostWindow!!.bringToDisplayFront()

            node.setAuthorities(DataAuthority.Auto)
        } else if (parent != null) {
            hostWindow = parent.hostWindow
            node.hostWindow = hostWindow
            node.setAuthorities(DataAuthority.Auto)
        }
        if (node.wantMouseMove)
            node.hostWindow?.let { dockNodeStartMouseMovingWindow(node, it) }
    }

    // Update focused node (the one whose title bar is highlight) within a node tree
    if (node.isSplitNode)
        assert(node.tabBar == null)
    if (node.isRootNode)
        g.navWindow?.let {
            val root = it.rootWindowDockStop!!
            if (root.dockNode != null && root.parentWindow === hostWindow)
                node.lastFocusedNodeId = root.dockNode!!.id
        }

    // We need to draw a background at the root level if requested by ImGuiDockNodeFlags_PassthruCentralNode, but we will only know the correct pos/size after
    // processing the resizing splitters. So we are using the DrawList channel splitting facility to submit drawing primitives out of order!
    val renderDockspaceBg = node.isRootNode && hostWindow != null && nodeFlags has DockNodeFlag.PassthruCentralNode
    if (renderDockspaceBg) {
        hostWindow!!.drawList.channelsSplit(2)
        hostWindow.drawList.channelsSetCurrent(1)
    }

    // Register a hit-test hole in the window unless we are currently dragging a window that is compatible with our dockspace
    val centralNode = node.centralNode
    val centralNodeHole = node.isRootNode && hostWindow != null && nodeFlags has DockNodeFlag.PassthruCentralNode && centralNode?.isEmpty == true
    var centralNodeHoleRegisterHitTestHole = centralNodeHole
    if (centralNodeHole)
        dragDropPayload?.let {
            if (it.isDataType(IMGUI_PAYLOAD_TYPE_WINDOW) && dockNodeIsDropAllowed(hostWindow!!, it.data as Window))
                centralNodeHoleRegisterHitTestHole = false
        }
    if (centralNodeHoleRegisterHitTestHole) {
        // Add a little padding to match the "resize from edges" behavior and allow grabbing the splitter easily.
        assert(node.isDockSpace) { "We cannot pass this flag without the DockSpace() api. Testing this because we also setup the hole in host_window->ParentNode" }
        val centralHole = Rect(centralNode!!.pos, centralNode.pos + centralNode.size)
        centralHole expand Vec2(-WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS, -WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS)
        if (centralNodeHole && !centralHole.isInverted) {
            hostWindow!!.setHitTestHole(centralHole.min, centralHole.max - centralHole.min)
            hostWindow.parentWindow!!.setHitTestHole(centralHole.min, centralHole.max - centralHole.min)
        }
    }

    // Update position/size, process and draw resizing splitters
    if (node.isRootNode)
        hostWindow?.let {
            dockNodeTreeUpdatePosSize(node, it.pos, it.size)
            dockNodeTreeUpdateSplitter(node)
        }

    // Draw empty node background (currently can only be the Central Node)
    if (hostWindow != null && node.isEmpty && node.isVisible && nodeFlags hasnt DockNodeFlag.PassthruCentralNode)
        hostWindow.drawList.addRectFilled(node.pos, node.pos + node.size, Col.DockingEmptyBg.u32)

    // Draw whole dockspace background if ImGuiDockNodeFlags_PassthruCentralNode if set.
    if (renderDockspaceBg && node.isVisible) {
        hostWindow!!.drawList.channelsSetCurrent(0)
        if (centralNodeHole)
            hostWindow.drawList.renderRectFilledWithHole(node.rect(), centralNode!!.rect(), Col.WindowBg.u32, 0f)
        else
            hostWindow.drawList.addRectFilled(node.pos, node.pos + node.size, Col.WindowBg.u32, 0f)
        hostWindow.drawList.channelsMerge()
    }

    // Draw and populate Tab Bar
    if (hostWindow != null && node.windows.isNotEmpty())
        dockNodeUpdateTabBar(node, hostWindow)
    else {
        node.wantCloseAll = false
        node.wantCloseTabId = 0
        node.isFocused = false
    }
    val tabBar = node.tabBar
    if (tabBar != null && tabBar.selectedTabId != 0)
        node.selectedTabId = tabBar.selectedTabId
    else if (node.windows.isNotEmpty())
        node.selectedTabId = node.windows[0].id

    // Draw payload drop target
    if (hostWindow != null && node.isVisible)
        if (node.isRootNode && (g.movingWindow == null || g.movingWindow!!.rootWindow !== hostWindow))
            beginDockableDragDropTarget(hostWindow)

    // We update this after DockNodeUpdateTabBar()
    node.lastFrameActive = g.frameCount

    // Recurse into children
    // FIXME-DOCK FIXME-OPT: Should not need to recurse into children
    if (hostWindow != null) {
        node.childNodes[0]?.let(::dockNodeUpdate)
        node.childNodes[1]?.let(::dockNodeUpdate)

        // Render outer borders last (after the tab bar)
        if (node.isRootNode)
            hostWindow.renderOuterBorders()
    }

    // End host window
    if (beginnedIntoHostWindow) //-V1020
        end()
}

/** Search function called once by root node in DockNodeUpdate() */
class DockNodeFindInfoResults {
    var centralNode: DockNode? = null
    var firstNodeWithWindows: DockNode? = null
    var countNodesWithWindows = 0
    //ImGuiWindowClass  WindowClassForMerges;
}

/** Update CentralNode, OnlyNodeWithWindows, LastFocusedNodeID. Copy window class. */
fun dockNodeUpdateForRootNode(node: DockNode) {

    dockNodeUpdateVisibleFlagAndInactiveChilds(node)

    // FIXME-DOCK: Merge this scan into the one above.
    // - Setup central node pointers
    // - Find if there's only a single visible window in the hierarchy (in which case we need to display a regular title bar -> FIXME-DOCK: that last part is not done yet!)
    val results = DockNodeFindInfoResults()
    dockNodeFindInfo(node, results)
    node.centralNode = results.centralNode
    node.onlyNodeWithWindows = if (results.countNodesWithWindows == 1) results.firstNodeWithWindows else null
    if (node.lastFocusedNodeId == 0)
        results.firstNodeWithWindows?.let { node.lastFocusedNodeId = it.id }

    // Copy the window class from of our first window so it can be used for proper dock filtering.
    // When node has mixed windows, prioritize the class with the most constraint (DockingAllowUnclassed = false) as the reference to copy.
    // FIXME-DOCK: We don't recurse properly, this code could be reworked to work from DockNodeUpdateScanRec.
    results.firstNodeWithWindows?.let { firstNodeWithWindows ->
        node.windowClass = firstNodeWithWindows.windows[0].windowClass
        for (n in 1 until firstNodeWithWindows.windows.size) {
            val clazz = firstNodeWithWindows.windows[n].windowClass
            if (!clazz.dockingAllowUnclassed) {
                node.windowClass = clazz
                break
            }
        }
    }
}

/** - Remove inactive windows/nodes.
 *  - Update visibility flag. */
fun dockNodeUpdateVisibleFlagAndInactiveChilds(node: DockNode) {

    val parent = node.parentNode
    assert(parent == null || parent.childNodes[0] == node || parent.childNodes[1] == node)

    // Inherit most flags
    if (parent != null)
        node.sharedFlags = parent.sharedFlags and DockNodeFlag._SharedFlagsInheritMask_

    // Recurse into children
    // There is the possibility that one of our child becoming empty will delete itself and moving its sibling contents into 'node'.
    // If 'node->ChildNode[0]' delete itself, then 'node->ChildNode[1]->Windows' will be moved into 'node'
    // If 'node->ChildNode[1]' delete itself, then 'node->ChildNode[0]->Windows' will be moved into 'node' and the "remove inactive windows" loop will have run twice on those windows (harmless)
    node.childNodes[0]?.let(::dockNodeUpdateVisibleFlagAndInactiveChilds)
    node.childNodes[1]?.let(::dockNodeUpdateVisibleFlagAndInactiveChilds)

    // Remove inactive windows
    // Merge node flags overrides stored in windows
    var windowN = -1
    while (++windowN < node.windows.size) {

        val window = node.windows[windowN]
        assert(window.dockNode === node)

        val nodeWasActive = node.lastFrameActive + 1 == g.frameCount
        var remove = false
        remove = remove || (nodeWasActive && window.lastFrameActive + 1 < g.frameCount)
        remove = remove || (nodeWasActive && (node.wantCloseAll || node.wantCloseTabId == window.id) && window.hasCloseButton && window.flags hasnt Wf.UnsavedDocument)  // Submit all _expected_ closure from last frame
        remove = remove || window.dockTabWantClose
        if (remove) {
            window.dockTabWantClose = false
            if (node.windows.size == 1 && !node.isCentralNode) {
                dockNodeHideHostWindow(node)
                node.state = DockNodeState.HostWindowHiddenBecauseSingleWindow
                dockNodeRemoveWindow(node, window, node.id) // Will delete the node so it'll be invalid on return
                return
            }
            dockNodeRemoveWindow(node, window, node.id)
            windowN--
        } else {
            node.localFlags = node.localFlags wo window.windowClass.dockNodeFlagsOverrideClear
            node.localFlags = node.localFlags or window.windowClass.dockNodeFlagsOverrideSet
        }
    }

    // Auto-hide tab bar option
    val nodeFlags = node.mergedFlags
    if (node.wantHiddenTabBarUpdate && node.windows.size == 1 && nodeFlags has DockNodeFlag.AutoHideTabBar && !node.isHiddenTabBar)
        node.wantHiddenTabBarToggle = true
    node.wantHiddenTabBarUpdate = false

    // Cancel toggling if we know our tab bar is enforced to be hidden at all times
    if (node.wantHiddenTabBarToggle)
        node.visibleWindow?.let {
            if (it.windowClass.dockNodeFlagsOverrideSet has DockNodeFlag._HiddenTabBar)
                node.wantHiddenTabBarToggle = false
        }

    // Apply toggles at a single point of the frame (here!)
    if (node.windows.size > 1)
        node.localFlags = node.localFlags wo DockNodeFlag._HiddenTabBar
    else if (node.wantHiddenTabBarToggle)
        node.localFlags = node.localFlags xor DockNodeFlag._HiddenTabBar
    node.wantHiddenTabBarToggle = false

    dockNodeUpdateVisibleFlag(node)
}

/** Compare TabItem nodes given the last known DockOrder (will persist in .ini file as hint), used to sort tabs when multiple tabs are added on the same frame. */
object tabItemComparerByDockOrder : Comparator<TabItem> {
    override fun compare(o1: TabItem, o2: TabItem): Int {
        val a = o1.window!!
        val b = o1.window!!
        val d = (if (a.dockOrder == -1) Int.MAX_VALUE else a.dockOrder) - if (b.dockOrder == -1) Int.MAX_VALUE else b.dockOrder
        return when {
            d != 0 -> d
            else -> a.beginOrderWithinContext - b.beginOrderWithinContext
        }
    }
}

fun dockNodeUpdateTabBar(node: DockNode, hostWindow: Window) {

    val nodeWasActive = node.lastFrameActive + 1 == g.frameCount
    val closedAll = node.wantCloseAll && nodeWasActive
    val closedOne: ID = node.wantCloseTabId and nodeWasActive.i
    node.wantCloseAll = false
    node.wantCloseTabId = 0

    // Decide if we should use a focused title bar color
    var isFocused = false
    val rootNode = dockNodeGetRootNode(node)
    g.navWindowingTarget.let {
        val nav = g.navWindow
        if (it != null)
            isFocused = it.dockNode === node
        else if (nav != null && nav.rootWindowForTitleBarHighlight === hostWindow.rootWindow && rootNode.lastFocusedNodeId == node.id)
            isFocused = true
    }

    // Hidden tab bar will show a triangle on the upper-left (in Begin)
    if (node.isHiddenTabBar || node.isNoTabBar) {
        node.visibleWindow = node.windows.getOrNull(0)
        node.isFocused = isFocused
        if (isFocused)
            node.lastFrameFocused = g.frameCount
        node.visibleWindow?.let { visible ->
            // Notify root of visible window (used to display title in OS task bar)
            if (isFocused || rootNode.visibleWindow == null)
                rootNode.visibleWindow = visible
            node.tabBar?.visibleTabId = visible.id
        }
        return
    }

    // Move ourselves to the Menu layer (so we can be accessed by tapping Alt) + undo SkipItems flag in order to draw over the title bar even if the window is collapsed
    val backupSkipItem = hostWindow.skipItems
    if (!node.isDockSpace) {
        hostWindow.skipItems = false
        hostWindow.dc.navLayerCurrent = NavLayer.Menu
    }

    // Use PushOverrideID() instead of PushID() to use the node id _without_ the host window ID.
    // This is to facilitate computing those ID from the outside, and will affect more or less only the ID of the collapse button, popup and tabs,
    // as docked windows themselves will override the stack with their own root ID.
    pushOverrideID(node.id)
    var tabBar_ = node.tabBar
    val tabBarIsRecreated = tabBar_ == null // Tab bar are automatically destroyed when a node gets hidden
    if (tabBar_ == null) {
        dockNodeAddTabBar(node)
        tabBar_ = node.tabBar
    }
    val tabBar = tabBar_!!

    var focusTabId: ID = 0
    node.isFocused = isFocused

    val nodeFlags = node.mergedFlags
    val hasWindowMenuButton = nodeFlags hasnt DockNodeFlag._NoWindowMenuButton && style.windowMenuButtonPosition != Dir.None
    val hasCloseButton = nodeFlags hasnt DockNodeFlag._NoCloseButton

    // In a dock node, the Collapse Button turns into the Window Menu button.
    // FIXME-DOCK FIXME-OPT: Could we recycle popups id across multiple dock nodes?
    if (hasWindowMenuButton && isPopupOpen("#WindowMenu")) {
        val tabId = dockNodeUpdateWindowMenu(node, tabBar)
        if (tabId != 0) {
            focusTabId = tabId
            tabBar.nextSelectedTabId = tabId
        }
        isFocused = isFocused || node.isFocused
    }

    // Layout
    val titleBarRect = Rect()
    val tabBarRect = Rect()
    val windowMenuButtonPos = Vec2()
    dockNodeCalcTabBarLayout(node, titleBarRect, tabBarRect, windowMenuButtonPos)

    // Title bar
    if (isFocused)
        node.lastFrameFocused = g.frameCount
    val titleBarCol = if (hostWindow.collapsed) Col.TitleBgCollapsed else if (isFocused) Col.TitleBgActive else Col.TitleBg
    hostWindow.drawList.addRectFilled(titleBarRect.min, titleBarRect.max, titleBarCol.u32, hostWindow.windowRounding, DrawCornerFlag.Top.i)

    // Docking/Collapse button
    if (hasWindowMenuButton) {
        if (collapseButton(hostWindow.getID("#COLLAPSE"), windowMenuButtonPos, node))
            openPopup("#WindowMenu")
        if (isItemActive)
            focusTabId = tabBar.selectedTabId
    }

    // Submit new tabs and apply NavWindow focus back to the tab bar. They will be added as Unsorted and sorted below based on relative DockOrder value.
    val tabsCountOld = tabBar.tabs.size
    node.windows.forEach { window ->
        g.navWindow?.let {
            if (it.rootWindowDockStop === window)
                tabBar.selectedTabId = window.id
        }
        if (tabBar.findTabByID(window.id) == null)
            tabBar.addTab(TabItemFlag._Unsorted.i, window)
    }

    // If multiple tabs are appearing on the same frame, sort them based on their persistent DockOrder value
    var tabsUnsortedStart = tabBar.tabs.size
    var tabN = tabBar.tabs.lastIndex
    while (tabN >= 0 && tabBar.tabs[tabN].flags has TabItemFlag._Unsorted) {
        // FIXME-DOCK: Consider only clearing the flag after the tab has been alive for a few consecutive frames, allowing late comers to not break sorting?
        tabBar.tabs[tabN].flags = tabBar.tabs[tabN].flags or TabItemFlag._Unsorted
        tabsUnsortedStart = tabN--
    }
    if (tabBar.tabs.size > tabsUnsortedStart) {
        val s = if (tabBar.tabs.size > tabsUnsortedStart + 1) " (will sort)" else ""
        IMGUI_DEBUG_LOG_DOCKING("In node 0x%08X: ${tabBar.tabs.size - tabsUnsortedStart} new appearing tabs:$s".format(node.id))
        tabN = tabsUnsortedStart
        while (tabN < tabBar.tabs.size)
            IMGUI_DEBUG_LOG_DOCKING(" - Tab '${tabBar.tabs[tabN].window!!.name}' Order ${tabBar.tabs[tabN++].window!!.dockOrder}")
        if (tabBar.tabs.size > tabsUnsortedStart + 1)
            tabBar.tabs.sortWith(tabItemComparerByDockOrder)
    }

    // Selected newly added tabs, or persistent tab ID if the tab bar was just recreated
    if (tabBarIsRecreated && tabBar.findTabByID(node.selectedTabId) != null) {
        tabBar.selectedTabId = node.selectedTabId
        tabBar.nextSelectedTabId = node.selectedTabId
    } else if (tabBar.tabs.size > tabsCountOld) {
        tabBar.selectedTabId = tabBar.tabs.last().window!!.id
        tabBar.nextSelectedTabId = tabBar.tabs.last().window!!.id
    }

    // Begin tab bar
    var tabBarFlags = TabBarFlag.Reorderable or TabBarFlag.AutoSelectNewTabs or // | ImGuiTabBarFlags_NoTabListScrollingButtons);
            TabBarFlag._SaveSettings or TabBarFlag._DockNode
    if (!hostWindow.collapsed && isFocused)
        tabBarFlags = tabBarFlags or TabBarFlag._IsFocused
    tabBar.beginEx(tabBarRect, tabBarFlags, node)
    //host_window->DrawList->AddRect(tab_bar_rect.Min, tab_bar_rect.Max, IM_COL32(255,0,255,255));

    // Submit actual tabs
    node.visibleWindow = null
    for (window in node.windows) {
        if ((closedAll || closedOne == window.id) && window.hasCloseButton && window.flags hasnt Wf.UnsavedDocument)
            continue
        if (window.lastFrameActive + 1 >= g.frameCount || !nodeWasActive) {
            var tabItemFlags = TabItemFlag.None.i
            if (window.flags has Wf.UnsavedDocument)
                tabItemFlags = tabItemFlags or TabItemFlag.UnsavedDocument
            if (tabBar.flags has TabBarFlag.NoCloseWithMiddleMouseButton)
                tabItemFlags = tabItemFlags or TabItemFlag.NoCloseWithMiddleMouseButton

            val tabOpen = ::_b.also {
                it.set(true)
            }
            tabBar.tabItemEx(window.name, if (window.hasCloseButton) tabOpen else null, tabItemFlags, window)
            if (!tabOpen())
                node.wantCloseTabId = window.id
            if (tabBar.visibleTabId == window.id)
                node.visibleWindow = window

            // Store last item data so it can be queried with IsItemXXX functions after the user Begin() call
            window.dockTabItemStatusFlags = hostWindow.dc.lastItemStatusFlags
            window.dockTabItemRect put hostWindow.dc.lastItemRect

            // Update navigation ID on menu layer
            g.navWindow?.let {
                if (it.rootWindowDockStop === window && window.dc.navLayerActiveMask hasnt (1 shl NavLayer.Menu))
                    hostWindow.navLastIds[1] = window.id
            }
        }
    }

    // Notify root of visible window (used to display title in OS task bar)
    node.visibleWindow?.let {
        if (isFocused || rootNode.visibleWindow == null)
            rootNode.visibleWindow = it
    }

    // Close button (after VisibleWindow was updated)
    // Note that VisibleWindow may have been overrided by CTRL+Tabbing, so VisibleWindow->ID may be != from tab_bar->SelectedTabId
    if (hasCloseButton)
        node.visibleWindow?.let { visibleWindow ->
            if (!visibleWindow.hasCloseButton) {
                pushItemFlag(ItemFlag.Disabled.i, true)
                pushStyleColor(Col.Text, style.colors[Col.Text] * Vec4(1f, 1f, 1f, 0.5f))
            }
            val buttonSz = g.fontSize
            if (closeButton(hostWindow.getID("#CLOSE"), titleBarRect.tr + Vec2(-style.framePadding.x * 2f - buttonSz, 0f)))
                tabBar.findTabByID(tabBar.visibleTabId)?.let {
                    node.wantCloseTabId = it.id
                    tabBar closeTab it
                }
            //if (IsItemActive())
            //    focus_tab_id = tab_bar->SelectedTabId;
            if (!visibleWindow.hasCloseButton) {
                popStyleColor()
                popItemFlag()
            }
        }

    // When clicking on the title bar outside of tabs, we still focus the selected tab for that node
    // FIXME: TabItem use AllowItemOverlap so we manually perform a more specific test for now (hovered || held)
    val titleBarId = hostWindow.getID("#TITLEBAR")
    if (g.hoveredId == 0 || g.hoveredId == titleBarId || g.activeId == titleBarId) {
        val (_, _, held) = buttonBehavior(titleBarRect, titleBarId)
        if (held) {
            if (isMouseClicked(MouseButton.Left))
                focusTabId = tabBar.selectedTabId

            // Forward moving request to selected window
            tabBar.findTabByID(tabBar.selectedTabId)?.let {
                startMouseMovingWindowOrNode(it.window!!, node, false)
            }
        }
    }

    // Forward focus from host node to selected window
    //if (is_focused && g.NavWindow == host_window && !g.NavWindowingTarget)
    //    focus_tab_id = tab_bar->SelectedTabId;

    // When clicked on a tab we requested focus to the docked child
    // This overrides the value set by "forward focus from host node to selected window".
    if (tabBar.nextSelectedTabId != 0)
        focusTabId = tabBar.nextSelectedTabId

    // Apply navigation focus
    if (focusTabId != 0)
        tabBar.findTabByID(focusTabId)?.let {
            focusWindow(it.window)
            navInitWindow(it.window!!, false)
        }

    endTabBar()
    popID()

    // Restore SkipItems flag
    if (!node.isDockSpace) {
        hostWindow.dc.navLayerCurrent = NavLayer.Main
        hostWindow.skipItems = backupSkipItem
    }
}

fun dockNodeAddTabBar(node: DockNode) {
    assert(node.tabBar == null)
    node.tabBar = TabBar()
}

fun dockNodeRemoveTabBar(node: DockNode) {
    if (node.tabBar == null)
        return
    node.tabBar = null
}

fun dockNodeUpdateWindowMenu(node: DockNode, tabBar: TabBar): ID {
    // Try to position the menu so it is more likely to stays within the same viewport
    var retTabId: ID = 0
    if (style.windowMenuButtonPosition == Dir.Left)
        setNextWindowPos(Vec2(node.pos.x, node.pos.y + frameHeight), Cond.Always, Vec2())
    else
        setNextWindowPos(Vec2(node.pos.x + node.size.x, node.pos.y + frameHeight), Cond.Always, Vec2(1f, 0f))
    if (beginPopup("#WindowMenu")) {
        node.isFocused = true
        if (tabBar.tabs.size == 1) {
            if (menuItem("Hide tab bar", "", node.isHiddenTabBar))
                node.wantHiddenTabBarToggle = true
        } else
            for (tab in tabBar.tabs) {
                assert(tab.window != null)
                if (selectable(tab.window!!.name, tab.id == tabBar.selectedTabId))
                    retTabId = tab.id
                sameLine()
                text("   ")
            }
        endPopup()
    }
    return retTabId
}

fun dockNodeUpdateVisibleFlag(node: DockNode) {
    // Update visibility flag
    var isVisible = if (node.parentNode == null) node.isDockSpace else node.isCentralNode
    isVisible = isVisible || node.windows.isNotEmpty()
    isVisible = isVisible || node.childNodes[0]?.isVisible == true
    isVisible = isVisible || node.childNodes[1]?.isVisible == true
    node.isVisible = isVisible
}

fun dockNodeStartMouseMovingWindow(node: DockNode, window: Window) {
    assert(node.wantMouseMove)
    window.startMouseMoving()
    g.activeIdClickOffset = g.io.mouseClickedPos[0] - node.pos
    g.movingWindow = window // If we are docked into a non moveable root window, StartMouseMovingWindow() won't set g.MovingWindow. Override that decision.
    node.wantMouseMove = false
}

fun dockNodeIsDropAllowedOne(payload: Window, hostWindow: Window): Boolean {
    hostWindow.dockNodeAsHost?.let {
        if (it.isDockSpace && payload.beginOrderWithinContext < hostWindow.beginOrderWithinContext)
            return false
    }

    val hostClass = hostWindow.dockNodeAsHost?.windowClass ?: hostWindow.windowClass
    val payloadClass = payload.windowClass
    if (hostClass.classId != payloadClass.classId)
        return when {
            hostClass.classId != 0 && hostClass.dockingAllowUnclassed && payloadClass.classId == 0 -> true
            payloadClass.classId != 0 && payloadClass.dockingAllowUnclassed && hostClass.classId == 0 -> true
            else -> false
        }

    return true
}

fun dockNodeIsDropAllowed(hostWindow: Window, rootPayload: Window): Boolean {
    if (rootPayload.dockNodeAsHost?.isSplitNode == true)
        return true

    val payloadCount = rootPayload.dockNodeAsHost?.windows?.size ?: 1
    for (payloadN in 0 until payloadCount) {
        val payload = rootPayload.dockNodeAsHost?.windows?.get(payloadN) ?: rootPayload
        if (dockNodeIsDropAllowedOne(payload, hostWindow))
            return true
    }
    return false
}

/** host_node may be NULL if the window doesn't have a DockNode already.
 *  FIXME-DOCK: This is misnamed since it's also doing the filtering. */
fun dockNodePreviewDockSetup(hostWindow: Window, hostNode: DockNode?, rootPayload: Window, data: DockPreviewData,
                             isExplicitTarget: Boolean, isOuterDocking: Boolean) {

    // There is an edge case when docking into a dockspace which only has inactive nodes.
    // In this case DockNodeTreeFindNodeByPos() will have selected a leaf node which is inactive.
    // Because the inactive leaf node doesn't have proper pos/size yet, we'll use the root node as reference.
    val rootPayloadAsHost = rootPayload.dockNodeAsHost
    val refNodeForRect = if (hostNode?.isVisible == false) dockNodeGetRootNode(hostNode) else hostNode
    if (refNodeForRect != null)
        assert(refNodeForRect.isVisible)

    // Filter, figure out where we are allowed to dock
    val srcNodeFlags = rootPayloadAsHost?.mergedFlags ?: rootPayload.windowClass.dockNodeFlagsOverrideSet
    val dstNodeFlags = hostNode?.mergedFlags ?: hostWindow.windowClass.dockNodeFlagsOverrideSet
    data.isCenterAvailable = true
    if (isOuterDocking)
        data.isCenterAvailable = false
    else if (dstNodeFlags has DockNodeFlag._NoDocking)
        data.isCenterAvailable = false
    else if (hostNode != null && dstNodeFlags has DockNodeFlag.NoDockingInCentralNode && hostNode.isCentralNode)
        data.isCenterAvailable = false
    else if ((hostNode == null || !hostNode.isEmpty) && rootPayloadAsHost?.isSplitNode == true && rootPayloadAsHost.onlyNodeWithWindows == null) // Is _visibly_ split?
        data.isCenterAvailable = false
    else if (dstNodeFlags has DockNodeFlag._NoDockingOverMe || srcNodeFlags has DockNodeFlag._NoDockingOverOther)
        data.isCenterAvailable = false

    data.isSidesAvailable = true
    if (dstNodeFlags has DockNodeFlag.NoSplit || io.configDockingNoSplit)
        data.isSidesAvailable = false
    else if (!isOuterDocking && hostNode != null && hostNode.parentNode == null && hostNode.isCentralNode)
        data.isSidesAvailable = false
    else if (dstNodeFlags has DockNodeFlag._NoDockingSplitMe || srcNodeFlags has DockNodeFlag._NoDockingSplitOther)
        data.isSidesAvailable = false

    // Build a tentative future node (reuse same structure because it is practical. Shape will be readjusted when previewing a split)
    data.futureNode.hasCloseButton = (hostNode?.hasCloseButton
            ?: hostWindow.hasCloseButton) || rootPayload.hasCloseButton
    data.futureNode.hasWindowMenuButton = if (hostNode != null) true else hostWindow.flags hasnt Wf.NoCollapse
    data.futureNode.pos put if (hostNode != null) refNodeForRect!!.pos else hostWindow.pos
    data.futureNode.size put if (hostNode != null) refNodeForRect!!.size else hostWindow.size

    // Calculate drop shapes geometry for allowed splitting directions
    assert(Dir.None.i == -1)
    data.splitNode = hostNode
    data.splitDir = Dir.None
    data.isSplitDirExplicit = false
    if (!hostWindow.collapsed)
        for (dir in Dir.values()) {
            if (dir == Dir.None && !data.isCenterAvailable)
                continue
            if (dir != Dir.None && !data.isSidesAvailable)
                continue
            if (dockNodeCalcDropRectsAndTestMousePos(data.futureNode.rect(), dir, data.dropRectsDraw[dir.i + 1], isOuterDocking, io.mousePos)) {
                data.splitDir = dir
                data.isSplitDirExplicit = true
            }
        }

    // When docking without holding Shift, we only allow and preview docking when hovering over a drop rect or over the title bar
    data.isDropAllowed = data.splitDir != Dir.None || data.isCenterAvailable
    if (!isExplicitTarget && !data.isSplitDirExplicit && !io.configDockingWithShift)
        data.isDropAllowed = false

    // Calculate split area
    data.splitRatio = 0f
    if (data.splitDir != Dir.None) {
        val splitDir = data.splitDir
        val splitAxis = if (splitDir == Dir.Left || splitDir == Dir.Right) Axis.X else Axis.Y
        val posNew = Vec2()
        val posOld = data.futureNode.pos // [JVM] safe
        val sizeNew = Vec2()
        val sizeOld = data.futureNode.size // [JVM] safe
        dockNodeCalcSplitRects(posOld, sizeOld, posNew, sizeNew, splitDir, Vec2(rootPayload.size))

        // Calculate split ratio so we can pass it down the docking request
        val splitRatio = saturate(sizeNew[splitAxis] / data.futureNode.size[splitAxis])
        data.futureNode.pos put posNew
        data.futureNode.size put sizeNew
        data.splitRatio = if (splitDir == Dir.Right || splitDir == Dir.Down) 1f - splitRatio else splitRatio
    }
}

fun dockNodePreviewDockRender(hostWindow: Window, hostNode: DockNode?, rootPayload: Window, data: DockPreviewData) {

    assert(g.currentWindow === hostWindow) { "Because we rely on font size to calculate tab sizes" }

    // With this option, we only display the preview on the target viewport, and the payload viewport is made transparent.
    // To compensate for the single layer obstructed by the payload, we'll increase the alpha of the preview nodes.
    val isTransparentPayload = io.configDockingTransparentPayload

    // In case the two windows involved are on different viewports, we will draw the overlay on each of them.
    val overlayDrawLists = mutableListOf(getForegroundDrawList(hostWindow.viewport!!))
    if (hostWindow.viewport !== rootPayload.viewport && !isTransparentPayload)
        overlayDrawLists += getForegroundDrawList(rootPayload.viewport!!)

    // Draw main preview rectangle
    val overlayColTabs = Col.TabActive.u32
    val overlayColMain = getColorU32(Col.DockingPreview, if (isTransparentPayload) 0.6f else 0.4f)
    val overlayColDrop = getColorU32(Col.DockingPreview, if (isTransparentPayload) 0.9f else 0.7f)
    val overlayColDropHovered = getColorU32(Col.DockingPreview, if (isTransparentPayload) 1.2f else 1f)
    val overlayColLines = getColorU32(Col.NavWindowingHighlight, if (isTransparentPayload) 0.8f else 0.6f)

    // Display area preview
    val canPreviewTabs = rootPayload.dockNodeAsHost.let { it == null || it.windows.isNotEmpty() }
    if (data.isDropAllowed) {
        val overlayRect = data.futureNode.rect()
        if (data.splitDir == Dir.None && canPreviewTabs)
            overlayRect.min.y += frameHeight
        if (data.splitDir != Dir.None || data.isCenterAvailable)
            for (overlay in overlayDrawLists)
                overlay.addRectFilled(overlayRect.min, overlayRect.max, overlayColMain, hostWindow.windowRounding)
    }

    // Display tab shape/label preview unless we are splitting node (it generally makes the situation harder to read)
    if (data.isDropAllowed && canPreviewTabs && data.splitDir == Dir.None && data.isCenterAvailable) {
        // Compute target tab bar geometry so we can locate our preview tabs
        val tabBarRect = Rect()
        dockNodeCalcTabBarLayout(data.futureNode, null, tabBarRect, null)
        val tabPos = Vec2(tabBarRect.min)
        if (hostNode?.tabBar != null) {
            hostNode.tabBar?.let {
                tabPos.x += when {
                    // We don't use OffsetNewTab because when using non-persistent-order tab bar it is incremented with each Tab submission.
                    !hostNode.isHiddenTabBar && !hostNode.isNoTabBar -> it.widthAllTabs + style.itemInnerSpacing.x
                    else -> style.itemInnerSpacing.x + tabItemCalcSize(hostNode.windows[0].name, hostNode.windows[0].hasCloseButton).x
                }
            }
        } else if (hostWindow.flags hasnt Wf._DockNodeHost)
            tabPos.x += style.itemInnerSpacing.x + tabItemCalcSize(hostWindow.name, hostWindow.hasCloseButton).x // Account for slight offset which will be added when changing from title bar to tab bar

        // Draw tab shape/label preview (payload may be a loose window or a host window carrying multiple tabbed windows)
        rootPayload.dockNodeAsHost?.let {
            assert(it.windows.size == it.tabBar!!.tabs.size)
        }
        val payloadCount = rootPayload.dockNodeAsHost?.tabBar?.tabs?.size ?: 1
        for (payloadN in 0 until payloadCount) {
            // Calculate the tab bounding box for each payload window
            val payload = rootPayload.dockNodeAsHost?.tabBar?.tabs?.get(payloadN)?.window ?: rootPayload
            if (!dockNodeIsDropAllowedOne(payload, hostWindow))
                continue

            val tabSize = tabItemCalcSize(payload.name, payload.hasCloseButton)
            val tabBb = Rect(tabPos.x, tabPos.y, tabPos.x + tabSize.x, tabPos.y + tabSize.y)
            tabPos.x += tabSize.x + style.itemInnerSpacing.x
            for (drawList in overlayDrawLists) {
                val tabFlags = TabItemFlag._Preview or if (payload.flags has Wf.UnsavedDocument) TabItemFlag.UnsavedDocument else TabItemFlag.None
                if (tabBb !in tabBarRect)
                    drawList.pushClipRect(tabBarRect.min, tabBarRect.max)
                tabItemBackground(drawList, tabBb, tabFlags, overlayColTabs)
                tabItemLabelAndCloseButton(drawList, tabBb, tabFlags, style.framePadding, payload.name.toByteArray(), 0, 0, false)
                if (tabBb !in tabBarRect)
                    drawList.popClipRect()
            }
        }
    }

    // Display drop boxes
    val overlayRounding = 3f max style.frameRounding
    for (dir in Dir.values()) {
        if (!data.dropRectsDraw[dir.i + 1].isInverted) {
            val drawR = data.dropRectsDraw[dir.i + 1]
            val drawRIn = Rect(drawR)
            drawRIn expand -2f
            val overlayCol = if (data.splitDir == dir && data.isSplitDirExplicit) overlayColDropHovered else overlayColDrop
            for (drawList in overlayDrawLists) {
                val center = floor(drawRIn.center)
                drawList.addRectFilled(drawR.min, drawR.max, overlayCol, overlayRounding)
                drawList.addRect(drawRIn.min, drawRIn.max, overlayColLines, overlayRounding)
                if (dir == Dir.Left || dir == Dir.Right)
                    drawList.addLine(Vec2(center.x, drawRIn.min.y), Vec2(center.x, drawRIn.max.y), overlayColLines)
                if (dir == Dir.Up || dir == Dir.Down)
                    drawList.addLine(Vec2(drawRIn.min.x, center.y), Vec2(drawRIn.max.x, center.y), overlayColLines)
            }
        }

        // Stop after ImGuiDir_None
        if (hostNode?.mergedFlags?.has(DockNodeFlag.NoSplit) == true || io.configDockingNoSplit)
            return
    }
}

/** window menu button == collapse button when not in a dock node.
 *  FIXME: This is similar to RenderWindowTitleBarContents, may want to share code. */
fun dockNodeCalcTabBarLayout(node: DockNode, outTitleRect: Rect?, outTabBarRect: Rect?, outWindowMenuButtonPos: Vec2?) {

    val r = Rect(node.pos.x, node.pos.y, node.pos.x + node.size.x, node.pos.y + g.fontSize + style.framePadding.y * 2f)
    outTitleRect?.put(r)

    val windowMenuButtonPos = Vec2(r.min)
    r.min.x += style.framePadding.x
    r.max.x -= style.framePadding.x
    if (node.hasCloseButton)
        r.max.x -= g.fontSize// +1.0f; // In DockNodeUpdateTabBar() we currently display a disabled close button even if there is none.
    if (node.hasWindowMenuButton && style.windowMenuButtonPosition == Dir.Left)
        r.min.x += g.fontSize // + g.Style.ItemInnerSpacing.x; // <-- Adding ItemInnerSpacing makes the title text moves slightly when in a docking tab bar. Instead we adjusted RenderArrowDockMenu()
    else if (node.hasWindowMenuButton && style.windowMenuButtonPosition == Dir.Right) {
        r.max.x -= g.fontSize + style.framePadding.x
        windowMenuButtonPos.put(r.max.x, r.min.y)
    }
    outTabBarRect?.put(r)
    outWindowMenuButtonPos?.put(windowMenuButtonPos)
}

/** [JVM] all Vec2 safe */
fun dockNodeCalcSplitRects(posOld: Vec2, sizeOld: Vec2, posNew: Vec2, sizeNew: Vec2, dir: Dir, sizeNewDesired: Vec2) {

    val dockSpacing = style.itemInnerSpacing.x
    val axis = if (dir == Dir.Left || dir == Dir.Right) Axis.X else Axis.Y
    posNew[axis xor 1] = posOld[axis xor 1]
    sizeNew[axis xor 1] = sizeOld[axis xor 1]

    // Distribute size on given axis (with a desired size or equally)
    val wAvail = sizeOld[axis] - dockSpacing
    if (sizeNewDesired[axis] > 0f && sizeNewDesired[axis] <= wAvail * 0.5f) {
        sizeNew[axis] = sizeNewDesired[axis]
        sizeOld[axis] = floor(wAvail - sizeNew[axis])
    } else {
        sizeNew[axis] = floor(wAvail * 0.5f)
        sizeOld[axis] = floor(wAvail - sizeNew[axis])
    }

    // Position each node
    if (dir == Dir.Right || dir == Dir.Down)
        posNew[axis] = posOld[axis] + sizeOld[axis] + dockSpacing
    else if (dir == Dir.Left || dir == Dir.Up) {
        posNew[axis] = posOld[axis]
        posOld[axis] = posNew[axis] + sizeNew[axis] + dockSpacing
    }
}

/** Retrieve the drop rectangles for a given direction or for the center + perform hit testing. */
fun dockNodeCalcDropRectsAndTestMousePos(parent: Rect, dir: Dir, outR: Rect, outerDocking: Boolean, testMousePos: Vec2?): Boolean {

    val parentSmallerAxis = parent.width min parent.height
    val hsForCentralNodes = (g.fontSize * 1.5f) min max(g.fontSize * 0.5f, parentSmallerAxis / 8f)
    val hsW: Float // Half-size, longer axis
    val hsH: Float // Half-size, smaller axis
    val off: Vec2 // Distance from edge or center
    if (outerDocking) {
        //hs_w = ImFloor(ImClamp(parent_smaller_axis - hs_for_central_nodes * 4.0f, g.FontSize * 0.5f, g.FontSize * 8.0f));
        //hs_h = ImFloor(hs_w * 0.15f);
        //off = ImVec2(ImFloor(parent.GetWidth() * 0.5f - GetFrameHeightWithSpacing() * 1.4f - hs_h), ImFloor(parent.GetHeight() * 0.5f - GetFrameHeightWithSpacing() * 1.4f - hs_h));
        hsW = floor(hsForCentralNodes * 1.5f)
        hsH = floor(hsForCentralNodes * 0.8f)
        off = Vec2(floor(parent.width * 0.5f - hsH), floor(parent.height * 0.5f - hsH))
    } else {
        hsW = floor(hsForCentralNodes)
        hsH = floor(hsForCentralNodes * 0.9f)
        off = Vec2(floor(hsW * 2.4f), floor(hsW * 2.4f))
    }

    val c = floor(parent.center)
    // @formatter:off
    when (dir) {
        Dir.None -> outR.put(c.x - hsW, c.y - hsW, c.x + hsW, c.y + hsW)
        Dir.Up -> outR.put(c.x - hsW, c.y - off.y - hsH, c.x + hsW, c.y - off.y + hsH)
        Dir.Down -> outR.put(c.x - hsW, c.y + off.y - hsH, c.x + hsW, c.y + off.y + hsH)
        Dir.Left -> outR.put(c.x - off.x - hsH, c.y - hsW, c.x - off.x + hsH, c.y + hsW)
        Dir.Right -> outR.put(c.x + off.x - hsH, c.y - hsW, c.x + off.x + hsH, c.y + hsW)
    }
    // @formatter:on
    if (testMousePos == null)
        return false

    val hitR = Rect(outR)
    if (!outerDocking) {
        // Custom hit testing for the 5-way selection, designed to reduce flickering when moving diagonally between sides
        hitR expand floor(hsW * 0.3f)
        val mouseDelta = testMousePos - c
        val mouseDeltaLen2 = mouseDelta.lengthSqr
        val rThresholdCenter = hsW * 1.4f
        val rThresholdSides = hsW * (1.4f + 1.2f)
        if (mouseDeltaLen2 < rThresholdCenter * rThresholdCenter)
            return dir == Dir.None
        if (mouseDeltaLen2 < rThresholdSides * rThresholdSides)
            return dir == getDirQuadrantFromDelta(mouseDelta.x, mouseDelta.y)
    }
    return testMousePos in hitR
}

fun dockNodeGetHostWindowTitle(node: DockNode): String = "##DockNode_%02X".format(node.id)

fun dockNodeGetTabOrder(window: Window): Int {
    val tabBar = window.dockNode!!.tabBar ?: return -1
    val tab = tabBar findTabByID window.id
    return tab?.let { tabBar getTabOrder it } ?: -1
}
