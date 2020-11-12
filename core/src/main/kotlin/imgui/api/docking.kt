package imgui.api

import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.getID
import imgui.ImGui.mainViewport
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowViewport
import imgui.classes.Viewport
import imgui.classes.WindowClass
import imgui.internal.floor
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.or
import imgui.static.dockContextAddNode
import imgui.static.dockContextFindNodeByID
import imgui.static.dockNodeUpdate

// Docking
// [BETA API] Enable with io.ConfigFlags |= ImGuiConfigFlags_DockingEnable.
// Note: You can use most Docking facilities without calling any API. You DO NOT need to call DockSpace() to use Docking!
// - To dock windows: if io.ConfigDockingWithShift == false (default) drag window from their title bar.
// - To dock windows: if io.ConfigDockingWithShift == true: hold SHIFT anywhere while moving windows.
// About DockSpace:
// - Use DockSpace() to create an explicit dock node _within_ an existing window. See Docking demo for details.
// - DockSpace() needs to be submitted _before_ any window they can host. If you use a dockspace, submit it early in your app.
interface docking {

    /** Create an explicit dockspace node within an existing window. Also expose dock node flags and creates a CentralNode by default.
     *  The Central Node is always displayed even when empty and shrink/extend according to the requested size of its neighbors.
     *  DockSpace() needs to be submitted _before_ any window they can host. If you use a dockspace, submit it early in your app. */
    fun dockSpace(id: ID, sizeArg: Vec2 = Vec2(), flags_: DockNodeFlags = DockNodeFlag.None.i, windowClass: WindowClass? = null) {
        val ctx = g
        val window = currentWindow
        if (g.io.configFlags hasnt ConfigFlag.DockingEnable)
            return

        // Early out if parent window is hidden/collapsed
        // This is faster but also DockNodeUpdateTabBar() relies on TabBarLayout() running (which won't if SkipItems=true) to set NextSelectedTabId = 0). See #2960.
        // If for whichever reason this is causing problem we would need to ensure that DockNodeUpdateTabBar() ends up clearing NextSelectedTabId even if SkipItems=true.
        var flags = flags_
        if (window.skipItems)
            flags = flags or DockNodeFlag.KeepAliveOnly

        assert(flags hasnt DockNodeFlag._DockSpace)
        val node = dockContextFindNodeByID(ctx, id) ?: dockContextAddNode(ctx, id).also {
            IMGUI_DEBUG_LOG_DOCKING("DockSpace: dockspace node 0x%08X created".format(id))
            it.localFlags = it.localFlags or DockNodeFlag._CentralNode
        }
        if (windowClass != null && windowClass.classId != node.windowClass.classId)
            IMGUI_DEBUG_LOG_DOCKING("DockSpace: dockspace node 0x%08X: setup WindowClass 0x%08X -> 0x%08X".format(id, node.windowClass.classId, windowClass.classId))
        node.sharedFlags = flags
        node.windowClass = windowClass ?: WindowClass()

        // When a DockSpace transitioned form implicit to explicit this may be called a second time
        // It is possible that the node has already been claimed by a docked window which appeared before the DockSpace() node, so we overwrite IsDockSpace again.
        if (node.lastFrameActive == g.frameCount && flags hasnt DockNodeFlag.KeepAliveOnly) {
            assert(!node.isDockSpace) { "Cannot call DockSpace() twice a frame with the same ID" }
            node.localFlags = node.localFlags or DockNodeFlag._DockSpace
            return
        }
        node.localFlags = node.localFlags or DockNodeFlag._DockSpace

        // Keep alive mode, this is allow windows docked into this node so stay docked even if they are not visible
        if (flags has DockNodeFlag.KeepAliveOnly) {
            node.lastFrameAlive = g.frameCount
            return
        }

        val contentAvail = contentRegionAvail
        val size = floor(sizeArg)
        if (size.x <= 0f)
            size.x = (contentAvail.x + size.x) max 4f // Arbitrary minimum child size (0.0f causing too much issues)
        if (size.y <= 0f)
            size.y = (contentAvail.y + size.y) max 4f
        assert(size.x > 0f && size.y > 0f) // TODO glm

        node.pos put window.dc.cursorPos
        node.size put size
        node.sizeRef put size
        setNextWindowPos(node.pos)
        setNextWindowSize(node.size)
        g.nextWindowData.posUndock = false

        // FIXME-DOCK Why do we need a child window to host a dockspace, could we host it in the existing window?
        var windowFlags = WindowFlag._ChildWindow or WindowFlag._DockNodeHost
        windowFlags = windowFlags or WindowFlag.NoSavedSettings or WindowFlag.NoResize or WindowFlag.NoCollapse or WindowFlag.NoTitleBar
        windowFlags = windowFlags or WindowFlag.NoScrollbar or WindowFlag.NoScrollWithMouse

        val title = "${window.name}/DockSpace_%08X".format(id)

        if (node.windows.isNotEmpty() || node.isSplitNode)
            pushStyleColor(Col.ChildBg, COL32(0, 0, 0, 0))
        pushStyleVar(StyleVar.ChildBorderSize, 0f)
        begin(title, null, windowFlags)
        popStyleVar()
        if (node.windows.isNotEmpty() || node.isSplitNode)
            popStyleColor()

        val hostWindow = g.currentWindow!!
        hostWindow.dockNodeAsHost = node
        hostWindow.childId = window.getID(title)
        node.hostWindow = hostWindow
        node.onlyNodeWithWindows = null

        assert(node.isRootNode)


        // We need to handle the rare case were a central node is missing.
        // This can happen if the node was first created manually with DockBuilderAddNode() but _without_ the ImGuiDockNodeFlags_Dockspace.
        // Doing it correctly would set the _CentralNode flags, which would then propagate according to subsequent split.
        // It would also be ambiguous to attempt to assign a central node while there are split nodes, so we wait until there's a single node remaining.
        // The specific sub-property of _CentralNode we are interested in recovering here is the "Don't delete when empty" property,
        // as it doesn't make sense for an empty dockspace to not have this property.
        if (node.isLeafNode && !node.isCentralNode)
            node.localFlags = node.localFlags or DockNodeFlag._CentralNode

        // Update the node
        dockNodeUpdate(node)

        g.withinEndChild = true
        end()
        g.withinEndChild = false
    }

