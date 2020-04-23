package imgui.internal.classes

import glm_.vec2.Vec2
import imgui.*
import imgui.classes.Storage
import imgui.internal.Axis


/** Docking system context */
class DockContext {
    val nodes = Storage()                      // Map ID -> ImGuiDockNode*: Active nodes
    val requests = ArrayList<DockRequest>()
    val settingsNodes = ArrayList<DockNodeSettings>()
    var wantFullRebuild = false
}

/** Store the source authority (dock node vs window) of a field */
enum class DataAuthority { Auto, DockNode, Window }

enum class DockNodeState { Unknown, HostWindowHiddenBecauseSingleWindow, HostWindowHiddenBecauseWindowsAreResizing, HostWindowVisible }

// sizeof() 116~160
class DockNode(
        val id: ID) {

    /** Flags shared by all nodes of a same dockspace hierarchy (inherited from the root node) */
    var sharedFlags = DockNodeFlag.None.i

    /** Flags specific to this node */
    var localFlags = DockNodeFlag.None.i

    var parentNode: DockNode? = null

    /** [Split node only] Child nodes (left/right or top/bottom). Consider switching to an array. */
    var childNodes = Array<DockNode?>(2) { null }

    /** Note: unordered list! Iterate TabBar->Tabs for user-order. */
    val windows = ArrayList<Window>()

    var tabBar: TabBar? = null

    /** Current position */
    val pos = Vec2()

    /** Current size */
    val size = Vec2()

    /** [Split node only] Last explicitly written-to size (overridden when using a splitter affecting the node), used to calculate Size. */
    val sizeRef = Vec2()

    /** [Split node only] Split axis (X or Y) */
    var splitAxis = Axis.None

    /** [Root node only] */
    val windowClass = WindowClass()


    var State = DockNodeState.Unknown

    var hostWindow: Window? = null

    /** Generally point to window which is ID is == SelectedTabID, but when CTRL+Tabbing this can be a different window. */
    var visibleWindow: Window? = null

    /** [Root node only] Pointer to central node. */
    var centralNode: DockNode? = null

    /** [Root node only] Set when there is a single visible node within the hierarchy. */
    var onlyNodeWithWindows: DockNode? = null

    /** Last frame number the node was updated or kept alive explicitly with DockSpace() + ImGuiDockNodeFlags_KeepAliveOnly */
    var lastFrameAlive = -1

    /** Last frame number the node was updated. */
    var lastFrameActive = -1

    /** Last frame number the node was focused. */
    var lastFrameFocused = -1

    /** [Root node only] Which of our child docking node (any ancestor in the hierarchy) was last focused. */
    var lastFocusedNodeId: ID = 0

    /** [Leaf node only] Which of our tab/window is selected. */
    var selectedTabId: ID = 0

    /** [Leaf node only] Set when closing a specific tab/window. */
    var wantCloseTabId: ID = 0

    var authorityForPos = DataAuthority.DockNode
    var authorityForSize = DataAuthority.DockNode
    var authorityForViewport = DataAuthority.Auto

    /** Set to false when the node is hidden (usually disabled as it has no active window) */
    var isVisible = true
    var isFocused = false
    var hasCloseButton = false
    var hasWindowMenuButton = false
    var enableCloseButton = false

    /** Set when closing all tabs at once. */
    var wantCloseAll = false
    var wantLockSizeOnce = false

    /** After a node extraction we need to transition toward moving the newly created host window */
    var wantMouseMove = false
    var wantHiddenTabBarUpdate = false
    var wantHiddenTabBarToggle = false

    /** Update by DockNodeTreeUpdatePosSize() write-filtering */
    var markedForPosSizeWrite = false

//    fun destroy() {} [JVM] useless

    val isRootNode: Boolean get() = parentNode == null
    val isDockSpace: Boolean get() = localFlags has DockNodeFlag._DockSpace
    val isFloatingNode: Boolean get() = parentNode == null && localFlags hasnt DockNodeFlag._DockSpace
    val isCentralNode: Boolean get() = localFlags has DockNodeFlag._CentralNode

    /** Hidden tab bar can be shown back by clicking the small triangle */
    val isHiddenTabBar: Boolean get() = localFlags has DockNodeFlag._HiddenTabBar

    /** Never show a tab bar */
    val isNoTabBar: Boolean get() = localFlags has DockNodeFlag._NoTabBar
    val isSplitNode: Boolean get() = childNodes[0] != null
    val isLeafNode: Boolean get() = childNodes[0] == null
    val isEmpty: Boolean get() = childNodes[0] == null && windows.isEmpty()
    val mergedFlags: DockNodeFlags get() = sharedFlags or localFlags
    fun rect() = Rect(pos.x, pos.y, pos.x + size.x, pos.y + size.y)
}