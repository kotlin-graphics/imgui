package imgui.api

import glm_.max
import imgui.*
import imgui.ImGui.findWindowByName
import imgui.ImGui.popID
import imgui.ImGui.pushOverrideID
import imgui.ImGui.style
import imgui.internal.classes.PtrOrIndex
import imgui.internal.classes.Rect
import imgui.internal.classes.TabBar
import kotlin.reflect.KMutableProperty0

/** Tab Bars, Tabs
 *  Note: Tabs are automatically created by the docking system. Use this to create tab bars/tabs yourself without docking being involved. */
interface tabBarsTabs {

    /** create and append into a TabBar */
    fun beginTabBar(strId: String, flags: TabBarFlags = 0): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val id = window.getID(strId)
        val tabBar = g.tabBars.getOrAddByKey(id)
        val tabBarBb = Rect(window.dc.cursorPos.x,
            window.dc.cursorPos.y,
            window.innerClipRect.max.x,
            window.dc.cursorPos.y + g.fontSize + style.framePadding.y * 2)
        tabBar.id = id
        return tabBar.beginEx(tabBarBb, flags or TabBarFlag._IsFocused, null)
    }

    /**  only call EndTabBar() if BeginTabBar() returns true! */
    fun endTabBar() {

        val window = g.currentWindow!!
        if (window.skipItems) return

        val tabBar = g.currentTabBar ?: error("Mismatched BeginTabBar()/EndTabBar()!")

        // Fallback in case no TabItem have been submitted
        if (tabBar.wantLayout)
            tabBar.layout()

        // Restore the last visible height if no tab is visible, this reduce vertical flicker/movement when a tabs gets removed without calling SetTabItemClosed().
        val tabBarAppearing = tabBar.prevFrameVisible + 1 < g.frameCount
        if (tabBar.visibleTabWasSubmitted || tabBar.visibleTabId == 0 || tabBarAppearing) {
            tabBar.currTabsContentsHeight = (window.dc.cursorPos.y - tabBar.barRect.max.y) max tabBar.currTabsContentsHeight
            window.dc.cursorPos.y = tabBar.barRect.max.y + tabBar.currTabsContentsHeight
        }
        else
            window.dc.cursorPos.y = tabBar.barRect.max.y + tabBar.prevTabsContentsHeight
        if (tabBar.beginCount > 1)
            window.dc.cursorPos put tabBar.backupCursorPos

        if (tabBar.flags hasnt TabBarFlag._DockNode) popID()

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

        val tabBar = g.currentTabBar ?: error("Needs to be called between BeginTabBar() and EndTabBar()!")
        assert(flags hasnt TabItemFlag._Button) { "BeginTabItem() Can't be used with button flags, use TabItemButton() instead!" }

        return tabBar.tabItemEx(label, pOpen, flags, null).also {
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

        val tabBar = g.currentTabBar ?: error("Needs to be called between BeginTabBar() and EndTabBar()!")
        assert(tabBar.lastTabItemIdx >= 0)
        val tab = tabBar.tabs[tabBar.lastTabItemIdx]
        if (tab.flags hasnt TabItemFlag.NoPushId) window.idStack.pop()
    }

    /** create a Tab behaving like a button. return true when clicked. cannot be selected in the tab bar. */
    fun tabItemButton(label: String, flags: TabItemFlags = TabItemFlag.None.i): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems)
            return false

        val tabBar = g.currentTabBar ?: error("Needs to be called between BeginTabBar() and EndTabBar()!")
        return tabBar.tabItemEx(label, null, flags or TabItemFlag._Button or TabItemFlag.NoReorder, null)
    }

    /** notify TabBar or Docking system of a closed tab/window ahead (useful to reduce visual flicker on reorderable tab bars).
     *  For tab-bar: call after BeginTabBar() and before Tab submissions. Otherwise call with a window name.
     *
     *  [Public] This is call is 100% optional but it allows to remove some one-frame glitches when a tab has been unexpectedly removed.
     *  To use it to need to call the function SetTabItemClosed() between BeginTabBar() and EndTabBar().
     *  Tabs closed by the close button will automatically be flagged to avoid this issue. */
    fun setTabItemClosed(label: String) {

        val tabBar = g.currentTabBar
        val isWithinManualTabBar = tabBar != null && tabBar.flags hasnt TabBarFlag._DockNode
        if (isWithinManualTabBar) {
            val tabId = tabBar!!.calcTabID(label, null)
            tabBar.findTabByID(tabId)?.wantClose = true // Will be processed by next call to TabBarLayout()
        } else findWindowByName(label)?.let { window ->
            if (window.dockIsActive)
                window.dockNode?.let { node ->
                val tabId = node.tabBar!!.calcTabID(label, window)
                node.tabBar!! removeTab tabId
                window.dockTabWantClose = true
            }
        }
    }

    // comfortable util
    val PtrOrIndex.tabBar: TabBar?
        get() = ptr ?: g.tabBars[index]
}