    /** Tips: Use with ImGuiDockNodeFlags_PassthruCentralNode!
     *  The limitation with this call is that your window won't have a menu bar.
     *  Even though we could pass window flags, it would also require the user to be able to call BeginMenuBar() somehow meaning we can't Begin/End in a single function.
     *  But you can also use BeginMainMenuBar(). If you really want a menu bar inside the same window as the one hosting the dockspace, you will need to copy this code somewhere and tweak it. */
    fun dockSpaceOverViewport(viewport_: Viewport? = null, dockspaceFlags: DockNodeFlags = DockNodeFlag.None.i, windowClass: WindowClass? = null): ID {

        val viewport = viewport_ ?: mainViewport

        setNextWindowPos(viewport.workPos)
        setNextWindowSize(viewport.workSize)
        setNextWindowViewport(viewport.id)

        var hostWindowFlags = WindowFlag.None.i
        hostWindowFlags = hostWindowFlags or WindowFlag.NoTitleBar or WindowFlag.NoCollapse or WindowFlag.NoResize or WindowFlag.NoMove or WindowFlag.NoDocking
        hostWindowFlags = hostWindowFlags or WindowFlag.NoBringToFrontOnFocus or WindowFlag.NoNavFocus
        if (dockspaceFlags has DockNodeFlag.PassthruCentralNode)
            hostWindowFlags = hostWindowFlags or WindowFlag.NoBackground

        val label = "DockSpaceViewport_%08X".format(viewport.id)

        pushStyleVar(StyleVar.WindowRounding, 0f)
        pushStyleVar(StyleVar.WindowBorderSize, 0f)
        pushStyleVar(StyleVar.WindowPadding, Vec2())
        begin(label, null, hostWindowFlags)
        popStyleVar(3)

        val dockspaceId = getID("DockSpace")
        dockSpace(dockspaceId, Vec2(), dockspaceFlags, windowClass)
        end()

        return dockspaceId
    }

    /** set next window dock id (FIXME-DOCK) */
    fun setNextWindowDockID(id: ID, cond: Cond = Cond.None) {
        g.nextWindowData.flags = g.nextWindowData.flags or NextWindowDataFlag.HasDock
        g.nextWindowData.dockCond = if (cond != Cond.None) cond else Cond.Always
        g.nextWindowData.dockId = id
    }

    /** set next window class (rare/advanced uses: provide hints to the platform back-end via altered viewport flags and parent/child info) */
    fun setNextWindowClass(windowClass: WindowClass) {
        assert((windowClass.viewportFlagsOverrideSet and windowClass.viewportFlagsOverrideClear) == 0) { "Cannot set both set and clear for the same bit" }
        g.nextWindowData.flags = g.nextWindowData.flags or NextWindowDataFlag.HasWindowClass
        g.nextWindowData.windowClass = windowClass
    }

    /** ~GetWindowDockID */
    val windowDockID: ID
        get() = g.currentWindow!!.dockId

    /** is current window docked into another window?
     *  ~IsWindowDocked */
    val isWindowDocked: Boolean
        get() = g.currentWindow!!.dockIsActive
}