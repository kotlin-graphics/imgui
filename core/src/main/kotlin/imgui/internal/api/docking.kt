package imgui.internal.api

import gli_.hasnt
import glm_.vec2.Vec2
import glm_.wo
import imgui.*
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.beginDragDropSource
import imgui.ImGui.beginDragDropTargetCustom
import imgui.ImGui.dockBuilderRemoveNodeChildNodes
import imgui.ImGui.endDragDropSource
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.frameHeight
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.saveIniSettingsToMemory
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.*
import imgui.internal.hash
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.SettingsHandler
import imgui.internal.sections.has
import imgui.static.*
import kotlin.reflect.KMutableProperty0

// Docking
// (some functions are only declared in imgui.cpp, see Docking section)
interface docking {

    fun dockContextInitialize(ctx: Context) {
        val g = ctx
        assert(g.dockContext == null)
        g.dockContext = DockContext()

        // Add .ini handle for persistent docking data
        g.settingsHandlers += SettingsHandler().apply {
            typeName = "Docking"
            typeHash = hash("Docking")
            clearAllFn = ::dockSettingsHandler_ClearAll
            readInitFn = ::dockSettingsHandler_ClearAll // Also clear on read
            readOpenFn = ::dockSettingsHandler_ReadOpen
            readLineFn = ::dockSettingsHandler_ReadLine
            applyAllFn = ::dockSettingsHandler_ApplyAll
            writeAllFn = ::dockSettingsHandler_WriteAll
        }
    }

    fun dockContextShutdown(ctx: Context) {
        val g = ctx
        g.dockContext = null
    }

    /** This function also acts as a defacto test to make sure we can rebuild from scratch without a glitch
     * [DEBUG] This function also acts as a defacto test to make sure we can rebuild from scratch without a glitch
     * (Different from DockSettingsHandler_ClearAll() + DockSettingsHandler_ApplyAll() because this reuses current settings!) */
    fun dockContextRebuildNodes(ctx: Context) {
        IMGUI_DEBUG_LOG_DOCKING("DockContextRebuild()")
        val dc = ctx.dockContext!!
        saveIniSettingsToMemory()
        val rootID = 0 // Rebuild all
        dockContextClearNodes(ctx, rootID, false)
        dockContextBuildNodesFromSettings(ctx, dc.settingsNodes)
        dockContextBuildAddWindowsToNodes(ctx, rootID)
    }

    /** Docking context update function, called by NewFrame() */
    fun dockContextUpdateUndocking(ctx: Context) {

        val g = ctx
        val dc = ctx.dockContext!!
        if (g.io.configFlags hasnt ConfigFlag.DockingEnable) {
            if (dc.nodes.isNotEmpty() || dc.requests.isNotEmpty())
                dockContextClearNodes(ctx, 0, true)
            return
        }

        // Setting NoSplit at runtime merges all nodes
        if (g.io.configDockingNoSplit)
            dc.nodes.values.forEach {
                if (it.isRootNode && it.isSplitNode) {
                    dockBuilderRemoveNodeChildNodes(it.id)
                    //dc->WantFullRebuild = true;
                }
            }

        // Process full rebuild
//        #if 0
//        if (ImGui::IsKeyPressed(ImGui::GetKeyIndex(ImGuiKey_C)))
//            dc->WantFullRebuild = true
//        #endif
        if (dc.wantFullRebuild) {
            dockContextRebuildNodes(ctx)
            dc.wantFullRebuild = false
        }

        // Process Undocking requests (we need to process them _before_ the UpdateMouseMovingWindowNewFrame call in NewFrame)
        for (req in dc.requests)
            if (req.type == DockRequestType.Undock)
                req.undockTargetWindow?.let { dockContextProcessUndockWindow(ctx, it) }
            else if (req.type == DockRequestType.Undock)
                req.undockTargetNode?.let { dockContextProcessUndockNode(ctx, it) }
    }

    /** Docking context update function, called by NewFrame() */
    fun dockContextUpdateDocking(ctx: Context) {
        val g = ctx
        val dc = ctx.dockContext!!
        if (g.io.configFlags hasnt ConfigFlag.DockingEnable)
            return

        // Process Docking requests
        for (req in dc.requests)
            if (req.type == DockRequestType.Dock)
                dockContextProcessDock(ctx, req)
        dc.requests.clear()

        // Create windows for each automatic docking nodes
        // We can have NULL pointers when we delete nodes, but because ID are recycled this should amortize nicely (and our node count will never be very high)
        for (node in dc.nodes.values)
//            if (ImGuiDockNode* node = (ImGuiDockNode*)dc->Nodes.Data[n].val_p)
            if (node.isFloatingNode)
                dockNodeUpdate(node)
    }

