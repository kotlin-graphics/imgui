package imgui.internal.classes

import glm_.vec2.Vec2
import imgui.ID
import imgui.TabBarFlags
import imgui.emptyFlags

class TabBarSection {
    /** Number of tabs in this section. */
    var tabCount = 0

    /** Sum of width of tabs in this section (after shrinking down) */
    var width = 0f

    /** Horizontal spacing at the end of the section. */
    var spacing = 0f
}

/** Storage for a tab bar (sizeof() 152 bytes) */
class TabBar {

    val tabs = ArrayList<TabItem>()

    var flags: TabBarFlags = emptyFlags()

    /** Zero for tab-bars used by docking */
    var id: ID = 0

    /** Selected tab/window */
    var selectedTabId: ID = 0

    /** Next selected tab/window. Will also trigger a scrolling animation */
    var nextSelectedTabId: ID = 0

    /** Can occasionally be != SelectedTabId (e.g. when previewing contents for CTRL+TAB preview) */
    var visibleTabId: ID = 0
    var currFrameVisible = -1
    var prevFrameVisible = -1
    val barRect = Rect()

    var currTabsContentsHeight = 0f

    /** Record the height of contents submitted below the tab bar */
    var prevTabsContentsHeight = 0f

    /** Actual width of all tabs (locked during layout) */
    var widthAllTabs = 0f

    /** Ideal width if all tabs were visible and not clipped */
    var widthAllTabsIdeal = 0f

    var scrollingAnim = 0f
    var scrollingTarget = 0f
    var scrollingTargetDistToVisibility = 0f
    var scrollingSpeed = 0f
    var scrollingRectMinX = 0f
    var scrollingRectMaxX = 0f
    var reorderRequestTabId: ID = 0
    var reorderRequestOffset = 0
    var beginCount = 0

    var wantLayout = false
    var visibleTabWasSubmitted = false

    /** Set to true when a new tab item or button has been added to the tab bar during last frame */
    var tabsAddedNew = false

    /** Number of tabs submitted this frame. */
    var tabsActiveCount = 0

    /** Index of last BeginTabItem() tab for use by EndTabItem()  */
    var lastTabItemIdx = -1

    var itemSpacingY = 0f

    /** style.FramePadding locked at the time of BeginTabBar() */
    var framePadding = Vec2()

    val backupCursorPos = Vec2()

    /** For non-docking tab bar we re-append names in a contiguous buffer. */
    val tabsNames = ArrayList<String>()

    val TabItem.order: Int
        get() = tabs.indexOf(this)

    fun getTabName(tab: TabItem): String = tab.name

    val TabItem.name: String
        get() {
            assert(nameOffset in tabsNames.indices)
            return tabsNames[nameOffset]
        }
}

