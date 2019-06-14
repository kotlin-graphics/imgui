package imgui.imgui

import glm_.max
import imgui.*
import imgui.ImGui.popId
import imgui.ImGui.pushOverrideID
import imgui.ImGui.style
import imgui.internal.Rect
import imgui.internal.TabBar
import imgui.internal.TabBarRef
import kotlin.reflect.KMutableProperty0
import imgui.internal.ColumnsFlag as Cf

/** Tab Bars, Tabs
 *  [BETA API] API may evolve!
 */
interface imgui_tabBarsTabs {

    /** create and append into a TabBar */
    fun beginTabBar(strId: String, flags: TabBarFlags = 0): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val id = window.getId(strId)
        val tabBar = g.tabBars.getOrAddByKey(id)
        val tabBarBb = Rect(window.dc.cursorPos.x, window.dc.cursorPos.y, window.innerWorkRect.max.x, window.dc.cursorPos.y + g.fontSize + style.framePadding.y * 2)
        tabBar.id = id
        return tabBar.beginEx(tabBarBb, flags or TabBarFlag.IsFocused)
    }

    /**  only call EndTabBar() if BeginTabBar() returns true! */
    fun endTabBar() {

        val window = g.currentWindow!!
        if (window.skipItems) return

        val tabBar = g.currentTabBar ?: error("Mismatched BeginTabBar()/EndTabBar()!") // FIXME-ERRORHANDLING
        if (tabBar.wantLayout)
            tabBar.layout()

        // Restore the last visible height if no tab is visible, this reduce vertical flicker/movement when a tabs gets removed without calling SetTabItemClosed().
        val tabBarAppearing = tabBar.prevFrameVisible + 1 < g.frameCount
        if (tabBar.visibleTabWasSubmitted || tabBar.visibleTabId == 0 || tabBarAppearing)
            tabBar.contentsHeight = (window.dc.cursorPos.y - tabBar.barRect.max.y) max 0f
        else
            window.dc.cursorPos.y = tabBar.barRect.max.y + tabBar.contentsHeight

        if (tabBar.flags hasnt TabBarFlag.DockNode)
            popId()

        g.currentTabBarStack.pop()
        g.currentTabBar = g.currentTabBarStack.lastOrNull()?.tabBar
    }

    /** create a Tab. Returns true if the Tab is selected. */
    fun beginTabItem(label: String, pOpen: BooleanArray, index: Int, flags: TabItemFlags = 0) =
            withBoolean(pOpen, index) { beginTabItem(label, it, flags) }

    /** create a Tab. Returns true if the Tab is selected. */
    fun beginTabItem(label: String, pOpen: KMutableProperty0<Boolean>? = null, flags: TabItemFlags = 0): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val tabBar = g.currentTabBar
                ?: error("Needs to be called between BeginTabBar() and EndTabBar()!") // FIXME-ERRORHANDLING
        return tabBar.tabItemEx(label, pOpen, flags).also {
            if (it && flags hasnt TabItemFlag.NoPushId) {
                val tab = tabBar.tabs[tabBar.lastTabItemIdx]
                pushOverrideID(tab.id) // We already hashed 'label' so push into the ID stack directly instead of doing another hash through PushID(label)
            }
        }
    }

    /** only call EndTabItem() if BeginTabItem() returns true! */
    fun endTabItem() {

        val window = g.currentWindow!!
        if (window.skipItems) return

        val tabBar = g.currentTabBar
                ?: error("Needs to be called between BeginTabBar() and EndTabBar()!") // FIXME-ERRORHANDLING
        assert(tabBar.lastTabItemIdx >= 0)
        val tab = tabBar.tabs[tabBar.lastTabItemIdx]
        if (tab.flags hasnt TabItemFlag.NoPushId)
            window.idStack.pop()
    }

    /** notify TabBar or Docking system of a closed tab/window ahead (useful to reduce visual flicker on reorderable tab bars).
     *  For tab-bar: call after BeginTabBar() and before Tab submissions. Otherwise call with a window name.
     *  [Public] This is call is 100% optional but it allows to remove some one-frame glitches when a tab has been unexpectedly removed.
     *  To use it to need to call the function SetTabItemClosed() after BeginTabBar() and before any call to BeginTabItem() */
    fun setTabItemClosed(tabOrDockedWindowLabel: String) {

        val isWithinManualTabBar = g.currentTabBar?.flags?.hasnt(TabBarFlag.DockNode) == true
        if (isWithinManualTabBar) {
            val tabBar = g.currentTabBar!!
            assert(tabBar.wantLayout) { "Needs to be called AFTER BeginTabBar() and BEFORE the first call to BeginTabItem()" }
            val tabId = tabBar calcTabID tabOrDockedWindowLabel
            tabBar removeTab tabId
        }
    }

    companion object {

        val TabBarRef.tabBar: TabBar?
            get() = ptr ?: g.tabBars[indexInMainPool]

        val TabBar.tabBarRef: TabBarRef?
            get() {
                if (this in g.tabBars)
                    return TabBarRef(g.tabBars.getIndex(this))
                return TabBarRef(this)
            }
    }
}