    fun dockContextGenNodeID(ctx: Context): ID {
        // Generate an ID for new node (the exact ID value doesn't matter as long as it is not already used)
        // FIXME-OPT FIXME-DOCK: This is suboptimal, even if the node count is small enough not to be a worry. We should poke in ctx->Nodes to find a suitable ID faster.
        var id: ID = 0x0001
        while (dockContextFindNodeByID(ctx, id) != null)
            id++
        return id
    }

    fun dockContextQueueDock(ctx: Context, target: Window, targetNode: DockNode?, payload: Window, splitDir: Dir,
                             splitRatio: Float, splitOuter: Boolean) {
        assert(target !== payload)
        val req = DockRequest()
        req.type = DockRequestType.Dock
        req.dockTargetWindow = target
        req.dockTargetNode = targetNode
        req.dockPayload = payload
        req.dockSplitDir = splitDir
        req.dockSplitRatio = splitRatio
        req.dockSplitOuter = splitOuter
        ctx.dockContext!!.requests += req
    }

    fun dockContextQueueUndockWindow(ctx: Context, window: Window) {
        ctx.dockContext!!.requests += DockRequest().apply {
            type = DockRequestType.Undock
            undockTargetWindow = window
        }
    }

    fun dockContextQueueUndockNode(ctx: Context, node: DockNode) {
        ctx.dockContext!!.requests += DockRequest().apply {
            type = DockRequestType.Undock
            undockTargetNode = node
        }
    }

    /** This is mostly used for automation. */
    fun dockContextCalcDropPosForDocking(target: Window, targetNode: DockNode, payload: Window, splitDir: Dir,
                                         splitOuter: Boolean, outPos: Vec2): Boolean {
        if (splitOuter)
            assert(false)
        else {
            val splitData = DockPreviewData()
            dockNodePreviewDockSetup(target, targetNode, payload, splitData, false, splitOuter)
            if (splitData.dropRectsDraw[splitDir.i + 1].isInverted)
                return false
            outPos put splitData.dropRectsDraw[splitDir.i + 1].center
            return true
        }
        return false
    }

    fun dockNodeGetRootNode(node_: DockNode): DockNode {
        var node: DockNode? = node_
        while (node!!.parentNode != null)
            node = node.parentNode
        return node
    }

    /** ~GetWindowDockNode */
    val windowDockNode: DockNode?
        get() = g.currentWindow!!.dockNode

    fun getWindowAlwaysWantOwnTabBar(window: Window): Boolean {
        if (g.io.configDockingAlwaysTabBar || window.windowClass.dockingAlwaysTabBar)
            if (window.flags hasnt (WindowFlag._ChildWindow or WindowFlag.NoTitleBar or WindowFlag.NoDocking))
                if (!window.isFallbackWindow)    // We don't support AlwaysTabBar on the fallback/implicit window to avoid unused dock-node overhead/noise
                    return true
        return false
    }

    fun beginDocked(window: Window, pOpen: KMutableProperty0<Boolean>?) {

        val ctx = g
        val autoDockNode = getWindowAlwaysWantOwnTabBar(window)
        if (autoDockNode) {
            if (window.dockId == 0) {
                assert(window.dockNode == null)
                window.dockId = dockContextGenNodeID(ctx)
            }
        } else {
            // Calling SetNextWindowPos() undock windows by default (by setting PosUndock)
            var wantUndock = false
            wantUndock = wantUndock || window.flags has WindowFlag.NoDocking
            wantUndock = wantUndock || (g.nextWindowData.flags has NextWindowDataFlag.HasPos && window.setWindowPosAllowFlags has g.nextWindowData.posCond && g.nextWindowData.posUndock)
            if (wantUndock) {
                dockContextProcessUndockWindow(ctx, window)
                return
            }
        }

        // Bind to our dock node
        var node = window.dockNode
        node?.let { assert(window.dockId == it.id) }
        if (window.dockId != 0 && node == null) {
            node = dockContextBindNodeToWindow(ctx, window)
            if (node == null)
                return
        }

//        #if 0
//        // Undock if the ImGuiDockNodeFlags_NoDockingInCentralNode got set
//        if (node->IsCentralNode && (node->Flags & ImGuiDockNodeFlags_NoDockingInCentralNode))
//        {
//            DockContextProcessUndockWindow(ctx, window)
//            return
//        }
//        #endif

        // Undock if our dockspace node disappeared
        // Note how we are testing for LastFrameAlive and NOT LastFrameActive. A DockSpace node can be maintained alive while being inactive with ImGuiDockNodeFlags_KeepAliveOnly.
        if (node!!.lastFrameAlive < g.frameCount) {
            // If the window has been orphaned, transition the docknode to an implicit node processed in DockContextUpdateDocking()
            val rootNode = dockNodeGetRootNode(node)
            if (rootNode.lastFrameAlive < g.frameCount)
                dockContextProcessUndockWindow(ctx, window)
            else {
                window.dockIsActive = true
                window.dockTabIsVisible = false
            }
            return
        }

        // Fast path return. It is common for windows to hold on a persistent DockId but be the only visible window,
        // and never create neither a host window neither a tab bar.
        // FIXME-DOCK: replace ->HostWindow NULL compare with something more explicit (~was initially intended as a first frame test)
        if (node.hostWindow == null) {
            window.dockIsActive = node.state == DockNodeState.HostWindowHiddenBecauseWindowsAreResizing
            window.dockTabIsVisible = false
            return
        }

        // We can have zero-sized nodes (e.g. children of a small-size dockspace)
        assert(node.hostWindow != null)
        assert(node.isLeafNode)
        assert(node.size.x >= 0f && node.size.y >= 0f) // TODO glm
        node.state = DockNodeState.HostWindowVisible

        // Undock if we are submitted earlier than the host window
        if (window.beginOrderWithinContext < node.hostWindow!!.beginOrderWithinContext) {
            dockContextProcessUndockWindow(ctx, window)
            return
        }

        // Position/Size window
        setNextWindowPos(node.pos)
        setNextWindowSize(node.size)
        g.nextWindowData.posUndock = false // Cancel implicit undocking of SetNextWindowPos()
        window.dockIsActive = true
        window.dockTabIsVisible = false
        if (node.sharedFlags has DockNodeFlag.KeepAliveOnly)
            return

        // When the window is selected we mark it as visible.
        if (node.visibleWindow === window)
            window.dockTabIsVisible = true

        // Update window flag
        assert(window.flags hasnt WindowFlag._ChildWindow)
        window.flags = window.flags or (WindowFlag._ChildWindow or WindowFlag.AlwaysUseWindowPadding or WindowFlag.NoResize)
        window.flags = when {
            node.isHiddenTabBar || node.isNoTabBar -> window.flags or WindowFlag.NoTitleBar
            else -> window.flags wo WindowFlag.NoTitleBar      // Clear the NoTitleBar flag in case the user set it: confusingly enough we need a title bar height so we are correctly offset, but it won't be displayed!
        }

        // Save new dock order only if the tab bar has been visible once.
        // This allows multiple windows to be created in the same frame and have their respective dock orders preserved.
        node.tabBar?.let {
            if (it.currFrameVisible != -1)
                window.dockOrder = dockNodeGetTabOrder(window)
        }

        if ((node.wantCloseAll || node.wantCloseTabId == window.id) && pOpen != null)
            pOpen.set(false)

        // Update ChildId to allow returning from Child to Parent with Escape
        val parentWindow = window.dockNode!!.hostWindow
        window.childId = parentWindow!!.getID(window.name)
    }

    fun beginDockableDragDropSource(window_: Window) {

        var window = window_
        assert(g.activeId == window.moveId)
        assert(g.movingWindow === window)

        window.dc.lastItemId = window.moveId
        window = window.rootWindow!!
        assert(window.flags hasnt WindowFlag.NoDocking)
        val isDragDocking = io.configDockingWithShift || g.activeIdClickOffset in Rect(0f, 0f, window.sizeFull.x, frameHeight)
        if (isDragDocking && beginDragDropSource(DragDropFlag.SourceNoPreviewTooltip or DragDropFlag.SourceNoHoldToOpenOthers or DragDropFlag.SourceAutoExpirePayload)) {
            setDragDropPayload(IMGUI_PAYLOAD_TYPE_WINDOW, window)
            endDragDropSource()
        }
    }

    fun beginDockableDragDropTarget(window: Window) {

        //IM_ASSERT(window->RootWindow == window); // May also be a DockSpace
        assert(window.flags hasnt WindowFlag.NoDocking)
        if (!g.dragDropActive)
            return
        if (!beginDragDropTargetCustom(window.rect(), window.id))
            return

        // Peek into the payload before calling AcceptDragDropPayload() so we can handle overlapping dock nodes with filtering
        // (this is a little unusual pattern, normally most code would call AcceptDragDropPayload directly)
        val payload = g.dragDropPayload
        if (!payload.isDataType(IMGUI_PAYLOAD_TYPE_WINDOW) || !dockNodeIsDropAllowed(window, payload.data as Window)) {
            endDragDropTarget()
            return
        }

        val payloadWindow = payload.data as Window
        acceptDragDropPayload(IMGUI_PAYLOAD_TYPE_WINDOW, DragDropFlag.AcceptBeforeDelivery or DragDropFlag.AcceptNoDrawDefaultRect)?.let {
            // Select target node
            var allowNullTargetNode = false
            val node: DockNode? = window.dockNodeAsHost?.let {
                dockNodeTreeFindNodeByPos(window.dockNodeAsHost!!, g.io.mousePos)
            } ?: window.dockNode ?: (null.also {allowNullTargetNode = true})

            val explicitTargetRect = node?.let { n -> n.tabBar?.let { t -> if (!n.isHiddenTabBar && !n.isNoTabBar) Rect(t.barRect) else null } }
                    ?: Rect(window.pos, window.pos + Vec2(window.size.x, frameHeight))
            val isExplicitTarget = io.configDockingWithShift || isMouseHoveringRect(explicitTargetRect.min, explicitTargetRect.max)

            // Preview docking request and find out split direction/ratio
            //const bool do_preview = true;     // Ignore testing for payload->IsPreview() which removes one frame of delay, but breaks overlapping drop targets within the same window.
            val doPreview = payload.preview || payload.delivery
            if (doPreview && (node != null || allowNullTargetNode)) {
                val splitInner = DockPreviewData()
                val splitOuter = DockPreviewData()
                var splitData = splitInner
                node?.let {
                    if (it.parentNode != null || it.isCentralNode) {
                        val rootNode = dockNodeGetRootNode(it)
                        dockNodePreviewDockSetup(window, rootNode, payloadWindow, splitOuter, isExplicitTarget, true)
                        if (splitOuter.isSplitDirExplicit)
                            splitData = splitOuter
                    }
                }
                dockNodePreviewDockSetup(window, node, payloadWindow, splitInner, isExplicitTarget, false)
                if (splitData == splitOuter)
                    splitInner.isDropAllowed = false

                // Draw inner then outer, so that previewed tab (in inner data) will be behind the outer drop boxes
                dockNodePreviewDockRender(window, node, payloadWindow, splitInner)
                dockNodePreviewDockRender(window, node, payloadWindow, splitOuter)

                // Queue docking request
                if (splitData.isDropAllowed && payload.delivery)
                    dockContextQueueDock(g, window, splitData.splitNode, payloadWindow, splitData.splitDir, splitData.splitRatio, splitData === splitOuter)
            }
        }
        endDragDropTarget()
    }

    /** [Internal] Called via SetNextWindowDockID() */
    fun setWindowDock(window: Window, dockId_: ID, cond: Cond) {
        var dockId = dockId_
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && window.setWindowDockAllowFlags hasnt cond)
            return
        window.setWindowDockAllowFlags = window.setWindowDockAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing)

        if (window.dockId == dockId)
            return

        // If the user attempt to set a dock id that is a split node, we'll dig within to find a suitable docking spot
        val ctx = g
        dockContextFindNodeByID(ctx, dockId)?.let { node ->
            var newNode = node
            if (newNode.isSplitNode) {
                // Policy: Find central node or latest focused node. We first move back to our root node.
                newNode = dockNodeGetRootNode(newNode)
                newNode.centralNode.let {
                    if (it != null) {
                        assert(it.isCentralNode)
                        dockId = it.id
                    } else
                        dockId = newNode.lastFocusedNodeId
                }
            }
        }

        if (window.dockId == dockId)
            return

        window.dockNode?.let {
            dockNodeRemoveWindow(it, window, 0)
        }
        window.dockId = dockId
    }
}